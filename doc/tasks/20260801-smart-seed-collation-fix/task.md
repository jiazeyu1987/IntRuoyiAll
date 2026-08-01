# 20260801 智能排产测试项 Seed Collation 修复

## Task Goal

修复 `20260726_system_codex_smart_scheduling_test_items.sql` 在测试服执行 required SQL 时因临时表/过程内字符串比较排序规则不一致触发的 MySQL `ERROR 1267`，并用 BDD/TDD 防止同类 Codex 测试项 seed 再次发布失败。

## Milestones

1. 建立修复任务文档与经验门禁。
2. 增加能复现 collation 缺陷的 RED 静态回归测试。
3. 最小修改 SQL seed，使临时表字符串列与 `system_codex_test_*` 目标列排序规则一致。
4. 运行目标测试、迁移策略门禁与结构验证。
5. 更新维护仓发布阻塞记录，提交当前任务实现。

## Expected Verification

- `script/tests/test_codex_smart_scheduling_test_items_seed.py` 先因缺少显式 `utf8mb4_0900_ai_ci` / collation 覆盖失败，再在修复后通过。
- `script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output <task evidence>` 通过。
- `git diff --check` 通过。
- 仅修改本任务 SQL、测试和任务/证据文档；不触碰并行前端任务改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过 SQL collation 显式声明和静态测试覆盖 required SQL 发布失败根因。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 数据修复临时表排序规则门禁：required SQL 使用临时表、字面量或过程变量与真实表字符列做 `JOIN`、`=`、`NOT EXISTS` 比较前，必须按真实表字符列 collation 显式声明临时字符串列或表达式。
- 测试管理 schema 迁移门禁：Codex 测试项 seed 必须以当前 `system_codex_test_case` / `system_codex_test_checkpoint` 迁移定义为准，不得凭历史默认 collation。
- PowerShell/TDD 门禁：中文 SQL 和测试文档按 UTF-8 读写；Maven/Python/脚本命令逐条记录退出码，不用 fallback 或 mock 成功。
- Git/worktree 门禁：本次最终用于重新发布的修复提交必须基于原测试服发布冻结提交 `9420210f7ad4fb2519c179458fae0e823d082b54`；在专用 clean worktree `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix` 上移植最小 SQL/test 修复，避免后续 `int_main` 新提交或主工作区脏改动进入构建输入。

## Current Status

ready_for_closeout

## Progress Log

- 2026-08-01：用户授权继续修复测试服发布阻塞；先在 `D:\IntRuoyiWorktree\r260801-smartseed-collation-fix` 验证根因修复，提交 `638093b76dbf9c0cafdf3d299a35882c152cad45`；复核发现该 worktree 基于后续主线 `7c7cce61ddf6ddd4c2d0dc2a8e002608a1f4a239`，不满足原发布冻结输入边界。
- 2026-08-01：创建最终发布修复 worktree `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix`，分支 `codex/20260801-smart-seed-collation-fix-frozen`，基线 HEAD=`9420210f7ad4fb2519c179458fae0e823d082b54`，登记 `int_main slot=7`、前端 `8088`、后端 `48088`，并从 `638093b76` 仅移植 SQL/test/经验修复。
- 2026-08-01：已完成 RED/GREEN；修复 SQL 临时表 collation，目标 pytest 4 passed，相邻 pytest 11 passed，migration policy gate passed。等待提交后用新 releaseTag 重新构建发布。
