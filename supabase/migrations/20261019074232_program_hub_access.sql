begin;
-- Private, UID-guarded helper breaks the program <-> assignment RLS cycle.
create function private.can_access_program(p_id uuid) returns boolean language sql stable security definer set search_path='' as $$
 select auth.uid() is not null and (private.app_role() in ('staff','admin') or
 (private.app_role()='member' and exists(select 1 from public.program_assignees a where a.program_id=p_id and a.profile_id=auth.uid())));
$$;
revoke all on function private.can_access_program(uuid) from public,anon;
grant execute on function private.can_access_program(uuid) to authenticated;
drop policy programs_read on public.programs;
create policy programs_read on public.programs for select to authenticated using(private.can_access_program(id));
drop policy program_assignees_read on public.program_assignees;
create policy program_assignees_read on public.program_assignees for select to authenticated using(private.can_access_program(program_id));
drop policy program_updates_read on public.program_updates;
create policy program_updates_read on public.program_updates for select to authenticated using(private.can_access_program(program_id));
drop policy programs_update on public.programs;
create policy programs_update on public.programs for update to authenticated using(private.can_access_program(id)) with check(private.can_access_program(id));
drop policy finances_read on public.finances;
create policy finances_read on public.finances for select to authenticated using((select private.app_role()) is not null and (program_id is null or private.can_access_program(program_id)));
drop policy events_read on public.events;
create policy events_read on public.events for select to authenticated using((select private.app_role()) is not null and (program_id is null or private.can_access_program(program_id)));
drop policy tasks_read on public.tasks;
create policy tasks_read on public.tasks for select to authenticated using(
 (select private.app_role()) is not null and (case when program_id is not null then private.can_access_program(program_id)
 else (select private.app_role()) in ('staff','admin') or created_by=(select auth.uid()) or assignee_id=(select auth.uid()) end));
drop policy tasks_insert on public.tasks;
create policy tasks_insert on public.tasks for insert to authenticated with check(created_by=(select auth.uid()) and
 (program_id is null or private.can_access_program(program_id)) and ((select private.app_role()) in ('staff','admin') or ((select private.app_role())='member' and (assignee_id is null or assignee_id=(select auth.uid())))));
drop policy tasks_update on public.tasks;
create policy tasks_update on public.tasks for update to authenticated
using((program_id is null or private.can_access_program(program_id)) and ((select private.app_role()) in ('staff','admin') or ((select private.app_role())='member' and (created_by=(select auth.uid()) or assignee_id=(select auth.uid())))))
with check((program_id is null or private.can_access_program(program_id)) and ((select private.app_role()) in ('staff','admin') or ((select private.app_role())='member' and (created_by=(select auth.uid()) or assignee_id=(select auth.uid())) and (assignee_id is null or assignee_id=(select auth.uid())))));
drop policy tasks_delete on public.tasks;
create policy tasks_delete on public.tasks for delete to authenticated using((program_id is null or private.can_access_program(program_id)) and ((select private.app_role()) in ('staff','admin') or ((select private.app_role())='member' and (created_by=(select auth.uid()) or assignee_id=(select auth.uid())))));
-- Validate newly chosen accounts, without rewriting or rejecting unchanged legacy assignments.
create function private.guard_task_assignment() returns trigger language plpgsql security invoker set search_path='' as $$
begin
 if tg_op='INSERT' or (new.program_id,new.assignee_id) is distinct from (old.program_id,old.assignee_id) then
  if new.assignee_id is not null and not exists(select 1 from public.profiles p where p.id=new.assignee_id and p.active and
   (new.program_id is null or p.role in ('staff','admin') or exists(select 1 from public.program_assignees a where a.program_id=new.program_id and a.profile_id=p.id))) then
   raise exception 'Assignee must be an active account with program access' using errcode='23514';
  end if;
 end if;return new;
