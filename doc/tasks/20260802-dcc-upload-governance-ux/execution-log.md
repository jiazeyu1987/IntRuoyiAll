# 20260802 DCC 上传治理体验优化 Execution Log

## User Intent

- 用户要求优化五项 DCC 上传链路体验：上传前置校验、受控浏览联动、签核追溯产品化、审批中心行增强、签名失败诊断。
- 任务必须保留真实 DCC 上传、分类、审批、电子签名、受控浏览链路，不引入 fallback、mock 或 API-only 替代真实用户路径。

## BDD

- BDD: 上传前置校验 -> Given 上传人填写文件编号、版本、分类和目录，When 提交前检查，Then 页面提前展示编号版本重复、分类上传权限、审批链路完整性和最终受控浏览目录落位状态。
- BDD: 生效文件受控浏览联动 -> Given 原版文件审批后生效，When 用户打开详情页，Then 页面展示受控浏览入口、最终目录、published/stamped 文件 ID 和 master 当前生效版本，并能一键跳转实际受控浏览位置。
- BDD: 签核追溯产品化 -> Given 文件存在上传与四级审批签名记录，When 用户查看详情页，Then 页面统一展示上传人、审核人、签名人、签名时间、签名方式、证据状态、文件哈希和盖章文件信息。
- BDD: 审批中心行增强 -> Given 审批人进入待办列表，When DCC 待办行出现，Then 行内直接显示文件编号、版本、分类、当前节点和是否需要盖章/分发。
- BDD: 签名失败诊断 -> Given 电子签名缺授权、签名图片失效、密码错误或证据快照失败，When 签名失败，Then 页面展示明确阻断原因，不以通用失败文案掩盖。

## Milestone Updates

- 2026-08-02: 创建任务目录。
- 2026-08-02: 读取 frontend-feature-delivery、backend-api-delivery、quality-assurance-test-suite 技能及其合同文件。
- 2026-08-02: 读取前端、后端、数据库、E2E、PowerShell、本机运行态、登录和任务收尾门禁。
- 2026-08-02: 发现工作区已有大量并行脏改动，包括 DCC 详情页；本任务仅追加必要改动，不回滚、不覆盖无关变更。

## Verification Evidence

- RED: node tests/e2e/dcc-upload-governance-ux-static.spec.js -> FAIL, expected missing dcc-upload-preflight-panel before implementation.
- GREEN: node tests/e2e/dcc-upload-governance-ux-static.spec.js -> PASS.
- GREEN: node tests/e2e/dcc-upload-current-version-static.spec.js -> PASS.
- GREEN: node tests/e2e/dcc-upload-category-permission-static.spec.js -> PASS.
- GREEN: node tests/e2e/dcc-detail-signature-view-mode-static.spec.js -> PASS.
- GREEN: node tests/e2e/dcc-approval-center-handling-entry-static.spec.js -> PASS.
- GREEN: node tests/e2e/approval-center-todo-standard-list-static.spec.js -> PASS.
- GREEN: pnpm ts:check -> PASS.
- RED: mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" test -> FAIL, upstream modules had no matching specified tests.
- RED: mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, two old DCC samples lacked formal version/category context.
- GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 10, Failures: 0, Errors: 0.
- GREEN: frontend-feature evidence validator -> PASS.
- GREEN: backend-api evidence validator -> PASS.
- GREEN: quality-assurance evidence validator -> PASS.
- GREEN: real E2E preflight -> PASS, `npx` present, local Chrome present, frontend `http://127.0.0.1:8081/` returned 200, backend `http://127.0.0.1:48081/actuator/health` returned UP, and both listener PIDs belonged to `E:\IntRuoyi` int_main runtime.
- GREEN: node --check doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs -> PASS.
- GREEN: node doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs -> PASS, password supplied via redacted environment variable expression, result written to `doc/tasks/20260802-dcc-upload-governance-ux/e2e-result-real-upload-revision.json`.
- GREEN: upload + revision E2E result -> PASS, file number `CODX-DCC-REV-20260802-20260802034644`, V1 controlled file `2054545668044070263`, V2 controlled file `2054545668044070264`, publish form instance `436`, publish BPM process `02285dac-8e25-11f1-a451-00155d9fd668`.
- GREEN: final business state -> PASS, V1.0 `SUPERSEDED`, V2.0 `ACTIVE`, master current active controlled file `2054545668044070264`, publish instance `EFFECTIVE`, upload approval task count `8`, publish approval task count `4`.
- GREEN: five-account non-admin actor chain -> PASS, uploader `pengyunfeng`; approvers `zhaohaichen`, `zhaojie`, `zhaomingyu`, `wangsiyu`; publisher `wangsiyu`.
- GREEN: sensitive scan of DCC task evidence directories -> PASS, no matches for known password literal, bearer token, access token, refresh token, password env var, or command-line password pattern.
- INFO: real E2E recorded 0 target network failures and 0 console errors; BPM process detail emitted 8 non-blocking pageerrors for `Cannot read properties of undefined (reading 'markers')` while target approval controls and final DB assertions passed.
- GREEN: experience-preflight -> PASS, existing `docs/e2e-rules.md#dcc-文控审批处理入口门禁` and `docs/experience-index.md` already cover DCC upload/revision, non-admin actor chain, publish permissions, `UserSelectV2`, and `APPROVE_USER_SELECT`; no new durable experience document required.

## Blockers

- Verification blockers resolved.
- Closeout blocker: workspace has extensive unrelated dirty changes and branch is already ahead of origin; no baseline commit, cleanup apply, task commit, or push was performed in this turn.

## Implementation Summary

- Added upload preflight panel using currentVersionInfo, category canUpload, approval/signoff position IDs, and selected upload directory path.
- Added detail controlled-browser linkage cards for final path, publishedFileId, stampedFileId, and master current active version.
- Added signature trace section with uploader/signature summary rows and CSV/print actions.
- Added centralized signature failure diagnostics for authorization, signature image, password, and evidence/snapshot/hash failures.
- Added DCC approval-center businessContextTags from backend and frontend row rendering.
- Added backend detail projection for source/original/published/stamped file IDs.
