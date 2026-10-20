-- Disposable only. Verify batching has exactly the existing row/metric boundaries.
begin;
insert into auth.users(id,email) values
 ('95000000-0000-0000-0000-000000000001','perf-bph@example.invalid'),
 ('95000000-0000-0000-0000-000000000002','perf-pj@example.invalid'),
 ('95000000-0000-0000-0000-000000000003','perf-other@example.invalid');
update public.profiles set active=true where id::text like '95000000-%';
update public.profiles set role='staff' where id='95000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub='95000000-0000-0000-0000-000000000001';
do $$declare p jsonb;begin
 p=public.save_program(null,'{"name":"Performance fixture","status":"Ongoing","progress":30,"description":"Excluded from directory","progress_notes":"Excluded from overview","assignee_ids":["95000000-0000-0000-0000-000000000002"]}');
 perform set_config('test.perf_program',p->>'id',true);
end$$;
insert into public.tasks(title,description,program_id,assignee_id,status,priority,due_date)
 select 'Fixture '||n,'Needle fixture',current_setting('test.perf_program')::uuid,'95000000-0000-0000-0000-000000000002',
 (array['Backlog','To Do','In Progress','Review','Done'])[1+(n%5)],case when n%2=0 then 'High' else 'Low' end,current_date-1 from generate_series(1,100)n;
-- Simulate an existing legacy row; privileged setup only, never hosted.
reset role;
alter table public.tasks disable trigger task_program_required;
insert into public.tasks(title) values('BPH personal');
alter table public.tasks enable trigger task_program_required;
set local role authenticated;
do $$declare board jsonb; later jsonb; directory jsonb; compact jsonb; full_data jsonb; filters jsonb;begin
 board=public.task_board();
 if board#>'{columns,To Do,items,0}'?'description' then raise exception 'board overfetches details';end if;
 if board->'summary'<>public.task_summary() then raise exception 'board summary differs';end if;
 if jsonb_array_length(board#>'{columns,To Do,items}')<>12 or not (board#>>'{columns,To Do,hasMore}')::boolean then raise exception 'board pagination not bounded';end if;
 later=public.task_board('{}',array[0,1,0,0,0]);
 if jsonb_array_length(later#>'{columns,To Do,items}')<>8 or (later#>>'{columns,To Do,hasMore}')::boolean then raise exception 'last page incorrect';end if;
 if later#>'{columns,Done}'<>board#>'{columns,Done}' then raise exception 'changing one page changed another column';end if;
 foreach filters in array array['{"q":"Needle","priority":"High"}'::jsonb,'{"scope":"overdue"}'::jsonb,'{"scope":"unassigned"}'::jsonb,'{"scope":"mine"}'::jsonb,'{"status":"Done"}'::jsonb] loop
  if public.task_board(filters)->'summary'<>public.task_summary(filters) then raise exception 'filtered board differs';end if;
 end loop;
 begin perform public.task_board('{}',array[-1,0,0,0,0]);raise exception 'negative page accepted';exception when check_violation then null;end;
 begin perform public.task_board('{}',null);raise exception 'null pages accepted';exception when check_violation then null;end;
 directory=public.workspace_directory();
 if jsonb_array_length(directory->'programs')<>1 or directory#>>'{programs,0,assignee_ids,0}'<>'95000000-0000-0000-0000-000000000002' then raise exception 'directory lost assignment';end if;
 if directory#>'{programs,0}'?'description' or directory#>'{profiles,0}'?'email' then raise exception 'directory overfetches fields';end if;
 compact=public.dashboard_overview();full_data=public.dashboard_stats();
 if compact->'overview'<>full_data->'overview' then raise exception 'overview counts changed';end if;
 if compact#>'{summary,totalPrograms}'<>full_data#>'{summary,totalPrograms}' or compact#>'{summary,recordedPrograms}'<>full_data#>'{summary,recordedPrograms}' or compact#>'{summary,completedTasks}'<>full_data#>'{summary,completedTasks}' then raise exception 'dashboard totals changed';end if;
 if compact#>'{programFocus,0}'?'progress_notes' or compact?'monthly' then raise exception 'chart endpoint overfetch';end if;
end$$;
set local request.jwt.claim.sub='95000000-0000-0000-0000-000000000002';
do $$begin
 if public.task_board()->'summary'<>public.task_summary() or (public.task_board()#>>'{summary,total}')::int<>100 then raise exception 'PJ board visibility changed';end if;
 if public.task_board()#>'{summary,workload}'<>'[]'::jsonb then raise exception 'Staff workload leak';end if;
 if jsonb_array_length(public.workspace_directory()->'programs')<>1 then raise exception 'PJ directory inaccessible';end if;
 if public.dashboard_overview()->'overview'<>public.dashboard_stats()->'overview' then raise exception 'Staff overview differs';end if;
end$$;
set local request.jwt.claim.sub='95000000-0000-0000-0000-000000000003';
do $$begin
 if (public.task_board()#>>'{summary,total}')::int<>0 or jsonb_array_length(public.workspace_directory()->'programs')<>0 or (public.dashboard_overview()#>>'{summary,totalPrograms}')::int<>0 then raise exception 'unrelated Staff saw private workspace';end if;
end$$;
reset role;
update public.profiles set active=false where id='95000000-0000-0000-0000-000000000003';
set local role authenticated;
do $$begin
 if (public.task_board()#>>'{summary,total}')::int<>0 or jsonb_array_length(public.workspace_directory()->'profiles')<>0 then raise exception 'inactive user received directory/work';end if;
end$$;
reset role;
set local role anon;
do $$begin
 begin perform public.task_board();raise exception 'anonymous board';exception when insufficient_privilege then null;end;
 begin perform public.workspace_directory();raise exception 'anonymous directory';exception when insufficient_privilege then null;end;
 begin perform public.dashboard_overview();raise exception 'anonymous overview';exception when insufficient_privilege then null;end;
end$$;
reset role;
rollback;
