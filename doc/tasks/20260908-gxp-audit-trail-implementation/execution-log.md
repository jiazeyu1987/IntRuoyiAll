# Execution Log - 20260908 GxP Audit Trail Implementation

## BDD Scenarios

BDD: Project OWNER uses authoritative access rules -> Given a user only has an active project-code correction assignment, When a revision or submit operation asserts project OWNER, Then access is denied; only a current `dcc_project_access_rule` OWNER rule matching USER, DEPT, ROLE, or POSITION grants project ownership.

BDD: Legacy GxP subject index upgrades safely -> Given an existing `gxp_audit_event.subject_id` has the legacy full-width index, When the forward migration runs, Then it drops the old subject index before widening the column and recreates the index with a 191-character prefix afterward.

BDD: DCC approval reason is user-authored -> Given an approval request has a blank reason, When it enters through the DCC controller, approval-center adapter, or signature service, Then the request is rejected before workflow/signature persistence and no default approval reason is fabricated.

BDD: GXP audit append success -> Given a registered GxP write operation with required reason and before/after state, When the business service commits the change through the audit contract, Then the audit event is appended in the same transaction with actor, timestamp, action, reason, before/after, hash and policy version.

BDD: Missing audit policy blocks business write -> Given a GxP write operation is not registered in the approved policy, When the business service attempts to commit the change, Then the audit contract fails with GXP_AUDIT_POLICY_NOT_FOUND and the business data is rolled back.

BDD: Missing required reason blocks business write -> Given a registered operation requires reason text, When the caller omits the reason, Then the audit contract fails with GXP_AUDIT_REASON_REQUIRED and the business data is rolled back.

BDD: Audit append failure rolls back business write -> Given the database cannot append the audit event, When a GxP business write executes, Then no successful business state is committed and no default-success audit result is returned.

BDD: Idempotency conflict is rejected -> Given the same idempotency key was used with a different canonical payload, When the audit contract receives the second command, Then it fails with GXP_AUDIT_IDEMPOTENCY_CONFLICT.

BDD: Coverage gate rejects unregistered write entry -> Given a new GxP write method is added without policy registration, When the coverage check runs in CI, Then the check fails and reports the missing source locator and operationId.

BDD: Electronic signature writes unified audit in same transaction -> Given an authorized signer creates an electronic signature record, When `ElectronicSignatureServiceImpl.sign` inserts the signature record, Then it appends `signature.record.create` through `GxpAuditService.append` before transaction commit with reason, subject version, signature record id, content hash and before/after state.

BDD: Electronic signature rolls back when unified audit append fails -> Given the unified audit append contract rejects or fails, When electronic signature creation calls the audit contract, Then the signature record insert rolls back and no default-success signature result is returned.

BDD: DCC publish approval request writes unified audit in same transaction -> Given a controlled DCC file is ready to publish and a user submits a publish reason, When `DccControlledFilePublishServiceImpl.publishControlledFile` creates and submits the approval form, Then it appends `dcc.controlled-file.publish` through `GxpAuditService.append` before transaction commit with file identity, version, reason, idempotency key, and before/after state.

BDD: DCC publish approval request does not fabricate electronic signature identity -> Given DCC publish only starts the approval workflow and is not itself an electronic signature record, When the unified audit command is built, Then `signatureRecordId` remains absent and signature evidence is supplied only by the separate electronic signature operation.

BDD: System permission assignment writes unified audit in same transaction -> Given role-menu, user-role, or role-data-scope configuration is changed, When the assignment service commits the change, Then it appends the registered `system.permission.*.assign` operation through `GxpAuditService.append` with subject identity and before/after permission state before transaction commit.

BDD: System permission assignment rolls back when unified audit append fails -> Given unified audit append fails during a permission assignment, When the role-menu or user-role assignment has changed database rows, Then the surrounding transaction rolls back and no successful permission state is committed.

BDD: System configuration package import writes unified audit in same transaction -> Given a confirmed system configuration package import passes precheck and snapshot matching, When the package replaces configuration rows, Then it appends `system.config-package.import` with before/after snapshot hashes and restored counts before transaction commit.

BDD: System configuration package import rolls back when unified audit append fails -> Given unified audit append fails after configuration replacement, When the import attempts to return success, Then the transaction rolls back to the previous configuration snapshot and no default-success import response is returned.

