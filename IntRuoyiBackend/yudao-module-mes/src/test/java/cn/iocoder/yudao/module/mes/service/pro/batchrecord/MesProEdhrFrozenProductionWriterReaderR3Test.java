package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.*;
import cn.iocoder.yudao.module.mes.enums.wm.MesWmProductIssueStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real completion/backfill/receipt hashing/reader; only persistence and other domain ports are doubles.
 * The transaction flag is a unit-test boundary, not evidence of MySQL locks or database rollback. */
class MesProEdhrFrozenProductionWriterReaderR3Test {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 9, 24, 9, 0);
    @Mock private MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    @Mock private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock private MesProcessPoolOrderProcessCompletionMapper completionMapper;
    @Mock private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock private MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;
    @Mock private MesProWorkOrderMapper workOrderMapper;
    @Mock private MesTeamLeaderActiveOrderReleaseLossSourceReader lossSourceReader;
    @Mock private MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    @Mock private MesWmProductIssueMapper productIssueMapper;
    @Mock private MesWmProductIssueDetailMapper productIssueDetailMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingMapper pickListBindingMapper;
    @Mock private MesProcessPoolActiveOrderPickListBindingItemMapper pickListBindingItemMapper;
    // CALLS_REAL_METHODS allows new mapper default methods to run their real wrapper construction.
    @Mock(answer = Answers.CALLS_REAL_METHODS) private MesProProcessPoolEventMapper eventMapper;
    @Mock(answer = Answers.CALLS_REAL_METHODS) private MesProcessPoolSubmissionReviewMapper reviewMapper;
    @InjectMocks private MesTeamLeaderActiveOrderCompletionBackfillPortImpl writer;
    @Mock private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock private MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;
    @Mock private MesTeamLeaderActiveOrderCompletionProgressPort progressPort;
    @Mock private MesTeamLeaderActiveOrderPickListCompletionSourceService pickListSource;
    @Mock private MesActiveOrderTransferTraceService transferTrace;
    @Mock private MesPqcProcessInspectionAggregationService aggregation;
    @Mock private GxpAuditService gxpAuditService;
    @Mock private MesProEdhrBatchExecutionOriginMapper origins;
    @Mock private MesProEdhrBatchExecutionTraceLinkMapper traceLinks;
    @Mock private MesProProcessPoolPqcRecordMapper pqcRecords;
    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    @Mock private MesProEdhrReleaseTransactionMapper releases;
    private AutoCloseable mocks;
    private MesTeamLeaderActiveOrderCompletionServiceImpl completionService;
    private MesProcessPoolActiveOrderDO order;
    private MesProcessPoolReportAllocationDO allocation;
    private MesProProcessPoolEventDO event;
    private MesProcessPoolSubmissionReviewDO review;
    private MesProcessPoolActiveOrderCompletionReceiptDO persisted;
    private Long forcedFirstBackfillId;
    private final List<MesProcessPoolActiveOrderCompletionBackfillDO> materializations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        completionService = new MesTeamLeaderActiveOrderCompletionServiceImpl(activeOrderMapper, receiptMapper,
                progressPort, writer, pickListSource, transferTrace, aggregation);
        ReflectionTestUtils.setField(completionService, "gxpAuditService", gxpAuditService);
        ReflectionTestUtils.setField(completionService, "affectedStateCollector",
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector.class));
        order = MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).activeStatus("ACTIVE").version(2).build();
        order.setTenantId(1L);
        event = new MesProProcessPoolEventDO().setId(401L).setWorkOrderId(30L)
                .setRouteId(40L).setRouteProcessId(101L).setProcessId(1L).setEventType("PRODUCTION_SUBMIT")
                .setActualEmployeeId(51L).setSignatureId(52L).setSignatureUserId(51L)
                .setSignatureSnapshot(JsonUtils.toJsonString(Map.of("signatureId", 52L, "actorId", 51L)))
                .setRawPayload(payload(32, 77)).setServerSubmitTime(AT);
        event.setTenantId(1L);
        review = new MesProcessPoolSubmissionReviewDO().setId(601L).setEventId(401L)
                .setLeaderUserId(20L).setLeaderType("PRODUCTION").setReviewStatus("APPROVED")
                .setReviewedAt(AT.plusHours(1)).setReviewSignatureId(602L).setReviewSignatureUserId(20L)
                .setReviewSignatureSnapshotJson(reviewSignature(601L, 20L));
        review.setTenantId(1L);
        allocation = MesProcessPoolReportAllocationDO.builder().id(201L).activeOrderId(10L)
                .workOrderId(30L).routeProcessId(101L).processId(1L).eventId(401L).reviewId(601L)
                .leaderUserId(20L).allocatedQuantity(BigDecimal.TEN).allocationMode("MANUAL")
                .lifecycleStatus("CURRENT").createdVersion(1).confirmedAt(AT.plusHours(1)).build();
        allocation.setTenantId(1L);
        doAnswer(call -> event).when(eventMapper).selectOne(any(Wrapper.class));
        doAnswer(call -> review).when(reviewMapper).selectOne(any(Wrapper.class));
        doAnswer(call -> List.of(event)).when(eventMapper).selectProductionSubmitsByIds(List.of(401L));
        doAnswer(call -> List.of(review)).when(reviewMapper).selectListByEventId(401L);
        wireOrderSources();
        when(progressPort.read(anyLong(), any())).thenReturn(new MesTeamLeaderActiveOrderCompletionProgress()
                .setProductionProgressPercent(BigDecimal.valueOf(100))
                .setInspectionProgressPercent(BigDecimal.valueOf(100)));
        when(activeOrderMapper.selectByIdForUpdate(anyLong())).thenAnswer(call -> order);
        when(activeOrderMapper.markCompleted(anyLong(), anyInt(), anyLong())).thenReturn(1);
        when(receiptMapper.selectByActiveOrderIdForUpdate(anyLong())).thenAnswer(call -> persisted);
        when(receiptMapper.selectByIdempotencyKeyForUpdate(anyString())).thenAnswer(call -> persisted);
        when(receiptMapper.insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class))).thenAnswer(call -> {
            persisted = call.getArgument(0);
            persisted.setId(order.getId() + 9000);
            return 1;
        });
        when(backfillMapper.insert(any(MesProcessPoolActiveOrderCompletionBackfillDO.class))).thenAnswer(call -> {
            MesProcessPoolActiveOrderCompletionBackfillDO row = call.getArgument(0);
            row.setId(forcedFirstBackfillId != null && materializations.isEmpty()
                    ? forcedFirstBackfillId : 1100L + materializations.size());
            materializations.add(row);
            return 1;
        });
        when(completionMapper.updateById(any(MesProcessPoolOrderProcessCompletionDO.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() throws Exception {
        TransactionSynchronizationManager.clear();
        mocks.close();
    }

    @Test
    void realWriterProducesHashBoundFactsThatTheReaderCanUse() {
        var batch = completeAndBind();
        JSONObject facts = productionFacts(persisted);
        assertEquals(1, facts.getIntValue("formatVersion"));
        var frozenEvent = facts.getJSONArray("events").getJSONObject(0);
        assertEquals(event.getRawPayload(), frozenEvent.getString("rawPayload"));
        assertEquals(DigestUtil.sha256Hex(event.getRawPayload()), frozenEvent.getString("payloadContentHash"));
        assertEquals(601L, facts.getJSONArray("reviews").getJSONObject(0).getLong("id"));
        assertEquals(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(persisted), persisted.getReceiptHash());
        assertEquals(2, materializations.size());
        assertParameter(batch, "32");
        assertTrue(keys(batch, Category.EQUIPMENT).contains("EQUIPMENT:PRODUCTION:deviceId:77"));
        assertTrue(keys(batch, Category.PERSON).contains("PERSON:EMPLOYEE:actualEmployeeId:51"));
        assertTrue(keys(batch, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:20"));
    }

    @Test
    void realWriterReceiptSurvivesReorderedJsonColumnReadbackInActualValidator() throws Exception {
        var batch = completeAndBind();
        var origin = origins.selectListByBatchExecutionId(batch.getId()).get(0);
        String formalSource = persisted.getFormalSourceSnapshotJson();
        String lossFacts = persisted.getLossConditionFactsJson();

        // Simulate a JSON column readback that preserves values but changes whitespace and object-key order.
        persisted.setFormalSourceSnapshotJson(" \n" + reverseObjectOrder(formalSource) + "\n ")
                .setLossConditionFactsJson(" \n" + reverseObjectOrder(lossFacts) + "\n ");

        assertDoesNotThrow(() -> MesProEdhrReverseTraceReceiptValidator.validate(batch, origin, persisted));
    }

    @Test
    void runtimeSerializerPreservesSnowflakeIdentityThroughRealWriterAndReader() {
        var originalMapper = JsonUtils.getObjectMapper();
        try {
            var runtimeMapper = originalMapper.copy().registerModule(
                    new cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration()
                            .timestampSupportModuleBean());
            JsonUtils.init(runtimeMapper);
            long snowflakeOrderId = 9_007_199_254_740_993L;
            order.setId(snowflakeOrderId);
            allocation.setActiveOrderId(snowflakeOrderId);
            wireOrderSources();
            var batch = completeAndBind();
            var binding = JsonUtils.parseTree(persisted.getFormalSourceSnapshotJson()).get("activeOrderBinding");
            assertTrue(binding.get("id").isTextual(), "use the actual runtime Long serializer contract");
            assertEquals(Long.toString(snowflakeOrderId), binding.get("id").textValue());
            assertEquals(snowflakeOrderId, persisted.getActiveOrderId());
            productionFacts(persisted);
            assertParameter(batch, "32");
            assertTrue(keys(batch, Category.EQUIPMENT).contains("EQUIPMENT:PRODUCTION:deviceId:77"));
            assertTrue(keys(batch, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:20"));
        } finally {
            JsonUtils.init(originalMapper);
        }
    }

    @Test
    void realWriterLargeBatchIdSurvivesEquivalentJsonReadbackAndCompletionReplay() throws Exception {
        var originalMapper = JsonUtils.getObjectMapper();
        try {
            var configuration = new cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration();
            configuration.jsonUtils(originalMapper.copy().registerModule(configuration.timestampSupportModuleBean()));
            forcedFirstBackfillId = 9_007_199_254_740_993L;

            completeAndBind();
            Long receiptId = persisted.getId();
            assertEquals(forcedFirstBackfillId, persisted.getBatchRecordId());
            var completed = completionMapper.selectListByWorkOrderIdsForUpdate(List.of(30L)).get(0);
            assertEquals(forcedFirstBackfillId, completed.getBackfillExecutionId());
            JsonNode replayId = JsonUtils.parseTree(persisted.getSignatureSnapshotJson())
                    .path("productionCompletionSignatures").path(0).path("backfillExecutionId");
            assertTrue(replayId.isMissingNode(), "receipt snapshot is captured before writeback outputs");

            persisted.setFormalSourceSnapshotJson(" \n" + reverseObjectOrder(persisted.getFormalSourceSnapshotJson()) + "\n ")
                    .setSignatureSnapshotJson(" \n" + reverseObjectOrder(persisted.getSignatureSnapshotJson()) + "\n ")
                    .setLossConditionFactsJson(" \n" + reverseObjectOrder(persisted.getLossConditionFactsJson()) + "\n ");
            order.setVersion(3).setActiveStatus("COMPLETED");

            var replay = completionService.complete(20L, command());
            assertEquals(receiptId, replay.getCompletionReceiptId());
            assertEquals(persisted.getReceiptHash(), replay.getReceiptHash());
            verify(receiptMapper, times(1)).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
            assertEquals(2, materializations.size());
        } finally {
            JsonUtils.init(originalMapper);
        }
    }

    @Test
    void productionSourceWithoutWorkOrderCanFreezeForAnExplicitTargetAllocation() {
        event.setWorkOrderId(null).setSignatureSnapshot(null);
        var batch = completeAndBind();
        var frozen = productionFacts(persisted).getJSONArray("events").getJSONObject(0);
        assertNull(frozen.getLong("workOrderId"), "do not invent the target order as source identity");
        assertNull(frozen.getString("signatureSnapshot"), "submit producer does not supply this snapshot");
        assertEquals(30L, persisted.getWorkOrderId());
        assertParameter(batch, "32");
        assertTrue(keys(batch, Category.EQUIPMENT).contains("EQUIPMENT:PRODUCTION:deviceId:77"));
        assertTrue(keys(batch, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:20"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"null", " "})
    void absentJsonObjectsAreExplicitlyRejectedBeforeMaterialization(String json) {
        event.setRawPayload(json);
        assertThrows(ServiceException.class, () -> completionService.complete(20L, command()));
        event.setRawPayload(payload(32, 77));
        review.setReviewSignatureSnapshotJson(json);
        assertThrows(ServiceException.class, () -> completionService.complete(20L, command()));
        verifyNoInteractions(backfillMapper);
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void originalReviewerAndLaterAllocatorRemainDistinctRoles() {
        review.setLeaderUserId(21L).setReviewSignatureUserId(21L)
                .setReviewSignatureSnapshotJson(reviewSignature(601L, 21L));
        assertEquals(20L, allocation.getLeaderUserId());
        var batch = completeAndBind();
        productionFacts(persisted);
        assertParameter(batch, "32");
        assertTrue(keys(batch, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:21"));
        assertFalse(keys(batch, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:20"));
    }

    @Test
    void laterCurrentPayloadAndReviewMutationCannotChangeFrozenHistory() {
        var batch = completeAndBind();
        productionFacts(persisted);
        String frozenReceiptHash = persisted.getReceiptHash();
        event.setRawPayload(payload(99, 88)).setActualEmployeeId(151L).setSignatureUserId(153L);
        review.setLeaderUserId(120L).setReviewSignatureUserId(120L);
        clearInvocations(eventMapper, reviewMapper);
        assertParameter(batch, "32");
        assertTrue(keys(batch, Category.EQUIPMENT).contains("EQUIPMENT:PRODUCTION:deviceId:77"));
        assertFalse(keys(batch, Category.EQUIPMENT).contains("EQUIPMENT:PRODUCTION:deviceId:88"));
        assertTrue(keys(batch, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:20"));
        verify(eventMapper, never()).selectProductionSubmitsByIds(anyList());
        verify(reviewMapper, never()).selectListByEventId(anyLong());
        assertEquals(frozenReceiptHash, persisted.getReceiptHash());
    }

    @Test
    void realRevisionServiceCanCorrectCurrentPayloadWithoutChangingTheFrozenReceipt() {
        var batch = completeAndBind();
        productionFacts(persisted);
        String originalPayload = event.getRawPayload();
        var revisionMapper = mock(MesProProcessPoolEventRevisionMapper.class);
        var diffs = mock(MesProProcessPoolEventRevisionDiffMapper.class);
        var fifo = mock(MesProcessPoolFifoAllocationService.class);
        var signatureService = mock(MesProBatchRecordExecutionSignatureService.class);
        doReturn(event).when(eventMapper).selectByIdForUpdate(401L);
        doReturn(null).when(eventMapper).selectBySignatureId(952L);
        when(revisionMapper.insert(any(MesProProcessPoolEventRevisionDO.class))).thenAnswer(call -> {
            MesProProcessPoolEventRevisionDO row = call.getArgument(0);
            assertEquals(originalPayload, row.getBeforePayload());
            assertEquals(payload(99, 88), row.getAfterPayload());
            row.setId(951L);
            return 1;
        });
        doAnswer(call -> {
            MesProProcessPoolEventDO update = call.getArgument(0);
            assertEquals(event.getId(), update.getId());
            event.setRawPayload(update.getRawPayload());
            return 1;
        }).when(eventMapper).updateById(any(MesProProcessPoolEventDO.class));
        var revisionService = new MesProcessPoolEventRevisionServiceImpl(eventMapper, revisionMapper,
                diffs, fifo, reviewMapper, signatureService);
        Long revisionId = revisionService.updateProductionReportRecord(new MesProcessPoolEventRevisionUpdateReqBO()
                .setEventId(401L).setAfterPayload(payload(99, 88)).setChangeReason("R3 correction isolation")
                .setRevisionSignatureId(952L).setRevisionSignatureUserId(53L).setModifiedByUserId(53L)
                .setRevisionSignatureSnapshot(JsonUtils.toJsonString(Map.of("signatureId", 952L, "actorId", 53L)))
                .setChangedFields(List.of(new MesProcessPoolEventRevisionFieldChangeBO()
                        .setFieldCode("DEVICE_PARAMETERS.TEMP").setFieldName("TEMP").setBeforeValue("32").setAfterValue("99")
                        .setAffectsQuantityFragment(false).setOriginalField(MesProcessPoolFragmentOriginalField.DEVICE_PARAMETERS))));
        assertEquals(951L, revisionId);
        assertEquals(payload(99, 88), event.getRawPayload());
        verifyNoInteractions(fifo);
        assertParameter(batch, "32");
        assertTrue(keys(batch, Category.EQUIPMENT).contains("EQUIPMENT:PRODUCTION:deviceId:77"));
    }

    @Test
    void alreadyCorrectedPayloadGetsAnExplicitContentVersionWithoutInventingRevisionId() {
        event.setRawPayload(payload(99, 88));
        var batch = completeAndBind();
        var frozen = productionFacts(persisted).getJSONArray("events").getJSONObject(0);
        assertEquals(DigestUtil.sha256Hex(payload(99, 88)), frozen.getString("payloadContentHash"));
        assertFalse(frozen.containsKey("revisionId"));
        assertParameter(batch, "99");
    }

    @Test
    void anotherBatchCanFreezeANewContentVersionOfTheSameEvent() {
        var first = completeAndBind();
        var firstReceipt = persisted;
        productionFacts(firstReceipt);
        order.setId(11L);
        allocation.setId(202L).setActiveOrderId(11L).setReviewId(611L).setLeaderUserId(21L);
        review.setId(611L).setLeaderUserId(21L).setReviewSignatureId(612L).setReviewSignatureUserId(21L)
                .setReviewSignatureSnapshotJson(reviewSignature(611L, 21L));
        event.setRawPayload(payload(99, 88));
        persisted = null;
        wireOrderSources();
        var second = completeAndBind();
        assertNotEquals(firstReceipt.getSourceSnapshotHash(), persisted.getSourceSnapshotHash());
        assertParameter(first, "32");
        assertParameter(second, "99");
        assertTrue(keys(first, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:20"));
        assertFalse(keys(first, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:21"));
        assertTrue(keys(second, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:21"));
    }

    @Test
    void unchangedReplayAndUnboundExtraReviewDoNotInsertAgain() {
        completeAndBind();
        productionFacts(persisted);
        Long receiptId = persisted.getId();
        var extra = new MesProcessPoolSubmissionReviewDO().setId(999L).setEventId(401L).setLeaderUserId(999L);
        doReturn(List.of(review, extra)).when(reviewMapper).selectListByEventId(401L);
        event.setReportManagementStatus("ARCHIVED");
        var completed = completionMapper.selectListByWorkOrderIdsForUpdate(List.of(30L)).get(0);
        completed.setUpdateTime(AT.plusDays(1));
        completed.setUpdater("20");
        assertEquals("SUCCESS", completed.getBackfillStatus());
        assertEquals(persisted.getBatchRecordId(), completed.getBackfillExecutionId());
        var liveDraft = writer.prepare(20L, order, command());
        var sourceCompletion = JsonUtils.parseTree(liveDraft.getFormalSourceSnapshotJson())
                .path("completions").get(0);
        assertFalse(sourceCompletion.has("backfillStatus"));
        assertFalse(sourceCompletion.has("backfillExecutionId"));
        var signatureCompletion = JsonUtils.parseTree(liveDraft.getSignatureSnapshotJson())
                .path("productionCompletionSignatures").get(0);
        assertEquals("SUCCESS", signatureCompletion.path("backfillStatus").asText());
        assertEquals(persisted.getBatchRecordId().longValue(),
                signatureCompletion.path("backfillExecutionId").asLong());
        order.setVersion(3).setActiveStatus("COMPLETED");
        var replay = completionService.complete(20L, command());
        assertEquals(receiptId, replay.getCompletionReceiptId());
        completed.setBackfillExecutionId(-1L);
        assertThrows(ServiceException.class, () -> completionService.complete(20L, command()));
        verify(receiptMapper, times(1)).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
        assertEquals(2, materializations.size());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"missingId", "differentId", "failedStatus", "error"})
    void changedWritebackBindingStillConflictsWithoutMutatingTheReceipt(String mutation) {
        completeAndBind();
        String receiptBefore = JSON.toJSONString(persisted);
        var completed = completionMapper.selectListByWorkOrderIdsForUpdate(List.of(30L)).get(0);
        switch (mutation) {
            case "missingId" -> completed.setBackfillExecutionId(null);
            case "differentId" -> completed.setBackfillExecutionId(persisted.getBatchRecordId() + 1);
            case "failedStatus" -> completed.setBackfillStatus("FAILED");
            case "error" -> completed.setBackfillError("writeback failed");
            default -> throw new IllegalArgumentException(mutation);
        }

        ServiceException conflict = assertThrows(ServiceException.class,
                () -> completionService.complete(20L, command()));

        assertEquals(1040760368, conflict.getCode());
        assertEquals(receiptBefore, JSON.toJSONString(persisted));
        verify(receiptMapper, times(1)).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
        verify(receiptMapper, never()).updateById(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
        assertEquals(2, materializations.size());
    }

    @Test
    void changedBoundPayloadConflictsOnReplayButOriginalReceiptStillReads() {
        var batch = completeAndBind();
        String originalJson = persisted.getFormalSourceSnapshotJson();
        event.setRawPayload(payload(99, 88));
        assertThrows(ServiceException.class, () -> completionService.complete(20L, command()));
        assertEquals(originalJson, persisted.getFormalSourceSnapshotJson());
        verify(receiptMapper, times(1)).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
        assertParameter(batch, "32");
    }

    @Test
    void changedBoundReviewConflictsOnReplay() {
        completeAndBind();
        review.setReviewedAt(AT.plusHours(2));
        assertThrows(ServiceException.class, () -> completionService.complete(20L, command()));
        verify(receiptMapper, times(1)).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void missingBoundEventFailsBeforeBackfillOrReceiptInsert() {
        doReturn(null).when(eventMapper).selectOne(any(Wrapper.class));
        assertRejectedBeforeMaterialization();
    }

    @Test
    void missingBoundReviewFailsBeforeBackfillOrReceiptInsert() {
        doReturn(null).when(reviewMapper).selectOne(any(Wrapper.class));
        assertRejectedBeforeMaterialization();
    }

    @Test
    void missingAllocationReviewIdCannotUseTheLatestReview() {
        allocation.setReviewId(null);
        assertRejectedBeforeMaterialization();
    }

    @Test
    void differentProcessEventCannotBeFrozenForThisAllocation() {
        event.setProcessId(999L);
        assertRejectedBeforeMaterialization();
    }

    @Test
    void sharedEventFromAnotherRouteUsesTargetParameterIdentityAndPreservesSourceIdentity() {
        // Shared-allocation producer selects the target by processId, not source routeProcessId.
        event.setWorkOrderId(930L).setRouteId(940L).setRouteProcessId(999L);
        var batch = completeAndBind();
        var frozenEvent = productionFacts(persisted).getJSONArray("events").getJSONObject(0);
        assertEquals(930L, frozenEvent.getLong("workOrderId"));
        assertEquals(940L, frozenEvent.getLong("routeId"));
        assertEquals(999L, frozenEvent.getLong("routeProcessId"));
        assertEquals(1L, frozenEvent.getLong("processId"));
        assertEquals(event.getRawPayload(), frozenEvent.getString("rawPayload"));
        assertEquals(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(persisted), persisted.getReceiptHash());
        var parameter = reader(Category.PARAMETER).readCatalog(batch).items().stream()
                .filter(row -> "PARAMETER:TEMP".equals(row.getEvidenceKey())).findFirst().orElseThrow();
        assertEquals("32", parameter.getSavedValue());
        assertEquals("41", parameter.getQualifiers().get("routeVersionId"));
        assertEquals("101", parameter.getQualifiers().get("routeProcessId"));
        assertEquals("1", parameter.getQualifiers().get("processId"));
        assertEquals("PARAMETER|TEMP|41|101|1|C", parameter.getSemanticIdentity());
        assertTrue(keys(batch, Category.EQUIPMENT).contains("EQUIPMENT:PRODUCTION:deviceId:77"));
        assertTrue(keys(batch, Category.PERSON).contains("PERSON:SYSTEM_USER:PRODUCTION_REVIEW:20"));
    }

    @Test
    void allocationOutsideTheTargetFrozenProcessCannotBeFrozen() {
        allocation.setRouteProcessId(999L);
        event.setRouteProcessId(999L);
        assertRejectedBeforeMaterialization();
    }

    @Test
    void missingPayloadCannotBecomeAnEmptySuccessfulFreeze() {
        event.setRawPayload(null);
        assertRejectedBeforeMaterialization();
    }

    @Test
    void signatureForAnotherReviewActorCannotBeFrozen() {
        review.setReviewSignatureSnapshotJson(reviewSignature(601L, 999L));
        assertRejectedBeforeMaterialization();
    }

    @Test
    void changedFrozenPayloadCannotPassTheExistingReceiptHash() {
        var batch = completeAndBind();
        var snapshot = JSON.parseObject(persisted.getFormalSourceSnapshotJson());
        assertNotNull(snapshot.getJSONObject("productionFacts"));
        snapshot.getJSONObject("productionFacts").getJSONArray("events").getJSONObject(0)
                .put("rawPayload", payload(99, 88));
        persisted.setFormalSourceSnapshotJson(snapshot.toJSONString());
        var error = assertThrows(IllegalStateException.class, () -> reader(Category.PARAMETER).readCatalog(batch));
        assertTrue(error.getMessage().startsWith("SOURCE_CONFLICT:"), error.getMessage());
    }

    @Test
    void legacyReceiptReplayDoesNotManufactureNewFrozenEvidence() {
        completeAndBind();
        var snapshot = JSON.parseObject(persisted.getFormalSourceSnapshotJson());
        assertNotNull(snapshot.remove("productionFacts"), "the original must be generated by the real writer");
        persisted.setFormalSourceSnapshotJson(snapshot.toJSONString());
        persisted.setSourceSnapshotHash(DigestUtil.sha256Hex(DigestUtil.sha256Hex(persisted.getFormalSourceSnapshotJson())
                + "|" + persisted.getLossConditionFactsJson()));
        persisted.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(persisted));
        String legacyJson = persisted.getFormalSourceSnapshotJson();
        assertThrows(ServiceException.class, () -> completionService.complete(20L, command()));
        assertEquals(legacyJson, persisted.getFormalSourceSnapshotJson());
        verify(receiptMapper, times(1)).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
        verify(receiptMapper, never()).updateById(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void reviewOfAnotherEventCannotBeFrozen() {
        review.setEventId(402L);
        assertRejectedBeforeMaterialization();
    }

    @Test
    void unsignedOrRejectedReviewCannotBeFrozen() {
        review.setReviewSignatureSnapshotJson(null);
        assertRejectedBeforeMaterialization();
        review.setReviewSignatureSnapshotJson(reviewSignature(601L, 20L)).setReviewStatus("REJECTED");
        assertRejectedBeforeMaterialization();
    }

    @Test
    void anotherTenantEventCannotBeFrozen() {
        event.setTenantId(2L);
        assertRejectedBeforeMaterialization();
    }

    @Test
    void eventLockFailureIsPropagatedWithoutRetryOrPartialReceipt() {
        var busy = new CannotAcquireLockException("R3 test: event is locked");
        doThrow(busy).when(eventMapper).selectOne(any(Wrapper.class));
        assertSame(busy, assertThrows(CannotAcquireLockException.class,
                () -> completionService.complete(20L, command())));
        verify(eventMapper, times(1)).selectOne(any(Wrapper.class));
        verifyNoInteractions(backfillMapper);
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    @Test
    void reviewLockFailureIsPropagatedWithoutRetryOrPartialReceipt() {
        var busy = new CannotAcquireLockException("R3 test: review is locked");
        doThrow(busy).when(reviewMapper).selectOne(any(Wrapper.class));
        assertSame(busy, assertThrows(CannotAcquireLockException.class,
                () -> completionService.complete(20L, command())));
        verify(reviewMapper, times(1)).selectOne(any(Wrapper.class));
        verifyNoInteractions(backfillMapper);
    }

    @Test
    void writerCannotFreezeWithoutTheExistingTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThrows(IllegalStateException.class, () -> writer.prepare(20L, order, command()));
        verifyNoInteractions(eventMapper, reviewMapper, backfillMapper);
    }

    @Test
    void receiptSourceReplaySelfInvocationAlsoRequiresTheExistingTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThrows(IllegalStateException.class, () -> writer.matchesReceiptSources(
                20L, order, command(), new MesProcessPoolActiveOrderCompletionReceiptDO()));
        verifyNoInteractions(eventMapper, reviewMapper, backfillMapper);
    }

    private void assertRejectedBeforeMaterialization() {
        assertThrows(RuntimeException.class, () -> completionService.complete(20L, command()));
        verifyNoInteractions(backfillMapper);
        verify(receiptMapper, never()).insert(any(MesProcessPoolActiveOrderCompletionReceiptDO.class));
    }

    private JSONObject productionFacts(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        JSONObject facts = JSON.parseObject(receipt.getFormalSourceSnapshotJson()).getJSONObject("productionFacts");
        assertNotNull(facts, "real writer must freeze productionFacts; test must not manufacture this JSON");
        return facts;
    }

    private String reverseObjectOrder(String json) throws Exception {
        return reverseObjectOrder(JsonUtils.getObjectMapper().readTree(json)).toString();
    }

    private JsonNode reverseObjectOrder(JsonNode node) {
        if (node.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.reverse(names);
            for (String name : names) {
                result.set(name, reverseObjectOrder(node.get(name)));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            node.elements().forEachRemaining(child -> result.add(reverseObjectOrder(child)));
            return result;
        }
        return node;
    }

    private MesTeamLeaderActiveOrderCompletionCommand command() {
        return new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(order.getId())
                .setExpectedVersion(2).setIdempotencyKey("r3-order-" + order.getId()).setConfirmNoReplenishmentInfo(true);
    }

    private MesProEdhrBatchExecutionDO completeAndBind() {
        completionService.complete(20L, command());
        assertNotNull(persisted, "capture the real completion service's receipt insert");
        var receipt = persisted;
        var batch = new MesProEdhrBatchExecutionDO().setId(order.getId() + 8000).setTenantId(1L)
                .setWorkOrderId(30L).setRouteId(40L).setRouteVersionId(41L).setAggregateHash("batch-hash");
        batch.setUpdateTime(AT.plusDays(1));
        when(origins.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionOriginDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                        .setWorkOrderId(30L).setActiveOrderId(order.getId())
                        .setCompletionBackfillReceiptId(receipt.getId()).setCompletionVersion(receipt.getCompletedVersion())
                        .setCompletionBackfillReceiptHash(receipt.getReceiptHash()).setSourceSnapshotHash(receipt.getSourceSnapshotHash())));
        when(receiptMapper.selectByIdAndTenantId(receipt.getId(), 1L)).thenReturn(receipt);
        String witness = JsonUtils.toJsonString(Map.of("sourceType", "PRODUCTION_SUBMIT",
                "sourceId", receipt.getBatchRecordId(), "witnessHash", receipt.getReceiptHash()));
        when(traceLinks.selectListByBatchExecutionId(batch.getId())).thenReturn(List.of(
                new MesProEdhrBatchExecutionTraceLinkDO().setBatchExecutionId(batch.getId()).setTenantId(1L)
                        .setLinkType("PRODUCTION_SUBMIT").setRelationStatus("BOUND").setSourceObjectId(receipt.getBatchRecordId())
                        .setSnapshotJson(witness).setSnapshotHash(MesProEdhrBatchTraceSourceHash.calculate("PRODUCTION_SUBMIT", witness))));
        return batch;
    }

    private MesProEdhrFormalReverseTraceAdapter reader(Category category) {
        return new MesProEdhrFormalReverseTraceAdapter(category, null, origins, traceLinks, null, null, null,
                eventMapper, pqcRecords, reviewMapper, applications, releases, receiptMapper);
    }

    private void assertParameter(MesProEdhrBatchExecutionDO batch, String expected) {
        var item = reader(Category.PARAMETER).readCatalog(batch).items().stream()
                .filter(row -> "PARAMETER:TEMP".equals(row.getEvidenceKey())).findFirst().orElseThrow();
        assertEquals(expected, item.getSavedValue());
    }

    private Set<String> keys(MesProEdhrBatchExecutionDO batch, Category category) {
        return reader(category).readCatalog(batch).items().stream().map(row -> row.getEvidenceKey()).collect(Collectors.toSet());
    }

    private static String payload(int value, int deviceId) {
        return JsonUtils.toJsonString(Map.of("deviceParameterReadings", List.of(Map.of("parameterCode", "TEMP",
                "value", value, "deviceId", deviceId, "unit", "C")),
                "selectedDevices", List.of(Map.of("deviceId", deviceId, "deviceCode", "D" + deviceId))));
    }

    private static String reviewSignature(long id, long actor) {
        return JsonUtils.toJsonString(Map.of("signatureId", id + 1, "actorId", actor,
                "processPoolEventId", 401L, "eventType", "PRODUCTION_SUBMIT", "leaderType", "PRODUCTION",
                "actionType", "TEAM_LEADER_REVIEW", "reviewStatus", "APPROVED", "reviewedAt", AT.plusHours(1)));
    }

    private void wireOrderSources() {
        long orderId = order.getId();
        var snapshot = MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(101L).activeOrderId(orderId)
                .workOrderId(30L).routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                .plannedQuantitySnapshot(BigDecimal.TEN).build();
        snapshot.setTenantId(1L);
        when(snapshotMapper.selectListByActiveOrderIdForUpdate(orderId)).thenReturn(List.of(snapshot));
        when(allocationMapper.selectListByActiveOrderIdForUpdate(orderId)).thenReturn(List.of(allocation));
        when(completionMapper.selectListByWorkOrderIdsForUpdate(List.of(30L))).thenReturn(List.of(
                MesProcessPoolOrderProcessCompletionDO.builder().id(301L).workOrderId(30L).routeProcessId(101L)
                        .processId(1L).targetQuantity(BigDecimal.TEN).confirmedQuantity(BigDecimal.TEN)
                        .completionStatus("COMPLETED").lastEventId(401L).lastReviewId(review.getId())
                        .sourceEventIdsJson("[401]").sourceAllocationIdsJson("[" + allocation.getId() + "]")
                        .aggregateHash("completion-hash").build()));
        var task = MesPqcInspectionTaskDO.builder().id(701L).activeOrderId(orderId).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L).qaProcessId(71L)
                .actualInspectionQuantity(1).submittedEventId(402L).taskStatus("CONFIRMED").build();
        task.setTenantId(1L);
        var detail = MesPqcProcessInspectionAggregateDetailDO.builder().id(801L).activeOrderId(orderId)
                .workOrderId(30L).routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L)
                .sourcePqcRecordId(901L).sourcePieceDetailId(1801L).eventId(402L).reviewId(603L)
                .actualInspectionQuantity(1).pqcTaskId(701L).sampleNo(1).itemCode("PQC-N")
                .regulationVersionId(72L).resultType("NUMBER").itemResult("1").measuredValue("1").judgement("PASS")
                .aggregatedAt(AT.plusHours(2)).build();
        detail.setTenantId(1L);
        when(pqcTaskMapper.selectListByActiveOrderIdForUpdate(orderId)).thenReturn(List.of(task));
        when(aggregateDetailMapper.selectListByActiveOrderIdForUpdate(orderId)).thenReturn(List.of(detail));
        var pqcEvent = new MesProProcessPoolEventDO().setId(402L).setEventType("PQC_INSPECTION")
                .setWorkOrderId(30L).setRouteId(40L).setQaProcessId(71L)
                .setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(701L)
                .setRecordbookSourceType("MES_PQC_INSPECTION_TASK").setRecordbookSourceId(701L)
                .setActualEmployeeId(61L).setSignatureUserId(61L).setServerSubmitTime(AT);
        pqcEvent.setTenantId(1L);
        doReturn(pqcEvent).when(eventMapper).selectById(402L);
        var pqcRecord = new MesProProcessPoolPqcRecordDO().setId(901L).setEventId(402L).setWorkOrderId(30L)
                .setRouteId(40L).setQaProcessId(71L).setActualEmployeeId(61L).setSignatureUserId(61L).setServerSubmitTime(AT);
        pqcRecord.setTenantId(1L);
        when(pqcRecords.selectByEventId(402L)).thenReturn(pqcRecord);
        var pqcReview = new MesProcessPoolSubmissionReviewDO().setId(603L).setEventId(402L)
                .setLeaderUserId(63L).setLeaderType("PQC").setReviewStatus("APPROVED")
                .setReviewedAt(AT.plusHours(1)).setReviewSignatureId(604L).setReviewSignatureUserId(63L)
                .setReviewSignatureSnapshotJson(JsonUtils.toJsonString(Map.of("signatureId", 604L,
                        "actorId", 63L, "processPoolEventId", 402L, "actionType", "TEAM_LEADER_REVIEW",
                        "eventType", "PQC_INSPECTION", "leaderType", "PQC", "reviewStatus", "APPROVED")));
        pqcReview.setTenantId(1L);
        doReturn(pqcReview).when(reviewMapper).selectById(603L);
        when(workOrderMapper.selectByIdForUpdate(30L)).thenReturn(MesProWorkOrderDO.builder()
                .id(30L).productId(1001L).batchCode("R3-BATCH").build());
        when(productIssueMapper.selectListByWorkOrderIdForUpdate(30L)).thenReturn(List.of(MesWmProductIssueDO.builder()
                .id(1901L).workOrderId(30L).status(MesWmProductIssueStatusEnum.FINISHED.getStatus()).code("R3-ISSUE").build()));
        when(productIssueDetailMapper.selectListByIssueIdForUpdate(1901L)).thenReturn(List.of(MesWmProductIssueDetailDO.builder()
                .id(1902L).issueId(1901L).lineId(1903L).materialStockId(1904L).itemId(1001L)
                .quantity(BigDecimal.ONE).batchId(1905L).batchCode("R3-RAW").build()));
        when(pickListBindingMapper.selectListByActiveOrderId(orderId)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingDO.builder().id(8801L).activeOrderId(orderId)
                        .workOrderId(30L).pickListId(9901L).sourceSnapshotHash("pick-hash").build()));
        when(pickListBindingItemMapper.selectListByBindingId(8801L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingItemDO.builder().id(8811L).bindingId(8801L).pickListItemId(9911L).build()));
        // Fixed loss-domain evidence isolates changes in the production freeze from unrelated loss hashes.
        var lossEvent = MesProProcessPoolEventDO.builder().id(401L).workOrderId(30L).routeId(40L)
                .routeProcessId(101L).processId(1L).build();
        var loss = new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource().setSnapshot(snapshot)
                .setFeedback(MesProFeedbackDO.builder().id(501L).workOrderId(30L).routeId(40L).processId(1L)
                        .unqualifiedQuantity(BigDecimal.ZERO).build()).setEvent(lossEvent)
                .setFormalLossQuantity(BigDecimal.ZERO).setHasActualLoss(false).setZeroLossConfirmed(true)
                .setLossDecision("NO_LOSS").setReplenishmentSources(List.of()).setLossDetails(List.of())
                .setAllocation(MesProcessPoolReportAllocationDO.builder().id(201L).build())
                .setReview(MesProcessPoolSubmissionReviewDO.builder().id(601L).build());
        when(lossSourceReader.read(any())).thenReturn(new MesTeamLeaderActiveOrderReleaseLossSourceReadResult()
                .setBlockers(List.of()).setProcessSources(List.of(loss)));
    }
}
