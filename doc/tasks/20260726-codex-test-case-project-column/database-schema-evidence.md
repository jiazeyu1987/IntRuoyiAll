# Database Schema Evidence

## Scope

- 若当前测试项持久化表缺少项目字段，需要补充 schema/seed 或等价正式数据契约，并验证现有测试项分类。

## Rollback

- 回滚策略：若迁移在未分类项处中止，不会修改为非法默认项目；用户需先补正式项目归属后重跑迁移。
- 若需结构回滚，需在发布回滚流程中删除 `project` 索引和列；本任务未执行真实数据库变更。

## BDD

- BDD: 当前测试项项目 backfill -> Given `system_codex_test_case` 已有测试项, When 迁移运行, Then 非删除测试项被归入 `智能排产`、`文控` 或 `批记录`，无法归类时中止。

## Verification

- `python -m pytest script\tests\test_codex_test_management_migration.py script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py script\tests\test_codex_test_case_project_migration.py` -> PASS。

## Blockers

- 未执行真实数据库迁移；本次仅交付迁移脚本和静态契约验证。

## Evidence

- Data change: `system_codex_test_case.project varchar(16) NOT NULL COMMENT '所属项目：智能排产/文控/批记录'`。
- Migration: `IntRuoyiBackend/sql/mysql/20260726_system_codex_test_case_project.sql` 添加字段、回填分类、校验未分类项并补 `idx_system_codex_test_case_tenant_project`。
- Seed changes: 智能排产种子写入 `智能排产`；DCC 文控种子写入 `文控`。
- Existing data classification: `文控` 按 DCC/文控关键字；`批记录` 按 eDHR/批记录/记录本关键字；`智能排产` 按排产/smart-scheduling/scheduler 关键字。
- Safety: 非删除测试项无法归类时 `SIGNAL SQLSTATE '45000'` 中止迁移；不写默认项目、不静默降级。
- RED: SQL 静态合同新增后，项目字段/种子/backfill 缺失导致 5 个预期失败。
- GREEN: `python -m pytest script\tests\test_codex_test_management_migration.py script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py script\tests\test_codex_test_case_project_migration.py` -> PASS，11 passed。
