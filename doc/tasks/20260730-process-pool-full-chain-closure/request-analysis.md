# Request Analysis

## User Goal
Use the current production report-work entry as the frontline recordbook entry. A single operator submit should produce report-work data, original recordbook data, and process-pool data. The remaining chain to close is binding data source, real frontline submit, PQC pool entry, FIFO orchestration by production work order planned start time, automatic review-copy limit rules, team-leader workbench, and full E2E.

## Current System
- Backend already contains process-pool tables, event submission, PQC/FIFO/review-copy/revision services, and a one-shot `/mes/pro/feedback/frontline/submit` API.
- Frontend already contains fixed production/PQC template panels and API wrappers, but production submit still validates a template payload and shows success instead of calling the one-shot submit API.
- PQC UI is present, but submission remains blocked.
- Binding source interfaces exist, while production Spring beans still need to be verified or implemented.
- Timeline, review copy, event revision pages exist; review copy is still manually driven by JSON field mappings instead of automatic device/template limits.

## Constraints
- No scheduling system dependency; FIFO targets production work orders only.
- Actual processes are rolling and parallel; route setup can exist but must not gate frontline entry by route sequence.
- Original records remain editable only with revision log/signature and without mutating allocated/locked quantities.
- Missing prerequisites must fail fast; no mock success, fallback, silent downgrade, or API-only E2E substitution.
- Work must stay in `D:\IntRuoyiWorktree\process-pool-full-chain-closure`.

## Unknowns
- Exact formal tables for device-account route binding and employee-template binding need repository/schema confirmation.
- Electronic signature acquisition path for frontline fixed submit needs existing API/component confirmation.
- Full E2E test data availability for production work order, bound account, employee, and signature must be confirmed before running browser tests.

## Risks
- The request spans backend, frontend, schema, permissions, and runtime data; full closeout may require several commits and real local runtime setup.
- Starting from local HEAD includes local ahead baseline commits; final integration must consider `origin/int_main` divergence.
- If test data or login/signature prerequisites are missing, full E2E must be recorded as blocked rather than passed.

## Validation Surface
- Backend JUnit for services/controllers/mappers.
- Frontend static contracts for fixed-template submit, PQC submit, workbench route/API.
- Playwright real E2E on worktree ports `8082/48082`.
- SQL/schema policy gates for migrations or menu changes.

## Blocking Prerequisites
- None for static/code implementation in the new worktree.
- Full E2E remains contingent on local runtime, login, signature, and task-owned data.
