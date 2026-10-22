-- Disposable local/CI only. Fixtures and rollback rehearsal all roll back.
begin;
insert into auth.users(id,email) values
 ('98000000-0000-0000-0000-000000000001','finance-manager@example.invalid'),
 ('98000000-0000-0000-0000-000000000002','finance-pj@example.invalid'),
 ('98000000-0000-0000-0000-000000000003','finance-outsider@example.invalid');
update public.profiles set active=true where id::text like '98000000-%';
update public.profiles set role='staff' where id='98000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub='98000000-0000-0000-0000-000000000001';
do $$declare p jsonb;begin
 p=public.save_program(null,'{"name":"Finance fixture","status":"Planning","assignee_ids":["98000000-0000-0000-0000-000000000002"]}');
 perform set_config('test.finance_program',p->>'id',true);
end$$;
insert into public.finances(description,program_id,type,amount,category,status,record_kind,reimbursement_status,notes,source_ref)
 values('Fixture request',current_setting('test.finance_program')::uuid,'expense',12345,'Equipment','pending','reimbursement_request','pending','Awaiting receipt','Fixture source');
do $$begin
 if (select transaction_date from public.finances where description='Fixture request') is not null then raise exception 'unknown date fabricated';end if;
 begin update public.finances set status='approved';raise exception 'request counted as actual';exception when check_violation then null;end;
 update public.finances set reimbursement_status='paid';
 if (public.dashboard_stats()->'summary'->>'expense')::numeric<>0 then raise exception 'reimburse implied approval';end if;
 update public.finances set record_kind='transaction';
 update public.finances set status='approved';
 if (select transaction_date from public.finances) is not null then raise exception 'approval fabricated date';end if;
 if (public.dashboard_stats()->'summary'->>'expense')::numeric<>12345 then raise exception 'undated approved actual missing';end if;
 if exists(select 1 from jsonb_array_elements(public.dashboard_stats()->'monthly') m where (m->>'expense')::numeric<>0) then raise exception 'undated actual assigned to invented month';end if;
 update public.finances set transaction_date=current_date;
 if (public.dashboard_stats()->'summary'->>'expense')::numeric<>12345 then raise exception 'approved transaction missing';end if;
end$$;
set local request.jwt.claim.sub='98000000-0000-0000-0000-000000000002';
do $$begin
 if (select count(*) from public.finances)<>1 then raise exception 'PJ cannot read finance';end if;
 update public.finances set reimbursement_status='unknown';
 if (select reimbursement_status from public.finances)<>'paid' then raise exception 'PJ modified finance';end if;
end$$;
set local request.jwt.claim.sub='98000000-0000-0000-0000-000000000003';
do $$begin if exists(select 1 from public.finances) then raise exception 'finance leaked';end if;end$$;
set local role anon;
do $$begin begin perform * from public.finances;raise exception 'anon read finance';exception when insufficient_privilege then null;end;end$$;
reset role;
-- Rehearse the prior schema only after removing fixture-only data.
delete from public.finances where program_id=current_setting('test.finance_program')::uuid;
alter table public.finances drop constraint finance_approved_transaction;
-- Restore the stricter previous constraint as a rollback rehearsal.
alter table public.finances add constraint finance_approved_transaction check(status<>'approved' or (record_kind='transaction' and transaction_date is not null));
alter table public.finances drop constraint finance_approved_transaction,
 drop column record_kind,drop column reimbursement_status,drop column notes,drop column source_ref,
 alter column transaction_date set not null;
do $$begin if exists(select 1 from information_schema.columns where table_schema='public' and table_name='finances' and column_name='record_kind') then raise exception 'rollback failed';end if;end$$;
rollback;
