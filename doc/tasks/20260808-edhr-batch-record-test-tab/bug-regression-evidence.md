# Bug Regression Evidence: admin 看不到批记录测试入口

## Bug

用户反馈“用芋道源码/admin 没有看到 批记录测试 页签”。代码已有隐藏路由和批次执行页面内 tabs，但 admin 可见菜单数据源没有新增 `批记录测试` 菜单项。

## Expected

使用芋道源码/admin 进入 eDHR 菜单时，应能通过正式可见菜单看到 `批记录测试`，并打开 `/mes/pro/feedback/edhr-batch-test` 对应的生产组长测试页。

## Reproduction

- 静态复现：`node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`
- 失败点：缺少 `IntRuoyiBackend\sql\mysql\20260808_mes_edhr_batch_record_test_menu.sql`，无法证明 `system_menu`、`system_tenant_package`、`system_role_menu` 会把 `批记录测试` 暴露给 admin。

## Root Cause

首次实现只满足前端隐藏路由和 `EdhrBatchRecordTabs` 内部页签；但芋道源码/admin 的可见入口来自后端动态菜单、租户套餐和角色菜单绑定。没有正式菜单迁移时，用户从 admin 菜单路径进入不会看到新增入口。

## RED: admin 可见菜单迁移缺失

- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL，原因是缺少 admin 可见菜单迁移 `20260808_mes_edhr_batch_record_test_menu.sql`。

## Fix

- 新增 `IntRuoyiBackend\sql\mysql\20260808_mes_edhr_batch_record_test_menu.sql`。
- 菜单 ID：`900440`。
- 菜单名：`批记录测试`。
- 路径：`/mes/pro/feedback/edhr-batch-test`。
- 组件：`mes/pro/edhr-batch/BatchRecordTestPage`。
- 权限：`mes:pro-edhr-batch-execution:query`。
- 同步加入目标租户套餐和 admin 角色菜单绑定；缺少父菜单、批次执行菜单、套餐、角色绑定或 ID/路径冲突时 fail fast。

## GREEN: regression fixed

- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> PASS，3 tests。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS。

## Verification

静态合同已覆盖前端路由、页面、职责列表、按钮、upsert + execution、以及 admin 可见菜单迁移；SQL 合同已覆盖 release metadata、fail-fast guard、菜单 ID/路径/组件、排序、租户套餐和 admin 角色绑定。

## Blockers

None for the admin visibility issue. 2026-08-08 18:11 fresh Playwright 登录 `芋道源码/admin` 后，侧边栏可见 `批记录测试`，目标页 `/mes/pro/feedback/edhr-batch-test` 可见 `生产组长` tab，且 `internalBatchExecutionTabCount=0`。


## Bug: 批记录测试被做成批次执行内部页签

用户进一步反馈：当前做成了“批次执行”里的一个 tab，但实际需要的是类似“PQC组长”的独立页签/菜单页。

## Expected: 独立菜单页

`批记录测试` 应通过 eDHR 动态菜单作为独立入口打开 `/mes/pro/feedback/edhr-batch-test`，页面自身展示标题和“生产组长”内部 tab；`EdhrBatchRecordTabs.vue` 不应包含 `批记录测试` / `test` 分支。

## RED: internal tab contract failed

- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL，原因是 `EdhrBatchRecordTabs.vue` 仍包含 `<el-tab-pane label="批记录测试" name="test" />`。
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> FAIL，原因是 SQL 菜单排序仍按 `批次执行` sort=6、`批记录测试` sort=7。

## Fix: independent page correction

- `BatchRecordTestPage.vue` 移除 `EdhrBatchRecordTabs active-tab="test"`，新增页面级标题和 `data-edhr-batch-record-test-page` 锚点。
- `EdhrBatchRecordTabs.vue` 移除 `test` tab 类型和 `/mes/pro/feedback/edhr-batch-test` 跳转映射。
- 菜单 SQL 合同锁定 `批记录测试` sort=6、`批次执行` sort=7，使其表现为同级独立菜单入口。

## GREEN: independent page regression fixed

- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_edhr_batch_record_test_menu_sql.py` -> PASS，3 tests。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-edhr-qa-menu-static.spec.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS，exit code 0。
- GREEN: `node doc\tasks\20260808-edhr-batch-record-test-tab\verify-batch-record-test-visible.cjs` -> PASS，真实登录后侧边栏出现 `批记录测试`，页面标题和 `生产组长` tab 可见。
