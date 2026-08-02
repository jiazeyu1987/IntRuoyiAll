# Verification Report

## Result

Latest requested full fresh E2E rerun is **PASS**.

Run `20260802222723` completed the clarified DCC “作废/废止” path by real Playwright UI flow: V1 was released, V2 was uploaded and approved, V2 publish approval completed, V1 automatically became `SUPERSEDED`, V2 became `ACTIVE`, master switched to V2, and controlled browsing no longer exposed V1 as a current effective file. Historical run `20260802212823` remains recorded as a superseded blocker after a publish approval HTTP `500`, but it is no longer the latest result.

## Scope

- Scenario: DCC 文控文件升版本后，旧版本自动失效/不再作为当前有效文件展示。
- Required path: V1.0 original release -> V2.0 revision upload -> non-admin approval/signature -> publish approval -> V1.0 `SUPERSEDED` -> V2.0 `ACTIVE` -> master current active V2 -> controlled browser and traceability verification.
- Environment: local `int_main`, frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`, MinIO ready on `http://127.0.0.1:9000/minio/health/ready`.
- Credential handling: password was injected only through the approved `DCC_E2E_PASSWORD` PowerShell expression; no plaintext password is recorded.
- Latest result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802222723.json`.
- Previous blocked result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802212823.json`.
- Chain JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\chain-result.json`.

## Fix Verification

| Check | Result |
| --- | --- |
| Regression RED | Missing focused render-safety static contract failed before adding `tests/e2e/dcc-detail-approval-render-safety-static.spec.js` |
| Static GREEN | `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS |
| Script check | `node --check tests/e2e/dcc-detail-approval-render-safety-static.spec.js` -> PASS |
| Package script | `pnpm e2e:dcc:detail-approval-render-safety:static` -> PASS |
| Adjacent approval contract | `node tests/e2e/dcc-detail-approval-own-task-without-process-query-static.spec.js` -> PASS |
| Adjacent handling summary contract | `pnpm e2e:dcc:detail-handling-summary:static` -> PASS |
| Type check | `pnpm ts:check` -> PASS |
| Full real E2E | `full-rerun-e2e-result-20260802222723.json` -> PASS |

The focused contract locks the previous approval detail rendering failure: no `})const openControlledBrowserLocation` glue remains, the real approver anchor `审批阶段进度` remains present, and every detail dialog `*.visible` v-model, including `controlledPrintDialog.visible`, has initialized reactive state.

## Latest Full Rerun

| Field | Evidence |
| --- | --- |
| Run ID | `20260802222723` |
| File number | `CODX-DCC-REV-FULL-20260802-20260802222723` |
| Result | `PASS`, command exit code `0` |
| V1 controlled file | `2054545668044070307`, `V1.0`, change type `NEW`, final status `SUPERSEDED` |
| V2 controlled file | `2054545668044070308`, `V2.0`, change type `REVISION`, final status `ACTIVE` |
| Master | `2054545668044062911`, `ACTIVE_CHAIN`, current active version `2054545668044070308` |
| V1 successor | `2054545668044070308` |
| Publish form instance | `442`, status `EFFECTIVE`, object `2054545668044070308`, version `V2.0` |
| Publish BPM process | `76f6dfd2-8e7e-11f1-aa29-00155d2984a0` |
| Published/stamped file | `9198354916370` |
| Revision reason | `升版 E2E 20260802222723` |
| Target errors | wrapper and chain `targetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]` |
| Final read-only DB verification | PASS: V1 `SUPERSEDED`, V2 `ACTIVE`, master points to V2, publish instance `EFFECTIVE`, DCC signatures valid |

## Historical Blocker

Run `20260802212823` is retained as historical evidence only: it created task-owned V1 `2054545668044070305` and V2 `2054545668044070306`, then blocked at publish approval node `zhaojie` with `/admin-api/bpm/task/approve` HTTP `500`. No API-only, SQL mutation, deletion, or admin workaround was used. This blocker was superseded by the successful full rerun `20260802222723`.

## Browser Evidence

- Browser user: non-admin `wangsiyu`.
- Controlled browser path: `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-REV-FULL-20260802-20260802222723&status=ACTIVE&pageNo=1&pageSize=10`.
- Controlled browser current row: ID `2054545668044070308`, version `V2.0`, status `ACTIVE`; old V1 ID `2054545668044070307` was not returned as a current active row.
- Detail opened from browser: `/dcc/controlled-file/detail/2054545668044070308?traceability=1&from=browser...`.
- Traceability detail: page loaded V2 `ACTIVE`, version history contained V1 `SUPERSEDED` and V2 `ACTIVE`, and revision reason `升版 E2E 20260802222723` was visible.
- Screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png` and `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.

## Approval And Signature Evidence

- V1 original release approvals: 4 completed DCC tasks by non-admin users `zhaohaichen` / `zhaojie` / `zhaomingyu` / `wangsiyu`.
- V2 revision approvals: 4 completed DCC tasks by non-admin users `zhaohaichen` / `zhaojie` / `zhaomingyu` / `wangsiyu`.
- DCC electronic signatures: 8 records across V1/V2, all `signatureMode=PASSWORD`, `passwordVerified=1`, `evidenceStatus=VALID`.
- Publish approval: 4 completed BPM tasks in process `76f6dfd2-8e7e-11f1-aa29-00155d2984a0` by assignees `376`, `1074`, `424`, `910250`.
- Publish approver selection evidence: first three publish nodes selected the next non-admin approver explicitly; final node completed without next approver.

## Acceptance Status

- PASS: no deletion was used as obsolete.
- PASS: no manual obsolete approval was used for this clarified path.
- PASS: no admin account was used.
- PASS: no API-only or SQL mutation was used to change status, approvals, signatures, master pointer, or files.
- PASS: latest requested fresh rerun completed publish approval to final effective state.
- PASS: controlled browser final effect was verified; V1 is not returned as current effective and V2 is opened as current active.
- PASS: final read-only DB verification completed for file status, master pointer, approval tasks, signatures, and controlled-browser result.

## Manual Obsolete Note

Earlier manual “作废当前版本” approval remains out of the clarified scope. That path was separately blocked by missing runtime `OBSOLETE` approval policy and was not bypassed. The accepted path for this task is revision publish automatic invalidation: V1 `SUPERSEDED`, V2 `ACTIVE`, master points to V2, and controlled browsing no longer exposes V1 as current effective.
