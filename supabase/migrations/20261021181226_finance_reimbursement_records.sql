begin;
-- Approval, reimbursement, and a request for funds are separate facts.
alter table public.finances alter column transaction_date drop not null;
alter table public.finances
 add column record_kind text not null default 'transaction' check(record_kind in ('transaction','funding_request','reimbursement_request')),
 add column reimbursement_status text not null default 'unknown' check(reimbursement_status in ('unknown','not_requested','pending','paid','not_required')),
 add column notes text check(length(notes)<=2000),
 add column source_ref text check(length(source_ref)<=300),
 add constraint finance_approved_transaction check(status<>'approved' or (record_kind='transaction' and transaction_date is not null));
grant update(record_kind,reimbursement_status,notes,source_ref) on public.finances to authenticated;
-- Existing BPH-write and program/PJ-read policies also govern the added columns.
commit;
