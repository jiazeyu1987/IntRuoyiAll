package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness=Strictness.LENIENT)
class DccWorkflowTaskIdentityTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileMapper controlledFileMapper;
    @Mock private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Mock private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock private DccControlledFileTaskAssigneeSnapshotMapper taskAssigneeSnapshotMapper;
    @Mock private BpmTaskService bpmTaskService;
    @Mock private PermissionApi permissionApi;
    @Mock private DccSignatureVerificationService signatureVerificationService;
    @InjectMocks private DccControlledFileWorkflowServiceImpl service;
    private DccControlledFileDO locked;
    private DccControlledFileMasterDO master;
    private Task task;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        var initial=file();locked=file();
        master=DccControlledFileMasterDO.builder().id(10L).tenantId(1L).build();
        when(controlledFileMapper.selectById(42L)).thenReturn(initial);
        when(controlledFileMapper.selectByIdAndTenantForUpdate(1L,42L)).thenAnswer(ignored->locked);
        when(controlledFileMasterMapper.selectByIdForUpdate(10L)).thenAnswer(ignored->master);
        task=mock(Task.class);when(task.getId()).thenReturn("approval-task");
        when(task.getProcessInstanceId()).thenReturn("approval-round");
        when(task.getTaskDefinitionKey()).thenReturn("MATRIX_APPROVAL");
        when(task.getTenantId()).thenReturn("1");when(task.getAssignee()).thenReturn("99");
        when(bpmTaskService.validateTask(99L,"approval-task")).thenReturn(task);
        when(routeSnapshotMapper.selectListByControlledFileId(42L)).thenReturn(List.of(
                DccControlledFileRouteSnapshotDO.builder().controlledFileId(42L).stageCode("MATRIX_APPROVAL")
                        .resolvedUserIds("99").build()));
        when(permissionApi.hasAnyPermissions(any(),any(String[].class))).thenReturn(true);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void exactTenantMasterFileRoundAndTaskCanReadApprovalReadiness() {
        assertTrue(readiness().getReady());
    }
    @Test void aForeignTaskTenantCannotPassDomainApprovalReadiness() {
        when(task.getTenantId()).thenReturn("122");denied();
    }
    @Test void aMissingTaskTenantCannotBeInferredFromTheFile() {
        when(task.getTenantId()).thenReturn(null);denied();
    }
    @Test void aForeignMasterTenantCannotAuthorizeTheLockedVersion() {
        master.setTenantId(122L);denied();
    }
    @Test void aForeignLockedFileTenantCannotUseThePrelockIdentity() {
        locked.setTenantId(122L);denied();
    }
    @Test void aChangedMasterAfterLockingCannotUseThePrelockMaster() {
        locked.setMasterId(20L);denied();
    }
    @Test void aDifferentLockedFileCannotUseTheRequestedFileId() {
        locked.setId(43L);denied();
    }
    @Test void aChangedWorkflowActionAfterLockingCannotUseThePrelockProcessKey() {
        locked.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION);denied();
    }
    @Test void aDifferentTaskCannotUseTheRequestedTaskId() {
        when(task.getId()).thenReturn("another-task");denied();
    }
    @Test void approveEntryRejectsTheForeignTaskBeforeAnySignature() {
        when(task.getTenantId()).thenReturn("122");
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO();
        request.setTaskId("approval-task");request.setPassword("test-only");request.setReason("批准意见");
        assertServiceException(()->service.approveTask(99L,42L,request),CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        assertNoActionWrites();
    }
    @Test void rejectEntryRejectsTheForeignTaskBeforeAnySignature() {
        when(task.getTenantId()).thenReturn("122");
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO();
        request.setTaskId("approval-task");request.setPassword("test-only");request.setReason("驳回意见");
        assertServiceException(()->service.rejectTask(99L,42L,request),CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        assertNoActionWrites();
    }
    private void denied() {
        assertServiceException(this::readiness,CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED);
        assertNoActionWrites();
    }
    private void assertNoActionWrites() {
        verifyNoInteractions(signatureVerificationService);
        verifyNoInteractions(taskAssigneeSnapshotMapper);
        verify(controlledFileMapper,never()).updateById(any(DccControlledFileDO.class));
        verify(bpmTaskService,never()).approveTask(any(),any());
        verify(bpmTaskService,never()).rejectTask(any(),any());
    }
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTaskReadinessRespVO readiness() {
        var request=new DccControlledFileTaskReadinessReqVO();request.setTaskId("approval-task");
        return service.getTaskActionReadiness(99L,42L,request);
    }
    private DccControlledFileDO file() {
        return DccControlledFileDO.builder().id(42L).masterId(10L).tenantId(1L).categoryId(20L)
                .processInstanceId("approval-round").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .status("PENDING_MATRIX_APPROVAL").versionNo("A/1").build();
    }
}
