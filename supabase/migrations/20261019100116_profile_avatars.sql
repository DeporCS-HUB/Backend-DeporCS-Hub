begin;
alter table public.profiles add column avatar_path text;
alter table public.profiles add constraint profile_avatar_owned_path check(avatar_path is null or avatar_path ~ ('^'||id::text||'/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$'));
grant update(avatar_path) on public.profiles to authenticated;
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values('depor-avatars','depor-avatars',false,524288,array['image/jpeg']);
-- Storage serves/updates file bytes; never manipulate storage.objects metadata as an operator.
create policy depor_avatar_read on storage.objects for select to authenticated using(
 bucket_id='depor-avatars' and (select private.app_role()) is not null and
 name ~ ('^'||(select auth.uid())::text||'/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$'));
create policy depor_avatar_insert on storage.objects for insert to authenticated with check(
 bucket_id='depor-avatars' and (select private.app_role()) is not null and
 name ~ ('^'||(select auth.uid())::text||'/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$'));
create policy depor_avatar_update on storage.objects for update to authenticated using(
 bucket_id='depor-avatars' and (select private.app_role()) is not null and
 name ~ ('^'||(select auth.uid())::text||'/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$'))
with check(bucket_id='depor-avatars' and (select private.app_role()) is not null and
 name ~ ('^'||(select auth.uid())::text||'/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$'));
create policy depor_avatar_delete on storage.objects for delete to authenticated using(
 bucket_id='depor-avatars' and (select private.app_role()) is not null and
 name ~ ('^'||(select auth.uid())::text||'/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.jpg$'));
commit;
