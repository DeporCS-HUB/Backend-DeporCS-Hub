begin;
-- Additive migration; apply after 20261008154150_core.sql in development first.
alter table public.profiles add constraint profile_name_not_blank check (length(btrim(name)) > 0);
grant update(name) on public.profiles to authenticated;
create policy profiles_update_self on public.profiles for update to authenticated
 using (id=auth.uid() and private.app_role() is not null)
 with check (id=auth.uid() and private.app_role() is not null);

create table public.events (
 id uuid primary key default gen_random_uuid(),
 name text not null check (length(btrim(name)) between 1 and 160),
 description text check (length(description)<=2000),
 program_id uuid references public.programs(id) on delete restrict,
 venue text not null check (length(btrim(venue)) between 1 and 160),
 start_date date not null, end_date date not null check (end_date>=start_date),
 status text not null default 'Planning' check (status in ('Planning','Confirmed','Completed','Cancelled')),
 permit_status text not null default 'Pending' check (permit_status in ('Not required','Pending','Approved','Rejected')),
 created_by uuid not null default auth.uid() references public.profiles(id),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create index events_program on public.events(program_id);
create index events_start on public.events(start_date,id);
create trigger event_updated before update on public.events for each row execute function public.touch_updated_at();
alter table public.events enable row level security;
revoke all on public.events from anon,authenticated;
grant select,insert,delete on public.events to authenticated;
grant update(name,description,program_id,venue,start_date,end_date,status,permit_status) on public.events to authenticated;
create policy events_read on public.events for select to authenticated using (private.app_role() is not null);
create policy events_insert on public.events for insert to authenticated
 with check (private.app_role() in ('staff','admin') and created_by=auth.uid());
create policy events_update on public.events for update to authenticated
 using (private.app_role() in ('staff','admin')) with check (private.app_role() in ('staff','admin'));
create policy events_delete on public.events for delete to authenticated using (private.app_role() in ('staff','admin'));
commit;
