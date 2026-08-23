# Execution Log

## Current Status

in_progress

## Intent

Implement only Flow8 batch-execution four-material readiness. Exclude Flow10 final-state ownership, Flow4/6 behavior, old mixed commits, and unrelated PQC/QA/ERP files.

## BDD

- BDD: Four independent materials are mandatory -> Given an existing legal batch execution, When release readiness is checked and any one of the four current material nodes is missing or invalid, Then the server blocks and does not return MATERIALS_READY.
- BDD: Current evidence becomes ready -> Given all four current nodes are completed with valid persisted attachment metadata and hashes, When readiness is checked, Then the server returns MATERIALS_READY without writing RELEASED.
- BDD: Replacement invalidates prior readiness -> Given a previously ready manifest, When a newer attachment version is pending, void, incomplete, or has changed evidence, Then readiness becomes MATERIALS_RECHECK_REQUIRED and no older attachment is accepted.
- BDD: Batch creation is independent -> Given a legal Flow6/9 batch creation request with no materials, When the batch is created or reused, Then Flow8 does not block creation; the material state is pending until a release attempt.
- BDD: Formal trace source is required -> Given Flow7 Origin/TraceLink evidence is unavailable, When a release entry consumes the gate, Then it fails fast with TRACE_MAPPING_BLOCKED before material readiness and does not fabricate a source.

## Evidence

- Baseline: 8af0aa8f2f740cfa8e125a31e695bedbb4c9d619.
- Branch: codex/20260824-flow8-four-material-gate.
- Initial worktree: clean; no staged or unstaged changes.
- BDD: Four independent materials are mandatory -> Given an existing legal batch execution, When one current material is missing, Then the gate returns `MATERIALS_PENDING` and blocks release.
- BDD: Current evidence becomes ready -> Given all four current nodes and persisted file metadata/hash evidence are valid, When the gate evaluates the batch, Then it returns `MATERIALS_READY` with a deterministic manifest and does not write `RELEASED`.
- BDD: Replacement invalidates prior readiness -> Given an older valid attachment, When a newer `PENDING` or `VOID` version exists, Then the gate returns `MATERIALS_RECHECK_REQUIRED` and does not fall back.
- BDD: Formal trace source is required -> Given Flow7 source precheck is missing or invalid, When the gate runs, Then it fails before material queries with stable blocker `TRACE_MAPPING_BLOCKED`.
- BDD: Legacy switches cannot affect the hard gate -> Given a passed four-material precheck, When old dossier configuration changes, Then submit continues to use only the frozen material manifest.
- RED: `& 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -pl yudao-module-mes -am "-Dtest=MesProEdhrFourMaterialGateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `cannot find symbol: MesProEdhrFourMaterialGateService` at test line 35; exit code 1.
- GREEN: `& 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -pl yudao-module-mes -am "-Dtest=MesProEdhrFourMaterialGateServiceTest,MesProEdhrFourMaterialGateReleaseContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests, 0 failures, 0 errors; exit code 0.
- REGRESSION: same Maven path with `cn.iocoder.yudao.module.mes.MesProEdhrReleaseServiceImplTest,cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImplTest` plus the two Flow8 tests -> PASS, 40 tests, 0 failures, 0 errors; exit code 0.
- GREEN (latest): focused Flow8 service and release-contract selection -> PASS, 10 tests, 0 failures, 0 errors; exit code 0.
- RED: legacy dossier configuration changes after precheck -> FAIL at `requireDossierRequirementConfigHashCurrent`; 1 test, 1 error; exit code 1.
- GREEN: same legacy-configuration scenario after removing its runtime release influence -> PASS, 1 test, 0 failures, 0 errors; exit code 0.
- REGRESSION: expanded batch-execution selection -> FAIL, 213 tests, 174 errors; 167 errors are missing existing `MesBatchExecutionEntryContractService` bean and 7 errors are absent legacy `resolveTaskGate` reflection signatures. No Flow8 assertion failed; exit code 1.
- GREEN: `& 'C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd' -pl yudao-module-mes -am -DskipTests compile` -> PASS across 24 reactor modules; exit code 0.
- PRECOMMIT: `& .\scripts\preflight\branch-runtime-port-guard.ps1` -> PASS; slot 9, frontend 8090, backend 48090.

## Blockers

- Flow7 production resolver is consumed directly. A real tenant dataset proving persisted source change between precheck and submit remains an external integration prerequisite.
- Real Playwright prerequisites are not supplied; E2E remains NOT RUN and will not be replaced by mocks or API-only checks.
- The expanded batch-execution regression remains blocked by existing test-container/legacy-reflection failures owned outside Flow8.
