# Frontend Feature Evidence

## Feature

- Goal: 按用户截图删除 Profile 配置页签中 `ERP表格自动同步` 页面的顶部汇总区、`Job 调度` 明细区和 `最近执行记录` 明细区。
- Non-goal: 不改变 ERP 正式同步链路、Job API、权限、后端接口、数据库 schema 或 NAS 页签删除后的既有行为。
- Entry point: `Profile` 页面配置页签下的 `ERP表格自动同步` 组件。
- Owned files: `IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue`、`IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js`。

## Acceptance

- AC1: 页面不再显示 `配置来源 / 已选表格 / 每日 Cron / 启用 Job / 最近状态 / 最近开始时间` 汇总区。
- AC2: 页面不再显示 `Job 调度` 表格及 `处理器 / Job ID / Job 状态 / 当前 Cron` 等内部调度列。
- AC3: 页面不再显示 `最近执行记录` 表格及 `运行编号 / 触发 / 状态 / 开始时间 / 数量 / 失败原因` 等运行明细列。
- AC4: 页面仍保留 `启用自动同步`、`每日开始时间`、ERP 表格列表、`保存配置`、`立即执行一次`，并继续显示 ERP 表格名称、本地页签名称、最近一次同步时间。

## BDD

- BDD: ERP table sync hides summary panel -> Given 用户打开 Profile 配置页签的 ERP 表格自动同步, When 页面渲染, Then 页面不显示配置来源、已选表格、每日 Cron、启用 Job、最近状态、最近开始时间汇总区。
- BDD: ERP table sync hides job schedule details -> Given 用户只需要配置同步表格和时间, When 页面渲染, Then 页面不显示 Job 调度明细表、处理器、Job ID、Job 状态、当前 Cron 等内部调度信息。
- BDD: ERP table sync hides recent run records -> Given 页面保留 ERP 表格列表的最近一次同步时间, When 页面渲染, Then 页面不再显示最近执行记录表和失败原因列。

## RED

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL, expected reason: 合同禁止截图中的额外展示区后，旧组件仍包含 `配置来源`、`Job 调度`、`最近执行记录` 等内容。

## GREEN

- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。

## Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- `pnpm ts:check` in `IntRuoyiFronted` -> BLOCKED, unrelated current workspace errors in `src/views/mes/pro/processpool/QaRegulationPage.vue`.
- UI states: 加载态仍由 `loading/jobLoading/watermarkLoading` 覆盖，空水位显示 `-`，API 错误仍通过 `ElMessage.error` 暴露。
- Permission: 权限入口沿用 Profile 配置页签访问控制，本次不新增路由或菜单权限。

## Blockers

- `pnpm ts:check` 被无关 QA 规程页面当前类型错误阻塞：`finalInspectionRequired`、`finalInspectionNotApplicableReason`、`qaRulesQuery` 缺失。
