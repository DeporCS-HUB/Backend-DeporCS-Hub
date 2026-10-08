-- Disposable development database only. Fixtures roll back.
begin;
insert into auth.users(id,email,raw_user_meta_data) values
 ('00000000-0000-0000-0000-000000000001','member@example.invalid','{"name":"Member"}'),
 ('00000000-0000-0000-0000-000000000002','staff@example.invalid','{"name":"Staff"}'),
 ('00000000-0000-0000-0000-000000000003','other@example.invalid','{"name":"Other"}');
update public.profiles set active=true;
update public.profiles set role='staff' where id='00000000-0000-0000-0000-000000000002';
set local role authenticated;
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000002';
insert into public.programs(id,name,pic,start_date,end_date) values('10000000-0000-0000-0000-000000000001','Program','Staff',current_date,current_date);
insert into public.events(id,name,venue,program_id,start_date,end_date) values('40000000-0000-0000-0000-000000000001','Tournament','SOR','10000000-0000-0000-0000-000000000001',current_date,current_date+1);
update public.events set permit_status='Approved' where id='40000000-0000-0000-0000-000000000001';
do $$begin
 if (select permit_status from public.events limit 1)<>'Approved' then raise exception 'staff event update failed';end if;
 begin insert into public.events(name,venue,start_date,end_date) values('Invalid','SOR',current_date+1,current_date);raise exception 'invalid event dates accepted';exception when check_violation then null;end;
 begin insert into public.events(name,venue,start_date,end_date,status) values('Invalid','SOR',current_date,current_date,'Invented');raise exception 'invalid event status accepted';exception when check_violation then null;end;
 begin delete from public.programs where id='10000000-0000-0000-0000-000000000001';raise exception 'event program FK bypassed';exception when foreign_key_violation or restrict_violation then null;end;
 begin update public.events set created_by='00000000-0000-0000-0000-000000000003';raise exception 'event owner overwritten';exception when insufficient_privilege then null;end;
end$$;
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000001';
update public.profiles set name='Updated Member' where id=auth.uid();
update public.profiles set name='Hijacked' where id='00000000-0000-0000-0000-000000000003';
update public.events set permit_status='Rejected';
delete from public.events;
do $$begin
 if (select name from public.profiles where id=auth.uid())<>'Updated Member' then raise exception 'own name not saved';end if;
 if (select name from public.profiles where id='00000000-0000-0000-0000-000000000003')<>'Other' then raise exception 'other profile changed';end if;
 if (select count(*) from public.events)<>1 or (select permit_status from public.events limit 1)<>'Approved' then raise exception 'member modified event';end if;
 begin update public.profiles set role='admin';raise exception 'role escalation';exception when insufficient_privilege then null;end;
 begin update public.profiles set active=false;raise exception 'activation changed';exception when insufficient_privilege then null;end;
 begin update public.profiles set id='00000000-0000-0000-0000-000000000003';raise exception 'identity changed';exception when insufficient_privilege then null;end;
 begin update public.profiles set name='   ' where id=auth.uid();raise exception 'blank name accepted';exception when check_violation then null;end;
 begin insert into public.events(name,venue,start_date,end_date) values('Unauthorized','SOR',current_date,current_date);raise exception 'member created event';exception when insufficient_privilege then null;end;
end$$;
reset role;
update public.profiles set active=false where id='00000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000001';
update public.profiles set name='Inactive changed' where id=auth.uid();
do $$begin
 if (select count(*) from public.events)<>0 then raise exception 'inactive event access';end if;
 if (select name from public.profiles where id=auth.uid())<>'Updated Member' then raise exception 'inactive profile changed';end if;
end$$;
set local request.jwt.claim.sub='00000000-0000-0000-0000-000000000002';
delete from public.events;
do $$begin if (select count(*) from public.events)<>0 then raise exception 'staff event delete failed';end if;end$$;
reset role;
set local role anon;
do $$begin
 begin perform count(*) from public.events;raise exception 'anonymous events readable';exception when insufficient_privilege then null;end;
 begin update public.profiles set name='Anonymous';raise exception 'anonymous profile changed';exception when insufficient_privilege then null;end;
end$$;
reset role;
rollback;
