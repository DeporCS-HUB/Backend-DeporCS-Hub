-- Disposable local/CI only. No real identities or operational data.
begin;
insert into auth.users(id,email) values
 ('97000000-0000-0000-0000-000000000001','manager@example.invalid'),
 ('97000000-0000-0000-0000-000000000002','pj@example.invalid'),
 ('97000000-0000-0000-0000-000000000003','outsider@example.invalid');
update public.profiles set active=true where id::text like '97000000-%';
update public.profiles set role='staff' where id='97000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000001';
do $$declare p jsonb;begin
 p=public.save_program(null,'{"name":"Fixture program","status":"Ongoing","assignee_ids":["97000000-0000-0000-0000-000000000002"]}');
 perform set_config('test.program',p->>'id',true);
end$$;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000002';
insert into public.program_goals(program_id,title,metric_type,target_value,unit,target_period)
 values(current_setting('test.program')::uuid,'Weekly sessions','count',1,'sesi','Per minggu');
do $$begin
 if (select actual_value from public.program_goals limit 1) is not null then raise exception 'unknown became zero';end if;
 begin update public.program_goals set actual_value=0;raise exception 'unproven actual accepted';exception when check_violation then null;end;
 update public.program_goals set actual_value=0,measurement_period='Week 1',measured_on=current_date,evidence='Verified no session';
 if (select actual_value from public.program_goals limit 1)<>0 then raise exception 'measured zero lost';end if;
 begin update public.program_goals set actual_value=0.5;raise exception 'fractional count accepted';exception when check_violation then null;end;
 begin update public.program_goals set metric_type='percent',target_value=101;raise exception 'invalid percent accepted';exception when check_violation then null;end;
 begin update public.program_goals set program_id=gen_random_uuid();raise exception 'parent changed';exception when insufficient_privilege then null;end;
 begin update public.program_goals set created_by='97000000-0000-0000-0000-000000000001';raise exception 'author spoofed';exception when insufficient_privilege then null;end;
 begin update public.program_goals set source_key='fake';raise exception 'import identity changed';exception when insufficient_privilege then null;end;
end$$;
insert into public.program_milestones(program_id,title,due_date,start_time,end_time,status,schedule_status,description)
 values(current_setting('test.program')::uuid,'Fixture match','2026-10-28','18:30','19:30','Planned','Tentative','Await revised slot');
do $$begin
 begin update public.program_milestones set end_time='17:30';raise exception 'reversed time accepted';exception when check_violation then null;end;
 update public.program_milestones set status='Done';
 if (select start_time from public.program_milestones where title='Fixture match')<>'18:30'::time then raise exception 'status update erased time';end if;
 update public.program_milestones set status='Cancelled';
 begin insert into public.program_notes(program_id,body,source_key) values(current_setting('test.program')::uuid,'Fake import','spoof');raise exception 'import metadata spoofed';exception when insufficient_privilege then null;end;
end$$;
reset role;
insert into public.program_notes(program_id,body,created_by,occurred_on,source_ref,source_key)
 values(current_setting('test.program')::uuid,'Historical report','97000000-0000-0000-0000-000000000001','2026-09-25','Fixture source','fixture-note');
set local role authenticated;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000002';
do $$declare item jsonb;begin
 select value into item from jsonb_array_elements(public.program_timeline(current_setting('test.program')::uuid)->'items') where value->>'kind'='import';
 if item->>'occurred_on'<>'2026-09-25' or item->>'source_ref'<>'Fixture source' or item->>'actor_id'<>'97000000-0000-0000-0000-000000000001' then raise exception 'historical provenance lost';end if;
 if exists(select 1 from public.program_updates where program_id=current_setting('test.program')::uuid) then raise exception 'import fabricated progress audit';end if;
end$$;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000003';
do $$begin
 if exists(select 1 from public.program_goals) then raise exception 'goals leaked';end if;
 begin insert into public.program_goals(program_id,title,metric_type,target_value,unit,target_period) values(current_setting('test.program')::uuid,'Intrusion','count',1,'sesi','Week');raise exception 'outsider wrote goal';exception when insufficient_privilege then null;end;
 update public.program_goals set actual_value=null;
 delete from public.program_goals;
end$$;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000001';
do $$begin if (select count(*) from public.program_goals)<>1 then raise exception 'outsider deleted goal';end if;end$$;
delete from public.program_assignees where program_id=current_setting('test.program')::uuid;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000002';
do $$begin if exists(select 1 from public.program_goals) then raise exception 'removed PJ retained goal access';end if;end$$;
set local role anon;
do $$begin
 begin perform * from public.program_goals;raise exception 'anon read goals';exception when insufficient_privilege then null;end;
end$$;
rollback;
