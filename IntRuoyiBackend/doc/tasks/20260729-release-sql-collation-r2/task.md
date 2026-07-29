# 20260729 Release SQL Collation R2

## Task Goal

修复测试服仅发布 `release-20260729-sqlfix-test-r260729b-r2` 时暴露的 `20260726_system_codex_smart_scheduling_test_items.sql` 排序规则不一致问题，确保测试项种子 SQL 的临时表字符列与真实 `system_codex_test_case` / `system_codex_test_checkpoint` 字符列排序规则一致。

## Milestones

- [x] 冻结失败证据并定位 failing migration。
- [x] 编写 RED 回归测试覆盖临时表 collation 契约。
- [x] 修复 SQL 临时表 DDL。
- [x] 运行定向回归与 release migration policy gate。
- [ ] 提交并推送修复提交，供下一轮干净发布 worktree 冻结。

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py -q`
- `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output <evidence>`
- 下一轮 `build-release` 的 Manifest `sourceRepos[*].commit` 等于本修复提交且 `dirty=false`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，统一 seed 临时表 collation，避免 MySQL 8 真实库字符列比较失败。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- Trigger: SQL 使用临时表/字面量与真实表字符列进行 `JOIN`、`=`、`NOT EXISTS` 比较。
- Preflight check: 临时字符串列必须声明与目标列一致的 `CHARACTER SET` / `COLLATE`，或比较表达式显式 `COLLATE`。
- Blocker: MySQL `ERROR 1267 Illegal mix of collations` 必须停止发布并冻结 lock、migration、`.env` 和实际镜像证据。
- Verification: 用定向 SQL 静态测试与 migration policy gate 证明修复；发布必须重新 build 新 releaseTag。
- Forbidden action: 不得改库默认 collation、手改真实表、扩大 WHERE 范围或复用失败 releaseTag。
- Evidence: `docs/database-rules.md`、维护仓 `doc/tasks/20260729-head-test-only-release/evidence/test-server-failure-freeze-r260729b-r2.out`。

## Current Status

ready_for_commit

## Verification Result

- RED: `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL，缺少 `collate=utf8mb4_0900_ai_ci`。
- GREEN: `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py -q` -> PASS，`9 passed`。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260729-head-test-only-release\evidence\migration-policy-gate-r260729c.json` -> PASS。
