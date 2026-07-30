# Verification Report

## Result

completed

## Removed

- `D:\IntRuoyiWorktree\20260727-batch-record-form-terminology`
- `D:\IntRuoyiWorktree\20260727-route-history-cancelled-version-view`
- `D:\IntRuoyiWorktree\20260728-edhr-dynamic-form-cell-link-runtime`
- `D:\IntRuoyiWorktree\20260728-edhr-scrap-assist-switch`
- `D:\IntRuoyiWorktree\20260728-node-chain-route-filter`
- `D:\IntRuoyiWorktree\20260729-codex-monitor-manual-refresh`
- `D:\IntRuoyiWorktree\20260729-edhr-parallel-highlight-e2e`
- `D:\IntRuoyiWorktree\edhr-special-node-filler-e2e-20260727`
- `D:\IntRuoyiWorktree\onlyoffice-test-release-20260727`

## Preserved

- `E:\IntRuoyi` / branch `int_main`
- `D:\IntRuoyiWorktree\20260730-banzuzhang` / branch `codex/20260730-banzuzhang`

## Blocked Remaining Worktrees

- None after explicit user authorization to discard dirty and unmerged contents.

## Additional Removed After Authorization

- `D:\IntRuoyiWorktree\20260727-route-flow-batch-record-form-source-e2e`
- `D:\IntRuoyiWorktree\20260727_int_main_latest_backend_runtime`
- `D:\IntRuoyiWorktree\20260728-codex-node-chain-first-node-contract`
- `D:\IntRuoyiWorktree\r260729-sql-collation`
- `D:\IntRuoyiWorktree\route-import-graph-fix`

## Evidence

- `git -C E:\IntRuoyi worktree list --porcelain` shows only `int_main` and `20260730-banzuzhang`.
- Deleted target directory checks all returned `Exists = False`.
- `D:\IntRuoyiWorktree\.ports\worktree-ports.json` active entries now contain only `20260730-banzuzhang`.
- Temporary cleanup directory `D:\IntRuoyiWorktree\.runtime\empty-delete-source-20260730-worktree-prune` returned `Exists = False`.
