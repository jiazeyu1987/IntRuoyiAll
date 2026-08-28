# Backend API Evidence

## Scope

- Service scope: stage1 simulation fixture creation and active-order simulation completion facts used by stage2.5.
- Contract scope: stage2.5 must consume the stage1 active order, call the formal active-order completion node, and hand off the completion receipt to Flow6.

## API And Data Contract

- No public API shape was changed in this selective merge.
- Stage2.5 implementation was already content-aligned with the source worktree; this merge adds the missing stage1 formal product issue materialization needed by that contract.
- Stage1 cleanup remains scoped by simulation run and actor marker.

## Auth, Permissions, Validation, Error Behavior

- No permission rules were changed.
- Missing stage1 formal product issue data fails fast through explicit `STAGE1_FORMAL_PRODUCT_ISSUE_*` errors.
- No fallback, silent downgrade, mock success, or client-side source trust was introduced.

## Required Config, Services, Fixtures, Migrations

- Uses existing MES item, warehouse, material stock, batch, and product issue mappers already present in `int_main`.
- No new SQL migration is required by this selective merge.

## BDD

- BDD: Stage2.5 consumes Stage1 formal source -> Given stage1 creates a task-owned active order with formal material issue data and 100% production/PQC facts; When stage2.5 runs; Then it completes the active order through the formal completion node and passes the receipt to Flow6.
- BDD: Selective merge boundary -> Given the source worktree has unrelated stage4/5/6 and non-MES changes; When merging into `int_main`; Then only stage1/stage2.5 target files are changed.

## RED

- RED: Source worktree evidence -> FAIL, stage2.5 real completion could not proceed when stage1 created only simulated pick-list data but no formal product issue source.

## GREEN

- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-stage2-5-static.spec.cjs` -> PASS.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesStage2_5ReceiptHandoffContractTest,MesTeamLeaderActiveOrderSimulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 6, Failures: 0, Errors: 0, Skipped: 0.

## Verification

- `git diff --check -- <selected paths>` -> PASS.
- Selective boundary verified by changed path list: only stage1/stage2.5 backend/test files plus task documentation are part of this task.

## Observability

- No runtime logging changes were required.
- Simulation records continue to carry `simulationRunId`, `simulationStage`, and actor-scoped markers for cleanup/audit.

## Blockers

- None for this selective stage1/stage2.5 merge.
- Full real E2E for downstream stage4/5/6 remains outside this task scope.
