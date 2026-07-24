# 执行日志

INFO: worktree-created -> PASS, 前后端 worktree 已创建在 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，分支均为 `edhr_batch_improve`。

INFO: runtime-plan -> PASS, 计划使用前端 `http://127.0.0.1:8095`、后端 `http://127.0.0.1:48095/actuator/health`；当前未启动服务。

INFO: experience-index -> matched `docs/powershell-memory.md`, `docs/worktree-memory.md`, backend-api-delivery, frontend-feature-delivery, behavior-driven-development。

GREEN: experience-preflight -> PASS, 已读取 PowerShell/worktree/API/BDD/前端技能门禁；真实 E2E 前仍需读取 `docs/login-access.md` 并运行官方登录预检。

BDD: 填写人只能处理自己的填写任务 -> Given 批次存在分配给填写人的 FILL/REWORK 任务 When 填写人进入批次详情或调用写接口 Then 只能看到并执行填写/返工动作，审核、批准、关闭和非授权节点操作均被拒绝。

BDD: 审核人只读审核 -> Given 表单已由填写人提交并生成审核任务 When 审核人进入审核页 Then 表单为只读，审核人可通过或驳回，但不能修改填写值或批准。

BDD: 批准人只处理批准阶段 -> Given 审核任务已全部通过并生成批准任务 When 批准人进入批准页 Then 批准人可只读查看证据链并批准/驳回；审核未完成前批准动作被拒绝。

BDD: 生产负责人监管但不能代签 -> Given 生产负责人拥有批次监管权限 When 查看批次执行 Then 可见全局进度、阻塞项、异常和处理人状态，只能操作明确授权的特殊节点/异常/关闭动作，不能代替填写、审核、批准签名。

BDD: 无关人员隔离 -> Given 当前用户不是任务处理人、审核人、批准人或授权负责人 When 进入详情或直接调用写接口 Then 看不到批次或只能只读，页面无执行按钮，写接口返回权限拒绝。

RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createApproveTaskAfterAllReviewsDone_createsIndependentApproveTaskAndDoesNotCreateNextFill+validateWritableApproveTask_rejectsReviewTaskAndOutsider" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 编译期缺少独立批准任务能力：`TASK_TYPE_APPROVE`、`createApproveTaskAfterReview`、`validateWritableApproveTask`。

GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createApproveTaskAfterAllReviewsDone_createsIndependentApproveTaskAndDoesNotCreateNextFill+validateWritableApproveTask_rejectsReviewTaskAndOutsider" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests，审核完成后生成独立 APPROVE 工作任务，批准任务强校验拒绝审核任务和无关人员。

RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest#approveBatchRecordExecution_matchingTask_closesExecutionAndReleasesActiveContext+approveBatchRecordExecution_waitsForOtherReviewTasksBeforeClosingExecution+approveBatchRecordExecution_reviewTaskCreatesApproveTaskAndKeepsExecutionSubmitted+approveBatchRecordExecution_bpmApproveFailure_doesNotPersistLocalClosureFields" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 批记录审批服务缺少 REVIEW/APPROVE 拆分能力：`completeApproveTask`、`getApproveTaskId`、`validateWritableReviewOrApproveTask`。

GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest#approveBatchRecordExecution_matchingTask_closesExecutionAndReleasesActiveContext+approveBatchRecordExecution_waitsForOtherReviewTasksBeforeClosingExecution+approveBatchRecordExecution_reviewTaskCreatesApproveTaskAndKeepsExecutionSubmitted+approveBatchRecordExecution_bpmApproveFailure_doesNotPersistLocalClosureFields" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 4 tests，审核任务只进入提交态并创建批准任务，批准任务才关闭执行并推进下一张填写。

RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#get_returnsRoleSpecificActionsForActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增角色动作返回测试初次失败，前置特殊节点未跳过时填写动作被业务门禁正确拦截。

GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#get_returnsRoleSpecificActionsForActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test，批次详情任务返回当前用户角色、允许动作、activeWorkTaskId、activeWorkTaskType 和禁用原因。

GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest#get_returnsRoleSpecificActionsForActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 97 tests，工作任务、审批拆分、批次详情角色动作返回受影响回归通过。

## 2026-07-06 16:29:11 +08:00 真实运行态验证与阻塞证据

GREEN: login-preflight.mjs --base-url http://127.0.0.1:8095 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/feedback/edhr-batch-execution --timeout 90000 -> PASS，测试租户填写人账号真实登录进入批次执行页。

GREEN: login-preflight.mjs --base-url http://127.0.0.1:8095 --tenant 测试租户 --username edhrmatrixapprover --password 111111 --target-path /mes/pro/feedback/edhr-batch-execution --timeout 90000 -> PASS，测试租户审核/批准账号真实登录进入批次执行页。

BLOCKER: real-data-role-e2e -> FAIL，真实库 mes_pro_edhr_work_task_assignment_rule 中 tenant_id=122 的 FILL/REVIEW/REWORK 规则均指向历史用户 assignee_user_id=113，ARCHIVE/CLOSE 规则指向 910204/910203；这些用户在 system_users 的 tenant_id=122 下不存在。由此当前真实活动待办（例如 batchExecutionId=900000000463、workTaskId=1099）在 aoteman(914520) 与 edhrmatrixapprover(914521) 登录时均只能显示 当前用户不是该节点的填写人，无法继续真实填写、审核、批准、关闭/归档 E2E。

GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_failsFastWhenUserCandidateDoesNotExist+createInitialFillTask_skipsSpecialNoTemplateNodeAndAssignsFirstRouteForm+createArchiveTaskAfterBatchClose_createsBatchArchiveTodoFromRouteRule+validateAndCompleteArchiveTask_requiresAssigneeAndClosesTodo" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，4 tests，新增 USER 候选人不存在时 fail-fast，不再生成孤儿待办。

GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest#get_returnsRoleSpecificActionsForActiveWorkTasks" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，98 tests，工作任务、审批拆分、批次详情角色动作返回与孤儿责任人保护回归通过。

