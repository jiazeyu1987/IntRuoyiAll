# Verification Report

## Result

Latest full fresh E2E rerun is **BLOCKED**, not PASS.

The clarified business path remains: DCC “作废/废止” for this requirement does not run a manual obsolete approval; it is verified by publishing a V2 revision so the old V1 current version automatically becomes `SUPERSEDED`. A prior full business-state run proved that state transition, but it is not a clean release-grade E2E pass because the underlying approval chain still recorded pageerrors. The latest full rerun then blocked earlier on the V1 approval detail page runtime error.

Manual “作废当前版本” approval remains out of current scope after the user clarification. It is still separately blocked by missing runtime OBSOLETE approval policy and was not bypassed.

## Scope

- Scenario: DCC 文控文件升版本后，旧版本自动失效/不再作为当前有效文件展示。
- Required path: V1.0 original release -> V2.0 revision upload -> non-admin approval/signature -> publish approval -> V1.0 `SUPERSEDED` -> V2.0 `ACTIVE` -> master current active V2 -> controlled browser and traceability verification.
- Environment: local `int_main`, frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`, MinIO ready on `http://127.0.0.1:9000/minio/health/ready`.
- Credential handling: password was injected only through the approved `DCC_E2E_PASSWORD` PowerShell expression; no plaintext password is recorded.
- Latest result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\full-rerun-e2e-result-20260802194027.json`.

## Latest Full Rerun

| Field | Evidence |
| --- | --- |
| Run ID | `20260802194027` |
| File number | `CODX-DCC-REV-FULL-20260802-20260802194027` |
| Result | `BLOCKED`, command exit code `1` |
| Completed phase | V1 original upload only |
| V1 controlled file | `2054545668044070296`, `V1.0`, change type `NEW`, current status `PENDING_DOC_CONTROL_REVIEW` |
| Current task | `DOC_CONTROL_REVIEW`, assignee `376`, unfinished |
| Blocker | Real approval detail page did not render `审批阶段进度`; Playwright timed out waiting for it |
| Runtime error | `Cannot read properties of undefined (reading 'visible')` from `src/views/dcc/controlled-file/detail/index.vue` |
| Target errors | chain `pageErrors=19`, `consoleErrors=1`, `targetNetworkFailures=0` |
| Impact | V2 was not created in this latest rerun; master was not switched; controlled browser final effect could not be verified for this run |

Read-only DB impact check confirms this run left only the task-owned V1 under review; no SQL/API status mutation, admin account, direct deletion, or forced obsolete operation was used.

## Prior Business-State Evidence

| Field | Evidence |
| --- | --- |
| Run ID | `20260802193142` |
| File number | `CODX-DCC-REV-FULL-20260802-20260802193142` |
| Wrapper result | `full-rerun-e2e-result.json` recorded business phases as `PASS` |
| Master ID | `2054545668044062902` |
| Master current active version | `2054545668044070294` |
| Old version / V1 | `2054545668044070293`, `V1.0`, change type `NEW`, final status `SUPERSEDED` |
| New version / V2 | `2054545668044070294`, `V2.0`, change type `REVISION`, final status `ACTIVE` |
| V1 successor | `2054545668044070294` |
| V2 published/stamped file ID | `9198354916366` |
| Revision reason / traceability remark | `升版 E2E 20260802193142` |
| Clean-gate caveat | underlying chain result recorded publish approval pageerrors: `Cannot read properties of null (reading 'nextSibling')` |

This prior run proves the desired business state can be reached through the real revision-publish path, but the current task gate requires target approval/DCC chain `pageErrors=0`, so it is retained as evidence rather than final PASS.

## Browser Evidence

- Prior completed browser user: non-admin `wangsiyu`.
- Prior browser path: `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-REV-FULL-20260802-20260802193142&status=ACTIVE&pageNo=1&pageSize=10`.
- Controlled browser result: current row ID `2054545668044070294`, version `V2.0`, status `ACTIVE`; old V1 ID `2054545668044070293` was not returned as a current active row.
- Detail opened from browser: `/dcc/controlled-file/detail/2054545668044070294?traceability=1&from=browser...`.
- Traceability detail result: page loaded V2 `ACTIVE`, version history contained V1 `SUPERSEDED` and V2 `ACTIVE`, and the revision reason `升版 E2E 20260802193142` was visible.
- Screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png` and `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.

## Approval And Signature Evidence

- Prior completed run V1/V2 upload-revision approvals: 8 completed DCC tasks across 文控审核、审核会签、批准、文控批准.
- Prior V2 DCC electronic signature users: `zhaohaichen` / 赵海辰, `zhaojie` / 赵杰, `zhaomingyu` / 赵明玉, `wangsiyu` / 王思雨.
- Prior V2 DCC signature status: all records are `PASSWORD`, `passwordVerified=1`, `evidenceStatus=VALID`.
- Prior publish form action instance: `439`, status `EFFECTIVE`, object ID `2054545668044070294`, object version `V2.0`.
- Prior publish BPM process: `e9f1447a-8e65-11f1-93ff-00155d2984a0`, with 4 completed non-admin tasks: 文控审核 `376`, 审核会签 `1074`, 批准 `424`, 文控批准 `910250`.
- Latest blocked run has no completed approval/signature evidence beyond the pending first V1 `DOC_CONTROL_REVIEW` task.

## Acceptance Status

- Current acceptance result: **BLOCKED** until the DCC approval detail runtime error is fixed and a fresh complete Playwright chain passes with target DCC/approval `pageErrors=0`, `consoleErrors=0`, and `targetNetworkFailures=0`.
- Business-state evidence available: old V1 `2054545668044070293` became `SUPERSEDED`; new V2 `2054545668044070294` became `ACTIVE`; master points to V2;受控浏览 no longer exposes V1 as current active.
- Latest clean rerun evidence missing: V2 creation, publish approval completion, old-version automatic invalidation, final controlled browser verification, and traceability verification for run `20260802194027`.

## Manual Obsolete Note

- Earlier manual obsolete path was tested only until the real page policy-resolution step and remains BLOCKED: `/form-center/actions/resolve` returned `No published business approval policy matched action OBSOLETE`.
- No API-only, SQL status update, admin account, direct deletion, or forced obsolete mutation was used.
- This blocker does not redefine the clarified acceptance path; the current blocker is the real approval detail page runtime error on the revision-publish chain.
