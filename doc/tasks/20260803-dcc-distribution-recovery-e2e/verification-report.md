# Verification Report

## Result

PASS for the available real-page DCC distribution/recovery chain. The separate current-run fresh upload candidate remains BLOCKED and is recorded under `Blocked Fresh Candidate`; it was not used to claim PASS.

## Scope

- Target scenario: DCC 文控“文件分发/旧版回收”真实 Playwright E2E。
- Required path: non-admin user, real page distribution/receipt/recovery actions, V1 -> V2 version transition, V2 current effective use, V1 non-misuse, final read-only API/DB reconciliation.
- Business rule clarified by user: do not search for another paper-distribution-rule entry. The `文控权限 > 分发规则` tab is the distribution rule source; its department is also the paper distribution department.
- Safety boundary: no admin account, no API-only substitute, no direct SQL/API insert/update for distribution, receipt, recovery, version state, approval state, or publish state.

## Rule Reads

- Read `AGENTS.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/frontend-development.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, and Playwright skill `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`.

## Runtime

- Backend `http://127.0.0.1:48081/actuator/health` returned `UP`; frontend `http://127.0.0.1:8081/` returned HTTP 200.
- Tenant: `芋道源码` / tenant ID `1`.
- Browser: Playwright used local Chrome at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Password handling: password was injected through `DCC_E2E_PASSWORD`; no plaintext password is written in task artifacts.
- Current verification command: `node doc/tasks/20260803-dcc-distribution-recovery-e2e/dcc-rule-trace-current-verify.cjs` with PowerShell environment-variable password injection.

## Distribution Rule Source

- Real page path: `/dcc/controlled-file/categories?tab=distribution-rules`.
- Screenshot: `screenshots/current-distribution-rules-906104.png`.
- Category row visible on the real page: `DCC_OTHER_TEMPLATE_900250 / 其他 / 必须 / 质量体系部（公盘目录） / 启用 1 条`.
- Read-only API captured from that same page load: `/admin-api/dcc/file-categories/906104/distribution-rules` returned rule ID `106`, department ID `253`, active `true`, `distributionMedium=PUBLIC_FOLDER`.
- Per user clarification, this same UI department rule is the paper distribution department source. The real detail pages and DB reconcile paper distribution rows to department ID `253 / 质量体系部`.

## Verified File Chain

- File number: `CODX-DCC-DIST-906104-DISTTENANT120260802195305`.
- Category: `906104 / 其他`, code `DCC_OTHER_TEMPLATE_900250`, `distributionRequired=1`.
- V1: controlled file ID `2054545668044070297`, version `V1.0`, final status `SUPERSEDED`.
- V2: controlled file ID `2054545668044070302`, version `V2.0`, final status `ACTIVE`.
- Master: `2054545668044062904`, status `ACTIVE_CHAIN`, `currentActiveControlledFileId=2054545668044070302`.
- Source action evidence from real Playwright page chain: `doc/tasks/20260802-dcc-distribution-recovery-e2e/paper-chain-tenant1-result.json`, `paper-chain-tenant1-training-resume.json`, `paper-issue-recovery-final-result.json`, and `final-pass-readonly-db-verification.json`.
- Current traceability re-verification evidence: `current-rule-trace-verification.json` and screenshots `current-v2-distribution-trace.png`, `current-v1-recovery-trace.png`, `current-controlled-browser-v2-only.png`.

## Page Evidence

