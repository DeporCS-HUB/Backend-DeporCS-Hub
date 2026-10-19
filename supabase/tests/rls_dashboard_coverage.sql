-- Disposable database only: no fixtures are persisted.
begin;
insert into auth.users(id,email) values ('92000000-0000-0000-0000-000000000001','coverage@example.invalid');
update public.profiles set active=true,role='staff' where id='92000000-0000-0000-0000-000000000001';
insert into public.programs(id,name,pic,kind,status,progress,created_by)
 select ('93000000-0000-0000-0000-'||lpad(n::text,12,'0'))::uuid,'Coverage '||lpad(n::text,2,'0'),'Fixture','Proker',case when n=35 then 'Completed' else 'Ongoing' end,case when n=35 then 100 when n=1 then 0 else null end,'92000000-0000-0000-0000-000000000001'::uuid from generate_series(1,35)n;
insert into public.program_assignees(program_id,profile_id) values ('93000000-0000-0000-0000-000000000035','92000000-0000-0000-0000-000000000001');
set local role authenticated;
set local request.jwt.claim.sub='92000000-0000-0000-0000-000000000001';
do $$declare d jsonb; s jsonb;begin
 d=public.dashboard_stats();
 if (d#>>'{summary,totalPrograms}')::int<>35 or (d#>>'{summary,recordedPrograms}')::int<>2 or jsonb_array_length(d->'programFocus')<>30 then raise exception 'global coverage uses preview';end if;
 if (select sum((value->>'count')::int) from jsonb_array_elements(d->'programStatusCounts'))<>35 then raise exception 'status counts truncated';end if;

 if not ('prefix (a+)+$ 100%_* suffix' ~* '***=(a+)+$ 100%_*') or ('aaaa 100%' ~* '***=(a+)+$ 100%_*') then raise exception 'literal search interpreted metacharacters';end if;
end$$;
reset role;
update public.profiles set role='member' where id='92000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$declare d jsonb; s jsonb;begin
 d=public.dashboard_stats();
 if (d#>>'{summary,totalPrograms}')::int<>1 or jsonb_array_length(d->'programFocus')<>1 then raise exception 'Staff dashboard leaked other programs';end if;
 select value into s from jsonb_array_elements(d->'staffPerformance') where value->>'id'=auth.uid()::text;
 if (s#>>'{programs,0,status}') is distinct from 'Completed' or (s#>>'{programs,0,progress}')::int is distinct from 100 then raise exception 'off-preview Staff status lost';end if;
end$$;
reset role;
update public.profiles set active=false where id='92000000-0000-0000-0000-000000000001';
set local role authenticated;
do $$begin if (public.dashboard_stats()#>>'{summary,totalPrograms}')::int<>0 then raise exception 'inactive profile saw dashboard records';end if;end$$;
reset role;
set local role anon;
do $$begin begin perform public.dashboard_stats();raise exception 'anonymous dashboard access';exception when insufficient_privilege then null;end;end$$;
rollback;
