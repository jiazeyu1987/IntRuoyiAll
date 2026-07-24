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
- No commits or pushes were performed because unrelated tracked deletions already exist in the workspaces and must not be mixed into this task without explicit commit direction.
