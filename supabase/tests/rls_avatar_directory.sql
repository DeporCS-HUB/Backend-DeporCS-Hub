-- Disposable stand-in Storage policy harness only; all fixture rows roll back.
begin;
insert into auth.users(id,email) values
 ('96000000-0000-0000-0000-000000000001','photo-owner@example.invalid'),
 ('96000000-0000-0000-0000-000000000002','photo-reader@example.invalid');
update public.profiles set active=true where id::text like '96000000-%';
set local role authenticated;
set local request.jwt.claim.sub='96000000-0000-0000-0000-000000000001';
insert into storage.objects(bucket_id,name) values
 ('depor-avatars','96000000-0000-0000-0000-000000000001/96100000-0000-0000-0000-000000000001.jpg'),
 ('depor-avatars','96000000-0000-0000-0000-000000000001/96100000-0000-0000-0000-000000000002.jpg');
update public.profiles set avatar_path='96000000-0000-0000-0000-000000000001/96100000-0000-0000-0000-000000000001.jpg' where id=auth.uid();
set local request.jwt.claim.sub='96000000-0000-0000-0000-000000000002';
do $$begin
 if (select count(*) from storage.objects)<>1 then raise exception 'teammate read stale photo or cannot read current photo';end if;
 if not exists(select 1 from storage.objects where name like '%000000000001.jpg') then raise exception 'wrong member photo';end if;
 update storage.objects set metadata='{"spoof":true}';
 if found then raise exception 'reader changed another member photo';end if;
 delete from storage.objects;
 if found then raise exception 'reader deleted another member photo';end if;
 update public.profiles set avatar_path=null where id='96000000-0000-0000-0000-000000000001';
 if found then raise exception 'reader changed another member photo pointer';end if;
 begin insert into storage.objects(bucket_id,name) values('depor-avatars','96000000-0000-0000-0000-000000000001/96100000-0000-0000-0000-000000000003.jpg');raise exception 'reader uploaded for someone else';exception when insufficient_privilege then null;end;
end$$;
reset role;
update public.profiles set role='staff' where id='96000000-0000-0000-0000-000000000002';
set local role authenticated;
do $$begin
 if (select count(*) from storage.objects)<>1 then raise exception 'BPH team read failed';end if;
 delete from storage.objects;
 if found then raise exception 'BPH deleted member photo';end if;
end$$;
reset role;
update public.profiles set active=false where id='96000000-0000-0000-0000-000000000002';
set local role authenticated;
do $$begin if exists(select 1 from storage.objects) then raise exception 'inactive reader saw photo';end if;end$$;
reset role;
update public.profiles set active=true where id='96000000-0000-0000-0000-000000000002';
update public.profiles set active=false where id='96000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$begin if exists(select 1 from storage.objects) then raise exception 'inactive member photo was shared';end if;end$$;
reset role;
update public.profiles set active=true,avatar_path=null where id='96000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$begin if exists(select 1 from storage.objects) then raise exception 'removed photo remains shared';end if;end$$;
reset role;
set local role anon;
do $$begin
 begin if exists(select 1 from storage.objects) then raise exception 'anonymous photo read';end if;exception when insufficient_privilege then null;end;
end$$;
reset role;
do $$begin if (select public from storage.buckets where id='depor-avatars') then raise exception 'public bucket';end if;end$$;
rollback;
