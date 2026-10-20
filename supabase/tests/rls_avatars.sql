-- Disposable policy harness ONLY, no files are uploaded and fixtures roll back.
begin;
insert into auth.users(id,email) values
 ('97000000-0000-0000-0000-000000000001','owner@example.invalid'),
 ('97000000-0000-0000-0000-000000000002','other@example.invalid');
update public.profiles set active=true where id::text like '97000000-%';
set local role authenticated;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000001';
insert into storage.objects(bucket_id,name) values('depor-avatars','97000000-0000-0000-0000-000000000001/98000000-0000-0000-0000-000000000001.jpg');
update storage.objects set metadata='{"size":100}' where bucket_id='depor-avatars';
update public.profiles set avatar_path='97000000-0000-0000-0000-000000000001/98000000-0000-0000-0000-000000000001.jpg' where id=auth.uid();
do $$begin
 if (select count(*) from storage.objects)<>1 then raise exception 'owner cannot read avatar';end if;
 begin insert into storage.objects(bucket_id,name) values('depor-avatars','97000000-0000-0000-0000-000000000002/98000000-0000-0000-0000-000000000002.jpg');raise exception 'uploaded under another account';exception when insufficient_privilege then null;end;
 begin update storage.objects set name='97000000-0000-0000-0000-000000000002/98000000-0000-0000-0000-000000000002.jpg';raise exception 'moved into another account';exception when insufficient_privilege then null;end;
 begin update public.profiles set avatar_path='97000000-0000-0000-0000-000000000002/98000000-0000-0000-0000-000000000002.jpg' where id=auth.uid();raise exception 'pointed profile at another account';exception when check_violation then null;end;
 begin update public.profiles set avatar_path='https://evil.invalid/tracker.jpg' where id=auth.uid();raise exception 'external photo accepted';exception when check_violation then null;end;
end$$;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000002';
update public.profiles set avatar_path=null where id='97000000-0000-0000-0000-000000000001';
delete from storage.objects;
do $$begin if (select count(*) from storage.objects)<>1 then raise exception 'active teammate cannot read selected avatar';end if;end$$;
reset role;
update public.profiles set role='staff' where id='97000000-0000-0000-0000-000000000002';
set local role authenticated;
do $$begin if (select count(*) from storage.objects)<>1 then raise exception 'BPH cannot read selected team photo';end if;end$$;
set local request.jwt.claim.sub='97000000-0000-0000-0000-000000000001';
do $$begin if (select count(*) from storage.objects)<>1 then raise exception 'other account deleted owner photo';end if;end$$;
reset role;
update public.profiles set active=false where id='97000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$begin
 if exists(select 1 from storage.objects) then raise exception 'inactive owner read photo';end if;
 begin insert into storage.objects(bucket_id,name) values('depor-avatars','97000000-0000-0000-0000-000000000001/98000000-0000-0000-0000-000000000002.jpg');raise exception 'inactive owner uploaded';exception when insufficient_privilege then null;end;
end$$;
reset role;
do $$begin
 if (select public from storage.buckets where id='depor-avatars') then raise exception 'avatar bucket is public';end if;
 if (select file_size_limit from storage.buckets where id='depor-avatars')<>524288 then raise exception 'avatar size not bounded';end if;
end$$;
rollback;
