# Task: DCC 四层审批真实 E2E 后端修复

## Goal

修复 DCC 真实四层审批 E2E 中暴露出的后端任务阶段识别错误：BPM 实际任务 `taskDefinitionKey=approveTask`，但 DCC 工作流校验仍只接受固定阶段编码，导致真实第一层审批即失败。

## Scope

- 检查同仓库上一条后端任务状态；若未完成，先显式阻塞或闭环。
- 在修改生产代码前创建当前任务文档、执行日志和后端证据。
- 严格按 BDD + TDD，先补失败测试再做最小修复。
- 只修复 DCC 审批动作中“真实 BPM 任务节点 -> DCC 阶段”识别与推进逻辑，不引入 fallback 分支。
- 若需要同时调整流程变量读取、快照匹配或阶段推进校验，一并在本任务内完成。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-dcc-category-matrix-derived-route/task.md`
- Status before this task: completed.
- Impact: matrix派生基础已经就绪，本任务只处理真实审批动作阶段识别问题。

## Milestones

- [x] M1: Confirm previous backend task status and create this task directory.
- [x] M2: Record BDD scenarios and RED evidence for the failing real approval action.
- [x] M3: Implement the minimal backend fix for stage resolution and approval progression.
- [x] M4: Run GREEN backend verification and update evidence.
- [x] M5: Commit only this task's backend changes if verification passes.

## Expected Verification

- The first real DCC approval action no longer returns `Controlled file task stage is not aligned with the fixed DCC workflow`.
- Backend tests cover generic BPM task keys used by the deployed DCC process.
- The fix does not break matrix review unanimity or matrix approval any-one semantics.

## Current Status

Completed on 2026-05-16. The backend now accepts the live generic `approveTask` node shape when it maps to the current fixed DCC stage, and the full real approval chain is verified again after the fix.

## Blocker And Impact

- Blocker: none remaining.
- Impact: the backend no longer blocks the first live approval action, and the four-stage category-derived workflow can complete end to end.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccBaseSchemaTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccApprovalRouteAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `npx --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-stage-approval-e2e\scripts\verify-dcc-four-stage-approval-e2e.mjs` -> PASS
- Real verification evidence: controlled file `2054545668044042256`, process instance `2e4339f3-5096-11f1-8526-00155d09335a`, final backend status `FINALIZING`.
