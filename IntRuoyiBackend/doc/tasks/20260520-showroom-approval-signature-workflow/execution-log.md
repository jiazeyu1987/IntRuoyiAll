# Execution Log: 展厅审批签名流程与站内信审批跳转（后端）

BDD: 有主管时走主管到企宣 -> Given 提交人能解析到有效主管 / When 提交产品审批 / Then 变更单必须先进入主管待审，再推进到企宣待审。

BDD: 缺主管时直接进企宣 -> Given 提交人无部门、部门无负责人或负责人无效 / When 提交产品审批 / Then 变更单必须直接进入企宣待审，不得报主管缺失错误。

BDD: 审批动作必须验签 -> Given 审批人在主管或企宣阶段执行通过/驳回 / When 未通过 DCC 电子签名授权或密码验签 / Then 接口必须失败，且状态不得推进。

BDD: 待审批通知必须带审批跳转语义 -> Given 变更单进入新的待审批阶段 / When 系统发送站内信 / Then `templateParams` 必须包含审批跳转所需的 `targetType/targetId/changeRequestId/notifyOpen=approval`。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomWorkflowApprovalTest,ShowroomAssignmentWorkflowTest,ShowroomApprovalSignatureServiceTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前 `showroom_change_request_signature` 表、`ShowroomApprovalSignatureService`、`ShowroomChangeRequestSignatureDO/Mapper` 与 DCC 电子签名授权复用依赖均不存在；同时 `ShowroomApprovalDetail.signatureRecords()` 与新的审批请求体字段尚未实现。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomWorkflowApprovalTest,ShowroomAssignmentWorkflowTest,ShowroomApprovalSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，后端 showroom 核心 contract/workflow/assignment/signature 单测通过。

GREEN: `mvn -pl yudao-module-showroom -DskipTests compile` -> PASS，后端主代码可编译。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#approvalGetShouldReturnDiffRichDetailInsteadOfBareRequest+approvalPageShouldOnlyReturnPendingTasksForCurrentReviewer+assignmentEndpointsShouldPersistNotifyLinkageAndAutoSubmit+productSubmitShouldAllowWholeDraftDiffWithoutExplicitFieldCodes+productSubmitShouldStartAtGaoxinApprovalWhenSubmitterDeptMissing+productWorkflowShouldCreateNotifyMessagesForReviewersAndSubmitter" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，和本次审批签名主线直接相关的 6 条 integration 子集通过。

INFO: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前整份 integration 集仍有大量本任务外的既有失败，集中在登录上下文、产品权限和 narrations/批处理断言漂移。
