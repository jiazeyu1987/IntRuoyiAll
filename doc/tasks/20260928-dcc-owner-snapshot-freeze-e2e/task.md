# DCC Owner Snapshot Freeze E2E

## Goal

Use the real Playwright UI in tenant `芋道源码` to verify that a newly submitted DCC upload snapshots the production and quality department leaders at task creation, and that changing department leaders later does not transfer the existing obligations. Restore the original leaders through the department UI.

## BDD

- BDD-09 (creation-time snapshot): Given the production and quality signoff matrix selects those departments and their configured leaders are Owner A (`910326`) and Owner B (`910327`), when admin submits a new upload with `needTraining=false`, then the created signoff obligations are assigned to `910326` and `910327` and retain the leader configuration digest captured at task creation.
- BDD-10 (frozen owner): Given that the created file has obligations assigned to Owner A and Owner B, when admin changes the production leader to Owner B and the quality leader to Owner A through the department management UI, then refreshing the original task detail still shows the original department obligations and assignees `910326` / `910327`, with unchanged creation-time digests.
- Restoration: Given the department leaders were exchanged for the test, when admin restores production to Owner A and quality to Owner B and reopens both department forms, then the original mapping is visible again.

### Given / When / Then

- Given: tenant `芋道源码`, login `admin`, runtime `http://127.0.0.1:8062` / backend `48062`; production leader Owner A `910326`; quality leader Owner B `910327`; both departments selected by the active signoff matrix; task-owned unique file number `DCC-OWNER-SNAPSHOT-<UTC>`; `needTraining=false`.
- When: use the upload page to fill the form and submit; inspect the task from its real detail page and approval center; exchange department leaders using the real department management forms; refresh the created task detail; restore and reopen the original department mappings.
- Then: the natural page response/detail projection shows exactly the production and quality department obligations with assignee IDs `910326` and `910327` and non-empty leader configuration digests; after the exchange, the old task still displays the same department-to-assignee mapping and digests; after restoration, department management displays production -> Owner A and quality -> Owner B.

## Milestones

1. Confirm runtime ownership and task-owned upload source; complete before any business write.
2. Create and submit one unique upload through the real page with training unchecked.
3. Verify the created task, department obligations, assignee IDs, digests, and approval-center entry using natural page responses and visible UI.
4. Exchange leaders in the department UI, reopen the created task, and verify its snapshots are unchanged.
5. Restore original leaders through the UI, reopen both forms, and save final evidence.

## Expected Verification

- Playwright uses the real browser page and only UI-driven business actions; no direct `fetch`, API client, database, or Git operations.
- Evidence records UTC run ID, unique file number, file ID, page URLs, relevant natural request/response HTTP and business codes, department obligations, assignee IDs, digests, visible page facts, console/page errors, and screenshots.
- Final status distinguishes `PASS`, `BLOCKED`, or `FAIL`. BDD-09's pre-creation configuration-change timing variant is separately identified if not executed; the explicitly requested create-under-A/B then swap-after-creation sequence is the primary BDD-10 proof.
- `node --check` validates the Playwright script; task documents receive structural checks.

## Design Constraints

- Use only the given task-owned upload; do not open, modify, or act on existing DCC business files.
- Only admin performs upload and read-only task inspection. Owner accounts are not used to log in.
- Department changes are temporary and must be restored to production -> Owner A (`910326`) and quality -> Owner B (`910327`) through the UI, then reopened and verified.
- If submission is blocked by signature qualification, record the actual UI error and stop unless the page provides a precise missing ordinary DCC signoff role; any permitted role change must be made through admin UI and must not grant administrator or log roles.
- Never record passwords, cookies, tokens, or credential payloads in evidence.
- No API/fetch/DB/Git; do not restart either runtime service.

## Current Status

blocked

## Cleanup Keep

- doc/tasks/20260928-dcc-owner-snapshot-freeze-e2e/task.md
- doc/tasks/20260928-dcc-owner-snapshot-freeze-e2e/execution-log.md
- doc/tasks/20260928-dcc-owner-snapshot-freeze-e2e/verification-report.md
- doc/tasks/20260928-dcc-owner-snapshot-freeze-e2e/owner-snapshot-freeze-real-ui.e2e.cjs
