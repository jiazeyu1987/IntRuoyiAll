# Verification Report

## Current Status

in_progress

The Flow8 code slice is ready for isolated task-owned commit handoff; the end-to-end business task remains `in_progress`.

- PASS: four fixed node types are mandatory and independently evaluated.
- PASS: legacy dossier requirement switches and their config hash no longer participate in release decisions.
- PASS: current task approval, latest attachment action/version, persisted file metadata, SHA-256, retention hash, attachment hash, Flow7 source snapshot, and deterministic manifest are checked.
- PASS: latest `PENDING`/`VOID` evidence blocks without falling back; source/manifest change invalidates the precheck snapshot.
- PASS: precheck, submit, submit-for-approval, and final approval consume the shared server gate. The gate contains no `RELEASED` write.
- PASS: focused Flow8 10/10 and Flow8/release regression 40/40; MES 24-module compile passed.
- PASS: v6 branch runtime guard passed for slot 9 (`int_main`, frontend 8090, backend 48090).
- FAIL (external baseline): expanded batch-execution regression has 174 setup/reflection errors unrelated to the Flow8 gate.
- NOT RUN: real Playwright and production migration verification. No schema migration is introduced by this slice.

Remaining release blocker: provide a writable test tenant, production/PQC/manager accounts, an existing mapped batch execution, the four cleanable material files, and cleanup authority for the real Playwright path.
