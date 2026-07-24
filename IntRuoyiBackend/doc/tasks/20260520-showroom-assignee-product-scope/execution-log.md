# Execution Log: 展厅被指派产品可见范围与审批页签收口（后端）

BDD: 被指派编辑人进入产品管理时只能看到自己被指派的产品 -> Given 当前登录用户具备 `EDITOR` 角色并存在指向产品的真实 OPEN 指派, When 调用 `GET /showroom/product/page`, Then 返回列表只包含该用户被指派的产品且总数同步收口。

BDD: 被指派编辑人不能越权读取未指派产品详情 -> Given 当前登录用户不是目标产品的指派编辑人且不是审核人, When 调用 `GET /showroom/product/get`, Then 接口 fail-fast 返回无权访问错误。

BDD: 审批中心只返回当前审批人需要处理的审批单 -> Given 当前登录用户是部门主管审批人或企宣审批人, When 调用 `GET /showroom/approval/page`, Then 只返回当前用户正处于待处理阶段的审批单。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail+approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail` 当前仍返回 2 条产品，`approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer` 当前仍返回 2 条审批，证明产品列表与审批列表都还没有按当前登录人收口。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail+approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#approvalGetShouldReturnDiffRichDetailInsteadOfBareRequest+productAdminApisShouldReadLatestDraftRevisionWhileKeepingLiveRevisionPointer+productPageShouldReturnTotalAndRespectRequestedPageSlice+assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail+approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer+productPageShouldExposeLatestZhNarrationAudioAndVoiceWithoutFallback" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-assignee-product-scope --mode preview` -> PASS
