# eDHR 批次执行角色化权限与操作体验改造（后端）

## 任务目标
在独立 worktree `edhr_batch_improve` 中完成 eDHR 批次执行后端角色化权限与动作能力改造，使填写人、审核人、批准人、生产负责人和无关人员在批次执行、填写、审核、批准、监管和隔离场景下拥有明确且后端强校验的可见性与可操作性。

## Milestones
- [x] 建立后端任务记录、经验门禁、BDD 场景和运行态计划
- [ ] 盘点现有批次执行、工作任务、审批和放行相关服务/接口
- [ ] 编写失败测试覆盖角色能力字段、写操作授权拒绝、批准任务模型
- [ ] 实现后端能力字段与写操作强校验
- [x] 运行后端单元/集成/契约回归
- [ ] 支撑真实 E2E 和 5 个角色子 agent 验收（阻塞：测试租户真实待办规则指向不存在历史用户）
- [x] 提交、融合 `int_main`、合并后回归和 worktree 清理

## Expected Verification
- 后端详情/任务接口返回当前用户维度的 `visible / currentUserRole / allowedActions / disabledReason / assignee / reviewer / approver` 等能力字段。
- 填写、保存、提交、审核、批准、驳回、特殊节点跳过/完成、关闭批次等写操作均按当前登录人、任务类型、任务归属和节点状态强校验。
- 独立批准任务模型存在或被补齐，审核未完成时不能批准。
- 无关人员绕过页面直接调用写接口会被后端拒绝。
- 后端测试、真实 E2E 和 5 个角色验收均通过。

## Current Status
completed

## 当前状态
后端与前端角色化能力字段、审核/批准拆分、孤儿责任人 fail-fast 保护、特殊节点 CLOSE 负责人强校验、关闭态 openTask 写入拒绝均已实现。路线 900025 / 批次 900000000462 / 执行 778 已通过 worktree 真实数据 E2E；填写人、审核人、批准人、生产负责人、无关人员 5 个角色独立复核均 PASS。后端提交 `c7a6b7ecdc` 已快进融合 `int_main`，合并后 ERP+MES reactor 关键回归、SQL 契约、前端静态检查均 PASS；`edhr_batch_improve` worktree 已删除。本任务完成。

## Previous Task Check
- 后端 worktree 从 `int_main` 创建；仓库内既有任务文档很多，最近文档为 checkout 时间戳，不足以可靠判定唯一“上一任务”。
- 本任务在独立 worktree、独立分支中推进，不接管主工作区未提交改动。

## 经验门禁
- PowerShell：已读取 `docs/powershell-memory.md`，命令使用 UTF-8，禁止 `&&`。
- Worktree：已读取 `docs/worktree-memory.md` 与 worktree skill；开发必须在 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，不复用主工作区 `8081/48081` 做证据。
- 后端 API：已读取 backend-api-delivery skill 和 evidence contract；权限、校验、错误必须后端可信，缺少前置条件时 fail fast。
- BDD/TDD：先记录 Given/When/Then，再 RED -> GREEN -> REGRESSION。
- 真实 E2E：执行前必须读取 `docs/login-access.md` 并跑官方登录预检；写入型 E2E 优先测试租户，芋道源码默认只读。
- 禁止 fallback：不得用 mock、静默降级、临时权限放宽或接口绕过掩盖问题。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，后端作为权限与动作能力唯一可信来源。
- 是否存在临时补丁或绕过：否。

## BDD Scenarios
BDD: 填写人只能处理自己的填写任务 -> Given 批次存在分配给填写人的 FILL/REWORK 任务 When 填写人进入批次详情或调用写接口 Then 只能看到并执行填写/返工动作，审核、批准、关闭和非授权节点操作均被拒绝。

BDD: 审核人只读审核 -> Given 表单已由填写人提交并生成审核任务 When 审核人进入审核页 Then 表单为只读，审核人可通过或驳回，但不能修改填写值或批准。

BDD: 批准人只处理批准阶段 -> Given 审核任务已全部通过并生成批准任务 When 批准人进入批准页 Then 批准人可只读查看证据链并批准/驳回；审核未完成前批准动作被拒绝。

BDD: 生产负责人监管但不能代签 -> Given 生产负责人拥有批次监管权限 When 查看批次执行 Then 可见全局进度、阻塞项、异常和处理人状态，只能操作明确授权的特殊节点/异常/关闭动作，不能代替填写、审核、批准签名。

BDD: 无关人员隔离 -> Given 当前用户不是任务处理人、审核人、批准人或授权负责人 When 进入详情或直接调用写接口 Then 看不到批次或只能只读，页面无执行按钮，写接口返回权限拒绝。

