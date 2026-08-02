# Execution Log

## User Intent

- 用户要求在 `E:\IntRuoyi` 对 DCC 文控“文件分发/旧版回收”进行真实 Playwright E2E 验证。
- 必须使用非 admin 账号，密码通过环境变量注入且不记录明文。
- 必须通过真实页面操作；API/DB 只用于最终只读核验。
- 若缺页面入口、权限、测试数据或运行态，记录 E2E BLOCKED，不使用 API-only、SQL 改状态或 admin 账号绕过。

## Rule Reads

- Read `AGENTS.md`.
- Read `docs/e2e-rules.md`.
- Read `docs/login-access.md`.
- Read `docs/frontend-development.md`.
- Read `docs/task-closeout-rules.md`.
- Read `docs/local-runtime.md`.
- Read `docs/worktree-restrictions.md`.
- Read `docs/branch-runtime-ports.md`.
- Read `docs/database-rules.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/powershell-memory.md`.
- Read `docs/experience-index.md`; matching gates copied into `task.md`.
- Read Playwright skill `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`.

## BDD

BDD: DCC controlled file distribution and old-version recovery -> Given a non-admin DCC distribution owner, a non-admin recipient, and a task-owned ACTIVE V1 controlled file that can be revised to ACTIVE V2, When the owner registers controlled-copy distribution through the real page, the recipient signs/acknowledges receipt through the real page, V1 is superseded by V2, and the old V1 copy is recovered through the real page, Then the page shows distribution, receipt, recovery responsibility and timestamps, V2 remains distributable, V1 cannot be reused as an effective distribution source, and read-only API/DB evidence matches the UI.

## Command Intent Log

- Created task directory `doc/tasks/20260802-dcc-distribution-recovery-e2e`.
- Created baseline `task.md` and `execution-log.md`.
- Observed existing dirty worktree before this task; this task will only add/update files under `doc/tasks/20260802-dcc-distribution-recovery-e2e/` and run verification commands.

## RED / GREEN

- RED: credential-preflight -> FAIL, no DCC/non-admin E2E tenant, distributor, recipient, approver/signature username/password environment variables were present in the initial shell.
- GREEN: credential-preflight-resolved -> PASS, user provided non-admin password injection expression; subsequent Playwright runs used `DCC_E2E_PASSWORD` without logging plaintext.
- GREEN: paper-chain-preparation -> PASS, real DCC upload/revision/publish pages prepared task-owned file `CODX-DCC-DIST-REC-DISTREC20260802173908` with V1 `2054545668044070279` = `SUPERSEDED`, V2 `2054545668044070280` = `ACTIVE`, and paper distribution rows `4323` / `4324`.
- RED: paper-issue-recovery-real-page -> FAIL, non-admin `wangsiyu` reached official traceability detail path and saw “确认纸质发放”, but the target POST returned business code `1080000049` (`Current user cannot acknowledge this paper distribution`) because category `907233` has no active `DISTRIBUTE` permission rule.
- GREEN: controlled-browser-v2-only-readonly -> PASS, real controlled browser page for the paper-chain file returned only V2 `ACTIVE`; V1 was not visible as current effective file.
- GREEN: final-readonly-db-blocker-verification -> PASS, read-only DB confirmed V1/V2 states, PENDING paper distribution rows, empty paper recipients/recovery fields, and missing category `DISTRIBUTE` rule.

## Evidence

- Runtime preflight: frontend `http://127.0.0.1:8081/` returned HTTP 200; port 8081 belongs to `E:\IntRuoyi\IntRuoyiFronted` Vite command.
- Runtime preflight: backend `http://127.0.0.1:48081/actuator/health` returned `UP`; current 48081 process runs `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-180316.jar`.
- Browser preflight: local Chrome found at `C:\Program Files\Google\Chrome\Application\chrome.exe`; Edge also found.
- Credential preflight: initial missing-password blocker was superseded by user-provided PowerShell environment injection; reports do not record the plaintext password.
- Static route/source reconnaissance: DCC detail page contains distribution and recovery UI labels/actions (`分发状态`, `确认签收`, `确认纸质发放`, `确认回收`) and backend controllers expose paper/electronic distribution endpoints.
- Real page preparation evidence: `paper-chain-result.json` status `PASS`; `fileNumber=CODX-DCC-DIST-REC-DISTREC20260802173908`; V1 `2054545668044070279`; V2 `2054545668044070280`; publish instance `438`.
- Real page issue/recovery attempt: `paper-issue-recovery-final-result.json`; page path `http://127.0.0.1:8081/dcc/controlled-file/detail/2054545668044070280?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`; visible table included QA / 待分发 / 纸质发放 / 确认纸质发放; POST `/paper-distributions/{id}/acknowledge` returned business code `1080000049`.
- Route guard evidence: direct non-viewer detail without approved query redirected to `/dcc/controlled-file/browser`; official allowed non-viewer path for this scenario is `traceability=1&from=browser&returnTo=...`, which was used for the final page attempt.
- Read-only controlled browser evidence: `controlled-browser-paper-v1-v2-probe.json` status `PASS`, total `1`, returned only V2 `2054545668044070280` / `V2.0` / `ACTIVE`; V1 visible = `false`.
- Read-only DB evidence: `blocked-readonly-db-verification.json` shows V1 `SUPERSEDED`, V2 `ACTIVE`, master current active points to V2, distribution `4323` = V1/PAPER/PENDING, distribution `4324` = V2/PAPER/PENDING, no recipients, no `acknowledgedBy`, no `recoveredBy`, and no active `DISTRIBUTE` rule for category `907233`.

