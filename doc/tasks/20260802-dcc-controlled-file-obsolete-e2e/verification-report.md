# Verification Report

## Result

Latest full fresh E2E rerun is **PASS**.

The clarified DCC “作废/废止” path was verified as requested: no manual obsolete approval was used. A task-owned V1 controlled file was released first, then a V2 revision was uploaded, approved, signed, and published. The old V1 automatically became `SUPERSEDED`, the new V2 became `ACTIVE`, master current active version switched to V2, and controlled browsing no longer exposes V1 as the current effective file.

## Scope

- Scenario: DCC 文控文件升版本后，旧版本自动失效/不再作为当前有效文件展示。
- Required path: V1.0 original release -> V2.0 revision upload -> non-admin approval/signature -> publish approval -> V1.0 `SUPERSEDED` -> V2.0 `ACTIVE` -> master current active V2 -> controlled browser and traceability verification.
- Environment: local `int_main`, frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`, MinIO ready on `http://127.0.0.1:9000/minio/health/ready`.
- Credential handling: password was injected only through the approved `DCC_E2E_PASSWORD` PowerShell expression; no plaintext password is recorded.
- Result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802201023.json`.
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
| Full real E2E | `full-rerun-e2e-result-20260802201023.json` -> PASS |

The focused contract locks the previous approval detail rendering failure: no `})const openControlledBrowserLocation` glue remains, the real approver anchor `审批阶段进度` remains present, and every detail dialog `*.visible` v-model, including `controlledPrintDialog.visible`, has initialized reactive state.

## Latest Full Rerun

| Field | Evidence |
| --- | --- |
| Run ID | `20260802201023` |
| File number | `CODX-DCC-REV-FULL-20260802-20260802201023` |
| Result | `PASS`, command exit code `0` |
| Target errors | wrapper and chain `targetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]` |
| V1 controlled file | `2054545668044070300`, `V1.0`, change type `NEW`, final status `SUPERSEDED` |
| V2 controlled file | `2054545668044070301`, `V2.0`, change type `REVISION`, final status `ACTIVE` |
| Master | `2054545668044062907`, `ACTIVE_CHAIN`, current active version `2054545668044070301` |
| V1 successor | `2054545668044070301` |
| Publish form instance | `440`, status `EFFECTIVE`, object `2054545668044070301`, version `V2.0` |
| Publish BPM process | `53fbed5f-8e6b-11f1-93ff-00155d2984a0` |
| Published/stamped file | `9198354916368` |
| Revision reason | `升版 E2E 20260802201023` |

## Browser Evidence

- Browser user: non-admin `wangsiyu`.
- Controlled browser path: `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-REV-FULL-20260802-20260802201023&status=ACTIVE&pageNo=1&pageSize=10`.
- Controlled browser current row: ID `2054545668044070301`, version `V2.0`, status `ACTIVE`; old V1 ID `2054545668044070300` was not returned as a current active row.
- Detail opened from browser: `/dcc/controlled-file/detail/2054545668044070301?traceability=1&from=browser...`.
- Traceability detail: page loaded V2 `ACTIVE`, version history contained V1 `SUPERSEDED` and V2 `ACTIVE`, and revision reason `升版 E2E 20260802201023` was visible.
- Screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png` and `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.

## Approval And Signature Evidence

- V1 original release approvals: 4 completed DCC tasks by non-admin users `zhaohaichen` / `zhaojie` / `zhaomingyu` / `wangsiyu`.
- V2 revision approvals: 4 completed DCC tasks by non-admin users `zhaohaichen` / `zhaojie` / `zhaomingyu` / `wangsiyu`.
- DCC electronic signatures: 8 records across V1/V2, all `signatureMode=PASSWORD`, `passwordVerified=1`, `evidenceStatus=VALID`.
- Publish approval: 4 completed BPM tasks in process `53fbed5f-8e6b-11f1-93ff-00155d2984a0` by assignees `376`, `1074`, `424`, `910250`.
- Publish approver selection evidence: first three publish nodes selected the next non-admin approver explicitly; final node completed without next approver.

## Acceptance Status

- PASS: no deletion was used as obsolete.
- PASS: no manual obsolete approval was used for this clarified path.
- PASS: no admin account was used.
- PASS: no API-only or SQL mutation was used to change status, approvals, signatures, master pointer, or files.
- PASS: controlled browser final effect was verified from the real page.
- PASS: final read-only DB verification confirms V1 `SUPERSEDED`, V2 `ACTIVE`, master current V2, publish instance `EFFECTIVE`, approval tasks, and signature evidence.

## Manual Obsolete Note

Earlier manual “作废当前版本” approval remains out of the clarified scope. That path was separately blocked by missing runtime `OBSOLETE` approval policy and was not bypassed. The accepted path for this task is revision publish automatic invalidation: V1 `SUPERSEDED`, V2 `ACTIVE`, master points to V2, and controlled browsing no longer exposes V1 as current effective.
