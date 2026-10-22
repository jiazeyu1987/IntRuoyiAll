package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Offline behavior tests: real W06 services, mocked external boundaries, no database or runtime writes. */
class DccW06TrainingLifecycleTest {
    private final LocalDateTime now = LocalDateTime.of(2026,10,11,12,0);
    private DccOfflineTrainingRecordService training;
    private DccControlledFileMapper files;
    private AdminUserApi users;
    private PermissionApi permissions;
    private RoleApi roles;
    private DccControlledFileCategoryPermissionSupport categories;
    private DccControlledFileAssignmentScopeService scope;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        files=mock(DccControlledFileMapper.class);users=mock(AdminUserApi.class);
        permissions=mock(PermissionApi.class);roles=mock(RoleApi.class);
        categories=mock(DccControlledFileCategoryPermissionSupport.class);
        scope=mock(DccControlledFileAssignmentScopeService.class);
        training=spy(new DccOfflineTrainingRecordService());
        wire(training,"files",files,"users",users,"permissions",permissions,
                "categories",categories,"fileScope",scope,"projectAssignments",mock(DccProjectCodeAssignmentMapper.class));
        when(users.getUser(99L)).thenReturn(account(99L));when(users.getUser(77L)).thenReturn(account(77L));
        when(permissions.hasAnyPermissions(99L,"dcc:controlled-file:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(77L,"dcc:controlled-file:query")).thenReturn(true);
        when(categories.hasCategoryPermission(2L,99L,DccFileCategoryPermissionActionEnum.TRAINING_RECORD)).thenReturn(true);
        when(scope.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        doAnswer(call->new DccOfflineTrainingRecordService.Waiting(call.getArgument(0),"round-1","execution-1"))
                .when(training).requireWaiting(any(DccControlledFileDO.class));
    }
    @AfterEach void clearContext() {TenantContextHolder.clear();TransactionSynchronizationManager.clear();}

    @Test void uploaderWithoutDocControlCanSubmitAndDiscoverOwnRecordTask() {
        var file=waiting();when(files.selectOfflineTrainingCandidates(1L)).thenReturn(List.of(file));
        assertTrue(training.canUpload(99L,file));
        assertEquals(List.of(file),training.listForActor(99L));
        assertEquals(List.of(99L),training.requireConfiguredRecipients(file));
        verifyNoInteractions(roles);
    }
    @Test void confirmedOwnerCannotReplaceUploaderButCanReadThisRoundProgress() {
        var file=waiting();
        assertFalse(training.canUpload(77L,file));
        assertThrows(IllegalStateException.class,()->training.requireUpload(77L,file));
        assertTrue(training.canReadProgress(77L,file));
        file.setFileOwnerProcessInstanceId("other-round");
        assertFalse(training.canReadProgress(77L,file));
    }
    @Test void organizationTaskIsExactRoundAndDoesNotGrantUpload() {
        var file=waiting();when(files.selectOfflineTrainingCandidates(1L)).thenReturn(List.of(file));
        assertTrue(training.canOrganizeTraining(77L,file));assertEquals(List.of(file),training.listForActor(77L));
        assertFalse(training.canUpload(77L,file));
        file.setFileOwnerSignatureId(null);assertFalse(training.canOrganizeTraining(77L,file));
        file.setFileOwnerSignatureId(8L);file.setFileOwnerProcessInstanceId("other-round");
        assertFalse(training.canOrganizeTraining(77L,file));
        file.setFileOwnerProcessInstanceId("round-1");users.getUser(77L).setStatus(1);
        assertFalse(training.canOrganizeTraining(77L,file));
    }
    @Test void trainingBodyRequiresConfirmedOwnerAndActiveRealReceiveTask() {
        var file=waiting();
        assertEquals(true,ReflectionTestUtils.invokeMethod(training,"canReadTrainingBody",77L,file));
        assertEquals(false,ReflectionTestUtils.invokeMethod(training,"canReadTrainingBody",99L,file));
        file.setTrainingRecordFileId(111L);file.setStatus("CONTROLLED_PENDING_EFFECTIVE");
        assertTrue(training.canReadProgress(77L,file));
        assertEquals(false,ReflectionTestUtils.invokeMethod(training,"canReadTrainingBody",77L,file));
        file.setTrainingRecordFileId(null);file.setStatus(DccOfflineTrainingRecordService.STATUS);
        doThrow(new IllegalStateException("DCC_OFFLINE_TRAINING_EXECUTION_REQUIRED")).when(training).requireWaiting(file);
        assertThrows(IllegalStateException.class,()->ReflectionTestUtils.invokeMethod(training,"canReadTrainingBody",77L,file));
    }
    @Test void futureControlledVersionCannotUseLegacyManualDistributionEntry() {
        var service=new DccControlledFileFinalizationServiceImpl();var tx=mock(org.springframework.transaction.support.TransactionTemplate.class);
        doAnswer(call->{call.<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>getArgument(0)
                .accept(new SimpleTransactionStatus());return null;}).when(tx).executeWithoutResult(any());
        var file=waiting();file.setStatus("CONTROLLED_PENDING_EFFECTIVE");file.setControlledTime(now);
        file.setTrainingRecordFileId(111L);file.setDistributionPayloadHash("a".repeat(64));
        when(files.selectById(20L)).thenReturn(file);when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(file);
        when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(categories.hasCategoryPermission(2L,99L,DccFileCategoryPermissionActionEnum.DISTRIBUTE)).thenReturn(true);
        var categoryRows=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);
        when(categoryRows.selectById(2L)).thenThrow(new AssertionError("pending version reached distribution preparation"));
        wire(service,"controlledFileMapper",files,"transactionTemplate",tx,"permissionApi",permissions,
                "permissionSupport",categories,"categoryMapper",categoryRows);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.releaseManualDistribution(99L,20L));
        verifyNoInteractions(categoryRows);
    }
    @Test void disabledUploaderAndMissingCategoryDutyAreExplicitlyRejected() {
        var file=waiting();users.getUser(99L).setStatus(1);
        assertFalse(training.canUpload(99L,file));
        assertThrows(RuntimeException.class,()->training.requireConfiguredRecipients(file));
        users.getUser(99L).setStatus(0);
        when(categories.hasCategoryPermission(2L,99L,DccFileCategoryPermissionActionEnum.TRAINING_RECORD)).thenReturn(false);
        assertFalse(training.canUpload(99L,file));
        assertThrows(RuntimeException.class,()->training.requireConfiguredRecipients(file));
    }
    @Test void pastDateStillRejectsANewOrChangedApplicationBeforeAnyLifecycleWrite() {
        var date=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return now;}};
        var file=waiting();file.setEffectiveMode(DccWorkflowDatePolicy.PRESET_DATE);
        file.setEffectiveDate(now.toLocalDate().minusDays(2));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->date.requireEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate()));
        assertNull(file.getControlledTime());assertNull(file.getActivatedTime());
        verify(files,never()).updateById(any(DccControlledFileDO.class));
    }
    @Test void committedControlCannotBeOverwrittenByLateFinalizationFailure() {
        var tx=mock(PlatformTransactionManager.class);when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var file=waiting();file.setStatus("CONTROLLED_PENDING_EFFECTIVE");file.setControlledTime(now);
        when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(file);
        var platform=mock(DccControlledContentAdapter.class);
        var service=new DccControlledFileFinalizationFailureService(tx,files,platform);
        assertDoesNotThrow(()->service.recordFailure(1L,20L,"FINALIZATION_FAILED",99L,"late attempt","retry-1"));
        verify(files,never()).markFinalizationFailedWhenStatus(anyLong(),anyLong(),anyString(),anyString(),anyLong());
        verifyNoInteractions(platform);
    }
    @Test void retryRereadsUnderMasterThenVersionLockAndReturnsCompletedControl() {
        var service=new DccControlledFileFinalizationServiceImpl();var masters=mock(DccControlledFileMasterMapper.class);
        var stale=waiting();stale.setStatus("FINALIZATION_FAILED");
        var committed=waiting();committed.setStatus("CONTROLLED_PENDING_EFFECTIVE");committed.setControlledTime(now);
        when(files.selectById(20L)).thenReturn(stale);
        when(masters.selectByIdForUpdate(3L)).thenReturn(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).build());
        when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(committed);
        wire(service,"controlledFileMapper",files,"controlledFileMasterMapper",masters);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(service,"finalizeRevision",20L,false,99L,"retry-1",true));
        var order=inOrder(files,masters);order.verify(files).selectById(20L);
        order.verify(masters).selectByIdForUpdate(3L);order.verify(files).selectByIdAndTenantForUpdate(1L,20L);
        verify(files,never()).updateById(any(DccControlledFileDO.class));
    }
    @Test void reusingBoundStampPreservesOriginalStampTime() {
        var service=new DccControlledFileFinalizationServiceImpl();var file=waiting();
        file.setPublishedFileId(111L);file.setStampedFileId(111L);file.setStampedTime(now.minusDays(1));
        Object artifact=ReflectionTestUtils.invokeMethod(service,"resolveStampedPublishedArtifact",file,false);
        assertEquals(file.getStampedTime(),ReflectionTestUtils.invokeMethod(artifact,"stampedTime"));
    }
    private DccControlledFileDO waiting() {
        return DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).categoryId(2L).dccProjectCodeId(4L)
                .requesterId(99L).needTraining(true).approvedTime(now.minusDays(3)).versionNo("B/1")
                .processInstanceId("round-1").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .fileOwnerUserId(77L).fileOwnerSignatureId(8L).fileOwnerApprovalTaskId("approval-task-1")
                .fileOwnerProcessInstanceId("round-1").fileOwnerSelectedTime(now.minusDays(3))
                .status(DccOfflineTrainingRecordService.STATUS).build();
    }
    private AdminUserRespDTO account(Long id) {return new AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0).setNickname("account-"+id);}
    private static void wire(Object bean,Object... fields) {for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);}
}
