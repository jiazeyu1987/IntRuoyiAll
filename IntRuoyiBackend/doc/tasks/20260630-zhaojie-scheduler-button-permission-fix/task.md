# 任务：排产员按钮权限正式角色范围修复

- Task ID: `20260630-zhaojie-scheduler-button-permission-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`
- User Request: `5532 mes:pro-work-order:create / 900200 mes:pro-work-order:create-erp / 5552 mes:pro-feedback:create / 5555 mes:pro-feedback:export / 5969 mes:pro-feedback:approve / 5535 mes:pro-work-order:export 这些有对应的角色吗?如果没有,把这些权限也加在排产员上`

## Task Goal

从正式角色范围 SQL 根因修复 `tenant_id=1` 下 `排产员(mes_scheduler)` 缺少 MES 生产工单与生产报工按钮权限的问题，使 `zhaojie` 对应角色在应用正式 SQL 后具备用户明确要求的按钮权限，并用 SQL 合同测试锁定角色边界。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-empty-current-schedule-regression\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成排程日历空态回归修复；本次进入独立的角色范围 SQL 修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL、pytest 证据、任务文档统一显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式幂等 SQL 白名单与回归测试固定角色边界。
- `是否存在临时补丁或绕过`：否。禁止只在运行库手工加角色菜单代替正式 SQL。

## BDD 场景

- `BDD: 排产员保留生产工单按钮权限 -> Given 排产员已拥有生产工单页面入口 / When 应用正式角色范围 SQL / Then scheduler 白名单包含 5532=create、5535=export、900200=create-erp。`
- `BDD: 排产员保留生产报工按钮权限 -> Given 排产员已拥有生产报工页面入口 / When 应用正式角色范围 SQL / Then scheduler 白名单包含 5552=create、5555=export、5969=approve。`
- `BDD: 角色范围修复不顺带放大其他角色 -> Given 本次仅修复排产员按钮权限 / When 应用正式角色范围 SQL / Then workshop_director 与 team_leader 不额外获得 5532、5535、5555、5969、900200。`
- `BDD: 缺失菜单基线时 SQL 必须 fail-fast -> Given 角色范围 SQL 依赖上述按钮菜单存在 / When 某个目标菜单缺失 / Then SQL 应通过基线检查直接失败，而不是静默跳过授权。`

## Milestones

1. M1：建立任务台账并确认角色/菜单现状。`completed`
2. M2：补 RED SQL 合同测试。`completed`
3. M3：修改正式 SQL 白名单与基线校验。`completed`
4. M4：执行 GREEN 验证并回填证据。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-zhaojie-scheduler-button-permission-fix\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-zhaojie-scheduler-button-permission-fix\database-schema-evidence.md`

## Current Blockers

- 无。

## Final Verification Result

- 已确认这些权限存在现成角色承载：
  - `super_admin`：`5532/5535/5552/5555/5969/900200`
  - `tenant_id=122` 的 `mes_scheduler`：`5532/5552/900200`
  - `mes_team_leader`：`5552`
- 已从正式 SQL 根因修复：`20260629_mes_smart_scheduling_role_scope.sql` 的 `scheduler` 白名单补齐：
  - `5532 = mes:pro-work-order:create`
  - `5535 = mes:pro-work-order:export`
  - `5552 = mes:pro-feedback:create`
  - `5555 = mes:pro-feedback:export`
  - `5969 = mes:pro-feedback:approve`
  - `900200 = mes:pro-work-order:create-erp`
- 已同步补齐菜单基线检查，避免这些按钮菜单缺失时被静默跳过。
- 已通过：
  - `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-zhaojie-scheduler-button-permission-fix\bug-regression-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-zhaojie-scheduler-button-permission-fix\database-schema-evidence.md`
- 已应用本机运行库并回查通过：
  - `tenant_id=1` 的 `mes_scheduler/排产员` 当前已有效拥有 `5532/5535/5552/5555/5969/900200`

## Current Status

completed
