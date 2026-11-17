package cn.iocoder.yudao.module.dcc.signature.governance;

import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.SignatureGovernanceController;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordRespVO;
import cn.iocoder.yudao.module.dcc.dal.mysql.signature.governance.SignatureGovernanceRecordMapper;
import cn.iocoder.yudao.module.dcc.signature.service.records.SignatureGovernanceRecordServiceImpl;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import org.junit.jupiter.api.Test;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignatureGovernanceEvidenceDetailTest {
    final SignatureGovernanceRecordMapper mapper = mock(SignatureGovernanceRecordMapper.class);
    final ElectronicSignatureQueryService query = mock(ElectronicSignatureQueryService.class);
    final SignatureGovernanceRecordServiceImpl service = new SignatureGovernanceRecordServiceImpl(mapper, null, null, query);

    private void record() {
        var row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("UNIFIED-64344"); row.setSourceRecordId(64344L);
        row.setSourceTable("system_electronic_signature");
        when(mapper.selectSignatureRecordByGlobalId("UNIFIED-64344")).thenReturn(row);
    }
    private ElectronicSignatureEvidenceDTO evidence(Long id, String contentHash) {
        return new ElectronicSignatureEvidenceDTO(id, "MES", "TEAM_LEADER_REVIEW", "MES_BATCH_RECORD", "event-subject", "1",
                101L, "REVIEW", "复核", "复核意见", null, null, "PASSWORD", contentHash, "evidence-hash",
                "SHA256", "1", "1", "VALID", null, null, null, null, "{\"executionId\":\"0\"}", null, null, null, "复核人");
    }
    private ElectronicSignatureVerificationDTO verification(Long id, String status) {
        return new ElectronicSignatureVerificationDTO(id, status, "content-hash", "calculated", "evidence-hash", "calculated",
                "SHA256", "1", "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "DEVELOPMENT_DISABLED", List.of("GXP_AUDIT_DEVELOPMENT_DISABLED"), null, "V2");
    }
    @Test void exactReviewWithNoBusinessPathShowsOriginalEvidenceAndDiagnosticStatuses() {
        record();
        when(query.getById(64344L)).thenReturn(evidence(64344L, "content-hash"));
        for (String status : List.of("VALID", "INCOMPLETE", "MISMATCH")) {
            var diagnostic = verification(64344L, status);
            when(query.verifyEvidence(64344L)).thenReturn(diagnostic);
            var result = service.getEvidence("UNIFIED-64344");
            assertEquals(64344L, result.evidence().id()); assertSame(diagnostic, result.verification());
            assertEquals("NOT_BOUND", result.verification().timeTrustStatus());
        }
    }
    @Test void missingTenantScopedRecordCannotReadUnifiedEvidence() {
        var error = assertThrows(ServiceException.class, () -> service.getEvidence("UNIFIED-64344"));
        assertEquals(1_080_000_174, error.getCode());
        verifyNoInteractions(query);
    }
    @Test void rejectsWrongIdentityAndSnapshot() {
        record();
        when(query.verifyEvidence(64344L)).thenReturn(verification(64344L, "VALID"));
        when(query.getById(64344L)).thenReturn(evidence(64351L, "content-hash"));
        assertThrows(ServiceException.class, () -> service.getEvidence("UNIFIED-64344"));
        when(query.getById(64344L)).thenReturn(evidence(64344L, "changed-hash"));
        assertThrows(ServiceException.class, () -> service.getEvidence("UNIFIED-64344"));
        when(query.getById(64344L)).thenReturn(evidence(64344L, "content-hash"));
        when(query.verifyEvidence(64344L)).thenReturn(verification(64351L, "VALID"));
        assertThrows(ServiceException.class, () -> service.getEvidence("UNIFIED-64344"));
    }
    @Test void malformedAndNonUnifiedIdentitiesAreNotRerouted() {
        for (String id : List.of("UNIFIED-0", "UNIFIED--1", "FILE-64344", "UNIFIED-64344/other"))
            assertThrows(ServiceException.class, () -> service.getEvidence(id));
        verifyNoInteractions(mapper, query);
    }
    @Test void verificationFailureIsPropagated() {
        record(); when(query.getById(64344L)).thenReturn(evidence(64344L, "content-hash"));
        var failure = new IllegalArgumentException("FORMAT_UNSUPPORTED");
        when(query.verifyEvidence(64344L)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalArgumentException.class, () -> service.getEvidence("UNIFIED-64344")));
    }
    @Test void endpointUsesExistingGovernanceQueryPermission() throws Exception {
        var method = SignatureGovernanceController.class.getMethod("getSignatureRecordEvidence", String.class);
        assertEquals("@ss.hasPermission('signature-governance:policy:query')", method.getAnnotation(PreAuthorize.class).value());
    }
}
