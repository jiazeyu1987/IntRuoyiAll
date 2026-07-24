# 执行日志

BDD: 英文名严格唯一补齐旧编号 -> Given 当前 `INT-*` 产品旧编号为空且旧底表 `product_*` 英文名与其规范化后严格相等，When 生成英文名回填 SQL，Then 只生成该唯一映射的受保护更新，不修改产品名称。

BDD: 英文名不可唯一时阻塞 -> Given 同一 `product_*` 英文名命中多个空旧编号 `INT-*` 候选、没有候选、候选已有旧编号或候选不是 `INT-*`，When 生成英文名回填 SQL，Then 该行进入 blocker，不生成更新。

BDD: 导入规则保持旧编号精确匹配 -> Given 基础底表导入包含 `product_*` 编码，When 执行 `/showroom/product/import-base-workbook` 相关回归，Then 仍只通过 `legacy_product_code` 更新当前产品，未映射旧编号继续 skipped。

- 已读取经验门禁：`docs/powershell-memory.md`、`docs/experience-index.md`、`database-schema-delivery/SKILL.md`、`database-contract.md`。
- GREEN: experience-preflight -> PASS，本任务只生成本地 SQL/报告并运行本地测试；不执行真实库写入、服务器操作或真实 E2E。
- RED: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> FAIL, expected reason: `showroom_generate_english_name_legacy_mapping_sql.py` 尚不存在，英文名唯一匹配 SQL 生成能力未实现。
- GREEN: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> PASS, 3 tests。
- GREEN: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> PASS, 4 tests，覆盖唯一英文名映射、部分映射部分 blocker、多候选、无候选、候选已有旧编号、候选非 `INT-*`。
- GREEN: `python script/showroom_generate_english_name_legacy_mapping_sql.py` -> PASS_WITH_BLOCKERS，生成 8 条 SQL；16 条继续人工确认；无 `showroom_product_revision` 更新。
- GENERATED: `sql/mysql/20260706_showroom_legacy_product_code_english_name_backfill.sql` -> 8 UPDATEs, mappings: tenant 1/122 的 `product_015 -> INT-15`、`product_081 -> INT-82`、`product_086 -> INT-87`、`product_095 -> INT-96`。
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS, 53 tests，确认导入规则仍通过 `legacy_product_code` 精确映射，未恢复名称自动匹配。
- CLEANUP-PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-showroom-legacy-code-english-name-backfill --mode preview` -> READY；首次预览会删除数据库证据和生成报告，已将二者加入 `Cleanup Keep`，因为它们是本任务正式交付证据。
