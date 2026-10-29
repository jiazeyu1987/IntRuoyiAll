package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** A single-record request must never authorize a batch void or silently bypass its reviewer. */
@ExtendWith(MockitoExtension.class)
class MesEdhrSingleRecordVoidTest {
    @InjectMocks private MesProEdhrRecordChangeServiceImpl service;
    @Mock private MesProBatchRecordExecutionMapper executionMapper;
    @Mock private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Mock private MesProEdhrRecordChangeEventMapper eventMapper;
    @Mock private MesProEdhrReleaseTransactionMapper releaseMapper;
    @Mock private MesProBatchRecordExecutionSignatureService signatureService;
    @Mock private MesEdhrRecordChangeTaskService taskService;
    @Mock private MesMarketReleaseAccess marketReleaseAccess;
    @Mock private MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    @Mock private MesProEdhrWorkTaskService workTaskService;
    @Mock private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;
    private MockedStatic<SecurityFrameworkUtils> security;
    private MesProBatchRecordExecutionDO execution;

    @BeforeEach void setUp() {
        TenantContextHolder.setTenantId(1L);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(10L);
        execution = new MesProBatchRecordExecutionDO().setId(30L).setBatchExecutionId(40L)
                .setStatus(3).setActiveRevisionFlag(true).setFieldAuditHeadHash("head-1");
    }
    @AfterEach void tearDown() { security.close(); TenantContextHolder.clear(); }

    private EdhrRecordChangeRequestReqVO request() {
        return new EdhrRecordChangeRequestReqVO().setBatchExecutionId(40L).setExecutionId(30L)
                .setApproverUserId(20L).setReasonCategory("DATA_ERROR").setReasonText("已确认记录证据错误")
                .setPassword("request-password");
    }
    private void source() {
        when(executionMapper.selectById(30L)).thenReturn(execution);
        when(executionMapper.selectByIdForUpdate(30L)).thenReturn(execution);
    }
    private MesProEdhrRecordChangeEventDO event() {
        return new MesProEdhrRecordChangeEventDO().setId(50L).setChangeCode("VOID-50")
                .setChangeType("VOID").setTargetScope("EXECUTION").setChangeStatus("SUBMITTED")
                .setExecutionId(30L).setBatchExecutionId(40L).setRequestedBy(10L)
                .setPreviousStatus("3").setPreviousHeadHash("head-1").setReasonText("证据错误");
    }

    @Test void requestCreatesSameFormalApprovalTaskAndPreservesConfirmedRecord() {
        source();
        when(signatureService.recordSubmitSignature(eq(30L), anyString(), nullable(String.class))).thenReturn(61L);
        when(eventMapper.insert(any(MesProEdhrRecordChangeEventDO.class))).thenAnswer(call -> {
            ((MesProEdhrRecordChangeEventDO) call.getArgument(0)).setId(50L); return 1;
        });
        var response = service.requestVoidExecution(request());
        assertEquals("VOID", response.getChangeType());
        assertEquals("EXECUTION", response.getTargetScope());
        assertEquals(40L, response.getBatchExecutionId());
        assertEquals(61L, response.getRequestSignatureId());
        verify(taskService).validateApprover(20L, 10L);
        verify(taskService).assign(argThat(e -> e.getId().equals(50L) && e.getExecutionId().equals(30L)), eq(20L));
        verify(executionMapper, never()).updateById(any(MesProBatchRecordExecutionDO.class));
        verify(archiveMapper, never()).updateById(any(MesProBatchRecordExecutionArchiveDO.class));
    }

    @Test void requestRejectsCrossBatchRecordBeforeSigningOrAssignment() {
        source();
        assertThrows(IllegalArgumentException.class, () -> service.requestVoidExecution(request().setBatchExecutionId(41L)));
        verifyNoInteractions(signatureService, taskService, eventMapper);
    }

    @Test void requestRejectsSupersededRevision() {
        source(); execution.setActiveRevisionFlag(false).setSupersededByExecutionId(31L);
        assertThrows(RuntimeException.class, () -> service.requestVoidExecution(request()));
        verifyNoInteractions(signatureService, taskService, eventMapper);
    }

    @Test void requestCannotHideMissingSignature() {
        source();
        assertThrows(IllegalStateException.class, () -> service.requestVoidExecution(request()));
        verify(eventMapper, never()).insert(any(MesProEdhrRecordChangeEventDO.class));
    }

    @Test void approvalRequiresSelectedIndependentReviewerBeforeSigning() {
        var event = event();
        when(eventMapper.selectByIdAndTenantForUpdate(50L, 1L)).thenReturn(event);
        doThrow(new IllegalArgumentException("只有指定审批人可以审批")).when(taskService).requireReviewer(event);
        assertThrows(IllegalArgumentException.class, () -> service.approveVoidExecution(new EdhrRecordChangeApproveReqVO().setChangeEventId(50L)));
        verifyNoInteractions(signatureService, executionMapper, archiveMapper);
    }

