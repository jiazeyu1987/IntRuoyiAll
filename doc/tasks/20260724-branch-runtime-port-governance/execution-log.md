# Execution Log

## 2026-07-24

- USER INTENT: Implement durable branch-specific local runtime ports across BatchRecord, Shedule, and QMS workspaces while preserving `int_main` defaults and merge safety.
- BDD: branch runtime contract -> Given a workspace for `int_batch`, `int_shedule`, or `int_qms`, When the branch runtime guard runs, Then it must require the expected frontend/backend port pair and block drift back to `8081/48081`.
- BDD: merge protection -> Given code is merged from `int_main`, When committing, merging, or pushing branch runtime files, Then Git hooks must run the guard and fail fast if the port contract is missing or changed.
- PRECHECK: Read `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/powershell-memory.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, and `docs/frontend-development.md`.
- PRECHECK: BatchRecord branch is `int_batch`; Shedule branch is `int_shedule`; QMS was on `int_main` and was switched to new branch `int_qms` before branch-specific edits.
- PRECHECK: Existing unrelated tracked deletions remain untouched: showroom Win7 zip and one frontend task xlsx file.
- RED: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> FAIL, expected reason: guard script did not exist yet.
- IMPLEMENTED: Added `docs/branch-runtime-ports.md`, branch env files, runtime resolver/start scripts, Git hook scripts, and guard script.
- IMPLEMENTED: Updated `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `AGENTS.md`, and `IntRuoyiFronted/vite.config.ts` so branch scripts can inject worktree-slot ports without editing `int_main` defaults.
- GREEN: BatchRecord guard -> PASS, `int_batch` frontend `8041`, backend `48041`.
- GREEN: Shedule guard -> PASS, `int_shedule` frontend `8021`, backend `48021`.
- GREEN: QMS guard -> PASS, `int_qms` frontend `8061`, backend `48061`.
- GREEN: worktree slot verification -> PASS, slot `1` maps to BatchRecord `8042/48042`, Shedule `8022/48022`, QMS `8062/48062`.
- GREEN: hook install -> PASS, `git config core.hooksPath` is `.githooks` in all three workspaces.
- EXPERIENCE: Applied `project-experience-consolidation`; no existing `docs/worktree-memory.md` exists, so durable rules were merged into existing `docs/branch-runtime-ports.md`, `docs/local-runtime.md`, and `docs/worktree-restrictions.md`. No new long-term memory document was created.
- BLOCKER: commit/push closeout not performed; each workspace has pre-existing unrelated tracked deletions outside this task.

- UPDATE: User added D:\ProjectPackage\IntRuoyi\IntRuoyiAll; assigned it as the primary local int_main repository with frontend 8081 and backend 48081.
- GREEN: Primary int_main guard -> PASS, D:\ProjectPackage\IntRuoyi\IntRuoyiAll branch int_main frontend 8081, backend 48081.
- IMPLEMENTED: Added post-merge hook so fast-forward merges run the runtime guard after merge; pre-push remains the blocking gate before remote propagation.

## 2026-08-23 v6 slot range alignment

- BDD: slot 31 is valid -> Given the v6 registry contains an active worktree with slot `31`, When the runtime profile and guard execute, Then slot `31` resolves through the second extension range and is not rejected.
- BDD: guard protection remains -> Given valid slots `1..50`, When a duplicate slot/port, base-port collision, or slot `>=51` is registered, Then the guard fails fast.
- GREEN: main runtime guard -> PASS for `int_main/int_main`, frontend `8081`, backend `48081`.
- GREEN: Maven environment -> `MAVEN_HOME=C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16`; `mvn -version` -> Apache Maven `3.9.16`, Java `21.0.10`.
- GREEN: task-local runtime contract test -> `TEMP/TMP/TMPDIR=.runtime\pytest-tmp python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` -> `22 passed in 10.66s` on current `int_main` (the isolated branch v6 slice also passed its 14-test subset).
- COMMIT: main already contains the equivalent v6 runtime implementation in `ea39dacc2` (`任务: 统一运行时 V6 槽位合同`); no duplicate merge was attempted.
