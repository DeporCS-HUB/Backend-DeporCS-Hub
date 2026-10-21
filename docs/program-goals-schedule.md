# Program goals, evidence and scheduled agenda

Each private program has paginated goals, an agenda and an append-only timeline. BPH and assigned PJ use their own caller JWT; removal of an assignment revokes child access through existing program RLS. No extra service, SDK, polling or paid resource is introduced.

## Goals

`GET /api/programs/:id/goals?page=0&size=20` returns `{data: [...], meta: {hasMore}}`. POST on the collection creates; PUT and DELETE on `/:goalId` edit/remove. Program and creator are server-owned. Writes cannot supply import identities, timestamps or another parent.

A goal contains `title`, `metric_type` (`count`, `percent`, `boolean`), positive `target_value`, optional `actual_value`, `unit`, `target_period`, optional `measurement_period`, `measured_on`, `evidence` and `source_ref`. Counts are whole numbers; percentages are 0–100; boolean target is 1 and actual is 0/1. Null actual means unmeasured, while 0 is an observed zero. Every non-null actual requires a nonblank measurement period, date and evidence. The source target cadence and the actual reporting window are separate. Recurring indicators are manual snapshots for the named period, not claims that all weeks/months met target. Updating one replaces its snapshot; historical measurement-series charts are not implemented.

Target comparisons do not change program progress, calculate an overall percentage, or create Staff performance scores. Program percentages already entered by users remain intact. More actual than target is displayed numerically; the visual bar stops at 100%. Evidence/source text is collapsed by default.

## Agenda

Existing milestone endpoints accept optional `start_time`, `end_time`, `venue`, `description`, `schedule_status` (`Tentative`/`Confirmed`) and `source_ref`. Times are local Asia/Jakarta wall-clock times and render WIB. A start time requires a date; an end time requires a later start on the same day. Overnight agenda requires a future contract change. Status supports `Planned`, `Done`, `Cancelled`. Legacy callers without new fields remain accepted; omitted certainty defaults to Tentative. Full PUT may clear omitted optional fields, so the frontend explicitly preserves all editable fields on status toggle. Update grants still prohibit moving a milestone to another program or changing its creator/import identity.

A revised start applies only to the explicitly affected start/cabangs. Do not cascade later matches or invent new slots. Ambiguous or overlapping slots remain Tentative with the source time and explanation. Source-cell ranges can identify scheduled block boundaries, which are allocations rather than actual match duration. A schedule is never evidence of a completed activity.

## Historical evidence

Maintenance imports use `program_notes.occurred_on/source_ref/source_key`. The existing invoker timeline RPC labels these `kind: import`, exposes actual recording time plus optional reported-event date, and keeps the real recording actor. Regular API notes cannot supply import fields. The UI labels reconstruction separately from live progress updates. Range/unknown event dates remain null and are described in the body. Imports do not backdate `created_at` or fabricate `program_updates`, preserving the 30-day Staff metric.

Imported source keys are unique per program. Apply private operational input outside this public repository, insert-only on conflict; do not overwrite later edits on rerun. Resolve existing programs/accounts, preserve their IDs/assignments, and fail transactionally on ambiguous matches. Proposed follow-ups enter Backlog with no invented assignee or deadline. Source chats, contact details, workbooks and operational datasets must not be committed here. Public test fixtures are fictitious.

## Validation and release

Run Java 21 `mvn verify`, all migration/RLS suites on a disposable database, frontend lint/unit/build and affected browser flows at 375/1440px. Verify unknown vs zero, evidence requirements, timing preservation on toggle, source labels, denied outsiders/anonymous users, and removed PJ access. Fixture browser results do not prove hosted Auth login.

Apply the additive migration to the verified authorized development project before releasing the backend and then frontend. Existing callers remain compatible. Application rollback can retain the new tables/columns without data loss. Never reset/bootstrap hosted data. New goals/agenda/history are loaded only in program workspace tabs; dashboard initial reads do not grow with evidence text.
