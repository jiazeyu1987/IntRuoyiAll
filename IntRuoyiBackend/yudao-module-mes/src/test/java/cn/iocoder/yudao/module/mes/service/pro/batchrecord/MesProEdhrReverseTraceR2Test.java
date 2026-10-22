package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionPublishProjectionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real FIELD adapter plus explicit empty category test doubles; no business/database writes. */
class MesProEdhrReverseTraceR2Test {
    private final Map<Long, MesProEdhrBatchExecutionOriginDO> origins = new LinkedHashMap<>();
    private final Map<Long, MesProcessPoolActiveOrderCompletionReceiptDO> receipts = new LinkedHashMap<>();
    private MesProEdhrReverseTraceServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        var originMapper = mock(MesProEdhrBatchExecutionOriginMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var receiptMapper = mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var transactions = mock(MesProEdhrReleaseTransactionMapper.class);
        var visibility = mock(MesProEdhrBatchExecutionVisibilityService.class);
        var anchor = batch(9002L);
        var emptyCandidate = batch(9001L);
        when(batches.selectPage(any(EdhrBatchExecutionPageReqVO.class))).thenAnswer(call -> {
            var request = call.getArgument(0, EdhrBatchExecutionPageReqVO.class);
            return request.getBatchExecutionIds() == null
                    ? new PageResult<>(List.of(anchor, emptyCandidate), 2L)
                    : new PageResult<>(List.of(anchor), 1L);
        });
        when(visibility.canViewBatch(any(), any())).thenReturn(true);
        for (var batch : List.of(anchor, emptyCandidate)) {
            var receipt = receipt(batch);
            receipts.put(batch.getId(), receipt);
            var origin = new MesProEdhrBatchExecutionOriginDO().setId(batch.getId() + 10)
                    .setTenantId(1L).setBatchExecutionId(batch.getId()).setActiveOrderId(6001L)
                    .setWorkOrderId(1001L).setCompletionBackfillReceiptId(receipt.getId())
                    .setCompletionVersion(1).setCompletionBackfillReceiptHash(receipt.getReceiptHash())
                    .setSourceSnapshotHash(receipt.getSourceSnapshotHash());
            origins.put(batch.getId(), origin);
            when(originMapper.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(origin));
            when(receiptMapper.selectByIdAndTenantId(receipt.getId(), 1L)).thenReturn(receipt);
            var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(batch.getId() + 30)
                    .setActiveOrderId(6001L).setWorkOrderId(1001L).setBatchExecutionId(batch.getId())
                    .setPqcReleaseWorkTaskId(7101L).setReleaseTransactionId(batch.getId() + 40)
                    .setReleaseApprovalWorkTaskId(7102L);
            application.setTenantId(1L);
            when(applications.selectListByBatchExecutionIds(List.of(batch.getId())))
                    .thenReturn(List.of(application));
            when(transactions.selectByBatchExecutionId(batch.getId())).thenReturn(
                    new MesProEdhrReleaseTransactionDO().setId(batch.getId() + 40)
                            .setBatchExecutionId(batch.getId()).setWorkOrderId(1001L).setReleaseStatus("RELEASED"));
        }
        var order = new MesProcessPoolActiveOrderDO().setId(6001L).setWorkOrderId(1001L)
                .setRouteId(2001L).setRouteVersionId(2002L).setQaRegulationVersionId(2003L);
        order.setTenantId(1L);
        when(orders.selectById(6001L)).thenReturn(order);
        var executions = mock(MesProBatchRecordExecutionMapper.class);
        when(executions.selectPage(any(MesProBatchRecordExecutionPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(), 0L));
        var tasks = mock(MesProEdhrBatchExecutionTaskMapper.class);
        var task = new MesProEdhrBatchExecutionTaskDO().setId(3001L).setBatchExecutionId(anchor.getId())
                .setFormTemplateId(4001L).setFormTemplateVersionId(4002L)
                .setFormBindingKey("r2-dynamic").setFormCenterInstanceId(5001L);
        when(tasks.selectListByBatchExecutionId(anchor.getId())).thenReturn(List.of(task));
        when(tasks.selectListByBatchExecutionId(emptyCandidate.getId())).thenReturn(List.of());
        var instances = mock(FormActionInstanceMapper.class);
        var instance = new FormActionInstanceDO().setId(5001L).setDataDomain("MES").setSystemCode("MES")
                .setObjectType("EDHR_ROUTE_FORM").setObjectId("3001").setObjectVersion("2002")
                .setActionCode(MesProRouteVersionPublishProjectionServiceImpl.routeFormActionCode(2002L, "r2-dynamic"))
                .setFormDataJson("{\"batchExecutionId\":9002,\"batchTaskId\":3001,\"formTemplateId\":4001,"
                        + "\"formTemplateVersionId\":4002,\"temperature\":32}");
        instance.setTenantId(1L);
        when(instances.selectById(5001L)).thenReturn(instance);
        List<MesProEdhrReverseTraceSourceAdapter> adapters = new ArrayList<>();
        adapters.add(new MesProEdhrFormalReverseTraceAdapter(Category.FIELD, executions, originMapper,
                mock(MesProEdhrBatchExecutionTraceLinkMapper.class), tasks, instances,
                mock(MesProBatchRecordExecutionFieldAuditItemMapper.class), mock(MesProProcessPoolEventMapper.class),
                mock(MesProProcessPoolPqcRecordMapper.class), mock(MesProcessPoolSubmissionReviewMapper.class),
                applications, transactions, receiptMapper));
        for (Category category : Category.values()) {
            if (category == Category.FIELD) continue;
            var empty = mock(MesProEdhrReverseTraceSourceAdapter.class);
            when(empty.category()).thenReturn(category);
            when(empty.readCatalog(any())).thenReturn(new MesProEdhrReverseTraceSourceAdapter.CatalogResult(
                    "empty-v1", "empty-" + category, "NO_RECORDED_FACT", "NO_RECORDED_FACT", "No recorded facts", List.of()));
            adapters.add(empty);
        }
        service = new MesProEdhrReverseTraceServiceImpl(batches, originMapper, orders, receiptMapper,
                applications, transactions, visibility, adapters);
    }

    @AfterEach
    void tearDown() { TenantContextHolder.clear(); }

    @ParameterizedTest
    @ValueSource(longs = {9007199254740990L, 9007199254740991L, 9007199254740992L,
            1923456789012345678L, Long.MAX_VALUE, -9007199254740991L, Long.MIN_VALUE})
    void realWriterNumberSerializerPreservesEveryCommonIdentity(long id) throws Exception {
        var original = JsonUtils.getObjectMapper();
        try {
            var configuration = new YudaoJacksonAutoConfiguration();
            configuration.jsonUtils(original.copy().registerModule(configuration.timestampSupportModuleBean()));
            var batch = batch(9002L).setWorkOrderId(id).setRouteId(id).setRouteVersionId(id);
            batch.setTenantId(id);
            var receipt = receipt(batch).setActiveOrderId(id).setWorkOrderId(id).setRouteId(id).setRouteVersionId(id);
            receipt.setTenantId(id);
            receipt.setFormalSourceSnapshotJson(JsonUtils.toJsonString(Map.of(
                    "activeOrderBinding", Map.of("id", id, "tenantId", id, "workOrderId", id,
                            "routeId", id, "routeVersionId", id, "leaderUserId", id),
                    "workOrderBinding", Map.of("id", id, "productId", id, "batchCode", "R2"))));
            var token = JsonUtils.getObjectMapper().readTree(receipt.getFormalSourceSnapshotJson())
                    .get("activeOrderBinding").get("id");
            assertEquals(id >= 9007199254740991L || id <= -9007199254740991L, token.isTextual());
            assertEquals(Long.toString(id), token.asText());
            var origin = origins.get(9002L).setActiveOrderId(id).setWorkOrderId(id);
            origin.setTenantId(id);
            bindHashes(receipt, origin);
            assertDoesNotThrow(() -> MesProEdhrReverseTraceReceiptValidator.validate(batch, origin, receipt));
        } finally {
            JsonUtils.init(original);
        }
        assertSame(original, JsonUtils.getObjectMapper());
    }

    @ParameterizedTest
    @ValueSource(strings = {"6001.0", "6001.5", "6.001e3", "\"6001.0\"", "\"6.001e3\"",
            "\"+6001\"", "\"06001\"", "\" 6001\"", "\"6001 \"", "\"\"", "\"no-id\"",
            "9223372036854775808", "-9223372036854775809", "\"9223372036854775808\"",
            "\"-9223372036854775809\"", "true", "[]", "{}", "\"6002\"", "\"-0\""})
    void nonWriterIdentityTokensRemainConflictsEvenWithValidHashes(String jsonToken) throws Exception {
        var receipt = receipts.get(9002L);
        var snapshot = (ObjectNode) JsonUtils.getObjectMapper().readTree(receipt.getFormalSourceSnapshotJson());
        ((ObjectNode) snapshot.get("activeOrderBinding")).set("id", JsonUtils.getObjectMapper().readTree(jsonToken));
        receipt.setFormalSourceSnapshotJson(JsonUtils.toJsonString(snapshot));
        var origin = origins.get(9002L);
        bindHashes(receipt, origin);
        var exception = assertThrows(IllegalStateException.class,
                () -> MesProEdhrReverseTraceReceiptValidator.validate(batch(9002L), origin, receipt));
        assertTrue(exception.getMessage().startsWith("SOURCE_CONFLICT:"));
    }

    @Test
    void canonicalDecimalStringsKeepAllServiceEntrypointsAvailable() throws Exception {
        for (var entry : receipts.entrySet()) {
            var receipt = entry.getValue();
            var snapshot = (ObjectNode) JsonUtils.getObjectMapper().readTree(receipt.getFormalSourceSnapshotJson());
            for (String binding : List.of("activeOrderBinding", "workOrderBinding")) {
                var object = (ObjectNode) snapshot.get(binding);
                object.fieldNames().forEachRemaining(name -> object.put(name, object.get(name).asText()));
            }
            receipt.setFormalSourceSnapshotJson(JsonUtils.toJsonString(snapshot));
            bindHashes(receipt, origins.get(entry.getKey()));
        }
        validDynamicFieldStillHasCatalogMatchAndEvidenceWhenOtherCategoriesHaveNoRecords();
    }

    @Test
    void mysqlJsonFormattingAndObjectKeyOrderMustNotInvalidateCanonicalSourceHash() throws Exception {
        var receipt = receipts.get(9002L);
        var origin = origins.get(9002L);
        String canonicalHash = canonicalSourceHash(receipt.getFormalSourceSnapshotJson(),
                receipt.getLossConditionFactsJson());
        receipt.setSourceSnapshotHash(canonicalHash);
        receipt.setFormalSourceSnapshotJson(" { \"workOrderBinding\" : { \"id\" : 1001 }, "
                + "\"pqcDetails\" : [ ], \"activeOrderBinding\" : { \"routeVersionId\" : 2002,"
                + "\"routeId\" : 2001, \"workOrderId\" : 1001, \"tenantId\" : 1, \"id\" : 6001 },"
                + "\"formalProductIssueDetails\" : { }, \"completions\" : [ ], \"snapshots\" : [ ],"
                + "\"allocations\" : [ ], \"pickListBindings\" : [ ], \"pickListBindingItems\" : { },"
                + "\"formalProductIssues\" : [ ], \"pqcTasks\" : [ ] } ");
        receipt.setLossConditionFactsJson(" [ ] ");
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        origin.setSourceSnapshotHash(canonicalHash).setCompletionBackfillReceiptHash(receipt.getReceiptHash());

        assertDoesNotThrow(() -> MesProEdhrReverseTraceReceiptValidator.validate(
                batch(9002L), origin, receipt));
    }

    @Test
    void nestedLossJsonFormattingMustUseTheSameCanonicalSourceContract() throws Exception {
        var receipt = receipts.get(9002L);
        var origin = origins.get(9002L);
        String rawLoss = "[{\"b\":{\"y\":2,\"x\":1},\"a\":1}]";
        String equivalentLoss = " [ { \"a\" : 1.0, \"b\" : { \"x\" : 1, \"y\" : 2 } } ] ";
        receipt.setLossConditionFactsJson(equivalentLoss);
        receipt.setSourceSnapshotHash(canonicalSourceHash(receipt.getFormalSourceSnapshotJson(), rawLoss));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        origin.setSourceSnapshotHash(receipt.getSourceSnapshotHash())
                .setCompletionBackfillReceiptHash(receipt.getReceiptHash());

        assertDoesNotThrow(() -> MesProEdhrReverseTraceReceiptValidator.validate(
                batch(9002L), origin, receipt));
    }

    @Test
    void changingFormalSourceAndRehashingReceiptMustStillConflictWithImmutableOrigin() throws Exception {
        var receipt = receipts.get(9002L);
        var origin = origins.get(9002L);
        String originalSourceHash = origin.getSourceSnapshotHash();
        receipt.setFormalSourceSnapshotJson(receipt.getFormalSourceSnapshotJson()
                .replace("\"completions\":[]", "\"completions\":[{\"id\":999}]"));
        receipt.setSourceSnapshotHash(canonicalSourceHash(receipt.getFormalSourceSnapshotJson(),
                receipt.getLossConditionFactsJson()));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> MesProEdhrReverseTraceReceiptValidator.validate(batch(9002L), origin, receipt));
        assertTrue(exception.getMessage().startsWith("SOURCE_CONFLICT:"));
        assertEquals(originalSourceHash, origin.getSourceSnapshotHash());
    }

