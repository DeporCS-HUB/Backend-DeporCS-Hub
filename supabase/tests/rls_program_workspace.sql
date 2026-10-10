-- Disposable local/CI database only. Fixtures and every mutation roll back.
begin;
insert into auth.users(id,email,raw_user_meta_data) values
 ('90000000-0000-0000-0000-000000000001','manager@example.invalid','{"name":"Manager"}'),
 ('90000000-0000-0000-0000-000000000002','pj@example.invalid','{"name":"Assigned Staff"}'),
 ('90000000-0000-0000-0000-000000000003','other@example.invalid','{"name":"Other Staff"}');
update public.profiles set active=true where id::text like '90000000-%';
update public.profiles set role='staff' where id='90000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub='90000000-0000-0000-0000-000000000001';
insert into public.programs(id,name,pic) values('91000000-0000-0000-0000-000000000001','Shared unit','Two PJ');
do $$declare result jsonb;begin
 result=public.save_program('91000000-0000-0000-0000-000000000001','{"name":"Shared unit","pic":"Two PJ","kind":"UKOR","status":"Ongoing","assignee_ids":["90000000-0000-0000-0000-000000000002","90000000-0000-0000-0000-000000000003"]}');
 if jsonb_array_length(result->'assignee_ids')<>2 or result->'progress'<>'null'::jsonb then raise exception 'multi PJ or unknown progress lost';end if;
 begin
  perform public.save_program('91000000-0000-0000-0000-000000000001','{"name":"Must not persist","pic":"Missing PJ","status":"Planning","assignee_ids":["99999999-0000-0000-0000-000000000001"]}');
  raise exception 'invalid PJ accepted';
 exception when check_violation then null;end;
 if (select name from public.programs where id='91000000-0000-0000-0000-000000000001')<>'Shared unit' or (select count(*) from public.program_assignees)<>2 then raise exception 'failed assignment changed program or removed PJ';end if;
end$$;
insert into public.tasks(title,status,assignee_id,due_date) values
 ('Finished task','Done','90000000-0000-0000-0000-000000000002',null),
 ('Overdue task','To Do','90000000-0000-0000-0000-000000000002',(now() at time zone 'Asia/Jakarta')::date-1),
 ('Unassigned document','To Do',null,null);
set local request.jwt.claim.sub='90000000-0000-0000-0000-000000000002';
update public.programs set progress=40,progress_notes='Selection underway' where id='91000000-0000-0000-0000-000000000001';
do $$declare metrics jsonb;begin
 if (select progress from public.programs where id='91000000-0000-0000-0000-000000000001')<>40 then raise exception 'assigned Staff update failed';end if;
 if (select count(*) from public.program_updates where actor_id=auth.uid())<>1 then raise exception 'audit missing';end if;
 begin update public.programs set budget=500 where id='91000000-0000-0000-0000-000000000001';raise exception 'Staff changed budget';exception when insufficient_privilege then null;end;
 begin update public.programs set name='Unauthorized' where id='91000000-0000-0000-0000-000000000001';raise exception 'Staff changed name';exception when insufficient_privilege then null;end;
 begin insert into public.program_assignees values('91000000-0000-0000-0000-000000000001',auth.uid());raise exception 'Staff self assigned';exception when insufficient_privilege then null;end;
 begin perform public.save_program(null,'{}');raise exception 'Staff created program through RPC';exception when insufficient_privilege then null;end;
 begin insert into public.program_updates(program_id,status) values('91000000-0000-0000-0000-000000000001','Completed');raise exception 'fake audit accepted';exception when insufficient_privilege then null;end;
 select value into metrics from jsonb_array_elements(public.dashboard_stats()->'staffPerformance') where value->>'id'=auth.uid()::text;
 if (metrics->>'assignedTasks')::int<>2 or (metrics->>'completedTasks')::int<>1 or (metrics->>'overdueTasks')::int<>1 or (metrics->>'updatesLast30Days')::int<>1 or jsonb_array_length(metrics->'programs')<>1 then raise exception 'staff aggregation is wrong or duplicated';end if;
end$$;
-- Identical writes do not inflate contribution counts.
update public.programs set progress=40,progress_notes='Selection underway' where id='91000000-0000-0000-0000-000000000001';
do $$begin if (select count(*) from public.program_updates where actor_id=auth.uid())<>1 then raise exception 'no-op inflated updates';end if;end$$;
set local request.jwt.claim.sub='90000000-0000-0000-0000-000000000001';
delete from public.program_assignees where profile_id='90000000-0000-0000-0000-000000000003';
set local request.jwt.claim.sub='90000000-0000-0000-0000-000000000003';
update public.programs set progress=100 where id='91000000-0000-0000-0000-000000000001';
do $$begin if (select progress from public.programs where id='91000000-0000-0000-0000-000000000001')<>40 then raise exception 'removed PJ retained edit access';end if;end$$;
reset role;
update public.profiles set active=false where id='90000000-0000-0000-0000-000000000003';
set local role authenticated;
do $$begin if exists(select 1 from public.program_assignees) or exists(select 1 from public.program_updates) then raise exception 'inactive user read new tables';end if;end$$;
reset role;
set local role anon;
do $$begin
 begin perform count(*) from public.program_updates;raise exception 'anonymous read audit';exception when insufficient_privilege then null;end;
 begin perform public.save_program(null,'{}');raise exception 'anonymous RPC call';exception when insufficient_privilege then null;end;
end$$;
rollback;
