# Feature

将批次执行内部 `PQC填写` tab 拆出为独立页面入口 `一线PQC`。页面继续复用正式 PQC 填写组件，不引入 mock、默认成功或隐藏错误。

## Acceptance

- A1: 批次执行内部 tabs 不再显示 `PQC填写`，也不保留 `pqc` tab 路由映射。
- A2: 独立路由 `/mes/pro/feedback/edhr-batch-pqc-fill` 的标题为 `一线PQC`，组件仍为 `BatchPqcFillPage.vue` / `MesProEdhrBatchPqcFill`。
- A3: `BatchPqcFillPage.vue` 显示独立标题 `一线PQC`，并保留 `<FrontlineFixedTemplatePanel mode="pqc" />`。
- A4: 动态菜单迁移新增 `一线PQC`，写入租户套餐和 admin 角色绑定。
- A5: 本机 `芋道源码/admin` 真实登录后可以看到并打开 `一线PQC`。

## UI Entry Points

- Menu: eDHR 批记录 > 一线PQC。
- Route: `/mes/pro/feedback/edhr-batch-pqc-fill`。
- Component: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue`。
- Reused panel: `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue` with `mode="pqc"`。

## API Contracts And Data States

- Permission: `mes:pro-edhr-batch-execution:query` remains unchanged.
- Dynamic menu row: `system_menu.id=900438`，`name=一线PQC`，`path=/mes/pro/feedback/edhr-batch-pqc-fill`。
- Data state: local DB migration applied only for verification; no PQC business data writes during admin visibility E2E.

## BDD:

- BDD: PQC独立页签 -> Given admin 登录系统并拥有 MES 批次执行相关权限 / When 打开 eDHR 菜单 / Then 能看到独立入口 `一线PQC` 并进入正式 PQC 填写页面。
- BDD: 批次执行内部移除PQC -> Given 用户进入批次执行页面 / When 查看页面内部 tab / Then 不再出现 `PQC填写` 内部 tab。
- BDD: 正式入口不降级 -> Given PQC 填写依赖正式路由、组件和权限配置 / When 独立入口加载 / Then 不使用 mock、默认成功或吞异常绕过缺失配置。

## RED:

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py -q` -> FAIL，新增 SQL 合同断言捕获目标迁移缺少 `2 AS type`，真实执行会造成 `INSERT` 列和值数量错位。

## GREEN:

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py -q` -> PASS，3 passed。
- `node tests\e2e\edhr-frontline-pqc-tab-static.spec.js` -> PASS。
- `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js` -> PASS。
- `node tests\e2e\edhr-frontline-pqc-menu-real.e2e.js` -> PASS，`芋道源码/admin` 可见并打开 `一线PQC`。
- `pnpm ts:check` -> PASS。

## Verification

- Responsive/accessibility/loading/empty/error: 本任务未改 PQC 面板业务渲染逻辑；独立页面只新增标题容器并复用既有正式面板。
- Permission check: admin 登录后权限响应包含 `一线PQC`，左侧 eDHR 菜单可见该项。
- E2E path: Playwright 真实页面点击 `一线PQC`，URL 为 `/mes/pro/feedback/edhr-batch-pqc-fill`，标题 `[data-edhr-frontline-pqc-page-title]` 可见，`data-edhr-batch-record-tabs` 数量为 0。
- Migration check: 目标迁移及依赖闭包 19 个文件通过 release migration policy gate。

## Blockers

- Full repository migration gate is blocked by unrelated `20260805_erp_nas_table_auto_sync.sql` metadata.
- Full repository `git diff --check` is blocked by unrelated conflict markers outside this task scope.
- Task-owned checks and admin visibility E2E passed.
