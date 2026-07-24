# 任务：展厅被指派产品可见范围与审批页签收口（后端）

## Goal

让被指派填写产品详情的用户进入 `展厅 -> 产品管理` 后只能看到指派给自己的产品；让有展厅产品审核权限的用户访问 `展厅 -> 审批中心` 时只看到自己需要处理的审批单。后端必须以当前登录用户为准做 fail-fast 访问控制，不得返回全量数据后依赖前端隐藏。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomAssignmentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\workflow\service\ShowroomWorkflowFacade.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-assignee-product-scope\**`

## Non-Scope

- 不改动展厅审批流程本身的顺序与角色模型
- 不新增 fallback 数据范围或兼容老权限码
- 不改数据库 schema
- 不替代系统菜单授权，只在接口层补真实数据范围和访问校验

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-company-menu-direct-save\task.md`
- Status before this task: `Blocked on 2026-05-20`
- Impact: 上一后端任务已明确记录 unrelated `ShowroomHttpApiIntegrationTest.java` 断言漂移阻塞；本次任务继续处理新的展厅接口可见范围需求，但不与上一条 blocked 任务混提交。

## Milestones

- [x] M1: 创建后端任务文档并确认上一同仓任务状态。
- [x] M2: 先补 RED 测试，锁定“指派编辑人产品列表收口”和“审批人审批列表收口”。
- [x] M3: 实现后端最小访问控制与数据过滤。
- [x] M4: 执行定向集成测试并记录 GREEN。
- [x] M5: 更新任务记录、执行 cleanup preview，并输出菜单授权要求。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 需要时用更窄的 `ShowroomHttpApiIntegrationTest#...` 定向方法组合验证
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-assignee-product-scope --mode preview`

## Current Status

Completed on 2026-05-20.

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail+approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#approvalGetShouldReturnDiffRichDetailInsteadOfBareRequest+productAdminApisShouldReadLatestDraftRevisionWhileKeepingLiveRevisionPointer+productPageShouldReturnTotalAndRespectRequestedPageSlice+assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail+approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer+productPageShouldExposeLatestZhNarrationAudioAndVoiceWithoutFallback" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-assignee-product-scope --mode preview`

## Notes

- 菜单仍由真实 `system_menu` / `system_role_menu` 控制；本次后端只补“进入菜单之后按当前登录人收口”的访问范围。
- 当前工作区在同仓库同模块仍存在本任务之外的未提交改动，且部分文件与本次修改重叠；为避免混入无关变更，本次未自动执行 Git commit。
