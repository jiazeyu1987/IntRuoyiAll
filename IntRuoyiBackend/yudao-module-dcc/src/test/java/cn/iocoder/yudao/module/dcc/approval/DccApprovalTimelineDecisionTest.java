package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.ZoneId;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccApprovalTimelineDecisionTest {
    @ParameterizedTest
    @CsvSource({"2,SUPERVISOR_REVIEW,APPROVED,审核通过", "2,MATRIX_REVIEW,APPROVED,审核通过",
        "2,MATRIX_APPROVAL,APPROVED,批准通过", "2,DOC_CONTROL_APPROVAL,APPROVED,批准通过",
        "3,MATRIX_APPROVAL,REJECTED,驳回", "4,MATRIX_REVIEW,CANCELLED,取消", "5,MATRIX_REVIEW,RETURNED,退回",
        "1,MATRIX_REVIEW,CURRENT,处理中", "7,MATRIX_APPROVAL,CURRENT,处理中",
        "2,DELAYED_EFFECTIVE_CONFIRM,APPROVED,延迟生效已确认"})
    void usesDecisionAndNodeInsteadOfEndTime(int status,String node,String action,String label) {
        var task=mock(HistoricTaskInstance.class);
        when(task.getTaskLocalVariables()).thenReturn(Map.of(BpmnVariableConstants.TASK_VARIABLE_STATUS,status));
        when(task.getTaskDefinitionKey()).thenReturn(node);when(task.getEndTime()).thenReturn(new Date());
        ApprovalTaskTimelineEntry result=ReflectionTestUtils.invokeMethod(mock(DccApprovalTaskAdapter.class, CALLS_REAL_METHODS),"toTimelineEntry",task,"20");
        assertEquals(action,result.getAction());assertEquals(label,result.getActionLabel());
        assertEquals("CURRENT".equals(action)?"RUNNING":"DONE",result.getStatus());
    }
    @Test void endedTaskWithoutDecisionIsNotReportedAsApproved() {
        var task=mock(HistoricTaskInstance.class);
        when(task.getTaskLocalVariables()).thenReturn(Map.of());when(task.getEndTime()).thenReturn(new Date());
        assertThrows(IllegalStateException.class,()->ReflectionTestUtils.invokeMethod(mock(DccApprovalTaskAdapter.class, CALLS_REAL_METHODS),"toTimelineEntry",task,"20"));
    }

    private final LocalDateTime created = LocalDateTime.of(2026, 10, 27, 11, 0);
    private final LocalDateTime assigned = created.plusMinutes(2);

    private HistoricTaskInstance reviewTask(String action, String actor) {
        var task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("review-1");
        when(task.getProcessInstanceId()).thenReturn("round-1");
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getAssignee()).thenReturn(actor);
        when(task.getCreateTime()).thenReturn(date(created));
        when(task.getTaskLocalVariables()).thenReturn(Map.of(BpmnVariableConstants.TASK_VARIABLE_STATUS,
                "APPROVED".equals(action) ? 2 : "REJECTED".equals(action) ? 3 : 1));
        if (!"CURRENT".equals(action)) when(task.getEndTime()).thenReturn(date(assigned.plusMinutes(3)));
        return task;
    }

    private static Date date(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    private DccControlledFileSignatureDO assignmentSignature() {
        return DccControlledFileSignatureDO.builder().id(100L).controlledFileId(20L).taskId("review-1")
                .processInstanceId("round-1").actorId(10L).actionType("ASSIGN").meaningCode("MATRIX_REVIEW_ASSIGN")
                .signedAt(assigned).evidenceStatus("VALID").build();
    }

    private List<ApprovalTaskTimelineEntry> timeline(HistoricTaskInstance task, List<DccControlledFileSignatureDO> signatures) {
        ApprovalTaskTimelineEntry base = ReflectionTestUtils.invokeMethod(
                mock(DccApprovalTaskAdapter.class, CALLS_REAL_METHODS), "toTimelineEntry", task, "20");
        return DccApprovalTaskAdapter.signoffTimelineEntries(task, base, signatures);
    }

    @Test void unassignedTaskShowsOnlyLeaderStageAndDoesNotInventActualReview() {
        var rows = timeline(reviewTask("CURRENT", "10"), List.of());
        assertEquals(1, rows.size());
        assertEquals("部门负责人签名指派", rows.get(0).getNodeName());
        assertEquals("MATRIX_REVIEW_ASSIGN", rows.get(0).getNodeCode());
        assertEquals("CURRENT", rows.get(0).getAction());
    }

    @Test void assignedTaskShowsCompletedLeaderAndRunningActualSignerWithSeparateActorsAndTimes() {
        var rows = timeline(reviewTask("CURRENT", "11"), List.of(assignmentSignature()));
        assertEquals(2, rows.size());
        assertNotEquals(rows.get(0).getId(), rows.get(1).getId());
        assertEquals("指派完成", rows.get(0).getActionLabel());
        assertEquals(10L, rows.get(0).getActorUserId());
        assertEquals(assigned, rows.get(0).getActedAt());
        assertEquals("DCC_SIGNATURE", rows.get(0).getEvidenceType());
        assertEquals("100", rows.get(0).getDomainReferenceId());
        assertEquals("实际会签人会签", rows.get(1).getNodeName());
        assertEquals(11L, rows.get(1).getActorUserId());
        assertEquals(assigned, rows.get(1).getActedAt());
        assertEquals("CURRENT", rows.get(1).getAction());
    }

    @ParameterizedTest @CsvSource({"APPROVED", "REJECTED"})
    void completedReviewRetainsIndependentAssignmentAndActualReviewResult(String action) {
        var rows = timeline(reviewTask(action, "11"), List.of(assignmentSignature()));
        assertEquals("APPROVED", rows.get(0).getAction());
        assertEquals(action, rows.get(1).getAction());
        assertEquals(assigned.plusMinutes(3), rows.get(1).getActedAt());
    }

    @Test void selfAssignmentStillProducesTwoIndependentStages() {
        var rows = timeline(reviewTask("APPROVED", "10"), List.of(assignmentSignature()));
        assertEquals(2, rows.size());
        assertEquals(rows.get(0).getActorUserId(), rows.get(1).getActorUserId());
        assertNotEquals(rows.get(0).getNodeCode(), rows.get(1).getNodeCode());
    }

    @Test void leaderRejectionBeforeAssignmentDoesNotProduceCompletedAssignmentOrActualReview() {
        var rows = timeline(reviewTask("REJECTED", "10"), List.of());
        assertEquals(1, rows.size());
        assertEquals("部门负责人签名指派", rows.get(0).getNodeName());
        assertEquals("REJECTED", rows.get(0).getAction());
    }

    @Test void signaturesFromOtherRoundsFilesOrDepartmentsCannotCompleteThisAssignment() {
        var otherRound = assignmentSignature(); otherRound.setProcessInstanceId("old-round");
        var otherTask = assignmentSignature(); otherTask.setTaskId("other-department-task");
        var otherFile = assignmentSignature(); otherFile.setControlledFileId(21L);
        var rows = timeline(reviewTask("CURRENT", "10"), List.of(otherRound, otherTask, otherFile));
        assertEquals(1, rows.size());
        assertEquals("CURRENT", rows.get(0).getAction());
    }

    @Test void missingInvalidOrDuplicatedAssignmentEvidenceIsReportedInsteadOfInventingSuccess() {
        assertThrows(IllegalStateException.class, () -> timeline(reviewTask("APPROVED", "11"), List.of()));
        var signature = assignmentSignature(); signature.setEvidenceStatus("INVALID");
        assertThrows(IllegalStateException.class, () -> timeline(reviewTask("CURRENT", "11"), List.of(signature)));
        assertThrows(IllegalStateException.class, () -> timeline(reviewTask("CURRENT", "11"),
                List.of(assignmentSignature(), assignmentSignature())));
    }

    @Test void todoNameUsesPersistedAssignmentStateAndLeavesOtherWorkflowsUntouched() {
        var task = reviewTask("CURRENT", "10");
        assertEquals("部门负责人签名指派", DccApprovalTaskAdapter.businessTaskName(task, "dcc-controlled-file-upload"));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("dccAssignmentSignatureId", 100L));
        assertEquals("实际会签人会签", DccApprovalTaskAdapter.businessTaskName(task, "dcc-controlled-file-upload"));
        when(task.getName()).thenReturn("历史审批");
        assertEquals("历史审批", DccApprovalTaskAdapter.businessTaskName(task, "dcc-controlled-file-approval"));
    }

    private DccApprovalTaskAdapter adapterForTimeline(String processKey) {
        var adapter = mock(DccApprovalTaskAdapter.class, CALLS_REAL_METHODS);
        var tasks = mock(BpmTaskService.class);
        var processes = mock(BpmProcessInstanceService.class);
        var files = mock(DccControlledFileMapper.class);
        var signatures = mock(DccControlledFileSignatureMapper.class);
        var process = mock(HistoricProcessInstance.class);
        when(process.getProcessDefinitionKey()).thenReturn(processKey);
        when(process.getBusinessKey()).thenReturn("20");
        when(process.getStartUserId()).thenReturn("99");
        when(process.getProcessVariables()).thenReturn(Map.of("objectId", 20L));
        when(processes.getHistoricProcessInstanceMap(Set.of("round-1"))).thenReturn(Map.of("round-1", process));
        when(files.selectByIdIncludingDeleted(20L)).thenReturn(new DccControlledFileDO());
        var task = reviewTask("CURRENT", "11");
        when(tasks.getTaskListByProcessInstanceId("round-1", true)).thenReturn(List.of(task));
        when(signatures.selectListByControlledFileId(20L)).thenReturn(List.of(assignmentSignature()));
        ReflectionTestUtils.setField(adapter, "bpmTaskService", tasks);
        ReflectionTestUtils.setField(adapter, "processInstanceService", processes);
        ReflectionTestUtils.setField(adapter, "controlledFileMapper", files);
        ReflectionTestUtils.setField(adapter, "signatureMapper", signatures);
        return adapter;
    }

    @ParameterizedTest @CsvSource({"dcc-controlled-file-upload", "dcc-controlled-file-revision", "dcc-controlled-file-obsolete"})
    void publicTimelineReturnsBothStagesForExistingNativeProcesses(String processKey) {
        var rows = adapterForTimeline(processKey).listTimeline(ApprovalTaskTimelineQueryContext.of(
                11L, ApprovalModuleCode.DCC, "DCC_CONTROLLED_FILE_TASK", null, "20", "round-1"));
        assertEquals(List.of("部门负责人签名指派", "实际会签人会签"), rows.stream().map(ApprovalTaskTimelineEntry::getNodeName).toList());
    }

    @Test void signedLeaderCanReadAssignmentHistoryAfterTaskMovesToActualSigner() {
        assertEquals(2, adapterForTimeline("dcc-controlled-file-upload").listTimeline(ApprovalTaskTimelineQueryContext.of(
                10L, ApprovalModuleCode.DCC, "DCC_CONTROLLED_FILE_TASK", null, "20", "round-1")).size());
    }

    @Test void unrelatedUserCannotReadSignoffHistory() {
        var adapter = adapterForTimeline("dcc-controlled-file-upload");
        assertThrows(IllegalStateException.class, () -> adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(
                12L, ApprovalModuleCode.DCC, "DCC_CONTROLLED_FILE_TASK", null, "20", "round-1")));
    }
}
