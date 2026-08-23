# Verification Report

## Scope

- BatchRecord workspace: `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll`, branch `int_batch`.
- Shedule workspace: `E:\IntRuoyiBranch\Shedule\IntRuoyiAll`, branch `int_shedule`.
- QMS workspace: E:\IntRuoyiBranch\QMS\IntRuoyiAll, branch int_qms.
- Primary int_main workspace: D:\ProjectPackage\IntRuoyi\IntRuoyiAll, branch int_main, assigned frontend 8081, backend 48081.

## Results

- scripts\preflight\branch-runtime-port-guard.ps1 passed in primary int_main: frontend 8081, backend 48081.

- `scripts\preflight\branch-runtime-port-guard.ps1` passed in BatchRecord: frontend `8041`, backend `48041`.
- `scripts\preflight\branch-runtime-port-guard.ps1` passed in Shedule: frontend `8021`, backend `48021`.
- `scripts\preflight\branch-runtime-port-guard.ps1` passed in QMS: frontend `8061`, backend `48061`.
- `scripts\runtime\show-branch-runtime.ps1 -Slot 1` passed in all three workspaces and confirmed slot-derived ports.
- `scripts\git\install-branch-runtime-hooks.ps1` set `core.hooksPath=.githooks` in all three workspaces.
- Shedule legacy `.env.shedule` and local port test were corrected from `8061/48061` to `8021/48021`.

## Remaining Closeout

- No services were started in this verification; this task verifies configuration and guard behavior only.
- Historical note: the original closeout did not commit because unrelated tracked deletions existed; the current v6 runtime implementation is already committed in main as `ea39dacc2`.

## 2026-08-23 v6 slot range alignment

- Contract: PASS. Main runtime files use `2026-08-22-branch-runtime-v6`, additional slots `1..50`, and reject `slot >=51`.
- Slot 31: PASS by profile calculation; it uses the second extension band and does not collide with base ports.
- Main guard: PASS, `int_main/int_main` -> frontend `8081`, backend `48081`.
- Maven: PASS, `MAVEN_HOME` resolves to Maven `3.9.16` with Java `21.0.10`.
- Python runtime contract tests: PASS, `22 passed in 10.66s` on current `int_main` with task-local temporary directories.
- Main containment: PASS, `ea39dacc2` contains the v6 runtime implementation; no duplicate merge or unrelated dirty-file staging was performed.
- Scope: no service, database, registry entry, other worktree, Flow4 business code, or unrelated dirty file was modified by this main-thread evidence update.
