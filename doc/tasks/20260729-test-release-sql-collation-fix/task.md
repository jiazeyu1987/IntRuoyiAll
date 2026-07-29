# 测试服发布 SQL collation 阻塞修复

## Task Goal

修复仅测试服发布中 `20260726_dcc_codex_test_items_seed.sql` 在测试服真实 MySQL 8 schema 上触发 `ERROR 1267 Illegal mix of collations` 的阻塞，使后续发布包可以基于新的已提交 HEAD 重新构建并仅发布测试服。

## Milestones

- [x] 创建隔离修复 worktree 并记录分支、HEAD、槽位。
- [x] 增加 RED 静态 SQL 回归测试，复现临时 seed 表 collation 与真实目标表列不一致。
- [x] 最小修复 SQL，使 seed 临时表字符串列与测试服真实目标列 collation 一致。
- [x] 运行定向 pytest、release migration policy gate 和必要构建门禁。
- [ ] 提交修复并推送分支。
- [ ] 将修复提交作为新的发布起点，交回维护发布流程重新构建与仅测试服发布。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` 先失败后通过。
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output <task-evidence>` 通过。
- `git status --short --branch` 证明只包含本任务 SQL、测试和任务文档改动。
- 提交后记录 commit hash，后续发布必须使用包含该 commit 的干净 release worktree。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复 seed SQL 的目标列 collation 契约，阻止 MySQL 8 真实库再次在 required SQL 阶段失败。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: required SQL 使用临时表、中文测试项名称或状态字段与 `system_codex_test_case` / `system_codex_test_checkpoint` 字符列做 `JOIN`、`=`、`NOT EXISTS` 比较。
- Preflight check: 临时表字符串列必须与测试服真实目标列 `utf8mb4_0900_ai_ci` 一致，或比较表达式显式统一到目标 collation。
- Blocker: MySQL `ERROR 1267 Illegal mix of collations`、静态测试发现 `tmp_*_seed` 仍使用 `utf8mb4_unicode_ci` 与目标表比较。
- Verification: 定向 pytest、release migration policy gate、发布后真实 migration 结果。
- Forbidden action: 禁止修改数据库默认排序规则、手改真实表排序规则、跳过 failed migration 或用临时 SQL 手工补数据冒充发布成功。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260729-head-test-only-release\execution-log.md`；`docs/database-rules.md#数据修复临时表排序规则门禁`。

## Current Status

in_progress

- Worktree: `D:\IntRuoyiWorktree\r260729-sql-collation`
- Branch: `codex/20260729-test-release-sql-collation`
- Base HEAD: `e56433700dc88a743c8707210f0da33edea41abc`
- Runtime slot: `int_main` slot `14`，frontend `8095`，backend `48095`
- RED pytest 已复现旧 SQL 使用 `utf8mb4_unicode_ci` 的失败；GREEN pytest 5 passed。
- `20260726_dcc_codex_test_items_seed.sql` 仅将两个临时 seed 表改为 `COLLATE=utf8mb4_0900_ai_ci`。
- release migration policy gate PASS，`migrationCount=390`。
- bug / database schema 技能证据校验 PASS；经验门禁已合并到既有 `docs/database-rules.md` 与 `docs/experience-index.md`。
- 下一步：验证证据契约、暂存当前任务文件、提交并推送修复分支。
