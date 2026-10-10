begin;
alter table public.programs alter column start_date drop not null, alter column end_date drop not null,
 alter column progress drop not null, alter column progress drop default,
 alter column budget drop not null, alter column budget drop default;
alter table public.programs drop constraint programs_status_check;
alter table public.programs add constraint programs_status_check check(status in ('Planning','Ongoing','Active','Completed','Cancelled','Unspecified'));
alter table public.programs add column kind text not null default 'Proker' check(kind in ('Proker','UKOR')),
 add column progress_notes text check(length(progress_notes)<=2000),
 add column next_milestone text check(length(next_milestone)<=160), add column milestone_date date;
grant update(kind,progress_notes,next_milestone,milestone_date) on public.programs to authenticated;
create table public.program_assignees (
 program_id uuid not null references public.programs(id) on delete cascade,
 profile_id uuid not null references public.profiles(id) on delete cascade,
 primary key(program_id,profile_id)
);
create index program_assignees_profile on public.program_assignees(profile_id,program_id);
create index programs_milestone on public.programs(milestone_date) where milestone_date is not null;
create table public.program_updates (
 id bigint generated always as identity primary key,
 program_id uuid not null references public.programs(id) on delete cascade,
 actor_id uuid references public.profiles(id) on delete set null,
 status text not null, progress integer, notes text, created_at timestamptz not null default now()
);
create index program_updates_actor_date on public.program_updates(actor_id,created_at desc);
create index program_updates_program on public.program_updates(program_id);
alter table public.program_assignees enable row level security;
alter table public.program_updates enable row level security;
revoke all on public.program_assignees,public.program_updates from public,anon,authenticated;
revoke all on sequence public.program_updates_id_seq from public,anon,authenticated;
grant select on public.program_assignees,public.program_updates to authenticated;
grant insert,delete on public.program_assignees to authenticated;
create policy program_assignees_read on public.program_assignees for select to authenticated using((select private.app_role()) is not null);
create policy program_assignees_insert on public.program_assignees for insert to authenticated with check((select private.app_role()) in ('staff','admin'));
create policy program_assignees_delete on public.program_assignees for delete to authenticated using((select private.app_role()) in ('staff','admin'));
create policy program_updates_read on public.program_updates for select to authenticated using((select private.app_role()) is not null);
insert into public.program_assignees(program_id,profile_id) select id,pic_id from public.programs where pic_id is not null;
drop policy programs_update on public.programs;
create policy programs_update on public.programs for update to authenticated
using((select private.app_role()) in ('staff','admin') or ((select private.app_role())='member' and exists(select 1 from public.program_assignees a where a.program_id=programs.id and a.profile_id=(select auth.uid()))))
with check((select private.app_role()) in ('staff','admin') or ((select private.app_role())='member' and exists(select 1 from public.program_assignees a where a.program_id=programs.id and a.profile_id=(select auth.uid()))));
-- RLS controls rows; the trigger also limits the columns an assigned Staff can change.
create function private.guard_program_progress() returns trigger language plpgsql security invoker set search_path='' as $$
begin
 if private.app_role()='member' and (to_jsonb(new)-array['status','progress','progress_notes','updated_at']) is distinct from (to_jsonb(old)-array['status','progress','progress_notes','updated_at']) then
  raise exception 'Staff may only update assigned program progress' using errcode='42501';
 end if;
 return new;
end $$;
revoke all on function private.guard_program_progress() from public,anon,authenticated;
create trigger program_progress_guard before update on public.programs for each row execute function private.guard_program_progress();
-- Audit rows can only be produced by this trigger, after an authorized update.
create function private.record_program_update() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is not null and private.app_role() is not null and (new.status,new.progress,new.progress_notes) is distinct from (old.status,old.progress,old.progress_notes) then
  insert into public.program_updates(program_id,actor_id,status,progress,notes) values(new.id,auth.uid(),new.status,new.progress,new.progress_notes);
 end if;
 return new;