    @Test void batchVoidCannotBeProcessedThroughSingleRecordApprovalOrDecision() {
        when(eventMapper.selectByIdAndTenantForUpdate(50L, 1L)).thenReturn(event().setTargetScope("BATCH").setExecutionId(null));
        assertThrows(RuntimeException.class, () -> service.approveVoidExecution(new EdhrRecordChangeApproveReqVO().setChangeEventId(50L)));
        assertThrows(RuntimeException.class, () -> service.decide(new EdhrRecordChangeDecideReqVO().setChangeEventId(50L)
                .setDecision("REJECTED").setReason("不得绕过正式整批流程").setPassword("review-password")));
        verifyNoInteractions(taskService, signatureService, executionMapper, archiveMapper);
    }

    @Test void approvalRejectsChangedContentSnapshot() {
        var event = event();
        when(eventMapper.selectByIdAndTenantForUpdate(50L, 1L)).thenReturn(event);
        when(executionMapper.selectByIdForUpdate(30L)).thenReturn(execution.setFieldAuditHeadHash("head-2"));
        assertThrows(IllegalArgumentException.class, () -> service.approveVoidExecution(new EdhrRecordChangeApproveReqVO().setChangeEventId(50L)));
        verifyNoInteractions(signatureService);
        verify(archiveMapper, never()).updateById(any(MesProBatchRecordExecutionArchiveDO.class));
    }

