package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrBatchExecutionController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchHistoryPageTest {
    @Mock MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock MesProEdhrBatchSearchMapper batchSearchMapper;
    @Mock MesProEdhrBatchVisibilityQueryService batchVisibilityQueryService;
    @Mock MesProEdhrTraceSearchScopeService traceSearchScopeService;
    private final MesProEdhrTraceSearchScope traceScope = MesProEdhrTraceSearchScope.empty();
    @Mock MesProEdhrBatchVisibilityQuery visibilityQuery;
    private final LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query = new LambdaQueryWrapperX<>();
    @Mock MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock MesProEdhrBatchExecutionVisibilityService batchExecutionVisibilityService;
    @Mock MesProEdhrBatchLookupService batchLookupService;
    @Mock MesProEdhrBatchExecutionVisibilityService.VisibilitySession visibilitySession;
    @Mock MesProEdhrOperationAuditService operationAuditService;
    @Mock MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock MesProEdhrWorkTaskService workTaskService;
    @InjectMocks MesProEdhrBatchExecutionServiceImpl service;

    @BeforeEach void tenant() {
        TenantContextHolder.setTenantId(1L);
    }
    private void prepareQuery() {
        when(traceSearchScopeService.prepare(any())).thenReturn(traceScope);
        when(batchVisibilityQueryService.prepare(any(), eq(0L), same(traceScope))).thenReturn(visibilityQuery);
        when(batchExecutionMapper.buildPageQuery(any())).thenReturn(query);
        when(visibilityQuery.apply(query)).thenReturn(query);
        when(visibilityQuery.tenantId()).thenReturn(1L);
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void overviewLoadsOneReleaseBatchWithoutDetailOrRepairQueries() {
        var request = new EdhrBatchExecutionPageReqVO();
        var first = batch(1L);
        var second = batch(2L);
        var approvedAt = LocalDateTime.of(2026, 10, 27, 10, 0);
        first.setCreateTime(approvedAt.minusDays(1));
        stubPage(request, List.of(first, second), 20L);
        when(releaseTransactionMapper.selectListByBatchExecutionIds(List.of(1L, 2L))).thenReturn(List.of(
                new MesProEdhrReleaseTransactionDO().setId(12L).setBatchExecutionId(1L)
                        .setReleaseStatus("RELEASED").setApprovedAt(approvedAt),
                new MesProEdhrReleaseTransactionDO().setId(11L).setBatchExecutionId(1L).setReleaseStatus("REJECTED")));

        var page = service.getHistoryPage(request);

        assertTrue(request.getCompletedTraceOnly());
        assertTrue(request.getExcludeStatuses().contains(50));
        assertEquals(20L, page.getTotal());
        var row = page.getList().get(0);
        assertEquals(1L, row.getId());
        assertEquals("EDHR-1", row.getBatchExecutionCode());
        assertEquals("WO-1", row.getWorkOrderCode());
        assertEquals("LOT-1", row.getBatchCode());
        assertEquals("Product", row.getProductName());
        assertEquals(20, row.getStatus());
        assertEquals(first.getCreateTime(), row.getCreateTime());
        assertEquals(12L, row.getReleaseTransactionId());
        assertEquals("RELEASED", row.getReleaseStatus());
        assertEquals(approvedAt, row.getReleaseApprovedAt());
        assertNull(page.getList().get(1).getReleaseTransactionId());
        verify(batchExecutionMapper).buildPageQuery(request);
        verifyNoMoreInteractions(batchExecutionMapper);
        verify(releaseTransactionMapper).selectListByBatchExecutionIds(List.of(1L, 2L));
        verifyNoMoreInteractions(releaseTransactionMapper);
        verifyNoInteractions(batchTaskMapper, workTaskService);
        verify(batchLookupService).validateSearchHits(anyList(), same(request));
        verify(operationAuditService).record(any());
    }

    @Test void visibilityIsAppliedBeforePaginationAndTotal() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setPageNo(2); request.setPageSize(1);
        stubPage(request, List.of(batch(2L)), 2L);
        var page = service.getHistoryPage(request);
        assertEquals(2L, page.getTotal());
        assertEquals(List.of(2L), page.getList().stream().map(row -> row.getId()).toList());
        verify(releaseTransactionMapper).selectListByBatchExecutionIds(List.of(2L));
        verifyNoInteractions(batchTaskMapper, workTaskService);
        verify(batchLookupService).validateSearchHits(anyList(), same(request));
    }

    @Test void traceFiltersRunBeforePagination() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setTraceMaterial("MAT"); request.setPageSize(1); request.setPageNo(2);
        stubPage(request, List.of(batch(3L)), 2L);
        var page = service.getHistoryPage(request);
        assertEquals(2L, page.getTotal());
        assertEquals(3L, page.getList().get(0).getId());
        verify(batchLookupService).validateSearchHits(argThat(rows -> rows.size() == 1 && rows.get(0).getId() == 3L), same(request));
        verify(batchLookupService, never()).prepareWindowFilter(any());
        verifyNoInteractions(batchTaskMapper, workTaskService);
    }

    @Test void emptyPageDoesNotQueryReleaseDetails() {
        var request = new EdhrBatchExecutionPageReqVO();
        prepareQuery();
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(0L);
        assertTrue(service.getHistoryPage(request).getList().isEmpty());
        verifyNoInteractions(releaseTransactionMapper, batchTaskMapper, workTaskService);
    }

    @Test void databaseCountRetainsOnlyRequestedPageWithoutScanning() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setPageNo(2); request.setPageSize(1);
        stubPage(request, List.of(batch(3L)), 3L);
        var page = service.getHistoryPage(request);
        assertEquals(3L, page.getTotal());
        assertEquals(List.of(3L), page.getList().stream().map(row -> row.getId()).toList());
        verify(batchSearchMapper).selectSearchPage(1L, query, 1L, 1);
        verify(batchExecutionMapper, never()).selectScanWindow(any(), any());
        verify(batchExecutionMapper, never()).selectList(request);
        verifyNoInteractions(visibilitySession, batchExecutionVisibilityService);
    }

    @Test void traceFailureInReturnedPageFailsRequest() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setPageSize(1); request.setTraceMaterial("MAT");
        var rows = List.of(batch(3L));
        stubPage(request, rows, 2L);
        doThrow(new IllegalStateException("invalid formal origin"))
                .when(batchLookupService).validateSearchHits(rows, request);
        assertThrows(IllegalStateException.class, () -> service.getHistoryPage(request));
        verifyNoInteractions(operationAuditService, releaseTransactionMapper);
    }

    @Test void queryFailurePropagates() {
        var request = new EdhrBatchExecutionPageReqVO();
        prepareQuery();
        when(batchSearchMapper.selectSearchCount(1L, query)).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.getHistoryPage(request));
        verifyNoInteractions(operationAuditService, releaseTransactionMapper);
    }

    @Test void historyCannotOptIntoReworkedRecordsAndPreservesAdditionalExclusions() {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setCompletedTraceOnly(false);
        request.setExcludeStatuses(List.of(60));
        request.setReleaseApprovedTime(new LocalDateTime[]{LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 27, 0, 0)});
        prepareQuery();
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(0L);
        service.getHistoryPage(request);
        assertTrue(request.getCompletedTraceOnly());
        assertEquals(List.of(60, 50), request.getExcludeStatuses());
        assertTrue(request.getReleasedOnly());
    }

    @Test void historyTimesComeFromFormalOriginAndNotBatchCreationTime() {
        var request = new EdhrBatchExecutionPageReqVO();
        var batch = batch(1L);
        var joinedAt = LocalDateTime.of(2026, 10, 1, 8, 0);
        var pqcAt = joinedAt.plusDays(2);
        batch.setCreateTime(joinedAt.plusDays(1));
        stubPage(request, List.of(batch), 1L);
        when(batchExecutionOriginMapper.selectHistoryTimes(1L, List.of(1L))).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginMapper.HistoryTimes().setBatchExecutionId(1L)
                        .setActiveOrderId(11L).setResolvedActiveOrderId(11L).setStartedAt(joinedAt).setPqcReleasedAt(pqcAt)));
        var row = service.getHistoryPage(request).getList().get(0);
        assertEquals(joinedAt, row.getStartedAt());
        assertEquals(pqcAt, row.getPqcReleasedAt());
        assertNull(row.getReleaseApprovedAt());
        verify(batchExecutionOriginMapper).selectHistoryTimes(1L, List.of(1L));
    }

    @Test void ambiguousFormalOriginsFailInsteadOfGuessingStartTime() {
        var request = new EdhrBatchExecutionPageReqVO();
        stubPage(request, List.of(batch(1L)), 1L);
        var at = LocalDateTime.of(2026, 10, 1, 8, 0);
        when(batchExecutionOriginMapper.selectHistoryTimes(1L, List.of(1L))).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginMapper.HistoryTimes().setBatchExecutionId(1L)
                        .setActiveOrderId(11L).setResolvedActiveOrderId(11L).setStartedAt(at),
                new MesProEdhrBatchExecutionOriginMapper.HistoryTimes().setBatchExecutionId(1L)
                        .setActiveOrderId(12L).setResolvedActiveOrderId(12L).setStartedAt(at)));
        assertThrows(IllegalStateException.class, () -> service.getHistoryPage(request));
    }

    @Test void lightweightEndpointRequiresExistingQueryPermission() throws Exception {
        var method = MesProEdhrBatchExecutionController.class.getMethod("getHistoryPage", EdhrBatchExecutionPageReqVO.class);
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private void stubPage(EdhrBatchExecutionPageReqVO request, List<MesProEdhrBatchExecutionDO> rows, long total) {
        prepareQuery();
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(total);
        when(batchSearchMapper.selectSearchPage(1L, query,
                (long) (request.getPageNo() - 1) * request.getPageSize(), request.getPageSize())).thenReturn(rows);
    }

    private MesProEdhrBatchExecutionDO batch(Long id) {
        return new MesProEdhrBatchExecutionDO().setId(id).setBatchExecutionCode("EDHR-" + id)
                .setWorkOrderCode("WO-" + id).setBatchCode("LOT-" + id).setProductName("Product").setStatus(20);
    }
}
