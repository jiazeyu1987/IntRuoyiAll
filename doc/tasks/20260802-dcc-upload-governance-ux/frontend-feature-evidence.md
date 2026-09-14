# Frontend Feature Evidence - DCC 上传治理体验优化

## Feature goal and non-goals

- Goal: 优化 DCC 上传链路五项用户可见体验：上传前置校验、受控浏览联动、签核追溯、审批中心上下文、签名失败诊断。
- Non-goals: 不重建上传审批流，不绕过后端正式校验，不新增 mock/fallback，不替代真实 E2E 上传链路。

## Requirements and acceptance ids

- REQ-1: 上传提交前展示文件编号/版本重复、分类上传权限、审批人链路、受控浏览目录落位状态。
- REQ-2: 详情页展示受控浏览入口、最终目录、publishedFileId、stampedFileId、master 当前生效版本。
- REQ-3: 详情页展示签核追溯区，支持导出和打印。
- REQ-4: 审批中心 DCC 行展示文件编号、版本、分类、当前节点、盖章/分发上下文。
- REQ-5: 签名失败按未授权、签名图片失效、密码错误、证据快照失败显示明确原因。

## UI entry points, routes, components, and owned files

- Upload page: IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue
- Detail page: IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue
- Signature action helper: IntRuoyiFronted/src/views/dcc/controlled-file/detail/approval-actions.ts
- Approval center: IntRuoyiFronted/src/views/approval-center/index.vue
- API types: IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts, IntRuoyiFronted/src/api/approval-center/index.ts
- Static contract: IntRuoyiFronted/tests/e2e/dcc-upload-governance-ux-static.spec.js

## API contracts and data states

- ControlledFileVO exposes sourceFileId/originalFileId/publishedFileId/stampedFileId for detail linkage.
- ApprovalTaskSummaryVO exposes businessContextTags for DCC approval center rows.
- Upload preflight uses currentVersionInfo, selectedCategory.canUpload, selectedUploadDirectoryPath, approvalPositionIds and signoffPositionIds.

## BDD scenarios

- BDD: 上传前置校验 -> Given 上传人填写文件编号、版本、分类和目录, When 提交前检查, Then 页面提前展示重复、权限、审批链路和目录落位状态。
- BDD: 生效文件受控浏览联动 -> Given 文件审批后生效, When 用户打开详情页, Then 页面展示受控浏览入口、最终目录、published/stamped 文件 ID 和 master 当前生效版本。
- BDD: 签核追溯产品化 -> Given 文件存在上传和审批签名记录, When 查看详情页, Then 页面统一展示签核追溯并支持导出/打印。
- BDD: 审批中心行增强 -> Given 审批人进入待办列表, When DCC 待办行出现, Then 行内显示文件编号、版本、分类、当前节点和盖章/分发状态。
- BDD: 签名失败诊断 -> Given 电子签名缺授权、签名图片失效、密码错误或证据快照失败, When 签名失败, Then 页面展示明确阻断原因。

## RED command and expected failure

- RED: node tests/e2e/dcc-upload-governance-ux-static.spec.js -> FAIL, expected missing data-testid="dcc-upload-preflight-panel" before implementation.

## GREEN command and passing result

- GREEN: node tests/e2e/dcc-upload-governance-ux-static.spec.js -> PASS: DCC upload governance UX static contract.
- GREEN: pnpm ts:check -> PASS.

## Responsive, accessibility, loading, empty, error, and permission checks

- Responsive: 新增上传预检卡片和详情受控浏览卡片提供 1280px/720px 响应式网格。
- Loading: 上传预检在 currentVersionLookupLoading 时显示检查中。
- Empty: 追溯表无记录时显示空状态；导出/打印按钮无数据时禁用并提示。
- Error: 签名 catch 统一进入 resolveDccApprovalSignatureErrorMessage。
- Permission: 分类上传权限继续使用 selectedCategory.value?.canUpload 和既有后端 fail-fast；签名留痕管理权限缺失时不阻断审批任务加载。

## E2E or component verification path

- Static contract covers the five user-visible behaviors and formal data source tokens.
- Real write-type upload E2E was not rerun in this turn; this task focused on UX/code optimization with static and targeted backend verification.

## Blockers and follow-up skills

- Closeout commit/push not performed because workspace contains extensive unrelated dirty changes from parallel tasks; task remains ready_for_closeout rather than completed.

## Acceptance

- ACCEPTED: Five requested UX behaviors are covered by the DCC upload governance UX static contract.
- ACCEPTED: Frontend TypeScript check passes.

## Verification

- PASS: node tests/e2e/dcc-upload-governance-ux-static.spec.js
- PASS: pnpm ts:check
