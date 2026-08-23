# Backend API Evidence

## Scope

Flow8 server-side four-material readiness service and its integration with eDHR release precheck, submit, submit-for-approval, and final approval. Flow10 final-state ownership is unchanged.

## API And Data Contract

- Required nodes: `INCOMING_INSPECTION_REPORT`, `STERILIZATION_REPORT`, `FINISHED_PRODUCT_INSPECTION_REPORT`, `FINISHED_PRODUCT_INSPECTION_RECORD`.
- Input: persisted `batchExecutionId`; Flow7 `Origin/TraceLink` precheck; current batch tasks; current attachment versions; persisted file metadata.
- Output: `MATERIALS_PENDING`, `MATERIALS_RECHECK_REQUIRED`, or `MATERIALS_READY` plus deterministic manifest hash.
- `MATERIALS_READY` requires all four exact nodes. The two finished-product nodes cannot substitute for each other.
- The gate does not create/reuse batches and does not write `RELEASED`.

## Auth Permissions Validation And Errors

- Existing release entry authorization and signatures remain unchanged.
- Flow7 mapping is read before material queries; missing/invalid mapping is translated to stable `TRACE_MAPPING_BLOCKED`.
- Missing materials raise Flow8 gate blocker status; stale precheck/final manifest raises `PRO_EDHR_RELEASE_MATERIAL_MANIFEST_STALE`.
- No old required-setting flag participates in the material decision.
- Legacy dossier configuration changes do not stale a passed four-material manifest or block submit.

## Services Fixtures And Migrations

- Required services: Flow7 `MesProEdhrBatchTraceabilityService`, file persistence `FileService`, batch task and attachment mappers.
- Unit tests mock these dependencies to isolate gate rules; production wiring consumes the real services without fallback.
- No database schema or configuration migration is introduced.

## BDD Scenarios

- BDD: Four independent materials are mandatory -> Given a legal batch execution, When any fixed material is missing, Then readiness is blocked.
- Given any one exact node is absent, when release readiness is checked, then release is blocked with `MATERIALS_PENDING`.
- Given all four current versions and hashes are valid, when readiness is checked, then `MATERIALS_READY` and a manifest are returned.
- Given a newer `PENDING` or `VOID` attachment exists, when readiness is checked, then `MATERIALS_RECHECK_REQUIRED` is returned without old-version fallback.
- Given Flow7 mapping is missing, when readiness is checked, then `TRACE_MAPPING_BLOCKED` is returned before material persistence is read.

## RED

RED: Four-material gate service does not exist -> FAIL, expected missing service symbol.

`mvn.cmd -pl yudao-module-mes -am -Dtest=MesProEdhrFourMaterialGateServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` failed with missing `MesProEdhrFourMaterialGateService`; exit code 1.

## GREEN

GREEN: Four-material gate and release integration -> PASS.

- Focused Flow8: 10 tests passed, 0 failures/errors; exit code 0.
- Flow8 plus release regression: 40 tests passed, 0 failures/errors; exit code 0.
- MES reactor compile: 24 modules succeeded; exit code 0.

## Contract And Integration Verification

Contract test verifies every known release route reaches the shared gate, precheck manifest is frozen and rechecked, and Flow8 gate code does not write `STATUS_RELEASED`. `git diff --check` is required before commit.

## Observability

Existing release check items record each exact node result and the precheck snapshot records the material manifest. Existing release transaction/audit logging remains the observability owner.

## Blockers And Downstream Needs

- Real Playwright needs a writable tenant, production/PQC/manager accounts, mapped batch execution, four cleanable files, and cleanup authority.
- Expanded batch-execution regression is blocked by existing missing test bean and legacy reflection-signature errors outside Flow8.
- Production source-change evidence requires a real Flow7 persisted mapping dataset; no mock is accepted as E2E proof.