## Blockers

- E2E BLOCKED: current task-owned category `过程检验规程` / `907233` lacks an active `DISTRIBUTE` permission rule, so non-admin DCC user `wangsiyu` cannot execute the real page paper issue action even though the button is visible.
- Impact: cannot complete paper recipient responsibility, V1 recovery responsibility, recovery timestamp, or final PASS for both “分发”和“回收”; rows `4323` / `4324` remain `PENDING`.
- Safety boundary: no admin fallback, no API-only substitute, no SQL/API status insert/update, no direct permission data repair, and no product-code changes outside this scenario.

## Resume 2026-08-02 21:09

- User changed direction to use a category that has distribution permission rules, then asked to continue.
- Re-read required execution rules and Playwright skill: `AGENTS.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/frontend-development.md`, `docs/database-rules.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, and `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`.
- Runtime check: frontend `http://127.0.0.1:8081/` returned HTTP 200 and is owned by `E:\IntRuoyi\IntRuoyiFronted` Vite; backend `http://127.0.0.1:48081/actuator/health` refused connection and port `48081` is not listening.
- Runtime check: observed active `yudao-server` Maven package processes under `E:\IntRuoyi\IntRuoyiBackend`; waited 120 seconds and processes remained, with new package processes appearing. No process was killed.
- Restart evidence from existing logs remains blocked by unrelated MES mapper parse failure: `MesProProcessPoolTimelineReadMapper.xml` -> `SAXParseException: 前言中不允许有内容。`
- Read-only DB scan: tenant `芋道源码` category `906104 / 其他` has distribution rule `106` and non-admin `wangsiyu` has `DISTRIBUTE` via role `910431`, but `APPROVE` is only granted to user ID `1`; current file `CODX-DCC-DIST-906104-DISTTENANT120260802195305` has V1 `ACTIVE`, V2 `READY_TO_PUBLISH`, distributions `4341` and `4344` still `PENDING`.
- Read-only DB scan: tenant `122` category `900347 / Codex Local DCC Category` has non-admin `aoteman` with `APPROVE / DISTRIBUTE / UPLOAD` and distribution rule, but no published DCC `PUBLISH / READY_TO_PUBLISH` business approval policy.
- Read-only DB scan: split-user full candidate count is `0`; no existing category satisfies published publish policy + active distribution rule + non-admin APPROVE + non-admin DISTRIBUTE without changing permissions/policies.
- E2E BLOCKED: cannot continue real page publish/distribution/recovery. This run did not use admin, did not use API-only as a substitute, and did not insert/update SQL/API records or permission rules.
- Evidence: `tenant1-current-blocked-readonly-db-verification.json`; `candidate-permission-scan-20260802-210906.json`; `verification-report.md`.

## Resume 2026-08-02 21:25

- User reported backend has started and asked to continue.
- Runtime check: backend `http://127.0.0.1:48081/actuator/health` returned `UP`; port `48081` owned by Java process running `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-170535.jar`; frontend `8081` returned HTTP 200.
- GREEN: V1 paper distribution real page -> PASS. Ran `dcc-distribution-recovery-e2e.cjs` with `DCC_E2E_DISTRIBUTION_RECOVERY_MODE=ack-v1-only`, tenant `芋道源码`, non-admin actor `wangsiyu`, recipient `panhaitao`, password injected through `DCC_E2E_PASSWORD`.
- Real page path: `http://127.0.0.1:8081/dcc/controlled-file/detail/2054545668044070297?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`.
- Page evidence before action: V1 distribution table showed `质量体系部 / 待分发 / 纸质发放 / 确认纸质发放`.
- Page evidence after action: V1 distribution table showed `潘海涛 (panhaitao) / 已确认 / 纸质发放 / 发放：王思雨 (wangsiyu) / 时间：2026-08-02 21:25:23 / 确认回收`.
- Read-only DB verification: distribution `4341` moved to `ACKNOWLEDGED`, `acknowledged_by=910250 / wangsiyu`, recipient row `46820` points to user `173 / panhaitao`; V2 distribution `4344` remains `PENDING`; no recovery fields exist.
- RED: V2 publish real page after backend restore -> FAIL. Reran `dcc-paper-chain-prepare-e2e.cjs` with existing V1/V2 IDs; page loaded but `发布申请` button did not become visible within 30 seconds.
- Read-only DB permission verification: category `906104` `APPROVE` remains assigned only to user ID `1`; `wangsiyu` only has `DISTRIBUTE` via role `dcc_distribute_e2e`.
- E2E BLOCKED: recovery must not be clicked while V1 is still `ACTIVE`; V2 is still `READY_TO_PUBLISH`, so old-version recovery/non-misuse cannot be completed through the required real page path.
- Evidence: `paper-issue-recovery-final-result.json`; `tenant1-post-v1-ack-readonly-db-verification.json`; `publish-blocked-after-backend-up.json`; `verification-report.md`.
