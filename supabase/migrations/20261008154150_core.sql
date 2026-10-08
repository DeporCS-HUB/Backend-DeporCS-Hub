begin;
create schema private;
revoke all on schema private from public, anon, authenticated;
grant usage on schema private to authenticated;
-- Intentionally fail on conflicting existing tables; inspect and reconcile before applying.
-- auth.users is managed by Supabase Auth; never create a public password table.
create table public.profiles (
 id uuid primary key references auth.users(id) on delete cascade,
 name text not null check (length(name) between 1 and 120),
 role text not null default 'member' check (role in ('member','staff','admin')),
 active boolean not null default false,
 created_at timestamptz not null default now()
);
create function private.bootstrap_profile() returns trigger language plpgsql security definer set search_path = '' as $$
begin
 insert into public.profiles(id,name) values(new.id,left(coalesce(nullif(btrim(new.raw_user_meta_data->>'name'),''),split_part(new.email,'@',1),'Member'),120));
 return new;
end $$;
revoke all on function private.bootstrap_profile() from public;
create trigger depor_auth_profile after insert on auth.users for each row execute function private.bootstrap_profile();
-- Existing auth accounts receive inactive member profiles; no role from user metadata is trusted.
insert into public.profiles(id,name)
select id,left(coalesce(nullif(btrim(raw_user_meta_data->>'name'),''),split_part(email,'@',1),'Member'),120) from auth.users;
create function private.app_role() returns text language sql stable security definer set search_path = '' as $$
 select role from public.profiles where id=auth.uid() and active;
