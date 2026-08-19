# 20260819 C015 发布迁移门禁修复

## Task Goal

修复 C015 发布迁移元数据与 app 仓发布门禁规则，使主程序当前分支能够形成新的已提交 HEAD，并让后续仅测试服发布包可证明来源于已提交代码。

## Milestones

1. 建立修复任务记录、经验门禁和 BDD/TDD 证据。
2. 复现 app 仓 migration policy gate 漏检 executable -> evidence-only 依赖的问题。
3. 补齐 app 仓门禁规则，并调整 C015 schema 迁移只依赖可执行 bootstrap。
4. 运行聚焦测试、完整 migration policy gate、维护仓实际 ops gate 复验。
5. 只暂存本任务文件，提交新的已提交 HEAD，供测试服发布流程重新冻结。

## Expected Verification

- `script/tests/test_release_migration_policy_gate.py` 新增回归先 RED 后 GREEN。
- `script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` 对 app 仓完整 SQL 通过。
- 维护仓实际 `ops/release/run-release-migration-policy-gate.py` 对修复后的 app SQL root 通过。
- 提交只包含本任务相关的发布门禁脚本、C015 SQL 元数据和任务证据。

## 经验门禁

- 发布迁移门禁：schema/data/menu/config/permission/seed 属于 executable；preflight/backfill/postflight/rollback-dry-run 属于 evidence-only；executable migration 不得 dependsOn evidence-only migration。
- 数据库迁移门禁：SQL 元数据、dependsOn 和 release migration policy gate 必须在构建发布前通过；不得通过未提交 SQL、手工远端数据库修补或降级门禁继续发布。
- Git 门禁：主工作区已有大量并发 dirty 内容；只允许选择性暂存本任务文件，提交前后必须检查 staged 清单和工作区状态，不得 `git add -A`。
- PowerShell 门禁：命令不使用 `&&`；中文与任务文档用 UTF-8；命令失败以退出码和后置证据共同判定。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；同步 app 仓发布门禁规则，并修复 C015 元数据依赖。
- 是否存在临时补丁或绕过：否。

## Current Status

in_progress

## Notes

- 本修复由测试服发布流程阻塞触发，用户已授权正式源码修复和提交新的已提交 HEAD。
- 当前主程序主工作区 dirty 很多，本任务不清理、不回滚、不提交无关改动。