BDD: eDHR field update writes unified audit in same transaction -> Given an eDHR field value save writes the local field audit batch, item hash chain and signature binding, When `MesProBatchRecordExecutionFieldAuditServiceImpl.saveChanges` returns success, Then it must also append `edhr.execution.field.update` through `GxpAuditService.append` with signature id, before/after field audit head hashes, cell value hashes, revision, reason category/text and idempotency key.

BDD: eDHR field update rolls back when unified audit append fails -> Given an eDHR field value save has inserted field audit batch, items and signature evidence inside the transaction, When the unified audit append contract fails, Then the field projection, audit batch, audit items and signature record all roll back and no default-success eDHR save result is returned.

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

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFilePublishServiceTest#publishControlledFile_submitsFormCenterActionWithoutApplyingDomainEffect+publishControlledFile_auditAppendFailureRejectsPublishResult" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected TDD/compiler reason before production implementation: DCC publish audit assertions were added before service support; first RED also exposed a missing `assertThrows` static import in the new test.

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFilePublishServiceTest#publishControlledFile_submitsFormCenterActionWithoutApplyingDomainEffect+publishControlledFile_auditAppendFailureRejectsPublishResult" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 2 tests passed; DCC publish approval request writes unified audit append command and audit append failure does not return a successful publish result.

GREEN: python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml -> PASS, operations=8, annotations=7, sha256=61a0206128d8d0d0fbf41b736a22639eddd3569af8638c628fac554dd41f13e6 after DCC policy correction.

RED: mvn -pl yudao-module-system -am "-Dtest=PermissionServiceTest#assignRoleMenuShouldAppendUnifiedGxpAuditAndRollbackOnAppendFailure+assignUserRoleShouldAppendUnifiedGxpAuditAndRollbackOnAppendFailure+assignRoleDataScopeShouldAppendUnifiedGxpAudit,SystemConfigPackageServiceImplTest#importPackageShouldAppendUnifiedGxpAudit+importPackageShouldRollbackWhenUnifiedGxpAuditAppendFails" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected TDD reason: permission assignment methods had no `GxpAuditService.append`; configuration package fixture also exposed missing `canonical_username` handling before GREEN.

GREEN: mvn -pl yudao-module-system "-Dtest=PermissionServiceTest#assignRoleMenuShouldAppendUnifiedGxpAuditAndRollbackOnAppendFailure+assignUserRoleShouldAppendUnifiedGxpAuditAndRollbackOnAppendFailure+assignRoleDataScopeShouldAppendUnifiedGxpAudit,SystemConfigPackageServiceImplTest#importPackageShouldAppendUnifiedGxpAudit+importPackageShouldRollbackWhenUnifiedGxpAuditAppendFails" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 5 tests passed; permission assignment and configuration package import now append unified audit in the same transaction and reject default success when append fails.

GREEN: python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml -> PASS, operations=8, annotations=7, sha256=61a0206128d8d0d0fbf41b736a22639eddd3569af8638c628fac554dd41f13e6 after system permission/config package connection.

GREEN: python -m py_compile script/gxp_audit_coverage_gate.py; python -X utf8 script/gxp_audit_coverage_gate.py --root . --policy config/gxp-audit-policy.yaml -> PASS; coverage gate now prunes generated directories such as `target`, `.git`, `node_modules`, `dist`, and `build` during directory traversal so CI does not spend time scanning generated source trees.

BLOCKED: PASS FOR OPERATIONAL COMPLIANCE -> runtime NTP, WORM/Object Lock, backup restore rehearsal, periodic review SOP execution, QA signature, training records, DB role separation and privileged audit externalization are real environment/quality records and are not present in this code-only implementation turn.

GREEN: project-experience-consolidation -> PASS, reusable GxP business-evidence boundary merged into `docs/backend-development.md#GxP-业务写入统一审计接入门禁` and routed from `docs/experience-index.md`; no new long-term experience document created.

