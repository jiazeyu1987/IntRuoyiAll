# Bug Regression Evidence - FDA Audit Trace Permission Scope

## Summary

- Bug summary: 写入型真实 UI E2E 创建本地 PRECHECK 样本后，批次追溯操作审计查询被对象级权限范围拒绝。
- Expected behavior: 本地状态样本创建出的 batch task 必须具备可审计的 `BATCH_EXECUTION_TASK` permission scope，使批次追溯可通过 `AUDIT_VIEW` 门禁展示 `LOCAL_STATE_SAMPLE_CREATE`。
- Reproduction: `node doc\tasks\20260724-batch-fda-audit-log-coverage\operation-audit-trace-write-sample.e2e.cjs` -> FAIL，错误为 `eDHR 对象级权限范围不存在或未启用：BATCH_EXECUTION:900000000788`。

## Root Cause

- `MesProEdhrLocalStateSampleServiceImpl` 插入 `MesProEdhrBatchExecutionTaskDO` 后未创建/绑定 `permissionScopeId`，而 `MesProEdhrOperationAuditController` 对批次追溯按 batch task scope 执行 `AUDIT_VIEW`。

## Regression Test

- `MesProEdhrLocalStateSampleServiceTest#createLocalStateSample_writesExpectedStateCombination` 断言 batch task 写入权限 scope。
- 静态契约断言服务保存 `BATCH_EXECUTION_TASK` / `AUDIT_VIEW` scope 并回写 `permissionScopeId`。

## Verification

- RED: 静态契约新增断言后 FAIL，证明生产代码未绑定 scope。
- GREEN: 静态契约 PASS，生产代码已保存 scope 并回写 `permissionScopeId`。
- Risk: 修复只影响本地状态样本创建路径；正式业务批次仍沿用既有权限范围来源。
- Blockers: Maven compile/JUnit 被非本任务 route projection 编译错误阻塞；修复后真实 E2E 需待后端可编译并重启后重跑。

## Final E2E Pass - 2026-07-25 15:25 Asia/Shanghai

- GREEN: The missing permission scope regression is fixed in runtime and verified through real UI E2E.
- Verification: sample `900000000799` displays `LOCAL_STATE_SAMPLE_CREATE` in batch trace operation audit with `ALLOW` / `SUCCESS`.
- Regression scope: local state sample creation now binds `BATCH_EXECUTION_TASK` / `AUDIT_VIEW`; ordinary production batch permission scope behavior is unchanged.
- Blockers: none for the requested E2E path.
