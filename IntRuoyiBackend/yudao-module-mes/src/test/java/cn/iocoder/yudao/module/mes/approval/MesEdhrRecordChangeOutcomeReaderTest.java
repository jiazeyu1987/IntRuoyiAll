package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrRecordChangeApprovalEvidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrRecordChangeOutcomeReaderTest {
    @InjectMocks MesEdhrRecordChangeOutcomeReader reader;
    @Mock MesProEdhrRecordChangeEventMapper events;
    @Mock MesEdhrRecordChangeApprovalEvidence evidence;

    @Test void completedTaskWithoutFormalSignedDecisionNeverInventsApproval() {
        var task = new MesProEdhrWorkTaskDO().setId(1L).setTaskType("RECORD_CHANGE_APPROVE")
                .setBusinessScopeType("RECORD_CHANGE").setBusinessScopeId(7L).setStatus("DONE");
        when(events.selectOne(any())).thenReturn(new MesProEdhrRecordChangeEventDO().setId(7L).setChangeStatus("SUBMITTED"));
        var result = reader.read(task, 1L);
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult()); assertNull(result.actorUserId()); assertNull(result.approvalRemark());
        verifyNoInteractions(evidence);
    }

    @Test void completionActorMustMatchPersistedApprover() {
        var task = new MesProEdhrWorkTaskDO().setId(1L).setTaskType("RECORD_CHANGE_APPROVE")
                .setBusinessScopeType("RECORD_CHANGE").setBusinessScopeId(7L).setStatus("DONE").setAssigneeUserId(20L);
        when(events.selectOne(any())).thenReturn(new MesProEdhrRecordChangeEventDO().setId(7L)
                .setChangeStatus("EFFECTIVE").setApprovalSignatureId(99L).setApprovedBy(21L));
        assertThrows(IllegalStateException.class, () -> reader.read(task, 1L));
        verifyNoInteractions(evidence);
    }
    @Test void negativeDecisionRemainsReadableAfterCorrectionIsResubmitted() {
        var at = java.time.LocalDateTime.of(2026, 10, 28, 12, 0);
        var task = new MesProEdhrWorkTaskDO().setId(1L).setTaskType("RECORD_CHANGE_APPROVE")
                .setBusinessScopeType("RECORD_CHANGE").setBusinessScopeId(7L).setStatus("DONE")
                .setAssigneeUserId(20L).setSourceUserId(10L).setCompletedAt(at);
        var event = new MesProEdhrRecordChangeEventDO().setId(7L).setChangeStatus("RESUBMITTED")
                .setApprovalSignatureId(99L).setApprovedBy(20L).setApprovedAt(at).setRequestedBy(10L)
                .setDecisionReason("补充证据");
        when(events.selectOne(any())).thenReturn(event);
        var result = reader.read(task, 1L);
        assertEquals(cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult.REJECT, result.approvalResult());
        assertEquals("REJECT", result.decisionState());
        assertEquals("补充证据", result.approvalRemark());
        verify(evidence).requireRejected(event);
        verify(evidence, never()).requireApproved(any());
    }

}