end $$;
revoke all on function private.record_program_update() from public,anon,authenticated;
create trigger program_progress_audit after update on public.programs for each row execute function private.record_program_update();
-- BPH writes a program and its multiple PJ assignments in one transaction, under caller RLS.
create function public.save_program(p_id uuid,p_payload jsonb) returns jsonb language plpgsql security invoker set search_path='' as $$
declare result public.programs; previous public.programs; members uuid[]; member uuid;
begin
 if private.app_role() not in ('staff','admin') or private.app_role() is null then raise exception 'BPH required' using errcode='42501'; end if;
 if p_id is not null then
  select * into previous from public.programs where id=p_id for update;
  if not found then return null; end if;
 end if;
 if p_payload->'assignee_ids' is not null and p_payload->'assignee_ids'<>'null'::jsonb then
  if jsonb_typeof(p_payload->'assignee_ids')<>'array' or jsonb_array_length(p_payload->'assignee_ids')>30 then raise exception 'Invalid PJ list' using errcode='23514'; end if;
  select coalesce(array_agg(distinct value::uuid),'{}'::uuid[]) into members from jsonb_array_elements_text(p_payload->'assignee_ids');
 elsif p_payload->>'pic_id' is not null and (p_id is null or (p_payload->>'pic_id')::uuid is distinct from previous.pic_id) then
  members=array[(p_payload->>'pic_id')::uuid];
 elsif p_id is null then
  members='{}'::uuid[];
 end if;
 if members is not null and exists(select 1 from unnest(members) m left join public.profiles p on p.id=m where p.id is null or not p.active) then raise exception 'PJ must be an active profile' using errcode='23514'; end if;
 if p_id is null then
  insert into public.programs(name,description,pic,pic_id,start_date,end_date,status,progress,budget,kind,progress_notes,next_milestone,milestone_date)
  values(p_payload->>'name',p_payload->>'description',p_payload->>'pic',members[1],(p_payload->>'start_date')::date,(p_payload->>'end_date')::date,p_payload->>'status',(p_payload->>'progress')::integer,(p_payload->>'budget')::numeric,coalesce(p_payload->>'kind','Proker'),p_payload->>'progress_notes',p_payload->>'next_milestone',(p_payload->>'milestone_date')::date) returning * into result;
 else
  update public.programs set name=p_payload->>'name',description=p_payload->>'description',pic=p_payload->>'pic',
  pic_id=case when members is null then coalesce((p_payload->>'pic_id')::uuid,previous.pic_id) else members[1] end,
  start_date=(p_payload->>'start_date')::date,end_date=(p_payload->>'end_date')::date,status=p_payload->>'status',progress=(p_payload->>'progress')::integer,budget=(p_payload->>'budget')::numeric,
  kind=coalesce(p_payload->>'kind',previous.kind),progress_notes=coalesce(p_payload->>'progress_notes',previous.progress_notes),
  next_milestone=coalesce(p_payload->>'next_milestone',previous.next_milestone),milestone_date=case when p_payload->>'next_milestone' is null then previous.milestone_date else (p_payload->>'milestone_date')::date end
  where id=p_id returning * into result;
 end if;
 if members is not null then
  delete from public.program_assignees where program_id=result.id;
  foreach member in array members loop insert into public.program_assignees(program_id,profile_id) values(result.id,member); end loop;
 end if;
 return to_jsonb(result)||jsonb_build_object('assignee_ids',coalesce((select jsonb_agg(profile_id order by profile_id) from public.program_assignees where program_id=result.id),'[]'::jsonb));
