# Verification Report

## Result

PASS for the clarified chain: revision publish makes the old current version automatically invalid/作废口径为 `SUPERSEDED`.

Manual “作废当前版本” approval remains BLOCKED by missing runtime OBSOLETE policy, but the user clarified that this is not the path to verify now.

## Scope

- Scenario: DCC 文控文件升版本后，旧版本自动失效/不再作为当前有效文件展示。
- Verified path: V1.0 original release -> V2.0 revision upload -> non-admin approval/signature -> publish approval -> V1.0 `SUPERSEDED` -> V2.0 `ACTIVE` -> master current active V2 -> controlled browser and traceability verification.
- Environment: local `int_main`, frontend `http://127.0.0.1:8081`, backend `http://127.0.0.1:48081`, Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Credential handling: password was injected only through `DCC_E2E_PASSWORD` PowerShell expression; no plaintext password is recorded.
- Final result JSON: `E:\IntRuoyi\doc\tasks\20260802-dcc-controlled-file-obsolete-e2e\revision-auto-obsolete-e2e-result.json`.

## Controlled File Evidence

| Field | Evidence |
| --- | --- |
| File number | `CODX-DCC-REV-FULL-20260802-20260802091213` |
| Master ID | `2054545668044062882` |
| Master current active version | `2054545668044070272` |
| Old version / V1 | `2054545668044070271`, `V1.0`, change type `NEW`, final status `SUPERSEDED` |
| New version / V2 | `2054545668044070272`, `V2.0`, change type `REVISION`, final status `ACTIVE` |
| V1 successor | `2054545668044070272` |
| V2 published file ID | `9198354916362` |
| V2 stamped file ID | `9198354916362` |
| Revision reason / traceability remark | `升版 E2E 20260802091213` |

## Real Playwright Evidence

- Login/browser user: non-admin `wangsiyu`.
- Browser path: `/dcc/controlled-file/browser?scope=global&keyword=CODX-DCC-REV-FULL-20260802-20260802091213&status=ACTIVE&pageNo=1&pageSize=10`.
- Controlled browser result: current row ID `2054545668044070272`, version `V2.0`, status `ACTIVE`; old V1 ID `2054545668044070271` was not returned as a current active row.
- Detail opened from browser: `/dcc/controlled-file/detail/2054545668044070272?traceability=1&from=browser...`.
- Traceability detail result: page loaded V2 `ACTIVE`, version history contained V1 `SUPERSEDED` and V2 `ACTIVE`, and the revision reason `升版 E2E 20260802091213` was visible.
- Target-page health: `targetNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- Screenshots: `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\browser-current-v2.png` and `E:\IntRuoyi\doc\tasks\20260802-dcc-revision-publish-real-e2e\detail-version-history.png`.

## Approval And Signature Evidence

- V1/V2 upload-revision approval tasks: 8 completed DCC tasks across 文控审核、审核会签、批准、文控批准.
- V2 DCC electronic signature users: `zhaohaichen` / 赵海辰, `zhaojie` / 赵杰, `zhaomingyu` / 赵明玉, `wangsiyu` / 王思雨.
- V2 DCC signature status: all records are `PASSWORD`, `passwordVerified=1`, `evidenceStatus=VALID`.
- Publish form action instance: `437`, status `EFFECTIVE`, object ID `2054545668044070272`, object version `V2.0`.
- Publish BPM process: `6f007746-8e52-11f1-ada6-00155d2984a0`, with 4 completed non-admin tasks: 文控审核 `376`, 审核会签 `1074`, 批准 `424`, 文控批准 `910250`.
- BPM signature table: no separate `bpm_approval_signature_record` rows for this publish process; this publish path uses DCC electronic signature evidence, which is present and valid.

## Read-Only DB Evidence

- V1 final status: `SUPERSEDED`; `supersededByFileId=2054545668044070272`.
- V2 final status: `ACTIVE`; `supersededByFileId=null`.
- Master status: `ACTIVE_CHAIN`; `currentActiveControlledFileId=2054545668044070272`.
- Published/stamped file linkage: V2 has `publishedFileId=9198354916362` and `stampedFileId=9198354916362`.
- Publish instance: `id=437`, `status=EFFECTIVE`, `objectVersion=V2.0`.
- Approval/signature rows: DCC signature rows for V1 and V2 are present with valid password-verification evidence.

## Acceptance Result

- 文件 ID: old V1 `2054545668044070271`; new current V2 `2054545668044070272`.
- 作废/失效前口径: V1 was the original current effective version in the release chain.
- 作废/失效后状态: V1 is `SUPERSEDED`; V2 is `ACTIVE`.
- Master 当前有效版本: `2054545668044070272`, no longer V1.
- 受控浏览验证结果: ACTIVE browser list returns/opens V2; V1 is not available as the current active row.
- 版本历史/追溯: V1/V2 are visible in version history; V1 status `SUPERSEDED`, V2 status `ACTIVE`, revision reason visible.
- 审批/签名证据: V1/V2 upload-revision approval and V2 DCC electronic signatures are complete and valid; publish BPM approval is effective.

## Manual Obsolete Note

- Earlier manual obsolete path was tested only until the real page policy-resolution step and remains BLOCKED: `/form-center/actions/resolve` returned `No published business approval policy matched action OBSOLETE`.
- No API-only, SQL status update, admin account, direct deletion, or forced obsolete mutation was used.
- This blocker does not invalidate the clarified acceptance path because the user requested the升版自动作废/失效链路 instead of manual obsolete approval.
