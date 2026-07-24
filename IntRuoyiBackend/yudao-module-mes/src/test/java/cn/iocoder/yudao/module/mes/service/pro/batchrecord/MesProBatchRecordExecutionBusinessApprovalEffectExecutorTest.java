package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalEffectResult;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalRequest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordApprovalSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordApprovalSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordExecutionBusinessApprovalEffectExecutorTest {

    private MesProBatchRecordExecutionBusinessApprovalEffectExecutor executor;

    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @Mock
    private MesProBatchRecordApprovalSnapshotMapper approvalSnapshotMapper;
    @Mock
    private MesProBatchRecordExecutionSignatureService executionSignatureService;
    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @Mock
    private cn.iocoder.yudao.module.bpm.service.task.BpmTaskService bpmTaskService;

    @BeforeEach
    void setUp() {
        executor = new MesProBatchRecordExecutionBusinessApprovalEffectExecutor();
        ReflectionTestUtils.setField(executor, "executionMapper", executionMapper);
        ReflectionTestUtils.setField(executor, "approvalSnapshotMapper", approvalSnapshotMapper);
        ReflectionTestUtils.setField(executor, "executionSignatureService", executionSignatureService);
        ReflectionTestUtils.setField(executor, "workTaskService", workTaskService);
        ReflectionTestUtils.setField(executor, "bpmTaskService", bpmTaskService);
    }

    @Test
    void executorCodeIsStablePlatformCode() {
        assertEquals("EDHR_BATCH_EXECUTION_SUBMIT_REVIEW", executor.getExecutorCode());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void markPendingAttachesBpmProcessAndCreatesReviewTasks() {
        when(executionMapper.selectByIdForUpdate(9001L)).thenReturn(new MesProBatchRecordExecutionDO()
                .setId(9001L)
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT));
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-9001");
        when(task.getAssignee()).thenReturn("88");
        when(bpmTaskService.getRunningTaskListByProcessInstanceId("process-9001", null, "approveNode"))
                .thenReturn(List.of(task));

        BusinessApprovalEffectResult result = executor.markPending(context(), request("process-9001"));

        assertEquals("SUBMITTED", result.getResultState());
        verify(executionSignatureService).attachSubmitSignatureProcessInstance(1301L, 9001L, "process-9001");
        verify(executionSignatureService).bindSignatureFieldAuditEvidence(1301L, 9001L,
                7L, "field-head-hash", "cell-values-hash");
        ArgumentCaptor<MesProBatchRecordApprovalSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordApprovalSnapshotDO.class);
        verify(approvalSnapshotMapper).insert(snapshotCaptor.capture());
        assertEquals(9001L, snapshotCaptor.getValue().getExecutionId());
        assertEquals("mes-edhr-approval-v1", snapshotCaptor.getValue().getProcessDefinitionKey());
        assertEquals("process-9001", snapshotCaptor.getValue().getProcessInstanceId());
        assertEquals("SUBMITTED", snapshotCaptor.getValue().getApprovalStatus());
        assertEquals("task-9001", snapshotCaptor.getValue().getCurrentBpmTaskId());
        assertEquals(1301L, snapshotCaptor.getValue().getSubmitSignatureId());
        ArgumentCaptor<MesProBatchRecordExecutionDO> executionCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionDO.class);
        verify(executionMapper).updateById(executionCaptor.capture());
        assertEquals(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_SUBMITTED,
                executionCaptor.getValue().getStatus());
        assertEquals("process-9001", executionCaptor.getValue().getProcessInstanceId());
        ArgumentCaptor<List> reviewTaskCaptor = ArgumentCaptor.forClass(List.class);
        verify(workTaskService).createReviewTasks(eq(8001L), eq(9001L), reviewTaskCaptor.capture());
        MesProEdhrReviewTaskCreateCommand command =
                (MesProEdhrReviewTaskCreateCommand) reviewTaskCaptor.getValue().get(0);
        assertEquals("R1C1", command.getSignatureCellKey());
        assertEquals(88L, command.getAssigneeUserId());
        assertEquals("task-9001", command.getBpmTaskId());
    }

    @Test
    void directExecutionApprovesExecutionWithoutBpmReviewTasks() {
        when(executionMapper.selectByIdForUpdate(9001L)).thenReturn(new MesProBatchRecordExecutionDO()
                .setId(9001L)
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_DRAFT));
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);

        BusinessApprovalEffectResult result = executor.executeDirect(context(), request(null));

        assertEquals("APPROVED", result.getResultState());
        verify(executionSignatureService, never()).attachSubmitSignatureProcessInstance(any(), any(), any());
        verify(approvalSnapshotMapper).insert(any(MesProBatchRecordApprovalSnapshotDO.class));
        ArgumentCaptor<MesProBatchRecordExecutionDO> executionCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionDO.class);
        verify(executionMapper).updateById(executionCaptor.capture());
        assertEquals(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED,
                executionCaptor.getValue().getStatus());
        verify(workTaskService, never()).createReviewTasks(any(), any(), any());
        verify(workTaskService).completeFillAndCreateNextFillAfterOrdinarySubmit(8001L, 9001L);
    }

    @Test
    void executeApprovedClosesSnapshotExecutionAndReviewTask() {
        when(executionMapper.selectByIdForUpdate(9001L)).thenReturn(new MesProBatchRecordExecutionDO()
                .setId(9001L)
                .setStatus(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_SUBMITTED)
                .setProcessInstanceId("process-9001"));
        when(approvalSnapshotMapper.selectByExecutionId(9001L)).thenReturn(MesProBatchRecordApprovalSnapshotDO.builder()
                .id(7001L)
                .executionId(9001L)
                .processInstanceId("process-9001")
                .approvalStatus("SUBMITTED")
                .currentBpmTaskId("task-9001")
                .build());
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);
        when(approvalSnapshotMapper.approveAndClearCurrentBpmTask(any(MesProBatchRecordApprovalSnapshotDO.class)))
                .thenReturn(1);
        MesProEdhrWorkTaskDO reviewTask = new MesProEdhrWorkTaskDO()
                .setId(8002L)
                .setExecutionId(9001L)
                .setBpmTaskId("task-9001");
        when(workTaskService.getActiveReviewTaskByBpmTaskId(9001L, "task-9001")).thenReturn(reviewTask);
        when(workTaskService.completeOneReviewTask(8002L, 9001L)).thenReturn(reviewTask);

        BusinessApprovalEffectResult result = executor.executeApproved(context(), request("process-9001"), 88L);

        assertEquals("APPROVED", result.getResultState());
        ArgumentCaptor<MesProBatchRecordApprovalSnapshotDO> snapshotCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordApprovalSnapshotDO.class);
        verify(approvalSnapshotMapper).approveAndClearCurrentBpmTask(snapshotCaptor.capture());
        assertEquals(7001L, snapshotCaptor.getValue().getId());
        assertEquals("APPROVED", snapshotCaptor.getValue().getApprovalStatus());
        assertEquals(88L, snapshotCaptor.getValue().getApprovedBy());
        assertNotNull(snapshotCaptor.getValue().getApprovedAt());
        assertNotNull(snapshotCaptor.getValue().getClosedAt());
        ArgumentCaptor<MesProBatchRecordExecutionDO> executionCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionDO.class);
        verify(executionMapper).updateById(executionCaptor.capture());
        assertEquals(MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED,
                executionCaptor.getValue().getStatus());
        assertEquals(88L, executionCaptor.getValue().getApprovedBy());
        assertNotNull(executionCaptor.getValue().getApprovedAt());
        assertNotNull(executionCaptor.getValue().getClosedAt());
        verify(executionMapper).clearActiveContextKey(9001L);
        verify(workTaskService).completeOneReviewTask(8002L, 9001L);
        verify(workTaskService).createNextFillAfterReview(reviewTask);
    }

    private BusinessApprovalContext context() {
        return BusinessApprovalContext.builder()
                .tenantId(122L)
                .dataDomain("MES")
                .systemCode("MES")
                .objectType("EDHR_BATCH_EXECUTION")
                .objectId("9001")
                .objectVersion("1")
                .actionCode("SUBMIT_REVIEW")
                .objectState("DRAFT")
                .applicantUserId(99L)
                .reason("submit review")
                .variables(Map.ofEntries(
                        entry("workTaskId", 8001L),
                        entry("processDefinitionKey", "mes-edhr-approval-v1"),
                        entry("approvalTaskDefinitionKey", "approveNode"),
                        entry("submitSignatureId", 1301L),
                        entry("submittedBy", 99L),
                        entry("submittedAt", "2026-07-21T10:15:30"),
                        entry("approvalSnapshotJson", "{\"executionId\":9001}"),
                        entry("approvalSnapshotHash", "approval-snapshot-hash"),
                        entry("fieldAuditRevision", 7L),
                        entry("fieldAuditHeadHash", "field-head-hash"),
                        entry("cellValuesHash", "cell-values-hash"),
                        entry("edhrReviewSignatureCells", """
                                [{"signatureCellKey":"R1C1","signatureRowIndex":1,"signatureColumnIndex":1,
                                "reviewSourceType":"POST","reviewSourceId":7001,"reviewSourceName":"QA",
                                "candidateUserIds":[88],"assigneeUserId":88,"assigneeUserName":"Reviewer"}]
                                """)))
                .build();
    }

    private BusinessApprovalRequest request(String processInstanceId) {
        return BusinessApprovalRequest.builder()
                .requestId(91001L)
                .processInstanceId(processInstanceId)
                .context(context())
                .build();
    }
}
