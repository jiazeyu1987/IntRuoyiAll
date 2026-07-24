# Codex Branch Runtime Handoff

PORT_CONTRACT_VERSION: 2026-07-24-branch-runtime-v1

## What This Protects

This repository participates in the IntRuoyi local multi-branch runtime contract. Each branch profile has its own local frontend/backend ports, and each profile may create additional worktrees by adding a stable slot number to its base ports.

All future Codex tasks must preserve these files and rules during merge, commit, and push:

- `docs/branch-runtime-ports.md`
- `docs/local-runtime.md`
- `docs/worktree-restrictions.md`
- `scripts/preflight/branch-runtime-port-guard.ps1`
- `scripts/runtime/branch-runtime-profile.ps1`
- `.githooks/pre-commit`
- `.githooks/pre-merge-commit`
- `.githooks/post-merge`
- `.githooks/pre-push`

## Port Matrix

| Profile | Workspace | Frontend | Backend |
| --- | --- | ---: | ---: |
| `int_main` | `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` | `8081` | `48081` |
| `int_batch` | `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll` | `8041` | `48041` |
| `int_shedule` | `E:\IntRuoyiBranch\Shedule\IntRuoyiAll` | `8021` | `48021` |
| `int_qms` | `E:\IntRuoyiBranch\QMS\IntRuoyiAll` | `8061` | `48061` |

## Worktree Rule

- Base workspace uses `slot = 0`.
- Additional worktree uses a stable positive slot.
- Frontend port = profile frontend base port + slot.
- Backend port = profile backend base port + slot.
- Examples: `int_main slot=1 -> 8082/48082`, `int_batch slot=1 -> 8042/48042`, `int_shedule slot=1 -> 8022/48022`, `int_qms slot=1 -> 8062/48062`.

## Required First Commands For Future Codex Tasks

Run these from the repository root before starting branch runtime work:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\git\install-branch-runtime-hooks.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\show-branch-runtime.ps1
```

## Runtime Commands

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\start-branch-frontend.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\start-branch-backend.ps1 -Build
```

For additional worktrees, pass the assigned slot:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\start-branch-frontend.ps1 -Slot 1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\start-branch-backend.ps1 -Slot 1 -Build
```

## Merge / Commit / Push Protection

- `pre-commit` blocks commits if the contract is missing or drifted.
- `pre-merge-commit` checks non-fast-forward merge commits.
- `post-merge` checks after fast-forward merges because those cannot be blocked before completion.
- `pre-push` blocks bad contract state from leaving the machine.

If any guard fails, stop and fix the contract instead of changing ports, deleting hooks, or bypassing the guard.