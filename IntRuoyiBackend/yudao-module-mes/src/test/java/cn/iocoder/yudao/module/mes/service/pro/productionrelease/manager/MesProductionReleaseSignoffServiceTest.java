package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import cn.hutool.crypto.digest.DigestUtil;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProductionReleaseSignoffServiceTest {
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
        when(query.verifyEvidence(10L)).thenReturn(new ElectronicSignatureVerificationDTO(10L, "VALID", "content", "content", "hash", "hash", "SHA-256", "v1"));
        var service = new MesProductionReleaseSignoffService(query);
        assertTrue(service.isVerified(3L, 9L, subject, "hash", "approved"));
        assertEquals(Optional.of(10L),
                service.findVerifiedSignatureId(3L, 9L, subject, "hash", "approved"));
        assertFalse(service.isVerified(4L, 9L, subject, "hash", "approved"));
        assertEquals(Optional.empty(),
                service.findVerifiedSignatureId(4L, 9L, subject, "hash", "approved"));
        assertFalse(service.isVerified(3L, 8L, subject, "hash", "approved"));
        assertFalse(service.isVerified(3L, 9L, subject, "hash", "changed"));
        when(query.verifyEvidence(10L)).thenReturn(new ElectronicSignatureVerificationDTO(10L, "MISMATCH", "", "", "hash", "changed", "SHA-256", "v1"));
        assertFalse(service.isVerified(3L, 9L, subject, "hash", "approved"));
    }
}
