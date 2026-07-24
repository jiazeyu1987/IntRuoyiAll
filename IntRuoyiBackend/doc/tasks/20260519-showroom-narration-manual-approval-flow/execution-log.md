# Execution Log: 修复展厅讲解提交链路自动推进发布

BDD: 讲解提交后不得自动生效 -> Given 一个已生成讲解稿和讲解音频的讲解版本 / When 用户在讲解工作台执行提交动作 / Then 后端只能把状态推进到待主管审批，不能在同一次请求里自动完成主管审批、高昕审批和发布。

BDD: 讲解只有人工审批并确认发布后才生效 -> Given 一个讲解版本已经依次完成主管审批和高昕审批 / When 用户显式执行发布确认 / Then 讲解版本才允许进入 `PUBLISHED` 并被 live 读取链路使用。

BDD: 已批准但未发布的讲解不能被误判为已生效 -> Given 一个讲解版本状态为 `APPROVED` 但尚未发布 / When 后台工作台展示当前状态 / Then 界面必须明确显示“已批准待发布”而不是“已发布”或成功生效。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest#narrationSubmitShouldStayPendingUntilManualApprovalsAndPublish" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ShowroomHttpApiIntegrationTest.narrationSubmitShouldStayPendingUntilManualApprovalsAndPublish` 断言收到 `PUBLISHED`，证明当前 `/showroom/narration/submit` 仍会自动推进发布。

M3: Completed. 后端新增 `/showroom/narration/supervisor-approve`、`/showroom/narration/gaoxin-approve`、`/showroom/narration/publish` 三个人工动作接口；`submitNarration` 只保留提交；前端讲解工作台已改成按状态显示“提交审批 / 主管审批通过 / 高昕审批通过 / 确认发布”，并把 `APPROVED` 文案改为“已批准待发布”。

RED: `node --test scripts/showroom-admin-workflow-workbenches.test.mjs` -> FAIL，`NarrationWorkspace.vue` 缺少“人工确认”提示与 `manualConfirmed` 闸门，草稿态仍可直接点“提交审批”。

GREEN: `node --test scripts/showroom-admin-workflow-workbenches.test.mjs` -> PASS，讲解工作台已增加“人工确认”提示、`manualConfirmed` 提交闸门，且草稿态提交按钮不再是直通动作。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-workflow-workbenches.test.mjs` -> PASS，前端 API 暴露、讲解工作台动作按钮、人工确认闸门与 `APPROVED` 状态文案/标签语义均已切换到人工审批后生效。

BLOCKER: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 当前工作区已有 `ShowroomPersistentContentService` 与内容 DO 契约不一致，编译阶段就出现大量 getter/setter/builder 缺失错误，导致本任务无法在现有工作区完成后端 GREEN。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomAssignmentWorkflowTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，当前 showroom 后端代码快照已恢复可编译并通过目标回归，讲解提交流转保持“人工确认 -> 提交审批 -> 主管审批 -> 企宣审批 -> 确认发布”的手动链路。
