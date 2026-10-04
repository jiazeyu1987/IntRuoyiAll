package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseAuthoritativeContextPort;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationAction;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFinalizationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerApprovalService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MesProEdhrNcrReleaseGateTest {
    private final MesProEdhrReleaseServiceImpl release = new MesProEdhrReleaseServiceImpl();
    private final MesProEdhrNonconformanceReviewServiceImpl reviews = new MesProEdhrNonconformanceReviewServiceImpl();
    private final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
    private final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesProEdhrNonconformanceReviewMapper reviewMapper = mock(MesProEdhrNonconformanceReviewMapper.class);
    private final MesProBatchRecordExecutionSignatureService signatures = mock(MesProBatchRecordExecutionSignatureService.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final MesReleaseAuthoritativeContextPort authority = mock(MesReleaseAuthoritativeContextPort.class);
    private final MesProductionReleaseManagerApprovalService managerApprovalService =
            mock(MesProductionReleaseManagerApprovalService.class);

    private final cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper changes = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper.class);

    @BeforeEach
    void setUp() {
        var lifecycle = new MesEdhrBatchLifecycleGuard();
        ReflectionTestUtils.setField(lifecycle, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(lifecycle, "changeMapper", changes);
        ReflectionTestUtils.setField(lifecycle, "batchMapper", batches);
        ReflectionTestUtils.setField(release, "lifecycleGuard", lifecycle);
        ReflectionTestUtils.setField(release, "gxpAuditService", mock(GxpAuditService.class));
        ReflectionTestUtils.setField(release, "releaseTransactionMapper", transactions);
        ReflectionTestUtils.setField(release, "batchExecutionMapper", batches);
        ReflectionTestUtils.setField(release, "nonconformanceReviewService", reviews);
        ReflectionTestUtils.setField(release, "executionSignatureService", signatures);
        ReflectionTestUtils.setField(release, "adminUserApi", users);
        ReflectionTestUtils.setField(release, "authoritativeContextPort", authority);
        ReflectionTestUtils.setField(release, "managerApprovalService", managerApprovalService);
        ReflectionTestUtils.setField(reviews, "reviewMapper", reviewMapper);
        ReflectionTestUtils.setField(reviews, "batchExecutionMapper", batches);
        ReflectionTestUtils.setField(reviews, "batchExecutionOriginMapper", origins);
        var transaction = new MesProEdhrReleaseTransactionDO().setId(11L).setBatchExecutionId(22L)
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL).setVersion(1);
        when(transactions.selectById(11L)).thenReturn(transaction);
        when(transactions.selectByIdForUpdate(11L)).thenReturn(transaction);
        when(batches.selectById(22L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(22L).setWorkOrderId(44L));
        when(origins.selectListByBatchExecutionId(22L)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setActiveOrderId(33L)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void pendingVoidRejectsApproveAndFinalizerBeforeSignatureOrAuthority(boolean direct) {
        when(changes.selectCount(any())).thenReturn(1L);
        try (var login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);
            var error = assertThrows(ServiceException.class, () -> {
                if (direct) release.finalizeRelease(command()); else release.approve(request());
            });
            assertTrue(error.getMessage().contains("作废申请"));
        }
        verifyNoInteractions(signatures, users, authority);
        verify(transactions, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
    }

    @ParameterizedTest
    @CsvSource({"pending_review,false,false", "closed,false,false", "pending_review,true,false", "closed,true,false",
            "pending_review,false,true", "closed,false,true", "pending_review,true,true", "closed,true,true"})
    void pendingOrVoidReviewWithoutBatchIdBlocksBeforeSignatureAndMutation(
            String status, boolean directFinalize, boolean workOrderRelation) {
        var review = MesProEdhrNonconformanceReviewDO.builder().id(55L).reviewCode("BHGSP-202609-00000000")
                .sourceType("ACTIVE_ORDER").sourceId(33L).activeOrderId(33L).workOrderId(44L)
                .reviewStatus(status).disposition("closed".equals(status) ? "void" : null)
                .nonconformanceReason("test review").build();
        assertNull(review.getBatchExecutionId());
        if (workOrderRelation) {
            when(origins.selectListByBatchExecutionId(22L)).thenReturn(List.of());
            when(reviewMapper.selectFirstBlockingByWorkOrderId(44L)).thenReturn(review);
        } else {
            when(reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(33L)).thenReturn(review);
        }
        try (MockedStatic<SecurityFrameworkUtils> login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);
            ServiceException error = assertThrows(ServiceException.class,
                    () -> { if (directFinalize) release.finalizeRelease(command()); else release.approve(request()); });
            assertTrue(error.getMessage().contains("BHGSP-202609-00000000"));
        }
        verifyNoInteractions(signatures, users, authority);
        verify(transactions, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
        verify(transactions, never()).approveProductionRelease(any(), any(), any(), any(), any(), any(), any(), any());
        verify(batches, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }

    @Test
    void orderWithoutBlockingReviewReachesNormalSignatureAndAuthoritativeChecks() {
        var nextStep = new IllegalStateException("authoritative release check reached");
        when(authority.require(any())).thenThrow(nextStep);
        when(signatures.recordMarketReleaseSignature(anyLong(), anyLong(), anyLong(), anyString(), any()))
                .thenReturn(77L);
        try (MockedStatic<SecurityFrameworkUtils> login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);
            assertSame(nextStep, assertThrows(IllegalStateException.class, () -> release.approve(request())));
        }
        verify(signatures).recordMarketReleaseSignature(66L, 22L, 11L, "test-password", null);
        verify(authority).require(any());
    }

    @Test
    void managedFinalizationReplayStillChecksReviewFreezeBeforeManagerBranch() {
        when(transactions.selectByIdForUpdate(11L)).thenReturn(new MesProEdhrReleaseTransactionDO()
                .setId(11L).setBatchExecutionId(22L).setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_RELEASED)
                .setVersion(2));
        when(managerApprovalService.isManagedReleaseTransaction(11L)).thenReturn(true);
        when(origins.selectListByBatchExecutionId(22L)).thenReturn(List.of());
        var review = MesProEdhrNonconformanceReviewDO.builder().id(56L)
                .reviewCode("BHGSP-202609-00000001")
                .sourceType("ACTIVE_ORDER").sourceId(33L).activeOrderId(33L).workOrderId(44L)
                .reviewStatus(MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW)
                .nonconformanceReason("pending review").build();
        when(reviewMapper.selectFirstBlockingByWorkOrderId(44L)).thenReturn(review);

        try (MockedStatic<SecurityFrameworkUtils> login = mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(66L);
            ServiceException error = assertThrows(ServiceException.class,
                    () -> release.finalizeRelease(command()));
            assertTrue(error.getMessage().contains("BHGSP-202609-00000001"));
        }
        verify(managerApprovalService, never()).isManagedReleaseTransaction(11L);
        verifyNoInteractions(signatures, users, authority);
    }

    @Test
    void batchDetailFrozenProjectionIncludesActiveOrderReviewWithoutDirectBatchLink() {
        var review = MesProEdhrNonconformanceReviewDO.builder().id(55L)
                .reviewCode("BHGSP-202609-00000000")
                .sourceType("ACTIVE_ORDER").sourceId(33L).activeOrderId(33L).workOrderId(44L)
                .reviewStatus(MesProEdhrNonconformanceReviewService.STATUS_PENDING_REVIEW)
                .nonconformanceReason("active order review").build();
        when(reviewMapper.selectFirstBlockingPqcSubmissionByActiveOrderId(33L)).thenReturn(review);

        assertTrue(reviews.isBatchFrozen(22L));
    }

    private MesProEdhrReleaseApproveReqVO request() {
        return new MesProEdhrReleaseApproveReqVO()
                .setReleaseTransactionId(11L)
                .setPassword("test-password")
                .setIdempotencyKey("ncr-gate-test");
    }

    private MesReleaseFinalizationCommand command() {
        return new MesReleaseFinalizationCommand()
                .setReleaseTransactionId(11L)
                .setAction(MesReleaseFinalizationAction.APPROVE)
                .setMaterialGateRequired(false);
    }
}