GREEN: baseline commit -> PASS, `f42b4c1111645b7baea4648542866cbbc71c1833` committed current dirty worktree per `docs/task-closeout-rules.md` baseline exception before push.
Baseline commit file list:
- IntRuoyiBackend/config/gxp-audit-policy.yaml
- IntRuoyiBackend/script/backup-ops/scripts/modules/Infra/ObjectOps.psm1
- IntRuoyiBackend/script/backup-ops/scripts/modules/Infra/SshOps.psm1
- IntRuoyiBackend/script/tests/test_backup_minimal_closure.py
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePublishServiceImpl.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePublishServiceTest.java
- IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/MesProBatchRecordReportControllerTest.java
- doc/tasks/20260908-gxp-audit-trail-implementation/backend-api-evidence.md
- doc/tasks/20260908-gxp-audit-trail-implementation/execution-log.md
- doc/tasks/20260908-gxp-audit-trail-implementation/task.md
- doc/tasks/20260908-gxp-audit-trail-implementation/verification-report.md
- docs/backend-development.md
- docs/experience-index.md
- docs/csv-validation/CSV-INT-20260908-7.1-7.4/现场记录表单册.docx
- docs/csv-validation/CSV-INT-20260908-7.1-7.4/现场证据包-正式交付版.docx
- docs/csv-validation/CSV-INT-20260908-7.1-7.4/签字与审批汇总表.docx
- docs/csv-validation/IntRuoyi-eDHR-CSV/现场证据包.xlsx
- docs/csv-validation/SOP-EDHR-090-文档培训版本控制-受控草案.docx
- docs/csv-validation/SOP-EDHR-系统操作手册编写与批准模板.docx
- docs/csv-validation/TRN-EDHR-培训课程大纲与考核标准-受控草案.docx

BLOCKED: git push origin int_main -> FAIL, GitHub push blocked by user-level Git proxy `http.https://github.com.proxy=http://127.0.0.1:7890`; `git push origin int_main` failed with `Could not connect to server`, leaving local `int_main` ahead of `origin/int_main` by 2 commits.

BLOCKED: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_appendsUnifiedGxpAuditWithSignatureAndStateEnvelope+saveChanges_rollsBackFieldAuditWhenUnifiedGxpAuditAppendFails" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL before reaching the new RED assertions, because existing non-task route-device-parameter changes in `MesProRouteFlowConfigServiceImpl` reference missing methods (`requireRouteProcessForDeviceParameter`, `selectEnabledProcessDeviceBindings`, `selectRouteDeviceParameterRules`, `toRouteDeviceParameterDeviceResp`, `normalizeRouteDeviceParameterOptionValues`, `normalizeRouteDeviceParameterText`, `validateRouteDeviceParameterRule`, `requireRouteDevice`, `assertDeviceMappedToRouteProcess`). Strict TDD cannot continue production implementation until this MES compile prerequisite is restored or the user explicitly authorizes fixing that separate route configuration work.

BLOCKED: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_appendsUnifiedGxpAuditWithSignatureAndStateEnvelope+saveChanges_rollsBackFieldAuditWhenUnifiedGxpAuditAppendFails" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL after the route-device-parameter main compile blocker was fixed, but still before reaching the new eDHR RED assertions because MES `testCompile` has existing non-task missing DCC test dependencies/packages (`cn.iocoder.yudao.module.dcc.api.projectcode`, `cn.iocoder.yudao.module.dcc.signature.*`, `DccElectronicSignatureAuthorizationService`, `DccProjectCodeMapper`, `DccProjectCodeDO`, `cn.iocoder.yudao.module.dcc.enums`). Strict TDD cannot continue production implementation until MES test compilation is restored or the user explicitly authorizes repairing that separate dependency drift.

BDD: System permission UI supplies audit reason and idempotency -> Given an administrator changes role menu, user role, or role data-scope configuration from the frontend, When the user submits the permission dialog, Then the request must include a human-entered change reason and a cryptographically generated idempotency key, and missing request fields must fail validation instead of defaulting to empty permission sets.

BDD: Unified ledger sequence uses a tenant watermark -> Given two GxP audit events are appended for the same tenant, When the unified audit service writes the events, Then ledger sequences are allocated through the per-tenant sequence watermark and the second event hash chain points to the first event.

RED: Static review 2026-09-09 -> FAIL, expected reason: permission assignment frontend did not collect reason/idempotency, backend request VOs defaulted missing sets to empty sets, and unified ledger sequence used `MAX(ledger_sequence)+1` for each append.

GREEN: mvn.cmd -pl yudao-module-system "-Dtest=GxpAuditServiceImplTest,PermissionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 42 tests passed; GxP append now includes tenant ledger sequence watermark regression and PermissionService still rejects audit append failure without default success.

