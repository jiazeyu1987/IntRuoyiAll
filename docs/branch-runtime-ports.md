# Branch Runtime Port Contract

PORT_CONTRACT_VERSION: 2026-07-24-branch-runtime-v2

## Purpose

This contract keeps local branch runtimes independent while code can still merge between branches and eventually flow back to `int_main`.

## Port Matrix

| Runtime profile | Branch/workspace | Frontend base port | Backend base port | Frontend env mode |
| --- | --- | ---: | ---: | --- |
| `int_main_d` | `int_main`, `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` | `8101` | `48101` | `branch-main-d` |
| `int_main` | `int_main`, `E:\IntRuoyi` | `8081` | `48081` | `env.local` |
| `int_batch` | `int_batch`, `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll` | `8041` | `48041` | `branch-batch` |
| `int_shedule` | `int_shedule`, `E:\IntRuoyiBranch\Shedule\IntRuoyiAll` | `8021` | `48021` | `branch-shedule` |
| `int_qms` | `int_qms`, `E:\IntRuoyiBranch\QMS\IntRuoyiAll` | `8061` | `48061` | `branch-qms` |

## Worktree Ports

- The base workspace of each runtime profile uses `slot = 0`.
- Additional worktrees for a profile must use a stable positive slot.
- Frontend worktree port = profile frontend base port + slot.
- Backend worktree port = profile backend base port + slot.
- Example: `int_batch` slot `2` uses frontend `8043` and backend `48043`.

## Protected Rules

- Do not change E:\IntRuoyi int_main defaults away from 8081/48081.
- D:\ProjectPackage\IntRuoyi\IntRuoyiAll is int_main_d and must use 8101/48101.
- The five base directories must remain unique: D-Main 8101/48101, E-Main 8081/48081, Batch 8041/48041, Shedule 8021/48021, QMS 8061/48061.
- Do not make branch-specific runtime ports by editing shared `IntRuoyiFronted\.env` or backend `application-local.yaml`.
- Use `scripts\runtime\start-branch-frontend.ps1` and `scripts\runtime\start-branch-backend.ps1` for branch-specific local debugging.
- Run `scripts\preflight\branch-runtime-port-guard.ps1` after merges and before commits or pushes.
- Install hooks once per workspace with `scripts\git\install-branch-runtime-hooks.ps1`; hooks run the guard for commit, merge-commit, post-merge, and push operations. Fast-forward merges are checked by post-merge and blocked from leaving the machine by pre-push if drift appears.
