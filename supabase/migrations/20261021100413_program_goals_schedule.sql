begin;
create table public.program_goals (
 id uuid primary key default gen_random_uuid(),program_id uuid not null references public.programs(id) on delete cascade,
 title text not null check(length(btrim(title)) between 1 and 400),
 metric_type text not null check(metric_type in ('count','percent','boolean')),
 target_value numeric not null check(target_value between 0.01 and 999999999),
 actual_value numeric check(actual_value between 0 and 999999999),
 unit text not null check(length(btrim(unit)) between 1 and 40),
 target_period text not null check(length(btrim(target_period)) between 1 and 120),
 measurement_period text check(length(measurement_period)<=120),measured_on date,
 evidence text check(length(evidence)<=1000),source_ref text check(length(source_ref)<=300),source_key text check(length(source_key)<=160),
 created_by uuid not null default auth.uid() references public.profiles(id),created_at timestamptz not null default now(),updated_at timestamptz not null default now(),
 unique(program_id,source_key),
 check(metric_type<>'percent' or (target_value<=100 and (actual_value is null or actual_value<=100))),
 check(metric_type<>'boolean' or (target_value=1 and (actual_value is null or actual_value in (0,1)))),
 check(metric_type<>'count' or (target_value=trunc(target_value) and (actual_value is null or actual_value=trunc(actual_value)))),
 check(actual_value is null or (measured_on is not null and length(btrim(measurement_period))>0 and measurement_period is not null and length(btrim(evidence))>0 and evidence is not null))
);
create index program_goals_program_order on public.program_goals(program_id,created_at,id);
create index program_goals_creator on public.program_goals(created_by);
alter table public.program_goals enable row level security;
revoke all on public.program_goals from public,anon,authenticated;
grant select,delete on public.program_goals to authenticated;
grant insert(program_id,title,metric_type,target_value,actual_value,unit,target_period,measurement_period,measured_on,evidence,source_ref,created_by) on public.program_goals to authenticated;
grant update(title,metric_type,target_value,actual_value,unit,target_period,measurement_period,measured_on,evidence,source_ref) on public.program_goals to authenticated;
create policy goals_read on public.program_goals for select to authenticated using(private.can_access_program(program_id));
create policy goals_insert on public.program_goals for insert to authenticated with check(private.can_access_program(program_id) and created_by=(select auth.uid()));
create policy goals_update on public.program_goals for update to authenticated using(private.can_access_program(program_id)) with check(private.can_access_program(program_id));
create policy goals_delete on public.program_goals for delete to authenticated using(private.can_access_program(program_id));
create trigger program_goals_updated before update on public.program_goals for each row execute function public.touch_updated_at();

alter table public.program_milestones
 add column start_time time,add column end_time time,add column venue text check(length(venue)<=160),
 add column description text check(length(description)<=2000),
 add column schedule_status text not null default 'Tentative' check(schedule_status in ('Confirmed','Tentative')),
 add column source_ref text check(length(source_ref)<=300),add column source_key text check(length(source_key)<=160),
 add constraint milestone_time_range check((end_time is null or (start_time is not null and end_time>start_time)) and (start_time is null or due_date is not null)),
 add constraint milestone_import_unique unique(program_id,source_key);
alter table public.program_milestones drop constraint program_milestones_status_check;
alter table public.program_milestones add constraint program_milestones_status_check check(status in ('Planned','Done','Cancelled'));
revoke insert on public.program_milestones from authenticated;
grant insert(id,program_id,title,due_date,status,start_time,end_time,venue,description,schedule_status,source_ref,created_by) on public.program_milestones to authenticated;
grant update(start_time,end_time,venue,description,schedule_status,source_ref) on public.program_milestones to authenticated;

-- Imported notes retain their real event date and source. API notes remain authored now.
alter table public.program_notes add column occurred_on date,add column source_ref text check(length(source_ref)<=300),add column source_key text check(length(source_key)<=160),add constraint note_import_unique unique(program_id,source_key);
revoke insert on public.program_notes from authenticated;
grant insert(program_id,body,created_by) on public.program_notes to authenticated;
create or replace function public.program_timeline(p_id uuid,p_page integer default 0,p_size integer default 20) returns jsonb language plpgsql stable security invoker set search_path='' as $$
declare result jsonb;
begin
 if not exists(select 1 from public.programs where id=p_id) then return null;end if;
 if p_page<0 or p_page>100000 or p_size not between 1 and 100 then raise exception 'Invalid pagination' using errcode='23514';end if;
 with activity as (
  select 'progress-'||id::text id,'progress' kind,actor_id,status,progress,notes body,created_at,null::date occurred_on,null::text source_ref,created_at sort_date from public.program_updates where program_id=p_id
  union all select 'note-'||id::text,case when source_key is not null then 'import' else 'note' end,created_by,null,null,body,created_at,occurred_on,source_ref,coalesce(occurred_on::timestamp at time zone 'Asia/Jakarta',created_at) from public.program_notes where program_id=p_id
 ), page as (select * from activity order by sort_date desc,id desc limit p_size+1 offset p_page::bigint*p_size),
 visible as(select * from page order by sort_date desc,id desc limit p_size)
 select jsonb_build_object('items',coalesce((select jsonb_agg(to_jsonb(v)-'sort_date' order by sort_date desc,id desc) from visible v),'[]'::jsonb),'hasMore',(select count(*)>p_size from page)) into result;
 return result;
end $$;
commit;
