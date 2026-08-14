# Verification Report

## Result

PASS.

## Scope

- Target scenario: DCC 文控“文件分发/旧版回收”真实 Playwright E2E。
- Required path: non-admin DCC user, real page distribution/receipt/recovery actions, V1 -> V2 version transition, V2 current effective use, V1 non-misuse, final read-only API/DB reconciliation.
- Safety boundary: no admin account, no API-only substitute, no direct SQL/API insert/update for distribution, receipt, recovery, version state, or approval state.

## Rule Reads

- Read `AGENTS.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/frontend-development.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, and Playwright skill.

## Runtime

- Frontend: `http://127.0.0.1:8081/` returned HTTP 200.
- Backend: `http://127.0.0.1:48081/actuator/health` returned `UP`.
- Browser: Playwright used local Chrome at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Password handling: password was injected only through `DCC_E2E_PASSWORD`; no plaintext password is written in this report.
- Runtime note: one retry hit transient tenant lookup 500 during backend runtime switch at 23:26, then `get-id-by-name` returned business code `0` and the real page flow was rerun successfully.

## Permission Prerequisite

- User explicitly authorized adding the missing `wangsiyu` ability: “给 wangsiyu 补上这个能力...然后把这个权限角色赋值给 wangsiyu”.
- Real page visibility probe showed `wangsiyu` could already see `文控权限 / 审阅矩阵` at `/dcc/controlled-file/categories?tab=review-matrix`; category `906104` was not on the first page, but `category:manage` permission was present. Evidence: `wangsiyu-category-manage-visibility-before.json`.
- Minimal permission prerequisite added: existing role `dcc_distribute_e2e` / role ID `910431`, already assigned to `wangsiyu` / user ID `910250`, received category `906104` `APPROVE` rule ID `2624`.
- No business records were inserted or updated by SQL/API: distribution, recipient, recovery, version status, and approval state were changed only through real pages. Evidence: `permission-setup-wangsiyu-approve-role.json`.

## Current File

- Tenant: `芋道源码` / tenant ID `1`; non-admin users only.
- Category: `906104` / `其他`; active distribution rule for department `253 / 质量体系部`; `DISTRIBUTE` and task-authorized `APPROVE` available to `wangsiyu` through role `910431`.
- File number: `CODX-DCC-DIST-906104-DISTTENANT120260802195305`.
- V1: controlled file ID `2054545668044070297`, version `V1.0`, final status `SUPERSEDED`.
- V2: controlled file ID `2054545668044070302`, version `V2.0`, final status `ACTIVE`.
- Master: `2054545668044062904`, `currentActiveControlledFileId=2054545668044070302`.

## Page Evidence

- V1 upload, four-step upload approval, training, and original release were completed through real pages before this continuation; V1 became `ACTIVE`.
- V1 paper distribution was completed through real detail page by `wangsiyu`; recipient `panhaitao` was selected through the real user selector. Evidence: `paper-issue-recovery-final-result.json` from the V1 partial run.
- V2 publish prerequisite was unblocked by the authorized permission rule, then real page publish application submitted as `wangsiyu`, producing publish form instance `443` and BPM process `39fb6cce-8e81-11f1-aa29-00155d2984a0`.
- V2 publish BPM completed through real BPM pages with non-admin users: `wangsiyu -> zhaohaichen -> zhaojie -> zhaomingyu`; publish instance `443` became `EFFECTIVE`.
- V2 training was completed by `zhaomingyu` through real “我的培训” after controlled reading reached `632 / 600` seconds; `wangsiyu` then performed real “正式下发”, making V2 `ACTIVE` and V1 `SUPERSEDED`. Evidence: `paper-chain-tenant1-training-resume.json`.
- Final distribution/recovery page paths:
- V2 distribution page: `/dcc/controlled-file/detail/2054545668044070302?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`.
- V1 recovery page: `/dcc/controlled-file/detail/2054545668044070297?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`.

## Distribution And Recovery

- V1 distribution record ID `4341`: medium `PAPER`, final status `RECOVERED`, version `V1.0`, file status `SUPERSEDED`.
- V1 recipient responsibility: `panhaitao` / user ID `173`, recipient row `46820`.
- V1 issue responsibility: `wangsiyu` / user ID `910250`, acknowledged at `2026-08-02 21:25:23`.
- V1 recovery responsibility: `wangsiyu` / user ID `910250`, recovered at `2026-08-02 23:30:08`.
- V2 distribution record ID `4344`: medium `PAPER`, final status `ACKNOWLEDGED`, version `V2.0`, file status `ACTIVE`.
- V2 recipient responsibility: `panhaitao` / user ID `173`, recipient row `46865`.
- V2 issue responsibility: `wangsiyu` / user ID `910250`, acknowledged at `2026-08-02 23:30:04`.
- Evidence: `paper-issue-recovery-final-result.json` and `final-pass-readonly-db-verification.json`.

## Old-Version Non-Misuse

- Controlled browser query for file number returned total `1`, only V2 ID `2054545668044070302`, version `V2.0`, status `ACTIVE`.
- V1 ID `2054545668044070297` was not visible as the current effective controlled file.
- Evidence: `paper-issue-recovery-final-result.json` -> `browserVerification`.

## Read-Only Reconciliation

- Final DB/API evidence file: `final-pass-readonly-db-verification.json`.
- Confirmed V1 `SUPERSEDED`, V2 `ACTIVE`, master current active version points to V2.
- Confirmed distribution `4341` is `RECOVERED` with recoveredBy `wangsiyu` and recoveredAt `2026-08-02 23:30:08`.
- Confirmed distribution `4344` is `ACKNOWLEDGED` with acknowledgedBy `wangsiyu` and acknowledgedAt `2026-08-02 23:30:04`.
- Confirmed publish instance `443` is `EFFECTIVE`, publish BPM completed 4 tasks, and V2 training progress row `1048` acknowledged by `zhaomingyu`.

## Evidence Files

- `permission-setup-wangsiyu-approve-role.json`
- `wangsiyu-category-manage-visibility-before.json`
- `paper-chain-tenant1-after-approve-permission.json`
- `paper-chain-tenant1-publish-approval-resume2.json`
- `paper-chain-tenant1-training-resume.json`
- `paper-issue-recovery-final-result.json`
- `final-pass-readonly-db-verification.json`
