# Verification Report

## Summary

- 已将 `ERP 表格` 选择区从横向复选框改为可选择列表。
- 列表列为 `ERP表格名称`、`本地页签名称`、`最近一次同步时间`。
- 最近同步时间来自同步水位，保存配置和立即执行仍使用选中的 `syncType` 集合。

## Verification Commands

- RED: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL，旧组件仍是复选框布局。
- GREEN: `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- TYPECHECK: `pnpm ts:check` in `IntRuoyiFronted` -> PASS。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-profile-erp-table-sync-list-display/frontend-feature-evidence.md` -> PASS。
- DIFF: `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-list-display` -> PASS。

## Acceptance Result

- PASS: `el-table` 选择列替代旧 `el-checkbox-group`。
- PASS: 三个用户要求列均存在：ERP 表格名称、本地页签名称、最近一次同步时间。
- PASS: 本地页签映射已显式声明：ERP商品 / MES物料产品、ERP库存、ERP采购订单、ERP销售订单、MES生产工单、ERP生产用料清单、ERP产品BOM。
- PASS: 最近一次同步时间由 `watermarkBySyncType` 映射后在 `syncTableRows` 中展示。
- PASS: 选中行通过 `handleSyncTableSelectionChange` 回写 `selectedSyncTypes`。

## Notes

- 本任务没有修改后端、Job API、菜单权限或数据库 schema。
- Cleanup preview/apply 已完成，临时 `frontend-feature-evidence.md` 已删除，核心任务记录已保留。
- 当前共享工作区仍有其它无关脏改动；本任务未提交。
