package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQueryContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccApprovalTaskAdapterTest {

    private static final List<String> EXPECTED_DCC_PROCESS_DEFINITION_KEYS = List.of(
            DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL,
            DccControlledFileProcessDefinitionKeys.UPLOAD,
            DccControlledFileProcessDefinitionKeys.REVISION,
            DccControlledFileProcessDefinitionKeys.OBSOLETE,
            DccControlledFileProcessDefinitionKeys.LEGACY_OBSOLETE_FORM_CENTER,
            "dcc-external-file-review"
    );

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
    @Mock
    private cn.iocoder.yudao.module.bpm.approval.service.ApprovalFlowableTaskQueryService centerQuery;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private DccProjectProductTaskDelegate projectApplications;
    @Mock
    private DccOfflineTrainingTaskDelegate offlineTraining;
    @InjectMocks
    private DccApprovalTaskAdapter adapter;

    @BeforeEach
    void injectCenterQuery() {
        org.springframework.test.util.ReflectionTestUtils.setField(adapter, "centerQuery", centerQuery);
    }

    @Test
    void pageTodoReturnsEmptyPageWithoutLoadingProcessInstancesWhenTaskPageIsEmpty() {
        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(), 23L));

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 4, 10));

        assertEquals(23L, page.getTotal());
        assertEquals(List.of(), page.getList());
        org.mockito.Mockito.verifyNoInteractions(processInstanceService, controlledFileMapper, fileCategoryMapper);
    }

    @Test
    void pageTodoMapsBpmDccTasksToControlledFileSummary() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(),eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6001");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(java.util.Set.of("pi-1")))
                .thenReturn(Map.of("pi-1", processInstance));

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6001L);
        file.setTitle("DCC-SOP-001");
        file.setFileNumber("SOP-001");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        file.setProcessInstanceId("pi-1");
        when(controlledFileMapper.selectByIdIncludingDeleted(6001L)).thenReturn(file);
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
        assertEquals(Set.of("PROCESS_IN_MODULE"), summary.getAvailableActions());
        assertEquals("/dcc/controlled-file/detail/6001", summary.getDetailRoute());
        assertEquals(Map.of(
                "handling", "approval",
                "from", "approval-center",
                "processInstanceId", "pi-1",
                "taskId", "task-1"
        ), summary.getDetailQuery());

        ArgumentCaptor<ApprovalTaskQueryContext> captor=ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).todo(captor.capture(),eq("DCC_TASK"));
        assertEquals(100L,captor.getValue().getLoginUserId());
        verify(workflowService, never()).getControlledFile(anyLong());
    }

    @Test
    void pageTodoDoesNotProjectNativeFinalReviewTaskAfterFileBecameActive() {
        // Terminal native reviews are excluded by the center SQL before counting/paging.
        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(), 0L));
        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));
        assertEquals(0L, page.getTotal());
        assertTrue(page.getList().isEmpty());
        verify(controlledFileMapper, never()).selectByIdIncludingDeleted(anyLong());
    }

    @Test
    void pageTodoDoesNotProjectNativeFinalReviewTaskAfterFileWasSuperseded() {
        // Terminal native reviews are excluded by the center SQL before counting/paging.
        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(), 0L));
        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));
        assertEquals(0L, page.getTotal());
        assertTrue(page.getList().isEmpty());
        verify(controlledFileMapper, never()).selectByIdIncludingDeleted(anyLong());
    }

    @Test
    void pageTodoMapsExternalReviewTasksToModuleProcessingOnly() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("external-task-1");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("external-pi-1");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6012");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstance.getProcessDefinitionKey()).thenReturn("dcc-external-file-review");
        when(processInstanceService.getProcessInstanceMap(Set.of("external-pi-1")))
                .thenReturn(Map.of("external-pi-1", processInstance));

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6012L);
        file.setTitle("外来文件评审样例");
        file.setFileNumber("EXT-6012");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(6012L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L).name("外来文件").distributionRequired(Boolean.FALSE).build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals(Set.of("PROCESS_IN_MODULE"), page.getList().get(0).getAvailableActions());
        assertEquals("external-task-1", page.getList().get(0).getSourceTaskId());
        assertEquals("6012", page.getList().get(0).getBusinessKey());
    }

    @Test
    void pageTodoMapsObsoleteFormCenterProcessByObjectIdVariable() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("obsolete-task-1");
        when(task.getName()).thenReturn("作废审核");
        when(task.getTaskDefinitionKey()).thenReturn("OBSOLETE_APPROVAL");
        when(task.getProcessInstanceId()).thenReturn("obsolete-pi-1");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.OBSOLETE);
        when(processInstance.getBusinessKey()).thenReturn("FORM_ACTION:FCI-1-1789847768616");
        when(processInstance.getProcessVariables()).thenReturn(Map.of("objectId", "6003"));
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("obsolete-pi-1")))
                .thenReturn(Map.of("obsolete-pi-1", processInstance));

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6003L);
        file.setTitle("待作废文控文件");
        file.setFileNumber("OBSOLETE-6003");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("ACTIVE");
        when(controlledFileMapper.selectByIdIncludingDeleted(6003L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.FALSE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals("6003", page.getList().get(0).getBusinessKey());
        assertEquals("OBSOLETE-6003", page.getList().get(0).getBusinessCode());
        assertEquals(Set.of("APPROVE", "REJECT"),
                page.getList().get(0).getAvailableActions());
    }

    @Test
    void pageTodoKeepsDocControlApprovalInModuleBecauseQuickApproveRequiresArtifacts() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-final");
        when(task.getName()).thenReturn("文控批准");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_APPROVAL");
        when(task.getProcessInstanceId()).thenReturn("pi-final");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(),eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6004");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-final")))
                .thenReturn(Map.of("pi-final", processInstance));

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6004L);
        file.setTitle("DCC-SOP-004");
        file.setFileNumber("SOP-004");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_APPROVAL");
        when(controlledFileMapper.selectByIdIncludingDeleted(6004L)).thenReturn(file);
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
    void pageTodoKeepsNativeDocControlReviewInModuleBecauseFinalReviewOwnsEvidenceAndFinalization() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-native-final");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-native-final");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6005");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-native-final")))
                .thenReturn(Map.of("pi-native-final", processInstance));

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6005L);
        file.setTitle("DCC-SOP-005");
        file.setFileNumber("SOP-005");
        file.setVersionNo("A/1");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(6005L)).thenReturn(file);
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
    void pageTodoUsesSnapshotNotControlledFileViewPermission() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-matrix-review");
        when(task.getName()).thenReturn("审核会签");
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-matrix-review");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(),eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("2054545668044070330");
        when(processInstance.getStartUserId()).thenReturn("1");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-matrix-review")))
                .thenReturn(Map.of("pi-matrix-review", processInstance));

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(2054545668044070330L);
        file.setTitle("P4 受控文件");
        file.setFileNumber("DCC-P4-202609081528-NEW");
        file.setVersionNo("A/1");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_MATRIX_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(2054545668044070330L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.TRUE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(1074L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals("DCC-P4-202609081528-NEW", page.getList().get(0).getBusinessCode());
        assertEquals(Set.of("APPROVE", "REJECT", "PROCESS_IN_MODULE"), page.getList().get(0).getAvailableActions());
        verify(workflowService, never()).getControlledFile(anyLong());
    }

    @Test
    void pageTodoFiltersByControlledFileNumberInsteadOfBpmTaskName() {
        Task matchingTask = mock(Task.class);
        when(matchingTask.getId()).thenReturn("task-keyword-match");
        when(matchingTask.getName()).thenReturn("文控审核");
        when(matchingTask.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(matchingTask.getProcessInstanceId()).thenReturn("pi-keyword-match");
        when(matchingTask.getCreateTime()).thenReturn(new Date(1782180000000L));

        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(matchingTask), 1L));

        ProcessInstance matchingProcess = mock(ProcessInstance.class);
        when(matchingProcess.getBusinessKey()).thenReturn("6010");
        when(matchingProcess.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-keyword-match")))
                .thenReturn(Map.of("pi-keyword-match", matchingProcess));

        DccControlledFileDO matchingFile = new DccControlledFileDO();
        matchingFile.setId(6010L);
        matchingFile.setTitle("关键词匹配文件");
        matchingFile.setFileNumber("OBSOLETE-KEYWORD-6010");
        matchingFile.setVersionNo("A");
        matchingFile.setCategoryId(7001L);
        matchingFile.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(6010L)).thenReturn(matchingFile);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.FALSE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, "OBSOLETE-KEYWORD-6010", 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals("OBSOLETE-KEYWORD-6010", page.getList().get(0).getBusinessCode());
        ArgumentCaptor<ApprovalTaskQueryContext> query = ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).todo(query.capture(), eq("DCC_TASK"));
        assertEquals("OBSOLETE-KEYWORD-6010", query.getValue().getKeyword());
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
    void reviewObsoleteApproveCompletesFormCenterBpmTask() {
        adapter.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.DCC,
                "DCC_CONTROLLED_FILE_TASK", "obsolete-task-approve", "6001", "obsolete-pi-approve",
                ApprovalTaskReviewResult.APPROVE, "作废审批通过", "secret", false));

        ArgumentCaptor<DccControlledFileApproveTaskReqVO> captor=ArgumentCaptor.forClass(DccControlledFileApproveTaskReqVO.class);
        verify(workflowService).approveTask(eq(100L),eq(6001L),captor.capture());
        assertEquals("obsolete-task-approve",captor.getValue().getTaskId());
        assertEquals("secret",captor.getValue().getPassword());
        assertEquals("作废审批通过",captor.getValue().getReason());
        verify(bpmTaskService,never()).approveTask(any(),any());
    }

    @Test
    void reviewObsoleteRejectCompletesFormCenterBpmTask() {
        adapter.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.DCC,
                "DCC_CONTROLLED_FILE_TASK", "obsolete-task-reject", "6001", "obsolete-pi-reject",
                ApprovalTaskReviewResult.REJECT, "作废申请退回", "secret", false));

        ArgumentCaptor<DccControlledFileRejectTaskReqVO> captor=ArgumentCaptor.forClass(DccControlledFileRejectTaskReqVO.class);
        verify(workflowService).rejectTask(eq(100L),eq(6001L),captor.capture());
        assertEquals("obsolete-task-reject",captor.getValue().getTaskId());
        assertEquals("secret",captor.getValue().getPassword());
        verify(bpmTaskService,never()).rejectTask(any(),any());
    }

    @Test
    void reviewApproveRejectsBlankReasonBeforeWorkflow() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> adapter.review(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.DCC,
                        "DCC_CONTROLLED_FILE_TASK", "task-approve", "6001", "pi-1",
                        ApprovalTaskReviewResult.APPROVE, "   ", "secret", false)));

        assertEquals("APPROVAL_REASON_REQUIRED: DCC approve requires reason", error.getMessage());
        verify(workflowService, never()).approveTask(anyLong(), anyLong(), any());
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
        when(centerQuery.todo(any(),eq("DCC_TASK")))
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
    void pageDoneCarriesExactApprovalHistoryBodyContextForRetainedOldVersion() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("historic-upload-task-A1");
        when(task.getName()).thenReturn("批准并确认文件负责人");
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_APPROVAL");
        when(task.getProcessInstanceId()).thenReturn("historic-upload-round-A1");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));
        when(centerQuery.done(any(), eq("DCC_TASK"))).thenReturn(new PageResult<>(List.of(task), 1L));
        HistoricProcessInstance process = mock(HistoricProcessInstance.class);
        when(process.getBusinessKey()).thenReturn("4230");
        when(process.getStartUserId()).thenReturn("501");
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.UPLOAD);
        when(processInstanceService.getHistoricProcessInstanceMap(Set.of("historic-upload-round-A1")))
                .thenReturn(Map.of("historic-upload-round-A1", process));
        DccControlledFileDO file = DccControlledFileDO.builder()
                .id(4230L).masterId(9202L).dccProjectCodeId(305L).fileTypeTaxonomyId(113L)
                .title("保留的旧A1").fileNumber("DCC-OLD-A1").versionNo("A/1")
                .categoryId(7002L).status("OBSOLETE").changeType("NEW")
                .publishedFileId(9002L).supersededByFileId(4244L)
                .processInstanceId("historic-upload-round-A1").build();
        file.setTenantId(1L);
        file.setDeleted(Boolean.FALSE);
        when(controlledFileMapper.selectByIdIncludingDeleted(4230L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7002L)).thenReturn(DccFileCategoryDO.builder()
                .id(7002L).name("历史文件类别").distributionRequired(Boolean.FALSE).build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));
        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        ApprovalTaskSummary summary = page.getList().get(0);
        assertEquals(Boolean.FALSE, summary.getBusinessDeleted());
        assertEquals("/dcc/controlled-file/detail/4230", summary.getDetailRoute());
        assertEquals("historic-upload-task-A1", summary.getSourceTaskId());
        assertEquals("historic-upload-round-A1", summary.getProcessInstanceId());
        verify(controlledFileMapper).selectByIdIncludingDeleted(4230L);
        try {
            String wire = summary.getDetailQuery().get("bodyReadContext");
            org.junit.jupiter.api.Assertions.assertNotNull(wire,
                    "retained A1 history must supply its exact bodyReadContext instead of a DAILY viewer link");
            var context = cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBodyReadContext.fromWire(4230L, wire);
            assertEquals(cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY, context.intent());
            assertEquals("4230", context.controlledFileId());
            assertEquals("A/1", context.expectedVersionNo());
            assertEquals("UPLOAD", context.applicationType());
            assertEquals("historic-upload-round-A1", context.bpmRound());
            assertEquals("historic-upload-task-A1", context.taskId());
            assertEquals(Map.of("viewer", "1", "from", "approval-center", "bodyReadContext", wire), summary.getDetailQuery());
        } finally {
            org.mockito.Mockito.verifyNoMoreInteractions(controlledFileMapper);
            org.mockito.Mockito.verifyNoInteractions(workflowService);
        }
    }

    @Test
    void historyBodyLinkUsesHistoricDefinitionRatherThanFilesOriginalChangeType() {
        var file = DccControlledFileDO.builder().id(4230L).versionNo("A/1").changeType("NEW").build();
        var task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("completed-task");
        when(task.getProcessInstanceId()).thenReturn("selected-history-round");
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        for (var entry : Map.of(DccControlledFileProcessDefinitionKeys.REVISION, "REVISION",
                DccControlledFileProcessDefinitionKeys.OBSOLETE, "OBSOLETE",
                DccControlledFileProcessDefinitionKeys.LEGACY_OBSOLETE_FORM_CENTER, "OBSOLETE").entrySet()) {
            var process = mock(HistoricProcessInstance.class);
            when(process.getProcessDefinitionKey()).thenReturn(entry.getKey());
            Map<String, String> query = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    DccApprovalTaskAdapter.class, "approvalCenterHistoryDetailQuery", file, task, process);
            org.junit.jupiter.api.Assertions.assertNotNull(query);
            var context = cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBodyReadContext.fromWire(4230L, query.get("bodyReadContext"));
            assertEquals(entry.getValue(), context.applicationType());
            assertEquals("A/1", context.expectedVersionNo());
            assertEquals("selected-history-round", context.bpmRound());
            assertEquals("completed-task", context.taskId());
        }
    }

    @Test
    void historyBodyLinkIsUnavailableForUnknownDefinitionOrIncompleteIdentity() {
        var file = DccControlledFileDO.builder().id(4230L).versionNo("A/1").changeType("NEW").build();
        var task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn("completed-task");
        when(task.getProcessInstanceId()).thenReturn("selected-history-round");
        when(task.getEndTime()).thenReturn(new Date(1782180300000L));
        var process = mock(HistoricProcessInstance.class);
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.UPLOAD);
        java.util.function.Supplier<Map<String, String>> link = () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                DccApprovalTaskAdapter.class, "approvalCenterHistoryDetailQuery", file, task, process);
        org.junit.jupiter.api.Assertions.assertNotNull(link.get());
        for (String definition : new String[]{null, DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL, "dcc-external-file-review"}) {
            when(process.getProcessDefinitionKey()).thenReturn(definition);
            org.junit.jupiter.api.Assertions.assertNull(link.get());
        }
        when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.UPLOAD);
        file.setVersionNo(null);
        org.junit.jupiter.api.Assertions.assertNull(link.get());
        file.setVersionNo("A/1");
        when(task.getId()).thenReturn(null);
        org.junit.jupiter.api.Assertions.assertNull(link.get());
        when(task.getProcessInstanceId()).thenReturn(" ");
        org.junit.jupiter.api.Assertions.assertNull(link.get());
    }

    @Test
    void pageDoneCarriesExactLegacyApprovalHistoryBodyContextForItsBoundFile() {
        when(fileCategoryMapper.selectById(7002L)).thenReturn(DccFileCategoryDO.builder()
                .id(7002L).name("保留历史类别").distributionRequired(Boolean.FALSE).build());
        int reads = 0;
        for (String changeType : List.of("NEW", "REVISION")) {
            String round = "bound-legacy-round-" + changeType;
            String taskId = "completed-legacy-task-" + changeType;
            var task = mock(HistoricTaskInstance.class);
            when(task.getId()).thenReturn(taskId);
            when(task.getName()).thenReturn("历史批准");
            when(task.getTaskDefinitionKey()).thenReturn("MATRIX_APPROVAL");
            when(task.getProcessInstanceId()).thenReturn(round);
            when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
            when(task.getEndTime()).thenReturn(new Date(1782180300000L));
            when(task.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2));
            when(centerQuery.done(any(), eq("DCC_TASK"))).thenReturn(new PageResult<>(List.of(task), 1L));
            var process = mock(HistoricProcessInstance.class);
            when(process.getBusinessKey()).thenReturn("4230");
            when(process.getStartUserId()).thenReturn("501");
            when(process.getProcessDefinitionKey()).thenReturn(DccControlledFileProcessDefinitionKeys.LEGACY_APPROVAL);
            when(processInstanceService.getHistoricProcessInstanceMap(Set.of(round))).thenReturn(Map.of(round, process));
            var file = DccControlledFileDO.builder().id(4230L).title("合法已绑定旧A1")
                    .fileNumber("DCC-LEGACY-A1").versionNo("A/1").categoryId(7002L)
                    .status("OBSOLETE").publishedFileId(9002L).supersededByFileId(4244L)
                    .requesterId(501L).processInstanceId(round).changeType(changeType).build();
            file.setDeleted(Boolean.FALSE);
            when(controlledFileMapper.selectByIdIncludingDeleted(4230L)).thenReturn(file);

            var page = adapter.page(ApprovalTaskQueryContext.of(100L,
                    ApprovalTaskViewType.DONE, ApprovalModuleCode.DCC, null, 1, 10));
            assertEquals(1L, page.getTotal());
            assertEquals(1, page.getList().size());
            var summary = page.getList().get(0);
            verify(controlledFileMapper, org.mockito.Mockito.times(++reads)).selectByIdIncludingDeleted(4230L);
            try {
                String wire = summary.getDetailQuery().get("bodyReadContext");
                org.junit.jupiter.api.Assertions.assertNotNull(wire,
                        "the explicitly bound LEGACY_APPROVAL family must retain its historical body link");
                var context = cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBodyReadContext.fromWire(4230L, wire);
                assertEquals(cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY, context.intent());
                assertEquals("4230", context.controlledFileId());
                assertEquals("A/1", context.expectedVersionNo());
                assertEquals("NEW".equals(changeType) ? "UPLOAD" : "REVISION", context.applicationType());
                assertEquals(round, context.bpmRound());
                assertEquals(taskId, context.taskId());
                assertEquals("/dcc/controlled-file/detail/4230", summary.getDetailRoute());
            } finally {
                org.mockito.Mockito.verifyNoMoreInteractions(controlledFileMapper);
                org.mockito.Mockito.verifyNoInteractions(workflowService);
            }
        }
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
        when(centerQuery.done(any(),eq("DCC_TASK")))
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

        ArgumentCaptor<ApprovalTaskQueryContext> captor=ArgumentCaptor.forClass(ApprovalTaskQueryContext.class);
        verify(centerQuery).done(captor.capture(),eq("DCC_TASK"));
        assertEquals(100L,captor.getValue().getLoginUserId());
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
        when(centerQuery.done(any(),eq("DCC_TASK")))
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
        when(centerQuery.done(any(),eq("DCC_TASK")))
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
        when(centerQuery.done(any(),eq("DCC_TASK")))
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
    void pageTodoUsesNullAssigneeFilterWhenGlobalViewEnabled() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-global");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-global");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(),eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));

        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getBusinessKey()).thenReturn("6010");
        when(processInstance.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-global")))
                .thenReturn(Map.of("pi-global", processInstance));

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6010L);
        file.setTitle("DCC-SOP-010");
        file.setFileNumber("SOP-010");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(6010L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.TRUE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10, true));

        assertEquals(1L, page.getTotal());
        verify(centerQuery).todo(org.mockito.ArgumentMatchers.argThat(ApprovalTaskQueryContext::isGlobalView),eq("DCC_TASK"));
    }
    @Test
    void nativeOwnerApprovalActionsRequireTheModuleDialogForBothUploadAndRevision() {
        for (String key : List.of(DccControlledFileProcessDefinitionKeys.UPLOAD, DccControlledFileProcessDefinitionKeys.REVISION)) {
            Set<String> actions = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                    DccApprovalTaskAdapter.class, "resolveTodoAvailableActions", key, "MATRIX_APPROVAL", "PENDING_MATRIX_APPROVAL");
            assertEquals(Set.of("PROCESS_IN_MODULE"), actions);
        }
    }

    @Test
    void fileAndNativeTasksShareOneSortedPageWindowAndAccurateTotal() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-mixed");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-mixed");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(centerQuery.todo(any(), eq("DCC_TASK")))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        ProcessInstance process = mock(ProcessInstance.class);
        when(process.getBusinessKey()).thenReturn("6011");when(process.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-mixed"))).thenReturn(Map.of("pi-mixed", process));
        var file = new DccControlledFileDO();file.setId(6011L);file.setTitle("真实文件");file.setFileNumber("DOC-11");
        file.setVersionNo("A/1");file.setStatus("PENDING_DOC_CONTROL_REVIEW");file.setCategoryId(7001L);
        when(controlledFileMapper.selectByIdIncludingDeleted(6011L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder().id(7001L).name("实际类别").build());
        var nativeRow = ApprovalTaskSummary.builder().id("DCC:PROJECT_NATIVE:9007199254740993:REVIEW")
                .businessKey("9007199254740993").taskCreatedAt(java.time.LocalDateTime.of(2026,10,5,1,0)).build();
        when(projectApplications.list(any())).thenReturn(List.of(nativeRow));
        var first = adapter.page(ApprovalTaskQueryContext.of(100L, ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 1));
        var second = adapter.page(ApprovalTaskQueryContext.of(100L, ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 2, 1));
        assertEquals(2,first.getTotal());assertEquals(2,second.getTotal());
        assertEquals("9007199254740993",first.getList().get(0).getBusinessKey());
        assertEquals("6011",second.getList().get(0).getBusinessKey());
    }
}
