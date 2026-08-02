# Verification Report

## Result

E2E BLOCKED.

## Scope

- Target scenario: DCC 文控“文件分发/旧版回收”真实 Playwright E2E。
- Required path: non-admin DCC user, real page distribution/receipt/recovery actions, V1->V2 version transition, V2 current effective use, V1 non-misuse, final read-only API/DB reconciliation.
- Safety boundary: no admin account, no API-only substitute, no direct SQL/API insert/update for distribution, receipt, recovery, version state, or permissions.

## Rule Reads

- Read `AGENTS.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/frontend-development.md`, `docs/local-runtime.md`, `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, `docs/experience-index.md`, and Playwright skill.

## Runtime

- Frontend: `http://127.0.0.1:8081/` returned HTTP 200 and is owned by the `E:\IntRuoyi\IntRuoyiFronted` Vite process.
- Backend: `http://127.0.0.1:48081/actuator/health` returned `UP`; current runtime jar is `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-180316.jar`.
- Browser: Playwright used local Chrome at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Password handling: password was injected only through `DCC_E2E_PASSWORD`; no plaintext password is written in this report.

## Task-Owned File

- File number: `CODX-DCC-DIST-REC-DISTREC20260802173908`.
- Master ID: `2054545668044062889`; current active controlled file ID points to V2 `2054545668044070280`.
- V1: controlled file ID `2054545668044070279`, version `V1.0`, status `SUPERSEDED`.
- V2: controlled file ID `2054545668044070280`, version `V2.0`, status `ACTIVE`.
- Preparation evidence: `paper-chain-result.json` status `PASS`, produced by real DCC upload/revision/publish pages.

## Page Evidence

- Direct non-viewer detail route without an approved query redirected to `/dcc/controlled-file/browser`; this matches the route guard behavior and was not bypassed.
- Formal traceability detail path used: `http://127.0.0.1:8081/dcc/controlled-file/detail/2054545668044070280?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`.
- The V2 traceability detail page rendered the distribution table with QA / 待分发 / 纸质发放 and the visible action “确认纸质发放”.
- Clicking “确认纸质发放” as non-admin `wangsiyu` through the real page called the target paper issue endpoint and returned business code `1080000049`: `Current user cannot acknowledge this paper distribution`.
- Failure evidence: `paper-issue-recovery-final-result.json`; screenshot: `screenshots/failure.png`.

## Distribution And Recovery

- V1 distribution record ID: `4323`; medium `PAPER`; status `PENDING`; recipients `[]`; acknowledgedBy `null`; recoveredBy `null`; recoveredAt `null`.
- V2 distribution record ID: `4324`; medium `PAPER`; status `PENDING`; recipients `[]`; acknowledgedBy `null`; recoveredBy `null`; recoveredAt `null`.
- Recipient responsibility: intended paper recipient `panhaitao` could not be recorded because paper issue was blocked before recipient rows were created.
- Recovery responsibility: not created; V1 could not be advanced to `ACKNOWLEDGED`, so the page could not proceed to “确认回收”.
- Recovery record ID: BLOCKED / not generated.

## Permission Blocker

- Read-only DB verification shows category `907233` / `过程检验规程` has active `APPROVE` and `UPLOAD` rules, but no active `DISTRIBUTE` rule.
- Because `DccPaperDistributionAckServiceImpl` requires category `DISTRIBUTE` permission for both paper issue and recovery, the current environment lacks the formal permission precondition for this scenario.
- This is a permissions/test-data blocker, not a runtime, MinIO, or credential blocker.

## Old-Version Non-Misuse

- Real controlled browser page path: `http://127.0.0.1:8081/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-DIST-REC-DISTREC20260802173908&pageNo=1&pageSize=20`.
- Browser result: total `1`, returned only V2 `2054545668044070280` / `V2.0` / `ACTIVE`.
- V1 `2054545668044070279` was not visible as the current effective controlled file.
- Evidence: `controlled-browser-paper-v1-v2-probe.json`.

## Final Read-Only Reconciliation

- Evidence file: `blocked-readonly-db-verification.json`.
- Confirmed V1 `SUPERSEDED`, V2 `ACTIVE`, master current active = V2.
- Confirmed paper distribution records exist for V1/V2 but remain `PENDING`.
- Confirmed no receipt/recipient/recovery responsibility was created due the real page permission blocker.

## Required To Unblock

- Add or assign a formal, active `DISTRIBUTE` category permission rule for category `907233` to an approved non-admin DCC distribution owner, then rerun the same Playwright path.
- Do not unblock by admin login, API-only issue/recovery, direct SQL status changes, or direct insertion of distribution/recipient/recovery records.
