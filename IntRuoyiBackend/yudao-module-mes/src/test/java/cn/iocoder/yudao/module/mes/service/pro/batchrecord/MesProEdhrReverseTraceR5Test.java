package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionOriginDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Service versioning contract only; adapters are explicit test doubles, not historical-source proof. */
class MesProEdhrReverseTraceR5Test {
    private final MesProEdhrBatchExecutionMapper batches = mock(MesProEdhrBatchExecutionMapper.class);
    private final MesProEdhrBatchExecutionOriginMapper origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
    private final MesProcessPoolActiveOrderMapper orders = mock(MesProcessPoolActiveOrderMapper.class);
    private final MesProcessPoolActiveOrderCompletionReceiptMapper receipts = mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
    private final MesProEdhrBatchExecutionVisibilityService visibility = mock(MesProEdhrBatchExecutionVisibilityService.class);
    private final MesProEdhrBatchExecutionDO anchor = batch(200L);
    private final MesProEdhrBatchExecutionDO target = batch(100L);
    private final TargetScope scope = new TargetScope().setKind("RELEASED_HISTORY")
            .setReleaseApprovedFrom(LocalDateTime.of(2026, 9, 1, 0, 0))
            .setReleaseApprovedTo(LocalDateTime.of(2026, 9, 2, 0, 0));
    private List<MesProEdhrBatchExecutionDO> candidates;
    private List<MesProEdhrReverseTraceSourceAdapter> adapters;
    private MesProEdhrReverseTraceServiceImpl service;
    private String anchorVersion;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        candidates = List.of(target);
        anchorVersion = "anchor-source-v1";
        stubChain(anchor);
        stubChain(target);
        when(batches.selectPage(any(EdhrBatchExecutionPageReqVO.class))).thenAnswer(invocation -> {
            EdhrBatchExecutionPageReqVO request = invocation.getArgument(0);
            // Mirrors the mapper contract: independent ID lookup versus date-filtered candidate rows.
            return request.getBatchExecutionIds() == null
                    ? new PageResult<>(candidates, (long) candidates.size())
                    : new PageResult<>(List.of(anchor), 1L);
        });
        when(visibility.canViewBatch(any(), any())).thenReturn(true);
        adapters = Arrays.stream(Category.values()).map(category -> {
            MesProEdhrReverseTraceSourceAdapter adapter = spy(new MesProEdhrReverseTraceTestFixtureAdapter(category));
            doAnswer(invocation -> {
                var original = (MesProEdhrReverseTraceSourceAdapter.CatalogResult) invocation.callRealMethod();
                var batch = invocation.getArgument(0, MesProEdhrBatchExecutionDO.class);
                return new MesProEdhrReverseTraceSourceAdapter.CatalogResult(
                        batch.getId().equals(anchor.getId()) && category == Category.FIELD
                                ? anchorVersion : original.sourceVersion(),
                        original.sourceIdentity(), original.status(), original.reasonCode(), original.reason(), original.items());
            }).when(adapter).readCatalog(any());
            return adapter;
        }).toList();
        service = new MesProEdhrReverseTraceServiceImpl(batches, origins, orders, receipts,
                applications, transactions, visibility, adapters);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void anchorSourceMutationChangesCatalogPagingVersionEvenWithoutCandidates(boolean empty) {
        if (empty) candidates = List.of();
        var first = service.getCatalog(catalogRequest());
        assertEquals(1L, first.getTotal());
        anchorVersion = "anchor-source-v2";
        var next = service.getCatalog(catalogRequest().setPageNo(2).setPageSize(1));
        assertEquals(1L, next.getTotal());
        assertNotEquals(first.getCatalogVersion(), next.getCatalogVersion());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oldQueryVersionIsRejectedAfterOutOfRangeAnchorMutation(boolean empty) {
        if (empty) candidates = List.of();
        var request = queryRequest(service.getCatalog(catalogRequest()).getCatalogVersion());
        var initial = service.query(request);
        assertEquals(empty ? "NO_MATCH" : "MATCHED", initial.getQueryStatus());
        assertEquals(empty ? 0L : 1L, initial.getTotal());
        anchorVersion = "anchor-source-v2";
        var stale = service.query(request);
        assertEquals("BLOCKED", stale.getQueryStatus());
        assertEquals("CATALOG_STALE", stale.getReasonCode());
        assertNull(stale.getTotal());
        assertTrue(stale.getList().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void oldEvidenceVersionIsRejectedAfterOutOfRangeAnchorMutation(boolean empty) {
        if (empty) candidates = List.of();
        var query = queryRequest(service.getCatalog(catalogRequest()).getCatalogVersion());
        var result = service.query(query);
        assertEquals("COMPLETE", result.getCoverageStatus());
        var request = evidenceRequest(query, result.getQueryHash());
        if (!empty) assertEquals("MATCHED", service.evidence(request).getEvidenceStatus());
        anchorVersion = "anchor-source-v2";
        var stale = service.evidence(request);
        assertEquals("BLOCKED", stale.getEvidenceStatus());
        assertEquals("CATALOG_STALE", stale.getReasonCode());
        assertNull(stale.getTotal());
        assertTrue(stale.getItems().isEmpty());
    }

    @Test
    void unchangedSourcesAgreeAcrossEndpointsWithoutAddingAnchorToResults() {
        var catalog = service.getCatalog(catalogRequest());
        var request = queryRequest(catalog.getCatalogVersion());
        var query = service.query(request);
        var evidence = service.evidence(evidenceRequest(request, query.getQueryHash()));
        assertEquals("MATCHED", query.getQueryStatus());
        assertEquals("MATCHED", evidence.getEvidenceStatus());
        assertEquals(catalog.getCatalogVersion(), query.getCatalogVersion());
        assertEquals(catalog.getCatalogVersion(), evidence.getCatalogVersion());
        assertEquals(1, query.getCoverage().getCandidateBatchCount());
        assertEquals(1L, query.getTotal());
        assertEquals(List.of("100"), query.getList().stream().map(QueryItem::getBatchExecutionId).toList());
        var calls = ArgumentCaptor.forClass(EdhrBatchExecutionPageReqVO.class);
        verify(batches, atLeastOnce()).selectPage(calls.capture());
        calls.getAllValues().stream().filter(call -> call.getBatchExecutionIds() == null).forEach(call -> {
            assertEquals(Boolean.TRUE, call.getReleasedOnly());
            assertArrayEquals(new LocalDateTime[]{scope.getReleaseApprovedFrom(), scope.getReleaseApprovedTo()},
                    call.getReleaseApprovedTime());
        });
        verify(visibility, atLeastOnce()).requireVisibleBatch(anchor, null);
        verify(visibility, atLeastOnce()).canViewBatch(target, null);
    }

    @Test
    void emptyRangeDoesNotEvaluateOrReturnAnchorAsCandidate() {
        candidates = List.of();
        var result = service.query(queryRequest(service.getCatalog(catalogRequest()).getCatalogVersion()));
        assertEquals("NO_MATCH", result.getQueryStatus());
        assertEquals("COMPLETE", result.getCoverageStatus());
        assertEquals(0, result.getCoverage().getCandidateBatchCount());
        assertEquals(0, result.getCoverage().getEvaluatedBatchCount());
        assertEquals(0L, result.getTotal());
        assertTrue(result.getList().isEmpty());
        for (var adapter : adapters) verify(adapter, never()).evaluate(any(), any());
    }

    @Test
    void anchorAlreadyInCandidatesIsVersionedOncePerCategory() {
        candidates = List.of(anchor);
        // Keep source versions consistent with this existing fixture's evaluation contract.
        anchorVersion = "fixture-v1-200";
        var catalog = service.getCatalog(catalogRequest());
        for (var adapter : adapters) {
            verify(adapter, times(adapter.category() == Category.FIELD ? 2 : 1)).readCatalog(anchor);
        }
        var result = service.query(queryRequest(catalog.getCatalogVersion()));
        assertEquals("MATCHED", result.getQueryStatus());
        assertEquals(1, result.getCoverage().getCandidateBatchCount());
        assertEquals(1L, result.getTotal());
    }

    private CatalogRequest catalogRequest() {
        return new CatalogRequest().setAnchorBatchExecutionId("200").setCategory(Category.FIELD).setTargetScope(scope);
    }

    private QueryRequest queryRequest(String version) {
        return new QueryRequest().setAnchorBatchExecutionId("200").setCatalogVersion(version)
                .setTargetScope(scope).setLogic("AND").setConditions(List.of(new Condition()
                        .setConditionId("C1").setEvidenceKey("fixture-field").setSourceView("RECORDED")
                        .setOperator("EQ").setValue("fixture")));
    }

    private EvidenceRequest evidenceRequest(QueryRequest query, String hash) {
        return new EvidenceRequest().setAnchorBatchExecutionId("200").setTargetBatchExecutionId("100")
                .setCatalogVersion(query.getCatalogVersion()).setQueryHash(hash).setTargetScope(scope)
                .setLogic(query.getLogic()).setConditions(query.getConditions());
    }

    private static MesProEdhrBatchExecutionDO batch(long id) {
        return new MesProEdhrBatchExecutionDO().setId(id).setTenantId(1L).setWorkOrderId(id + 1)
                .setRouteId(3000L)
                .setRouteVersionId(3001L).setBatchExecutionCode("BE-" + id).setBatchCode("B-" + id);
    }

    private void stubChain(MesProEdhrBatchExecutionDO batch) {
        long id = batch.getId();
        when(orders.selectById(id + 2)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(id + 2).setWorkOrderId(batch.getWorkOrderId()).setQaRegulationVersionId(7005L));
        String snapshot = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Map.of(
                "activeOrderBinding", Map.of("id", id + 2, "tenantId", 1L,
                        "workOrderId", batch.getWorkOrderId(), "routeId", batch.getRouteId(), "routeVersionId", batch.getRouteVersionId()),
                "workOrderBinding", Map.of("id", batch.getWorkOrderId())));
        var receipt = new MesProcessPoolActiveOrderCompletionReceiptDO().setId(id + 3)
                .setActiveOrderId(id + 2).setWorkOrderId(batch.getWorkOrderId())
                .setRouteId(batch.getRouteId()).setRouteVersionId(batch.getRouteVersionId())
                .setCompletedVersion(1).setBatchRecordId(id + 8).setProcessInspectionId(id + 9)
                .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setFormalSourceSnapshotJson(snapshot).setLossConditionFactsJson("[]");
        receipt.setTenantId(1L);
        receipt.setSourceSnapshotHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(snapshot) + "|[]"));
        receipt.setReceiptHash(cn.iocoder.yudao.module.mes.service.pro.processpool.team
                .MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(origins.selectListByBatchExecutionId(id)).thenReturn(List.of(new MesProEdhrBatchExecutionOriginDO()
                .setBatchExecutionId(id).setTenantId(1L).setWorkOrderId(batch.getWorkOrderId())
                .setActiveOrderId(id + 2).setCompletionBackfillReceiptId(receipt.getId())
                .setCompletionVersion(receipt.getCompletedVersion())
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                .setSourceSnapshotHash(receipt.getSourceSnapshotHash())));
        when(receipts.selectByIdAndTenantId(id + 3, 1L)).thenReturn(receipt);
        when(applications.selectListByBatchExecutionIds(List.of(id))).thenReturn(List.of(
                new MesProcessPoolActiveOrderReleaseApplicationDO().setId(id + 4).setActiveOrderId(id + 2)
                        .setBatchExecutionId(id).setPqcReleaseWorkTaskId(id + 5)
                        .setReleaseTransactionId(id + 6).setReleaseApprovalWorkTaskId(id + 7)));
        when(transactions.selectByBatchExecutionId(id)).thenReturn(new MesProEdhrReleaseTransactionDO()
                .setId(id + 6).setBatchExecutionId(id).setReleaseStatus("RELEASED"));
    }
}
