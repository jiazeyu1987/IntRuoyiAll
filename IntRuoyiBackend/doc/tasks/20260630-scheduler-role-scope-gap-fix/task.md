# 任务：排产员正式角色范围 SQL 漏项修复（后端/SQL）

- Task ID: `20260630-scheduler-role-scope-gap-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

修复 `20260629_mes_smart_scheduling_role_scope.sql` 的 `scheduler` 白名单，使正式角色范围与当前智能排产前后端权限合同一致，补齐 `生产排产查询` 与 `排产工单调整` 所需权限。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-scheduler-role-scope-gap-analysis\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成只读审计并定位 `5541/5583`；本次开始正式修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL、pytest 证据、任务文档统一显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式 SQL 与契约测试固定角色边界。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产员角色范围保留生产排产查询权限 -> Given 生产排产页面菜单 5540 已属于排产员正式职责 / When 应用最新 SQL / Then scheduler 白名单同步包含 5541 以满足 get/page/gantt-list 查询鉴权。`
- `BDD: 排产员角色范围保留排产工单调整权限 -> Given 排产工单冻结解冻调整与同步进度入口面向排产员开放 / When 应用最新 SQL / Then scheduler 白名单同步包含 5583 且 workshop_director/team_leader 不新增该权限。`

## Milestones

1. M1：建立后端修复任务文档与执行日志。`completed`
2. M2：补 RED 契约测试。`completed`
3. M3：修复 SQL 白名单并跑到 GREEN。`completed`
4. M4：回填 bug/schema 证据并完成收尾。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-scheduler-role-scope-gap-fix\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-scheduler-role-scope-gap-fix\database-schema-evidence.md`

## Final Verification Result

- 当前正式 SQL 已把 `scheduler` 白名单补齐为同时包含：
  - `5541 = mes:pro-task:query`
  - `5583 = mes:pro-schedule-order:update`
- 已确认 `workshop_director` / `team_leader` 白名单未被顺带扩大。
- 已通过：
  - `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
  - 技能证据校验器（bug regression / database schema）。

## Current Blockers

- 无。

## Current Status

- `completed`
