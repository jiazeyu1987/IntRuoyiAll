# DCC-STATIC-027 Execution Log

## Scope

- Bug: DCC-STATIC-027 修改正式文件基础信息未同步逻辑身份，旧编号可命中新编号文件。
- Source evidence: `E:\IntRuoyi\docs\bugs\20260912-dcc-90-step-static-audit.md` 中 DCC-STATIC-027 条目；当前干净 worktree 未继承 `E:\IntRuoyi` 未提交 diff。
- Verification boundary: 静态代码逻辑检查、定向 Java 单元测试或编译验证；不执行 E2E、不启动服务、不写数据库、不操作远程、不提交/推送 Git。

## BDD

BDD: metadata identity change syncs master key -> Given 一个已 ACTIVE 的正式 DCC 文件使用 OLD 逻辑身份生效, When 文控合法修改基础信息为 NEW 文件编号、项目或分类, Then 同一事务必须校验 NEW 的 `tenantId + dccProjectCodeId + fileTypeTaxonomyLeafId + normalizedFileNumber` 唯一性，并把 Master 权威身份同步为 NEW。

BDD: old identity read is rejected after rename -> Given Master 或 ACTIVE 文件身份不一致导致 OLD 逻辑键仍可命中 Master, When 当前版本查询按 OLD 发起, Then 服务必须拒绝该不一致身份，不能返回 fileNumber=NEW 且 matched=true 的结果。

## TDD Evidence

RED: `mvn -pl yudao-module-dcc '-Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest' test` -> FAIL, expected reason: 3 focused regression failures showed metadata update did not call `selectByNewLogicalIdentity`, did not reject conflicting new logical identity, and current-version lookup did not reject Master/ACTIVE file identity mismatch.

GREEN: `mvn -pl yudao-module-dcc '-Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest' test` -> PASS, 142 tests, 0 failures, 0 errors, 0 skipped.

## Implementation Notes

- `DccControlledFileMetadataUpdateServiceImpl` now normalizes updated file numbers to the canonical uppercase identity form, validates the new logical key when project, taxonomy leaf and file number are all present, and writes `dccProjectCodeId`, `fileTypeTaxonomyLeafId`, and `normalizedFileNumber` back to the Master update payload.
- `DccControlledFileWorkflowServiceImpl` now checks the ACTIVE file number/project/taxonomy against the requested identity and the Master identity before returning current-version details, so stale OLD identity cannot return a NEW active file.

## Verification

GREEN: `git diff --check` -> PASS; only Git line-ending normalization warnings were reported.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-027-metadata-identity-sync --mode preview --worktree-closeout off --json` -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`; delete/blocked/warnings empty.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-027-metadata-identity-sync\verification-report.md` -> PASS, bug regression evidence valid.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-027-metadata-identity-sync --mode apply --worktree-closeout off --json` -> PASS, no deleted paths; worktree closeout disabled to honor the user's no commit/merge/push scope.

## Final Status

completed: scoped fix and required verification are complete. No E2E, service runtime, database write, remote operation, or Git push was performed; the later local `int_main` integration commit is recorded below.

## Experience Consolidation

- Read `project-experience-consolidation` skill. No long-term experience document was updated because the project already has DCC composite-identity guidance in `docs/backend-development.md`, and the current user scope restricted edits to DCC-STATIC-027 task-owned code/tests/evidence.

## Int Main Integration

- User request: 融合进 `int_main`。
- Target workspace: `E:\IntRuoyi`, branch `int_main`.
- Applied the four DCC-STATIC-027 implementation/test changes from the isolated worktree onto `int_main`; unrelated staged, unstaged, and untracked changes already present in `E:\IntRuoyi` were not staged or committed by this task.
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileMetadataUpdateServiceTest,DccControlledFileWorkflowServiceImplTest" test` from `E:\IntRuoyi\IntRuoyiBackend` -> PASS, 153 tests, 0 failures, 0 errors, 0 skipped.
- GREEN: `git -C E:\IntRuoyi diff --check -- <DCC-STATIC-027 files>` -> PASS.
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` from `E:\IntRuoyi` -> PASS for `int_main/int_main` frontend 8081 backend 48081.
- COMMIT: `38ddcc335 fix: sync DCC metadata logical identity` on local `int_main`, containing only the four DCC-STATIC-027 Java/test files.
- Remote: no `git push` was executed in this step.