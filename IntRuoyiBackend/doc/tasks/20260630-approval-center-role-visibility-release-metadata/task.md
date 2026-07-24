# 20260630 审批中心角色可见性 SQL 发布元数据修复

## Task Goal

修复 `sql/mysql/20260630_approval_center_role_visibility.sql` 缺少 `release-migration` 元数据导致主分支真实发布前 migration policy gate 失败的问题，使该 SQL 满足正式发布契约并可重新进入测试服真实发布链路。

## Scope

- 仓库：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- 文件：
  - `sql/mysql/20260630_approval_center_role_visibility.sql`
  - `doc/tasks/20260630-approval-center-role-visibility-release-metadata/task.md`
  - `doc/tasks/20260630-approval-center-role-visibility-release-metadata/execution-log.md`

## Non-Scope

- 不修改 SQL 业务逻辑
- 不直接执行测试服发布
- 不手工修改数据库数据

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-order-replan-production-material-required\task.md`
- 状态：未读取本任务前不承接其业务范围；本次为独立发布阻塞修复任务，仅处理当前主分支 migration metadata 缺失问题。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 先读索引，只打开命中的发布、PowerShell 与 worktree 经验。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - 发布门禁失败应先修复契约层问题，再回到真实发布链路。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
  - SQL 缺少 `release-migration` 元数据时不得进入真实 `build-release`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文与 SQL 文件读取必须显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；补齐正式 `release-migration` 元数据，而不是绕过 migration gate。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 审批中心角色可见性 SQL 必须满足发布契约 -> Given 主分支真实发布前 migration policy gate 扫描到 `20260630_approval_center_role_visibility.sql` When 执行发布门禁 Then 该 SQL 必须具备合法 `release-migration` 元数据，且门禁不再因缺少元数据失败。

## Milestones

1. 建立后端阻塞修复任务台账并记录经验门禁。`COMPLETED`
2. 为目标 SQL 补齐最小正确的 `release-migration` 元数据。`COMPLETED`
3. 重新执行 migration policy gate 验证门禁通过。`COMPLETED`
4. 回填证据并完成本任务。`COMPLETED`

## Expected Verification

- `sql/mysql/20260630_approval_center_role_visibility.sql` 顶部存在合法 `release-migration` 注释
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS

## Current Status

COMPLETED：已补齐 `20260630_approval_center_role_visibility.sql`、`20260630_dcc_admin_full_config_managed_scope.sql`、`20260630_mes_pro_work_order_erp_snapshot_fields.sql` 的最小 `release-migration` 元数据，并修正 `20260630_dcc_admin_full_config_menu.sql` 的 `dependsOn` 格式；`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` 已通过，主分支发布前门禁恢复放行。
