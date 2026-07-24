# Task: DCC v1 backend workflow id generation fix

## Goal

Fix DCC workflow persistence so `/dcc/controlled-files/submit` can insert the controlled-file record and route snapshots successfully, instead of failing on missing primary-key defaults.

## Scope

- Repair the primary-key generation strategy for `dcc_controlled_file`.
- Repair the primary-key generation strategy for `dcc_controlled_file_route_snapshot` if it shares the same defect.
- Keep the fix scoped to DCC workflow persistence and the minimum schema or DO changes required.
- Verify the real submit path can move past local persistence and reach BPM startup.

## Previous Task Check

- Previous backend task: `doc/tasks/20260513-dcc-v1-backend-original-upload-contract/task.md`
- Status before this task: completed and committed in `9ff2f86eee`.
- Impact: original-file upload is now closed, so this task can focus only on workflow persistence after upload succeeds.

## Milestones

- [x] M1: Previous backend task checked before new work.
- [x] M2: Task document, execution log, and backend evidence file created before production code changes.
- [x] M3: BDD scenario and RED verification captured for workflow submit persistence.
- [x] M4: Minimal code or schema fix implemented for DCC workflow ids.
- [x] M5: Targeted backend verification completed, including real submit moving past insert.
- [x] M6: Task-only backend changes committed after verification passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.worktrees\dcc-v1-backend\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Real API verification against isolated backend `48082` for `/dcc/controlled-files/submit`

## Current Status

Completed. DCC workflow submit now persists `dcc_controlled_file` and `dcc_controlled_file_route_snapshot` with MySQL `AUTO_INCREMENT` ids, and the isolated runtime submit path moves past local persistence into BPM startup. The fix was applied at the schema layer to match the repository's MySQL convention instead of leaving DCC as a one-off `ASSIGN_ID` exception.
