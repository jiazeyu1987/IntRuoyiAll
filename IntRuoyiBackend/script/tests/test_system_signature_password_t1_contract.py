from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_signature_password_migration_adds_history_and_state_columns():
    migration = (ROOT / "sql/mysql/20260908_system_signature_password_t1.sql").read_text(encoding="utf-8")

    assert "DELIMITER $$" in migration
    assert "CREATE PROCEDURE intruoyi_add_system_users_esign_t1_column" in migration
    assert "CALL intruoyi_add_system_users_esign_t1_column('canonical_username'" in migration
    assert "CALL intruoyi_add_system_users_esign_t1_column('password_credential_status'" in migration
    assert "CREATE TABLE IF NOT EXISTS `system_user_password_history`" in migration
    assert "`password_hash` varchar(100) NOT NULL" in migration
    assert "UNIQUE KEY `uk_system_users_tenant_canonical_username` (`tenant_id`, `canonical_username`)" in migration
    assert migration.count("DELIMITER ;") >= 2


def test_system_test_schema_contains_signature_password_t1_fields():
    schema = (ROOT / "yudao-module-system/src/test/resources/sql/create_tables.sql").read_text(encoding="utf-8")

    assert '"canonical_username" varchar(64) not null' in schema
    assert '"password_credential_status" varchar(32) not null default \'ACTIVE\'' in schema
    assert 'CREATE TABLE IF NOT EXISTS "system_user_password_history"' in schema
    assert '"password_hash" varchar(100) not null' in schema
    assert 'unique ("tenant_id", "canonical_username")' in schema


def test_password_policy_uses_90_day_expiry_boundary():
    source = (ROOT / "yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserPasswordPolicy.java").read_text(encoding="utf-8")

    assert "MAX_AGE_DAYS = 90" in source
    assert "plusDays(MAX_AGE_DAYS).isAfter(now)" in source


def test_signature_identity_t2_uses_failure_window_and_reauth_contract():
    migration = (ROOT / "sql/mysql/20260908_system_signature_identity_t2.sql").read_text(encoding="utf-8")
    schema = (ROOT / "yudao-module-system/src/test/resources/sql/create_tables.sql").read_text(encoding="utf-8")
    service = (ROOT / "yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserService.java").read_text(encoding="utf-8")
    impl = (ROOT / "yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserServiceImpl.java").read_text(encoding="utf-8")

    assert "login_failure_window_start_time" in migration
    assert '"login_failure_window_start_time" timestamp default null' in schema
    assert "reauthenticateForSignature(Long id, String rawPassword)" in service
    assert "USER_LOGIN_FAILURE_WINDOW_MINUTES = 15" in impl
    assert "USER_LOGIN_LOCK_MINUTES = 30" in impl


def test_unified_electronic_signature_t3_kernel_contract():
    root_pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    migration = (ROOT / "sql/mysql/20260908_system_electronic_signature_t3.sql").read_text(encoding="utf-8")
    service = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureServiceImpl.java").read_text(encoding="utf-8")
    command = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/api/dto/ElectronicSignatureCommand.java").read_text(encoding="utf-8")

    assert "<module>yudao-module-signature</module>" in root_pom
    assert "CREATE TABLE IF NOT EXISTS `system_electronic_signature`" in migration
    assert "`signed_at` datetime NOT NULL" in migration
    assert "`content_hash` char(64) NOT NULL" in migration
    assert "`evidence_hash` char(64) NOT NULL" in migration
    assert "SecurityFrameworkUtils.getLoginUserId()" in service
    assert "adminUserApi.reauthenticateForSignature(actorId, command.credential())" in service
    assert "LocalDateTime.now()" in service
    assert "String credential" in command
    assert "signedAt" not in command