GREEN: mvn.cmd -pl yudao-module-system "-Dtest=RoleConfigPackageServiceImplTest,TenantServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 33 tests run, 0 failures, 1 skipped; internal tenant/package permission assignment callers now provide explicit audit reason and idempotency key.

RED: pnpm.cmd -s exec vue-tsc --noEmit --skipLibCheck -> FAIL, Node heap OOM at 4GB; verification command resource precondition insufficient for this frontend workspace.

GREEN: pnpm.cmd ts:check -> PASS, project-defined 8GB relaxed Vue/TypeScript check completed successfully; permission API types and three assignment dialogs compile.

GREEN: python -X utf8 -m pytest script\tests\test_gxp_audit_core_contract.py -q -> PASS, 2 tests passed; SQL contract still validates GxP audit core after adding tenant ledger sequence watermark.

GREEN: python -X utf8 -m py_compile script\gxp_audit_coverage_gate.py -> PASS; coverage gate script syntax valid after restricting scanning to module source roots.

GREEN: python -X utf8 script\gxp_audit_coverage_gate.py --root . --policy config\gxp-audit-policy.yaml -> PASS, operations=8, annotations=7, sha256=61a0206128d8d0d0fbf41b736a22639eddd3569af8638c628fac554dd41f13e6; generated/runtime directories are no longer scanned.

BLOCKED: mvn.cmd -pl yudao-module-mes -am -DskipTests compile -> interrupted after long-running javac because another MES Maven compile process was already active and both consumed CPU/memory concurrently; no MES compile PASS/FAIL evidence was produced in this turn.

GREEN: mvn.cmd -pl yudao-module-mes -am -DskipTests compile -> PASS, 25-module reactor including yudao-module-system, yudao-module-dcc and yudao-module-mes compiled successfully after the parallel compile cleared.

GREEN: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_appendsUnifiedGxpAuditWithSignatureAndStateEnvelope+saveChanges_rollsBackFieldAuditWhenUnifiedGxpAuditAppendFails" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 2 tests passed; eDHR field save appends unified GxP audit with signature/state envelope and rolls back when unified audit append fails.

BDD: M5 runtime evidence must fail closed -> Given the unified GxP audit software controls are implemented, When formal NTP, WORM/Object Lock, backup/recovery, periodic review, QA signoff, training, DB role separation, privileged audit externalization or monitoring evidence is missing, Then the operational compliance package must mark the missing item as BLOCKED and must not claim `PASS FOR OPERATIONAL COMPLIANCE`.

GREEN: M5 local read-only evidence collection -> PASS, 2026-09-09 18:08:26 +08:00; Windows Time service is `Running / Automatic`, `w32tm /query /status` reports source `time.windows.com,0x9` and last successful sync `2026/9/9 16:07:48`; local backend `http://127.0.0.1:48081/actuator/health` returned HTTP 200. This evidence is limited to the local validation environment and does not replace formal production chrony/NTP evidence.

GREEN: M5 operational evidence documents -> PASS, added `m5-operational-compliance-evidence.md`, `m5-periodic-review-sop.md`, and `m5-signoff-training-record.md` with fail-closed status mapping, SOP steps, signoff/training templates and explicit forbidden substitute evidence.

RED: M6 DCC targeted Maven regression -> FAIL, `DccSignatureVerificationServiceImpl` referenced the formal missing-reason error without importing it; after that compile fix, the adapter regression still failed because direct VO construction bypassed Bean Validation.

GREEN: M6 DCC targeted Maven regression -> PASS, 35 tests; OWNER persistence, request validation, approval adapter validation and signature service validation are green.

GREEN: M6 GxP SQL contract -> PASS, 6 tests; legacy subject index drop/widen/recreate order is locked.

GREEN: full release migration policy gate -> PASS, 620 migrations; DCC project access migration and direct publish policy metadata are valid.

INFO: Cleanup Keep updated -> `git check-ignore -v` shows the new M5 evidence files are hidden by `.git/info/exclude` rule `/doc/tasks/*/`; `task.md` now lists the three M5 files under `## Cleanup Keep` so closeout does not treat them as disposable artifacts.

BLOCKED: PASS FOR OPERATIONAL COMPLIANCE -> formal production/review environment evidence is still missing: enterprise-approved NTP source and drift proof, WORM/Object Lock primary/replica receipts, unified GxP archive package recovery rehearsal, executed periodic review batch, QA signature, completed training records, DB grants/role separation, privileged audit externalization receipts and monitoring alert rehearsal.
