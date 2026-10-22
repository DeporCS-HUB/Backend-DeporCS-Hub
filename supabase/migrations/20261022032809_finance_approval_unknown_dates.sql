begin;
-- Approval is independent of whether a historical transaction date is known.
-- Undated approved transactions enter the total, but never an invented month.
alter table public.finances drop constraint finance_approved_transaction;
alter table public.finances add constraint finance_approved_transaction
 check(status<>'approved' or record_kind='transaction');
commit;
