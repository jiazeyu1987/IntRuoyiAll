# Execution Log: 展厅产品按指派与审批链收紧可见性（后端）

BDD: 被指派人持续可见 -> Given 产品存在最新整产品指派 / When 被指派人提交审批并经历审核和发布 / Then 后端产品列表仍返回该产品且详情访问仍放行

BDD: 审批链从指派即看 -> Given 产品刚被整产品指派且尚未提交审批 / When 登录该产品当前主管或企宣审批人 / Then 后端产品列表已返回该产品

BDD: 无关用户不可见不可改 -> Given 产品已有最新整产品指派 / When 无关用户访问列表、详情、草稿保存、提交审批或指派创建接口 / Then 后端显式拒绝并返回 access denied

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly+publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增权限场景在切到“当前登录用户”校验后先暴露出旧测试入口未按真实登录用户创建产品指派，说明旧测试与旧实现都还未完全落到新权限模型。

GREEN: `mvn -pl yudao-module-showroom -am -DskipTests compile` -> PASS

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly+publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#assignedEditorShouldOnlySeeAssignedProductAndBeDeniedForOtherProductDetail+wholeProductAssignmentShouldExposeFillingStatusAndAssignedEditorAccess+assignedEditorShouldKeepLifecycleVisibilityForAssignedProduct+assignedEditorShouldBeReadOnlyAfterWholeAssignmentClosed+supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly+publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

GREEN: inline Playwright live chain -> PASS，产品 `FULL1779293717036(id=210)` 由 `showroomeditor` 真实提交为 `changeRequestId=23`，`showroomsupervisor` 与 `aoteman` 在审批中心完成真实签名审批后，编辑人列表仍可见 `已发布` 状态产品。

- Blocker: 当前 `ruoyi-vue-pro` 工作区内存在大量与本任务无关的既有未提交改动，且部分与本任务命中的 showroom 文件重叠，无法安全创建单独提交。

BDD: 待审产品即使随后出现新的整产品指派也必须继续对当前审批人可见 -> Given 同一产品已有待主管审批变更单且之后又创建了一条新的整产品指派给其他部门编辑 / When 当前主管通过 `notifyOpen=approval` deep link 或产品详情接口访问该产品 / Then 后端仍必须按待审链放行列表与详情访问，不得被后创建的 assignment 遮蔽。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#supervisorPendingApprovalProductShouldStayVisibleEvenIfLaterWholeAssignmentExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增回归用例证明：当待审产品随后新增一条不归当前主管链的整产品指派时，产品页列表会错误返回 `total=0`。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#supervisorPendingApprovalProductShouldStayVisibleEvenIfLaterWholeAssignmentExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，controller 已把 `workflowFacade.listPendingApprovalsForReviewer(...)` 的产品待审域并入产品可见性判定。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer+supervisorPendingApprovalProductShouldStayVisibleEvenIfLaterWholeAssignmentExists+supervisorShouldSeeOnlyAssignedProductBeforeSubmissionAndStayReadOnly+publicityShouldSeeAllProductsWhileUnrelatedUserShouldSeeNoneAndCannotManage" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，待审可见性修复未破坏既有审批页、主管只读和企宣全量可见规则。

GREEN: 主租户真实 API 回归 -> PASS，`wangyiru / tenant-id=1` 对 `productId=214 / changeRequestId=28` 的接口结果已从：
- `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20&productId=214 -> total=0`
- `GET /admin-api/showroom/product/get?id=214 -> SHOWROOM_PRODUCT_ACCESS_DENIED`
修复为：
- `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20&productId=214 -> total=1`
- `GET /admin-api/showroom/product/get?id=214 -> code=0`
