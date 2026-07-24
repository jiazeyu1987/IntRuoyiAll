# 任务：修复自动排产权限拆分未同步租户包的发布阻塞

- Task ID: `20260630-mes-auto-schedule-tenant-package-release-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

修复 `sql/mysql/20260624_mes_auto_schedule_permission_split.sql` 只新增自动排产动作菜单与角色菜单、却未同步租户包 `menu_ids` 的契约缺口，消除测试服 `20260617_mes_scheduler_role_smart_scheduling_tab.sql` 因智能排产递归菜单树不完整而失败的问题，并恢复主分支测试服真实发布闭环。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-mes-schedule-issue-structured-backflow-release-fix\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成 `20260624_mes_schedule_issue_structured_backflow.sql` 幂等性修复；本次进入新的发布 SQL 契约阻塞修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
  - 命中发布经验索引，需读取构建发布预检经验。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - required SQL 在测试服失败时先只读核对真实库状态、租户包和菜单基线，禁止手工改测试库绕过。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
  - 发布失败优先排查 required SQL 契约与前置菜单/租户包一致性；修复后需重新走主分支 `build-release -> publish-test`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。回到前置 SQL 契约补齐租户包同步，不手工改测试库、不放宽发布脚本校验。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 自动排产动作菜单进入租户包 -> Given 20260624 自动排产权限拆分会新增 900180/900181/900182 子菜单 When required SQL 执行完成 Then 含智能排产父菜单 900120 的租户包必须同步包含这 3 个动作菜单。`
- `BDD: 智能排产递归授权 SQL 不再被前置菜单缺口阻断 -> Given 测试租户排产员角色会执行 20260617_mes_scheduler_role_smart_scheduling_tab.sql When 发布 required SQL 继续执行 Then 不因租户包缺少 900180/900181/900182 而触发 `Missing MES smart scheduling menu tree in tenant package`。`

## Milestones

1. M1：建立任务文档并记录新的真实发布阻塞。`completed`
2. M2：补 RED 门禁测试，证明自动排产权限拆分 SQL 未同步租户包。`completed`
3. M3：最小修复 SQL 并通过 GREEN 验证。`completed`
4. M4：提交后端主分支修复并回到主分支真实发布闭环。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_mes_auto_schedule_permission_sql.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## Current Blockers

- 无。自动排产动作菜单同步租户包的 SQL 修复已提交后端主分支并真实越过测试服此前阻塞点；后续新的发布阻塞已转入新的后端任务处理。
