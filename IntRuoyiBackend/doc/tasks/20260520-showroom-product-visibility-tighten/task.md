# 任务：展厅产品按指派与审批链收紧可见性（后端）

## Goal

在 showroom 后端实现统一产品访问域和动作权限拆分：

- `showroom_publicity` 产品全量可见
- 最新整产品指派的被指派人可见该产品，且提交后、审核中、发布后持续可见
- 该产品审批链上的审核人从指派创建开始即可见
- 无关用户列表不可见、详情不可进、编辑提交和管理动作显式拒绝
- 产品返回补 `editable` 标记，前端可直接消费

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomAssignmentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomWorkflowFacade.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomApprovalActorResolver.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-visibility-tighten\**`

## Non-Scope

- 不改数据库 schema
- 不改审批路线顺序
- 不新增 fallback 可见范围、兼容老权限码或静默绕过
- 不重做前台展示接口

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-publicity-product-assignment\task.md`
- Status before this task: `Blocked on 2026-05-20`
- Impact: 旧任务已因范围过窄被显式阻塞，本次以后端统一可见域任务继续推进。

## Milestones

- [x] M1: 收口上一同仓任务状态并创建新任务文档。
- [x] M2: 记录 BDD 并补后端 RED 测试。
- [x] M3: 实现最小后端访问域、动作权限和 `editable` 契约。
- [x] M4: 跑定向集成测试并记录 GREEN。
- [x] M5: 更新证据、cleanup 预览与提交/阻塞说明。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-visibility-tighten\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-product-visibility-tighten --mode preview`

## Current Status

Completed with blocker on 2026-05-21.

## Current Verification Result

- PASS: `mvn -pl yudao-module-showroom -am -DskipTests compile`
- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly+publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail+wholeProductAssignmentShouldExposeFillingStatusAndAssignedEditorAccess+assignedEditorShouldKeepLifecycleVisibilityForAssignedProduct+assignedEditorShouldBeReadOnlyAfterWholeAssignmentClosed+supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly+publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer+supervisorPendingApprovalProductShouldStayVisibleEvenIfLaterWholeAssignmentExists+supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly+publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: 真实浏览器 live 闭环使用同一套后端契约完成 `showroomeditor 提交 -> showroomsupervisor 签名审批 -> aoteman 签名发布 -> 编辑人发布后仍可见`。
- PASS: 主租户真实 API 回归：`wangyiru / tenant-id=1` 对 `productId=214` 的 `GET /admin-api/showroom/product/page?...productId=214` 已从 `total=0` 修复为 `total=1`，`GET /admin-api/showroom/product/get?id=214` 也恢复放行。

## Current Blockers

- Blocker: 当前 `ruoyi-vue-pro` 工作区内存在大量与本任务无关的既有未提交改动，且部分与本任务命中的 showroom 文件重叠，无法安全创建单独提交。

## Real Data Notes

- 真实 E2E 使用测试租户 `122`，并补齐了 `showroomsupervisor / showroomviewer / dept_id=910301` 这组最小权限验证数据。
- 关键 live 闭环样本为产品 `FULL1779293717036(id=210)`、变更单 `23`。
