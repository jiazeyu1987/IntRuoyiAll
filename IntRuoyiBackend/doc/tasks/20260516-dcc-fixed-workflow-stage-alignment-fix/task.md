# Task: DCC 固定四层审批阶段对齐修复

## Goal

修复真实 DCC 四层审批链路中“提交成功但第一层审批失败”的后端缺陷，使固定四层分类派生流程在真实待办任务上能够按 `文控审核 -> 审核会签 -> 批准 -> 文控批准` 顺序推进。

## Scope

- 基于真实 E2E 暴露的 live blocker 建立当前后端任务文档，再开始生产代码修改。
- 严格按 BDD + TDD 记录 RED 证据、补充或更新后端测试，再做最小实现。
- 排查 DCC 固定四层审批阶段与 BPM 当前待办节点、流程变量、路线快照、阶段状态推进之间的对齐逻辑。
- 不引入 fallback，不绕过真实 BPM 待办，也不把固定四层退回为单节点通用审批。
- 修复后必须重启本地后端，并通过真实 Playwright 路径重新验证上传、待办生成和第一层审批通过。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-dcc-category-matrix-derived-route/task.md`
- Status before this task: completed.
- Impact: the matrix-derived route implementation is already landed, and this task focuses only on the runtime stage-alignment defect exposed by the real E2E flow.

## Milestones

- [x] M1: Create this backend task directory and record the live RED blocker before code changes.
- [x] M2: Add or update BDD scenarios plus failing backend verification for stage alignment.
- [x] M3: Fix the fixed-four-stage task/stage alignment in backend workflow handling.
- [x] M4: Run GREEN backend verification and restart the local backend.
- [x] M5: Re-run real Playwright E2E to confirm the first approval can complete and continue through the chain.

## Expected Verification

- `POST /admin-api/dcc/controlled-files/{id}/approve-task` succeeds for the first live pending task of a fixed-four-stage category-derived workflow.
- The controlled file status advances away from `PENDING_DOC_CONTROL_REVIEW` after the first approval.
- Real frontend approval signature dialog can close successfully for the first stage instead of hanging on `确认签名`.

## Current Status

Completed. The backend submit contract now splits first-stage and downstream assignee variables correctly, the runtime BPM model is redeployed as a fixed four-node definition, and the real Playwright chain advances through all four approval stages.

## Blocker And Impact

- Blocker: none remaining for this stage-alignment fix.
- Impact: the fixed-four-stage matrix workflow now supports the full live approval chain instead of stopping on the first approval step.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccApprovalRouteAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- Runtime BPM model `44a108c8-4eb4-11f1-950d-00155db32d8f` updated and redeployed to process definition version `3`.
- Local backend restarted successfully on `48081` using explicit datasource overrides to `127.0.0.1:23306`.
- Real Playwright E2E then passed end to end for controlled file `2054545668044042254`.
