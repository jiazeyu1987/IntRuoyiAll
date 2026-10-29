package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchListPerformanceTest {
    @Mock MesProEdhrBatchSummaryReader batchSummaryReader;
    @Mock MesProEdhrBatchSearchMapper batchSearchMapper;
    @Mock MesProEdhrBatchVisibilityQueryService batchVisibilityQueryService;
    @Mock MesProEdhrTraceSearchScopeService traceSearchScopeService;
    private final MesProEdhrTraceSearchScope traceScope = MesProEdhrTraceSearchScope.empty();
    @Mock MesProEdhrBatchVisibilityQuery visibilityQuery;
    @Mock MesProEdhrBatchLookupService batchLookupService;
    @Mock MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock MesProEdhrOperationAuditEventMapper operationAuditEventMapper;
    @Mock MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock MesProEdhrRecordChangeEventMapper recordChangeEventMapper;
    @Mock MesProEdhrBatchExecutionVisibilityService batchExecutionVisibilityService;
    @Mock MesProEdhrBatchStageResolver batchStageResolver;
    @Mock MesProEdhrNonconformanceReviewService nonconformanceReviewService;
    @Mock MesMarketReleaseAccess marketReleaseAccess;
    @Mock MesProEdhrOperationAuditService operationAuditService;
    @Mock MesProEdhrWorkTaskService workTaskService;
    @Mock MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock MesProEdhrWorkTaskAssignmentRuleMapper workTaskAssignmentRuleMapper;
    @Mock MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @InjectMocks MesProEdhrBatchExecutionServiceImpl service;

    @Test void summaryKeepsReleaseAndHandoverActionsWithoutDetailCapabilities() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setSummaryOnly(true);
        var batch = new MesProEdhrBatchExecutionDO().setId(7L).setStatus(40)
                .setBatchExecutionCode("EDHR-7").setBlockedCount(2);
        var access = new MesMarketReleaseAccess.Access(true, false, true, true, 9L, 1L, 2L, "recipient");
        stubPage(request, batch);
        when(batchSummaryReader.read(eq(List.of(batch)), eq(0L), any())).thenReturn(java.util.Map.of(7L,
                new MesProEdhrBatchSummaryReader.Facts(new MesProEdhrReleaseTransactionDO().setId(8L).setReleaseStatus("RELEASED"),
                        17L, null, false, access)));
        var row = service.getPage(request).getList().get(0);
        assertEquals("EDHR-7", row.getBatchExecutionCode());
        assertEquals(2, row.getBlockedCount());
        assertEquals(8L, row.getReleaseTransactionId());
        assertEquals("RELEASED", row.getReleaseStatus());
        assertSame(access, row.getMarketReleaseAccess());
        assertNull(row.getTasks());
        assertNull(row.getCanClose());
        assertNull(row.getCanArchive());
        verifyNoInteractions(workTaskService, workTaskMapper, workTaskAssignmentRuleMapper, processFormPermissionRuleMapper);
        verify(batchExecutionVisibilityService, never()).resolveCurrentProcessFillerMap(any());
    }

    @Test void summaryIsExplicitAndUsesNormalBooleanRequestBinding() {
        var request = new EdhrBatchExecutionPageReqVO();
        assertNull(request.getSummaryOnly());
        var binder = new DataBinder(request);
        binder.bind(new MutablePropertyValues(java.util.Map.of("summaryOnly", "true")));
        assertFalse(binder.getBindingResult().hasErrors());
        assertEquals(Boolean.TRUE, request.getSummaryOnly());
        binder.bind(new MutablePropertyValues(java.util.Map.of("summaryOnly", "not-a-boolean")));
        assertTrue(binder.getBindingResult().hasFieldErrors("summaryOnly"));
    }

    @Test void activeSummaryKeepsPendingVoidWithdrawalAndLocksWithoutRedundantRepair() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setSummaryOnly(true);
        var batch = new MesProEdhrBatchExecutionDO().setId(7L).setStatus(10);
        stubPage(request, batch);
        var pendingVoid = new MesProEdhrRecordChangeEventDO().setId(90L).setRequestedBy(0L)
                .setChangeStatus("SUBMITTED").setBpmProcessInstanceId("void-process");
        when(batchSummaryReader.read(eq(List.of(batch)), eq(0L), any())).thenReturn(java.util.Map.of(7L,
                new MesProEdhrBatchSummaryReader.Facts(new MesProEdhrReleaseTransactionDO().setId(8L).setReleaseStatus("PENDING_APPROVAL"),
                        17L, pendingVoid, true, null)));
        var row = service.getPage(request).getList().get(0);
        assertEquals(90L, row.getPendingVoidChangeEventId());
        assertEquals("void-process", row.getPendingVoidProcessInstanceId());
        assertEquals("SUBMITTED", row.getPendingVoidChangeStatus());
        assertTrue(row.getCanWithdrawVoidRequest());
        assertTrue(row.getProductionActionLocked());
        assertNotNull(row.getProductionActionLockReason());
        assertTrue(row.getReleaseActionLocked());
        assertNotNull(row.getReleaseActionLockReason());
        verifyNoInteractions(workTaskService, workTaskAssignmentRuleMapper, processFormPermissionRuleMapper);
        verifyNoInteractions(workTaskMapper, batchTaskMapper);
        verify(batchExecutionMapper, never()).selectById(any());
    }

    @Test void activeSummaryDoesNotRepairMissingTasksOrInventDetailReadiness() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setSummaryOnly(true);
        var batch = new MesProEdhrBatchExecutionDO().setId(7L).setStatus(10).setRouteId(99L).setRouteVersionId(100L);
        stubPage(request, batch);
        when(batchSummaryReader.read(eq(List.of(batch)), eq(0L), any())).thenReturn(java.util.Map.of(7L,
                new MesProEdhrBatchSummaryReader.Facts(null, null, null, false, null)));
        var row = service.getPage(request).getList().get(0);
        assertEquals(7L, row.getId());
        assertNull(row.getCanClose());
        assertNull(row.getTasks());
        verifyNoInteractions(batchTaskMapper, workTaskService, workTaskMapper, releaseTransactionMapper,
                nonconformanceReviewService, workTaskAssignmentRuleMapper, processFormPermissionRuleMapper);
        verify(batchExecutionMapper, never()).selectById(any());
        verify(batchExecutionMapper, never()).updateById(any(MesProEdhrBatchExecutionDO.class));
    }
    private void stubPage(EdhrBatchExecutionPageReqVO request, MesProEdhrBatchExecutionDO batch) {
        var query = new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>();
        when(traceSearchScopeService.prepare(request)).thenReturn(traceScope);
        when(batchVisibilityQueryService.prepare(request, 0L, traceScope)).thenReturn(visibilityQuery);
        when(batchExecutionMapper.buildPageQuery(request)).thenReturn(query);
        when(visibilityQuery.apply(query)).thenReturn(query);
        when(visibilityQuery.tenantId()).thenReturn(1L);
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(1L);
        when(batchSearchMapper.selectSearchPage(1L, query, 0L, request.getPageSize())).thenReturn(List.of(batch));
    }

}
