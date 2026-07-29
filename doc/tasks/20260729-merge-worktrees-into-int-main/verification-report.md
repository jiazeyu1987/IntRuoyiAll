# Verification Report

## Result

Partial integration completed. Local `int_main` contains all currently eligible worktree branches, but the overall user request is blocked from full completion because two attached worktrees have unresolved task blockers and were not merged.

## Worktree Inventory

- Total registered worktrees: 12.
- Main workspace: `E:\IntRuoyi`, branch `int_main`.
- Attached worktrees: 11 under `D:\IntRuoyiWorktree\`.
- Attached branches that are ancestors of local `int_main`: 9.
- Attached branches not merged: 2.

## Merged Or Already Integrated

- `codex/20260727-batch-record-form-terminology`
- `codex/20260727-route-history-cancelled-version-view`
- `codex/20260728-codex-node-chain-first-node-contract`
- `codex/20260728-edhr-dynamic-form-cell-link-runtime`
- `codex/20260728-edhr-scrap-assist-switch-clean`
- `codex/20260728-node-chain-route-filter`
- `codex/20260729-edhr-parallel-highlight-e2e`
- `codex/edhr-special-node-filler-e2e-20260727`
- `codex/20260727-onlyoffice-test-release`

## Blocked

- `codex/20260727-route-flow-batch-record-form-source-e2e`: task status `blocked`; exact `球囊扩张导管` lacks formal per-process batch-record report source, and PTCA page verification lacks test-tenant login.
- `codex/restart-int-main-latest-backend-20260727`: task status `blocked_for_e2e_validation`; worktree is dirty, frontend still references `/batch-record-cell-link/prefill`, and real E2E lacks `LOCAL_DATABASE_FIXTURE`.

## Verification Evidence

- Branch runtime port guard: PASS after each merge and after final baseline commit.
- Node-chain backend test: PASS, `CodexTestExecutionServiceImplTest`, 8 tests.
- Dynamic form cell-link static contract: PASS.
- Special node guard checks: PASS, Node syntax check and 45 Maven tests.
- Route history checks: PASS, two frontend static contracts and 97 Maven tests.
- OnlyOffice release tooling: initial RED caught duplicate `Write-FrontendReleaseInfo`; after merge fix, `pytest` PASS with 129 tests.

## Residual Risk

- Local push still requires final history scan and network push verification.
- `D:\IntRuoyiWorktree\20260728-codex-node-chain-first-node-contract` has a runtime-only dirty PID file: `.runtime\codex-test-runner\codex-runner.pid`.
