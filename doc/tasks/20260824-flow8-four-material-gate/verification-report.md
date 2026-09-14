# Verification Report

## Current Status

in_progress

The Flow8 task-owned slice is integrated on `int_main` as `4b764f835`, with source-binding recheck review `18c37fe4e`; the end-to-end business task remains `in_progress`.

- PASS: four fixed node types are mandatory and independently evaluated.
- PASS: legacy dossier requirement switches and their config hash no longer participate in release decisions.
- PASS: current task approval, latest attachment action/version, persisted file metadata, SHA-256, retention hash, attachment hash, Flow7 source snapshot, and deterministic manifest are checked.
- PASS: latest `PENDING`/`VOID` evidence blocks without falling back; source/manifest change invalidates the precheck snapshot.
- PASS: a source snapshot change now transitions the gate to `MATERIALS_RECHECK_REQUIRED`; it remains blocked while persisted material-task source witnesses reference the old snapshot and only returns `MATERIALS_READY` after those witnesses are refreshed. `routeBindingSnapshotHash` cannot substitute for the material source witness.
- PASS: precheck, submit, submit-for-approval, and final approval consume the shared server gate. The gate contains no `RELEASED` write.
- PASS (historical): focused Flow8 10/10 and Flow8/release regression 40/40; MES 24-module compile passed on the prior integrated baseline.
- PASS: v6 branch runtime guard passed for the isolated worktree (slot 9, frontend 8090, backend 48090) and for integrated `int_main` (frontend 8081, backend 48081).
- PASS: integrated `int_main` rerun: the same 40 targeted Flow8/release tests passed and the MES 24-module reactor completed successfully.
- PASS: `pnpm ts:check` completed successfully after the profile page became read-only.
- PASS (historical): the prior targeted backend run completed 12/12 with 0 failures and 0 errors after the source-binding recheck fix.
- BLOCKED (latest direct run): the MES module compile stopped before tests on 7 unrelated dirty production-release/simulation errors (`MesReleaseAuthoritativeContextPortImpl`, `MesStage5FinalReleaseSimulationServiceImpl`, and `MesStage6IdiSimulationServiceImpl`). Flow8 files were not the failure source and no unrelated files were changed.
- BLOCKED (latest reactor retry): Maven stopped in unrelated dirty `yudao-module-bpm` test compilation with 9 errors in `FormTemplateFillRuleAutoDetectServiceTest`; `yudao-module-mes` tests were not reached. No BPM files were changed for Flow8.
- RED found and isolated a Flow8 ordering defect in final approval; the gate ran after authoritative context resolution. The fix is limited to moving the shared gate before that context call.
- FAIL (external baseline): `pnpm build:prod` stops at `TrainingRulesReadonlyTab.vue`, an unrelated empty SFC with no template or script block.
- FAIL (external baseline): expanded batch-execution regression has 174 setup/reflection errors unrelated to the Flow8 gate.
- NOT RUN: real Playwright and production migration execution. The current round adds the idempotent `20260826_mes_edhr_material_task_source_witness.sql` migration, but the dirty integration worktree does not provide a safe database verification context.

## Current int_main Repair Verification

- PASS: `material_source_snapshot_hash` is present in the task model and test schema.
- PASS: normal, production-release, and rejected-batch re-execution provisioning paths pass the formal `sourceSnapshotHash` into all four material tasks; missing source evidence fails fast.
- PASS: the gate reads only the dedicated material source witness and the Flow7 source precheck.
- PASS: Flow8 regression coverage proves that matching route-binding metadata alone remains blocked.
- PASS: `git diff --check` and the Flow8 static source/migration contract checks.
- BLOCKED: Maven GREEN is not available in the current worktree because an unrelated stage5 source calls missing `ErpKingdeeProductionPickListMapper.hardDeleteById(Long)`, and the dirty test source set references unrelated deleted MES services. These files were not changed by Flow8.

Remaining release blocker: provide a writable test tenant, production/PQC/manager accounts, an existing mapped batch execution, the four cleanable material files, and cleanup authority for the real Playwright path.