end $$;
revoke all on function private.guard_task_assignment() from public,anon,authenticated;
create trigger task_assignment_guard before insert or update on public.tasks for each row execute function private.guard_task_assignment();
create index tasks_status_date on public.tasks(status,due_date,id);

create table public.program_notes (
 id uuid primary key default gen_random_uuid(), program_id uuid not null references public.programs(id) on delete cascade,
 body text not null check(length(btrim(body)) between 1 and 2000),
 created_by uuid not null default auth.uid() references public.profiles(id),created_at timestamptz not null default now()
);
create table public.program_milestones (
 id uuid primary key default gen_random_uuid(),program_id uuid not null references public.programs(id) on delete cascade,
 title text not null check(length(btrim(title)) between 1 and 160),due_date date,
 status text not null default 'Planned' check(status in ('Planned','Done')),
 created_by uuid not null default auth.uid() references public.profiles(id),created_at timestamptz not null default now(),updated_at timestamptz not null default now()
);
create index program_notes_program_date on public.program_notes(program_id,created_at desc,id);
create index program_notes_creator on public.program_notes(created_by);
create index program_milestones_program_date on public.program_milestones(program_id,due_date,id);
create index program_milestones_creator on public.program_milestones(created_by);
alter table public.program_notes enable row level security;
alter table public.program_milestones enable row level security;
revoke all on public.program_notes,public.program_milestones from public,anon,authenticated;
grant select,insert on public.program_notes to authenticated;
grant select,insert,delete on public.program_milestones to authenticated;
grant update(title,due_date,status) on public.program_milestones to authenticated;
create policy program_notes_read on public.program_notes for select to authenticated using(private.can_access_program(program_id));
create policy program_notes_insert on public.program_notes for insert to authenticated with check(private.can_access_program(program_id) and created_by=(select auth.uid()));
create policy program_milestones_read on public.program_milestones for select to authenticated using(private.can_access_program(program_id));
create policy program_milestones_insert on public.program_milestones for insert to authenticated with check(private.can_access_program(program_id) and created_by=(select auth.uid()));
create policy program_milestones_update on public.program_milestones for update to authenticated using(private.can_access_program(program_id)) with check(private.can_access_program(program_id));
create policy program_milestones_delete on public.program_milestones for delete to authenticated using(private.can_access_program(program_id));
create trigger program_milestone_updated before update on public.program_milestones for each row execute function public.touch_updated_at();

-- The old text PIC remains a compatibility label. Account UUIDs alone determine access.
-- Wrap the existing invoker RPC, deriving its label from active profile records.
alter function public.save_program(uuid,jsonb) rename to save_program_accounts_impl;
revoke all on function public.save_program_accounts_impl(uuid,jsonb) from public,anon;
-- Keep implementation out of the exposed public API.
alter function public.save_program_accounts_impl(uuid,jsonb) set schema private;
create function public.save_program(p_id uuid,p_payload jsonb) returns jsonb language plpgsql security invoker set search_path='' as $$
declare members uuid[]; label text;
begin
 if private.app_role() not in ('staff','admin') or private.app_role() is null then raise exception 'BPH required' using errcode='42501';end if;
 if p_payload->'assignee_ids' is not null and p_payload->'assignee_ids'<>'null'::jsonb then
  if jsonb_typeof(p_payload->'assignee_ids')<>'array' or jsonb_array_length(p_payload->'assignee_ids') not between 1 and 30 then raise exception 'Select at least one account' using errcode='23514';end if;
  select array_agg(distinct value::uuid) into members from jsonb_array_elements_text(p_payload->'assignee_ids');
 elsif p_payload->>'pic_id' is not null then members=array[(p_payload->>'pic_id')::uuid];
 elsif p_id is not null then select array_agg(profile_id) into members from public.program_assignees where program_id=p_id;
 end if;
 if coalesce(cardinality(members),0)=0 or exists(select 1 from unnest(members) m left join public.profiles p on p.id=m where p.id is null or not p.active) then raise exception 'Select active PJ accounts' using errcode='23514';end if;
 select left(string_agg(name,', ' order by name,id),120) into label from public.profiles where id=any(members);
 return private.save_program_accounts_impl(p_id,p_payload||jsonb_build_object('pic',label,'assignee_ids',to_jsonb(members)));
