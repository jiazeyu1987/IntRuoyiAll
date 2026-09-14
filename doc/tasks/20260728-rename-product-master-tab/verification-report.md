# 验证报告：产品主数据页签重命名

## Result

- Status: completed。
- 用户可见入口/页签名称已从 `产品主数据` 改为 `展厅主数据`。
- 业务对象、导入导出、DCC 选择等仍保留 `产品主数据` 文案，避免扩大语义变更。

## Evidence

- RED: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> FAIL，缺少 `20260728_rename_mdm_product_menu.sql`。
- GREEN: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mdm-tenant-package-real-setup-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mdm-real-data-prerequisite-guards-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/mdm-product-real-setup.e2e.js; node --check tests/e2e/mdm-role-menu-real-setup.e2e.js; node --check tests/e2e/mdm-tenant-package-real-setup.e2e.js` -> PASS。
- GREEN: 聚焦迁移门禁 `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260607_product_master_data.sql --sql-file sql\mysql\20260728_rename_mdm_product_menu.sql` -> PASS，`migrationCount=2`。

## Notes

- 全量文案扫描超时，未作为通过依据；受影响目录聚焦扫描完成且 `garbled_text=0`。
- 全量 MySQL migration policy gate 当前存在既有无关 blocker：`20260725_mes_edhr_recordbook_global_setting.sql: config-seed`。
- Cleanup preview/apply 已通过，无删除项、无阻塞。
- 项目经验已沉淀到 `docs/frontend-development.md#动态菜单页签重命名门禁` 并在 `docs/experience-index.md` 建立关键词索引。
