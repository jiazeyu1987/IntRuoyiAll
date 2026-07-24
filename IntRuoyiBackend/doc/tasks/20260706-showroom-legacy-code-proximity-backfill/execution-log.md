# 执行日志

BDD: 多候选用邻近编号唯一推理 -> Given `product_066` 同时命中 `INT-67` 和 `INT-97`，且两者中英文名均严格相等、旧编号均为空，When 生成旧编号回填 SQL，Then 选择数字距离唯一最近且距离为 1 的 `INT-67`。

BDD: 邻近编号不可判定时继续阻塞 -> Given 多候选距离并列、最近距离大于 1、中文或英文不完全相等、候选已有旧编号或候选不是 `INT-*`，When 生成旧编号回填 SQL，Then 不生成推理更新并记录 blocker。

BDD: 导入规则保持旧编号精确匹配 -> Given 基础底表导入包含 `product_*` 编码，When 执行展厅导入回归，Then 仍只通过 `legacy_product_code` 更新当前产品，未恢复名称自动匹配。

- 已读取经验门禁：`docs/powershell-memory.md`、`docs/experience-index.md`、`database-schema-delivery/SKILL.md`、`database-contract.md`。
- GREEN: experience-preflight -> PASS，本任务只生成本地 SQL/报告并运行本地测试；不执行真实库写入、服务器操作或真实 E2E。
- DISCOVERY: 当前可推理新增 4 条：租户 1/122 的 `product_066 -> INT-67`、`product_149 -> INT-150`；两组均为同中英文多候选，最近候选距离 1，另一候选距离分别为 31 和 66。
- RED: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> FAIL, expected reason: 当前生成器仍将同中英文多候选记录为 `ENGLISH_NAME_MATCH_NOT_UNIQUE`，不会用邻近编号推理。
- GREEN: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> PASS, 6 tests，覆盖唯一英文名、部分 blocker、邻近编号唯一推理、距离过大阻塞、多候选/无候选/已占用/非 INT 候选。
- GREEN: `python script/showroom_generate_english_name_legacy_mapping_sql.py` -> PASS_WITH_BLOCKERS，生成 12 条 SQL；12 条继续人工确认；无 `showroom_product_revision` 更新。
- GENERATED: 新增邻近推理映射 4 条，tenant 1/122 的 `product_066 -> INT-67`、`product_149 -> INT-150`，`resolution=PROXIMITY_UNIQUE_NEAREST`，`proximity_distance=1`。
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS, 53 tests，确认导入规则仍通过 `legacy_product_code` 精确映射，未恢复名称自动匹配。
- CLEANUP-PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-showroom-legacy-code-proximity-backfill --mode preview` -> READY，keep task/db evidence，delete `<none>`，blocked `<none>`。
