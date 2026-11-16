package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.stream.LongStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Interaction bounds, not a benchmark: database totals are deliberately much larger than returned pages. */
@ExtendWith(MockitoExtension.class)
class MesProEdhrBatchDatabasePageTest {
    @Mock MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock MesProEdhrBatchSearchMapper batchSearchMapper;
    @Mock MesProEdhrBatchVisibilityQueryService batchVisibilityQueryService;
    @Mock MesProEdhrTraceSearchScopeService traceSearchScopeService;
    private final MesProEdhrTraceSearchScope traceScope = MesProEdhrTraceSearchScope.empty();
    @Mock MesProEdhrBatchVisibilityQuery visibility;
    @Mock MesProEdhrBatchLookupService batchLookupService;
    @Mock MesProEdhrBatchExecutionVisibilityService batchExecutionVisibilityService;
    @Mock MesProEdhrBatchExecutionOriginMapper batchExecutionOriginMapper;
    @Mock MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock MesProEdhrBatchSummaryReader batchSummaryReader;
    @Mock MesProEdhrBatchStageResolver batchStageResolver;
    @Mock MesProEdhrOperationAuditService operationAuditService;
    @InjectMocks MesProEdhrBatchExecutionServiceImpl service;
    final LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query = new LambdaQueryWrapperX<>();

    @BeforeEach void prepare() {
        TenantContextHolder.setTenantId(1L);
        when(traceSearchScopeService.prepare(any())).thenReturn(traceScope);
        when(batchVisibilityQueryService.prepare(any(), eq(0L), same(traceScope))).thenReturn(visibility);
        when(batchExecutionMapper.buildPageQuery(any())).thenReturn(query);
        when(visibility.apply(query)).thenReturn(query);
        when(visibility.tenantId()).thenReturn(1L);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void largeDatabaseTotalReadsOnePageAndValidatesOnlyHits(boolean history) {
        var request = request(3, 20);
        request.setTraceMaterial("target-material");
        var rows = rows(20);
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(1_000_000L);
        when(batchSearchMapper.selectSearchPage(1L, query, 40L, 20)).thenReturn(rows);
        // A corrupt batch outside this returned page must never reach the formal evidence reader.
        doAnswer(call -> {
            List<MesProEdhrBatchExecutionDO> hits = call.getArgument(0);
            if (hits.stream().anyMatch(batch -> batch.getId() == 999L))
                throw new IllegalStateException("corrupt off-page evidence");
            assertEquals(rows, hits);
            return null;
        }).when(batchLookupService).validateSearchHits(anyList(), same(request));
        if (!history) {
            var facts = rows.stream().collect(java.util.stream.Collectors.toMap(MesProEdhrBatchExecutionDO::getId,
                    ignored -> new MesProEdhrBatchSummaryReader.Facts(null, null, null, false, null)));
            when(batchSummaryReader.read(eq(rows), eq(0L), any())).thenReturn(facts);
        }
        var result = history ? service.getHistoryPage(request) : service.getPage(request);
        assertEquals(1_000_000L, result.getTotal());
        assertEquals(20, result.getList().size());
        var order = inOrder(batchVisibilityQueryService, batchExecutionMapper, visibility, batchSearchMapper, batchLookupService);
        order.verify(batchVisibilityQueryService).prepare(request, 0L, traceScope);
        order.verify(batchExecutionMapper).buildPageQuery(request);
        order.verify(visibility).apply(query);
        order.verify(batchSearchMapper).selectSearchCount(1L, query);
        order.verify(batchSearchMapper).selectSearchPage(1L, query, 40L, 20);
        order.verify(batchLookupService).validateSearchHits(rows, request);
        verifyNoMoreInteractions(batchSearchMapper, batchLookupService);
        verify(batchExecutionMapper, never()).selectScanWindow(any(), any());
        verify(batchExecutionMapper, never()).selectList(request);
        verifyNoInteractions(batchExecutionVisibilityService);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void corruptMatchedEvidencePropagatesOnBothEntryPoints(boolean history) {
        var request = request(1, 20);
        var rows = rows(1);
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(10L);
        when(batchSearchMapper.selectSearchPage(1L, query, 0L, 20)).thenReturn(rows);
        var failure = new IllegalStateException("corrupt matched evidence");
        doThrow(failure).when(batchLookupService).validateSearchHits(rows, request);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> {
            if (history) service.getHistoryPage(request); else service.getPage(request);
        }));
        verifyNoInteractions(operationAuditService, batchSummaryReader, releaseTransactionMapper,
                batchExecutionOriginMapper);
    }

    @Test void maximumPageValidatesIn128RowChunksWithoutDroppingRows() {
        var request = request(2, 200);
        var rows = rows(200);
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(900L);
        when(batchSearchMapper.selectSearchPage(1L, query, 200L, 200)).thenReturn(rows);
        var page = service.getHistoryPage(request);
        assertEquals(900L, page.getTotal());
        assertEquals(rows.stream().map(MesProEdhrBatchExecutionDO::getId).toList(),
                page.getList().stream().map(row -> row.getId()).toList());
        var order = inOrder(batchLookupService);
        order.verify(batchLookupService).validateSearchHits(rows.subList(0, 128), request);
        order.verify(batchLookupService).validateSearchHits(rows.subList(128, 200), request);
        verifyNoMoreInteractions(batchLookupService);
    }

    @Test void emptyRequestedPagePreservesDatabaseTotalWithoutReadingEvidence() {
        var request = request(30, 20);
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(10L);
        when(batchSearchMapper.selectSearchPage(1L, query, 580L, 20)).thenReturn(List.of());
        var page = service.getHistoryPage(request);
        assertEquals(10L, page.getTotal());
        assertTrue(page.getList().isEmpty());
        verifyNoInteractions(batchLookupService, releaseTransactionMapper, batchExecutionOriginMapper);
    }

    @Test void zeroMatchesDoesNotFetchPageOrEvidence() {
        var page = service.getHistoryPage(request(1, 20));
        assertEquals(0L, page.getTotal());
        verify(batchSearchMapper).selectSearchCount(1L, query);
        verifyNoMoreInteractions(batchSearchMapper);
        verifyNoInteractions(batchLookupService, releaseTransactionMapper, batchExecutionOriginMapper);
    }

    @Test void pageQueryFailurePropagatesWithoutSuccessAudit() {
        var request = request(1, 20);
        when(batchSearchMapper.selectSearchCount(1L, query)).thenReturn(10L);
        var failure = new IllegalStateException("database unavailable");
        when(batchSearchMapper.selectSearchPage(1L, query, 0L, 20)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> service.getHistoryPage(request)));
        verifyNoInteractions(batchLookupService, operationAuditService);
    }

    private EdhrBatchExecutionPageReqVO request(int pageNo, int pageSize) {
        var request = new EdhrBatchExecutionPageReqVO();
        request.setPageNo(pageNo); request.setPageSize(pageSize); request.setSummaryOnly(true);
        return request;
    }
    private List<MesProEdhrBatchExecutionDO> rows(int size) {
        return LongStream.rangeClosed(1, size).mapToObj(id ->
                new MesProEdhrBatchExecutionDO().setId(id).setStatus(20)).toList();
    }
}
