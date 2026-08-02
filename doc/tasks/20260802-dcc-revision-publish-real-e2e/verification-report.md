# 20260802 DCC 升版修订发布真实 E2E Verification Report

## Status

- Result: `PASS`
- Verified at: `2026-08-02 17:30:14 +08:00`
- Environment: local `int_main`, frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Credential handling: non-admin accounts only; password was injected through `DCC_E2E_PASSWORD` PowerShell expression and was not written to logs or reports.

## Scope

- Requested path: ACTIVE V1.0 controlled file -> V2.0 revision upload -> submit approval -> non-admin approvals/signatures -> effective publish -> V1.0 superseded -> master current version V2.0 -> controlled browser and version history verification -> read-only DB audit.
- Constraint honored: no admin account, no API-only substitute, no direct SQL/API state update, no direct approval/status/master pointer mutation.
- Final evidence file: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\e2e-result.json`.
- Screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png`, `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.

## Controlled File Evidence

| Field | Evidence |
| --- | --- |
| File number | `CODX-DCC-REV-FULL-20260802-20260802091213` |
| Master ID | `2054545668044062882` |
| Master current active version | `2054545668044070272` |
| V1 controlled file | `2054545668044070271`, `V1.0`, `NEW`, final status `SUPERSEDED` |
| V2 controlled file | `2054545668044070272`, `V2.0`, `REVISION`, final status `ACTIVE` |
| V1 successor | `2054545668044070272` |
| V2 published file ID | `9198354916362` |
| V2 stamped file ID | `9198354916362` |
| Revision reason / submit remark | `升版 E2E 20260802091213` |

## Approval And Signature Evidence

- Upload/revision approval tasks: `8` completed DCC tasks across V1.0 and V2.0.
- V2 DCC signature users: `zhaohaichen` / 赵海辰, `zhaojie` / 赵杰, `zhaomingyu` / 赵明玉, `wangsiyu` / 王思雨.
- V2 DCC signature mode/status: all four records are `PASSWORD`, `passwordVerified=1`, `evidenceStatus=VALID`.
- Publish form action instance: `437`, status `EFFECTIVE`, object ID `2054545668044070272`, object version `V2.0`.
- Publish BPM process: `6f007746-8e52-11f1-ada6-00155d2984a0`, with four completed non-admin tasks: 文控审核 `376`, 审核会签 `1074`, 批准 `424`, 文控批准 `910250`.
- BPM signature table: no `bpm_approval_signature_record` rows for this publish process; this BPM publish path did not require a separate canvas/image BPM signature. The required DCC electronic signature evidence for V2 approval is present and valid.

## Controlled Browse Evidence

- Browser path: `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-REV-FULL-20260802-20260802091213&status=ACTIVE&pageNo=1&pageSize=10`.
- Browser list result: current row ID `2054545668044070272`, version `V2.0`, status `ACTIVE`; V1 ID `2054545668044070271` was not returned as a current active browse row.
- Browser detail path: controlled browser file-number link opened `/dcc/controlled-file/detail/2054545668044070272?viewer=1`.
- Detail verification: preview layout rendered for V2 `ACTIVE`, published/stamped file IDs are both `9198354916362`.
- Version traceability: version dialog showed `V1.0` and `V2.0`; detail API `versionHistory` contained V1 `SUPERSEDED` and V2 `ACTIVE`; submit remark showed `升版 E2E 20260802091213`.

## Commands

- `node --check doc/tasks/20260802-dcc-revision-publish-real-e2e/dcc-revision-publish-real-e2e.cjs` -> PASS.
- Real Playwright chain: V1 upload/approval, V2 revision upload/approval, publish submit, publish BPM approval -> PASS in the same task using non-admin accounts.
- Supplemental Playwright browser/history verification with `DCC_E2E_USE_EXISTING_CHAIN=1` -> PASS.
- Final read-only DB verification in `e2e-result.json` -> PASS.
- Sensitive scan for plaintext password/token patterns in this task directory -> PASS, no matches.

## Cleanup / Recovery

- Final PASS test data is task-owned and intentionally retained for audit traceability; no SQL/API cleanup was used.
- A failed retry created task-owned partial data `CODX-DCC-REV-FULL-20260802-20260802091853`, controlled file `2054545668044070274`, status `PENDING_MATRIX_REVIEW`, with one completed reviewer task and one pending reviewer task. It was not SQL/API-cleaned because the task forbids direct state or approval mutation.
- `chain-result.json` is a failed retry artifact and is not final PASS evidence; final evidence is `e2e-result.json`.
