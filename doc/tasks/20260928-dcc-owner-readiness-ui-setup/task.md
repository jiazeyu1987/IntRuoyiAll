# DCC Owner Readiness UI Setup

## Goal

Use the real Playwright UI in the authorized isolated runtime to configure only the DCC approval-readiness prerequisites for the already-created test owners. Do not create or submit a controlled-file business record. Preserve the existing signoff approval candidates (admin) and department leader mappings (production -> Owner A, quality -> Owner B).

## BDD

- BDD-01 (ordinary system position): Given Owner A (`dccE2EOwnerA09286704`, ID `910326`) and Owner B (`dccE2EOwnerB09284620`, ID `910327`) are enabled test users with only `审批中心入口` and `DCC Action View`, when admin assigns the least-privileged ordinary system position through the user form, then reopening each user form shows that position and no administrator position/role was added.
- BDD-02 (authorized valid personal signature): Given the existing task-owned PNG `doc/tasks/20260921-dcc-three-workflows-implementation/artifacts/test-signature-admin-20260926.png` is explicitly approved as a test image, when each owner completes the official UI flow to upload and enable their own test signature and admin grants the corresponding DCC signature authorization, then reopening the official signature pages shows authorization enabled and each owner's image/evidence at status `VALID` (or the exact official review state and blocker if a separate review is required).
- BDD-03 (least DCC review permission): Given the approval route keeps `admin` as the approval candidate, when admin grants only the smallest UI-configurable DCC co-sign and document-control `REVIEW` scopes to the two owners, then the reopened permission UI shows those REVIEW scopes and does not grant `APPROVE`, administrator, signature-governance administration, or log access.
- BDD-04 (no business action): Given readiness setup is complete, when this task ends, then no upload, approval, revision, distribution, or obsolete business action has been performed and no new DCC controlled-file record has been submitted.
- BDD-05 (auditable UI writes): Given each authorized settings change is made, when Playwright observes the natural page request and response and reopens the same form, then evidence records the sanitized URL/method/HTTP and business result plus visible final values without passwords, tokens, cookies, or credential payloads.

## Given / When / Then

- Given: tenant `芋道源码`; admin login `admin`; isolated frontend `http://127.0.0.1:8062`; isolated backend port `48062`; Owner A ID `910326`; Owner B ID `910327`; current department leader mappings production -> A and quality -> B; current ordinary roles `审批中心入口` and `DCC Action View`; user-provided authorization for the listed test-only readiness configuration; existing test PNG listed above.
- When: use visible system-user forms to assign an ordinary system position; use the official signature authorization page for per-owner authorization; use each owner's own authenticated UI to upload/enable the approved test PNG; use the narrowest DCC permission UI to assign only REVIEW scopes. Observe only natural browser requests and reopen forms after each save.
- Then: each user's position, signature authorization/image state, and configured REVIEW scope are visible after reopening; the image/evidence reaches official `VALID` state if supported by the official flow; no APPROVE/admin/log or unrelated permission is added; department mapping and approval candidates remain unchanged; no DCC business file is created or acted on.

## Milestones

1. Read E2E/login/runtime/closeout rules and related owner setup reports; confirm port ownership and the existing test PNG before UI writes.
2. Inspect the visible system-user form and assign/reopen the minimum ordinary system position for both users.
3. Inspect the official DCC signature authorization UI; authorize both users only if the page exposes the requested narrow control; have each owner use their own account page to upload/enable the approved test image and verify official `VALID` status.
4. Inspect the DCC permission UI and assign only explicit REVIEW scopes for co-sign/document-control; stop if the UI cannot exclude APPROVE/admin/log access.
5. Reopen all changed UI forms, verify department leaders and route approval candidates are unchanged, capture sanitized natural-response evidence and screenshots, and write the verification report.

## Outcome

- A already had the ordinary `普通员工` system position; B received it through the visible user form and was reopened to verify it. Both retained `DCC Action View` and `审批中心入口`.
- Admin enabled DCC signature authorization for A/B through the visible user-authorization page; natural PUTs returned HTTP 200/business code 0 and reopened rows showed `已启用`, `未锁定`.
- A hidden DCC button permission `DCC 测试 REVIEW 权限` with exact permission `dcc:controlled-file:review` was created under the DCC root through system menu UI. It was assigned only to the dedicated `DCC E2E Owner Review 20260928` role. Reopening the role showed only this button permission under DCC; no browse, approve, admin, or log permission.
- The dedicated role is present on both owner rows after real user-role UI verification; each row retains the two preexisting roles and `普通员工` position.
- Blocked: each owner must still authenticate on their own UI to upload/enable the supplied generated test image and reach the official signature image status. Current owner passwords were not provided in this task turn. No owner image status is claimed.
- No DCC business file was created or acted on. Department leaders and approval candidates were not changed.

## Expected Verification

- Playwright drives visible pages only. No `fetch`, APIRequest, `axios`, direct HTTP/curl, database access, Git command, backend restart, or hidden/forced UI action.
- For every save/upload/enable action: capture natural request method, URL path, HTTP status, sanitized business code/message when available, then reopen the relevant user/signature/permission form and assert the visible value.
- Capture screenshots for the reopened user, signature authorization, own-signature, and permission forms. Do not expose credentials or browser storage.
- Verify no DCC upload/approval/revision/distribution/obsolete action was submitted and no business record creation request occurred.
- Record any absent/restricted UI scope as `BLOCKED` with the exact page and visible limitation; do not substitute a broader role or privileged API.

## Design Constraints

- Only tenant `芋道源码`, isolated runtime `8062/48062`, and users `910326` / `910327` are in scope.
- Do not touch the shared `int_main` backend, other users/departments, route matrices, approval candidates, signature policy, or DCC business files. Do not modify shared existing role/menu definitions; if REVIEW can only be represented by a dedicated least-privilege DCC role/menu, configure only that dedicated test scope authorized in the current user turn.
- Keep existing department leaders production -> Owner A and quality -> Owner B; do not exchange them.
- Do not grant `APPROVE`, admin, signature-governance administration, or DCC log permissions. A permission screen that cannot represent the requested REVIEW-only scope is a blocker.
- Use only the supplied test PNG. Do not create, synthesize, trace, or misrepresent a person's real signature; label this asset as test evidence in the formal UI where a reason/description is required.
- Do not record passwords, tokens, cookies, request bodies containing credentials, or browser storage values in task assets.
- Do not use API/fetch, database, Git, or service lifecycle operations.

## Current Status

blocked

### Blockers

- Owner A and B current passwords were not included in this task turn. The official `我的签名` page is owner-only, so each owner must log in themselves or provide current credentials through an approved private channel before their own image can be uploaded/enabled.
- The supplied PNG is explicitly authorized for test use and is visibly labeled generated test material. It has not been uploaded and must not be represented as a real person's genuine signature.

## Cleanup Keep

- doc/tasks/20260928-dcc-owner-readiness-ui-setup/task.md
- doc/tasks/20260928-dcc-owner-readiness-ui-setup/execution-log.md
- doc/tasks/20260928-dcc-owner-readiness-ui-setup/verification-report.md
- doc/tasks/20260928-dcc-owner-readiness-ui-setup/owner-readiness-real-ui.e2e.cjs
