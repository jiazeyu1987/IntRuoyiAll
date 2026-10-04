package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class DccObsoleteTaskReadinessTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileMapper files;
    @Mock private DccControlledFileMasterMapper masters;
    @Mock private DccControlledFileTaskAssigneeSnapshotMapper snapshots;
    @Mock private BpmTaskService bpm;
    @Mock private RuntimeService runtime;
    @Mock private TaskService tasks;
    @Mock private DccControlledFileRouteReadinessService participants;
    @Mock private DccSignatureVerificationService signatures;
    @Mock private DccControlledFileSignatureBindingService bindings;
    private final DccControlledFileWorkflowServiceImpl workflow = new DccControlledFileWorkflowServiceImpl();
    private final DccWorkflowSignoffAssignmentService signoff = new DccWorkflowSignoffAssignmentService();
    private DccControlledFileDO file;
    private DccControlledFileTaskAssigneeSnapshotDO obligation;
    private Task task;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(workflow, "controlledFileMapper", files);
        ReflectionTestUtils.setField(workflow, "controlledFileMasterMapper", masters);
        ReflectionTestUtils.setField(workflow, "bpmTaskService", bpm);
        ReflectionTestUtils.setField(workflow, "signoffAssignmentService", signoff);
        ReflectionTestUtils.setField(signoff, "fileMapper", files);
        ReflectionTestUtils.setField(signoff, "snapshotMapper", snapshots);
        ReflectionTestUtils.setField(signoff, "bpmTaskService", bpm);
        ReflectionTestUtils.setField(signoff, "runtimeService", runtime);
        ReflectionTestUtils.setField(signoff, "taskService", tasks);
        ReflectionTestUtils.setField(signoff, "readinessService", participants);
        ReflectionTestUtils.setField(signoff, "signatureService", signatures);
        ReflectionTestUtils.setField(signoff, "signatureBindingService", bindings);
        file = DccControlledFileDO.builder().id(10L).masterId(20L).tenantId(1L)
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .processInstanceId("upload-completed").status("ACTIVE").publishedFileId(30L).build();
        when(files.selectById(10L)).thenReturn(file);
        when(files.selectByIdAndTenantForUpdate(1L, 10L)).thenReturn(file);
        when(masters.selectByIdForUpdate(20L)).thenReturn(
                DccControlledFileMasterDO.builder().id(20L).tenantId(1L).build());
        task = mock(Task.class);
        when(task.getId()).thenReturn("obsolete-task");
        when(task.getTenantId()).thenReturn("1");
        when(task.getProcessInstanceId()).thenReturn("obsolete-round-2");
        when(task.getProcessDefinitionId()).thenReturn("dcc-controlled-file-obsolete:4:definition");
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getTaskLocalVariables()).thenReturn(Map.of(
                BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID, "obsolete-round-2:dept-51"));
        when(bpm.getTask("obsolete-task")).thenReturn(task);
        when(bpm.validateTask(99L, "obsolete-task")).thenReturn(task);
        when(runtime.getVariables("obsolete-round-2")).thenReturn(Map.of(
                "systemCode", "DCC", "objectType", "CONTROLLED_FILE", "actionCode", "OBSOLETE", "objectId", 10L));
        obligation = DccControlledFileTaskAssigneeSnapshotDO.builder().id(40L).tenantId(1L)
                .controlledFileId(10L).stageCode("MATRIX_REVIEW").processInstanceId("obsolete-round-2")
                .assigneeUserId(99L).bpmTaskId("obsolete-task").assignmentSignatureId(50L)
                .assignedTime(LocalDateTime.of(2026, 10, 2, 10, 0)).build();
        when(snapshots.selectOne(any(Wrapper.class))).thenReturn(obligation);
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void signedAssigneeCanPreviewIndependentObsoleteReviewWithoutWrites() {
        var result = readiness();
        assertTrue(result.getReady());
        assertFalse(result.getFinalApproval(), "Obsolete has no doc-control artifact approval");
        assertEquals(List.of(), result.getBlockers());
        verify(bpm).validateTask(99L, "obsolete-task");
        verify(participants).requireReadyParticipants("MATRIX_REVIEW", List.of(99L));
        verifyNoInteractions(signatures, bindings, tasks);
        verify(bpm, never()).approveTask(any(), any());
        verify(bpm, never()).rejectTask(any(), any());
    }

    @Test void obsoleteApprovalDoesNotRequireSignoffAssignmentOrNativeDocControlArtifacts() {
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_APPROVAL");
        obligation.setAssignmentSignatureId(null);
        var result = readiness();
        assertTrue(result.getReady());
        assertFalse(result.getFinalApproval());
        verify(participants).requireReadyParticipants("MATRIX_APPROVAL", List.of(99L));
        verifyNoInteractions(snapshots, signatures, bindings, tasks);
    }

    @Test void unsignedOrDifferentRoundAssignmentCannotPassReadiness() {
        obligation.setAssignmentSignatureId(null);
        assertThrows(RuntimeException.class, this::readiness);
        obligation.setAssignmentSignatureId(50L);
        obligation.setProcessInstanceId("obsolete-round-1");
        assertThrows(RuntimeException.class, this::readiness);
        verifyNoInteractions(signatures, bindings, tasks);
    }

    @Test void anotherTenantCannotInspectReadiness() {
        when(task.getTenantId()).thenReturn("2");
        assertThrows(RuntimeException.class, this::readiness);
        verifyNoInteractions(signatures, bindings, tasks, participants);
    }

    @Test void participantFailurePropagatesWithoutSuccessfulReadiness() {
        doThrow(new IllegalStateException("participant disabled")).when(participants)
                .requireReadyParticipants("MATRIX_REVIEW", List.of(99L));
        var failure = assertThrows(IllegalStateException.class, this::readiness);
        assertEquals("participant disabled", failure.getMessage());
        verifyNoInteractions(signatures, bindings, tasks);
    }

    @Test void taskReassignedOrCompletedBetweenLookupAndValidationIsRejected() {
        when(bpm.validateTask(99L, "obsolete-task")).thenThrow(new IllegalStateException("task no longer assigned"));
        var failure = assertThrows(IllegalStateException.class, this::readiness);
        assertEquals("task no longer assigned", failure.getMessage());
        verifyNoInteractions(signatures, bindings, tasks, participants);
    }

    @Test void obsoleteTaskForAnotherFileCannotAuthorizeTheRequestedFile() {
        when(runtime.getVariables("obsolete-round-2")).thenReturn(Map.of(
                "systemCode", "DCC", "objectType", "CONTROLLED_FILE", "actionCode", "OBSOLETE", "objectId", 11L));
        assertServiceException(this::readiness, CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        verifyNoInteractions(signatures, bindings, tasks, participants);
    }

    @Test void differentFileTenantOrMissingControlledCopyCannotPassReadiness() {
        file.setTenantId(2L);
        assertServiceException(this::readiness, CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        file.setTenantId(1L);
        file.setPublishedFileId(null);
        assertServiceException(this::readiness, CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        verifyNoInteractions(signatures, bindings, tasks, participants);
    }

    @Test void unrelatedStageOrChangedTaskIdentityCannotPassReadiness() {
        when(task.getTaskDefinitionKey()).thenReturn("DOC_CONTROL_REVIEW");
        assertServiceException(this::readiness, CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(task.getId()).thenReturn("another-task");
        assertServiceException(this::readiness, CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        verifyNoInteractions(signatures, bindings, tasks, participants);
    }

    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessRespVO readiness() {
        var request = new DccControlledFileTaskReadinessReqVO();
        request.setTaskId("obsolete-task");
        return workflow.getTaskActionReadiness(99L, 10L, request);
    }
}
