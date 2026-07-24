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
