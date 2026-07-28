# 20260728 批记录表单列表产品名称下拉筛选

## Task Goal

将批记录表单列表顶部“产品名称”快速筛选值输入框改为可输入的候选下拉框：点击显示当前批记录表单目录中实际存在的产品名称，点击候选后立即过滤；手动输入或复制产品名称后仍通过查询按钮过滤。后续按用户截图要求移除同一工具栏红框中的“批量删除”按钮，并按用户确认修正“填写人”列点击行为：点击应显示 `批记录表单填写人设置` 小弹窗以更换填写人。

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
- `node tests/e2e/batch-record-form-latest-version-switch-static.spec.js`
- `node tests/e2e/batch-record-force-unbind-delete-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260728-batch-record-product-name-dropdown/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260728-batch-record-product-name-dropdown/backend-api-evidence.md`
- 真实 E2E：本机 `8081/48081` 与登录前置可用时，通过 Playwright 在真实页面验证点击候选自动过滤、手输后点击查询过滤。

## Applicable Gates

- 严格无 fallback：候选接口和前端请求不得用 mock、默认成功、空数据兜底或静默吞错掩盖失败。
- 前端静态契约隔离：当前需求新增聚焦静态合同，不为通过既有宽合同改无关行为。
- Element Plus 选择框显示门禁：字段、条件、产品名称输入区和候选下拉必须有专用布局合同，不能只靠 tooltip 或默认省略宽度。
- 批记录三类配置术语契约：本任务只修改批记录表单列表产品名称筛选，不触碰表单槽位 `formBindings` 或工序开始配置。
- PowerShell / Git 门禁：脏工作区需先独立基线提交；PowerShell 不使用 `&&`；Maven `-D` 参数整体加引号。

## Current Status

blocked

## Completed Work

- 后端源码已包含 `GET /admin-api/mes/pro/batch-record-report/product-name-options`，委托 `MesProBatchRecordReportService#getProductNameOptions(keyword, latestVersionOnly)`。
- 服务候选口径复用批记录表单列表可见视图、Jimu 报表存在性过滤、版本产品拆行、`latestVersionOnly` 和 `productName` 包含匹配。
- 前端 `BatchRecordReportApi` 已新增 `getProductNameOptions(keyword?, latestVersionOnly?)`。
- 批记录表单列表 `productName` 快速筛选已改为 `autocomplete`，设置 `triggerOnFocus: true`，候选选择触发快速过滤查询，手工输入仍保留“查询”按钮。
- 已修复快速过滤控件宽度收缩：字段显示完整“产品名称”、条件显示完整“包含”、产品名称输入区加宽且不收缩，候选下拉可换行完整显示较长产品名称。
- 已按最新截图移除工具栏“批量删除”按钮，并清理仅服务于该按钮的多选列、选中状态和批量删除处理函数。
- 已修正填写人列点击行为：不再因 `fillAssignments` 打开全屏 `填写配置`，而是打开 `批记录表单填写人设置` 小弹窗；右侧 `填写配置` 按钮仍进入全屏配置。
- 已补齐后端 JUnit 和前端静态合同，并通过定向验证。

## Remaining Blockers

- 本机真实 E2E 未通过：`8081/48081` 运行态可达，但页面自身请求 `/admin-api/mes/pro/batch-record-report/product-name-options` 返回业务码 `404`；同一页面列表 `/page` 返回业务码 `0`、总数 `320`、首屏 `20` 行且均有非空产品名称，说明当前 `48081` 后端进程未加载本次新增 Controller 路由。
- Git closeout 未完成：当前分支 `int_main` 本地 ahead 6 / behind 6，且任务外文件 `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue` 存在未提交并行改动；本任务未回滚、覆盖或提交该并行改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；新增正式只读候选接口，候选口径与列表实际可见数据一致。
- `是否存在临时补丁或绕过`：否。