    @Test void approvalVoidsOnlyTargetRecordAndArchiveThenCompletesOriginalTask() {
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(20L);
        source(); var event = event();
        lenient().doThrow(new IllegalStateException("市场授权已收回")).when(marketReleaseAccess).requireReadable(40L);
        lenient().doThrow(new IllegalStateException("非市场办理人")).when(marketReleaseAccess).requireWritable(40L);
        event.setSourceArchiveId(70L).setPreviousArchiveHash("archive-hash");
        when(eventMapper.selectByIdAndTenantForUpdate(50L, 1L)).thenReturn(event);
        when(eventMapper.selectById(50L)).thenReturn(event);
        when(signatureService.recordApprovalSignature(any())).thenReturn(62L);
        when(executionMapper.updateById(any(MesProBatchRecordExecutionDO.class))).thenReturn(1);
        when(archiveMapper.updateById(any(MesProBatchRecordExecutionArchiveDO.class))).thenReturn(1);
        when(eventMapper.updateById(any(MesProEdhrRecordChangeEventDO.class))).thenReturn(1);
        when(archiveMapper.selectLatestByExecutionId(30L)).thenReturn(new MesProBatchRecordExecutionArchiveDO().setId(70L).setSha256("archive-hash"));
        when(archiveMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(java.util.List.of(
                new MesProBatchRecordExecutionArchiveDO().setId(70L), new MesProBatchRecordExecutionArchiveDO().setId(71L)));
        service.approveVoidExecution(new EdhrRecordChangeApproveReqVO().setChangeEventId(50L).setPassword("review-password"));
        verify(executionMapper).updateById(argThat((MesProBatchRecordExecutionDO e) -> e.getId().equals(30L) && e.getStatus() == 4 && e.getVoidedByChangeEventId().equals(50L)));
        verify(archiveMapper).updateById(argThat((MesProBatchRecordExecutionArchiveDO a) -> a.getId().equals(70L) && Boolean.FALSE.equals(a.getArchiveValidFlag()) && "VOIDED".equals(a.getArchiveValidStatus())));
        verify(archiveMapper).updateById(argThat((MesProBatchRecordExecutionArchiveDO a) -> a.getId().equals(71L) && Boolean.FALSE.equals(a.getArchiveValidFlag()) && "VOIDED".equals(a.getArchiveValidStatus())));
        verify(signatureService).recordApprovalSignature(argThat(command -> "RECORD_CHANGE".equals(command.getReviewSourceType()) && Long.valueOf(50L).equals(command.getReviewSourceId())));
        verify(eventMapper).updateById(argThat((MesProEdhrRecordChangeEventDO e) -> "EFFECTIVE".equals(e.getChangeStatus())
                && Long.valueOf(62L).equals(e.getApprovalSignatureId()) && Long.valueOf(20L).equals(e.getApprovedBy()) && e.getEffectiveAt() != null));
        verify(taskService).complete(event);
        verify(nonconformanceReviewService).ensureBatchNotFrozen(40L, "记录变更审批");
        verifyNoInteractions(marketReleaseAccess);
        verifyNoInteractions(workTaskService);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"VOID,REJECTED", "VOID,RETURNED", "REOPEN,REJECTED", "REOPEN,RETURNED", "SUPPLEMENT,REJECTED", "SUPPLEMENT,RETURNED"})
    void negativeDecisionPreservesRecordAndArchiveAndUsesSameTask(String changeType, String decision) {
        var event = event().setChangeType(changeType);
        lenient().doThrow(new IllegalStateException("市场授权已收回")).when(marketReleaseAccess).requireReadable(40L);
        lenient().doThrow(new IllegalStateException("非市场办理人")).when(marketReleaseAccess).requireWritable(40L);
        when(eventMapper.selectByIdAndTenantForUpdate(50L, 1L)).thenReturn(event);
        when(executionMapper.selectByIdForUpdate(30L)).thenReturn(execution);
        when(signatureService.recordApprovalSignature(any())).thenReturn(63L);
        when(eventMapper.updateById(any(MesProEdhrRecordChangeEventDO.class))).thenReturn(1);
        var result = service.decide(new EdhrRecordChangeDecideReqVO().setChangeEventId(50L)
                .setDecision(decision).setReason("证据不足").setPassword("review-password"));
        assertEquals(decision, result.getChangeStatus());
        assertEquals(63L, result.getApprovalSignatureId());
        verify(taskService).completeNegativeDecision(event);
        verify(nonconformanceReviewService).ensureBatchNotFrozen(40L, "记录变更审批");
        verifyNoInteractions(marketReleaseAccess);
        verify(executionMapper, never()).updateById(any(MesProBatchRecordExecutionDO.class));
        verify(archiveMapper, never()).updateById(any(MesProBatchRecordExecutionArchiveDO.class));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"VOID,APPROVE", "VOID,REJECTED", "VOID,RETURNED", "REOPEN,APPROVE", "REOPEN,REJECTED", "REOPEN,RETURNED", "SUPPLEMENT,APPROVE", "SUPPLEMENT,REJECTED", "SUPPLEMENT,RETURNED"})
    void frozenBatchBlocksEveryFormalDecisionBeforeSigning(String changeType, String decision) {
        when(eventMapper.selectByIdAndTenantForUpdate(50L, 1L)).thenReturn(event().setChangeType(changeType));
        if ("APPROVE".equals(decision) && !"VOID".equals(changeType)) {
            when(executionMapper.selectByIdForUpdate(30L)).thenReturn(execution);
        }
        doThrow(new IllegalStateException("批次已被冻结")).when(nonconformanceReviewService).ensureBatchNotFrozen(40L, "记录变更审批");
        assertThrows(IllegalStateException.class, () -> formalDecision(changeType, decision));
        verifyNoInteractions(signatureService, marketReleaseAccess);
        verify(nonconformanceReviewService).ensureBatchNotFrozen(40L, "记录变更审批");
        verify(executionMapper, never()).updateById(any(MesProBatchRecordExecutionDO.class));
        verify(archiveMapper, never()).updateById(any(MesProBatchRecordExecutionArchiveDO.class));
        verify(eventMapper, never()).updateById(any(MesProEdhrRecordChangeEventDO.class));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"VOID,APPROVE", "VOID,REJECTED", "VOID,RETURNED", "REOPEN,APPROVE", "REOPEN,REJECTED", "REOPEN,RETURNED", "SUPPLEMENT,APPROVE", "SUPPLEMENT,REJECTED", "SUPPLEMENT,RETURNED"})
    void pendingReleaseCannotBeBypassedByGoldenFingerForFormalDecision(String changeType, String decision) {
        when(eventMapper.selectByIdAndTenantForUpdate(50L, 1L)).thenReturn(event().setChangeType(changeType));
        if ("APPROVE".equals(decision) && !"VOID".equals(changeType)) {
            when(executionMapper.selectByIdForUpdate(30L)).thenReturn(execution);
        }
        when(releaseMapper.selectByBatchExecutionId(40L)).thenReturn(new MesProEdhrReleaseTransactionDO()
                .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL));
        lenient().when(goldenFingerPermissionService.hasGoldenFingerPermission(10L)).thenReturn(true);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> formalDecision(changeType, decision));
        verify(releaseMapper).selectByBatchExecutionId(40L);
        verifyNoInteractions(signatureService, marketReleaseAccess);
        verify(nonconformanceReviewService).ensureBatchNotFrozen(40L, "记录变更审批");
        verify(executionMapper, never()).updateById(any(MesProBatchRecordExecutionDO.class));
        verify(archiveMapper, never()).updateById(any(MesProBatchRecordExecutionArchiveDO.class));
        verify(eventMapper, never()).updateById(any(MesProEdhrRecordChangeEventDO.class));
    }

    private void formalDecision(String changeType, String decision) {
        if (!"APPROVE".equals(decision)) {
            service.decide(new EdhrRecordChangeDecideReqVO().setChangeEventId(50L).setDecision(decision)
                    .setReason("正式决定").setPassword("review-password"));
        } else {
            var request = new EdhrRecordChangeApproveReqVO().setChangeEventId(50L).setPassword("review-password");
            switch (changeType) {
                case "VOID" -> service.approveVoidExecution(request);
                case "REOPEN" -> service.approveReopenExecution(request);
                case "SUPPLEMENT" -> service.approveSupplement(request);
                default -> throw new IllegalArgumentException(changeType);
            }
        }
    }

}
