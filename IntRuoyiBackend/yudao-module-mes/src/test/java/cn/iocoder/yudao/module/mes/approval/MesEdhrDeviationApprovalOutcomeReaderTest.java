package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationHandlingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationHandlingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrDeviationApprovalOutcomeReaderTest {
    @Mock MesProEdhrDeviationMapper deviations;
    @Mock MesProEdhrDeviationHandlingMapper handlings;
    @Mock ElectronicSignatureQueryService signatures;
    MesEdhrDeviationApprovalOutcomeReader reader;
    static final LocalDateTime AT = LocalDateTime.of(2026, 10, 28, 10, 0);

    @BeforeEach void setUp() {
        reader = new MesEdhrDeviationApprovalOutcomeReader();
        ReflectionTestUtils.setField(reader, "deviations", deviations);
        ReflectionTestUtils.setField(reader, "handlings", handlings);
        ReflectionTestUtils.setField(reader, "signatures", signatures);
    }

    @Test void completedTaskWithoutExactSignatureReceiptRemainsUnknown() {
        var result = reader.read(task().setResponsibilityScopeJson(null), 71L);
        assertEquals("COMPLETED", result.taskOutcome());
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult());
        verifyNoInteractions(deviations, handlings, signatures);
    }

    @Test void malformedOrIncompleteReceiptCannotBecomeApproval() {
        for (String receipt : List.of("not-json", "{\"signatureId\":\"missing\"}", "{}")) {
            assertEquals("UNKNOWN", reader.read(task().setResponsibilityScopeJson(receipt), 71L).decisionState());
        }
        verifyNoInteractions(deviations, handlings, signatures);
    }

    @Test void currentSignedQaResponsibilityIsProvenByItsExactReceiptAndEvidence() {
        current();
        when(signatures.getById(9001L)).thenReturn(evidence(201L));
        when(signatures.verifyEvidence(9001L)).thenReturn(verified("VALID"));
        var result = reader.read(task(), 71L);
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.approvalResult());
        assertEquals(201L, result.actorUserId());
        assertEquals("已核对调查、纠正措施与验证证据", result.approvalRemark());
        assertEquals("9001", result.evidenceId());
    }

    @Test void earlierRevisionCannotBePresentedAsCurrentApproval() {
        current();
        when(handlings.selectByTenantAndDeviationId(71L, 901L)).thenReturn(handling().setContentVersion(2).setContentHash("new-hash"));
        var result = reader.read(task(), 71L);
        assertEquals("UNKNOWN", result.decisionState());
        assertEquals("SUPERSEDED", result.businessStatus());
        verifyNoInteractions(signatures);
    }

    @Test void anotherActorsValidSignatureDoesNotProveAssignedPersonsDecision() {
        current();
        when(signatures.getById(9001L)).thenReturn(evidence(202L));
        var result = reader.read(task(), 71L);
        assertNull(result.approvalResult());
        verify(signatures, never()).verifyEvidence(9001L);
    }

    @Test void failedEvidenceVerificationRemainsUnknown() {
        current();
        when(signatures.getById(9001L)).thenReturn(evidence(201L));
        when(signatures.verifyEvidence(9001L)).thenReturn(verified("INVALID"));
        assertNull(reader.read(task(), 71L).approvalResult());
    }

    @Test void ordinarySignatureTaskNeverBecomesApproval() {
        var result = reader.read(task().setTaskType("DEVIATION_SIGN"), 71L);
        assertNull(result.approvalResult());
        verifyNoInteractions(deviations, handlings, signatures);
    }

    private void current() {
        when(deviations.selectByTenantAndId(71L, 901L)).thenReturn(new MesProEdhrDeviationDO()
                .setId(901L).setTenantId(71L).setDeviationCode("PC-202610-0001").setStatus("OPEN"));
        when(handlings.selectByTenantAndDeviationId(71L, 901L)).thenReturn(handling());
    }
    private MesProEdhrDeviationHandlingDO handling() {
        return new MesProEdhrDeviationHandlingDO().setId(7701L).setTenantId(71L).setDeviationId(901L)
                .setContentVersion(1).setContentHash("hash-1").setReviewerAssignmentsJson("{\"QA\":201}");
    }
    private MesProEdhrWorkTaskDO task() {
        return new MesProEdhrWorkTaskDO().setId(6001L).setStatus("DONE").setTaskType("DEVIATION_APPROVE")
                .setAssigneeUserId(201L).setBusinessScopeType("EDHR_DEVIATION").setBusinessScopeId(901L)
                .setReviewSourceType("EDHR_DEVIATION_HANDLING").setReviewSourceId(7701L)
                .setSignatureCellKey("QA").setResponsibilitySourceVersion("1").setResponsibilitySourceDigest("hash-1")
                .setCompletedAt(AT.plusSeconds(1)).setResponsibilityScopeJson(JSON.toJSONString(Map.of(
                        "signatureId", 9001L, "actorUserId", 201L, "handlingId", 7701L, "node", "QA",
                        "contentVersion", "1", "contentHash", "hash-1", "userOpinion", "已核对调查、纠正措施与验证证据")));
    }
    private ElectronicSignatureEvidenceDTO evidence(Long actor) {
        String action = MesProBatchRecordExecutionSignatureService.ACTION_DEVIATION_HANDLING_QA;
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action,
                null, null, null, null, null, null, null, "EDHR_DEVIATION_HANDLING", 7701L,
                "偏差 PC-202610-0001 处理节点 QA", "QA", 1L, "hash-1", "hash-1", null);
        return new ElectronicSignatureEvidenceDTO(9001L, "MES", action, "MES_BATCH_RECORD", subject,
                MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), actor, "QA", "QA关闭确认",
                "已核对调查、纠正措施与验证证据", AT, "time-evidence", "SESSION_PLUS_PASSWORD", "content", "evidence",
                "SHA-256", "v1", "p1", "VALID", null, null, null, null,
                MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, action, "已核对调查、纠正措施与验证证据"),
                null, null, null, "QA用户");
    }
    private ElectronicSignatureVerificationDTO verified(String status) {
        return new ElectronicSignatureVerificationDTO(9001L, status, "content", "content", "evidence", "evidence",
                "SHA-256", "v1", "FULL", "VALID", "VALID", "SERVER", "VALID", "VALID", List.of(), AT, "V1");
    }
}
