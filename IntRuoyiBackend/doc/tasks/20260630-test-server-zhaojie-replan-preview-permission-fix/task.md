# 任务：测试服 zhaojie 预览重排无权限修复（后端/SQL）

- Task ID: `20260630-test-server-zhaojie-replan-preview-permission-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

修复智能排产正式角色范围 SQL，使 `排产员` 保留 `mes:pro-auto-schedule:preview/apply/replan` 中与既有职责一致的权限，避免测试服与本机后续落库后再次出现“排产员执行手动重排预览被后端拒绝”的问题。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-erp-production-admin-role\task.md`
- 状态：`blocked`
- 处理说明：上一后端任务已因用户切换问题暂停；本次进入新的权限合同修复任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL 与 pytest 证据统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 若后续需要真实登录复验，必须先走官方最小路径。
- `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - 本轮只补正式 SQL 与契约测试，不直接操作测试服。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式角色范围 SQL 和契约测试补齐自动排产权限白名单。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产员角色范围保留自动排产权限 -> Given scheduler 正式角色范围由 20260629 角色收敛 SQL 统一维护 / When 应用最新 SQL / Then scheduler 白名单保留 900180/900181/900182。`
- `BDD: 车间主任与班组长范围不被误扩大 -> Given 本次仅修复排产员缺失的 replan 权限 / When 应用最新 SQL / Then workshop_director/team_leader 白名单保持原边界，不被顺带放大。`

## Milestones

1. M1：确认后端权限合同与现有 SQL 漏项。`completed`
2. M2：补 RED 契约测试。`completed`
3. M3：修复角色范围 SQL 并跑到 GREEN。`completed`
4. M4：回填数据库证据与风险边界。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`

## Final Verification Result

- 当前正式 SQL 已把 `scheduler` 白名单补齐为包含 `900180/900181/900182`，与自动排产权限拆分合同一致。
- 本轮确认 `workshop_director` / `team_leader` 白名单未被顺带扩大。
- 已通过：`python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_smart_scheduling_role_scope_sql.py -q`

## Current Status

- `completed`

## Current Blockers

- 测试服真实库应用与 `zhaojie` 运行时权限复验不在本轮默认授权范围内。