- Distribution rules page: `/dcc/controlled-file/categories?tab=distribution-rules` showed the configured department `质量体系部` for category `906104`.
- V2 distribution trace page: `/dcc/controlled-file/detail/2054545668044070302?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`.
- V2 distribution section showed `质量体系部`, recipient `潘海涛 (panhaitao)`, status `已确认`, medium `纸质发放`, issuer `王思雨 (wangsiyu)`, issue time `2026-08-02 23:30:04`, and action `确认回收` still available.
- V1 recovery trace page: `/dcc/controlled-file/detail/2054545668044070297?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`.
- V1 recovery section showed `质量体系部`, recipient `潘海涛 (panhaitao)`, status `已回收`, medium `纸质发放`, issuer `王思雨 (wangsiyu)`, issue time `2026-08-02 21:25:23`, recoverer `王思雨 (wangsiyu)`, and recovery time `2026-08-02 23:30:08`.
- Controlled browser page: `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-DIST-906104-DISTTENANT120260802195305&pageNo=1&pageSize=20` returned total `1`, only V2 ID `2054545668044070302`, version `V2.0`, status `ACTIVE`; V1 was not visible as current effective.

## Distribution And Recovery

- V1 distribution record ID `4341`: `PAPER`, final status `RECOVERED`, controlled file ID `2054545668044070297`, version `V1.0`, file status `SUPERSEDED`, department ID `253`.
- V1 issue responsibility: `wangsiyu` / user ID `910250`, acknowledged at `2026-08-02 21:25:23`.
- V1 recipient responsibility: `panhaitao` / user ID `173`, recipient row `46820`.
- V1 recovery responsibility: `wangsiyu` / user ID `910250`, recovered at `2026-08-02 23:30:08`.
- V2 distribution record ID `4344`: `PAPER`, final status `ACKNOWLEDGED`, controlled file ID `2054545668044070302`, version `V2.0`, file status `ACTIVE`, department ID `253`.
- V2 issue responsibility: `wangsiyu` / user ID `910250`, acknowledged at `2026-08-02 23:30:04`.
- V2 recipient responsibility: `panhaitao` / user ID `173`, recipient row `46865`.

## Old-Version Non-Misuse

- PASS: V1 `2054545668044070297` is `SUPERSEDED` and recovered.
- PASS: V2 `2054545668044070302` is `ACTIVE` and acknowledged/distributable.
- PASS: master current active version points to V2.
- PASS: controlled browser current-effective query returns V2 only; V1 is absent from current-effective browse results.

## Read-Only Reconciliation

- Evidence file: `current-rule-trace-verification.json`.
- Final read-only DB confirmed category rule ID `106`, department ID `253`, V1/V2 statuses, master current pointer, V1 recovered paper distribution `4341`, V2 acknowledged paper distribution `4344`, issuer/recoverer usernames, recipient usernames, and timestamps.
- No target network failures, console errors, or page errors occurred in the final PASS run.

## Blocked Fresh Candidate

- Current-run fresh candidate `CODX-DCC-DIST-906104-DISTTENANT1202608030005` remains BLOCKED and was not used for PASS.
- V1 ID `2054545668044070310` is still `PENDING_MATRIX_APPROVAL`; active task `批准` is assigned to `zhaomingyu / 424`.
- Real page path reached: `/dcc/controlled-file/detail/2054545668044070310?handling=approval&from=approval-center&processInstanceId=0599828d-8e8f-11f1-a5cc-00155d2984a0&taskId=0cf42401-8e8f-11f1-a5cc-00155d2984a0`.
- Blocker: the real page did not expose an approval action button and displayed `受控打印动作投影缺失` / missing `PRINT` permission. Evidence: `paper-chain-full-result.json` and `screenshots/DISTTENANT1202608030005-approve-V1.0-zhaomingyu-button-missing.png`.
- Non-`其他` candidate category `907233 / 过程检验规程` also remains not used for PASS because the current UI lacks a category permission-rule page to add `DISTRIBUTE`; this was not bypassed by API/SQL.

## Safety

- No admin account was used for business E2E.
- No SQL/API inserted or updated distribution, recipient, recovery, approval, publish, or version state.
- The final DB/API usage was read-only reconciliation after real page verification.
- The only page preference changed by the current verification script was the local browser list page size for `dcc.controlledFile.permission.distributionRules`, to make the real table row visible without relying on the quick-filter control that emitted a page error in failed script attempts.
