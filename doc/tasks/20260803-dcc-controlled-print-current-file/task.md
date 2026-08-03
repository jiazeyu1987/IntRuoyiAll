# DCC Controlled Print Current File

## Task Goal

Fix the DCC controlled print failure where the current controlled file shows `Current controlled file cannot be printed as a controlled copy` under the direct controlled print strategy.

## Milestones

- [x] Reproduce the current-version controlled print rejection with a focused regression test.
- [x] Identify the backend/frontend boundary that incorrectly rejects the current effective file.
- [x] Implement the minimal root-cause fix without fallback, downgrade, or swallowed errors.
- [x] Run targeted RED/GREEN verification and adjacent controlled print regression checks.
- [x] Record closeout evidence and final status.

## Expected Verification

- Targeted controlled print regression test fails before the fix and passes after the fix.
- Existing DCC controlled print static contract remains passing.
- A focused backend or frontend check proves current `ACTIVE` controlled files can generate controlled print records while non-current or invalid versions still fail fast.

## Current Status

ready_for_closeout

Implementation and targeted verification are complete. Final task completion is blocked from `completed` by repository closeout requirements: the shared `int_main` workspace is behind `origin/int_main` and contains many unrelated dirty tracked/untracked changes, so this task cannot safely create the required isolated implementation/closeout commits without a separate baseline strategy.

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`: 否。
- `是否从根因和长期维护角度解决`: 是，目标是修正当前有效版本打印判定或调用边界。
- `是否存在临时补丁或绕过`: 否。

## Workspace Safety

- Initial `git status --short --branch` showed `int_main...origin/int_main [behind 2]` and many unrelated dirty tracked/untracked files.
- Current task changes must stay scoped to DCC controlled print code, focused tests, and this task directory.
- Current task-owned implementation files are `DccControlledFilePrintServiceImpl.java`, the controlled-print hunk in `DccControlledFileQueryServiceImpl.java`, `DccControlledFilePrintServiceImplTest.java`, `DccControlledPrintContractTest.java`, the controlled-print hunk in `detail/index.vue`, and the controlled-print static contract hunk.

## Cleanup Keep

- doc/tasks/20260803-dcc-controlled-print-current-file/bug-regression-evidence.md

## Experience Gates

- DCC 受控打印门禁: current controlled print eligibility must be based on the master current `ACTIVE` file plus menu/category `PRINT` permission; viewer/hidden sections must not request non-rendered print records.
- Windows Maven 增量输出删除卡住门禁: Maven verification used a task-scoped module command with `-Dmaven.compiler.useIncrementalCompilation=false` after prior Windows incremental deletion stalls were observed.
- Maven 目标目录文件系统异常门禁: no unrelated target directories were deleted or repaired; verification used the DCC module-local command that reached Surefire and returned `BUILD SUCCESS`.
- Experience consolidation updated `docs/e2e-rules.md#dcc-受控打印门禁` and `docs/experience-index.md` so future controlled-print bugs do not reintroduce `publishedFileId` / `stampedFileId` as an eligibility gate.
