# 任务：修复排产问题结构化回流 SQL 发布幂等性

- Task ID: `20260630-mes-schedule-issue-structured-backflow-release-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

修复 `sql/mysql/20260624_mes_schedule_issue_structured_backflow.sql` 在测试服发布阶段因重复加列失败的问题，使其满足 required SQL 可重复执行契约，并把本次失败固化为发布前门禁测试，恢复主分支测试服发布闭环。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-validation-boundary\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成；本次开始新的发布 SQL 契约修复任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`
  - 命中发布经验索引，需读取构建发布预检经验。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
  - required SQL 在测试服失败时先只读核对真实库状态，禁止手工改测试库绕过；问题需回到 SQL 契约修复并补成门禁。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`
  - 发布失败优先排查 migration / manifest / required SQL 契约；修复后需重新走主分支 `build-release -> publish-test`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接把 required SQL 改为幂等并增加回归门禁，而不是手工改测试服数据库。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产问题结构化回流 SQL 可重复执行 -> Given mes_pro_schedule_issue 可能已提前拥有 status/source 字段与索引 / When 发布 required SQL 重新执行 20260624_mes_schedule_issue_structured_backflow.sql / Then SQL 不因重复列或重复索引失败。`
- `BDD: 发布前能提前发现 SQL 不可重入风险 -> Given required SQL 会被纳入真实发布包 / When 运行发布前 SQL 幂等性门禁测试 / Then 不允许保留裸 ALTER ADD COLUMN 或裸 ADD KEY 造成重复执行失败。`

## Milestones

1. M1：建立任务文档并记录发布失败根因。`completed`
2. M2：补 RED/门禁测试落点并锁定幂等契约。`completed`
3. M3：最小修复 SQL 并通过 GREEN 验证。`completed`
4. M4：提交后端主分支修复并回到主分支真实发布闭环。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_release_sql_idempotency_contract.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## Current Blockers

- 无。后端主分支修复已提交，可回到维护仓重新执行主分支真实发布闭环。
