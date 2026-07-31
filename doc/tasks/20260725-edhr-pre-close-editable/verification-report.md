# Verification Report: eDHR Submitted Form Editable Before Close

## Scope

- 后端：已提交普通工序表单的关闭前修改、字段审计保存、重新提交、打开任务动作投影。
- 前端：批次详情“打开填写”按钮启用条件、填写页关闭前可修改提示。
- 非目标：不放开已关闭、已归档、已驳回、已作废批次；不放开放行审批、关闭、作废等批次级动作锁。

## Results

- PASS: `node tests\e2e\edhr-pre-release-editable-submit-static.spec.js`。
- PASS: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_pendingReleaseAllowsApprovedOrdinaryFillCompletedBeforeClose,MesProBatchRecordExecutionServiceImplTest#submitOrdinaryProcessResubmitAllowsPendingReleaseBeforeBatchClose,MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_fillCompletedOrdinaryPendingReleaseAllowsBeforeBatchClose' '-Dsurefire.failIfNoSpecifiedTests=false' test`。
- PASS: `node tests\e2e\edhr-golden-finger-static.spec.js`。
- PASS: `mvn -pl yudao-module-mes -am '-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_rejectsClosedBatch,MesProEdhrBatchExecutionServiceTest#releasePendingApproval_blocksCloseArchiveAndQualityReject,MesProBatchRecordExecutionServiceImplTest#submitBatchRecordExecution_goldenFingerStillRejectsTerminalExecution' '-Dsurefire.failIfNoSpecifiedTests=false' test`。
- PASS: `git diff --check`，仅输出 Windows CRLF 替换提示，无 whitespace error。
- PASS: task-closeout-cleanup preview/apply，无删除项、无 blocked、无 warnings。

## Open Items

- 当前仓库有并发任务脏改和未跟踪目录，本任务实现未提交/推送；提交前必须先按仓库规则处理或明确隔离非本任务改动。
