# 任务：修复 MES 排产员角色缺失导致正式发布阻塞

## 任务目标

修复 `release-20260701-1720-category-code-fix` 在正式服执行 `20260629_mes_smart_scheduling_role_scope.sql` 时因租户 1 缺少启用的 MES 排产员角色而失败的问题。方案必须回到 required SQL 的正式数据契约，不手工改正式库业务数据、不跳过迁移、不改发布状态。

## 经验门禁

- 命中 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`：发布失败优先只读定位真实库状态，再修复 SQL 契约和门禁测试。
- 命中 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`：发布必须使用单一 releaseTag 完成 build-release -> publish-test -> mark-tested -> promote-prod -> promote-backup。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`：涉及正式/备份发布必须按发布、备份、恢复规范验证。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`：服务器只读诊断和发布验证按统一访问方式执行。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`：后续重新发布必须使用本次专用临时 release worktree，不复用失败 worktree。
- 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文 SQL/SSH/MySQL 文本传输必须显式 UTF-8，避免 PowerShell 管道污染。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。缺少角色时由 required SQL 正式创建或恢复明确的 MES 排产员角色，不静默跳过。
- 是否从根因和长期维护角度解决：是。将排产员角色纳入与车间主任、班组长同级的幂等角色基线，保证后续角色菜单与用户角色迁移有稳定目标。
- 是否存在临时补丁或绕过：否。不手工修正式库、不改迁移记录、不拼接失败 releaseTag。

## BDD 场景

- Given 正式租户 1 缺少启用且未删除的 `排产员/mes_scheduler` 角色
- When 发布系统执行 `20260629_mes_smart_scheduling_role_scope.sql`
- Then SQL 应先恢复或创建唯一可用的 MES 排产员角色，再继续收口该角色菜单权限，并让后续 `role_assignment` 迁移可解析目标角色

## 里程碑

- [x] 只读诊断正式库缺失角色根因和失败迁移记录。
- [x] RED：补充 SQL 合同测试，证明当前 migration 没有创建排产员角色基线。
- [x] GREEN：最小修改 required SQL，幂等恢复/创建租户 1 排产员角色并解析目标 ID。
- [x] REGRESSION：运行角色范围测试、角色分配测试、迁移策略门禁和 TDD 合规校验。
- [x] 交付：提交后端修复，供维护仓用新提交、新 worktree、新 releaseTag 重新构建发布。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_mes_smart_scheduling_role_scope_sql.py script/tests/test_mes_smart_scheduling_role_assignment_sql.py -q`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql`
- `python -X utf8 tool/verify_tdd_compliance.py --task-dir doc/tasks/20260701-fix-mes-scheduler-role-scope-prod-missing --repo-root .`

## 当前状态

COMPLETED：后端 SQL 修复和回归验证均已通过；本任务改动可提交，并由维护仓使用新提交、新 worktree、新 releaseTag 重新构建发布。

## 完成结论

已完成 required SQL 根因修复：租户 1 缺少 `排产员/mes_scheduler` 时，`20260629_mes_smart_scheduling_role_scope.sql` 会幂等恢复或创建排产员角色，并解析单一目标角色 ID 继续角色菜单范围收口。失败的 `release-20260701-1720-category-code-fix` 不再继续复用，后续必须从包含本修复的新后端提交重新出包。
