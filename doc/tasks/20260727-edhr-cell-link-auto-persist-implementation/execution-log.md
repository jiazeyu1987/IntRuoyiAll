# Execution Log

## User Intent

用户要求实现设计文档中的开发验证任务：将 eDHR 批记录单元格链接改为创建/打开执行记录时自动落库预填值，并完成开发验证。

## Baseline And Ownership

- Current branch: `int_main`.
- Existing design task: `doc/tasks/20260727-edhr-cell-link-auto-persist-design/`.
- Dirty baseline before implementation:
  - `291306c4 chore: preserve dirty baseline before cell link auto persist`
  - `e0e51633 chore: preserve controlled browse diagnosis baseline`
- This task owns only changes made after the baseline commits for the auto-persist implementation and evidence.

## BDD Scenarios

- `BDD: Production work order batch code auto-persists on execution create/open -> Given` an enabled cell-link rule maps `PRODUCTION_WORK_ORDER.batchCode` to an empty target execution cell, `When` the execution record is created or opened, `Then` the backend persists the source value into `cell_values_json` and updates the field audit chain.
- `BDD: Existing manual target value is not overwritten -> Given` the target cell already has a stored value and the rule uses `ONLY_WHEN_EMPTY`, `When` the execution is opened again, `Then` the stored value remains unchanged and the auto-persist result reports `TARGET_ALREADY_MANUAL`.
- `BDD: Missing production batch code fails fast -> Given` an enabled production work order batch-code link exists but the work order `batchCode` is blank, `When` the execution is created or opened, `Then` the backend returns a clear missing source value error and does not write a blank/default value.
- `BDD: Repeated open is idempotent -> Given` the same rule and source value were already auto-persisted, `When` the execution is opened repeatedly, `Then` no duplicate audit batch is appended and the hash chain remains valid.
- `BDD: Frontend uses persisted values only -> Given` the execution detail does not contain a stored value, `When` the execution page hydrates draft state, `Then` the frontend must not inject `/prefill` values as if they were saved.

## RED/GREEN Evidence

- Pending RED: backend auto-persist service test.
- Pending RED: frontend static contract proving `/prefill` draft injection is still present.

## Milestone Updates

- Created implementation task directory and recorded applicable BDD scenarios.

## Blockers

- None currently.
