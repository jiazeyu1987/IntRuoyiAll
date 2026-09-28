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
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceSourceAdapter.EvaluationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category.FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrReverseTraceServiceTest {

    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;

    @Mock
    private MesProEdhrBatchExecutionOriginMapper originMapper;

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;

    @Mock
    private MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper;

    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;

    @Mock
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;

    @Mock
    private MesProEdhrBatchExecutionVisibilityService visibilityService;

    private MesProEdhrReverseTraceServiceImpl service;
    private MesProEdhrReverseTraceSourceAdapter testedField;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId());
        service = new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService, List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void catalogUsesReleasedHistoryAndReturnsExplicitSourceBlockedCategoriesUntilAdaptersExist() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubReleasedTransaction();
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));

        MesProEdhrReverseTraceModels.CatalogResponse response = service.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest()
                        .setAnchorBatchExecutionId("900000000000000001")
                        .setPageNo(1)
                        .setPageSize(20));

        assertEquals("900000000000000001", response.getAnchorBatchExecutionId());
        assertEquals(6, response.getCategories().size());
        assertTrue(response.getCategories().stream().allMatch(category ->
                "BLOCKED".equals(category.getStatus())
                        && "SOURCE_MISSING".equals(category.getReasonCode())));
        assertTrue(response.getItems().isEmpty());

        ArgumentCaptor<EdhrBatchExecutionPageReqVO> requestCaptor =
                ArgumentCaptor.forClass(EdhrBatchExecutionPageReqVO.class);
        org.mockito.Mockito.verify(batchExecutionMapper, org.mockito.Mockito.times(2))
                .selectPage(requestCaptor.capture());
        List<EdhrBatchExecutionPageReqVO> requests = requestCaptor.getAllValues();
        assertEquals(List.of(900000000000000001L), requests.get(0).getBatchExecutionIds());
        assertEquals(Boolean.TRUE, requests.get(0).getReleasedOnly());
        assertEquals(Boolean.TRUE, requests.get(1).getReleasedOnly());
        assertEquals(tenantId(), batch.getTenantId());
        verify(visibilityService).requireVisibleBatch(batch, null);
    }

    @Test
    void queryRejectsOrAndMoreThanTenConditionsBeforeReadingSources() {
        MesProEdhrReverseTraceModels.QueryRequest request = new MesProEdhrReverseTraceModels.QueryRequest()
                .setAnchorBatchExecutionId("900000000000000001")
                .setCatalogVersion("catalog-v1")
                .setLogic("OR")
                .setTargetScope(new MesProEdhrReverseTraceModels.TargetScope().setKind("RELEASED_HISTORY"));

        assertThrows(IllegalArgumentException.class, () -> service.query(request));

        request.setLogic("AND");
        request.setConditions(java.util.stream.IntStream.range(0, 11)
                .mapToObj(index -> new MesProEdhrReverseTraceModels.Condition()
                        .setConditionId("C" + index)
                        .setEvidenceKey("opaque-" + index)
                        .setSourceView("RECORDED")
                        .setOperator("EQ")
                        .setValue("x"))
                .toList());
        assertThrows(IllegalArgumentException.class, () -> service.query(request));
        verifyNoMoreInteractions(batchExecutionMapper, visibilityService);
    }

    @Test
    void queryRejectsDuplicateConditionIdsBeforeReadingSources() {
        MesProEdhrReverseTraceModels.Condition first = new MesProEdhrReverseTraceModels.Condition()
                .setConditionId("C1").setEvidenceKey("field-1").setSourceView("RECORDED")
                .setOperator("EQ").setValue("first");
        MesProEdhrReverseTraceModels.Condition duplicate = new MesProEdhrReverseTraceModels.Condition()
                .setConditionId("C1").setEvidenceKey("field-2").setSourceView("RECORDED")
                .setOperator("EQ").setValue("second");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.query(queryRequest().setConditions(List.of(first, duplicate))));

        assertEquals("conditionId must be unique", exception.getMessage());
        verifyNoMoreInteractions(batchExecutionMapper, originMapper, activeOrderMapper, completionReceiptMapper,
                releaseApplicationMapper, releaseTransactionMapper, visibilityService);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"C1", " C1 ", "", " "})
    void queryAndEvidenceRejectInvalidConditionIdsBeforeAnySourceRead(String secondId) {
        var first = queryRequest().getConditions().get(0);
        var second = new MesProEdhrReverseTraceModels.Condition().setConditionId(secondId)
                .setEvidenceKey("other").setSourceView("RECORDED").setOperator("EQ").setValue("other");
        var query = queryRequest().setConditions(List.of(first, second));
        assertThrows(IllegalArgumentException.class, () -> service.query(query));
        var evidence = new MesProEdhrReverseTraceModels.EvidenceRequest()
                .setAnchorBatchExecutionId(query.getAnchorBatchExecutionId()).setTargetBatchExecutionId(query.getAnchorBatchExecutionId())
                .setCatalogVersion(query.getCatalogVersion()).setQueryHash("hash").setLogic("AND")
                .setTargetScope(query.getTargetScope()).setConditions(query.getConditions());
        assertThrows(IllegalArgumentException.class, () -> service.evidence(evidence));
        org.mockito.Mockito.verifyNoInteractions(batchExecutionMapper, originMapper, activeOrderMapper,
                completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService);
    }

    @Test
    void distinctConditionIdsRetainAndSemantics() {
        var request = prepareInvalidEvaluation(new EvaluationResult("fixture-v1-900000000000000000",
                "fixture-source-900000000000000000", "COMPLETE", null, null, List.of()));
        var second = new MesProEdhrReverseTraceModels.Condition().setConditionId("C2").setEvidenceKey("fixture-field")
                .setSourceView("RECORDED").setOperator("EQ").setValue("different");
        org.mockito.Mockito.reset(testedField);
        doAnswer(invocation -> {
            var batch = invocation.getArgument(0, MesProEdhrBatchExecutionDO.class);
            return new EvaluationResult("fixture-v1-" + batch.getId(), "fixture-source-" + batch.getId(), "COMPLETE", null, null,
                    List.of(new MesProEdhrReverseTraceSourceAdapter.ConditionMatch("C1", true, "first", "first"),
                            new MesProEdhrReverseTraceSourceAdapter.ConditionMatch("C2", false, null, null)));
        }).when(testedField).evaluate(any(), anyList());
        var response = service.query(queryRequest().setCatalogVersion(request.getCatalogVersion())
                .setConditions(List.of(request.getConditions().get(0), second)));
        assertEquals("NO_MATCH", response.getQueryStatus());
        assertEquals(0L, response.getTotal());
    }

    @Test
    void evidencePreservesFormalSourceFailureWithoutPartialEvidence() {
        var request = prepareInvalidEvaluation(new EvaluationResult("fixture-v1-900000000000000000",
                "fixture-source-900000000000000000", "COMPLETE", null, null, List.of()));
        org.mockito.Mockito.doThrow(new IllegalStateException("SOURCE_MISSING:正式回执分配事件缺失"))
                .when(testedField).readEvidence(any(), anyList());
        var response = service.evidence(request);
        assertEquals("BLOCKED", response.getEvidenceStatus());
        assertEquals("SOURCE_MISSING", response.getReasonCode());
        assertEquals("正式回执分配事件缺失", response.getReason());
        assertNull(response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void repeatedFactsWithTheSameSemanticIdentityDoNotInvalidateTheCondition() {
        var request = prepareInvalidEvaluation(new EvaluationResult("fixture-v1-900000000000000000",
                "fixture-source-900000000000000000", "COMPLETE", null, null, List.of()));
        doAnswer(invocation -> {
            var original = (MesProEdhrReverseTraceSourceAdapter.CatalogResult) invocation.callRealMethod();
            var repeated = new MesProEdhrReverseTraceModels.CatalogItem().setEvidenceKey("fixture-field").setCategory(FIELD)
                    .setSourceView("RECORDED").setValueType("string").setUnit("text").setAllowedOperators(List.of("EQ"))
                    .setQualifiers(Map.of()).setSourceRef("second-event").setSavedValue("second recorded value");
            return new MesProEdhrReverseTraceSourceAdapter.CatalogResult(original.sourceVersion(), original.sourceIdentity(),
                    original.status(), null, null, List.of(original.items().get(0), repeated));
        }).when(testedField).readCatalog(any());
        var response = service.query(queryRequest().setCatalogVersion(request.getCatalogVersion()).setConditions(request.getConditions()));
        assertEquals("MATCHED", response.getQueryStatus());
        assertEquals(1L, response.getTotal());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"qualifiers", "type", "unit", "operators", "identity"})
    void conflictingCatalogContractsRemainBlocked(String conflict) {
        var request = prepareInvalidEvaluation(new EvaluationResult("fixture-v1-900000000000000000",
                "fixture-source-900000000000000000", "COMPLETE", null, null, List.of()));
        org.mockito.Mockito.reset(testedField);
        doAnswer(invocation -> {
            var original = (MesProEdhrReverseTraceSourceAdapter.CatalogResult) invocation.callRealMethod();
            var conflicting = new MesProEdhrReverseTraceModels.CatalogItem().setEvidenceKey("fixture-field").setCategory(FIELD)
                    .setSourceView("RECORDED").setValueType("string").setUnit("text").setAllowedOperators(List.of("EQ"))
                    .setQualifiers(Map.of());
            switch (conflict) {
                case "qualifiers" -> conflicting.setQualifiers(Map.of("sampleId", "B"));
                case "type" -> conflicting.setValueType("number");
                case "unit" -> conflicting.setUnit("kg");
                case "operators" -> conflicting.setAllowedOperators(List.of("NE"));
                case "identity" -> conflicting.setSemanticIdentity("another-version");
            }
            return new MesProEdhrReverseTraceSourceAdapter.CatalogResult(original.sourceVersion(), original.sourceIdentity(),
                    original.status(), null, null, List.of(original.items().get(0), conflicting));
        }).when(testedField).readCatalog(any());
        var response = service.query(queryRequest().setCatalogVersion(request.getCatalogVersion()).setConditions(request.getConditions()));
        assertEquals("BLOCKED", response.getQueryStatus());
        assertEquals("CONDITION_INVALID", response.getReasonCode());
        assertNull(response.getTotal());
        assertTrue(response.getList().isEmpty());
    }

    @Test
    void queryReturnsBlockedWithoutSuccessListWhenP2HasNoSourceAdapters() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubReleasedTransaction();
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        String catalogVersion = service.getCatalog(new MesProEdhrReverseTraceModels.CatalogRequest()
                .setAnchorBatchExecutionId("900000000000000001")).getCatalogVersion();

        MesProEdhrReverseTraceModels.QueryResponse response = service.query(
                new MesProEdhrReverseTraceModels.QueryRequest()
                        .setAnchorBatchExecutionId("900000000000000001")
                        .setCatalogVersion(catalogVersion)
                        .setLogic("AND")
                        .setTargetScope(new MesProEdhrReverseTraceModels.TargetScope().setKind("RELEASED_HISTORY"))
                        .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition()
                                .setConditionId("C1")
                                .setEvidenceKey("opaque-catalog-reference")
                                .setSourceView("RECORDED")
                                .setOperator("EQ")
                                .setValue("x"))));

        assertEquals("BLOCKED", response.getQueryStatus());
        assertEquals("SOURCE_MISSING", response.getReasonCode());
        assertTrue(response.getList().isEmpty());
        assertEquals(null, response.getTotal());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"INSPECTION,query", "INSPECTION,evidence", "MATERIAL,query", "MATERIAL,evidence"})
    void missingFormalOriginPreservesSourceMissingAtBothServiceBoundaries(String categoryName, String entryPoint) {
        var batch = releasedBatch();
        stubCompleteChainContext(batch);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        // Chain projection is complete, but the source adapter's own read finds no origin.
        var sourceOrigins = org.mockito.Mockito.mock(MesProEdhrBatchExecutionOriginMapper.class);
        var category = MesProEdhrReverseTraceModels.Category.valueOf(categoryName);
        var formal = new MesProEdhrFormalReverseTraceAdapter(category, null, sourceOrigins,
                null, null, null, null, null, null, null, null, null, null);
        assertEquals("SOURCE_MISSING", formal.readCatalog(batch).status());
        List<MesProEdhrReverseTraceSourceAdapter> adapters = java.util.Arrays.stream(MesProEdhrReverseTraceModels.Category.values())
                .map(value -> value == category ? formal : new MesProEdhrReverseTraceTestFixtureAdapter(value)).toList();
        service = new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService, adapters);
        var catalog = service.getCatalog(new MesProEdhrReverseTraceModels.CatalogRequest().setAnchorBatchExecutionId(String.valueOf(batch.getId())));
        var query = queryRequest().setCatalogVersion(catalog.getCatalogVersion()).setConditions(List.of(
                new MesProEdhrReverseTraceModels.Condition().setConditionId("C1").setEvidenceKey(categoryName + ":missing")
                        .setSourceView("RECORDED").setOperator("EQ").setValue("x")));
        var result = service.query(query);
        if ("query".equals(entryPoint)) {
            assertEquals("BLOCKED", result.getQueryStatus());
            assertEquals("SOURCE_MISSING", result.getReasonCode());
            assertEquals("批次正式来源关联缺失", result.getReason());
            assertNull(result.getTotal());
            assertTrue(result.getList().isEmpty());
        } else {
            var evidence = service.evidence(new MesProEdhrReverseTraceModels.EvidenceRequest()
                    .setAnchorBatchExecutionId(query.getAnchorBatchExecutionId()).setTargetBatchExecutionId(query.getAnchorBatchExecutionId())
                    .setCatalogVersion(query.getCatalogVersion()).setQueryHash(result.getQueryHash())
                    .setTargetScope(query.getTargetScope()).setLogic(query.getLogic()).setConditions(query.getConditions()));
            assertEquals("BLOCKED", evidence.getEvidenceStatus());
            assertEquals("SOURCE_MISSING", evidence.getReasonCode());
            assertEquals("批次正式来源关联缺失", evidence.getReason());
            assertNull(evidence.getTotal());
            assertTrue(evidence.getItems().isEmpty());
        }
    }

    @Test
    void catalogBlocksNonReleasedAnchorWithoutThrowingSystemError() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        org.mockito.Mockito.doNothing().when(visibilityService).requireVisibleBatch(any(), any());
        when(releaseTransactionMapper.selectByBatchExecutionId(batch.getId())).thenReturn(null);

        MesProEdhrReverseTraceModels.CatalogResponse response = service.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest()
                        .setAnchorBatchExecutionId(String.valueOf(batch.getId())));

        assertEquals(String.valueOf(batch.getId()), response.getAnchorBatchExecutionId());
        assertEquals(6, response.getCategories().size());
        assertTrue(response.getCategories().stream().allMatch(category ->
                "BLOCKED".equals(category.getStatus())
                        && "BATCH_SCOPE_INVALID".equals(category.getReasonCode())));
        assertEquals(null, response.getTotal());
        assertTrue(response.getItems().isEmpty());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"INSPECTION,true", "MATERIAL,true", "INSPECTION,false", "MATERIAL,false", "FIELD,true"})
    void formalCatalogProjectsBlockedAndLegalEmptyCountsSeparately(String categoryName, boolean filtered) {
        var batch = releasedBatch();
        stubCompleteChainContext(batch);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        var category = MesProEdhrReverseTraceModels.Category.valueOf(categoryName);
        var sourceOrigins = org.mockito.Mockito.mock(MesProEdhrBatchExecutionOriginMapper.class);
        var executions = org.mockito.Mockito.mock(
                cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper.class);
        if (category == FIELD) {
            when(executions.selectPage(any(cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO.class)))
                    .thenReturn(new PageResult<>(List.of(), 0L));
        }
        var formal = new MesProEdhrFormalReverseTraceAdapter(category, executions, sourceOrigins, null,
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper.class), null,
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper.class),
                null, null, null, null, null, null);
        assertEquals(category == FIELD ? "NO_RECORDED_FACT" : "SOURCE_MISSING", formal.readCatalog(batch).status());
        List<MesProEdhrReverseTraceSourceAdapter> adapters = java.util.Arrays.stream(MesProEdhrReverseTraceModels.Category.values())
                .map(value -> value == category ? formal : new MesProEdhrReverseTraceTestFixtureAdapter(value)).toList();
        service = new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService, adapters);
        var catalog = service.getCatalog(new MesProEdhrReverseTraceModels.CatalogRequest()
                .setAnchorBatchExecutionId(String.valueOf(batch.getId())).setCategory(filtered ? category : null));
        var state = catalog.getCategories().stream().filter(value -> value.getCategory() == category).findFirst().orElseThrow();
        assertEquals(category == FIELD ? "NO_RECORDED_FACT" : "BLOCKED", state.getStatus());
        assertEquals(category == FIELD ? "NO_RECORDED_FACT" : "SOURCE_MISSING", state.getReasonCode());
        assertTrue(state.getReason() != null && !state.getReason().isBlank());
        if (filtered) {
            assertTrue(catalog.getItems().isEmpty());
            if (category == FIELD) assertEquals(0L, catalog.getTotal());
            else assertNull(catalog.getTotal());
        } else {
            assertEquals(6, catalog.getCategories().size());
            assertEquals(5L, catalog.getTotal());
            assertEquals(5, catalog.getItems().size());
            assertTrue(catalog.getItems().stream().noneMatch(item -> item.getCategory() == category));
        }
    }

    @Test
    void evidenceRejectsMissingQueryIdentityAndDoesNotReadBusinessSources() {
        MesProEdhrReverseTraceModels.EvidenceRequest request =
                new MesProEdhrReverseTraceModels.EvidenceRequest()
                        .setCatalogVersion("catalog-v1")
                        .setQueryHash("query-hash")
                        .setTargetBatchExecutionId("");

        assertThrows(IllegalArgumentException.class, () -> service.evidence(request));
        verifyNoMoreInteractions(batchExecutionMapper, visibilityService);
    }

    @Test
    void canonicalQueryHashChangesForTimeBoundaryConditionOrderAndQualifier() {
        MesProEdhrReverseTraceModels.QueryRequest first = queryRequest()
                .setTargetScope(new MesProEdhrReverseTraceModels.TargetScope()
                        .setKind("RELEASED_HISTORY")
                        .setReleaseApprovedFrom(LocalDateTime.of(2026, 9, 1, 0, 0)))
                .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition()
                        .setConditionId("C1").setEvidenceKey("field-1").setSourceView("saved_record")
                        .setOperator("eq").setValue(" 32 ")
                        .setQualifiers(Map.of("material", "MAT-1"))));
        MesProEdhrReverseTraceModels.QueryRequest reordered = queryRequest()
                .setTargetScope(new MesProEdhrReverseTraceModels.TargetScope()
                        .setKind("RELEASED_HISTORY")
                        .setReleaseApprovedFrom(LocalDateTime.of(2026, 9, 2, 0, 0)))
                .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition()
                        .setConditionId("C2").setEvidenceKey("field-2").setSourceView("saved_record")
                        .setOperator("EQ").setValue("32")
                        .setQualifiers(Map.of("material", "MAT-1"))));

        assertTrue(!MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(first)
                .equals(MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(reordered)));
    }

    @Test
    void canonicalQueryHashIgnoresIndependentResultPaging() {
        MesProEdhrReverseTraceModels.QueryRequest query = queryRequest()
                .setPageNo(1).setPageSize(20);
        MesProEdhrReverseTraceModels.QueryRequest evidence = queryRequest()
                .setPageNo(2).setPageSize(100);

        assertEquals(MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(query),
                MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(evidence));
    }

    @Test
    void tenantMismatchRejectsBeforeObjectVisibilityCheck() {
        MesProEdhrBatchExecutionDO batch = releasedBatch().setTenantId(2L);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));

        MesProEdhrReverseTraceModels.CatalogResponse response = service.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest()
                        .setAnchorBatchExecutionId("900000000000000001"));
        assertEquals("BATCH_SCOPE_INVALID", response.getCategories().get(0).getReasonCode());
        verifyNoMoreInteractions(visibilityService);
    }

    @Test
    void objectVisibilityDenialStopsCatalogBeforeSourceEvaluation() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("not visible"))
                .when(visibilityService).requireVisibleBatch(batch, null);

        MesProEdhrReverseTraceModels.CatalogResponse response = service.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest()
                        .setAnchorBatchExecutionId("900000000000000001"));
        assertEquals("BATCH_SCOPE_INVALID", response.getCategories().get(0).getReasonCode());
        verify(visibilityService).requireVisibleBatch(batch, null);
    }

    @Test
    void queryRequestCarriesReleasedTimeBoundariesAndStillBlocksWithoutAdapters() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubCompleteChainContext(batch);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        MesProEdhrReverseTraceModels.QueryResponse response = service.query(queryRequest()
                .setCatalogVersion("stale-client-version")
                .setTargetScope(new MesProEdhrReverseTraceModels.TargetScope()
                        .setKind("RELEASED_HISTORY")
                        .setReleaseApprovedFrom(LocalDateTime.of(2026, 9, 1, 0, 0))
                        .setReleaseApprovedTo(LocalDateTime.of(2026, 9, 30, 23, 59))));

        assertEquals("CATALOG_STALE", response.getReasonCode());
        ArgumentCaptor<EdhrBatchExecutionPageReqVO> captor = ArgumentCaptor.forClass(EdhrBatchExecutionPageReqVO.class);
        verify(batchExecutionMapper, org.mockito.Mockito.atLeastOnce()).selectPage(captor.capture());
        assertTrue(java.util.Arrays.stream(captor.getAllValues().toArray())
                .anyMatch(value -> ((EdhrBatchExecutionPageReqVO) value).getReleaseApprovedTime() != null));
    }

    @Test
    void emptyCatalogVersionIsStructuredStaleInsteadOfAcceptedAsAnyNonEmptyToken() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubCompleteChainContext(batch);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));

        MesProEdhrReverseTraceModels.QueryResponse response = service.query(queryRequest().setCatalogVersion(""));

        assertEquals("CATALOG_STALE", response.getReasonCode());
        assertEquals("BLOCKED", response.getQueryStatus());
        assertTrue(response.getList().isEmpty());
    }

    @Test
    void typedCanonicalValuesDoNotCollide() {
        MesProEdhrReverseTraceModels.QueryRequest number = queryRequest().setConditions(List.of(
                new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                        .setEvidenceKey("field-1").setSourceView("RECORDED")
                        .setOperator("EQ").setValue(1)));
        MesProEdhrReverseTraceModels.QueryRequest text = queryRequest().setConditions(List.of(
                new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                        .setEvidenceKey("field-1").setSourceView("RECORDED")
                        .setOperator("EQ").setValue("1")));
        MesProEdhrReverseTraceModels.QueryRequest bool = queryRequest().setConditions(List.of(
                new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                        .setEvidenceKey("field-1").setSourceView("RECORDED")
                        .setOperator("EQ").setValue(true)));

        assertTrue(!MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(number)
                .equals(MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(text)));
        assertTrue(!MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(text)
                .equals(MesProEdhrReverseTraceServiceImpl.canonicalQueryHash(bool)));
    }

    @Test
    void nullCandidateTotalIsRejectedInsteadOfConvertedToSuccess() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubReleasedTransaction();
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), null));

        assertThrows(IllegalArgumentException.class, () -> service.query(queryRequest()));
    }

    @Test
    void sixClassTestFixturesCanEvaluateAndReadEvidenceWithoutProductionAdapters() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubReleasedTransaction();
        stubCompleteChainContext(batch);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        when(visibilityService.canViewBatch(any(), any())).thenReturn(true);
        org.mockito.Mockito.doNothing().when(visibilityService).requireVisibleBatch(any(), any());
        List<MesProEdhrReverseTraceSourceAdapter> fixtures = java.util.Arrays.stream(
                MesProEdhrReverseTraceModels.Category.values())
                .map(category -> (MesProEdhrReverseTraceSourceAdapter)
                        new MesProEdhrReverseTraceTestFixtureAdapter(category)).toList();
        MesProEdhrReverseTraceServiceImpl fixtureService =
                new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                        completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService,
                        fixtures);

        MesProEdhrReverseTraceModels.CatalogResponse catalog = fixtureService.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest()
                        .setAnchorBatchExecutionId("900000000000000001"));
        MesProEdhrReverseTraceModels.QueryRequest query = queryRequest()
                .setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition()
                        .setConditionId("C1").setEvidenceKey("fixture-field")
                        .setSourceView("RECORDED").setOperator("EQ").setValue("fixture")));
        MesProEdhrReverseTraceModels.QueryResponse result = fixtureService.query(query);

        assertEquals("MATCHED", result.getQueryStatus());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void combinedConditionsIntersectOnTheSameBatchExecution() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubReleasedTransaction();
        stubCompleteChainContext(batch);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        when(visibilityService.canViewBatch(any(), any())).thenReturn(true);
        org.mockito.Mockito.doNothing().when(visibilityService).requireVisibleBatch(any(), any());
        List<MesProEdhrReverseTraceSourceAdapter> fixtures = java.util.Arrays.stream(
                MesProEdhrReverseTraceModels.Category.values())
                .map(category -> (MesProEdhrReverseTraceSourceAdapter)
                        new MesProEdhrReverseTraceTestFixtureAdapter(category)).toList();
        MesProEdhrReverseTraceServiceImpl fixtureService =
                new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                        completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService,
                        fixtures);
        MesProEdhrReverseTraceModels.CatalogResponse catalog = fixtureService.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest().setAnchorBatchExecutionId(String.valueOf(batch.getId())));

        MesProEdhrReverseTraceModels.QueryResponse result = fixtureService.query(queryRequest()
                .setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(
                        new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                                .setEvidenceKey("fixture-field").setSourceView("RECORDED").setOperator("EQ").setValue("fixture"),
                        new MesProEdhrReverseTraceModels.Condition().setConditionId("C2")
                                .setEvidenceKey("fixture-parameter").setSourceView("RECORDED").setOperator("EQ").setValue("fixture")
                                .setQualifiers(Map.of("routeVersionId", "3001", "routeProcessId", "3002",
                                        "processId", "3003", "unit", "C")))));

        assertEquals("MATCHED", result.getQueryStatus());
        assertEquals(1L, result.getTotal());
        assertEquals(2, result.getList().get(0).getMatchCount());
    }

    @Test
    void parameterConditionRequiresCompleteFormalSemanticIdentityAtServiceBoundary() {
        MesProEdhrBatchExecutionDO batch = releasedBatch();
        stubReleasedTransaction();
        stubCompleteChainContext(batch);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(batch), 1L));
        when(visibilityService.canViewBatch(any(), any())).thenReturn(true);
        org.mockito.Mockito.doNothing().when(visibilityService).requireVisibleBatch(any(), any());
        List<MesProEdhrReverseTraceSourceAdapter> fixtures = java.util.Arrays.stream(
                MesProEdhrReverseTraceModels.Category.values())
                .map(category -> (MesProEdhrReverseTraceSourceAdapter)
                        new MesProEdhrReverseTraceTestFixtureAdapter(category)).toList();
        MesProEdhrReverseTraceServiceImpl fixtureService =
                new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                        completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService,
                        fixtures);
        var catalog = fixtureService.getCatalog(new MesProEdhrReverseTraceModels.CatalogRequest()
                .setAnchorBatchExecutionId(String.valueOf(batch.getId())));
        var missing = new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                .setEvidenceKey("fixture-parameter").setSourceView("RECORDED").setOperator("EQ").setValue("fixture");
        var partial = new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                .setEvidenceKey("fixture-parameter").setSourceView("RECORDED").setOperator("EQ").setValue("fixture")
                .setQualifiers(Map.of("routeVersionId", "3001", "routeProcessId", "3002", "processId", "3003"));
        var complete = new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                .setEvidenceKey("fixture-parameter").setSourceView("RECORDED").setOperator("EQ").setValue("fixture")
                .setQualifiers(Map.of("routeVersionId", "3001", "routeProcessId", "3002", "processId", "3003", "unit", "C"));

        assertEquals("BLOCKED", fixtureService.query(queryRequest().setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(missing))).getQueryStatus());
        assertEquals("BLOCKED", fixtureService.query(queryRequest().setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(partial))).getQueryStatus());
        var matched = fixtureService.query(queryRequest().setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(complete)));
        assertEquals("MATCHED", matched.getQueryStatus());
        assertEquals(1L, matched.getTotal());
    }

    @Test
    void multipleCandidatesUseEachBatchSourceVersionForAndIntersection() {
        MesProEdhrBatchExecutionDO newer = releasedBatch();
        MesProEdhrBatchExecutionDO older = releasedBatch().setId(900000000000000000L);
        stubReleasedTransaction();
        stubCompleteChainContext(newer);
        stubCompleteChainContext(older);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class))).thenAnswer(invocation -> {
            EdhrBatchExecutionPageReqVO request = invocation.getArgument(0);
            return request.getBatchExecutionIds() == null
                    ? new PageResult<>(List.of(newer, older), 2L)
                    : new PageResult<>(List.of(newer), 1L);
        });
        when(visibilityService.canViewBatch(any(), any())).thenReturn(true);
        org.mockito.Mockito.doNothing().when(visibilityService).requireVisibleBatch(any(), any());
        List<MesProEdhrReverseTraceSourceAdapter> fixtures = java.util.Arrays.stream(
                MesProEdhrReverseTraceModels.Category.values())
                .map(category -> (MesProEdhrReverseTraceSourceAdapter)
                        new MesProEdhrReverseTraceTestFixtureAdapter(category)).toList();
        MesProEdhrReverseTraceServiceImpl fixtureService =
                new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                        completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService,
                        fixtures);
        MesProEdhrReverseTraceModels.CatalogResponse catalog = fixtureService.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest().setAnchorBatchExecutionId(String.valueOf(newer.getId())));

        MesProEdhrReverseTraceModels.QueryResponse result = fixtureService.query(queryRequest()
                .setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition()
                        .setConditionId("C1").setEvidenceKey("fixture-field")
                        .setSourceView("RECORDED").setOperator("EQ").setValue("fixture"))));

        assertEquals("MATCHED", result.getQueryStatus());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void qualifiedDuplicateCatalogItemsAreResolvedForAnchorAndEachCandidate() {
        MesProEdhrBatchExecutionDO newer = releasedBatch();
        MesProEdhrBatchExecutionDO older = releasedBatch().setId(900000000000000000L);
        stubReleasedTransaction();
        stubCompleteChainContext(newer);
        stubCompleteChainContext(older);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class))).thenAnswer(invocation -> {
            EdhrBatchExecutionPageReqVO request = invocation.getArgument(0);
            return request.getBatchExecutionIds() == null
                    ? new PageResult<>(List.of(newer, older), 2L)
                    : new PageResult<>(List.of(newer), 1L);
        });
        when(visibilityService.canViewBatch(any(), any())).thenReturn(true);
        org.mockito.Mockito.doNothing().when(visibilityService).requireVisibleBatch(any(), any());
        List<MesProEdhrReverseTraceSourceAdapter> fixtures = java.util.Arrays.stream(
                MesProEdhrReverseTraceModels.Category.values())
                .map(category -> (MesProEdhrReverseTraceSourceAdapter)
                        new MesProEdhrReverseTraceTestFixtureAdapter(category, category == FIELD)).toList();
        MesProEdhrReverseTraceServiceImpl fixtureService =
                new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                        completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService,
                        fixtures);
        MesProEdhrReverseTraceModels.CatalogResponse catalog = fixtureService.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest().setAnchorBatchExecutionId(String.valueOf(newer.getId())));
        assertTrue(catalog.getItems().stream().anyMatch(item -> "fixture-field".equals(item.getEvidenceKey())
                && "B".equals(item.getQualifiers().get("sampleId"))));

        MesProEdhrReverseTraceModels.QueryResponse result = fixtureService.query(queryRequest()
                .setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition()
                        .setConditionId("C1").setEvidenceKey("fixture-field")
                        .setSourceView("RECORDED").setOperator("EQ").setValue("fixture")
                        .setQualifiers(Map.of("sampleId", "B")))));

        assertEquals("MATCHED", result.getQueryStatus());
        assertEquals(2L, result.getTotal());
    }

    @ParameterizedTest
    @MethodSource("invalidEvaluations")
    void queryBlocksInvalidEvaluationWithoutPartialResults(EvaluationResult evaluation,
                                                           String reasonCode, String reason) {
        MesProEdhrReverseTraceModels.EvidenceRequest request = prepareInvalidEvaluation(evaluation);
        MesProEdhrReverseTraceModels.QueryResponse response = service.query(queryRequest()
                .setCatalogVersion(request.getCatalogVersion()).setConditions(request.getConditions()));

        assertEquals("BLOCKED", response.getQueryStatus());
        assertEquals("BLOCKED", response.getCoverageStatus());
        assertEquals(reasonCode, response.getReasonCode());
        assertEquals(reason, response.getReason());
        assertNull(response.getTotal());
        assertTrue(response.getList().isEmpty());
    }

    @Test
    void formalEmptySourceCandidatePreservesTheOtherBatchMatch() {
        MesProEdhrBatchExecutionDO newer = releasedBatch();
        MesProEdhrBatchExecutionDO older = releasedBatch().setId(900000000000000000L);
        stubCompleteChainContext(newer);
        stubCompleteChainContext(older);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class))).thenAnswer(invocation -> {
            EdhrBatchExecutionPageReqVO request = invocation.getArgument(0);
            return request.getBatchExecutionIds() == null
                    ? new PageResult<>(List.of(newer, older), 2L) : new PageResult<>(List.of(newer), 1L);
        });
        when(visibilityService.canViewBatch(any(), any())).thenReturn(true);
        var executions = org.mockito.Mockito.mock(
                cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper.class);
        when(executions.selectPage(any(cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO.class)))
                .thenAnswer(invocation -> {
                    var request = (cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO) invocation.getArgument(0);
                    return request.getBatchExecutionId().equals(newer.getId())
                            ? new PageResult<>(List.of(new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO()
                                    .setId(3001L).setBatchExecutionId(newer.getId()).setBatchRecordVersionId(4001L)
                                    .setBatchRecordReportId("report-A").setFieldAuditRevision(2L)
                                    .setExecutionSnapshotJson("{\"fields\":[{\"fieldKey\":\"temperature\",\"fieldPath\":\"temperature\",\"label\":\"temperature\",\"rowIndex\":1,\"columnIndex\":2,\"valueType\":\"NUMBER\"}]}")
                                    .setCellValuesJson("[{\"rowIndex\":1,\"columnIndex\":2,\"value\":32}]")), 1L)
                            : new PageResult<>(List.of(), 0L);
                });
        MesProEdhrReverseTraceSourceAdapter field = new MesProEdhrFormalReverseTraceAdapter(FIELD,
                executions, originMapper, null,
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper.class), null,
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper.class),
                null, null, null, null, null, null);
        List<MesProEdhrReverseTraceSourceAdapter> adapters = java.util.Arrays.stream(MesProEdhrReverseTraceModels.Category.values())
                .map(category -> category == FIELD ? field : new MesProEdhrReverseTraceTestFixtureAdapter(category)).toList();
        service = new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService, adapters);
        var catalog = service.getCatalog(new MesProEdhrReverseTraceModels.CatalogRequest().setAnchorBatchExecutionId(String.valueOf(newer.getId())));
        var query = queryRequest().setCatalogVersion(catalog.getCatalogVersion()).setConditions(List.of(
                new MesProEdhrReverseTraceModels.Condition().setConditionId("C1").setEvidenceKey("FIELD:4001:[\"report-A\",\"temperature\",\"temperature\"]")
                        .setSourceView("SAVED_RECORD").setOperator("EQ").setValue(32)));

        var response = service.query(query);

        assertEquals("MATCHED", response.getQueryStatus());
        assertEquals("COMPLETE", response.getCoverageStatus());
        assertEquals(1L, response.getTotal());
        assertEquals(String.valueOf(newer.getId()), response.getList().get(0).getBatchExecutionId());
        var evidence = service.evidence(new MesProEdhrReverseTraceModels.EvidenceRequest()
                .setAnchorBatchExecutionId(query.getAnchorBatchExecutionId()).setTargetBatchExecutionId(String.valueOf(newer.getId()))
                .setCatalogVersion(query.getCatalogVersion()).setQueryHash(response.getQueryHash())
                .setLogic(query.getLogic()).setTargetScope(query.getTargetScope()).setConditions(query.getConditions()));
        assertEquals("MATCHED", evidence.getEvidenceStatus());
        assertEquals(1L, evidence.getTotal());
    }

    @Test
    void completeNonMatchingCandidateDoesNotEraseAnotherMatchOrBlockEvidence() {
        MesProEdhrReverseTraceModels.EvidenceRequest request = prepareInvalidEvaluation(new EvaluationResult(
                "fixture-v1-900000000000000000", "fixture-source-900000000000000000", "COMPLETE", null, null, List.of()));

        var query = service.query(queryRequest().setCatalogVersion(request.getCatalogVersion())
                .setConditions(request.getConditions()));
        var evidence = service.evidence(request);

        assertEquals("MATCHED", query.getQueryStatus());
        assertEquals(1L, query.getTotal());
        assertEquals("900000000000000001", query.getList().get(0).getBatchExecutionId());
        assertEquals("MATCHED", evidence.getEvidenceStatus());
        assertEquals(1L, evidence.getTotal());
    }

    @ParameterizedTest
    @MethodSource("invalidEvaluations")
    void evidenceBlocksInvalidEvaluationWithoutPartialResults(EvaluationResult evaluation,
                                                              String reasonCode, String reason) {
        MesProEdhrReverseTraceModels.EvidenceRequest request = prepareInvalidEvaluation(evaluation);
        MesProEdhrReverseTraceModels.EvidenceResponse response = service.evidence(request);

        assertEquals("BLOCKED", response.getEvidenceStatus());
        assertEquals(reasonCode, response.getReasonCode());
        assertEquals(reason, response.getReason());
        assertNull(response.getTotal());
        assertNull(response.getChainContext());
        assertTrue(response.getItems().isEmpty());
    }

    private static Stream<Arguments> invalidEvaluations() {
        String version = "fixture-v1-900000000000000000";
        String identity = "fixture-source-900000000000000000";
        return Stream.of(
                Arguments.of(null, "SOURCE_MISSING", "正式来源评估缺失"),
                Arguments.of(new EvaluationResult(version, identity, null, null, null, List.of()),
                        "SOURCE_MISSING", "正式来源评估未完成"),
                Arguments.of(new EvaluationResult(version, identity, "BLOCKED", " ", "\t", List.of()),
                        "SOURCE_MISSING", "正式来源评估未完成"),
                Arguments.of(new EvaluationResult(version, identity, "BLOCKED", "SOURCE_CONFLICT", null, List.of()),
                        "SOURCE_CONFLICT", "正式来源评估未完成"),
                Arguments.of(new EvaluationResult(version, identity, "BLOCKED", null, "正式记录缺失", List.of()),
                        "SOURCE_MISSING", "正式记录缺失"),
                Arguments.of(new EvaluationResult(version, identity, "BLOCKED", "SOURCE_CONFLICT", "正式记录冲突", List.of()),
                        "SOURCE_CONFLICT", "正式记录冲突"),
                Arguments.of(new EvaluationResult("changed-version", identity, "COMPLETE", null, null, List.of()),
                        "SOURCE_CONFLICT", "正式来源评估版本或身份不一致"),
                Arguments.of(new EvaluationResult(null, identity, "COMPLETE", null, null, List.of()),
                        "SOURCE_CONFLICT", "正式来源评估版本或身份不一致"),
                Arguments.of(new EvaluationResult(version, "changed-identity", "COMPLETE", null, null, List.of()),
                        "SOURCE_CONFLICT", "正式来源评估版本或身份不一致"),
                Arguments.of(new EvaluationResult(version, " ", "COMPLETE", null, null, List.of()),
                        "SOURCE_CONFLICT", "正式来源评估版本或身份不一致"));
    }

    private MesProEdhrReverseTraceModels.EvidenceRequest prepareInvalidEvaluation(EvaluationResult evaluation) {
        MesProEdhrBatchExecutionDO newer = releasedBatch();
        MesProEdhrBatchExecutionDO older = releasedBatch().setId(900000000000000000L);
        stubCompleteChainContext(newer);
        stubCompleteChainContext(older);
        when(batchExecutionMapper.selectPage(any(EdhrBatchExecutionPageReqVO.class))).thenAnswer(invocation -> {
            EdhrBatchExecutionPageReqVO request = invocation.getArgument(0);
            return request.getBatchExecutionIds() == null
                    ? new PageResult<>(List.of(newer, older), 2L)
                    : new PageResult<>(List.of(newer), 1L);
        });
        when(visibilityService.canViewBatch(any(), any())).thenReturn(true);
        MesProEdhrReverseTraceSourceAdapter field = spy(new MesProEdhrReverseTraceTestFixtureAdapter(FIELD));
        testedField = field;
        List<MesProEdhrReverseTraceSourceAdapter> fixtures = java.util.Arrays.stream(
                MesProEdhrReverseTraceModels.Category.values())
                .map(category -> category == FIELD ? field : new MesProEdhrReverseTraceTestFixtureAdapter(category))
                .toList();
        service = new MesProEdhrReverseTraceServiceImpl(batchExecutionMapper, originMapper, activeOrderMapper,
                completionReceiptMapper, releaseApplicationMapper, releaseTransactionMapper, visibilityService, fixtures);
        MesProEdhrReverseTraceModels.CatalogResponse catalog = service.getCatalog(
                new MesProEdhrReverseTraceModels.CatalogRequest().setAnchorBatchExecutionId(String.valueOf(newer.getId())));
        MesProEdhrReverseTraceModels.QueryRequest query = queryRequest().setCatalogVersion(catalog.getCatalogVersion())
                .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition().setConditionId("C1")
                        .setEvidenceKey("fixture-field").setSourceView("RECORDED").setOperator("EQ").setValue("fixture")));
        MesProEdhrReverseTraceModels.QueryResponse matched = service.query(query);
        assertEquals("MATCHED", matched.getQueryStatus());
        assertEquals(2L, matched.getTotal());
        doAnswer(invocation -> Objects.equals(invocation.getArgument(0, MesProEdhrBatchExecutionDO.class).getId(), older.getId())
                ? evaluation : invocation.callRealMethod())
                .when(field).evaluate(any(MesProEdhrBatchExecutionDO.class), anyList());
        return new MesProEdhrReverseTraceModels.EvidenceRequest()
                .setAnchorBatchExecutionId(query.getAnchorBatchExecutionId()).setTargetBatchExecutionId(String.valueOf(newer.getId()))
                .setCatalogVersion(query.getCatalogVersion()).setQueryHash(matched.getQueryHash())
                .setTargetScope(query.getTargetScope()).setLogic(query.getLogic()).setConditions(query.getConditions());
    }

    private void stubCompleteChainContext(MesProEdhrBatchExecutionDO batch) {
        Long batchId = batch.getId();
        Long activeOrderId = 7001L;
        Long receiptId = 7002L;
        Long applicationId = 7003L;
        Long releaseTransactionId = 7004L;
        String snapshot = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Map.of(
                "activeOrderBinding", Map.of("id", activeOrderId, "tenantId", tenantId(),
                        "workOrderId", batch.getWorkOrderId(), "routeId", batch.getRouteId(), "routeVersionId", batch.getRouteVersionId()),
                "workOrderBinding", Map.of("id", batch.getWorkOrderId())));
        MesProcessPoolActiveOrderCompletionReceiptDO receipt =
                new MesProcessPoolActiveOrderCompletionReceiptDO().setId(receiptId)
                        .setActiveOrderId(activeOrderId).setWorkOrderId(batch.getWorkOrderId())
                        .setRouteId(batch.getRouteId()).setRouteVersionId(batch.getRouteVersionId())
                        .setCompletedVersion(1).setBatchRecordId(7010L).setProcessInspectionId(7011L)
                        .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                        .setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                        .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                        .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                        .setFormalSourceSnapshotJson(snapshot).setLossConditionFactsJson("[]");
        receipt.setTenantId(tenantId());
        receipt.setSourceSnapshotHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(snapshot) + "|[]"));
        receipt.setReceiptHash(cn.iocoder.yudao.module.mes.service.pro.processpool.team
                .MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        when(originMapper.selectListByBatchExecutionId(batchId)).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(batchId).setTenantId(tenantId())
                        .setActiveOrderId(activeOrderId).setWorkOrderId(batch.getWorkOrderId())
                        .setCompletionBackfillReceiptId(receiptId).setCompletionVersion(receipt.getCompletedVersion())
                        .setCompletionBackfillReceiptHash(receipt.getReceiptHash()).setSourceSnapshotHash(receipt.getSourceSnapshotHash())));
        when(activeOrderMapper.selectById(activeOrderId)).thenReturn(new MesProcessPoolActiveOrderDO()
                .setId(activeOrderId).setWorkOrderId(batch.getWorkOrderId()).setQaRegulationVersionId(7005L));
        when(completionReceiptMapper.selectByIdAndTenantId(receiptId, tenantId())).thenReturn(receipt);
        when(releaseApplicationMapper.selectListByBatchExecutionIds(List.of(batchId)))
                .thenReturn(List.of(new MesProcessPoolActiveOrderReleaseApplicationDO().setId(applicationId)
                        .setActiveOrderId(activeOrderId).setBatchExecutionId(batchId)
                        .setPqcReleaseWorkTaskId(7006L).setReleaseTransactionId(releaseTransactionId)
                        .setReleaseApprovalWorkTaskId(7007L)));
        when(releaseTransactionMapper.selectByBatchExecutionId(batchId))
                .thenReturn(new MesProEdhrReleaseTransactionDO().setId(releaseTransactionId)
                        .setBatchExecutionId(batchId).setReleaseStatus("RELEASED"));
    }

    private MesProEdhrReverseTraceModels.QueryRequest queryRequest() {
        return new MesProEdhrReverseTraceModels.QueryRequest()
                .setAnchorBatchExecutionId("900000000000000001")
                .setCatalogVersion("catalog-v1")
                .setLogic("AND")
                .setTargetScope(new MesProEdhrReverseTraceModels.TargetScope().setKind("RELEASED_HISTORY"))
                .setConditions(List.of(new MesProEdhrReverseTraceModels.Condition()
                        .setConditionId("C1").setEvidenceKey("opaque-catalog-reference")
                        .setSourceView("RECORDED").setOperator("EQ").setValue("x")));
    }

    private MesProEdhrBatchExecutionDO releasedBatch() {
        MesProEdhrBatchExecutionDO batch = new MesProEdhrBatchExecutionDO()
                .setId(900000000000000001L)
                .setTenantId(tenantId())
                .setBatchExecutionCode("BE-001")
                .setWorkOrderId(1001L)
                .setWorkOrderCode("MO-001")
                .setBatchCode("B-001")
                .setProductName("示例产品")
                .setRouteId(3000L)
                .setRouteVersionId(3001L);
        batch.setUpdateTime(LocalDateTime.of(2026, 9, 24, 10, 0));
        return batch;
    }

    private static Long tenantId() {
        return 1L;
    }

    private void stubReleasedTransaction() {
        when(releaseTransactionMapper.selectByBatchExecutionId(any()))
                .thenReturn(new MesProEdhrReleaseTransactionDO().setReleaseStatus("RELEASED"));
    }
}
