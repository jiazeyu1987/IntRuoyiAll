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
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private DccProjectProductTaskDelegate projectApplications;
    @Mock
    private DccOfflineTrainingTaskDelegate offlineTraining;
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

        ArgumentCaptor<BpmTaskPageReqVO> captor = ArgumentCaptor.forClass(BpmTaskPageReqVO.class);
        verify(bpmTaskService, org.mockito.Mockito.times(EXPECTED_DCC_PROCESS_DEFINITION_KEYS.size()))
                .getTaskTodoPage(eq(100L), captor.capture());
        assertEquals(EXPECTED_DCC_PROCESS_DEFINITION_KEYS,
                captor.getAllValues().stream().map(BpmTaskPageReqVO::getProcessDefinitionKey).toList());
        verify(workflowService, never()).getControlledFile(anyLong());
    }

    @Test
    void pageTodoDoesNotProjectNativeFinalReviewTaskAfterFileBecameActive() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-active-final");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-active-final");
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessDefinitionKey()).thenReturn(
                DccControlledFileProcessDefinitionKeys.UPLOAD);
        when(processInstance.getBusinessKey()).thenReturn("6002");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-active-final")))
                .thenReturn(Map.of("pi-active-final", processInstance));
        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6002L);
        file.setTitle("Active DCC file");
        file.setFileNumber("ACTIVE-6002");
        file.setVersionNo("A/1");
        file.setCategoryId(7001L);
        file.setStatus("ACTIVE");
        when(controlledFileMapper.selectByIdIncludingDeleted(6002L)).thenReturn(file);

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(0L, page.getTotal());
        assertTrue(page.getList().isEmpty());
    }

    @Test
    void pageTodoDoesNotProjectNativeFinalReviewTaskAfterFileWasSuperseded() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-superseded-final");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("pi-superseded-final");
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(task), 1L));
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessDefinitionKey()).thenReturn(
                DccControlledFileProcessDefinitionKeys.REVISION);
        when(processInstance.getBusinessKey()).thenReturn("6003");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-superseded-final")))
                .thenReturn(Map.of("pi-superseded-final", processInstance));
        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6003L);
        file.setTitle("Superseded DCC file");
        file.setFileNumber("SUPERSEDED-6003");
        file.setVersionNo("A/1");
        file.setCategoryId(7001L);
        file.setStatus("SUPERSEDED");
        when(controlledFileMapper.selectByIdIncludingDeleted(6003L)).thenReturn(file);

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(0L, page.getTotal());
        assertTrue(page.getList().isEmpty());
    }

    @Test
    void pageTodoMapsExternalReviewTasksToModuleProcessingOnly() {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("external-task-1");
        when(task.getName()).thenReturn("文控审核");
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(task.getProcessInstanceId()).thenReturn("external-pi-1");
        when(task.getCreateTime()).thenReturn(new Date(1782180000000L));
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenAnswer(invocation -> {
                    BpmTaskPageReqVO reqVO = invocation.getArgument(1);
                    return "dcc-external-file-review".equals(reqVO.getProcessDefinitionKey())
                            ? new PageResult<>(List.of(task), 1L)
                            : new PageResult<>(List.of(), 0L);
                });

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
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenAnswer(invocation -> {
                    BpmTaskPageReqVO request = invocation.getArgument(1);
                    return DccControlledFileProcessDefinitionKeys.OBSOLETE.equals(request.getProcessDefinitionKey())
                            ? new PageResult<>(List.of(task), 1L)
                            : new PageResult<>(List.of(), 0L);
                });

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
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
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
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
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
        when(bpmTaskService.getTaskTodoPage(eq(1074L), any(BpmTaskPageReqVO.class)))
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

        Task nonMatchingTask = mock(Task.class);
        when(nonMatchingTask.getId()).thenReturn("task-keyword-miss");
        when(nonMatchingTask.getName()).thenReturn("文控审核");
        when(nonMatchingTask.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(nonMatchingTask.getProcessInstanceId()).thenReturn("pi-keyword-miss");
        when(nonMatchingTask.getCreateTime()).thenReturn(new Date(1782180000000L));

        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(matchingTask, nonMatchingTask), 2L));

        ProcessInstance matchingProcess = mock(ProcessInstance.class);
        when(matchingProcess.getBusinessKey()).thenReturn("6010");
        when(matchingProcess.getStartUserId()).thenReturn("501");
        ProcessInstance nonMatchingProcess = mock(ProcessInstance.class);
        when(nonMatchingProcess.getBusinessKey()).thenReturn("6011");
        when(nonMatchingProcess.getStartUserId()).thenReturn("501");
        when(processInstanceService.getProcessInstanceMap(Set.of("pi-keyword-match", "pi-keyword-miss")))
                .thenReturn(Map.of(
                        "pi-keyword-match", matchingProcess,
                        "pi-keyword-miss", nonMatchingProcess));

        DccControlledFileDO matchingFile = new DccControlledFileDO();
        matchingFile.setId(6010L);
        matchingFile.setTitle("关键词匹配文件");
        matchingFile.setFileNumber("OBSOLETE-KEYWORD-6010");
        matchingFile.setVersionNo("A");
        matchingFile.setCategoryId(7001L);
        matchingFile.setStatus("PENDING_DOC_CONTROL_REVIEW");
        DccControlledFileDO nonMatchingFile = new DccControlledFileDO();
        nonMatchingFile.setId(6011L);
        nonMatchingFile.setTitle("其他文件");
        nonMatchingFile.setFileNumber("OTHER-6011");
        nonMatchingFile.setVersionNo("A");
        nonMatchingFile.setCategoryId(7001L);
        nonMatchingFile.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(6010L)).thenReturn(matchingFile);
        when(controlledFileMapper.selectByIdIncludingDeleted(6011L)).thenReturn(nonMatchingFile);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.FALSE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, "OBSOLETE-KEYWORD-6010", 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals("OBSOLETE-KEYWORD-6010", page.getList().get(0).getBusinessCode());
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

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6001L);
        file.setTitle("DCC-SOP-001");
        file.setFileNumber("SOP-001");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(6001L)).thenReturn(file);
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
    void pageTodoPaginatesAfterFilteringSharedFormCenterTasks() {
        List<Task> sourceTasks = new ArrayList<>();
        Map<String, ProcessInstance> processInstances = new HashMap<>();
        for (int index = 1; index <= 10; index++) {
            Task formTask = mock(Task.class);
            String processInstanceId = "pi-form-action-" + index;
            when(formTask.getProcessInstanceId()).thenReturn(processInstanceId);
            sourceTasks.add(formTask);

            ProcessInstance formProcess = mock(ProcessInstance.class);
            when(formProcess.getBusinessKey()).thenReturn("FORM_ACTION:FCI-122-" + index);
            processInstances.put(processInstanceId, formProcess);
        }

        Task dccTask = mock(Task.class);
        when(dccTask.getId()).thenReturn("task-dcc-after-form-actions");
        when(dccTask.getName()).thenReturn("文控审核");
        when(dccTask.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        when(dccTask.getProcessInstanceId()).thenReturn("pi-dcc-after-form-actions");
        when(dccTask.getCreateTime()).thenReturn(new Date(1782180000000L));
        sourceTasks.add(dccTask);

        ProcessInstance dccProcess = mock(ProcessInstance.class);
        when(dccProcess.getBusinessKey()).thenReturn("6008");
        when(dccProcess.getStartUserId()).thenReturn("501");
        processInstances.put("pi-dcc-after-form-actions", dccProcess);

        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
                .thenAnswer(invocation -> {
                    BpmTaskPageReqVO request = invocation.getArgument(1);
                    int fromIndex = Math.min((request.getPageNo() - 1) * request.getPageSize(), sourceTasks.size());
                    int toIndex = Math.min(fromIndex + request.getPageSize(), sourceTasks.size());
                    return new PageResult<>(sourceTasks.subList(fromIndex, toIndex), (long) sourceTasks.size());
                });
        when(processInstanceService.getProcessInstanceMap(any()))
                .thenAnswer(invocation -> {
                    Set<String> ids = invocation.getArgument(0);
                    Map<String, ProcessInstance> result = new HashMap<>();
                    ids.forEach(id -> result.put(id, processInstances.get(id)));
                    return result;
                });

        DccControlledFileDO file = new DccControlledFileDO();
        file.setId(6008L);
        file.setTitle("DCC-SOP-008");
        file.setFileNumber("SOP-008");
        file.setVersionNo("A");
        file.setCategoryId(7001L);
        file.setStatus("PENDING_DOC_CONTROL_REVIEW");
        when(controlledFileMapper.selectByIdIncludingDeleted(6008L)).thenReturn(file);
        when(fileCategoryMapper.selectById(7001L)).thenReturn(DccFileCategoryDO.builder()
                .id(7001L)
                .name("SOP 文件")
                .distributionRequired(Boolean.TRUE)
                .build());

        PageResult<ApprovalTaskSummary> page = adapter.page(ApprovalTaskQueryContext.of(100L,
                ApprovalTaskViewType.TODO, ApprovalModuleCode.DCC, null, 1, 10));

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("6008", page.getList().get(0).getBusinessKey());
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
        verify(bpmTaskService, org.mockito.Mockito.times(EXPECTED_DCC_PROCESS_DEFINITION_KEYS.size()))
                .getTaskDonePage(eq(100L), captor.capture());
        assertEquals(EXPECTED_DCC_PROCESS_DEFINITION_KEYS,
                captor.getAllValues().stream().map(BpmTaskPageReqVO::getProcessDefinitionKey).toList());
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
        verify(bpmTaskService, org.mockito.Mockito.times(EXPECTED_DCC_PROCESS_DEFINITION_KEYS.size()))
                .getTaskTodoPage(eq(null), any(BpmTaskPageReqVO.class));
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
        when(bpmTaskService.getTaskTodoPage(eq(100L), any(BpmTaskPageReqVO.class)))
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
