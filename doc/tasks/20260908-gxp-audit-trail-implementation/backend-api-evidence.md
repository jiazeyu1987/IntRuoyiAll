# Backend API Evidence - 20260908 GxP Audit Trail Implementation

## Scope

Implemented internal unified GxP audit append contract in the system module and first coverage registration for high-risk GxP write paths. This task does not merge business APIs into one public API; business services keep their own APIs and must call/comply with the unified audit append contract.

## API Contract and Data Contract

Internal contract files:

- `GxpAuditService.append(GxpAuditCommand)` returns `GxpAuditAppendResult`.
- `GxpAuditCommand` requires operationId, subject identity/version, reason, before/after state envelopes, idempotency key, request/trace metadata and optional signature reference.
- `GxpAuditStateEnvelope` carries explicit state, objectVersion and canonicalJson.
- `@GxpWriteOperation(operationId = "...")` marks GxP write methods for coverage enforcement.

## Auth, Permissions, Validation, and Error Behavior

- Tenant is resolved from security context or required tenant context.
- Actor is resolved server-side from security context; jobs without a login user use explicit `SYSTEM_ACTOR` semantics.
- Missing policy fails with `GXP_AUDIT_POLICY_NOT_FOUND`.
- Missing required reason fails with `GXP_AUDIT_REASON_REQUIRED`.
- Missing before/after state fails with `GXP_AUDIT_BEFORE_AFTER_REQUIRED`.
- Missing required signature fails with `GXP_AUDIT_SIGNATURE_REQUIRED`.
- Same idempotency key with different canonical payload fails with `GXP_AUDIT_IDEMPOTENCY_CONFLICT`.
- Append failure is mapped to `GXP_AUDIT_APPEND_FAILED` and remains transactional.

## Required Config, Services, Fixtures, and Migrations

- Policy registry: `IntRuoyiBackend/config/gxp-audit-policy.yaml`.
- Coverage gate: `IntRuoyiBackend/script/gxp_audit_coverage_gate.py`.
- CI gate: `IntRuoyiBackend/.github/workflows/maven.yml` runs policy coverage and SQL contract checks before Maven build.
- Test fixtures: H2 mirrors for GxP audit core tables in system test resources.

## BDD Scenarios

BDD: GXP audit append success -> Given a registered GxP write operation with required reason and before/after state, When the business service commits the change through the audit contract, Then the audit event is appended in the same transaction with actor, timestamp, action, reason, before/after, hash and policy version.

BDD: Missing audit policy blocks business write -> Given a GxP write operation is not registered in the approved policy, When the business service attempts to commit the change, Then the audit contract fails with GXP_AUDIT_POLICY_NOT_FOUND and the business data is rolled back.

BDD: Coverage gate rejects unregistered write entry -> Given a new GxP write method is added without policy registration, When the coverage check runs in CI, Then the check fails and reports the missing source locator and operationId.

BDD: Electronic signature writes unified audit in same transaction -> Given an authorized signer creates an electronic signature record, When `ElectronicSignatureServiceImpl.sign` inserts the signature record, Then it appends `signature.record.create` through `GxpAuditService.append` before transaction commit with reason, subject version, signature record id, content hash and before/after state.

BDD: Electronic signature rolls back when unified audit append fails -> Given the unified audit append contract rejects or fails, When electronic signature creation calls the audit contract, Then the signature record insert rolls back and no default-success signature result is returned.

## RED Command and Expected Failure

RED: `mvn -pl yudao-module-system "-Dtest=GxpAuditPersistenceModelTest,GxpAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected fixture reason: `SecurityFrameworkUtils.setLoginUser(null request)` cannot build `WebAuthenticationDetails`; test fixture was corrected to set Spring Security authentication directly.

RED: `python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml` -> FAIL, expected scanner reason: service method source locator resolver looked at repository root instead of module `src/main/java` trees.

RED: `mvn -pl yudao-module-signature -am "-Dtest=ElectronicSignatureServiceImplTest#testSign_successBindsActorServerTimeAndContentHash+testSign_auditAppendFailureRollsBackSignatureRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected implementation reason: electronic signature service did not yet call unified `GxpAuditService.append`; first implementation attempt exposed duplicate legacy `signature.gxp` Mapper conflict, which was resolved by removing the obsolete local audit implementation.

## GREEN Command and Passing Result

GREEN: `mvn -pl yudao-module-system "-Dtest=GxpAuditPersistenceModelTest,GxpAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests passed.

GREEN: `python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml` -> PASS, operations=8, annotations=7, sha256=61a0206128d8d0d0fbf41b736a22639eddd3569af8638c628fac554dd41f13e6.

GREEN: `python -X utf8 -m pytest script/tests/test_gxp_audit_core_contract.py -q` -> PASS, 2 tests passed.

GREEN: `mvn -pl yudao-module-signature,yudao-module-dcc,yudao-module-mes -am -DskipTests compile` -> PASS, affected high-risk modules compile after annotation registration.

GREEN: `mvn -pl yudao-module-signature -am "-Dtest=ElectronicSignatureServiceImplTest#testSign_successBindsActorServerTimeAndContentHash+testSign_auditAppendFailureRollsBackSignatureRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests passed.

GREEN: `mvn -pl yudao-module-signature -am "-Dtest=ElectronicSignatureServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 11 tests passed.

## Contract or Integration Verification

- Unit tests verify append success, full evidence fields, server actor resolution, missing policy, missing reason, missing state, missing required signature, idempotency replay and idempotency conflict.
- Electronic signature service now calls the unified system `GxpAuditService.append` in the same Spring transaction after signature record insert; append failure propagates and rolls back the signature record.
- Coverage gate verifies all annotated GxP write operations are present in policy, source locators resolve, high-risk domains EDHR/DCC/SIGNATURE/SYSTEM/RELEASE are registered, and policy shape is complete.
- Initial high-risk annotated entry points: eDHR field audit save, DCC controlled file publish, electronic signature sign, role-menu assignment, user-role assignment, role-data-scope assignment, system config package import.

## Observability Touchpoints

Current implementation writes stable event id, ledger sequence, event hash, operation id, subject identity, actor, reason, before/after envelopes, policy version and request id. Production metrics/alerts for append failure, chain failure, watermarks, WORM and review gaps remain a later operational integration step.

## Blockers and Downstream Needs

- Actual production operational compliance remains BLOCKED until NTP, WORM/Object Lock, backup restore rehearsal, periodic review SOP, QA signature and training evidence exist.
- Some first-batch domain methods are currently registered/annotated for coverage, but not all domain write methods have been refactored to call `GxpAuditService.append` with complete before/after snapshots. Electronic signature is now connected; eDHR, DCC, permission/role configuration, system configuration and release/migration paths still require the same conversion.
