# Verification Report

## Scope

- Target registry: `D:\IntRuoyiWorktree\.ports\worktree-ports.json`
- Target profile: `int_main`
- Target slots: `1..19`
- Cleanup criterion: `createdAt < 2026-08-05` and neither frontend nor backend port is listening.

## Results

PASS.

## Cleared Slots

| Slot | Ports | Worktree |
| ---: | --- | --- |
| 1 | 8082 / 48082 | `process-pool-full-chain-closure` |
| 2 | 8083 / 48083 | `r260731c-smart-seed-fix` |
| 3 | 8084 / 48084 | `20260731_shengchanbanzuzhang` |
| 5 | 8086 / 48086 | `pml-test-r260731` |
| 6 | 8087 / 48087 | `r260801-smartseed-collation-fix` |
| 8 | 8089 / 48089 | `release-third-party-feedback-20260801` |
| 9 | 8090 / 48090 | `third-party-feedback-import-20260802` |
| 10 | 8091 / 48091 | `20260802-test-dcc-download-permission` |
| 11 | 8092 / 48092 | `worktree_20260803_p0` |
| 13 | 8094 / 48094 | `form-center-route-missing-20260803` |
| 14 | 8095 / 48095 | `dcc-upload-size-policy-fix` |
| 15 | 8096 / 48096 | `20260803_pqcc` |
| 16 | 8097 / 48097 | `dcc-approval-role-display` |
| 18 | 8099 / 48099 | `controlled-file-category-e2e-20260803` |
| 19 | 8100 / 48100 | `20260803_pqf` |

## Kept Slots

| Slot | Ports | Worktree | Reason |
| ---: | --- | --- | --- |
| 4 | 8085 / 48085 | `production-leader-tab-20260804` | Created on 2026-08-05 |
| 7 | 8088 / 48088 | `profile-nas-table-auto-sync` | Created on 2026-08-05 |
| 12 | 8093 / 48093 | `20260805-process-loss-reasons` | Created on 2026-08-05 and listening |
| 17 | 8098 / 48098 | `rrm-m0-m6-verification-20260803` | Listening |

## Verification Evidence

- Registry active slot readback: active `int_main` slots are 4, 7, 12, 17.
- Port listener scan: only 8093, 8098, 48093, 48098 are listening in the worktree range.
- Guard: `pwsh -NoProfile -File scripts/preflight/branch-runtime-port-guard.ps1` passed.
- Experience gate: `docs/worktree-memory.md#Worktree 旧无监听槽位释放门禁` added and indexed.
- Diff check: task docs and experience docs passed `git diff --check`; PowerShell reported only existing CRLF normalization warnings.
