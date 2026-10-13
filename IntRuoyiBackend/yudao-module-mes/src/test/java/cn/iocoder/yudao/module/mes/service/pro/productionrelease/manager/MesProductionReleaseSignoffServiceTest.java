package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import cn.hutool.crypto.digest.DigestUtil;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Verification DTOs are consumer-port fixtures, not evidence of the creation-audit algorithm.
class MesProductionReleaseSignoffServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"valid", "applicationMissing", "applicationZero", "applicationNegative",
            "applicationDifferent", "taskMissing", "taskDifferent", "transactionMissing", "transactionDifferent",
            "batchMissing", "batchDifferent"})
    void persistedBindingRequiresOnePositiveFormalWorkOrderAcrossEveryOwner(String graph) {
        TenantContextHolder.setTenantId(1L);
        try {
            var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(701L).setActiveOrderId(801L)
                    .setWorkOrderId(301L).setReleaseApprovalWorkTaskId(2001L)
                    .setReleaseTransactionId(1001L).setBatchExecutionId(901L);
            application.setTenantId(1L);
            var task = new MesProEdhrWorkTaskDO().setId(2001L).setWorkOrderId(301L).setBatchExecutionId(901L)
                    .setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION")
                    .setBusinessScopeId(1001L).setCandidateSourceType("ROLE_GROUP")
                    .setResponsibilitySourceKey(MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
            var transaction = new MesProEdhrReleaseTransactionDO().setId(1001L).setBatchExecutionId(901L)
                    .setWorkOrderId(301L);
            var batch = new MesProEdhrBatchExecutionDO().setId(901L).setTenantId(1L).setWorkOrderId(301L);
            switch (graph) {
                case "applicationMissing" -> application.setWorkOrderId(null);
                case "applicationZero" -> application.setWorkOrderId(0L);
                case "applicationNegative" -> application.setWorkOrderId(-1L);
                case "applicationDifferent" -> application.setWorkOrderId(302L);
                case "taskMissing" -> task.setWorkOrderId(null);
                case "taskDifferent" -> task.setWorkOrderId(302L);
                case "transactionMissing" -> transaction.setWorkOrderId(null);
                case "transactionDifferent" -> transaction.setWorkOrderId(302L);
                case "batchMissing" -> batch.setWorkOrderId(null);
                case "batchDifferent" -> batch.setWorkOrderId(302L);
            }
            Long originalApplicationWorkOrder = application.getWorkOrderId();
            if ("valid".equals(graph)) {
                assertDoesNotThrow(() -> MesProductionReleaseSignoffService.requireBinding(task, application, transaction, batch));
            } else {
                IllegalStateException failure = assertThrows(IllegalStateException.class,
                        () -> MesProductionReleaseSignoffService.requireBinding(task, application, transaction, batch));
                assertEquals("EDHR_MARKET_RELEASE_SIGNATURE_BINDING_INVALID", failure.getMessage());
            }
            assertEquals(originalApplicationWorkOrder, application.getWorkOrderId());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void verifiesPersistedSignatureAndRejectsWrongTaskActorAndTamperedEvidence() {
        var query = mock(ElectronicSignatureQueryService.class);
        var signature = mock(ElectronicSignatureEvidenceDTO.class);
        String imageHash = "a".repeat(64);
        String subject = Base64.getUrlEncoder().withoutPadding().encodeToString(("EDHR\nEDHR_WORK_TASK\n3\n3\n\n" + imageHash).getBytes(StandardCharsets.UTF_8));
        when(query.listBySubject("BPM", "BPM_APPROVAL_TASK", subject)).thenReturn(List.of(signature));
        when(signature.id()).thenReturn(10L);
        when(signature.signedAt()).thenReturn(java.time.LocalDateTime.of(2026,10,4,10,0));
        when(signature.contentHash()).thenReturn("content");
        when(signature.subjectVersion()).thenReturn(DigestUtil.sha256Hex(subject));
        when(signature.actorId()).thenReturn(9L);
        when(signature.taskId()).thenReturn("3");
        when(signature.moduleCode()).thenReturn("BPM");
        when(signature.subjectType()).thenReturn("BPM_APPROVAL_TASK");
        when(signature.subjectId()).thenReturn(subject);
        when(signature.actionCode()).thenReturn("APPROVE");
        when(signature.authenticationMethod()).thenReturn("SESSION_PLUS_PASSWORD");
        when(signature.verificationStatus()).thenReturn("VALID");
        when(signature.evidenceHash()).thenReturn("hash");
        when(signature.reason()).thenReturn("approved");
        when(signature.canonicalContentJson()).thenReturn("{\"adapter\":\"BPM_APPROVAL\",\"moduleCode\":\"EDHR\",\"sourceTaskType\":\"EDHR_WORK_TASK\",\"sourceTaskId\":\"3\",\"businessKey\":\"3\",\"processInstanceId\":\"\",\"reviewResult\":\"APPROVE\",\"reason\":\"approved\",\"signatureImagePayloadHash\":\""+imageHash+"\"}");
        when(query.verifyEvidence(10L)).thenReturn(new ElectronicSignatureVerificationDTO(10L, "VALID", "content", "content", "hash", "hash", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "CONSISTENT", List.of(), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
        var service = new MesProductionReleaseSignoffService(query);
        assertTrue(service.isVerified(3L, 9L, subject, "hash", "approved"));
        assertEquals(Optional.of(10L),
                service.findVerifiedSignatureId(3L, 9L, subject, "hash", "approved"));
        assertFalse(service.isVerified(4L, 9L, subject, "hash", "approved"));
        assertEquals(Optional.empty(),
                service.findVerifiedSignatureId(4L, 9L, subject, "hash", "approved"));
        assertFalse(service.isVerified(3L, 8L, subject, "hash", "approved"));
        assertFalse(service.isVerified(3L, 9L, subject, "hash", "changed"));
        when(query.verifyEvidence(10L)).thenReturn(new ElectronicSignatureVerificationDTO(10L, "MISMATCH", "", "", "hash", "changed", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "MISMATCH", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "CONSISTENT", List.of("DIGEST_MISMATCH"), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
        assertFalse(service.isVerified(3L, 9L, subject, "hash", "approved"));
    }
}
