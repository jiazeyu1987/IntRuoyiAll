# 20260801 智能排产测试项 Seed Collation 修复

## Task Goal

修复 `20260726_system_codex_smart_scheduling_test_items.sql` 在测试服执行 required SQL 时因临时表排序规则不一致触发的 MySQL `ERROR 1267`，以及同一临时表在单条校验语句中被重复读取触发的 MySQL `ERROR 1137`，并用 BDD/TDD 防止同类 Codex 测试项 seed 再次发布失败。

## Milestones

1. 建立修复任务文档与经验门禁。
2. 增加能复现 collation 缺陷和临时表重复读取缺陷的 RED 静态回归测试。
3. 最小修改 SQL seed，使临时表字符串列与 `system_codex_test_*` 目标列排序规则一致，并避免在同一语句中重复读取同一 TEMPORARY TABLE。
4. 运行目标测试、迁移策略门禁与结构验证。
5. 更新维护仓发布阻塞记录，提交当前任务实现。

## Expected Verification

- `script/tests/test_codex_smart_scheduling_test_items_seed.py` 先因缺少显式 `utf8mb4_0900_ai_ci` / collation 覆盖失败，并因临时表重复读取校验失败，再在修复后通过。
- `script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output <task evidence>` 通过。
- `git diff --check` 通过。
- 仅修改本任务 SQL、测试和任务/证据文档；不触碰并行前端任务改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过 SQL collation 显式声明、临时表计数变量拆分和静态测试覆盖 required SQL 发布失败根因。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 数据修复临时表排序规则门禁：required SQL 使用临时表、字面量或过程变量与真实表字符列做 `JOIN`、`=`、`NOT EXISTS` 比较前，必须按真实表字符列 collation 显式声明临时字符串列或表达式。
- MySQL TEMPORARY TABLE 校验门禁：同一 SQL 语句不得在外层查询与子查询中重复读取同一临时表；需要比较计数时先 `SELECT COUNT(*) INTO` 变量，再分语句比较。
- 测试管理 schema 迁移门禁：Codex 测试项 seed 必须以当前 `system_codex_test_case` / `system_codex_test_checkpoint` 迁移定义为准，不得凭历史默认 collation。
- PowerShell/TDD 门禁：中文 SQL 和测试文档按 UTF-8 读写；Maven/Python/脚本命令逐条记录退出码，不用 fallback 或 mock 成功。
- Git/worktree 门禁：本次最终用于重新发布的修复提交必须基于原测试服发布冻结提交 `9420210f7ad4fb2519c179458fae0e823d082b54`；在专用 clean worktree `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix` 上移植最小 SQL/test 修复，避免后续 `int_main` 新提交或主工作区脏改动进入构建输入。

## Current Status

ready_for_closeout

## Progress Log

- 2026-08-01：用户授权继续修复测试服发布阻塞；先在 `D:\IntRuoyiWorktree\r260801-smartseed-collation-fix` 验证根因修复，提交 `638093b76dbf9c0cafdf3d299a35882c152cad45`；复核发现该 worktree 基于后续主线 `7c7cce61ddf6ddd4c2d0dc2a8e002608a1f4a239`，不满足原发布冻结输入边界。
- 2026-08-01：创建最终发布修复 worktree `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix`，分支 `codex/20260801-smart-seed-collation-fix-frozen`，基线 HEAD=`9420210f7ad4fb2519c179458fae0e823d082b54`，登记 `int_main slot=7`、前端 `8088`、后端 `48088`，并从 `638093b76` 仅移植 SQL/test/经验修复。
- 2026-08-01：已完成第一轮 RED/GREEN；修复 SQL 临时表 collation，目标 pytest 4 passed，相邻 pytest 11 passed，migration policy gate passed。后续发布 `release-20260801-frozen-smartseed-fix-r260801b-r1` 仍在同一 SQL line 313 触发 MySQL `ERROR 1137 Can't reopen table: 'seed'`，确认还需修复临时表重复读取。
- 2026-08-01：新增临时表重复读取 RED 测试，修复为 `v_expected_checkpoint_count` / `v_actual_checkpoint_count` 分语句计数比较；目标 pytest 5 passed，相邻 pytest 12 passed，migration policy gate passed，新 migration SHA256=`e633f8ac1a008d6a46ebf94190614f6632b9094d66bb8242818ec3b0a78d934c`。等待提交后用新 releaseTag 重新构建发布。
