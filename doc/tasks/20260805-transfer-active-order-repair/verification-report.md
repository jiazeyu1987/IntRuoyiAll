# Verification Report

## Scope

记录调拨写入口与活跃订单边界修复的验证证据：MES 调拨只读化、活跃订单确认态校验、活跃订单当前班组长范围隔离。

## Results

- PASS: `node tests/e2e/mes-wm-transfer-readonly-static.spec.cjs`，确认调拨列表/详情不再暴露手工新增、编辑、删除、提交、确认、上架、完成、取消入口。
- PASS: 隔离 worktree `D:\IntRuoyiWorktree\transfer-active-order-verify-20260805` 中运行 `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesWmTransferManualWriteControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`；`MesWmTransferManualWriteControllerTest` 3 tests、`MesTeamLeaderActiveOrderServiceTest` 14 tests，合计 17 tests / 0 failures / 0 errors。
- PASS: 主工作区运行 `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesWmTransferManualWriteControllerTest,MesActiveOrderTransferTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`；BUILD SUCCESS，21 tests / 0 failures / 0 errors / 0 skipped。
- PASS: `node IntRuoyiFronted\tests\e2e\mes-wm-transfer-readonly-static.spec.cjs`，确认当前前端调拨入口仍保持只读。
- CONFIRMED BY INSPECTION: `MesTeamLeaderActiveOrderServiceImpl` 当前使用 `validateWorkOrderConfirmed` 与 `selectActiveListByLeader(leaderUserId)`；调拨单/行/明细控制器写接口均抛 `WM_TRANSFER_MANUAL_OPERATION_FORBIDDEN`。
- PASS: `task-closeout-cleanup` preview/apply 均通过，delete `<none>`、blocked `<none>`；任务专属验证 worktree `D:\IntRuoyiWorktree\transfer-active-order-verify-20260805` 已删除，`Test-Path` 复核为 `False`。

## Remaining Risks

- QA/PQC 与 AC-M10 SOP 无订单报工链路未纳入本切片。
- 当前仓库存在并行任务改动和 `ahead 13` 状态；本任务未提交/推送，避免混入非本任务变更，因此任务状态保持 `ready_for_closeout`。
