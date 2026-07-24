# Execution Log: 展厅企宣指定用户修改产品信息（后端）

BDD: 企宣创建产品整单指派后状态进入指派中 -> Given 产品存在且企宣选择了一个启用的编辑用户 / When 调用 `POST /showroom/assignment/create` 创建 `__PRODUCT_ALL_FIELDS__` 整单指派 / Then 后端返回 OPEN 指派记录，产品列表和详情都将当前产品状态呈现为 `IN_FILLING` 并返回指派对象。

BDD: 被指派用户只能修改被指派产品 -> Given 当前登录用户只有展厅编辑角色且存在 OPEN 产品整单指派 / When 访问产品列表和产品详情 / Then 列表只包含指派给自己的产品，访问未指派产品详情必须 fail-fast。

BDD: 整产品指派提交后被主管驳回会回到填写中 -> Given 一个通过整产品指派形成的产品审批单仍对应原 `__PRODUCT_ALL_FIELDS__` 指派 / When 主管驳回该审批单 / Then 系统必须给提交人发送真实驳回站内信、重新打开原整产品指派，并让产品状态重新呈现为 `IN_FILLING`。

BDD: 整产品指派提交后被企宣驳回会回到填写中 -> Given 一个通过整产品指派形成的产品审批单已经流转到企宣审批 / When 企宣驳回该审批单 / Then 系统必须给提交人发送真实驳回站内信、重新打开原整产品指派，并让产品状态重新呈现为 `IN_FILLING`。

BDD: 非整产品指派的手工提交驳回后保持已驳回 -> Given 一个没有原整产品指派的手工产品提交被驳回 / When 驳回通知发送完成 / Then 系统只发送真实驳回站内信，不自动补建或重开整产品指派，产品状态保持 `REJECTED`。

- 2026-05-20：按用户批准方案，将“驳回后回到指派中并通过站内信直接回改”并入本 task-id，不新开后端任务号。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAssignmentWorkflowTest#wholeProductAssignmentShouldReopenAfterRejectedChangeRequest+ShowroomHttpApiIntegrationTest#wholeProductAssignmentRejectedBySupervisorShouldReopenAssignmentAndNotifySubmitterForEdit+ShowroomHttpApiIntegrationTest#wholeProductAssignmentRejectedByGaoxinShouldReopenAssignmentAndNotifySubmitterForEdit+ShowroomHttpApiIntegrationTest#manualRejectedSubmissionShouldStayRejectedWithoutReopenedAssignment" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新场景暴露缺少“按 lastChangeRequestId 重开原整产品指派”、缺少驳回通知模板，以及驳回动作对真实登录/签名上下文的依赖。

GREEN: `pytest script/tests/test_showroom_sql_scripts.py -q` -> PASS

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomAssignmentWorkflowTest#wholeProductAssignmentShouldReopenAfterRejectedChangeRequest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS

BLOCKED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#wholeProductAssignmentRejectedBySupervisorShouldReopenAssignmentAndNotifySubmitterForEdit+wholeProductAssignmentRejectedByGaoxinShouldReopenAssignmentAndNotifySubmitterForEdit+manualRejectedSubmissionShouldStayRejectedWithoutReopenedAssignment" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前 `yudao-module-showroom` 中已有多份与本次需求无关的旧测试仍引用过时的 showroom 内容模型/构造签名，`testCompile` 阶段即被这些存量编译错误阻断，导致本次 3 个驳回场景暂时无法独立完成集成验证。
