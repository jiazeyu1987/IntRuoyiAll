package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterServiceImpl;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewCommand;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewContext;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProviderRegistry;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureImageSnapshotProvider;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordServiceImpl;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseBusinessReadinessService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Actual center -> adapter -> release -> manager binding; mapper/identity/side-effect ports are substitutes. */
class MesEdhrManagerBindingCenterPreflightTest {
    private final MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
    private final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
    private final MesProEdhrWorkTaskMapper taskRows = mock(MesProEdhrWorkTaskMapper.class);
    private final MesProEdhrReleaseTransactionEventMapper events = mock(MesProEdhrReleaseTransactionEventMapper.class);
    private final MesProEdhrBatchExecutionTaskMapper batchTasks = mock(MesProEdhrBatchExecutionTaskMapper.class);
    private final MesProductionReleaseSignoffService signoff = mock(MesProductionReleaseSignoffService.class);
    private final MesProductionReleaseBusinessReadinessService readiness = mock(MesProductionReleaseBusinessReadinessService.class);
    private final MesReleaseFlowAuditRecorder audit = mock(MesReleaseFlowAuditRecorder.class);
    private final MesEdhrBatchLifecycleGuard lifecycle = mock(MesEdhrBatchLifecycleGuard.class);
    private final MesProEdhrNonconformanceReviewService freeze = mock(MesProEdhrNonconformanceReviewService.class);
    private final ElectronicSignatureService electronicSignatures = mock(ElectronicSignatureService.class);
    private final ApprovalSignatureImageSnapshotProvider images = mock(ApprovalSignatureImageSnapshotProvider.class);
    private final ApprovalSignatureRecordServiceImpl signatureRecords = spy(new ApprovalSignatureRecordServiceImpl(electronicSignatures, images));
    private final MesProEdhrReleaseServiceImpl release = spy(new MesProEdhrReleaseServiceImpl());
    private final MesProEdhrApprovalTaskAdapter adapter = spy(new MesProEdhrApprovalTaskAdapter(tasks, release, null));
    private final ApprovalCenterServiceImpl center = new ApprovalCenterServiceImpl(
            new ApprovalTaskProviderRegistry(List.of(adapter)), mock(PermissionApi.class), mock(AdminUserApi.class), signatureRecords);
    private MockedStatic<SecurityFrameworkUtils> security;
    private MesProcessPoolActiveOrderReleaseApplicationDO application;
    private MesProEdhrWorkTaskDO task;
    private MesProEdhrReleaseTransactionDO transaction;
    private MesProEdhrBatchExecutionDO batch;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
        var manager = new MesProductionReleaseManagerApprovalServiceImpl(applications, transactions, events,
                taskRows, batches, batchTasks, signoff, readiness, audit);
        ReflectionTestUtils.setField(release, "workTaskService", tasks);
        ReflectionTestUtils.setField(release, "releaseTransactionMapper", transactions);
        ReflectionTestUtils.setField(release, "batchExecutionMapper", batches);
        ReflectionTestUtils.setField(release, "managerApprovalService", manager);
        ReflectionTestUtils.setField(release, "lifecycleGuard", lifecycle);
        ReflectionTestUtils.setField(release, "nonconformanceReviewService", freeze);
        application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(701L).setActiveOrderId(801L)
                .setWorkOrderId(301L).setBatchExecutionId(901L).setReleaseTransactionId(9200L)
                .setReleaseApprovalWorkTaskId(177L).setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING);
        application.setTenantId(1L);
        task = new MesProEdhrWorkTaskDO().setId(177L).setWorkOrderId(301L).setBatchExecutionId(901L)
                .setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(9200L)
                .setStatus(MesProEdhrWorkTaskStatus.TODO).setCandidateSourceType("ROLE_GROUP").setCandidateSourceId(77L)
                .setCandidateUserSnapshot("100").setResponsibilitySourceKey(MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
        transaction = new MesProEdhrReleaseTransactionDO().setId(9200L).setWorkOrderId(301L)
                .setBatchExecutionId(901L).setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL).setVersion(3);
        batch = new MesProEdhrBatchExecutionDO().setId(901L).setWorkOrderId(301L).setTenantId(1L);
        when(tasks.validateReleaseApprovalTask(177L, null)).thenReturn(task);
        when(transactions.selectById(9200L)).thenReturn(transaction);
        when(batches.selectById(901L)).thenReturn(batch);
        when(applications.selectListByReleaseTransactionId(9200L)).thenReturn(List.of(application));
    }

    @AfterEach
    void cleanup() {
        security.close();
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, 302L})
    void invalidFormalApplicationWorkOrderStopsActualCenterBeforeSignatureImageOrBusinessWrites(Long workOrderId) {
        application.setWorkOrderId(workOrderId);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> center.reviewTask(100L, command()));

        assertEquals("EDHR_MARKET_RELEASE_SIGNATURE_BINDING_INVALID", failure.getMessage());
        assertEquals(workOrderId, application.getWorkOrderId(), "no association may be filled from another owner");
        verify(adapter).preflightReview(any());
        verify(adapter, never()).review(any());
        verify(release).preflightApprovalCenterReview(100L, 177L);
        verify(release, never()).approve(any());
        assertReadOnlyGraphAndNoSignaturePorts();
    }

    @Test
    void completeMatchingGraphPassesActualAdapterReleaseAndManagerPreflightWithoutWrites() {
        adapter.preflightReview(ApprovalTaskReviewContext.of(100L, ApprovalModuleCode.EDHR, "EDHR_WORK_TASK",
                "177", "177", null, ApprovalTaskReviewResult.APPROVE, "reviewed", "test-only", false));

        verify(release).preflightApprovalCenterReview(100L, 177L);
        assertReadOnlyGraphAndNoSignaturePorts();
    }

    private void assertReadOnlyGraphAndNoSignaturePorts() {
        verify(tasks).validateReleaseApprovalTask(177L, null);
        verify(transactions).selectById(9200L);
        verify(batches).selectById(901L);
        verify(applications).selectListByReleaseTransactionId(9200L);
        verify(lifecycle).requireReleaseAllowed(901L);
        verify(freeze).ensureBatchNotFrozen(901L, "上市放行");
        verifyNoMoreInteractions(tasks, transactions, batches, applications, lifecycle, freeze);
        verifyNoInteractions(signatureRecords, electronicSignatures, images, signoff, readiness, audit,
                taskRows, events, batchTasks);
    }

    private ApprovalTaskReviewCommand command() {
        return new ApprovalTaskReviewCommand().setModuleCode(ApprovalModuleCode.EDHR).setSourceTaskType("EDHR_WORK_TASK")
                .setSourceTaskId("177").setBusinessKey("177").setResult(ApprovalTaskReviewResult.APPROVE)
                .setReason("reviewed").setSignaturePassword("test-only");
    }
}