def test_unified_signature_subject_id_capacity_supports_encoded_business_identity():
    base_migration = (ROOT / "sql/mysql/20260908_system_electronic_signature_t3.sql").read_text(encoding="utf-8")
    repair_migration = (ROOT / "sql/mysql/20260908_system_electronic_signature_subject_id_capacity.sql").read_text(encoding="utf-8")
    test_schema = (ROOT / "yudao-module-signature/src/test/resources/sql/create_tables.sql").read_text(encoding="utf-8")

    assert "`subject_id` varchar(2048) NOT NULL" in base_migration
    assert "MODIFY COLUMN `subject_id` varchar(2048) NOT NULL" in repair_migration
    assert '"subject_id" varchar(2048) not null' in test_schema


def test_bpm_t5_approval_signatures_delegate_to_unified_kernel():
    pom = (ROOT / "yudao-module-bpm/pom.xml").read_text(encoding="utf-8")
    approval_center = (ROOT / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/ApprovalCenterServiceImpl.java").read_text(encoding="utf-8")
    policy_admin = (ROOT / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/businessapproval/service/BusinessApprovalPolicyAdministrationService.java").read_text(encoding="utf-8")
    orchestrator = (ROOT / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/businessapproval/service/BusinessApprovalOrchestrator.java").read_text(encoding="utf-8")
    record_service = (ROOT / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/signature/ApprovalSignatureRecordServiceImpl.java").read_text(encoding="utf-8")
    adapter = (ROOT / "yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/signature/BpmApprovalSignatureSubjectAdapter.java").read_text(encoding="utf-8")

    assert "<artifactId>yudao-module-signature</artifactId>" in pom
    assert "validatePassword(" not in approval_center
    assert "validatePassword(" not in policy_admin
    assert "validatePassword(" not in orchestrator
    assert "ElectronicSignatureService" in record_service
    assert "electronicSignatureService.sign(new ElectronicSignatureCommand(" in record_service
    assert "signatureRecordMapper.insert(" not in record_service
    assert "implements ElectronicSignatureSubjectAdapter" in adapter
    assert "ApprovalSignatureRecordServiceImpl.SUBJECT_TYPE" in adapter


def test_dcc_t4_controlled_file_signatures_delegate_to_unified_kernel():
    pom = (ROOT / "yudao-module-dcc/pom.xml").read_text(encoding="utf-8")
    service = (ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSignatureVerificationServiceImpl.java").read_text(encoding="utf-8")
    api = (ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSignatureVerificationService.java").read_text(encoding="utf-8")
    adapter = (ROOT / "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileSignatureSubjectAdapter.java").read_text(encoding="utf-8")

    assert "<artifactId>yudao-module-signature</artifactId>" in pom
    assert "DccUnifiedSignatureResult verifyPasswordAndCreateSignature" in api
    assert "ElectronicSignatureService" in service
    assert "electronicSignatureService.sign(new ElectronicSignatureCommand(" in service
    assert "signatureMapper.insert(" not in service
    assert "isPasswordMatch(" not in service
    assert "implements ElectronicSignatureSubjectAdapter" in adapter
    assert "DCC_CONTROLLED_FILE" in adapter


def test_mes_t6_batch_record_signatures_delegate_to_unified_kernel():
    pom = (ROOT / "yudao-module-mes/pom.xml").read_text(encoding="utf-8")
    service = (ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java").read_text(encoding="utf-8")
    edhr_batch = (ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java").read_text(encoding="utf-8")
    void_effect = (ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchVoidEffectServiceImpl.java").read_text(encoding="utf-8")
    record_change = (ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRecordChangeServiceImpl.java").read_text(encoding="utf-8")
    release_service = (ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java").read_text(encoding="utf-8")
    adapter = (ROOT / "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesBatchRecordSignatureSubjectAdapter.java").read_text(encoding="utf-8")

    assert "<artifactId>yudao-module-signature</artifactId>" in pom
    assert "ElectronicSignatureService" in service
    assert "electronicSignatureService.sign(new ElectronicSignatureCommand(" in service
    assert "isPasswordMatch(" not in service
    assert "validatePassword(" not in edhr_batch
    assert "batchSignatureMapper.insert(" not in edhr_batch
    assert "validatePassword(" not in void_effect
    assert "batchSignatureMapper.insert(" not in void_effect
    assert "validatePassword(" not in record_change
    assert "batchSignatureMapper.insert(" not in record_change
    assert "validatePassword(" not in release_service
    assert "implements ElectronicSignatureSubjectAdapter" in adapter
    assert "MES_BATCH_RECORD" in adapter


def test_signature_t7_review_and_history_query_contract():
    migration = (ROOT / "sql/mysql/20260908_system_electronic_signature_t7.sql").read_text(encoding="utf-8")
    query_service = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/api/ElectronicSignatureQueryService.java").read_text(encoding="utf-8")
    query_impl = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureQueryServiceImpl.java").read_text(encoding="utf-8")
    review_service = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureComplianceReviewService.java").read_text(encoding="utf-8")
    review_do = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/dal/dataobject/ElectronicSignatureComplianceReviewDO.java").read_text(encoding="utf-8")
    test_schema = (ROOT / "yudao-module-signature/src/test/resources/sql/create_tables.sql").read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `system_electronic_signature_review`" in migration
    assert "`review_type` varchar(32) NOT NULL" in migration
    assert "`sop_version` varchar(64) NOT NULL" in migration
    assert "`training_evidence_id` varchar(128) NOT NULL" in migration
    assert "`due_at` datetime NOT NULL" in migration
    assert "`escalated_at` datetime DEFAULT NULL" in migration
    assert 'CREATE TABLE IF NOT EXISTS "system_electronic_signature_review"' in test_schema
    assert "listBySubject(" in query_service
    assert "verifyEvidence(" in query_service
    assert "getCanonicalContentJson" in query_impl
    assert "getEvidenceHash" in query_impl
    assert "VERIFICATION_STATUS_MISMATCH" in query_impl
    assert "REVIEW_TYPE_QUARTERLY" in review_service
    assert "REVIEW_TYPE_SPECIAL" in review_service
    assert "createQuarterlyReview" in review_service
    assert "createSpecialReview" in review_service
    assert "markOverdueEscalated" in review_service
    assert "sopVersion" in review_do
    assert "trainingEvidenceId" in review_do


def test_signature_t8_seal_time_privileged_audit_and_recovery_contract():
    migration = (ROOT / "sql/mysql/20260908_system_electronic_signature_t8.sql").read_text(encoding="utf-8")
    time_service = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureTrustedTimeService.java").read_text(encoding="utf-8")
    seal_service = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureSealService.java").read_text(encoding="utf-8")
    privileged_service = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignaturePrivilegedAuditService.java").read_text(encoding="utf-8")
    recovery_service = (ROOT / "yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureArchiveRecoveryService.java").read_text(encoding="utf-8")
    test_schema = (ROOT / "yudao-module-signature/src/test/resources/sql/create_tables.sql").read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS `system_electronic_signature_time_evidence`" in migration
    assert "CREATE TABLE IF NOT EXISTS `system_electronic_signature_seal`" in migration
    assert "CREATE TABLE IF NOT EXISTS `system_electronic_signature_privileged_audit`" in migration
    assert "CREATE TABLE IF NOT EXISTS `system_electronic_signature_archive_recovery`" in migration
    assert "`trusted_time_source` varchar(128) NOT NULL" in migration
    assert "`drift_millis` bigint NOT NULL" in migration
    assert "`previous_seal_hash` char(64) DEFAULT NULL" in migration
    assert "`worm_evidence_id` varchar(128) NOT NULL" in migration
    assert "`restore_evidence_hash` char(64) NOT NULL" in migration
    assert 'CREATE TABLE IF NOT EXISTS "system_electronic_signature_time_evidence"' in test_schema
    assert "MAX_TRUSTED_TIME_DRIFT_MILLIS = 1000L" in time_service
    assert "createTrustedTimeEvidence" in time_service
    assert "createDailySeal" in seal_service
    assert "previousSealHash" in seal_service
    assert "wormEvidenceId" in seal_service
    assert "recordPrivilegedAccess" in privileged_service
    assert "recordRecoveryVerification" in recovery_service