$$;
revoke all on function private.app_role() from public;
grant execute on function private.app_role() to authenticated;
create table public.programs (
 id uuid primary key default gen_random_uuid(),
 name text not null check(length(name) between 1 and 160), description text check(length(description)<=2000),
 pic text not null check(length(pic) between 1 and 120), pic_id uuid references public.profiles(id) on delete set null,
 start_date date not null, end_date date not null check(end_date>=start_date),
 status text not null default 'Planning' check(status in ('Planning','Ongoing','Active','Completed','Cancelled')),
 progress integer not null default 0 check(progress between 0 and 100),
 budget numeric(15,2) not null default 0 check(budget>=0),
 created_by uuid not null default auth.uid() references public.profiles(id),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table public.tasks (
 id uuid primary key default gen_random_uuid(), title text not null check(length(title) between 1 and 160),
 description text check(length(description)<=2000), program_id uuid references public.programs(id) on delete restrict,
 assignee_id uuid references public.profiles(id) on delete set null,
 status text not null default 'Backlog' check(status in ('Backlog','To Do','In Progress','Review','Done')),
 priority text not null default 'Medium' check(priority in ('Low','Medium','High')), due_date date,
 created_by uuid not null default auth.uid() references public.profiles(id),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table public.finances (
 id uuid primary key default gen_random_uuid(), description text not null check(length(description) between 1 and 200),
 program_id uuid references public.programs(id) on delete restrict,
 type text not null check(type in ('income','expense')), amount numeric(15,2) not null check(amount>0),
 category text not null check(length(category) between 1 and 80), transaction_date date not null,
 status text not null default 'pending' check(status in ('pending','approved','rejected')),
 created_by uuid not null default auth.uid() references public.profiles(id),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create table public.inventory (
 id uuid primary key default gen_random_uuid(), name text not null check(length(name) between 1 and 160),
 category text not null check(length(category) between 1 and 80), quantity integer not null check(quantity between 0 and 1000000),
 status text not null default 'Available' check(status in ('Available','Borrowed','Maintenance','Lost')),
 condition text not null default 'Baik' check(condition in ('Baik','Perawatan','Rusak')),
 emoji text check(length(emoji)<=16), location text check(length(location)<=160),
 created_by uuid not null default auth.uid() references public.profiles(id),
 created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create index tasks_assignee on public.tasks(assignee_id);
create index tasks_program on public.tasks(program_id);
create index tasks_creator on public.tasks(created_by);
create index finances_program on public.finances(program_id);
create index finances_date on public.finances(transaction_date);
create index programs_pic on public.programs(pic_id);
create index programs_start on public.programs(start_date);
create function public.touch_updated_at() returns trigger language plpgsql set search_path = '' as $$
begin new.updated_at=now();return new;end $$;
revoke all on function public.touch_updated_at() from public;
create trigger program_updated before update on public.programs for each row execute function public.touch_updated_at();
create trigger task_updated before update on public.tasks for each row execute function public.touch_updated_at();
create trigger finance_updated before update on public.finances for each row execute function public.touch_updated_at();
create trigger inventory_updated before update on public.inventory for each row execute function public.touch_updated_at();

alter table public.profiles enable row level security;
alter table public.programs enable row level security;
alter table public.tasks enable row level security;
alter table public.finances enable row level security;
alter table public.inventory enable row level security;
revoke all on public.profiles,public.programs,public.tasks,public.finances,public.inventory from anon,authenticated;
grant select on public.profiles,public.programs,public.tasks,public.finances,public.inventory to authenticated;
-- Role and active flags can only be changed by a trusted database operator/service role.
create policy profiles_read on public.profiles for select to authenticated using(id=auth.uid() or private.app_role() is not null);
create policy programs_read on public.programs for select to authenticated using(private.app_role() is not null);
create policy finances_read on public.finances for select to authenticated using(private.app_role() is not null);
create policy inventory_read on public.inventory for select to authenticated using(private.app_role() is not null);
create policy tasks_read on public.tasks for select to authenticated using(private.app_role() is not null);
grant insert,delete on public.programs,public.finances,public.inventory,public.tasks to authenticated;
grant update(name,description,pic,pic_id,start_date,end_date,status,progress,budget) on public.programs to authenticated;
grant update(description,program_id,type,amount,category,transaction_date,status) on public.finances to authenticated;
grant update(name,category,quantity,status,condition,emoji,location) on public.inventory to authenticated;
grant update(title,description,program_id,assignee_id,status,priority,due_date) on public.tasks to authenticated;
create policy programs_insert on public.programs for insert to authenticated with check(private.app_role() in ('staff','admin') and created_by=auth.uid());
create policy programs_update on public.programs for update to authenticated using(private.app_role() in ('staff','admin')) with check(private.app_role() in ('staff','admin'));
create policy programs_delete on public.programs for delete to authenticated using(private.app_role() in ('staff','admin'));
create policy finances_insert on public.finances for insert to authenticated with check(private.app_role() in ('staff','admin') and created_by=auth.uid());
create policy finances_update on public.finances for update to authenticated using(private.app_role() in ('staff','admin')) with check(private.app_role() in ('staff','admin'));
create policy finances_delete on public.finances for delete to authenticated using(private.app_role() in ('staff','admin'));
create policy inventory_insert on public.inventory for insert to authenticated with check(private.app_role() in ('staff','admin') and created_by=auth.uid());
create policy inventory_update on public.inventory for update to authenticated using(private.app_role() in ('staff','admin')) with check(private.app_role() in ('staff','admin'));
create policy inventory_delete on public.inventory for delete to authenticated using(private.app_role() in ('staff','admin'));
create policy tasks_insert on public.tasks for insert to authenticated with check(created_by=auth.uid() and (private.app_role() in ('staff','admin') or (private.app_role()='member' and (assignee_id is null or assignee_id=auth.uid()))));
create policy tasks_update on public.tasks for update to authenticated
using(private.app_role() in ('staff','admin') or (private.app_role()='member' and (created_by=auth.uid() or assignee_id=auth.uid())))
with check(private.app_role() in ('staff','admin') or (private.app_role()='member' and (created_by=auth.uid() or assignee_id=auth.uid()) and (assignee_id is null or assignee_id=auth.uid())));
create policy tasks_delete on public.tasks for delete to authenticated using(private.app_role() in ('staff','admin') or (private.app_role()='member' and (created_by=auth.uid() or assignee_id=auth.uid())));

-- SECURITY INVOKER preserves the caller's RLS; totals are calculated by Postgres, without REST pagination truncation.
create function public.dashboard_stats() returns jsonb language sql stable security invoker set search_path = '' as $$
 with p as (select count(*) total_programs,count(*) filter(where status in ('Active','Ongoing')) active_programs,count(*) filter(where status='Completed') completed_programs,coalesce(sum(budget),0) budget from public.programs),
 t as (select count(*) filter(where status<>'Done') pending_tasks,count(*) filter(where status='Done') completed_tasks from public.tasks),
 f as (select coalesce(sum(amount) filter(where type='expense' and status='approved'),0) expense,coalesce(sum(amount) filter(where type='income' and status='approved'),0) income from public.finances),
 i as (select coalesce(sum(quantity),0) total_assets from public.inventory),
 months as (select generate_series(1,12) m),
 monthly as (select m,coalesce(sum(amount) filter(where type='income'),0) income,coalesce(sum(amount) filter(where type='expense'),0) expense from months left join public.finances on extract(month from transaction_date)=m and extract(year from transaction_date)=extract(year from current_date) and status='approved' group by m),
 assets as (select status,coalesce(sum(quantity),0) quantity from public.inventory group by status)
 select jsonb_build_object(
 'summary',jsonb_build_object('totalPrograms',p.total_programs,'activePrograms',p.active_programs,'completedPrograms',p.completed_programs,'pendingTasks',t.pending_tasks,'completedTasks',t.completed_tasks,'budget',p.budget,'expense',f.expense,'income',f.income,'remaining',p.budget-f.expense,'budgetUtilization',case when p.budget>0 then round(f.expense/p.budget*100,1) else 0 end,'totalAssets',i.total_assets),
 'monthly',(select jsonb_agg(jsonb_build_object('month',m,'income',income,'expense',expense) order by m) from monthly),
 'inventory',coalesce((select jsonb_agg(jsonb_build_object('status',status,'quantity',quantity)) from assets),'[]'::jsonb),
 'upcomingPrograms',coalesce((select jsonb_agg(to_jsonb(upcoming)) from (select id,name,start_date,end_date,status from public.programs where end_date>=current_date and status not in ('Cancelled','Completed') order by start_date limit 5) upcoming),'[]'::jsonb)
 ) from p,t,f,i;
$$;
revoke all on function public.dashboard_stats() from public;
grant execute on function public.dashboard_stats() to authenticated;
commit;
