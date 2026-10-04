package cn.iocoder.yudao.module.signature.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureActionDefinition;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectSnapshot;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureArchiveRecoveryDO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureComplianceReviewDO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignaturePrivilegedAuditDO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureSealDO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureTimeEvidenceDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureArchiveRecoveryMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureComplianceReviewMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignaturePrivilegedAuditMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureSealMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureTimeEvidenceMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({ElectronicSignatureServiceImpl.class, ElectronicSignatureQueryServiceImpl.class,
        ElectronicSignatureComplianceReviewService.class, ElectronicSignatureTrustedTimeService.class, ElectronicSignatureSealService.class, ElectronicSignaturePrivilegedAuditService.class, ElectronicSignatureArchiveRecoveryService.class, ElectronicSignatureServiceImplTest.TestAdapterConfiguration.class})
public class ElectronicSignatureServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ElectronicSignatureServiceImpl signatureService;
    @Resource
    private ElectronicSignatureQueryService signatureQueryService;
    @Resource
    private ElectronicSignatureComplianceReviewService complianceReviewService;
    @Resource
    private ElectronicSignatureTrustedTimeService trustedTimeService;
    @Resource
    private ElectronicSignatureSealService sealService;
    @Resource
    private ElectronicSignaturePrivilegedAuditService privilegedAuditService;
    @Resource
    private ElectronicSignatureArchiveRecoveryService archiveRecoveryService;
    @Resource
    private ElectronicSignatureRecordMapper signatureRecordMapper;
    @Resource
    private ElectronicSignatureComplianceReviewMapper complianceReviewMapper;
    @Resource
    private ElectronicSignatureTimeEvidenceMapper timeEvidenceMapper;
    @Resource
    private ElectronicSignatureSealMapper sealMapper;
    @Resource
    private ElectronicSignaturePrivilegedAuditMapper privilegedAuditMapper;
    @Resource
    private ElectronicSignatureArchiveRecoveryMapper archiveRecoveryMapper;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private GxpAuditService gxpAuditService;

    @Test
    void selectedSignerAuthenticatesOwnPasswordAndRetainsLoginOperator() {
        var identity = new cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity("SYSTEM_USER", 202L, "员工B");
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            var result = signatureService.signAuthorized(buildCommand("selected-202", "V1", "production"), identity, null);
            verify(adminUserApi).reauthenticateForSignature(202L, "Signer@2026");
            verify(adminUserApi, never()).reauthenticateForSignature(eq(101L), anyString());
            var evidence = signatureQueryService.getById(result.signatureId());
            assertEquals(202L, evidence.actorId());
            var persisted = com.alibaba.fastjson.JSON.parseObject(evidence.canonicalContentJson()).getJSONObject("signatureIdentity");
            assertEquals(101L, persisted.getLong("operatorId"));
            assertEquals("SYSTEM_USER", persisted.getString("domain"));
            assertEquals("VALID", signatureQueryService.verifyEvidence(result.signatureId()).verificationStatus());
            assertEquals(result.signatureId(), signatureService.signAuthorized(buildCommand("selected-202", "V1", "production"), identity, null).signatureId());
        }
    }

    @Test
    void profileIdentityUsesOwnCredentialDomainAndVerifiesPersistedEvidence() {
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            var profile = new cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity("MES_EMPLOYEE_PROFILE", 202L, "临时工");
            var authenticate = mock(Runnable.class);
            var result = signatureService.signAuthorized(buildCommand("profile-202", "V1", "production"), profile, authenticate);
            verify(authenticate).run();
            verifyNoInteractions(adminUserApi);
            assertEquals("临时工", signatureQueryService.getById(result.signatureId()).actorDisplayName());
            assertEquals("VALID", signatureQueryService.verifyEvidence(result.signatureId()).verificationStatus());
            var system = new cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity("SYSTEM_USER", 202L, "系统员工");
            assertServiceException(() -> signatureService.signAuthorized(buildCommand("profile-202", "V1", "production"), system, null), ESIGN_DUPLICATE_IDEMPOTENCY_KEY);
        }
    }

    @Test
    void selectedSignerWrongCredentialAndProfileAuthenticationFailureLeaveNoRecord() {
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            doThrow(new IllegalArgumentException("wrong password")).when(adminUserApi).reauthenticateForSignature(202L, "Signer@2026");
            var system = new cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity("SYSTEM_USER", 202L, "员工B");
            assertThrows(IllegalArgumentException.class, () -> signatureService.signAuthorized(buildCommand("bad-202", "V1", "production"), system, null));
            var profile = new cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity("MES_EMPLOYEE_PROFILE", 202L, "临时工");
            assertThrows(IllegalStateException.class, () -> signatureService.signAuthorized(buildCommand("bad-profile", "V1", "production"), profile, () -> { throw new IllegalStateException("profile forbidden"); }));
            assertEquals(0L, signatureRecordMapper.selectCount(null));
            verify(gxpAuditService, never()).append(any());
        }
    }

    @Test
    public void testSign_successBindsActorServerTimeAndContentHash() {
        ElectronicSignatureCommand command = buildCommand("idem-001", "V1", "审批通过");
        when(gxpAuditService.append(any())).thenReturn(new GxpAuditAppendResult(9001L, 1L, "a".repeat(64), false));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            ElectronicSignatureResult result = signatureService.sign(command);

            assertNotNull(result.signatureId());
            assertEquals("VALID", result.verificationStatus());
            assertNotNull(result.signedAt());
            assertNotNull(result.timeEvidenceId());
            assertEquals("V1", result.subjectVersion());
            assertEquals(64, result.contentHash().length());
            assertEquals(64, result.evidenceHash().length());
            assertEquals("SHA-256", result.algorithm());
            verify(adminUserApi).reauthenticateForSignature(101L, "Signer@2026");
            ElectronicSignatureRecordDO record = signatureRecordMapper.selectById(result.signatureId());
            assertEquals(101L, record.getActorId());
            assertEquals("SESSION_PLUS_PASSWORD", record.getAuthenticationMethod());
            assertEquals("{\"name\":\"record\",\"version\":\"V1\"}", record.getCanonicalContentJson());
            assertTrue(record.getTimeEvidenceId().startsWith("SERVER_CLOCK:"));

            ArgumentCaptor<GxpAuditCommand> auditCaptor = ArgumentCaptor.forClass(GxpAuditCommand.class);
            verify(gxpAuditService).append(auditCaptor.capture());
            GxpAuditCommand auditCommand = auditCaptor.getValue();
            assertEquals("signature.record.create", auditCommand.getOperationId());
            assertEquals("TEST_RECORD:R001", auditCommand.getSubjectId());
            assertEquals("V1", auditCommand.getSubjectVersion());
            assertEquals("审批通过", auditCommand.getReason());
            assertEquals("SIGNATURE:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex("idem-001"),
                    auditCommand.getIdempotencyKey());
            assertEquals(String.valueOf(result.signatureId()), auditCommand.getSignatureRecordId());
            assertEquals(result.contentHash(), auditCommand.getSignatureContentHash());
            assertEquals("NO_SIGNATURE_RECORD", auditCommand.getBeforeState().getState());
            assertEquals("ELECTRONIC_SIGNATURE_RECORDED", auditCommand.getAfterState().getState());
            assertEquals("{}", auditCommand.getBeforeState().getCanonicalJson());
            assertTrue(auditCommand.getAfterState().getCanonicalJson().contains("\"signatureId\":" + result.signatureId()));
            assertTrue(auditCommand.getAfterState().getCanonicalJson().contains("\"verificationStatus\":\"VALID\""));
        }
    }

    @Test
    public void testSign_auditAppendFailureRollsBackSignatureRecord() {
        ElectronicSignatureCommand command = buildCommand("idem-audit-fail", "V1", "审批通过");
        when(gxpAuditService.append(any())).thenThrow(new IllegalStateException("audit append failed"));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            assertThrows(IllegalStateException.class, () -> signatureService.sign(command));

            assertEquals(0L, signatureRecordMapper.selectCount(null));
            verify(adminUserApi).reauthenticateForSignature(101L, "Signer@2026");
            verify(gxpAuditService).append(any());
        }
    }

    @Test
    public void testSign_sameIdempotencyReturnsExistingRecord() {
        ElectronicSignatureCommand command = buildCommand("idem-002", "V1", "审批通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            ElectronicSignatureResult first = signatureService.sign(command);
            ElectronicSignatureResult second = signatureService.sign(command);

            assertEquals(first.signatureId(), second.signatureId());
            verify(adminUserApi, times(1)).reauthenticateForSignature(101L, "Signer@2026");
            assertEquals(1L, signatureRecordMapper.selectCount(null));
        }
    }

    @Test
    public void testSign_longBusinessKeyProducesBoundedDistinctAuditKeysAndReplays() {
        String prefix = "MES|101|PQC_RELEASE|" + "x".repeat(110);
        when(gxpAuditService.append(any())).thenAnswer(invocation -> {
            GxpAuditCommand audit = invocation.getArgument(0);
            assertTrue(audit.getIdempotencyKey().length() <= 96,
                    "Signature audit key must satisfy the GxP V2 limit");
            return new GxpAuditAppendResult(9001L, 1L, "a".repeat(64), false);
        });
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            ElectronicSignatureCommand firstCommand = buildCommand(prefix + "A", "V1", "审批通过");
            ElectronicSignatureResult first = signatureService.sign(firstCommand);
            assertEquals(first.signatureId(), signatureService.sign(firstCommand).signatureId());
            signatureService.sign(buildCommand(prefix + "B", "V1", "审批通过"));
            ArgumentCaptor<GxpAuditCommand> audits = ArgumentCaptor.forClass(GxpAuditCommand.class);
            verify(gxpAuditService, times(2)).append(audits.capture());
            assertEquals("SIGNATURE:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(prefix + "A"),
                    audits.getAllValues().get(0).getIdempotencyKey());
            assertEquals("SIGNATURE:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(prefix + "B"),
                    audits.getAllValues().get(1).getIdempotencyKey());
            assertNotEquals(audits.getAllValues().get(0).getIdempotencyKey(),
                    audits.getAllValues().get(1).getIdempotencyKey());
            assertEquals(prefix + "A", signatureRecordMapper.selectById(first.signatureId()).getIdempotencyKey());
        }
    }

    @Test
    public void testSign_sameIdempotencyDifferentCommandRejected() {
        ElectronicSignatureCommand firstCommand = buildCommand("idem-003", "V1", "审批通过");
        ElectronicSignatureCommand secondCommand = buildCommand("idem-003", "V1", "复核通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            signatureService.sign(firstCommand);

            assertServiceException(() -> signatureService.sign(secondCommand),
                    ESIGN_DUPLICATE_IDEMPOTENCY_KEY);
        }
    }

    @Test
    public void testSign_requiresCurrentLoginUser() {
        ElectronicSignatureCommand command = buildCommand("idem-004", "V1", "审批通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);

            assertServiceException(() -> signatureService.sign(command), ESIGN_LOGIN_REQUIRED);
            verify(adminUserApi, never()).reauthenticateForSignature(anyLong(), anyString());
        }
    }

    @Test
    public void testSign_rejectsUnsignedSubjectVersionMismatch() {
        ElectronicSignatureCommand command = buildCommand("idem-005", "V2", "审批通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            ServiceException exception = assertThrows(ServiceException.class, () -> signatureService.sign(command));
            assertEquals(ESIGN_SUBJECT_NOT_SIGNABLE.getCode(), exception.getCode());
        }
    }

    @Test
    public void testQueryEvidence_listsSubjectAndVerifiesStoredHashes() {
        ElectronicSignatureCommand command = buildCommand("idem-006", "V1", "审批通过");
        when(adminUserApi.getUser(101L)).thenReturn(new AdminUserRespDTO().setId(101L).setNickname("张三"));

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            ElectronicSignatureResult signed = signatureService.sign(command);

            var evidence = signatureQueryService.listBySubject("TEST", "TEST_RECORD", "R001");
            assertEquals(1, evidence.size());
            ElectronicSignatureEvidenceDTO item = evidence.get(0);
            assertEquals(signed.signatureId(), item.id());
            assertEquals("APPROVE", item.actionCode());
            assertEquals("张三", item.actorDisplayName());
            assertEquals("{\"name\":\"record\",\"version\":\"V1\"}", item.canonicalContentJson());

            ElectronicSignatureVerificationDTO verification = signatureQueryService.verifyEvidence(signed.signatureId());
            assertEquals("VALID", verification.verificationStatus());
            assertEquals(verification.storedContentHash(), verification.calculatedContentHash());
            assertEquals(verification.storedEvidenceHash(), verification.calculatedEvidenceHash());
        }
    }

    @Test
    public void testQueryEvidence_acceptsDatabaseJsonFormattingWhenSemanticsMatch() {
        ElectronicSignatureCommand command = buildCommand("idem-json-format", "V1", "审批通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            ElectronicSignatureResult signed = signatureService.sign(command);
            signatureRecordMapper.updateById(new ElectronicSignatureRecordDO()
                    .setId(signed.signatureId())
                    .setCanonicalContentJson("{\"version\": \"V1\", \"name\": \"record\"}"));

            ElectronicSignatureVerificationDTO verification = signatureQueryService.verifyEvidence(signed.signatureId());
            assertEquals("VALID", verification.verificationStatus());
            assertEquals(verification.storedContentHash(), verification.calculatedContentHash());
            assertEquals(verification.storedEvidenceHash(), verification.calculatedEvidenceHash());
        }
    }

    @Test
    public void testSign_hashesJsonSemanticCanonicalForm() {
        ElectronicSignatureCommand command = buildCommand("idem-json-canonical", "V1", "审批通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            ElectronicSignatureResult result = signatureService.sign(command);

            assertEquals(
                    "3384dfd06702cb579f9488985d8fc899340d06626fc28f18a1cbf9acfe68ef2f",
                    result.contentHash());
        }
    }

    @Test
    public void testQueryEvidence_detectsContentTamperingWithoutRewritingHistory() {
        ElectronicSignatureCommand command = buildCommand("idem-007", "V1", "审批通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            ElectronicSignatureResult signed = signatureService.sign(command);
            signatureRecordMapper.updateById(new ElectronicSignatureRecordDO()
                    .setId(signed.signatureId())
                    .setCanonicalContentJson("{\"name\":\"tampered\",\"version\":\"V1\"}"));

            ElectronicSignatureVerificationDTO verification = signatureQueryService.verifyEvidence(signed.signatureId());
            assertEquals("MISMATCH", verification.verificationStatus());
            assertNotEquals(verification.storedContentHash(), verification.calculatedContentHash());
        }
    }

    @Test
    public void testSign_acceptsFullDccApprovalSubjectIdentityBeyondLegacyCapacity() {
        String fullDccSubjectId = "DCC_APPROVAL_CONTEXT:" + "A".repeat(900);
        ElectronicSignatureCommand command = buildCommand("idem-dcc-long-subject", fullDccSubjectId,
                "V1", "DCC 审批通过");

        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);

            ElectronicSignatureResult result = signatureService.sign(command);

            ElectronicSignatureRecordDO record = signatureRecordMapper.selectById(result.signatureId());
            assertEquals(fullDccSubjectId, record.getSubjectId());
            assertEquals(921, record.getSubjectId().length());
            assertEquals(1, signatureQueryService.listBySubject("TEST", "TEST_RECORD", fullDccSubjectId).size());
        }
    }

    @Test
    public void testComplianceReview_createsQuarterlySpecialAndEscalatesOverdue() {
        LocalDateTime plannedAt = LocalDateTime.of(2026, 9, 8, 9, 0);
        Long quarterlyId = complianceReviewService.createQuarterlyReview("ALL_MODULES", 101L, 102L,
                "ESIGN-SOP-2026Q3", "TRAINING-2026Q3", plannedAt, plannedAt.plusDays(14));
        Long specialId = complianceReviewService.createSpecialReview("MES", "KEY_ROTATION", 101L, 102L,
                "ESIGN-SOP-2026Q3", "TRAINING-2026Q3", plannedAt, plannedAt.plusDays(7));

        ElectronicSignatureComplianceReviewDO quarterly = complianceReviewMapper.selectById(quarterlyId);
        assertEquals("QUARTERLY", quarterly.getReviewType());
        assertEquals("ESIGN-SOP-2026Q3", quarterly.getSopVersion());
        assertEquals("TRAINING-2026Q3", quarterly.getTrainingEvidenceId());
        assertEquals("OPEN", quarterly.getStatus());

        complianceReviewService.markOverdueEscalated(specialId, 201L, plannedAt.plusDays(8));
        ElectronicSignatureComplianceReviewDO special = complianceReviewMapper.selectById(specialId);
        assertEquals("SPECIAL", special.getReviewType());
        assertEquals("ESCALATED", special.getStatus());
        assertEquals(201L, special.getEscalatedToUserId());
        assertNotNull(special.getEscalatedAt());
    }

    @Test
    public void testSealTimePrivilegedAuditAndRecoveryEvidence_recordsIndependentControls() {
        LocalDateTime observedAt = LocalDateTime.of(2026, 9, 8, 9, 0);
        Long timeEvidenceId = trustedTimeService.createTrustedTimeEvidence("NTP-PRIMARY",
                observedAt.minusNanos(500_000_000), observedAt);

        ElectronicSignatureTimeEvidenceDO timeEvidence = timeEvidenceMapper.selectById(timeEvidenceId);
        assertEquals("NTP-PRIMARY", timeEvidence.getTrustedTimeSource());
        assertEquals(500L, timeEvidence.getDriftMillis());
        assertEquals("TRUSTED", timeEvidence.getStatus());
        assertEquals(64, timeEvidence.getEvidenceHash().length());
        ServiceException driftException = assertThrows(ServiceException.class,
                () -> trustedTimeService.createTrustedTimeEvidence("NTP-PRIMARY",
                        observedAt.minusSeconds(2), observedAt));
        assertEquals(ESIGN_COMMAND_INVALID.getCode(), driftException.getCode());

        ElectronicSignatureCommand command = buildCommand("idem-008", "V1", "审批通过");
        try (MockedStatic<SecurityFrameworkUtils> mockedSecurity = mockStatic(SecurityFrameworkUtils.class)) {
            mockedSecurity.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            signatureService.sign(command);
        }

        Long sealId = sealService.createDailySeal(LocalDate.now(), "WORM-EVIDENCE-20260908");
        ElectronicSignatureSealDO seal = sealMapper.selectById(sealId);
        assertEquals("WORM-EVIDENCE-20260908", seal.getWormEvidenceId());
        assertEquals("SEALED", seal.getStatus());
        assertEquals(1, seal.getRecordCount());
        assertNull(seal.getPreviousSealHash());
        assertEquals(64, seal.getSealHash().length());

        Long privilegedAuditId = privilegedAuditService.recordPrivilegedAccess(201L,
                "ESIGN_EVIDENCE_EXPORT", "季度复核抽样导出");
        ElectronicSignaturePrivilegedAuditDO privilegedAudit = privilegedAuditMapper.selectById(privilegedAuditId);
        assertEquals(201L, privilegedAudit.getReviewerUserId());
        assertEquals("ESIGN_EVIDENCE_EXPORT", privilegedAudit.getOperationCode());
        assertEquals("RECORDED", privilegedAudit.getResultStatus());
        assertEquals(64, privilegedAudit.getEvidenceHash().length());

        String businessRecordHash = "a".repeat(64);
        String signatureRecordHash = "b".repeat(64);
        String snapshotHash = "c".repeat(64);
        Long recoveryId = archiveRecoveryService.recordRecoveryVerification(301L, 201L,
                businessRecordHash, signatureRecordHash, snapshotHash);
        ElectronicSignatureArchiveRecoveryDO recovery = archiveRecoveryMapper.selectById(recoveryId);
        assertEquals(301L, recovery.getArchiveId());
        assertEquals(201L, recovery.getRestoredBy());
        assertEquals("VERIFIED", recovery.getResultStatus());
        assertEquals(64, recovery.getRestoreEvidenceHash().length());
    }

    private ElectronicSignatureCommand buildCommand(String idempotencyKey, String expectedSubjectVersion, String reason) {
        return buildCommand(idempotencyKey, "R001", expectedSubjectVersion, reason);
    }

    private ElectronicSignatureCommand buildCommand(String idempotencyKey, String subjectId,
                                                    String expectedSubjectVersion, String reason) {
        return new ElectronicSignatureCommand("TEST", "APPROVE", "TEST_RECORD", subjectId,
                expectedSubjectVersion, "Signer@2026", reason, idempotencyKey, "2026-09-08T09:00:00", "Asia/Shanghai");
    }

    @TestConfiguration
    static class TestAdapterConfiguration {

        @Bean
        ElectronicSignatureSubjectAdapter testElectronicSignatureSubjectAdapter() {
            return new ElectronicSignatureSubjectAdapter() {

                @Override
                public String moduleCode() {
                    return "TEST";
                }

                @Override
                public Set<SignatureActionDefinition> supportedActions() {
                    return Set.of(new SignatureActionDefinition("TEST", "APPROVE", "TEST_RECORD",
                            "APPROVAL", "审批", "esign-policy-v1"));
                }

                @Override
                public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command) {
                    assertTrue(Set.of(101L, 202L).contains(command.actorId()));
                    return new SignatureSubjectSnapshot("TEST_RECORD", command.subjectId(), "V1",
                            "{\"name\":\"record\",\"version\":\"V1\"}",
                            null, null, null, "PI001", "TASK001", "APPROVE_NODE", 1);
                }
            };
        }
    }
}
