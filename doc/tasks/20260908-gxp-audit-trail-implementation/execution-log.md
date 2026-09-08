# Execution Log - 20260908 GxP Audit Trail Implementation

## BDD Scenarios

BDD: GXP audit append success -> Given a registered GxP write operation with required reason and before/after state, When the business service commits the change through the audit contract, Then the audit event is appended in the same transaction with actor, timestamp, action, reason, before/after, hash and policy version.

BDD: Missing audit policy blocks business write -> Given a GxP write operation is not registered in the approved policy, When the business service attempts to commit the change, Then the audit contract fails with GXP_AUDIT_POLICY_NOT_FOUND and the business data is rolled back.

BDD: Missing required reason blocks business write -> Given a registered operation requires reason text, When the caller omits the reason, Then the audit contract fails with GXP_AUDIT_REASON_REQUIRED and the business data is rolled back.

BDD: Audit append failure rolls back business write -> Given the database cannot append the audit event, When a GxP business write executes, Then no successful business state is committed and no default-success audit result is returned.

BDD: Idempotency conflict is rejected -> Given the same idempotency key was used with a different canonical payload, When the audit contract receives the second command, Then it fails with GXP_AUDIT_IDEMPOTENCY_CONFLICT.

BDD: Coverage gate rejects unregistered write entry -> Given a new GxP write method is added without policy registration, When the coverage check runs in CI, Then the check fails and reports the missing source locator and operationId.

BDD: Electronic signature writes unified audit in same transaction -> Given an authorized signer creates an electronic signature record, When `ElectronicSignatureServiceImpl.sign` inserts the signature record, Then it appends `signature.record.create` through `GxpAuditService.append` before transaction commit with reason, subject version, signature record id, content hash and before/after state.

BDD: Electronic signature rolls back when unified audit append fails -> Given the unified audit append contract rejects or fails, When electronic signature creation calls the audit contract, Then the signature record insert rolls back and no default-success signature result is returned.

## TDD Evidence

RED: pending -> write focused failing tests/contracts before production implementation.
GREEN: pending -> record passing commands after implementation.

## Current Notes

- 任务从文档任务中拆出，后续代码、测试、CI 证据均记录在本目录。
- E2E 未由用户当轮明确要求，本任务默认不执行 Playwright E2E。

RED: mvn -pl yudao-module-system "-Dtest=GxpAuditPersistenceModelTest,GxpAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected fixture reason before GREEN: SecurityFrameworkUtils.setLoginUser(null request) cannot build WebAuthenticationDetails; test must set Authentication directly without bypassing audit actor resolution.

RED: python -X utf8 script\gxp_audit_coverage_gate.py --root . --policy config\gxp-audit-policy.yaml -> FAIL, expected scanner reason: SERVICE_METHOD locator resolution looked at repository root instead of module src/main/java trees. SQL static contract already PASS independently.

RED: evidence validators -> FAIL, expected documentation reason: database/backend evidence files referenced BDD scenarios indirectly but validator requires local BDD markers.

GREEN: mvn -pl yudao-module-system "-Dtest=GxpAuditPersistenceModelTest,GxpAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 10 tests passed; unified audit append contract validates success, policy missing, reason missing, before/after missing, signature missing, idempotency replay and idempotency conflict.

GREEN: python -X utf8 script\gxp_audit_coverage_gate.py --root . --policy config\gxp-audit-policy.yaml -> PASS, operations=8, annotations=7, sha256=61a0206128d8d0d0fbf41b736a22639eddd3569af8638c628fac554dd41f13e6.

GREEN: python -X utf8 -m pytest script\tests\test_gxp_audit_core_contract.py -q -> PASS, 2 tests passed; core SQL contains required tables, idempotency hash, unique constraints and append-only triggers.

GREEN: mvn -pl yudao-module-signature,yudao-module-dcc,yudao-module-mes -am -DskipTests compile -> PASS; first high-risk annotated modules compile.

GREEN: database/backend evidence validators -> PASS; both evidence files satisfy required BDD/RED/GREEN contract markers.

RED: mvn -pl yudao-module-signature -am "-Dtest=ElectronicSignatureServiceImplTest#testSign_successBindsActorServerTimeAndContentHash+testSign_auditAppendFailureRollsBackSignatureRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected implementation reason: `ElectronicSignatureServiceImpl.sign` did not yet call the unified `GxpAuditService.append`; first implementation attempt also exposed a duplicate legacy `cn.iocoder.yudao.module.signature.gxp` local audit Mapper conflicting with system unified audit Mapper.

GREEN: mvn -pl yudao-module-signature -am "-Dtest=ElectronicSignatureServiceImplTest#testSign_successBindsActorServerTimeAndContentHash+testSign_auditAppendFailureRollsBackSignatureRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 2 tests passed; electronic signature writes unified audit append command and audit append failure rolls back signature record.

GREEN: python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml -> PASS, operations=8, annotations=7, sha256=61a0206128d8d0d0fbf41b736a22639eddd3569af8638c628fac554dd41f13e6; `signature.gxp` legacy references no longer present.

GREEN: mvn -pl yudao-module-signature -am "-Dtest=ElectronicSignatureServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 11 tests passed; signature service regression remains green after removing the duplicate local GxP audit implementation.

BLOCKED: PASS FOR OPERATIONAL COMPLIANCE -> runtime NTP, WORM/Object Lock, backup restore rehearsal, periodic review SOP execution, QA signature, training records, DB role separation and privileged audit externalization are real environment/quality records and are not present in this code-only implementation turn.
