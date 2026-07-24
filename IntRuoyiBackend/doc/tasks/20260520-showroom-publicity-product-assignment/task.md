# 任务：展厅企宣指定用户修改产品信息（后端）

## Goal

确认并补齐 `展厅 -> 产品管理` 中企宣创建产品整单指派及其后续驳回回改的后端行为：指派必须绑定真实启用编辑用户；指派创建后当前产品状态对管理端呈现为 `IN_FILLING`；产品列表和详情返回当前 OPEN 指派对象；审批驳回后若可追溯到原整产品指派，则重新打开原指派、发送驳回站内信给提交人，并让产品重新进入 `IN_FILLING`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomAssignmentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomWorkflowNotifyService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomWorkflowFacade.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\dal\mysql\workflow\ShowroomFieldAssignmentMapper.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\workflow\ShowroomAssignmentWorkflowTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\workflow\ShowroomWorkflowApprovalTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\20260520_showroom_notify_template_seed.sql`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-publicity-product-assignment\**`

## Non-Scope

- 不改数据库 schema。
- 不新增 fallback 审批人或 fallback 被指派人。
- 不改动展厅审批路线顺序。
- 不用前端隐藏替代后端访问控制。
- 不为没有原整产品指派的手工提交自动新建指派。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-app-config-live-data-local-verification\task.md`
- Status before this task: `Completed`
- Impact: 无，可继续处理企宣整单指派确认任务。

## Milestones

- [x] M1: 创建后端任务文档并确认上一同仓任务状态。
- [ ] M2: 补充 BDD 与后端定向测试，锁定“创建整单指派 -> 状态 IN_FILLING -> 返回指派对象”。
- [ ] M3: 补充驳回回改的 RED 测试，锁定“驳回通知 + 原整单指派重开 + 状态回到 IN_FILLING”。
- [ ] M4: 如现有实现不满足，完成最小后端修复。
- [ ] M5: 运行后端定向验证并记录 GREEN。
- [ ] M6: 运行 task-closeout-cleanup 预览，完成任务文档。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#wholeProductAssignmentShouldExposeFillingStatusAndAssignedEditorAccess+wholeProductAssignmentSubmitShouldCloseAssignmentAndEnterReview" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomAssignmentWorkflowTest+ShowroomWorkflowApprovalTest+ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python script/tests/test_showroom_sql_scripts.py`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-publicity-product-assignment --mode preview`

## Current Status

Blocked on 2026-05-20.

## 本次并入变更

- 不新开后端 task-id，直接并入本任务。
- 本次新增后端范围：
  - 审批驳回后发送真实驳回站内信。
  - 仅对可追溯到原 `__PRODUCT_ALL_FIELDS__` 整产品指派的驳回单，重新打开原指派。
  - 产品状态解析优先回到 `IN_FILLING`，不再让最新驳回单覆盖为 `REJECTED`。

## Blockers And Impact

- Blocker: 当前 `yudao-module-showroom` 里仍有多份与本次驳回回改无关的旧测试引用过时的 showroom 内容模型/构造签名，`ShowroomHttpApiIntegrationTest` 的 3 个新驳回场景在 `testCompile` 阶段就被这些存量错误阻断。
- Impact: 本任务已完成 SQL 与单元级 GREEN，但暂时无法在本仓拿到 3 个驳回集成场景的一次性 GREEN，因此当前不满足提交条件。