## Verification Log
- RED: `MesProEdhrWorkTaskServiceImplTest#createApproveTaskAfterAllReviewsDone_createsIndependentApproveTaskAndDoesNotCreateNextFill+validateWritableApproveTask_rejectsReviewTaskAndOutsider` -> FAIL，缺少独立 APPROVE 任务模型与批准任务写入校验。
- GREEN: `MesProEdhrWorkTaskServiceImplTest#createApproveTaskAfterAllReviewsDone_createsIndependentApproveTaskAndDoesNotCreateNextFill+validateWritableApproveTask_rejectsReviewTaskAndOutsider` -> PASS，审核后创建独立 APPROVE 工作任务，批准任务拒绝审核任务与无关人员。
- GREEN: `MesProBatchRecordExecutionServiceImplTest` 审批拆分目标用例 -> PASS，REVIEW 只创建批准任务，APPROVE 才关闭执行并推进。
- GREEN: `MesProEdhrBatchExecutionServiceTest#get_returnsRoleSpecificActionsForActiveWorkTasks` -> PASS，批次详情按当前用户返回角色、允许动作、活动工作任务和禁用原因。
- GREEN: `MesProEdhrBatchExecutionServiceTest#specialNodeWriteApis_rejectNonCloseOwner` -> PASS，非 CLOSE 负责人绕过前端调用特殊节点写接口会被拒绝。
- GREEN: `MesProEdhrBatchExecutionServiceTest#openTask_rejectsAlreadyApprovedRouteTask+openTask_rejectsClosedBatch` -> PASS，已完成任务与关闭批次不能重新打开填写。
- REGRESSION: `mvn.cmd -pl yudao-module-mes -Dtest=MesProEdhrWorkTaskServiceImplTest,MesProBatchRecordExecutionServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrWorkTaskFlowContractTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=0 test` -> PASS，159 tests，0 failures，0 errors。
- BUILD: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，生成 worktree 后端 jar 并用于 48095 真实 E2E。
- REAL E2E: `node tests\e2e\edhr-batch-role-permission-real-flow.e2e.js` -> PASS，baseUrl `http://127.0.0.1:8095`，backend `http://127.0.0.1:48095`，batch `900000000462`，execution `778`，process `吹球囊成型`，证据 `role-real-e2e-evidence.json`。
- ROLE VERIFY: 填写人、审核人、批准人、生产负责人、无关人员 5 个角色子任务复核均 PASS。


## 当前状态更新 - 2026-07-06 19:41:30 +08:00
- 已完成真实数据 E2E 与前后端 targeted 回归。
- 后端 targeted 回归：156 个用例通过，0 failure / 0 error。
- 等待五角色独立子任务复核、提交、融合 int_main、清理 worktree。

## 当前状态更新 - 2026-07-06 21:05:00 +08:00
- 最新后端 targeted 回归：159 个用例通过，0 failure / 0 error。
- 真实 E2E 已基于 worktree 后端 48095 / 前端 8095 重跑通过，证据顶层记录 routeCode=900025、routeId=922046、routeProcessId=922339、batchExecutionId=900000000462、batchTaskId=2732。
- 角色复核全部 PASS：填写人关闭态 reopen/resubmit 被拒绝；审核人 REVIEW 任务 DONE 且审核阶段动作限定为 REVIEW_APPROVE/REVIEW_REJECT；批准人 APPROVE 任务 DONE 且重复 approve 被拒绝；生产负责人特殊节点只由 CLOSE 动作驱动；无关人员同租户 403 或只读无动作、跨租户不可见。
- 当前进入提交、融合 `int_main`、合并后验证和 worktree 清理阶段。

## 合并后验证证据 - 2026-07-06 21:20:00 +08:00
- GREEN: merge-backend-fast-forward -> PASS，后端 `edhr_batch_improve` 已快进融合到 `int_main`，融合提交 `c7a6b7ecdc`。
- INFO: merge-backend-direct-mes-test -> BLOCKED_BY_UNRELATED_STALE_DEP，单独 `-pl yudao-module-mes` 首次验证命中本地旧 ERP 依赖，`ErpKingdeeConfigService.assertExternalWriteEnabled()` 符号缺失；源码中 ERP 接口已存在该方法，且本任务提交未触碰 ERP/金蝶同步文件。
- GREEN: merge-backend-targeted-regression -> PASS，command=mvn.cmd -pl yudao-module-erp,yudao-module-mes "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_usesProcessFormPermissionRuleCandidateSnapshot+createInitialFillTask_prefersExplicitAssigneeInsideRoleGroupCandidateSnapshot+createInitialFillTask_failsFastWhenExplicitAssigneeIsOutsideRoleGroupCandidateSnapshot,MesProEdhrBatchExecutionServiceTest#openTask_rejectsAlreadyApprovedRouteTask+openTask_rejectsClosedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test，result=Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: merge-sql-contract -> PASS，command=python -X utf8 -m pytest script\tests\test_edhr_work_task_flow_sql.py -q，result=4 passed。

## 收尾清理证据 - 2026-07-06 21:24:00 +08:00
- GREEN: worktree-runtime-stop -> PASS，已停止本次 worktree 后端 `48095` 与前端 `8095` 运行进程。
- GREEN: worktree-remove -> PASS，已删除 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_batch_improve`，`git worktree list` 不再包含 `edhr_batch_improve`。
- GREEN: final-status -> PASS，本任务后端工作完成；主工作区仍保留与本任务无关的既有未提交改动，未纳入本任务提交。
