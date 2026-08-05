package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccApprovalTaskAdapterTest {

    @Mock
    private BpmTaskService bpmTaskService;
    @Mock
    private BpmProcessInstanceService processInstanceService;
    @Mock
    private DccControlledFileWorkflowService workflowService;
    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccFileCategoryMapper fileCategoryMapper;
    @InjectMocks
    private DccApprovalTaskAdapter adapter;

    @Test
    void pageTodoMapsBpmDccTasksToControlledFileSummary() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6001");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(java.util.Set.of("pi-1")))
                .thenReturn(Map.of("pi-1", processInstance));

        DccControlledFileRespVO file = new DccControlledFileRespVO();
        file.setId(6001L);
        file.setTitle("DCC-SOP-001");
        file.setFileNumber("SOP-001");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        file.setProcessInstanceId("pi-1");
        when(workflowService.getControlledFile(6001L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.TRUE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, "SOP", 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("DCC:DCC_CONTROLLED_FILE_TASK:task-1", summary.getId());
        assertEquals(ApprovalModuleCode.DCC, summary.getModuleCode());
        assertEquals("DCC_CONTROLLED_FILE_TASK", summary.getSourceTaskType());
        assertEquals("6001", summary.getBusinessKey());
        assertEquals("DCC-SOP-001", summary.getBusinessTitle());
        assertEquals("SOP-001", summary.getBusinessCode());
        assertEquals("PENDING_DOC_CONTROL_REVIEW", summary.getBusinessStatus());
        assertEquals(List.of(
                "文件编号：SOP-001",
                "版本：A",
                "分类：SOP 文件",
                "当前节点：文控审核",
                "盖章：需要",
                "分发：需要"
        ), summary.getBusinessContextTags());
        assertEquals(Boolean.FALSE, summary.getBusinessDeleted());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals(Set.of("APPROVE", "REJECT", "PROCESS_IN_MODULE"), summary.getAvailableActions());
        assertEquals("/dcc/controlled-file/detail/6001", summary.getDetailRoute());
        assertEquals(Map.of(
                "handling", "approval",
                "from", "approval-center",
                "processInstanceId", "pi-1",
                "taskId", "task-1"
        ), summary.getDetailQuery());

        ArgumentCaptor<BpmTaskPageReqVO> captor = ArgumentCaptor.forClass(BpmTaskPageReqVO.class);
        verify(bpmTaskService).getTaskTodoPage(eq(100L), captor.capture());
        assertEquals("dcc-controlled-file-approval", captor.getValue().getProcessDefinitionKey());
    }

    @Test
    void pageTodoKeepsDocControlApprovalInModuleBecauseQuickApproveRequiresArtifacts() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-final");
        when(task.getName()).thenReturn("文控批准");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_APPROVAL");
        when(task.getProcessInstanceId()).thenReturn("pi-final");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6004");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-final")))
                .thenReturn(Map.of("pi-final", processInstance));

        DccControlledFileRespVO file = new DccControlledFileRespVO();
        file.setId(6004L);
        file.setTitle("DCC-SOP-004");
        file.setFileNumber("SOP-004");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_APPROVAL");
        when(workflowService.getControlledFile(6004L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.TRUE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(Set.of("PROCESS_IN_MODULE"), page.getList().get(0).getAvailableActions());
    }

    @Test
    void reviewApproveDelegatesToControlledFileWorkflow() {
        adapter.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.DCC,
                "DCC_CONTROLLED_FILE_TASK", "task-approve", "6001", "pi-1",
                ApprovalTaskReviewResult.APPROVE, "同意", "secret", false));

        ArgumentCaptor<DccControlledFileApproveTaskReqVO> captor =
                ArgumentCaptor.forClass(DccControlledFileApproveTaskReqVO.class);
        verify(workflowService).approveTask(eq(100L), eq(6001L), captor.capture());
        assertEquals("task-approve", captor.getValue().getTaskId());
        assertEquals("secret", captor.getValue().getPassword());
        assertEquals("同意", captor.getValue().getReason());
    }

    @Test
    void reviewRejectDelegatesToControlledFileWorkflow() {
        adapter.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.DCC,
                "DCC_CONTROLLED_FILE_TASK", "task-reject", "6001", "pi-1",
                ApprovalTaskReviewResult.REJECT, "资料不完整", "secret", false));

        ArgumentCaptor<DccControlledFileRejectTaskReqVO> captor =
                ArgumentCaptor.forClass(DccControlledFileRejectTaskReqVO.class);
        verify(workflowService).rejectTask(eq(100L), eq(6001L), captor.capture());
        assertEquals("task-reject", captor.getValue().getTaskId());
        assertEquals("secret", captor.getValue().getPassword());
        assertEquals("资料不完整", captor.getValue().getReason());
    }

    @Test
    void pageTodoFailsWhenBpmTaskHasNoBusinessKey() {
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("pi-missing");
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn(null);
        when(processInstanceService.getProcessInstanceMap(java.util.Set.of("pi-missing")))
                .thenReturn(Map.of("pi-missing", processInstance));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> adapter.page(ApprovalTaskQueryContext.of(100L, ApprovalTaskViewType.TODO,
                        ApprovalModuleCode.DCC, null, 1, 10)));

        assertEquals("APPROVAL_BUSINESS_KEY_REQUIRED: DCC BPM task missing controlled file business key", ex.getMessage());
    }

    @Test
    void pageTodoSkipsFormCenterBusinessActionProcessWithoutParsingAsDccFile() {
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("pi-form-action");
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("FORM_ACTION:FCI-122-1");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-form-action")))
                .thenReturn(Map.of("pi-form-action", processInstance));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(0L, page.getTotal());
        assertEquals(List.of(), page.getList());
        verify(workflowService, never()).getControlledFile(anyLong());
    }

    @Test
    void pageTodoKeepsDccRowsWhenSkippingSharedFormCenterProcess() {
        Task formTask = mock(Task.class);
        when(formTask.getProcessInstanceId()).thenReturn("pi-form-action");

        Task dccTask = mock(Task.class);
        when(dccTask.getId()).thenReturn("task-dcc");
        when(dccTask.getName()).thenReturn("文控审核");
        when(dccTask.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(dccTask.getProcessInstanceId()).thenReturn("pi-dcc");
        when(dccTask.getCreateTime()).thenReturn(new Date(1782180000000L));

        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(formTask, dccTask), 2L));

        ProcessInstance formProcess = mock(ProcessInstance.class);
        when(formProcess.getBusinessKey()).thenReturn("FORM_ACTION:FCI-122-1784320139265");
        ProcessInstance dccProcess = mock(ProcessInstance.class);
        when(dccProcess.getBusinessKey()).thenReturn("6001");
        when(dccProcess.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-form-action", "pi-dcc")))
                .thenReturn(Map.of("pi-form-action", formProcess, "pi-dcc", dccProcess));

        DccControlledFileRespVO file = new DccControlledFileRespVO();
        file.setId(6001L);
        file.setTitle("DCC-SOP-001");
        file.setFileNumber("SOP-001");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(workflowService.getControlledFile(6001L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.TRUE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("6001", page.getList().get(0).getBusinessKey());
        assertEquals("DCC-SOP-001", page.getList().get(0).getBusinessTitle());
    }

    @Test
    void pageDoneUsesDeletedControlledFileSnapshotForHistoricalSummary() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("historic-task-1");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("historic-pi-1");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));
        when(bpmTaskService.getTaskDonePage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6002");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getHistoricProcessInstanceMap(java.util.Set.of("historic-pi-1")))
                .thenReturn(Map.of("historic-pi-1", processInstance));

        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(6002L)
                .title("撤回后的历史文件")
                .fileNumber("DCC-6002")
                .versionNo("V1.0")
                .categoryId(7002L)
                .stampedFileId(9002L)
                .status("WITHDRAWN")
                .processInstanceId("historic-pi-1")
                .build();
        file.setDeleted(Boolean.TRUE);
        when(controlledFileMapper.selectByIdIncludingDeleted(6002L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7002L)).thenReturn(DccFileCategoryDO.builder()
                .id(7002L)
                .name("质量手册")
                .distributionRequired(Boolean.FALSE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("DCC:DCC_CONTROLLED_FILE_TASK:historic-task-1", summary.getId());
        assertEquals("6002", summary.getBusinessKey());
        assertEquals("撤回后的历史文件", summary.getBusinessTitle());
        assertEquals("DCC-6002", summary.getBusinessCode());
        assertEquals("WITHDRAWN", summary.getBusinessStatus());
        assertEquals(List.of(
                "文件编号：DCC-6002",
                "版本：V1.0",
                "分类：质量手册",
                "当前节点：文控审核",
                "盖章：已生成",
                "分发：不需要"
        ), summary.getBusinessContextTags());
        assertEquals(Boolean.TRUE, summary.getBusinessDeleted());
        assertEquals(ApprovalTaskReviewResult.APPROVE, summary.getApprovalResult());
        assertEquals(Boolean.TRUE, summary.getRequiresSignature());
        assertEquals("/dcc/controlled-file/detail/6002", summary.getDetailRoute());
        assertEquals(Map.of("viewer", "1", "from", "approval-center"), summary.getDetailQuery());

        ArgumentCaptor<BpmTaskPageReqVO> captor = ArgumentCaptor.forClass(BpmTaskPageReqVO.class);
        verify(bpmTaskService).getTaskDonePage(eq(100L), captor.capture());
        assertEquals("dcc-controlled-file-approval", captor.getValue().getProcessDefinitionKey());
        verify(controlledFileMapper).selectByIdIncludingDeleted(6002L);
        verify(workflowService, never()).getControlledFile(anyLong());
    }

    @Test
    void pageDoneKeepsLegacyHistoricalSnapshotWhenVersionNoOrCategoryIsMissing() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("historic-task-legacy-version");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("historic-pi-legacy-version");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));
        when(bpmTaskService.getTaskDonePage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6011");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getHistoricProcessInstanceMap(Set.of("historic-pi-legacy-version")))
                .thenReturn(Map.of("historic-pi-legacy-version", processInstance));

        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(6011L)
                .title("历史缺版本号文件")
                .fileNumber("DCC-6011")
                .status("APPROVED")
                .processInstanceId("historic-pi-legacy-version")
                .build();
        when(controlledFileMapper.selectByIdIncludingDeleted(6011L)).thenReturn(file);

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("6011", summary.getBusinessKey());
        assertEquals("历史缺版本号文件", summary.getBusinessTitle());
        assertEquals(List.of(
                "文件编号：DCC-6011",
                "版本：-",
                "分类：-",
                "当前节点：文控审核",
                "盖章：需要",
                "分发：不需要"
        ), summary.getBusinessContextTags());
        assertEquals(ApprovalTaskReviewResult.APPROVE, summary.getApprovalResult());
    }

    @Test
    void pageDoneReturnsEmptyPageWithoutLoadingProcessInstancesWhenHistoricPageIsEmpty() {
        when(bpmTaskService.getTaskDonePage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(), 0L));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(0L, page.getTotal());
        assertEquals(List.of(), page.getList());
        verify(processInstanceService, never()).getHistoricProcessInstanceMap(any());
    }

    @Test
    void pageDoneBuildsDeletedSummaryWhenHistoricalSnapshotMissing() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("historic-task-missing");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("historic-pi-missing");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));
        when(bpmTaskService.getTaskDonePage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6009");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getHistoricProcessInstanceMap(Set.of("historic-pi-missing")))
                .thenReturn(Map.of("historic-pi-missing", processInstance));

        when(controlledFileMapper.selectByIdIncludingDeleted(6009L)).thenReturn(null);

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals("6009", summary.getBusinessKey());
        assertEquals("已删除文控文件", summary.getBusinessTitle());
        assertEquals("6009", summary.getBusinessCode());
        assertEquals("DELETED", summary.getBusinessStatus());
        assertEquals(Boolean.TRUE, summary.getBusinessDeleted());
        assertEquals("/dcc/controlled-file/detail/6009", summary.getDetailRoute());
        assertEquals(Map.of("viewer", "1", "from", "approval-center"), summary.getDetailQuery());
    }

    @Test
    void pageDoneSkipsFormCenterBusinessActionProcessWithoutParsingAsDccFile() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getProcessInstanceId()).thenReturn("historic-pi-form-action");
        when(bpmTaskService.getTaskDonePage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        HistoricProcessInstance processInstance = mock(HistoricProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("FORM_ACTION:FCI-122-2");
        when(processInstanceService.getHistoricProcessInstanceMap(Set.of("historic-pi-form-action")))
                .thenReturn(Map.of("historic-pi-form-action", processInstance));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(0L, page.getTotal());
        assertEquals(List.of(), page.getList());
        verify(controlledFileMapper, never()).selectByIdIncludingDeleted(anyLong());
    }

    @Test
    void pageDoneKeepsDccRowsWhenSkippingSharedFormCenterProcess() {
        HistoricTaskInstance formTask = mock(HistoricTaskInstance.class);
        when(formTask.getProcessInstanceId()).thenReturn("historic-pi-form-action");

        HistoricTaskInstance dccTask = mock(HistoricTaskInstance.class);
        when(dccTask.getId()).thenReturn("historic-task-dcc");
        when(dccTask.getName()).thenReturn("文控审核");
        when(dccTask.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(dccTask.getProcessInstanceId()).thenReturn("historic-pi-dcc");
        when(dccTask.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(dccTask.getEndTime()).thenReturn(new Date(1782180300000L));
        when(dccTask.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));

        when(bpmTaskService.getTaskDonePage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(formTask, dccTask), 2L));

        HistoricProcessInstance formProcess = mock(HistoricProcessInstance.class);
        when(formProcess.getBusinessKey()).thenReturn("FORM_ACTION:FCI-122-1784320139265");
        HistoricProcessInstance dccProcess = mock(HistoricProcessInstance.class);
        when(dccProcess.getBusinessKey()).thenReturn("6002");
        when(dccProcess.getStartUserId()).thenReturn("501");
        when(processInstanceService.getHistoricProcessInstanceMap(Set.of("historic-pi-form-action", "historic-pi-dcc")))
                .thenReturn(Map.of("historic-pi-form-action", formProcess, "historic-pi-dcc", dccProcess));

        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(6002L)
                .title("历史文控文件")
                .fileNumber("DCC-6002")
                .versionNo("A")
                .categoryId(7002L)
                .status("APPROVED")
                .processInstanceId("historic-pi-dcc")
                .build();
        when(controlledFileMapper.selectByIdIncludingDeleted(6002L)).thenReturn(file);

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("6002", page.getList().get(0).getBusinessKey());
        assertEquals("历史文控文件", page.getList().get(0).getBusinessTitle());
    }

    @Test
    void pageTodoUsesNullAssigneeFilterWhenGlobalViewEnabled() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-global");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-global");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(bpmTaskService.getTaskTodoPage(eq(null), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6010");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-global")))
                .thenReturn(Map.of("pi-global", processInstance));

        DccControlledFileRespVO file = new DccControlledFileRespVO();
        file.setId(6010L);
        file.setTitle("DCC-SOP-010");
        file.setFileNumber("SOP-010");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(workflowService.getControlledFile(6010L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.TRUE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10, true));

        assertEquals(1L, page.getTotal());
        verify(bpmTaskService).getTaskTodoPage(eq(null), any(BpmTaskPageReqVO.class));
    }
}