end $$;
revoke all on function public.save_program(uuid,jsonb) from public,anon;
grant execute on function public.save_program(uuid,jsonb) to authenticated;
create or replace function public.dashboard_stats() returns jsonb language sql stable security invoker set search_path = '' as $$
 with p as (select count(*) total_programs,count(*) filter(where status in ('Active','Ongoing')) active_programs,count(*) filter(where status='Completed') completed_programs,coalesce(sum(budget),0) budget from public.programs),
 t as (select count(*) filter(where status<>'Done') pending_tasks,count(*) filter(where status='Done') completed_tasks from public.tasks),
 f as (select coalesce(sum(amount) filter(where type='expense' and status='approved'),0) expense,coalesce(sum(amount) filter(where type='income' and status='approved'),0) income from public.finances),
 i as (select coalesce(sum(quantity),0) total_assets from public.inventory),
 months as (select generate_series(1,12) m),
 monthly as (select m,coalesce(sum(amount) filter(where type='income'),0) income,coalesce(sum(amount) filter(where type='expense'),0) expense from months left join public.finances on extract(month from transaction_date)=m and extract(year from transaction_date)=extract(year from current_date) and status='approved' group by m),
 assets as (select status,coalesce(sum(quantity),0) quantity from public.inventory group by status)
 select jsonb_build_object(

 'overview',jsonb_build_object('proker',(select count(*) from public.programs where kind='Proker'),'ukor',(select count(*) from public.programs where kind='UKOR'),'openTasks',t.pending_tasks,'overdueTasks',(select count(*) from public.tasks where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date)),
 'programFocus',coalesce((select jsonb_agg(to_jsonb(q) order by q.kind,q.name) from (select id,name,kind,pic,status,progress,progress_notes,next_milestone,milestone_date,updated_at from public.programs order by kind,name limit 30)q),'[]'::jsonb),
 'upcomingMilestones',coalesce((select jsonb_agg(to_jsonb(q) order by q.milestone_date,q.name) from (select id,name,next_milestone,milestone_date,pic from public.programs where milestone_date>=(now() at time zone 'Asia/Jakarta')::date and status not in ('Completed','Cancelled') order by milestone_date,name limit 8)q),'[]'::jsonb),
 'attentionTasks',coalesce((select jsonb_agg(to_jsonb(q) order by q.due_date nulls last,q.title) from (select t.id,t.title,t.status,t.due_date,t.assignee_id,p.name assignee from public.tasks t left join public.profiles p on p.id=t.assignee_id where t.status<>'Done' order by t.due_date nulls last,t.title limit 6)q),'[]'::jsonb),
 'staffPerformance',coalesce((select jsonb_agg(to_jsonb(q) order by q.name) from (
 select p.id,p.name,coalesce(a.programs,'[]'::jsonb) programs,coalesce(t.assigned,0) "assignedTasks",coalesce(t.done,0) "completedTasks",coalesce(t.review,0) "reviewTasks",coalesce(t.overdue,0) "overdueTasks",coalesce(u.updates,0) "updatesLast30Days",u.last_update "lastUpdate"
 from public.profiles p
 left join (select a.profile_id,jsonb_agg(jsonb_build_object('id',g.id,'name',g.name,'kind',g.kind) order by g.kind,g.name) programs from public.program_assignees a join public.programs g on g.id=a.program_id group by a.profile_id)a on a.profile_id=p.id
 left join (select assignee_id,count(*) assigned,count(*) filter(where status='Done') done,count(*) filter(where status='Review') review,count(*) filter(where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date) overdue from public.tasks group by assignee_id)t on t.assignee_id=p.id
 left join (select actor_id,count(*) filter(where created_at>=now()-interval '30 days') updates,max(created_at) last_update from public.program_updates group by actor_id)u on u.actor_id=p.id
 where p.active and p.role='member')q),'[]'::jsonb),
 'summary',jsonb_build_object('totalPrograms',p.total_programs,'activePrograms',p.active_programs,'completedPrograms',p.completed_programs,'pendingTasks',t.pending_tasks,'completedTasks',t.completed_tasks,'budget',p.budget,'expense',f.expense,'income',f.income,'remaining',p.budget-f.expense,'budgetUtilization',case when p.budget>0 then round(f.expense/p.budget*100,1) else 0 end,'totalAssets',i.total_assets),
 'monthly',(select jsonb_agg(jsonb_build_object('month',m,'income',income,'expense',expense) order by m) from monthly),
 'inventory',coalesce((select jsonb_agg(jsonb_build_object('status',status,'quantity',quantity)) from assets),'[]'::jsonb),
 'upcomingPrograms',coalesce((select jsonb_agg(to_jsonb(upcoming)) from (select id,name,start_date,end_date,status from public.programs where end_date>=current_date and status not in ('Cancelled','Completed') order by start_date limit 5) upcoming),'[]'::jsonb)
 ) from p,t,f,i;
$$;
revoke all on function public.dashboard_stats() from public,anon;
grant execute on function public.dashboard_stats() to authenticated;
commit;
