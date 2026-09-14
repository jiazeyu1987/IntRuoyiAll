# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: eDHR historical batch reads can surface `eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录` when the frozen route task snapshot lacks complete `flowGraph.nodes` or `batchUseConfigs`, even though formal current BATCH per-process batch record bindings still exist.
- Expected: detail and review timeline reads must recover route form tasks only from formal batch record sources: current BATCH process config when it fully covers the route processes, or the frozen formal route snapshot when current config is absent. Missing formal sources must fail fast.

## Reproduction

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getDetail_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected recovered `taskTotal=6` but actual was `4`, proving route batch record tasks were not recovered from formal current BATCH config when the frozen task config was incomplete.

## Root Cause

- `getReviewTimeline` read the existing batch task list directly and built task gates before the active-batch route task recovery path ran.
- The affected path could therefore operate on special-only historical tasks and re-enter the missing formal batch record configuration error instead of first using the existing formal recovery path.

## Regression Test

- Added `MesProEdhrBatchExecutionServiceTest#getReviewTimeline_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete`.
- Existing detail recovery coverage remains `MesProEdhrBatchExecutionServiceTest#getDetail_recoversMissingRouteTasksFromCurrentBatchConfigWhenFrozenSnapshotIncomplete`.

## Fix

- `MesProEdhrBatchExecutionServiceImpl#getReviewTimeline` now runs `syncIfActive(batch)` in a transaction before reading task events, then reloads the latest batch and task rows.
- This reuses the established recovery path and does not introduce `formBindings`, default `MAIN`, 工序开始配置, mock success, swallowed exceptions, or silent fallback.

## GREEN / Verification Status

- GREEN: not obtained -> BLOCKED by local Maven/JDK build-output failure before Surefire execution; no passing GREEN is claimed.
- GREEN is blocked in this workspace. Multiple Maven attempts failed before Surefire could execute the targeted tests:
- `-am` targeted Maven stalled in javac / upstream reactor filesystem paths with no fresh Surefire report.
- Module-only full compile exposed or triggered stale/corrupt MES build output around `MesProScheduleCalendarDayDetailRespVO.LineDetailItem.LineDetailItemBuilder`.
- Module `clean test` hit the documented Windows `WinNTFileSystem.delete0` target deletion stall.
- Final targeted MES test JVM exited with code `-1` after javac started and produced no Surefire report.
- Static check passed: `git diff --check -- <touched service/test files>`.

## Risk And Regression Scope

- Scope: eDHR batch execution review timeline read path for active historical batches with missing route form tasks.
- Risk: `getReviewTimeline` now performs the same active-batch task recovery side effect as detail reads. This is intentional for consistency but still requires JUnit GREEN before commit.

## Blockers And Follow-Up

- Restore a healthy `yudao-module-mes` Maven build output state, then rerun the targeted eDHR tests with the standard project command.
- Do not commit or mark completed until the targeted JUnit produces a fresh Surefire PASS.
