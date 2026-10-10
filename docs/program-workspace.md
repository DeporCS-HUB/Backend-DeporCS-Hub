# Program workspace

Programs now distinguish Proker and UKOR and support multiple account-linked PJ records in `program_assignees`. Start/end dates, percentage and budget can be unknown. `Unspecified` means the status has not been supplied. Known next milestones use separate `next_milestone` / `milestone_date` fields, rather than inventing a whole-program date range.

## Access and writes

BPH uses existing POST/PUT `/api/programs` with `kind`, `assignee_ids`, `progress_notes`, `next_milestone` and `milestone_date`. `save_program(uuid,jsonb)` validates active PJ accounts and saves program/assignments atomically under caller RLS. Omitting the PJ list on a compatible older request preserves assignments; an explicit empty list clears them.

PUT `/api/programs/{uuid}/progress` accepts only `status`, optional `progress` (0–100) and optional `progress_notes` (up to 2,000 characters). BPH or an assigned active Staff can use it. The API rejects other fields. RLS checks assignment again and a column guard prohibits Staff changes to names, budgets, dates, identities or PJ assignments even through direct PostgREST.

`program_updates` records actual status/percentage/note changes, including the authenticated actor and time. Clients cannot insert or edit audit rows. No-op saves do not add activity counts. The seeding operator does not manufacture Staff update history. Raw roles remain staff/admin = BPH, member = Staff. Auth verification is unchanged.

## Dashboard measurements

`dashboard_stats()` remains SECURITY INVOKER and retains its existing finance keys. New keys are `overview`, `programFocus`, `upcomingMilestones`, `attentionTasks` and `staffPerformance`. Staff metrics cover account-linked responsibilities, assigned/completed/review/overdue tasks, actual updates over the last 30 days and last update time. Task and assignment aggregates are computed separately to avoid multiplying counts. Deadlines and milestone cutoffs use Asia/Jakarta dates. No attendance or subjective performance score is inferred. Unassigned tasks are not credited to individuals.

## Migration and verification

Migration `20261009173515_program_workspace.sql` was created with the Supabase CLI and applied to the authorized Free development project. The migration RPC did not complete; SQL execution applied the transaction, schema inspection verified it, and matching migration history was recorded without replaying DDL. Do not rerun an already recorded migration. Department data and personal PJ mappings were populated directly in development and are not public test fixtures.

Local PGlite passed all three migrations and three rollback RLS suites. Java verification has 56 passing tests, including seven new program API cases. `rls_program_workspace.sql` checks assigned/unassigned/inactive access, column restrictions, atomic PJ validation, audit integrity and accurate aggregation. Run these fixtures only in a disposable local/CI database, never on the populated hosted Auth project. Hosted permission verification uses simulated JWT claims and rolled-back progress changes; it does not constitute a real Auth login or authenticated browser acceptance test.

The hosted security advisor has no new schema findings. The existing disabled leaked-password protection warning is unchanged; remain on Free. Existing four created-by FK index recommendations and nine older RLS initialization recommendations remain outside this migration. New audit/PJ FKs have covering indexes; unused indexes on fresh tables are retained.
