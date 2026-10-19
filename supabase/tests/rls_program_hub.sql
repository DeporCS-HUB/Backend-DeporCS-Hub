-- Disposable local/CI only. All real authorization assertions run under caller RLS.
begin;
insert into auth.users(id,email) values
 ('94000000-0000-0000-0000-000000000001','bph@example.invalid'),
 ('94000000-0000-0000-0000-000000000002','pj@example.invalid'),
 ('94000000-0000-0000-0000-000000000003','outsider@example.invalid');
update public.profiles set active=true where id::text like '94000000-%';
update public.profiles set role='staff' where id='94000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub='94000000-0000-0000-0000-000000000001';
do $$declare p jsonb;begin
 p=public.save_program('95000000-0000-0000-0000-000000000001','{}');
 if p is not null then raise exception 'unknown update returned program';end if;
 exception when check_violation then null;
end$$;
do $$declare p jsonb;begin
 p=public.save_program(null,'{"name":"Private program","status":"Ongoing","pic":"Untrusted free text","assignee_ids":["94000000-0000-0000-0000-000000000002"]}');
 if p->>'pic'<>'pj' then raise exception 'PIC did not use account name';end if;
 perform set_config('test.program',p->>'id',true);
 begin perform public.save_program(null,'{"name":"Invalid","status":"Planning","pic":"Text only"}');raise exception 'text-only assignment accepted';exception when check_violation then null;end;
end$$;
insert into public.tasks(title,program_id,assignee_id,due_date) select 'Scoped '||n,current_setting('test.program')::uuid,'94000000-0000-0000-0000-000000000002',(now() at time zone 'Asia/Jakarta')::date-1 from generate_series(1,105)n;
insert into public.tasks(title) values('Unlinked BPH task');
insert into public.events(name,venue,program_id,start_date,end_date) values('Private event','Court',current_setting('test.program')::uuid,current_date,current_date);
insert into public.finances(description,program_id,type,amount,category,transaction_date) values('Private finance',current_setting('test.program')::uuid,'expense',1,'Fixture',current_date);
do $$begin
 begin insert into public.tasks(title,program_id,assignee_id) values('Invalid account',current_setting('test.program')::uuid,'94000000-0000-0000-0000-000000000003');raise exception 'non PJ assigned to closed program';exception when check_violation then null;end;
 if (public.task_summary(jsonb_build_object('program_id',current_setting('test.program')))->>'total')::int<>105 then raise exception 'summary truncated at REST page';end if;
end$$;
set local request.jwt.claim.sub='94000000-0000-0000-0000-000000000002';
insert into public.program_notes(program_id,body) values(current_setting('test.program')::uuid,'Actual update');
insert into public.program_milestones(id,program_id,title) values('96000000-0000-0000-0000-000000000001',current_setting('test.program')::uuid,'Selection');
update public.program_milestones set status='Done' where id='96000000-0000-0000-0000-000000000001';
update public.programs set progress=25 where id=current_setting('test.program')::uuid;
update public.tasks set status='Review' where title='Scoped 1';
do $$declare d jsonb;begin
 if (select count(*) from public.programs)<>1 or (select count(*) from public.tasks)<>105 then raise exception 'PJ workspace inaccessible or unrelated personal task visible';end if;
 if (select status from public.program_milestones where id='96000000-0000-0000-0000-000000000001')<>'Done' then raise exception 'PJ agenda write failed';end if;
 d=public.program_timeline(current_setting('test.program')::uuid,0,1);
 if jsonb_array_length(d->'items')<>1 or not (d->>'hasMore')::boolean then raise exception 'timeline pagination lost updates';end if;
 if (public.task_summary('{"scope":"mine","status":"Review"}')->>'total')::int<>1 then raise exception 'task summary filters differ from lists';end if;
 if jsonb_array_length(public.task_summary('{}')->'workload')<>0 then raise exception 'Staff read department workload';end if;
 begin update public.program_notes set body='Changed';raise exception 'append-only note changed';exception when insufficient_privilege then null;end;
end$$;
set local request.jwt.claim.sub='94000000-0000-0000-0000-000000000003';
do $$begin
 if exists(select 1 from public.programs) or exists(select 1 from public.program_assignees) or exists(select 1 from public.program_updates) or exists(select 1 from public.program_notes) or exists(select 1 from public.program_milestones) or exists(select 1 from public.tasks) or exists(select 1 from public.events) or exists(select 1 from public.finances) then raise exception 'closed program leaked through related table';end if;
 if public.program_timeline(current_setting('test.program')::uuid) is not null then raise exception 'timeline RPC leaked program';end if;
 if (public.task_summary('{}')->>'total')::int<>0 or (public.dashboard_stats()#>>'{summary,totalPrograms}')::int<>0 then raise exception 'aggregate leaked private data';end if;
 begin insert into public.program_notes(program_id,body) values(current_setting('test.program')::uuid,'Intrusion');raise exception 'outsider wrote note';exception when insufficient_privilege then null;end;
 begin insert into public.program_milestones(program_id,title) values(current_setting('test.program')::uuid,'Intrusion');raise exception 'outsider wrote milestone';exception when insufficient_privilege then null;end;
 begin insert into public.tasks(title,program_id) values('Intrusion',current_setting('test.program')::uuid);raise exception 'outsider wrote linked task';exception when insufficient_privilege then null;end;
end$$;
set local request.jwt.claim.sub='94000000-0000-0000-0000-000000000001';
delete from public.program_assignees where program_id=current_setting('test.program')::uuid;
set local request.jwt.claim.sub='94000000-0000-0000-0000-000000000002';
do $$begin if exists(select 1 from public.programs) or exists(select 1 from public.tasks) or exists(select 1 from public.program_notes) or exists(select 1 from public.program_milestones) then raise exception 'removed PJ retained access through owned records';end if;end$$;
reset role;
update public.profiles set active=false where id='94000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub='94000000-0000-0000-0000-000000000001';
do $$begin if private.can_access_program(current_setting('test.program')::uuid) or (public.task_summary('{}')->>'total')::int<>0 then raise exception 'inactive BPH retained access';end if;end$$;
reset role;
set local role anon;
do $$begin
 begin perform public.program_timeline(current_setting('test.program')::uuid);raise exception 'anonymous timeline access';exception when insufficient_privilege then null;end;
 begin perform public.task_summary('{}');raise exception 'anonymous summary access';exception when insufficient_privilege then null;end;
end$$;
rollback;
