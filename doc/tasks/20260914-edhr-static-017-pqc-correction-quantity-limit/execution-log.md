# Execution Log

## BDD / TDD Evidence

- BDD: PQC 更正拒绝损耗超过实际检验数量 -> Given 已存在 PQC 检验记录且更正请求将实际检验数量设为 5, When PQC 组长把损耗/报废数量更正为 10 并提交有效更正原因和签名信息, Then 服务端在签名和正式记录写入前拒绝，原事件、PQC 记录、逐件明细和更正日志保持不变。
- BDD: PQC 更正不得低于逐件失败明细要求 -> Given 更正请求包含 5 件逐件结果且至少 2 件为不合格或报废, When 损耗/报废数量更正为 1, Then 服务端在签名和正式记录写入前拒绝，避免总损耗数量小于逐件明细要求。
- BDD: PQC 更正允许合法数量关系 -> Given 更正请求实际检验数量为 5 且逐件失败/报废明细为 2 件, When 损耗/报废数量更正为 2, Then 请求通过数量关系校验并继续执行既有更正权限、状态、签名和写入流程。

## Rule Reading

- PASS: `AGENTS.md` read before edits.
- PASS: `docs/task-closeout-rules.md` read before edits.
- PASS: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\SKILL.md` and `references\bug-contract.md` read before edits.
- PASS: Relevant docs read: `docs/backend-development.md` PQC/eDHR quantity and correction parity sections; `docs/e2e-rules.md` frontline PQC gate section; `docs/bugs/20260913-edhr-additional-logic-audit.md` EDHR-STATIC-017 defect statement; `docs/bugs/20260912-edhr-90-step-static-audit.md` adjacent PQC quantity/verdict boundaries.

## Current Evidence

- Scope confirmed: only EDHR-STATIC-017 will be modified.
- Existing dirty worktree detected before this task; unrelated DCC/runtime/docs changes will be left untouched.
- RED: `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> FAIL, expected reason: PQC correction scrap input/request builder do not cap scrap quantity at actual inspection quantity.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` -> FAIL, command-level blocker before target test because upstream reactor modules had no matching specified test; reran MES module directly.
- RED: `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` from `IntRuoyiBackend\yudao-module-mes` -> FAIL as expected, `rejectsScrapQuantityAboveActualInspectionQuantityBeforeLoadingEvent` and `rejectsScrapQuantityBelowFailedPieceDetailFloorBeforeWrite` expected `ServiceException` but nothing was thrown.
- GREEN: `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> PASS.
- GREEN: `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest" test` from `IntRuoyiBackend\yudao-module-mes` -> PASS, 7 tests.
- GREEN: `node --check IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` -> PASS.
- GREEN: `git diff --check -- <task-owned paths>` -> PASS, only line-ending normalization warnings from `git diff` appeared in separate diff/stat commands.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260914-edhr-static-017-pqc-correction-quantity-limit\bug-regression-evidence.md` -> PASS.
- BLOCKED supplemental check: `node tests\e2e\team-leader-workbench-sfc-style-compile-static.spec.cjs` from `IntRuoyiFronted` -> BLOCKED, `Cannot find module 'postcss'`; `node_modules`, `node_modules\vue-tsc`, and `node_modules\postcss` are absent, and this task does not install dependencies.
- BLOCKED cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-017-pqc-correction-quantity-limit --mode preview` -> BLOCKED, `Current worktree branch could not be resolved`; `git status --short --branch` reports `## HEAD (no branch)`.
- SUPERSEDED closeout completion: earlier no-commit/no-push blocker was superseded by the later `提交并融合进int_main` authorization; git push remains unauthorized.
- Project experience consolidation: searched `docs/*memory*.md` and relevant long-term docs. Existing `docs/backend-development.md` already contains `Quantity and verdict reconciliation extension` and `Correction-path parity extension`; it was also pre-existing dirty before this task, so no long-term project doc was edited to avoid mixing unrelated changes or duplicating a task-specific business status.
- Git ignore note: task docs under `doc/tasks/20260914-edhr-static-017-pqc-correction-quantity-limit/` are ignored by `E:/IntRuoyi/.git/info/exclude:17:/doc/tasks/*/`; after follow-up authorization they were force-added only for this task record commit.
- SUPERSEDED continue follow-up: the initial `继续` remained read-only; the later `提交并融合进int_main` authorized local git commit/fusion but still did not authorize push, E2E, dependency install, service operations, database writes, or remote operation.
- READ-ONLY: `git branch --contains HEAD --all` -> current checkout remains `* (no branch)`; HEAD is also contained by several existing local branches, including `codex/20260914-dcc-static-017-access-subject-validation-fusion` and `int_main`, but no checkout/branch mutation was performed.
- READ-ONLY: `git worktree list --porcelain` -> current task worktree `C:/Users/BJB110/.codex/worktrees/cdb9/IntRuoyi` is detached at `b4303b4ed0c7f5344057694268b96fecdc5c2626`; main worktree `E:/IntRuoyi` is on `refs/heads/int_main`.
- BLOCKED cleanup preview rerun after `blocked` status: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-017-pqc-correction-quantity-limit --mode preview` -> still BLOCKED, `Current worktree branch could not be resolved`; keep list contains the four task evidence files and delete list is `<none>`.
- READ-ONLY scope check: `git diff --name-only -- <017-owned code paths> docs\bugs\20260913-edhr-additional-logic-audit.md` -> only the three task-owned code files are tracked modifications; the shared defect doc was not edited by this task.

## Implementation Notes

- Backend now rejects `scrapQuantity > actualInspectionQuantity` in command validation before loading the event or recording a signature.
- Backend now validates rebuilt piece details before revision/signature/formal table writes; distinct failed `sampleNo` count is the minimum allowed `scrapQuantity`.
- Frontend now caps the correction scrap input at the selected actual inspection quantity and repeats both quantity checks while building the correction request payload.
- Frontend carries item result type and standard bounds from the stored PQC snapshot into the correction form so boolean/numeric failed samples can be counted before submit.

## Suggested Shared Defect Table Update

- `EDHR-STATIC-017 | P2 | PQC更正允许损耗数量大于实际检验数量 | FIXED_STATIC_VERIFIED_SOURCE_ONLY`
- Note: verified by targeted backend unit and frontend static contract on 2026-09-14; later locally fused into `int_main`; no E2E, database write, service restart, remote operation, or git push was performed.

## Int Main Fusion Follow-up

- AUTHORIZATION: 2026-09-14 user requested `提交并融合进int_main`; interpreted as git commit/local `int_main` fusion authorization, without git push authorization.
- MAINLINE SCOPE: Source worktree `C:\Users\BJB110\.codex\worktrees\cdb9\IntRuoyi` is detached and contains mixed DCC/runtime dirty paths, so the whole worktree was not merged. Only EDHR-STATIC-017 allow-list code/docs were synchronized to `E:\IntRuoyi`.
- FUSION: Local `int_main` contains the EDHR-STATIC-017 frontend/backend correction guard from commit `2722c7ac1` and the combined PQC correction backend test/service state preserved through commit `3d774fe75` while resolving the EDHR-STATIC-015/016 cherry-pick conflict.
- INTEGRATION UNBLOCKER: `MesTeamLeaderOrderProcessCompletionServiceTest` was aligned with the already-committed constructor signature so the MES test source can compile on `int_main`.
- GREEN: `node IntRuoyiFronted\scripts\pqc-correction-quantity-limit-static.spec.cjs` from `E:\IntRuoyi` -> PASS.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-015-production-correction-sync-static.spec.cjs` from `E:\IntRuoyi` -> PASS, adjacent cherry-pick contract.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-016-pqc-correction-freeze-static.spec.cjs` from `E:\IntRuoyi` -> PASS, adjacent cherry-pick contract.
- GREEN: `mvn "-Dtest=MesProcessPoolPqcInspectionCorrectionServiceTest,MesProcessPoolProductionReportCorrectionServiceTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest" test` from `E:\IntRuoyi\IntRuoyiBackend\yudao-module-mes` -> PASS, 22 tests, including 8 PQC correction tests.
- GREEN: `git diff --cached --check` during cherry-pick resolution -> PASS.
- GREEN: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` from `E:\IntRuoyi` -> PASS for `int_main/int_main`, frontend 8081, backend 48081.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-017-pqc-correction-quantity-limit --mode preview` from `E:\IntRuoyi` -> PASS, keep four task files, delete/blocker/warnings none.
- CLEANUP: same command with `--mode apply` -> PASS, deleted paths none.
- CLEANUP: Temporary verification worktree `D:\IntRuoyiWorktree\20260914-edhr-static-017-main-verify` was removed with `git worktree remove --force`; follow-up scan found no registration for that path.
- PROJECT EXPERIENCE: `project-experience-consolidation` applied by checking existing long-term memory. `docs\worktree-memory.md` already contains the detached C盘 worktree and selective `int_main` fusion rule, so no new long-term experience doc was created or edited.
- BLOCKED COMPLETION: project closeout requires pushing `int_main` before marking `completed`, but this turn did not explicitly authorize `git push`; task remains `blocked` after local fusion.