    private void bindHashes(MesProcessPoolActiveOrderCompletionReceiptDO receipt, MesProEdhrBatchExecutionOriginDO origin) {
        receipt.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer
                .sourceSnapshotHash(receipt.getFormalSourceSnapshotJson(), receipt.getLossConditionFactsJson()));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        origin.setSourceSnapshotHash(receipt.getSourceSnapshotHash());
        origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
    }

    private String canonicalSourceHash(String formalSourceSnapshotJson, String lossConditionFactsJson)
            throws Exception {
        return MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(
                formalSourceSnapshotJson, lossConditionFactsJson);
    }

    @Test
    void validDynamicFieldStillHasCatalogMatchAndEvidenceWhenOtherCategoriesHaveNoRecords() {
        var catalog = service.getCatalog(catalogRequest());
        assertEquals("AVAILABLE", catalog.getCategories().get(0).getStatus());
        assertEquals(1L, catalog.getTotal());
        var query = queryRequest(catalog);
        var result = service.query(query);
        assertEquals("MATCHED", result.getQueryStatus());
        assertEquals("COMPLETE", result.getCoverageStatus());
        assertEquals(1L, result.getTotal());
        var evidence = service.evidence(evidenceRequest(query, result));
        assertEquals("MATCHED", evidence.getEvidenceStatus());
        assertEquals(1L, evidence.getTotal());
        assertTrue(evidence.getItems().get(0).getSourceRef().contains("bpm-form-instance:5001"));
    }

    @Test
    void validChainWithDifferentFieldValueReturnsCompleteNoMatch() {
        var query = queryRequest(service.getCatalog(catalogRequest()));
        query.getConditions().get(0).setValue(99);
        var result = service.query(query);
        assertEquals("NO_MATCH", result.getQueryStatus());
        assertEquals("COMPLETE", result.getCoverageStatus());
        assertEquals(0L, result.getTotal());
    }

    @ParameterizedTest(name = "{0}, corrupt batch {1}")
    @CsvSource({"originHash,9002", "originHash,9001", "version,9002", "version,9001",
            "status,9002", "status,9001", "workOrder,9002", "workOrder,9001",
            "snapshotHash,9002", "snapshotHash,9001", "receiptHash,9002", "receiptHash,9001",
            "batchRecordId,9002", "batchRecordId,9001", "inspectionId,9002", "inspectionId,9001",
            "routeId,9002", "routeId,9001", "routeVersion,9002", "routeVersion,9001",
            "missingRouteId,9002", "missingRouteId,9001", "missingRouteVersion,9002", "missingRouteVersion,9001",
            "snapshotRoot,9002", "snapshotRoot,9001", "snapshotMissingActive,9002", "snapshotMissingActive,9001",
            "snapshotMissingWork,9002", "snapshotMissingWork,9001", "snapshotActiveArray,9002", "snapshotActiveArray,9001",
            "snapshotWorkArray,9002", "snapshotWorkArray,9001", "snapshotId,9002", "snapshotId,9001",
            "snapshotTenant,9002", "snapshotTenant,9001", "snapshotWorkOrder,9002", "snapshotWorkOrder,9001",
            "snapshotRoute,9002", "snapshotRoute,9001", "snapshotVersion,9002", "snapshotVersion,9001",
            "snapshotWorkId,9002", "snapshotWorkId,9001", "snapshotMissingVersion,9002", "snapshotMissingVersion,9001",
            "snapshotFractionalId,9002", "snapshotFractionalId,9001"})
    void allEntrypointsRejectCommonChainCorruptionEvenForCandidateWithoutField(String defect, long batchId) {
        var validCatalog = service.getCatalog(catalogRequest());
        var query = queryRequest(validCatalog);
        var validResult = service.query(query);
        assertEquals("MATCHED", validResult.getQueryStatus(), "Fixture must have a genuine FIELD success path first");
        var evidenceRequest = evidenceRequest(query, validResult);
        corrupt(defect, batchId);
        var catalog = service.getCatalog(catalogRequest());
        var result = service.query(query);
        var evidence = service.evidence(evidenceRequest);
        String reason = defect.equals("batchRecordId") || defect.equals("inspectionId")
                || defect.equals("missingRouteId") || defect.equals("missingRouteVersion")
                || defect.startsWith("snapshotMissing")
                ? "SOURCE_MISSING" : "SOURCE_CONFLICT";
        assertAll(
                () -> assertTrue(catalog.getCategories().stream().allMatch(c -> "BLOCKED".equals(c.getStatus()))),
                () -> assertTrue(catalog.getCategories().stream().allMatch(c -> reason.equals(c.getReasonCode()))),
                () -> assertNull(catalog.getTotal()),
                () -> assertTrue(catalog.getItems().isEmpty()),
                () -> assertEquals("BLOCKED", result.getQueryStatus()),
                () -> assertEquals("BLOCKED", result.getCoverageStatus()),
                () -> assertEquals(reason, result.getReasonCode()),
                () -> assertNull(result.getTotal()),
                () -> assertTrue(result.getList().isEmpty()),
                () -> assertEquals("BLOCKED", evidence.getEvidenceStatus()),
                () -> assertEquals(reason, evidence.getReasonCode()),
                () -> assertNull(evidence.getTotal()),
                () -> assertTrue(evidence.getItems().isEmpty()));
    }

    private void corrupt(String defect, long batchId) {
        var origin = origins.get(batchId);
        var receipt = receipts.get(batchId);
        switch (defect) {
            case "originHash" -> origin.setCompletionBackfillReceiptHash("conflicting-origin-hash");
            case "version" -> origin.setCompletionVersion(2);
            case "status" -> {
                receipt.setReceiptStatus("BACKFILL_FAILED");
                receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
                origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
            }
            case "workOrder" -> {
                receipt.setWorkOrderId(9999L);
                receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
                origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
            }
            case "snapshotHash" -> {
                receipt.setSourceSnapshotHash("corrupted-source-hash");
                origin.setSourceSnapshotHash(receipt.getSourceSnapshotHash());
                receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
                origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
            }
            case "receiptHash" -> {
                receipt.setReceiptHash("corrupted-receipt-hash");
                origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
            }
            case "batchRecordId", "inspectionId" -> {
                if (defect.equals("batchRecordId")) receipt.setBatchRecordId(null);
                else receipt.setProcessInspectionId(null);
                receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
                origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
            }
            case "routeId", "routeVersion", "missingRouteId", "missingRouteVersion" -> {
                switch (defect) {
                    case "routeId" -> receipt.setRouteId(9991L);
                    case "routeVersion" -> receipt.setRouteVersionId(9992L);
                    case "missingRouteId" -> receipt.setRouteId(null);
                    case "missingRouteVersion" -> receipt.setRouteVersionId(null);
                    default -> throw new IllegalArgumentException(defect);
                }
                // Keep the receipt and origin hashes valid: this must fail on frozen route identity itself.
                receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
                origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
            }
            case "snapshotRoot", "snapshotMissingActive", "snapshotMissingWork", "snapshotActiveArray",
                    "snapshotWorkArray", "snapshotId", "snapshotTenant", "snapshotWorkOrder", "snapshotRoute",
                    "snapshotVersion", "snapshotWorkId", "snapshotMissingVersion", "snapshotFractionalId" -> {
                var snapshot = com.alibaba.fastjson.JSON.parseObject(receipt.getFormalSourceSnapshotJson());
                var binding = snapshot.getJSONObject("activeOrderBinding");
                switch (defect) {
                    case "snapshotMissingActive" -> snapshot.remove("activeOrderBinding");
                    case "snapshotMissingWork" -> snapshot.remove("workOrderBinding");
                    case "snapshotActiveArray" -> snapshot.put("activeOrderBinding", List.of());
                    case "snapshotWorkArray" -> snapshot.put("workOrderBinding", List.of());
                    case "snapshotId" -> binding.put("id", 9999L);
                    case "snapshotTenant" -> binding.put("tenantId", 9999L);
                    case "snapshotWorkOrder" -> binding.put("workOrderId", 9999L);
                    case "snapshotRoute" -> binding.put("routeId", 9999L);
                    case "snapshotVersion" -> binding.put("routeVersionId", 9999L);
                    case "snapshotWorkId" -> snapshot.getJSONObject("workOrderBinding").put("id", 9999L);
                    case "snapshotMissingVersion" -> binding.remove("routeVersionId");
                    case "snapshotFractionalId" -> binding.put("id", new java.math.BigDecimal("6001.5"));
                    case "snapshotRoot" -> { }
                    default -> throw new IllegalArgumentException(defect);
                }
                receipt.setFormalSourceSnapshotJson(defect.equals("snapshotRoot") ? "[]" : snapshot.toJSONString());
                receipt.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer
                        .sourceSnapshotHash(receipt.getFormalSourceSnapshotJson(), receipt.getLossConditionFactsJson()));
                origin.setSourceSnapshotHash(receipt.getSourceSnapshotHash());
                receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
                origin.setCompletionBackfillReceiptHash(receipt.getReceiptHash());
            }
            default -> throw new IllegalArgumentException(defect);
        }
    }

    private CatalogRequest catalogRequest() {
        return new CatalogRequest().setAnchorBatchExecutionId("9002").setCategory(Category.FIELD);
    }

    private QueryRequest queryRequest(CatalogResponse catalog) {
        return new QueryRequest().setAnchorBatchExecutionId("9002").setCatalogVersion(catalog.getCatalogVersion())
                .setLogic("AND").setTargetScope(new TargetScope().setKind("RELEASED_HISTORY"))
                .setConditions(List.of(new Condition().setConditionId("C1").setEvidenceKey("FIELD_DYNAMIC:4002:temperature")
                        .setSourceView("SAVED_RECORD").setOperator("EQ").setValue(32)));
    }

    private EvidenceRequest evidenceRequest(QueryRequest query, QueryResponse response) {
        return new EvidenceRequest().setAnchorBatchExecutionId("9002").setTargetBatchExecutionId("9002")
                .setCatalogVersion(query.getCatalogVersion()).setQueryHash(response.getQueryHash())
                .setTargetScope(query.getTargetScope()).setLogic(query.getLogic()).setConditions(query.getConditions());
    }

    private MesProEdhrBatchExecutionDO batch(long id) {
        return new MesProEdhrBatchExecutionDO().setId(id).setTenantId(1L).setWorkOrderId(1001L)
                .setRouteId(2001L).setRouteVersionId(2002L).setBatchExecutionCode("R2-" + id);
    }

    private MesProcessPoolActiveOrderCompletionReceiptDO receipt(MesProEdhrBatchExecutionDO batch) {
        String snapshot = "{\"activeOrderBinding\":{\"id\":6001,\"tenantId\":1,\"workOrderId\":1001,"
                + "\"routeId\":2001,\"routeVersionId\":2002},\"workOrderBinding\":{\"id\":1001},"
                + "\"allocations\":[],\"pqcTasks\":[],\"pqcDetails\":[],\"snapshots\":[],"
                + "\"pickListBindings\":[],\"pickListBindingItems\":{},\"completions\":[],"
                + "\"formalProductIssues\":[],\"formalProductIssueDetails\":{}}";
        var receipt = new MesProcessPoolActiveOrderCompletionReceiptDO().setId(batch.getId() + 20)
                .setActiveOrderId(6001L).setWorkOrderId(1001L).setRouteId(2001L).setRouteVersionId(2002L)
                .setFormalSourceSnapshotJson(snapshot).setLossConditionFactsJson("[]")
                .setReceiptStatus(MesProcessPoolActiveOrderCompletionReceiptDO.RECEIPT_STATUS_BACKFILL_SUCCEEDED)
                .setCompletionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.STATUS_SUCCESS)
                .setBatchRecordStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setProcessInspectionStatus(MesProcessPoolActiveOrderCompletionReceiptDO.BACKFILL_STATUS_SUCCESS)
                .setCompletedVersion(1).setBatchRecordId(8201L).setProcessInspectionId(8301L);
        receipt.setTenantId(1L);
        receipt.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer
                .sourceSnapshotHash(snapshot, "[]"));
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        return receipt;
    }
}
