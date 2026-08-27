# Codex Branch Runtime Handoff

PORT_CONTRACT_VERSION: 2026-08-24-branch-runtime-v7

## What This Protects

This repository participates in the IntRuoyi local multi-branch runtime contract. Each branch profile has its own local frontend/backend ports, and each profile may create additional worktrees by adding a stable slot number to its base ports.

All future Codex tasks must preserve these files and rules during merge, commit, and push:

- `docs/branch-runtime-ports.md`
- `docs/local-runtime.md`
- `docs/worktree-restrictions.md`
- `scripts/preflight/branch-runtime-port-guard.ps1`
- `scripts/runtime/branch-runtime-profile.ps1`
- `scripts/runtime/reserve-worktree-slot.ps1`
- `.githooks/pre-commit`
- `.githooks/pre-merge-commit`
- `.githooks/post-merge`
- `.githooks/pre-push`

## Port Matrix

| Profile | Workspace | Frontend | Backend |
| --- | --- | ---: | ---: |
| `int_main_d` | `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` | `8101` | `48101` |
| `int_main` | `E:\IntRuoyi` | `8081` | `48081` |
| `int_batch` | `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll` | `8041` | `48041` |
| `int_shedule` | `E:\IntRuoyiBranch\Shedule\IntRuoyiAll` | `8021` | `48021` |
| `int_qms` | `E:\IntRuoyiBranch\QMS\IntRuoyiAll` | `8061` | `48061` |

## Worktree Rule

- Base workspace uses `slot = 0`.
- Additional worktree uses a stable slot in `1..100`.
- Slots `1..19` keep the existing `profile base port + slot` mapping.
- Slots `20..30` use the dedicated extension ranges defined in `docs\branch-runtime-ports.md`.
- Examples: `int_main slot=1 -> 8082/48082`, `int_batch slot=1 -> 8042/48042`, `int_shedule slot=1 -> 8022/48022`, `int_qms slot=1 -> 8062/48062`.
- Slots `31..40` use the second dedicated extension ranges defined in `docs\branch-runtime-ports.md`.
- Slots `41..50` use the third dedicated extension ranges defined in `docs\branch-runtime-ports.md`.
- Slots `51..60`, `61..70`, `71..80`, `81..90`, and `91..100` use the fourth through eighth dedicated extension ranges defined in `docs\branch-runtime-ports.md`.
- `slot >= 101`, base-port collisions, duplicate active profile slots, and duplicate active ports must fail fast.
- After creating the worktree directory and before starting either service, reserve the slot with the registry script:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\reserve-worktree-slot.ps1 `
  -Name <worktree-directory-name> `
  -Path D:\IntRuoyiWorktree\<worktree-directory-name> `
  -Branch <branch-name> `
  -Profile <runtime-profile>
```

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

For registered additional worktrees, the runtime scripts read the assigned slot from the registry. An explicit `-Slot` is optional and must match the registered slot:

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
