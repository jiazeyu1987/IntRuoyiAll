# Development Plan

## Scope

- `MesStage2_5BackfillBatchExecutionSimulationServiceImpl`
- Stage2.5 task-owned command/result/service and contract validator fixtures
- Stage2.5 regression test

## Formal Boundary

Stage1 completion owns Tx-A and `BACKFILL_SUCCEEDED` receipt. Stage2.5 consumes that receipt and passes the normalized request to Flow6. No release dossier writer is allowed in Stage2.5.

## Out Of Scope

- Shared `E:\IntRuoyi` worktree changes.
- Stage4/5/6 simulation implementation beyond the snapshot contract needed by Stage2.5.
- Real tenant write E2E until credentials and cleanup scope are supplied.

