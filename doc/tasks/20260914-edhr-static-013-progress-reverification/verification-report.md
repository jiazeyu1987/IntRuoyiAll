# Verification Report

## Scope

EDHR-STATIC-013 生产进度复核：验证部分分配、多输出物料和拆分/合并提交不会让活跃订单列表进度、完成门禁或工序完成记录虚增。

## Status

- Status: ready_for_closeout
- E2E / Playwright: not run by explicit user instruction.
- Database writes: not run by explicit user instruction.
- Service startup / restart / stop: not run by explicit user instruction.
- Remote server operations: not run by explicit user instruction.
- Git commit / push: not run by explicit user instruction.

## Requirement Matrix

| Requirement | Evidence | Result |
| --- | --- | --- |
| 报工 100 件仅正式分配 40 件时，订单进度最多按 40 件纳入 | `MesOutputMaterialProgressCalculatorTest.partialAllocationCapsWholeEventMaterialDetails` | PASS |
| 多输出物料按每个物料的正式分配约束累计并取保守完成口径 | `MesOutputMaterialProgressCalculatorTest.multiOutputSplitAllocationsUseEachMaterialAllocationBeforeMinimumProgress` | PASS |
| 拆分提交与合并提交结果一致 | `MesOutputMaterialProgressCalculatorTest.splitAndCombinedOutputSubmissionsReturnSameConservativeProgress` | PASS |
| 活跃订单完成门禁使用冻结输出物料和正式分配事件 | `MesTeamLeaderActiveOrderCompletionProgressPortImplTest` 6 tests PASS | PASS |
| 工序完成记录使用同一保守进度口径 | `MesTeamLeaderOrderProcessCompletionServiceTest` 13 tests PASS | PASS |
| 静态合同仍锁定快照、mapper、列表进度、完成门禁和完工记录 | `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` | PASS |

## Commands

- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` -> PASS.
- `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -Dtest=MesOutputMaterialProgressCalculatorTest,MesTeamLeaderActiveOrderCompletionProgressPortImplTest,MesTeamLeaderOrderProcessCompletionServiceTest test` -> PASS, 24 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check -- IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\processpool\team\MesOutputMaterialProgressCalculatorTest.java IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\processpool\team\MesTeamLeaderActiveOrderCompletionProgressPortImplTest.java IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\processpool\team\MesTeamLeaderOrderProcessCompletionServiceTest.java` -> PASS; CRLF normalization warnings only.
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-013-progress-reverification --mode preview` -> BLOCKED; keep includes the three task records, delete is none, blocked reason is `Current worktree branch could not be resolved.`
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` from `D:\IntRuoyiWorktree\20260914-edhr-static-013-progress-reverification` -> PASS; branch `codex/20260914-edhr-static-013-progress-reverification`, profile `int_main`, frontend `8090`, backend `48090`.
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` from the D worktree -> PASS.
- `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-mes "-Dtest=MesOutputMaterialProgressCalculatorTest,MesTeamLeaderActiveOrderCompletionProgressPortImplTest,MesTeamLeaderOrderProcessCompletionServiceTest" test` from the D worktree -> PASS, 24 tests, 0 failures, 0 errors, 0 skipped.

## Findings

- PASS: 当前生产代码已将 EDHR-STATIC-013 的进度消费面收敛到 `MesOutputMaterialProgressCalculator`，且该计算器使用正式分配数量限制生产事件物料数量。
- FIXED TEST GAP: 本轮发现实际消费方 JUnit 使用旧桩导致 RED，已补齐正式输出物料快照、allocation `eventId` 和生产事件 `materialDetails`，避免静态字符串合同假绿。
- PASS: 本轮未发现需要修改生产代码的新增缺口。

## Blockers / Risks

- Prior blocker resolved: 用户已在 2026-09-14 明确授权 Git commit / merge / push；不再受此前禁止提交推送限制。
- Prior cleanup blocker isolated: C 盘 Codex 临时 worktree 仍为 detached HEAD 且混有非本任务 dirty；本轮改用 D 盘具名任务 worktree 执行最小 EDHR-STATIC-013 diff 提交与融合。
- Task evidence visibility: `doc/tasks/*/` 被 `E:/IntRuoyi/.git/info/exclude` 忽略；提交时需要对本任务三份记录使用 `git add -f`。
