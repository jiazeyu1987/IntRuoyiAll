# Task: DCC menu chinese labels

## Goal

Rename the DCC standard backend menu labels from temporary English names to stable Chinese labels while preserving the standard backend menu and role-permission chain.

## Scope

- Update the DCC menu seed names in `sql/mysql/20260513_dcc_base_schema.sql`.
- Update the runtime `system_menu` rows for DCC ids `6800-6814` through standard backend menu APIs.
- Keep existing paths, components, permissions, and role assignments unchanged.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-user-flow-contract/task.md`
- Status before this task: completed and committed.
- Impact: DCC menu rows already exist and current admin role already has them assigned, so this task only changes labels.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document and execution log created before production changes.
- [x] M3: BDD scenario and RED evidence captured for temporary non-Chinese DCC labels.
- [x] M4: SQL seed labels updated to Chinese names.
- [x] M5: Runtime menu labels updated through backend APIs and verified via `get-permission-info`.
- [x] M6: Task-only backend changes committed after verification passes.

## Expected Verification

- `get-permission-info` returns the `/dcc` menu tree with Chinese labels.
- `sql/mysql/20260513_dcc_base_schema.sql` contains the same Chinese labels for DCC menu seeds.

## Current Status

Completed. SQL seed, runtime menu labels, and task-only backend commit are complete.
