# Branch Runtime Port Contract

PORT_CONTRACT_VERSION: 2026-08-24-branch-runtime-v7

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
- Additional worktrees for a profile must use a stable slot in the closed range `1..100`.
- Slots `1..19` keep the existing mapping: worktree port = profile base port + slot.
- Slots `20..30` use the first dedicated extension ranges in the table below.
- Slots `31..40` use the second dedicated extension ranges in the table below.
- Slots `41..50` use the third dedicated extension ranges in the table below.
- Slots `51..60`, `61..70`, `71..80`, `81..90`, and `91..100` use the fourth through eighth dedicated extension ranges in the table below.
- `slot >= 101` is invalid.
- Active registry entries must be globally unique by `profile/slot`, frontend port, and backend port.
- Reserve a slot with `scripts\runtime\reserve-worktree-slot.ps1`; the script uses a cross-process mutex and selects the lowest available slot for the requested profile.

| Runtime profile | Slots `1..19` | Slots `20..30` | Slots `31..40` | Slots `41..50` | Slots `51..60` | Slots `61..70` | Slots `71..80` | Slots `81..90` | Slots `91..100` |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `int_shedule` | `8022-8040` / `48022-48040` | `8121-8131` / `48121-48131` | `8176-8185` / `48176-48185` | `8226-8235` / `48226-48235` | `8276-8285` / `48276-48285` | `8326-8335` / `48326-48335` | `8376-8385` / `48376-48385` | `8426-8435` / `48426-48435` | `8476-8485` / `48476-48485` |
| `int_batch` | `8042-8060` / `48042-48060` | `8132-8142` / `48132-48142` | `8186-8195` / `48186-48195` | `8236-8245` / `48236-48245` | `8286-8295` / `48286-48295` | `8336-8345` / `48336-48345` | `8386-8395` / `48386-48395` | `8436-8445` / `48436-48445` | `8486-8495` / `48486-48495` |
| `int_qms` | `8062-8080` / `48062-48080` | `8143-8153` / `48143-48153` | `8196-8205` / `48196-48205` | `8246-8255` / `48246-48255` | `8296-8305` / `48296-48305` | `8346-8355` / `48346-48355` | `8396-8405` / `48396-48405` | `8446-8455` / `48446-48455` | `8496-8505` / `48496-48505` |
| `int_main` | `8082-8100` / `48082-48100` | `8154-8164` / `48154-48164` | `8206-8215` / `48206-48215` | `8256-8265` / `48256-48265` | `8306-8315` / `48306-48315` | `8356-8365` / `48356-48365` | `8406-8415` / `48406-48415` | `8456-8465` / `48456-48465` | `8506-8515` / `48506-48515` |
| `int_main_d` | `8102-8120` / `48102-48120` | `8165-8175` / `48165-48175` | `8216-8225` / `48216-48225` | `8266-8275` / `48266-48275` | `8316-8325` / `48316-48325` | `8366-8375` / `48366-48375` | `8416-8425` / `48416-48425` | `8466-8475` / `48466-48475` | `8516-8525` / `48516-48525` |

## Protected Rules

- Do not change E:\IntRuoyi int_main defaults away from 8081/48081.
- D:\ProjectPackage\IntRuoyi\IntRuoyiAll is int_main_d and must use 8101/48101.
- The five base directories must remain unique: D-Main 8101/48101, E-Main 8081/48081, Batch 8041/48041, Shedule 8021/48021, QMS 8061/48061.
- Base workspaces must use `slot = 0`; they may not request additional worktree slots.
- Additional worktrees may not use `slot >= 101`, any base port, or any active registry slot/port owned by another worktree.
- Do not make branch-specific runtime ports by editing shared `IntRuoyiFronted\.env` or backend `application-local.yaml`.
- Use `scripts\runtime\start-branch-frontend.ps1` and `scripts\runtime\start-branch-backend.ps1` for branch-specific local debugging.
- Run `scripts\preflight\branch-runtime-port-guard.ps1` after merges and before commits or pushes.
- Install hooks once per workspace with `scripts\git\install-branch-runtime-hooks.ps1`; hooks run the guard for commit, merge-commit, post-merge, and push operations. Fast-forward merges are checked by post-merge and blocked from leaving the machine by pre-push if drift appears.
