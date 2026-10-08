-- Run only against a disposable DEVELOPMENT database. Every fixture rolls back.
begin;
do $$begin
 if exists(select 1 from auth.users) or exists(select 1 from public.profiles)
 or exists(select 1 from public.programs) or exists(select 1 from public.tasks)
 or exists(select 1 from public.finances) or exists(select 1 from public.inventory)
 or exists(select 1 from public.events) then
  raise exception 'RLS acceptance requires empty development database';
 end if;
end$$;
insert into auth.users(id,email,raw_user_meta_data) values
 ('00000000-0000-0000-0000-000000000001','member@example.invalid','{"role":"admin","active":true,"name":"Member"}'),
 ('00000000-0000-0000-0000-000000000002','staff@example.invalid','{"name":"Staff"}'),
 ('00000000-0000-0000-0000-000000000003','other@example.invalid','{"name":"Other"}'),
 ('00000000-0000-0000-0000-000000000004','fallback@example.invalid','{"name":"   "}');
do $$begin if (select name from public.profiles where id='00000000-0000-0000-0000-000000000004')<>'fallback' then raise exception 'blank metadata name not normalized';end if;end$$;
do $$begin if exists(select 1 from public.profiles where active) then raise exception 'unapproved account activated automatically';end if;end$$;
update public.profiles set active=true where id in ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000003');
update public.profiles set role='staff' where id='00000000-0000-0000-0000-000000000002';
-- Metadata cannot grant admin.
do $$begin if (select role from public.profiles where id='00000000-0000-0000-0000-000000000001')<>'member' then raise exception 'metadata privilege escalation';end if;end$$;
set local role authenticated;
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000002';
insert into public.programs(id,name,pic,start_date,end_date,budget) values('10000000-0000-0000-0000-000000000001','Development Program','Staff',current_date,current_date+7,1000000);
insert into public.finances(id,description,program_id,type,amount,category,transaction_date,status) values
 ('20000000-0000-0000-0000-000000000001','Expense','10000000-0000-0000-0000-000000000001','expense',100000,'Equipment',current_date,'approved'),
 ('20000000-0000-0000-0000-000000000002','Pending Expense',null,'expense',500000,'Equipment',current_date,'pending'),
 ('20000000-0000-0000-0000-000000000003','Income',null,'income',300000,'Sponsor',current_date,'approved');
insert into public.inventory(name,category,quantity,status,condition) values('Ball','Sports',3,'Available','Baik');
insert into public.tasks(id,title,status,assignee_id) values('30000000-0000-0000-0000-000000000001','Member assigned task','To Do','00000000-0000-0000-0000-000000000001');
-- More than REST's typical 1000-row limit: aggregation must not truncate.
insert into public.tasks(title,status) select 'Development task '||g,'Backlog' from generate_series(1,1001) g;
update public.inventory set quantity=4 where name='Ball';
do $$declare stats jsonb;begin
 stats=public.dashboard_stats();
 if (stats->'summary'->>'pendingTasks')::integer<>1002 then raise exception 'truncated task aggregate';end if;
 if (stats->'summary'->>'expense')::numeric<>100000 then raise exception 'unapproved expense counted';end if;
 if (stats->'summary'->>'income')::numeric<>300000 then raise exception 'income aggregate incorrect';end if;
 if (stats->'summary'->>'remaining')::numeric<>900000 then raise exception 'remaining aggregate incorrect';end if;
 if (stats->'summary'->>'totalAssets')::integer<>4 then raise exception 'inventory aggregate incorrect';end if;
end$$;
-- Staff CRUD and foreign-key protection.
do $$begin
 begin delete from public.programs where id='10000000-0000-0000-0000-000000000001';raise exception 'program deletion bypassed FK';exception when foreign_key_violation or restrict_violation then null;end;
 begin insert into public.inventory(name,category,quantity,status,condition) values('Invalid','Sports',-1,'Available','Baik');raise exception 'negative quantity accepted';exception when check_violation then null;end;
 begin insert into public.programs(name,pic,start_date,end_date) values('Bad dates','Staff',current_date+7,current_date);raise exception 'bad dates accepted';exception when check_violation then null;end;
end$$;
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000001';
do $$begin
 if private.app_role()<>'member' then raise exception 'wrong member role';end if;
 if (select count(*) from public.programs)<>1 then raise exception 'member cannot read programs';end if;
 begin update public.profiles set role='admin' where id=auth.uid();raise exception 'member changed role';exception when insufficient_privilege then null;end;
 begin insert into public.programs(name,pic,start_date,end_date) values('Unauthorized','Member',current_date,current_date);raise exception 'member created program';exception when insufficient_privilege then null;end;
 begin insert into public.finances(description,type,amount,category,transaction_date) values('Unauthorized','expense',1,'Sports',current_date);raise exception 'member created finance';exception when insufficient_privilege then null;end;
 begin insert into public.inventory(name,category,quantity,status,condition) values('Unauthorized','Sports',1,'Available','Baik');raise exception 'member created inventory';exception when insufficient_privilege then null;end;
 begin insert into public.tasks(title,assignee_id) values('Unauthorized assignment','00000000-0000-0000-0000-000000000003');raise exception 'member assigned another user';exception when insufficient_privilege then null;end;
 begin update public.tasks set created_by=auth.uid() where id='30000000-0000-0000-0000-000000000001';raise exception 'creator overwritten';exception when insufficient_privilege then null;end;
end$$;
update public.tasks set status='Done' where id='30000000-0000-0000-0000-000000000001';
insert into public.tasks(id,title,assignee_id) values('30000000-0000-0000-0000-000000000002','Own task',auth.uid());
update public.tasks set status='Review' where id='30000000-0000-0000-0000-000000000002';
do $$begin if (select status from public.tasks where id='30000000-0000-0000-0000-000000000001')<>'Done' then raise exception 'assigned task cannot update';end if;end$$;
delete from public.tasks where id='30000000-0000-0000-0000-000000000002';
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000003';
update public.tasks set status='Backlog' where id='30000000-0000-0000-0000-000000000001';
do $$begin if (select status from public.tasks where id='30000000-0000-0000-0000-000000000001')<>'Done' then raise exception 'foreign task edited';end if;end$$;
reset role;
update public.profiles set active=false where id='00000000-0000-0000-0000-000000000003';
set local role authenticated;
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000003';
do $$begin if (select count(*) from public.programs)<>0 then raise exception 'inactive member read data';end if;end$$;
reset role;
set local role anon;
do $$begin
 begin perform count(*) from public.programs;raise exception 'anonymous data access';exception when insufficient_privilege then null;end;
 begin perform public.dashboard_stats();raise exception 'anonymous aggregate access';exception when insufficient_privilege then null;end;
end$$;
reset role;
rollback;
