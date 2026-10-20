-- Disposable database only. Simulate a pre-migration legacy row without hosted writes.
begin;
insert into auth.users(id,email) values
 ('97000000-0000-0000-0000-000000000001','bph@example.invalid'),
 ('97000000-0000-0000-0000-000000000002','pj@example.invalid'),
 ('97000000-0000-0000-0000-000000000003','outsider@example.invalid');
update public.profiles set active=true where id::text like '97000000-%';
update public.profiles set role='staff' where id='97000000-0000-0000-0000-000000000001';
alter table public.tasks disable trigger task_program_required;
insert into public.tasks(id,title,created_by,assignee_id) values
 ('98000000-0000-0000-0000-000000000001','Legacy task','97000000-0000-0000-0000-000000000001','97000000-0000-0000-0000-000000000002');
alter table public.tasks enable trigger task_program_required;
set local role authenticated;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000001';
do $$declare p jsonb;begin
 p=public.save_program(null,'{"name":"Fixture Proker","kind":"Proker","status":"Planning","assignee_ids":["97000000-0000-0000-0000-000000000002"]}');
 perform set_config('test.task_proker',p->>'id',true);
 p=public.save_program(null,'{"name":"Fixture UKOR","kind":"UKOR","status":"Ongoing","assignee_ids":["97000000-0000-0000-0000-000000000002"]}');
 perform set_config('test.task_ukor',p->>'id',true);
 begin insert into public.tasks(title) values('Missing parent');raise exception 'parentless BPH task accepted';exception when check_violation then null;end;
 if (public.task_board()#>>'{summary,unlinked}')::int<>1 then raise exception 'legacy queue count missing';end if;
end$$;
insert into public.tasks(id,title,program_id,assignee_id) values
 ('98000000-0000-0000-0000-000000000002','Book court',current_setting('test.task_proker')::uuid,'97000000-0000-0000-0000-000000000002');
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000002';
insert into public.tasks(title,program_id,assignee_id) values('Prepare training',current_setting('test.task_ukor')::uuid,auth.uid());
-- A legacy partial status write is still valid until the user chooses its program.
update public.tasks set status='Review' where id='98000000-0000-0000-0000-000000000001';
do $$declare f jsonb;b jsonb;begin
 begin insert into public.tasks(title,assignee_id) values('Missing parent',auth.uid());raise exception 'parentless Staff task accepted';exception when check_violation then null;end;
 begin update public.tasks set program_id=null where id='98000000-0000-0000-0000-000000000002';raise exception 'linked task detached';exception when check_violation then null;end;
 for f in select value from jsonb_array_elements('[{"program_scope":"unlinked"},{"program_scope":"linked"},{"program_scope":"unlinked","scope":"mine"}]') loop
  b=public.task_board(f);if b->'summary'<>public.task_summary(f) then raise exception 'board/list summary mismatch';end if;
 end loop;
 if (public.task_summary('{"program_scope":"linked"}')->>'total')::int<>2 or (public.task_summary('{"program_scope":"unlinked"}')->>'total')::int<>1 then raise exception 'parent filter mismatch';end if;
 if jsonb_array_length(public.task_summary('{}')->'workload')<>0 then raise exception 'Staff received BPH workload';end if;
 if not exists(select 1 from jsonb_array_elements(public.workspace_directory()->'programs') p where p->>'kind'='UKOR') then raise exception 'directory kind missing';end if;
end$$;
-- Relink preserves title, status and assignment; no guessed program or duplicate row.
update public.tasks set program_id=current_setting('test.task_proker')::uuid where id='98000000-0000-0000-0000-000000000001';
do $$begin
 if (public.task_summary('{"program_scope":"unlinked"}')->>'total')::int<>0 then raise exception 'relinked task stayed in queue';end if;
 if not exists(select 1 from public.tasks where id='98000000-0000-0000-0000-000000000001' and title='Legacy task' and status='Review' and assignee_id=auth.uid()) then raise exception 'relink lost data';end if;
end$$;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000003';
do $$begin
 if exists(select 1 from public.tasks) or (public.task_board()#>>'{summary,total}')::int<>0 then raise exception 'unrelated Staff accessed program tasks';end if;
 begin insert into public.tasks(title,program_id) values('Intrusion',current_setting('test.task_ukor')::uuid);raise exception 'unrelated Staff created program task';exception when insufficient_privilege then null;end;
end$$;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000001';
delete from public.program_assignees where program_id=current_setting('test.task_proker')::uuid;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000002';
do $$begin if exists(select 1 from public.tasks where program_id=current_setting('test.task_proker')::uuid) then raise exception 'removed PJ retained linked tasks';end if;end$$;
reset role;
rollback;
