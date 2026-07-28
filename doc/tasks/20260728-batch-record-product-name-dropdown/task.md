# 20260728 批记录表单列表产品名称下拉筛选

## Task Goal

将批记录表单列表顶部“产品名称”快速筛选值输入框改为可输入的候选下拉框：点击显示当前批记录表单目录中实际存在的产品名称，点击候选后立即过滤；手动输入或复制产品名称后仍通过查询按钮过滤。

## Milestones

1. 任务启动与基线隔离：记录 BDD、读取适用门禁、保存既有脏工作区基线提交。
2. RED：新增后端候选接口测试和前端静态合同，确认现状失败。
3. GREEN：实现后端只读候选接口、前端 API wrapper、快速筛选 autocomplete 与点击候选自动查询。
4. REGRESSION：运行定向 Maven、前端静态合同、`pnpm ts:check` 与技能证据校验；能满足本机运行态前置时执行真实 Playwright 验证。
5. Closeout：更新证据、经验沉淀、清理本任务临时产物、提交并推送。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js`
- `node tests/e2e/edhr-batch-record-form-list-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260728-batch-record-product-name-dropdown/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260728-batch-record-product-name-dropdown/backend-api-evidence.md`
- 真实 E2E：本机 `8081/48081` 与登录前置可用时，通过 Playwright 在真实页面验证点击候选自动过滤、手输后点击查询过滤。

## Applicable Gates

- 严格无 fallback：候选接口和前端请求不得用 mock、默认成功、空数据兜底或静默吞错掩盖失败。
- 前端静态契约隔离：当前需求新增聚焦静态合同，不为通过既有宽合同改无关行为。
- 批记录三类配置术语契约：本任务只修改批记录表单列表产品名称筛选，不触碰表单槽位 `formBindings` 或工序开始配置。
- PowerShell / Git 门禁：脏工作区需先独立基线提交；PowerShell 不使用 `&&`；Maven `-D` 参数整体加引号。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；新增正式只读候选接口，候选口径与列表实际可见数据一致。
- `是否存在临时补丁或绕过`：否。

