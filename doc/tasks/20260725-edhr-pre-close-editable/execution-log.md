# Execution Log: eDHR Submitted Form Editable Before Close

## User Intent

用户确认：当前填写提交之后，应该在关闭前都可以进行修改。

## Precheck

- PRECHECK: 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- PRECHECK: 已读取 `bug-regression-fix-loop`、`backend-api-delivery`、`frontend-feature-delivery` 技能及其 evidence contract。
- PRECHECK: 已读取 `docs/experience-index.md`，命中 eDHR 批次任务配置来源、静态合同与真实 E2E 同步、PowerShell/编码、脏工作区基线门禁。
- PRECHECK: `git status --short --branch` 显示当前仓库已有并发任务修改；本任务不会回滚或覆盖非本任务文件。

## BDD

- BDD: submitted ordinary form can be amended before batch close -> Given eDHR 普通工序表单已填写提交且批次尚未关闭, When 原填写人从批次详情点击打开填写并保存/重新提交, Then 系统允许进入填写页并更新提交签名证据。
- BDD: closed batch remains locked -> Given eDHR 批次已关闭、归档、驳回或作废, When 用户尝试打开已提交普通表单继续填写, Then 后端拒绝打开且前端不应提供可执行写入动作。

## Milestone Log

- IN_PROGRESS: 准备新增 RED 测试，覆盖放行待处理但批次未关闭时已提交普通表单仍可打开。
- RED: `node tests\e2e\edhr-pre-release-editable-submit-static.spec.js` -> FAIL, expected reason: 页面仍使用“放行前可修改”旧文案，未表达关闭前可修改。
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_pendingReleaseAllowsApprovedOrdinaryFillCompletedBeforeClose,MesProBatchRecordExecutionServiceImplTest#submitOrdinaryProcessResubmitAllowsPendingReleaseBeforeBatchClose' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, expected reason: 批次详情任务卡片仍未投影 `OPEN_FORM`，且提交执行测试先暴露缺少 `MesProEdhrRecordbookGlobalSettingService` 测试装配依赖。
- IMPLEMENTED: 后端 `MesProEdhrPreReleaseEditabilityService` 移除放行审批待处理对已提交普通表单修改/重提的阻断，保留批次关闭、归档、驳回、作废终态阻断。
- IMPLEMENTED: 后端 `MesProEdhrBatchExecutionServiceImpl` 将打开任务动作锁细分为 `requireBatchActionUnlockedForOpenTask`，仅允许已提交普通路线表单在放行锁下继续打开，其它批次动作仍受放行锁约束。
- IMPLEMENTED: 前端批次详情 `canOpenTask` 改为以后端 `OPEN_FORM` 动作授权为准，不再仅因任务状态 `APPROVED/填写完成` 禁用“打开填写”。
- IMPLEMENTED: 前端填写页提示文案改为“关闭前可修改，重新提交将更新提交签名证据”。
- GREEN: `node tests\e2e\edhr-pre-release-editable-submit-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_pendingReleaseAllowsApprovedOrdinaryFillCompletedBeforeClose,MesProBatchRecordExecutionServiceImplTest#submitOrdinaryProcessResubmitAllowsPendingReleaseBeforeBatchClose,MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_fillCompletedOrdinaryPendingReleaseAllowsBeforeBatchClose' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 3 tests, 0 failures, 0 errors.
- REGRESSION: `node tests\e2e\edhr-golden-finger-static.spec.js` -> PASS。
- REGRESSION: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_rejectsClosedBatch,MesProEdhrBatchExecutionServiceTest#releasePendingApproval_blocksCloseArchiveAndQualityReject,MesProBatchRecordExecutionServiceImplTest#submitBatchRecordExecution_goldenFingerStillRejectsTerminalExecution' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 3 tests, 0 failures, 0 errors.
- CHECK: `git diff --check` -> PASS with line-ending warnings only; no whitespace errors reported.
- EXPERIENCE: 已读取 `project-experience-consolidation`；本次“关闭前可修改”属于当前业务规则调整，可能随流程策略变化，不写入长期经验文档。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-pre-close-editable --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为空。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-pre-close-editable --mode apply` -> PASS，无删除项。
- CLOSEOUT BLOCKER: 当前仓库存在多项并发任务脏改和未跟踪目录，本任务未提交/推送，避免把非本任务文件混入提交。
