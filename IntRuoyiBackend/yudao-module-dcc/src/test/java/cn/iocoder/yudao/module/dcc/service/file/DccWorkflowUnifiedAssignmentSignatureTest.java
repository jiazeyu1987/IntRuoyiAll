package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccWorkflowUnifiedAssignmentSignatureTest {
    @Test
    void assignmentSignsThroughRealUnifiedServiceAndRegisteredDccAdapter() {
        var unified = new ElectronicSignatureServiceImpl();
        var users = mock(AdminUserApi.class);
        var records = mock(ElectronicSignatureRecordMapper.class);
        var audit = mock(GxpAuditService.class);
        ReflectionTestUtils.setField(unified, "adminUserApi", users);
        ReflectionTestUtils.setField(unified, "signatureRecordMapper", records);
        ReflectionTestUtils.setField(unified, "gxpAuditService", audit);
        ReflectionTestUtils.setField(unified, "subjectAdapters", List.of(new DccControlledFileSignatureSubjectAdapter()));
        when(records.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call -> {
            call.<ElectronicSignatureRecordDO>getArgument(0).setId(71L);
            return 1;
        });
        var evidence = DccControlledFileSignatureEvidence.builder().revisionId(10L).versionNo("A/1")
                .recordHashSnapshot("source-hash").sourceFileHash("source-hash")
                .evidenceHash("domain-hmac").canonicalPayload("frozen-domain-payload").build();
        String subject = DccControlledFileSignatureSubjectAdapter.encodeSubjectId(10L, "assignment-task", "MATRIX_REVIEW",
                "ASSIGN", "MATRIX_REVIEW_ASSIGN", evidence);
        TenantContextHolder.setTenantId(1L);
        try (var login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var result = unified.sign(new ElectronicSignatureCommand("DCC", "ASSIGN", "DCC_CONTROLLED_FILE", subject,
                    DccControlledFileSignatureSubjectAdapter.subjectVersion(subject), "test-credential", "指派本人",
                    "assignment-command-1", null, null));
            assertEquals(71L, result.signatureId());
            var saved = ArgumentCaptor.forClass(ElectronicSignatureRecordDO.class);
            verify(records).insert(saved.capture());
            assertEquals("ASSIGN", saved.getValue().getActionCode());
            assertEquals("ASSIGN", saved.getValue().getMeaningCode());
            assertNotNull(saved.getValue().getEvidenceHash());
            verify(users).reauthenticateForSignature(99L, "test-credential");
            verify(audit).append(any());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void assignmentDomainVerifierMustNormalizeToTheSignedAssignedOutcome() {
        assertEquals("ASSIGNED", ReflectionTestUtils.invokeMethod(new DccElectronicSignatureManagementServiceImpl(),
                "normalizeTaskActionResult", "ASSIGN"));
    }

    @Test
    void workflowSubjectCarriesItsFrozenRoundAndRejectsActionOrVersionSubstitution() {
        var adapter=new DccControlledFileSignatureSubjectAdapter();
        var evidence=DccControlledFileSignatureEvidence.builder().revisionId(10L).versionNo("A/1")
                .processInstanceId("obsolete-round-2").evidencePayloadVersion("v4-workflow")
                .recordHashSnapshot("source-hash").sourceFileHash("source-hash").evidenceHash("domain-hmac")
                .canonicalPayload("actual-round-payload").build();
        String subject=DccControlledFileSignatureSubjectAdapter.encodeSubjectId(10L,"obsolete-task","MATRIX_REVIEW",
                "ASSIGN","MATRIX_REVIEW_ASSIGN",evidence);
        String version=DccControlledFileSignatureSubjectAdapter.subjectVersion(subject);
        var snapshot=adapter.loadAndAuthorize(new SignatureSubjectCommand(99L,"DCC","ASSIGN","DCC_CONTROLLED_FILE",
                subject,version,"指派本人"));
        assertEquals("obsolete-round-2",snapshot.processInstanceId());
        assertTrue(snapshot.canonicalContentJson().contains("\"processInstanceId\":\"obsolete-round-2\""));
        assertThrows(IllegalArgumentException.class,()->adapter.loadAndAuthorize(new SignatureSubjectCommand(
                99L,"DCC","APPROVE","DCC_CONTROLLED_FILE",subject,version,"同意")));
        assertThrows(IllegalArgumentException.class,()->adapter.loadAndAuthorize(new SignatureSubjectCommand(
                99L,"DCC","ASSIGN","DCC_CONTROLLED_FILE",subject,"changed-subject-version","指派本人")));
    }
    @Test
    void workflowSignatureNodeOrderMatchesMatrixThenApprovalThenDocumentControl() {
        var adapter=new DccControlledFileSignatureSubjectAdapter();
        var evidence=DccControlledFileSignatureEvidence.builder().revisionId(10L).versionNo("A/1")
                .processInstanceId("workflow-round").evidencePayloadVersion("v4-workflow")
                .recordHashSnapshot("source").sourceFileHash("source").evidenceHash("hmac").canonicalPayload("payload").build();
        java.util.List<Integer> orders=new java.util.ArrayList<>();
        for(String stage:java.util.List.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")) {
            String subject=DccControlledFileSignatureSubjectAdapter.encodeSubjectId(10L,stage+"-task",stage,
                    "APPROVE",stage+"_APPROVE",evidence);
            orders.add(adapter.loadAndAuthorize(new SignatureSubjectCommand(99L,"DCC","APPROVE","DCC_CONTROLLED_FILE",
                    subject,DccControlledFileSignatureSubjectAdapter.subjectVersion(subject),"同意")).nodeOrder());
        }
        assertTrue(orders.get(0)<orders.get(1));
        assertTrue(orders.get(1)<orders.get(2),"new workflow cannot show document control ahead of completed matrix approval");
    }
}
