# Verification Report

## Result

E2E BLOCKED.

## Scope

- Target scenario: DCC 文控“文件分发/旧版回收”真实 Playwright E2E。
- Required path: non-admin user, real page distribution/receipt/recovery actions, V1 -> V2 version transition, V2 current effective use, V1 non-misuse, final read-only API/DB reconciliation.
- Safety boundary: no admin account, no API-only substitute, no direct SQL/API insert/update for distribution, receipt, recovery, version state, or approval state.

## Rule Reads

- Read `AGENTS.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/frontend-development.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, and Playwright skill `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`.

## Runtime

- Backend was recovered before continuation: `48081` is owned by Java process `48940`, command line points to `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-170535.jar`, `/actuator/health` returned `UP`, and tenant lookup for `芋道源码` returned tenant ID `1`.
- Frontend `http://127.0.0.1:8081/` returned HTTP 200.
- Playwright used local Chrome at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Password was injected only through `DCC_E2E_PASSWORD`; no plaintext password is written in task artifacts.

## Primary Candidate

- File number: `CODX-DCC-DIST-906104-DISTTENANT1202608030005`.
- Tenant: `芋道源码` / tenant ID `1`.
- Category/directory: `906104 / 其他`, child directory `4.Ohter`.
- V1: controlled file ID `2054545668044070310`, version `V1.0`, final current-run status `PENDING_MATRIX_APPROVAL`.
- Active task before continuation: `批准`, task ID `0cf42401-8e8f-11f1-a5cc-00155d2984a0`, assignee `zhaomingyu / 424`.
- Real page path reached: `/dcc/controlled-file/detail/2054545668044070310?handling=approval&from=approval-center&processInstanceId=0599828d-8e8f-11f1-a5cc-00155d2984a0&taskId=0cf42401-8e8f-11f1-a5cc-00155d2984a0`.
- Blocker: real page opened in detail/read-only mode and did not expose any approval action button. Visible page alerts included `受控打印动作投影缺失` and `无受控打印权限 当前账号缺少受控打印菜单权限，或该文件类别未授予 PRINT 打印权限`.
- Evidence: `paper-chain-full-result.json`; screenshot `screenshots/DISTTENANT1202608030005-approve-V1.0-zhaomingyu-button-missing.png`.
- Impact: V1 could not become `ACTIVE`, so this candidate could not produce V2, distribution, receipt/issue, recovery, or old-version non-misuse evidence.

## Configuration Inventory

- Read-only DB inventory found no active category in the current runtime with the complete combination required for this paper recovery E2E: `APPROVE + PRINT + DISTRIBUTE + PAPER distribution rule + bound directory`.
- Tenant `芋道源码` category `906104 / 其他` has `APPROVE/DISTRIBUTE/UPLOAD/VIEW/...` and a distribution rule only for `PUBLIC_FOLDER:253`; it has no active `PRINT` permission and no active `PAPER` distribution rule.
- Tenant `芋道源码` category `907233 / 过程检验规程` has `APPROVE/DOWNLOAD/PRINT/UPLOAD/VIEW`, but no active `DISTRIBUTE` permission and no active distribution rule.
- Existing paper distributions are present in the database, but the current permission/category combination does not allow the required current-run page actions to complete.

## Secondary Candidate Checks

- Existing tenant `芋道源码` file `CODX-DCC-DIST-REC-DISTREC20260802173908`:
- V1 ID `2054545668044070279`, status `SUPERSEDED`, category `907233`; V2 ID `2054545668044070280`, status `ACTIVE`, category `907233`.
- V1 paper distribution ID `4323` and V2 paper distribution ID `4324` remained `PENDING` after failed attempts.
- Real page path reached as `wangsiyu`: `/dcc/controlled-file/detail/2054545668044070280?traceability=1&from=browser&returnTo=/dcc/controlled-file/browser`.
- `wangsiyu` could see the paper distribution row and `确认纸质发放`, but submitting the real page action failed with business code `1080000049`: `Current user cannot acknowledge this paper distribution`.
- `panhaitao` could log in but the same detail page showed `当前版本暂无分发记录`, so it could not act as the issuer/recoverer.
- Evidence: `paper-issue-recovery-wangsiyu-blocked.json`, `paper-issue-recovery-final-result.json`.

- Existing tenant `测试租户` file `CODX-DCC-DIST-900347-DIST90034720260802185602`:
- V1 ID `2054545668044070292`, status `ACTIVE`, category `900347`; V2 ID `2054545668044070295`, status `READY_TO_PUBLISH`, category `900347`.
- V1 paper distribution ID `4334` and V2 paper distribution ID `4337` are `PENDING`.
- Real page publish submit as non-admin `aoteman` reached the publish dialog, but the dialog showed `No published business approval policy matched action PUBLISH`; submitting returned business code `500 / 系统异常`.
- Read-only DB after failure confirmed V2 remained `READY_TO_PUBLISH` and no `bpm_form_action_instance` was created for V2.
- Evidence: `paper-chain-testtenant-result.json`.

## Acceptance Impact

- “分发” action: BLOCKED. Real page submission was attempted, but the only visible current-run paper row rejected the issuer due missing category `DISTRIBUTE` permission for that file category.
- “回收” action: BLOCKED. Recovery requires a paper distribution in `ACKNOWLEDGED` status; current-run acknowledgement could not be completed.
- Old-version non-misuse: BLOCKED for the current run because no candidate reached both V2 `ACTIVE` and V1 recoverable `ACKNOWLEDGED` state during this verification.
- Responsibility evidence: BLOCKED. Upload/approval responsibility exists for the primary candidate through `pengyunfeng`, `zhaohaichen`, `zhaojie`, and pending `zhaomingyu`, but receipt/recovery responsibility could not be produced in this run.

## Required Unblock

- Provide or configure a non-admin-testable category/file chain where the same tenant has a bound directory, published PUBLISH approval policy, active `DISTRIBUTE` permission for the issuer, and an active `PAPER` distribution rule for the target department.
- Alternatively provide an already V2 `ACTIVE` / V1 `SUPERSEDED` file in the same tenant where V1 has an `ACKNOWLEDGED` paper distribution that the non-admin DCC issuer can recover, and V2 has a `PENDING` paper distribution the same issuer can acknowledge.
- Do not unblock by direct SQL/API inserts or status changes for distribution, receipt, recovery, approval, or version state.

## Safety

- No admin account was used for the E2E business flow.
- No SQL/API inserted or updated distribution, receipt, recovery, approval, publish, or version state.
- API/DB usage was limited to read-only runtime diagnosis and final blocker verification; it did not substitute for page E2E.
