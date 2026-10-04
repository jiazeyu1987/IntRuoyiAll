package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.flowable.engine.TaskService;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;

/** D-owned combination regression that identifies the exact A signed payload integration gate. */
class DccSignedRelationAssignmentContractTest extends BaseMockitoUnitTest {
    @InjectMocks DccWorkflowSignoffAssignmentService service;
    @Mock DccControlledFileMapper fileMapper;@Mock DccControlledFileMasterMapper masterMapper;
    @Mock DccControlledFileTaskAssigneeSnapshotMapper snapshotMapper;@Mock BpmTaskService bpmTaskService;
    @Mock TaskService taskService;@Mock RuntimeService runtimeService;@Mock AdminUserApi adminUserApi;
    @Mock DccControlledFileRouteReadinessService readinessService;@Mock DccSignatureVerificationService signatureService;
    @Mock DccControlledFileSignatureBindingService signatureBindingService;
    @Mock cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService remediationService;
    DccControlledFileTaskAssigneeSnapshotDO row;
    @BeforeEach void prepare(){
        TenantContextHolder.setTenantId(1L);
        var file=DccControlledFileDO.builder().id(10L).masterId(20L).tenantId(1L).processInstanceId("round-1").status("PENDING_MATRIX_REVIEW").build();
        when(fileMapper.selectById(10L)).thenReturn(file);when(fileMapper.selectByIdAndTenantForUpdate(1L,10L)).thenReturn(file);
        when(masterMapper.selectByIdForUpdate(20L)).thenReturn(DccControlledFileMasterDO.builder().id(20L).tenantId(1L).build());
        var task=mock(Task.class);when(task.getId()).thenReturn("task-51");when(task.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");when(task.getProcessInstanceId()).thenReturn("round-1");when(task.getTenantId()).thenReturn("1");
        when(task.getTaskLocalVariables()).thenReturn(Map.of(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID,"obligation-51"));
        when(bpmTaskService.getTask("task-51")).thenReturn(task);when(bpmTaskService.validateTask(7L,"task-51")).thenReturn(task);
        row=DccControlledFileTaskAssigneeSnapshotDO.builder().id(1L).controlledFileId(10L).stageCode("MATRIX_REVIEW").departmentId(51L).leaderUserId(7L).assigneeUserId(7L).build();
        when(snapshotMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenAnswer(i->row);
        when(snapshotMapper.updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class))).thenReturn(1);
        when(adminUserApi.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L).setStatus(0).setDeptId(51L).setNickname("部门负责人").setPostIds(Set.of(10L)));
        when(signatureService.verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),eq("ASSIGN"),any(),any()))
                .thenReturn(DccUnifiedSignatureResult.builder().signatureId(7L).evidenceStatus("VALID").build());
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    DccSignoffAssignmentReqVO request(long responsible){
        var input=new DccSignoffAssignmentReqVO();input.setTaskId("task-51");input.setAssigneeUserId(7L);input.setPassword("isolated-test-password");input.setReason("会签指派");
        input.setRelationArrangements(List.of(new Arrangement(30L,responsible,LocalDateTime.of(2026,10,5,12,0))));return input;
    }
    @Test void changedRemediationAssigneeCannotReplayAnAlreadySignedAssignment(){
        service.assign(7L,10L,request(8L));var capture=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);
        verify(snapshotMapper).updateById(capture.capture());var saved=capture.getValue();
        row.setAssignmentSignatureId(saved.getAssignmentSignatureId());row.setAssignmentPayloadHash(saved.getAssignmentPayloadHash());row.setBpmTaskId("task-51");row.setAssignedTime(LocalDateTime.now());
        var failure=assertThrows(ServiceException.class,()->service.assign(7L,10L,request(9L)),"changed selected remediation actor must not reuse the old signature");
        assertEquals(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED.getCode(),failure.getCode());
        verify(remediationService,times(1)).saveArrangements(eq(7L),eq(10L),eq("round-1"),eq(request(8L).getRelationArrangements()),eq("会签指派"));
        verify(taskService,times(1)).setAssignee("task-51","7");
    }
    void persistSignedPayloadInFixture(){
        var capture=ArgumentCaptor.forClass(DccControlledFileTaskAssigneeSnapshotDO.class);verify(snapshotMapper).updateById(capture.capture());
        var saved=capture.getValue();row.setAssignmentSignatureId(saved.getAssignmentSignatureId());
        row.setAssignmentPayloadHash(saved.getAssignmentPayloadHash());row.setBpmTaskId("task-51");row.setAssignedTime(LocalDateTime.now());
    }
    @Test void changedDeadlineIsRejectedByTheSignedPayloadBeforeAnySecondArrangementWrite(){
        service.assign(7L,10L,request(8L));persistSignedPayloadInFixture();var changed=request(8L);
        changed.setRelationArrangements(List.of(new Arrangement(30L,8L,LocalDateTime.of(2026,10,6,12,0))));
        var failure=assertThrows(ServiceException.class,()->service.assign(7L,10L,changed));
        assertEquals(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED.getCode(),failure.getCode());
        verify(remediationService,times(1)).saveArrangements(any(),any(),any(),any(),any());verify(taskService,times(1)).setAssignee(any(),any());
    }
    @Test void equivalentArrangementOrderReplaysOneSignatureAndOneSaveBeforeTaskTransfer(){
        var first=request(8L);var earlier=new Arrangement(29L,9L,LocalDateTime.of(2026,10,5,11,0));
        first.setRelationArrangements(List.of(first.getRelationArrangements().get(0),earlier));service.assign(7L,10L,first);persistSignedPayloadInFixture();
        var order=inOrder(signatureService,snapshotMapper,remediationService,taskService);
        order.verify(signatureService).verifyPasswordAndCreateWorkflowSignature(eq(7L),eq(10L),eq("task-51"),eq("round-1"),eq("MATRIX_REVIEW"),eq("ASSIGN"),any(),contains(row.getAssignmentPayloadHash()));
        order.verify(snapshotMapper).updateById(any(DccControlledFileTaskAssigneeSnapshotDO.class));
        order.verify(remediationService).saveArrangements(7L,10L,"round-1",List.of(earlier,first.getRelationArrangements().get(0)),"会签指派");
        order.verify(taskService).setAssignee("task-51","7");
        var replay=request(8L);replay.setRelationArrangements(List.of(earlier,replay.getRelationArrangements().get(0)));
        assertDoesNotThrow(()->service.assign(7L,10L,replay));
        verify(signatureService,times(1)).verifyPasswordAndCreateWorkflowSignature(any(),any(),any(),any(),any(),any(),any(),any());
        verify(remediationService,times(1)).saveArrangements(any(),any(),any(),any(),any());verify(taskService,times(1)).setAssignee(any(),any());
    }
    @Test void arrangementFailureStopsTaskTransferAndPropagatesTheActualDomainFailure(){
        var failure=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_ARRANGEMENT_CONFLICT_REQUIRES_COORDINATION");
        doThrow(failure).when(remediationService).saveArrangements(any(),any(),any(),any(),any());
        assertSame(failure,assertThrows(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure.class,()->service.assign(7L,10L,request(8L))));
        verifyNoInteractions(taskService);
    }
}
