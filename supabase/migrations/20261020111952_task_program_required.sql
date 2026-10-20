begin;
-- Preserve legacy NULL parents without guessing their program. Only their existing
-- NULL value may remain during partial status edits; new tasks and detachment fail.
create function private.require_task_program() returns trigger language plpgsql security invoker set search_path='' as $$
begin
 if new.program_id is null then
  if tg_op='INSERT' then raise exception 'Pilih Proker/UKOR untuk tugas ini.' using errcode='23514';
  elsif old.program_id is not null then raise exception 'Tugas tidak dapat dilepas dari programnya.' using errcode='23514';end if;
 end if;
 return new;
end $$;
revoke all on function private.require_task_program() from public,anon,authenticated;
create trigger task_program_required before insert or update on public.tasks for each row execute function private.require_task_program();
create or replace function public.workspace_directory() returns jsonb language sql stable security invoker set search_path='' as $$
 select jsonb_build_object(
  'profiles',coalesce((select jsonb_agg(jsonb_build_object('id',p.id,'name',p.name,'role',p.role,'active',p.active,'avatar_path',p.avatar_path) order by p.name,p.id) from public.profiles p where p.active),'[]'::jsonb),
  'programs',coalesce((select jsonb_agg(jsonb_build_object('id',p.id,'name',p.name,'kind',p.kind,'assignee_ids',coalesce((select jsonb_agg(a.profile_id order by a.profile_id) from public.program_assignees a where a.program_id=p.id),'[]'::jsonb)) order by p.name,p.id) from public.programs p),'[]'::jsonb)
 );
$$;
create or replace function public.task_summary(p_filters jsonb default '{}'::jsonb) returns jsonb language sql stable security invoker set search_path='' as $$
 with filtered as (
 select * from public.tasks t where
  (coalesce(p_filters->>'program_scope','') not in ('linked','unlinked') or
   (p_filters->>'program_scope'='linked' and t.program_id is not null) or
   (p_filters->>'program_scope'='unlinked' and t.program_id is null)) and
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
 'unlinked',(select count(*) from filtered where program_id is null),'unassigned',(select count(*) from filtered where assignee_id is null),'statuses',coalesce((select jsonb_object_agg(status,count) from counts),'{}'::jsonb),
 'workload',case when private.app_role() in ('staff','admin') then coalesce((select jsonb_agg(to_jsonb(w) order by overdue desc,open desc,assignee_id) from workload w),'[]'::jsonb) else '[]'::jsonb end);
$$;
create or replace function public.task_board(p_filters jsonb default '{}'::jsonb,p_pages integer[] default array[0,0,0,0,0]) returns jsonb language plpgsql stable security invoker set search_path='' as $$
declare result jsonb;
begin
 if p_pages is null or array_ndims(p_pages)<>1 or cardinality(p_pages)<>5 or array_lower(p_pages,1)<>1 or exists(select 1 from unnest(p_pages) n where n is null or n<0 or n>100000) then raise exception 'Invalid board pagination' using errcode='23514';end if;
 with filtered as materialized (
  select * from public.tasks t where
  (coalesce(p_filters->>'program_scope','') not in ('linked','unlinked') or
   (p_filters->>'program_scope'='linked' and t.program_id is not null) or
   (p_filters->>'program_scope'='unlinked' and t.program_id is null)) and
  (nullif(p_filters->>'program_id','') is null or t.program_id=(p_filters->>'program_id')::uuid) and
  (nullif(p_filters->>'assignee_id','') is null or t.assignee_id=(p_filters->>'assignee_id')::uuid) and
  (nullif(p_filters->>'priority','') is null or t.priority=p_filters->>'priority') and
  (nullif(p_filters->>'status','') is null or t.status=p_filters->>'status') and
  (nullif(btrim(p_filters->>'q'),'') is null or strpos(lower(t.title||' '||coalesce(t.description,'')),lower(btrim(p_filters->>'q')))>0) and
  (coalesce(p_filters->>'scope','') not in ('mine','overdue','unassigned') or
   (p_filters->>'scope'='mine' and t.assignee_id=(select auth.uid())) or
   (p_filters->>'scope'='unassigned' and t.assignee_id is null) or
   (p_filters->>'scope'='overdue' and t.status<>'Done' and t.due_date<(now() at time zone 'Asia/Jakarta')::date))
 ), counts as(select status,count(*) count from filtered group by status),
 workload as(select assignee_id,count(*) total,count(*) filter(where status='Done') done,count(*) filter(where status<>'Done') open,
  count(*) filter(where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date) overdue from filtered group by assignee_id),
 columns as (
  select s.status,jsonb_build_object('items',coalesce((select jsonb_agg(to_jsonb(v) order by v.created_at desc,v.id) from (select id,title,status,priority,program_id,assignee_id,created_by,due_date,created_at,updated_at from filtered f where f.status=s.status order by f.created_at desc,f.id limit 12 offset p_pages[s.n]::bigint*12) v),'[]'::jsonb),
   'hasMore',(select count(*)>p_pages[s.n]::bigint*12+12 from filtered f where f.status=s.status)) value
  from (values('Backlog',1),('To Do',2),('In Progress',3),('Review',4),('Done',5)) s(status,n)
 )
 select jsonb_build_object('summary',jsonb_build_object('total',(select count(*) from filtered),'done',(select count(*) from filtered where status='Done'),
  'overdue',(select count(*) from filtered where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date),
  'unlinked',(select count(*) from filtered where program_id is null),'unassigned',(select count(*) from filtered where assignee_id is null),'statuses',coalesce((select jsonb_object_agg(status,count) from counts),'{}'::jsonb),
  'workload',case when (select private.app_role()) in ('staff','admin') then coalesce((select jsonb_agg(to_jsonb(w) order by overdue desc,open desc,assignee_id) from workload w),'[]'::jsonb) else '[]'::jsonb end),
  'columns',(select jsonb_object_agg(status,value) from columns)) into result;
 return result;
end $$;
revoke all on function public.workspace_directory(),public.task_summary(jsonb),public.task_board(jsonb,integer[]) from public,anon;
grant execute on function public.workspace_directory(),public.task_summary(jsonb),public.task_board(jsonb,integer[]) to authenticated;
commit;