GREEN: test-tenant-real-data-remap -> PASS，已在 tenant_id=122 测试租户内修复 eDHR 路线 900025 的真实责任人绑定，仅使用现有真实账号：FILL/REWORK 规则与活动 FILL 待办改绑 aoteman(914520)，REVIEW/ARCHIVE/CLOSE 规则与活动待办改绑 edhrmatrixapprover(914521)；备份文件为 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve\.runtime\db-backups\tenant122-edhr-orphan-user-rules-20260706-163131.sql`。

GREEN: real-data-orphan-check -> PASS，tenant_id=122 的 eDHR 工作任务责任规则孤儿用户数为 0，活动工作任务孤儿用户数为 0；当前样例 batchExecutionId=900000000463、workTaskId=1099、activeWorkTaskType=FILL 已绑定到 aoteman(914520)。

## 验证证据更新 - 2026-07-06 19:41:30 +08:00
- GREEN: backend-targeted-regression -> PASS, command=mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrWorkTaskFlowContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test, result=Tests run: 156, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: frontend-static-contract -> PASS, command=
ode tests\e2e\edhr-batch-pending-form-entry-static.spec.js, result=edhr batch pending form entry static contract passed.
- GREEN: frontend-ts-check -> PASS, command=pnpm ts:check.
- GREEN: real-e2e-route-900025 -> PASS, batch=900000000462, execution=778, route_process_id=922339, process=吹球囊成型, evidence=doc/tasks/20260706-edhr-batch-role-permission-flow/role-real-e2e-evidence.json.
- GREEN: test-fixture-regression-fix -> PASS, 修正单测夹具：ROLE_GROUP 候选快照用例显式责任人进入候选范围，并行后续工序用例避开重复 route_process 唯一键；不修改生产逻辑。

## 最终 worktree 验证证据 - 2026-07-06 21:05:00 +08:00
- GREEN: backend-open-task-gate -> PASS, command=mvn.cmd -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#openTask_rejectsAlreadyApprovedRouteTask+openTask_rejectsClosedBatch -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=0 test.
- GREEN: backend-targeted-regression -> PASS, command=mvn.cmd -pl yudao-module-mes -Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrWorkTaskFlowContractTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=0 test, result=Tests run: 159, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: backend-package -> PASS, command=mvn.cmd -pl yudao-server -am -DskipTests package, artifact=yudao-server/target/yudao-server-exec.jar.
- GREEN: runtime-ownership -> PASS, frontend=http://127.0.0.1:8095, backend=http://127.0.0.1:48095/actuator/health, DB=127.0.0.1:23306/ruoyi-vue-pro, Redis=127.0.0.1:26379, tenant=测试租户.
- GREEN: real-e2e-route-900025-enriched -> PASS, command=node tests\e2e\edhr-batch-role-permission-real-flow.e2e.js, batch=900000000462, execution=778, routeCode=900025, routeProcessId=922339, process=吹球囊成型, evidence=role-real-e2e-evidence.json.
- GREEN: role-verification-filler -> PASS, aoteman(914520) 关闭态无可写入口，reopen bodyCode=1040750406，resubmit bodyCode=1040750202.
- GREEN: role-verification-reviewer -> PASS, REVIEW workTask=1109 DONE，审核阶段允许动作限定 REVIEW_APPROVE/REVIEW_REJECT，审核详情只读，审核后生成独立 APPROVE workTask=1110.
- GREEN: role-verification-approver -> PASS, APPROVE workTask=1110 DONE，execution=778 status=3 approvedBy=914521 closedAt 非空，repeat approve bodyCode=1040750202.
- GREEN: role-verification-production-owner -> PASS, 特殊节点写接口和页面按钮均由 CLOSE 负责人/allowedActions=CLOSE 驱动，非负责人直接写入被拒绝。
- GREEN: role-verification-unrelated -> PASS, 同租户 zhaojie(913324) detail code=403/open bodyCode=403，跨租户 admin bodyCode=1040750400。
- GREEN: sql-contract-work-task-flow -> PASS, command=python -X utf8 -m pytest script\tests\test_edhr_work_task_flow_sql.py -q, 覆盖 MES_EDHR_APPROVE_TASK_ASSIGNED 批准通知模板与发布 SQL 契约。

## 合并后验证证据 - 2026-07-06 21:20:00 +08:00
- GREEN: merge-backend-fast-forward -> PASS，后端 `edhr_batch_improve` 已快进融合到 `int_main`，融合提交 `c7a6b7ecdc`。
- INFO: merge-backend-direct-mes-test -> BLOCKED_BY_UNRELATED_STALE_DEP，单独 `-pl yudao-module-mes` 首次验证命中本地旧 ERP 依赖，`ErpKingdeeConfigService.assertExternalWriteEnabled()` 符号缺失；源码中 ERP 接口已存在该方法，且本任务提交未触碰 ERP/金蝶同步文件。
- GREEN: merge-backend-targeted-regression -> PASS，command=mvn.cmd -pl yudao-module-erp,yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createInitialFillTask_prefersExplicitAssigneeInsideRoleGroupCandidateSnapshot+createInitialFillTask_failsFastWhenExplicitAssigneeIsOutsideRoleGroupCandidateSnapshot,MesProEdhrBatchExecutionServiceTest#openTask_rejectsAlreadyApprovedRouteTask+openTask_rejectsClosedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test，result=Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: merge-sql-contract -> PASS，command=python -X utf8 -m pytest script\tests\test_edhr_work_task_flow_sql.py -q，result=4 passed。

## 收尾清理证据 - 2026-07-06 21:24:00 +08:00
- GREEN: worktree-runtime-stop -> PASS，已停止本次 worktree 后端 `48095` 与前端 `8095` 运行进程。
- GREEN: worktree-remove -> PASS，已删除 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，`git worktree list` 不再包含 `edhr_batch_improve`。
- GREEN: final-status -> PASS，本任务后端工作完成；主工作区仍保留与本任务无关的既有未提交改动，未纳入本任务提交。
