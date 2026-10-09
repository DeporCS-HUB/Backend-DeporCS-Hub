# BPH and Staff adjustment

The application presents two department roles:
- **BPH** manages programs, finance, inventory, events, and task assignments.
- **Staff** is an executing member: reads department data, creates self-assigned tasks, and edits/deletes tasks owned or assigned to them under the existing ownership rules.

Own-profile display-name editing is available to both roles. The Team directory is read-only; trusted operators still manage account activation and role assignment. Neither role can promote themselves through Auth metadata or profile forms.

## Existing account compatibility

No migration, seed, role update, or Auth-account change is needed for this release. Stored database roles and existing RLS policies remain intact:

| Stored profile.role | API departmentRole | Product label |
| --- | --- | --- |
| staff | bph | BPH |
| admin | bph | BPH |
| member | staff | Staff |

The backend derives departmentRole only from the trusted database profile. It includes the field in login, refresh, session, profile directory, and own-profile update responses. The existing role field remains for compatibility with older clients and RLS.

The frontend prefers departmentRole and safely supports older backend responses using the same mapping. Unrecognized roles have no management controls. Backend and RLS remain the authority; UI labels cannot grant permission. The frontend can be deployed before the backend without disrupting existing sessions.

## Interface changes

Dashboard swimmer/trophy emoji stickers and inventory emoji art are removed. Inventory uses a consistent Lucide package icon and no longer offers an emoji input. Existing stored emoji values are preserved when editing a record; no data cleanup is performed.

The dashboard has a concise Indonesian workspace banner, role-specific quick links, and the same database totals. Team supports name search and BPH/Staff filtering on the current page, with existing pagination. Staff task forms do not load or display other assignee choices.

## Deployment checkpoint

The user reported successful Vercel deployment and login on 9 October 2026:
- Frontend: https://depor-cs-hub-web.vercel.app
- Backend: https://depor-cs-hub-api.vercel.app

Backend production origin must be exactly https://depor-cs-hub-web.vercel.app. API deployment inspection through the current Vercel connection is still denied; the user-reported success is separate from automated hosted validation.

This release does not populate department data. Database filling was explicitly deferred by the user.

## Validation

CI runs the frontend build/lint/unit suite and mocked browser flows, plus Java tests and existing disposable-PostgreSQL RLS tests. Tests check trusted department-role responses, denial of role spoofing, legacy-client compatibility, Staff ownership restrictions, and Team filters. Hosted browser/Supabase acceptance of this release is not established by mocked tests.
