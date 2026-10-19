-- Global KPI totals and Staff status must not depend on the 30-row preview.
create or replace function public.dashboard_stats() returns jsonb language sql stable security invoker set search_path = '' as $$
 with p as (select count(*) total_programs,count(*) filter(where progress is not null) recorded_programs,count(*) filter(where status in ('Active','Ongoing')) active_programs,count(*) filter(where status='Completed') completed_programs,coalesce(sum(budget),0) budget from public.programs),
 t as (select count(*) filter(where status<>'Done') pending_tasks,count(*) filter(where status='Done') completed_tasks from public.tasks),
 f as (select coalesce(sum(amount) filter(where type='expense' and status='approved'),0) expense,coalesce(sum(amount) filter(where type='income' and status='approved'),0) income from public.finances),
 i as (select coalesce(sum(quantity),0) total_assets from public.inventory),
 months as (select generate_series(1,12) m),
 monthly as (select m,coalesce(sum(amount) filter(where type='income'),0) income,coalesce(sum(amount) filter(where type='expense'),0) expense from months left join public.finances on extract(month from transaction_date)=m and extract(year from transaction_date)=extract(year from current_date) and status='approved' group by m),
 assets as (select status,coalesce(sum(quantity),0) quantity from public.inventory group by status)
 select jsonb_build_object(

 'programStatusCounts',coalesce((select jsonb_agg(to_jsonb(q) order by q.kind,q.status) from (select kind,status,count(*) count from public.programs group by kind,status)q),'[]'::jsonb),
 'overview',jsonb_build_object('proker',(select count(*) from public.programs where kind='Proker'),'ukor',(select count(*) from public.programs where kind='UKOR'),'openTasks',t.pending_tasks,'overdueTasks',(select count(*) from public.tasks where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date)),
 'programFocus',coalesce((select jsonb_agg(to_jsonb(q) order by q.kind,q.name) from (select id,name,kind,pic,status,progress,progress_notes,next_milestone,milestone_date,updated_at from public.programs order by kind,name limit 30)q),'[]'::jsonb),
 'upcomingMilestones',coalesce((select jsonb_agg(to_jsonb(q) order by q.milestone_date,q.name) from (select id,name,next_milestone,milestone_date,pic from public.programs where milestone_date>=(now() at time zone 'Asia/Jakarta')::date and status not in ('Completed','Cancelled') order by milestone_date,name limit 8)q),'[]'::jsonb),
 'attentionTasks',coalesce((select jsonb_agg(to_jsonb(q) order by q.due_date nulls last,q.title) from (select t.id,t.title,t.status,t.due_date,t.assignee_id,p.name assignee from public.tasks t left join public.profiles p on p.id=t.assignee_id where t.status<>'Done' order by t.due_date nulls last,t.title limit 6)q),'[]'::jsonb),
 'staffPerformance',coalesce((select jsonb_agg(to_jsonb(q) order by q.name) from (
 select p.id,p.name,coalesce(a.programs,'[]'::jsonb) programs,coalesce(t.assigned,0) "assignedTasks",coalesce(t.done,0) "completedTasks",coalesce(t.review,0) "reviewTasks",coalesce(t.overdue,0) "overdueTasks",coalesce(u.updates,0) "updatesLast30Days",u.last_update "lastUpdate"
 from public.profiles p
 left join (select a.profile_id,jsonb_agg(jsonb_build_object('id',g.id,'name',g.name,'kind',g.kind,'status',g.status,'progress',g.progress) order by g.kind,g.name) programs from public.program_assignees a join public.programs g on g.id=a.program_id group by a.profile_id)a on a.profile_id=p.id
 left join (select assignee_id,count(*) assigned,count(*) filter(where status='Done') done,count(*) filter(where status='Review') review,count(*) filter(where status<>'Done' and due_date<(now() at time zone 'Asia/Jakarta')::date) overdue from public.tasks group by assignee_id)t on t.assignee_id=p.id
 left join (select actor_id,count(*) filter(where created_at>=now()-interval '30 days') updates,max(created_at) last_update from public.program_updates group by actor_id)u on u.actor_id=p.id
 where p.active and p.role='member')q),'[]'::jsonb),
 'summary',jsonb_build_object('totalPrograms',p.total_programs,'recordedPrograms',p.recorded_programs,'activePrograms',p.active_programs,'completedPrograms',p.completed_programs,'pendingTasks',t.pending_tasks,'completedTasks',t.completed_tasks,'budget',p.budget,'expense',f.expense,'income',f.income,'remaining',p.budget-f.expense,'budgetUtilization',case when p.budget>0 then round(f.expense/p.budget*100,1) else 0 end,'totalAssets',i.total_assets),
 'monthly',(select jsonb_agg(jsonb_build_object('month',m,'income',income,'expense',expense) order by m) from monthly),
 'inventory',coalesce((select jsonb_agg(jsonb_build_object('status',status,'quantity',quantity)) from assets),'[]'::jsonb),
 'upcomingPrograms',coalesce((select jsonb_agg(to_jsonb(upcoming)) from (select id,name,start_date,end_date,status from public.programs where end_date>=current_date and status not in ('Cancelled','Completed') order by start_date limit 5) upcoming),'[]'::jsonb)
 ) from p,t,f,i;
$$;
revoke all on function public.dashboard_stats() from public,anon;
grant execute on function public.dashboard_stats() to authenticated;
