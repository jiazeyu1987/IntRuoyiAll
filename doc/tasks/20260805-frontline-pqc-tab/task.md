# 20260805-frontline-pqc-tab

## Task Goal

将批次执行页面内部 tab 中的“PQC填写”提取为独立页签，独立入口名称为“一线PQC”，并确保 admin 登录后可以看到该页签；原批次执行页面内部 tab 不再显示“PQC填写”。

## Milestones

- [x] M1: 定位批次执行页签、PQC 填写组件、路由和菜单权限来源。
- [x] M2: 编写并运行最小 RED 静态合同，证明旧结构仍把 PQC 填写放在批次执行内部 tab 且缺少独立“一线PQC”入口。
- [x] M3: 实现独立“一线PQC”入口，复用正式 PQC 填写能力，并移除批次执行内部 tab 展示。
- [x] M4: 完成 GREEN、回归与 admin 可见性验证。
- [ ] M5: 完成收尾、经验沉淀、提交与推送。

## Expected Verification

- 前端静态合同覆盖：批次执行页面不再渲染内部“PQC填写”tab；独立“一线PQC”路由/菜单入口存在；admin 权限响应或菜单配置可见。
- `pnpm ts:check` 或受影响范围的前端类型/静态检查。
- 如本机运行态可用，用 `芋道源码/admin` 真实登录路径确认能看到“一线PQC”页签；若运行态前置缺失，记录明确 blocker。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-frontline-pqc-tab/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

实现与任务范围内验证已完成：本机 `芋道源码/admin` 真实登录可在 eDHR 菜单看到“一线PQC”，点击后进入 `/mes/pro/feedback/edhr-batch-pqc-fill`，页面标题为“一线PQC”，且不再渲染批次执行内部 `PQC填写` tab。

收尾仍待处理：项目级全量 migration gate 被无关 `20260805_erp_nas_table_auto_sync.sql` 的 `type=schema,job` 阻塞；全量 `git diff --check` 被无关 Java 测试和 `docs/powershell-memory.md` 中既有 conflict marker 阻塞。本任务拥有文件的定向 diff 检查已通过。

## Design Constraint Checks

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是正式拆分页面入口和权限/路由配置，不通过隐藏错误或文案替换绕过。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 动态菜单页签重命名门禁：需同步核对前端标题、动态路由/菜单配置、角色或 admin 可见性，禁止只改组件标题掩盖正式菜单仍不可见。
- MES PQC 项目级检验快照门禁：本任务只调整 PQC 页面入口和位置，不修改结构化 `itemResults`、发布规程、检验标准、设备编号、组长复核或汇集事实链路。

## Git Baseline

- 既有脏改动基线提交：`4cd8ec941`，文件：`IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`。

## Verification Summary

- `node tests\e2e\edhr-frontline-pqc-tab-static.spec.js`：PASS。
- `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js`：PASS。
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_frontline_pqc_menu_sql.py IntRuoyiBackend\script\tests\test_mes_edhr_qa_menu_sql.py -q`：6 passed。
- `node tests\e2e\edhr-frontline-pqc-menu-real.e2e.js`：PASS，`芋道源码/admin` 可见并打开“一线PQC”。
- `pnpm ts:check`：PASS。
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py ... --sql-file <依赖闭包 19 个文件>`：PASS，输出 `migration-policy-gate.json`。
