# 展厅当前空旧产品编号识别回填

## 任务目标

- 针对当前数据库中 `INT-*` 且 `legacy_product_code` 为空的展厅产品，基于桌面底表 `展厅讲解软件产品资料更新底表.xlsx` 的 `product_*` 编号做一次可复现识别。
- 生成只读识别报告和受保护回填 SQL；本任务不直接执行数据库写入。
- 修正前一轮识别中“仅编号相同但中英文名称不一致”仍被列为可识别的问题，避免把明显不相干的旧编号写入当前产品。

## 经验门禁

- PowerShell/Windows shell：已命中 `docs/powershell-memory.md`；涉及中文、JSON、SQL、Python 子进程时必须显式 UTF-8，禁止使用默认 `Get-Content` / `Set-Content` / `>` 写中文。
- 项目经验索引：已命中 `docs/experience-index.md`；本任务不做服务器写入、发布、真实 E2E 或数据库写入，执行 SQL 只生成不应用。
- BDD + 严格 TDD：先写可观察行为与失败测试，再实现脚本，记录 `RED` / `GREEN` 证据。
- No fallback：不做模糊匹配、不做名称猜测、不做自动降级；无法唯一确认的候选进入 blocker。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将识别规则固化为可复现脚本和回归测试，而不是人工改 SQL。
- `是否存在临时补丁或绕过`：否。

## 里程碑

1. 已完成：核对当前识别报告、数据库来源和桌面底表读取约束。
2. 已完成：补 RED 回归测试覆盖同编号误配、中文/英文唯一匹配、冲突断言和 SQL 保护条件。
3. 已完成：实现当前库空旧编号识别 SQL 生成脚本。
4. 已完成：运行脚本自测、SQL 输出检查和展厅导入回归测试。
5. 已完成：更新任务文档并只提交本任务相关文件。

## 预期验证

- `python -m pytest script/tests/test_showroom_current_empty_legacy_mapping_generator.py`
- `python script/showroom_generate_current_empty_legacy_mapping_sql.py`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test`

## 当前状态

- 已完成，待提交。
- 当前只允许生成 SQL 和报告，不直接执行回填。

## 最终识别结果

- 当前库 `INT-*` 且旧编号为空产品：38 项。
- 生成可安全回填 SQL：11 条。
- 继续阻塞人工复核：21 条。
- 已明确排除误配：`product_064`、`product_075` 这类仅编号相同但中英文名称不一致的候选未进入 SQL。
- 生成 SQL：`sql/mysql/20260706_showroom_current_empty_legacy_product_code_backfill.sql`。
- 二次报告：`doc/tasks/20260706-showroom-empty-legacy-current-db-recognition/current-empty-legacy-sql-report.json`。

## Cleanup Keep

- `doc/tasks/20260706-showroom-empty-legacy-current-db-recognition/current-empty-legacy-recognition-report.csv`
- `doc/tasks/20260706-showroom-empty-legacy-current-db-recognition/current-empty-legacy-recognition-report.json`
- `doc/tasks/20260706-showroom-empty-legacy-current-db-recognition/current-empty-legacy-sql-report.json`
- `doc/tasks/20260706-showroom-empty-legacy-current-db-recognition/database-schema-evidence.md`
