# Verification Report

## Current Status

in_progress

The Flow8 task-owned slice is integrated on `int_main` as `4b764f835`; the end-to-end business task remains `in_progress`.

- PASS: four fixed node types are mandatory and independently evaluated.
- PASS: legacy dossier requirement switches and their config hash no longer participate in release decisions.
- PASS: current task approval, latest attachment action/version, persisted file metadata, SHA-256, retention hash, attachment hash, Flow7 source snapshot, and deterministic manifest are checked.
- PASS: latest `PENDING`/`VOID` evidence blocks without falling back; source/manifest change invalidates the precheck snapshot.
- PASS: a source snapshot change now transitions the gate to `MATERIALS_RECHECK_REQUIRED`; it remains blocked while task route bindings reference the old snapshot and only returns `MATERIALS_READY` after those bindings are refreshed.
- PASS: precheck, submit, submit-for-approval, and final approval consume the shared server gate. The gate contains no `RELEASED` write.
- PASS: focused Flow8 10/10 and Flow8/release regression 40/40; MES 24-module compile passed.
- PASS: v6 branch runtime guard passed for the isolated worktree (slot 9, frontend 8090, backend 48090) and for integrated `int_main` (frontend 8081, backend 48081).
- PASS: integrated `int_main` rerun: the same 40 targeted Flow8/release tests passed and the MES 24-module reactor completed successfully.
- PASS: `pnpm ts:check` completed successfully after the profile page became read-only.
- PASS: latest targeted backend run completed 12/12 with 0 failures and 0 errors after the source-binding recheck fix.
- FAIL (external baseline): `pnpm build:prod` stops at `TrainingRulesReadonlyTab.vue`, an unrelated empty SFC with no template or script block.
- FAIL (external baseline): expanded batch-execution regression has 174 setup/reflection errors unrelated to the Flow8 gate.
- NOT RUN: real Playwright and production migration verification. No schema migration is introduced by this slice.

Remaining release blocker: provide a writable test tenant, production/PQC/manager accounts, an existing mapped batch execution, the four cleanable material files, and cleanup authority for the real Playwright path.
