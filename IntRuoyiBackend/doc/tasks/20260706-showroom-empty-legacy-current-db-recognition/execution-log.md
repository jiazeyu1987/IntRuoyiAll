# 执行记录

## BDD

- BDD: 当前空旧编号严格识别 -> Given 当前 INT-* 产品旧编号为空且底表存在 product_* 候选 / When 生成识别 SQL / Then 只有中文或英文严格命中并能唯一确认的候选进入 SQL，编号相同但名称不一致的候选进入 blocker
- BDD: 邻近编号断冲突 -> Given 同一个 product_* 同时命中多个 INT-* / When 其中只有一个候选中文和英文严格相等且编号距离最近 / Then 选择最近候选，其他候选不写入 SQL
- BDD: SQL 安全保护 -> Given 生成回填 SQL / When SQL 被审阅 / Then 每条 UPDATE 都要求目标 legacy_product_code 仍为空且旧编号未被同租户其他产品占用

## 证据

- GREEN: experience-preflight -> PASS, 已读取 docs/powershell-memory.md 与 docs/experience-index.md；本任务不执行数据库写入、服务器写入、发布或真实 E2E。

## 进度

- 2026-07-06：创建任务文档，准备补 RED 测试。
- RED: python -m pytest script/tests/test_showroom_current_empty_legacy_mapping_generator.py -> FAIL, 生成器 script/showroom_generate_current_empty_legacy_mapping_sql.py 尚未实现。
- 2026-07-06：实现 `script/showroom_generate_current_empty_legacy_mapping_sql.py`，基于当前识别报告生成受保护 SQL。
- GREEN: python -m pytest script/tests/test_showroom_current_empty_legacy_mapping_generator.py -> PASS, 5 passed。
- GREEN: python script/showroom_generate_current_empty_legacy_mapping_sql.py -> PASS_WITH_BLOCKERS, generated 11 protected SQL rows; 21 rows remain blocked。
- GREEN: sql-safety-scan -> PASS, UPDATE=11，SQL 不包含 product_064、product_075、INT-83，包含 legacy_product_code IS NULL 与 NOT EXISTS。
- GREEN: mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test -> PASS, Tests run: 53, Failures: 0, Errors: 0, Skipped: 0。
- 2026-07-06：结论为可安全自动回填 11 条，其余 21 条继续人工复核；本任务未执行数据库写入。
