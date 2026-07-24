# Task: DCC v1 backend approval-to-publish chain

## Goal

Drive the DCC workflow from an existing pending approval into the post-approval publish state, and close the exact backend gaps that still prevent controlled release, stamp generation, or browser visibility after approval completion.

## Scope

- Inspect the current DCC approval completion listener and finalization path.
- Verify how an approved BPM instance should update `dcc_controlled_file`.
- Verify how stamped output should be generated and stored.
- Fix only the backend gaps required to move one real isolated-runtime record from `APPROVING` into its expected published state.
- Leave unrelated UI polish and unrelated infrastructure changes out of scope.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-workflow-id-generation-fix/task.md`
- Status before this task: completed and committed in `2956613387`.
- Impact: submit persistence and BPM startup are already closed, so this task can focus only on what happens after approval is completed.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document, execution log, and backend evidence file created before production code changes.
- [x] M3: BDD scenario and RED verification captured for approval-to-publish behavior.
- [x] M4: Minimal backend fix implemented for approval completion, stamp generation, or post-approval visibility.
- [x] M5: Targeted backend verification completed against the isolated runtime.
- [x] M6: Task-only backend changes committed after verification passes.

## Expected Verification

- Targeted Maven verification for the touched DCC finalization or listener tests
- Real isolated-runtime verification against backend `48082` using one existing DCC approval instance

## Current Status

Completed. Approval completion in the isolated runtime now advances a DCC record to `STAMPED`, creates the stamped `infra_file` artifact, clears the runtime task, and writes the DCC approval notification once the notify-template seed is present and valid.
