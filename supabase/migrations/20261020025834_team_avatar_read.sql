begin;
-- Share only the currently selected photo of active members with active department accounts.
-- The bucket stays private. Existing owner-only INSERT/UPDATE/DELETE policies remain unchanged.
create policy depor_avatar_department_read on storage.objects for select to authenticated using (
 bucket_id='depor-avatars' and (select private.app_role()) is not null and
 name in (select p.avatar_path from public.profiles p where p.active and p.avatar_path is not null)
);
commit;
