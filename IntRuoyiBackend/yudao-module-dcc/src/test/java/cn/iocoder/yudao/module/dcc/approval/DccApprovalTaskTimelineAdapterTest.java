package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQueryContext;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccApprovalTaskTimelineAdapterTest {

    @Mock
    private BpmTaskService bpmTaskService;
    @Mock
    private BpmProcessInstanceService processInstanceService;
    @Mock
    private DccControlledFileWorkflowService workflowService;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @InjectMocks
    private DccApprovalTaskAdapter adapter;

    @Test
    void listTimelineMapsHistoricTasksToUnifiedTimelineEntries() {
        HistoricTaskInstance start = mock(HistoricTaskInstance.class);
        when(start.getId()).thenReturn("historic-start");
        when(start.getName()).thenReturn("发起");
        when(start.getTaskDefinitionKey()).thenReturn("START_NODE");
        when(start.getAssignee()).thenReturn("100");
        when(start.getEndTime()).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.parse("2026-06-23T09:05:00")));

        HistoricTaskInstance review = mock(HistoricTaskInstance.class);
        when(review.getId()).thenReturn("historic-task-1");
        when(review.getName()).thenReturn("文控审核");
        when(review.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(review.getAssignee()).thenReturn("100");
        when(review.getEndTime()).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.parse("2026-06-23T10:30:00")));

        when(bpmTaskService.getTaskListByProcessInstanceId("pi-1", true)).thenReturn(List.of(start, review));

        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6001");
        when(processInstance.getStartUserId()).thenReturn("100");
        when(processInstanceService.getHistoricProcessInstanceMap(java.util.Set.of("pi-1")))
                .thenReturn(Map.of("pi-1", processInstance));

        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(6001L)
                .title("DCC-SOP-001")
                .fileNumber("SOP-001")
                .status("DONE")
                .processInstanceId("pi-1")
                .build();
        when(controlledFileMapper.selectByIdIncludingDeleted(6001L)).thenReturn(file);

        List<ApprovalTaskTimelineEntry> entries = adapter.listTimeline(
                ApprovalTaskTimelineQueryContext.of(100L, ApprovalModuleCode.DCC,
                        "DCC_CONTROLLED_FILE_TASK", "task-1", "6001", "pi-1"));

        assertEquals(2, entries.size());
        assertEquals("historic-task-1", entries.get(1).getId());
        assertEquals("审批通过", entries.get(1).getActionLabel());
        assertEquals("FLOWABLE_HISTORY", entries.get(1).getEvidenceType());

        verify(bpmTaskService).getTaskListByProcessInstanceId("pi-1", true);
        verify(workflowService, never()).getControlledFile(anyLong());
    }

    @Test
    void listTimelineFailsWhenBusinessObjectMissing() {
        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6009");
        when(processInstanceService.getHistoricProcessInstanceMap(java.util.Set.of("pi-missing")))
                .thenReturn(Map.of("pi-missing", processInstance));

        when(controlledFileMapper.selectByIdIncludingDeleted(6009L)).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.listTimeline(ApprovalTaskTimelineQueryContext.of(100L, ApprovalModuleCode.DCC,
                        "DCC_CONTROLLED_FILE_TASK", "task-1", "6009", "pi-missing")));

        assertEquals("APPROVAL_BUSINESS_OBJECT_REQUIRED: DCC controlled file summary snapshot not found 6009", ex.getMessage());
    }
}