end $$;
revoke all on function public.save_program(uuid,jsonb) from public,anon;
grant execute on function public.save_program(uuid,jsonb),private.save_program_accounts_impl(uuid,jsonb) to authenticated;

create function public.program_timeline(p_id uuid,p_page integer default 0,p_size integer default 20) returns jsonb language plpgsql stable security invoker set search_path='' as $$
declare result jsonb;
begin
 if not exists(select 1 from public.programs where id=p_id) then return null;end if;
 if p_page<0 or p_page>100000 or p_size not between 1 and 100 then raise exception 'Invalid pagination' using errcode='23514';end if;
 with activity as (
  select 'progress-'||id::text id,'progress' kind,actor_id,status,progress,notes body,created_at from public.program_updates where program_id=p_id
  union all select 'note-'||id::text,'note',created_by,null,null,body,created_at from public.program_notes where program_id=p_id
 ), page as (select * from activity order by created_at desc,id desc limit p_size+1 offset p_page::bigint*p_size),
 visible as(select * from page order by created_at desc,id desc limit p_size)
 select jsonb_build_object('items',coalesce((select jsonb_agg(to_jsonb(v) order by created_at desc,id desc) from visible v),'[]'::jsonb),'hasMore',(select count(*)>p_size from page)) into result;
 return result;
end $$;
create function public.task_summary(p_filters jsonb default '{}'::jsonb) returns jsonb language sql stable security invoker set search_path='' as $$
 with filtered as (
 select * from public.tasks t where
 (nullif(p_filters->>'program_id','') is null or t.program_id=(p_filters->>'program_id')::uuid) and
 (nullif(p_filters->>'assignee_id','') is null or t.assignee_id=(p_filters->>'assignee_id')::uuid) and
 (nullif(p_filters->>'priority','') is null or t.priority=p_filters->>'priority') and
 (nullif(p_filters->>'status','') is null or t.status=p_filters->>'status') and
 (nullif(btrim(p_filters->>'q'),'') is null or strpos(lower(t.title||' '||coalesce(t.description,'')),lower(btrim(p_filters->>'q')))>0) and
 (coalesce(p_filters->>'scope','') not in ('mine','overdue','unassigned') or
  (p_filters->>'scope'='mine' and t.assignee_id=auth.uid()) or
  (p_filters->>'scope'='unassigned' and t.assignee_id is null) or
  (p_filters->>'scope'='overdue' and t.status<>'Done' and t.due_date<(now() at time zone 'Asia/Jakarta')::date))
 ), counts as(select status,count(*) count from filtered group by status),
 workload as(select assignee_id,count(*) total,count(*) filter(where status='Done') done,count(*) filter(where status<>'Done') open,
 count(*) filter(where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date) overdue from filtered group by assignee_id)
 select jsonb_build_object('total',(select count(*) from filtered),'done',(select count(*) from filtered where status='Done'),
 'overdue',(select count(*) from filtered where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date),
 'unassigned',(select count(*) from filtered where assignee_id is null),'statuses',coalesce((select jsonb_object_agg(status,count) from counts),'{}'::jsonb),
 'workload',case when private.app_role() in ('staff','admin') then coalesce((select jsonb_agg(to_jsonb(w) order by overdue desc,open desc,assignee_id) from workload w),'[]'::jsonb) else '[]'::jsonb end);
$$;
revoke all on function public.program_timeline(uuid,integer,integer),public.task_summary(jsonb) from public,anon;
grant execute on function public.program_timeline(uuid,integer,integer),public.task_summary(jsonb) to authenticated;
commit;
