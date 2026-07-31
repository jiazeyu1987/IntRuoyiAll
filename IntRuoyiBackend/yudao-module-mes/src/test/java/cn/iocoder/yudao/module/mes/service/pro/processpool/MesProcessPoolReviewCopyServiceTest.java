package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyFieldDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyFieldMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolReviewCopyRuleMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyFieldMappingDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateFromRulesReqDTO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.dto.MesProcessPoolReviewCopyGenerateReqDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({MesProcessPoolReviewCopyServiceImpl.class, MesProcessPoolFifoAllocationService.class})
class MesProcessPoolReviewCopyServiceTest extends BaseDbUnitTest {

    @Resource
    private MesProcessPoolReviewCopyService reviewCopyService;

    @Resource
    private MesProProcessPoolEventMapper eventMapper;

    @Resource
    private MesProcessPoolReviewCopyMapper reviewCopyMapper;

    @Resource
    private MesProcessPoolReviewCopyFieldMapper reviewCopyFieldMapper;

    @Resource
    private MesProcessPoolFifoAllocationLineMapper allocationLineMapper;

    @Resource
    private MesProcessPoolReviewCopyRuleMapper reviewCopyRuleMapper;

    @Resource
    private MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;

    @Test
    void shouldPreserveRawEventPayloadWhenGenerateReviewCopy() {
        Long eventId = insertEvent("{\"pressure\":50,\"outputQuantity\":10}");
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId,
                List.of(mapping("pressure", "压力", "20", "40", false, null)));

        Long reviewCopyId = reviewCopyService.generateAndSubmitReviewCopy(req);

        MesProProcessPoolEventDO event = eventMapper.selectById(eventId);
        assertEquals("{\"pressure\":50,\"outputQuantity\":10}", event.getRawPayload());
        MesProcessPoolReviewCopyDO reviewCopy = reviewCopyMapper.selectById(reviewCopyId);
        assertEquals(event.getRawPayload(), reviewCopy.getRawPayloadSnapshot());
        assertEquals(event.getFeedbackSourceType(), reviewCopy.getFeedbackSourceType());
        assertEquals(event.getFeedbackSourceId(), reviewCopy.getFeedbackSourceId());
        assertEquals(event.getRecordbookSourceType(), reviewCopy.getRecordbookSourceType());
        assertEquals(event.getRecordbookSourceId(), reviewCopy.getRecordbookSourceId());
    }

    @Test
    void shouldClampValueToMaxWhenRawValueExceedsMax() {
        MesProcessPoolReviewCopyFieldDO field = generateSingleField("50");

        assertEquals("50", field.getRawValue());
        assertEquals("40", field.getCorrectedValue());
        assertEquals(MesProcessPoolReviewCopyFieldDO.RULE_CLAMP_TO_MAX, field.getRuleType());
    }

    @Test
    void shouldClampValueToMinWhenRawValueBelowMin() {
        MesProcessPoolReviewCopyFieldDO field = generateSingleField("10");

        assertEquals("10", field.getRawValue());
        assertEquals("20", field.getCorrectedValue());
        assertEquals(MesProcessPoolReviewCopyFieldDO.RULE_CLAMP_TO_MIN, field.getRuleType());
    }

    @Test
    void shouldKeepValueWhenRawValueWithinRange() {
        MesProcessPoolReviewCopyFieldDO field = generateSingleField("30");

        assertEquals("30", field.getRawValue());
        assertEquals("30", field.getCorrectedValue());
        assertEquals(MesProcessPoolReviewCopyFieldDO.RULE_UNCHANGED_IN_RANGE, field.getRuleType());
    }

    @Test
    void shouldBlockWhenLimitMetadataMissing() {
        Long eventId = insertEvent("{\"pressure\":50}");
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId,
                List.of(mapping("pressure", "压力", null, "40", false, null)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopy(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_LIMIT_METADATA_REQUIRED.getCode(), ex.getCode());
        assertEquals(0L, reviewCopyMapper.selectCount());
    }

    @Test
    void shouldBlockWhenFieldMappingMissing() {
        Long eventId = insertEvent("{\"pressure\":50}");
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId, List.of());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopy(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED.getCode(), ex.getCode());
        assertEquals(0L, reviewCopyMapper.selectCount());
    }

    @Test
    void shouldBlockWhenRawPayloadMissing() {
        Long eventId = insertEvent("");
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId,
                List.of(mapping("pressure", "压力", "20", "40", false, null)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopy(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED.getCode(), ex.getCode());
        assertEquals(0L, reviewCopyMapper.selectCount());
    }

    @Test
    void shouldRequireReviewerSignatureWhenSubmitReviewCopy() {
        Long eventId = insertEvent("{\"pressure\":50}");
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId,
                List.of(mapping("pressure", "压力", "20", "40", false, null)));
        req.setReviewerSignatureId(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopy(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_REQUIRED.getCode(),
                ex.getCode());
        assertEquals(0L, reviewCopyMapper.selectCount());
    }

    @Test
    void shouldRejectReviewerSignatureWhenSignerIsNotReviewer() {
        Long eventId = insertEvent("{\"pressure\":50}");
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId,
                List.of(mapping("pressure", "压力", "20", "40", false, null)));
        req.setReviewerSignatureUserId(randomLongId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopy(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_REVIEWER_SIGNATURE_MISMATCH.getCode(),
                ex.getCode());
        assertEquals(0L, reviewCopyMapper.selectCount());
    }

    @Test
    void shouldRejectDuplicateReviewerSignature() {
        Long eventId = insertEvent("{\"pressure\":50}");
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId,
                List.of(mapping("pressure", "压力", "20", "40", false, null)));
        Long signatureId = req.getReviewerSignatureId();
        Long reviewCopyId = reviewCopyService.generateAndSubmitReviewCopy(req);
        assertNotNull(reviewCopyId);
        Long anotherEventId = insertEvent("{\"pressure\":30}");
        MesProcessPoolReviewCopyGenerateReqDTO duplicateReq = validReq(anotherEventId,
                List.of(mapping("pressure", "压力", "20", "40", false, null)));
        duplicateReq.setReviewerSignatureId(signatureId);
        duplicateReq.setReviewerUserId(req.getReviewerUserId());
        duplicateReq.setReviewerSignatureUserId(req.getReviewerUserId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopy(duplicateReq));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_SIGNATURE_DUPLICATE.getCode(), ex.getCode());
        assertEquals(1L, reviewCopyMapper.selectCount());
    }

    @Test
    void shouldRejectReviewCorrectionForAllocatedQuantityFragment() {
        Long eventId = insertEvent("{\"outputQuantity\":50}");
        Long fragmentId = randomLongId();
        allocationLineMapper.insert(MesProcessPoolFifoAllocationLineDO.builder()
                .allocationBatchNo("ALLOC-F5")
                .processPoolId(10L)
                .sourceEventId(eventId)
                .sourceQuantityFragmentId(fragmentId)
                .sourceRouteProcessId(30L)
                .sourceProcessId(40L)
                .sourceFragmentQuantity(new BigDecimal("50"))
                .targetWorkOrderId(900L)
                .targetWorkOrderCode("WO-F5")
                .targetRouteProcessId(31L)
                .targetProcessId(41L)
                .allocatedQuantity(new BigDecimal("10"))
                .allocationStatus(MesProcessPoolFifoAllocationLineDO.STATUS_ALLOCATED)
                .build());
        MesProcessPoolReviewCopyGenerateReqDTO req = validReq(eventId,
                List.of(mapping("outputQuantity", "输出数量", "20", "40", true, fragmentId)));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopy(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_ALLOCATED_FRAGMENT_LOCKED.getCode(), ex.getCode());
        assertEquals(0L, reviewCopyMapper.selectCount());
    }

    @Test
    void shouldGenerateReviewCopyFromFormalRulesAndResolveOutputFragment() {
        Long eventId = insertEvent("{\"outputQuantity\":50}");
        MesProProcessPoolQuantityFragmentDO fragment = insertOutputFragment(eventId);
        insertRule("outputQuantity", "输出数量", "20", "40", true, "OUTPUT");
        Long reviewerUserId = randomLongId();

        Long reviewCopyId = reviewCopyService.generateAndSubmitReviewCopyFromRules(
                MesProcessPoolReviewCopyGenerateFromRulesReqDTO.builder()
                        .eventId(eventId)
                        .reviewerUserId(reviewerUserId)
                        .reviewerSignatureId(randomLongId())
                        .reviewerSignatureUserId(reviewerUserId)
                        .reviewerSignatureSnapshot("{\"signature\":\"review-from-rules\"}")
                        .build());

        List<MesProcessPoolReviewCopyFieldDO> fields =
                reviewCopyFieldMapper.selectListByReviewCopyId(reviewCopyId);
        assertEquals(1, fields.size());
        assertEquals(fragment.getId(), fields.get(0).getSourceQuantityFragmentId());
        assertEquals("50", fields.get(0).getRawValue());
        assertEquals("40", fields.get(0).getCorrectedValue());
        assertEquals(MesProcessPoolReviewCopyFieldDO.RULE_CLAMP_TO_MAX, fields.get(0).getRuleType());
    }

    @Test
    void shouldBlockAutomaticReviewCopyWhenFormalRulesAreMissing() {
        Long eventId = insertEvent("{\"pressure\":50}");
        Long reviewerUserId = randomLongId();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> reviewCopyService.generateAndSubmitReviewCopyFromRules(
                        MesProcessPoolReviewCopyGenerateFromRulesReqDTO.builder()
                                .eventId(eventId)
                                .reviewerUserId(reviewerUserId)
                                .reviewerSignatureId(randomLongId())
                                .reviewerSignatureUserId(reviewerUserId)
                                .reviewerSignatureSnapshot("{\"signature\":\"review-from-rules\"}")
                                .build()));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_REVIEW_COPY_FIELD_MAPPING_REQUIRED.getCode(), ex.getCode());
        assertEquals(0L, reviewCopyMapper.selectCount());
    }

    private void insertRule(String fieldCode, String fieldName, String lowerLimit, String upperLimit,
                            boolean affectsAllocation, String sourceQuantityType) {
        reviewCopyRuleMapper.insert(MesProcessPoolReviewCopyRuleDO.builder()
                .processId(50L)
                .deviceId(70L)
                .templateType("PRODUCTION_SIMPLE")
                .fieldCode(fieldCode)
                .fieldName(fieldName)
                .lowerLimit(new BigDecimal(lowerLimit))
                .upperLimit(new BigDecimal(upperLimit))
                .valueType("DECIMAL")
                .affectsAllocation(affectsAllocation)
                .allocationField(affectsAllocation ? MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY.name() : null)
                .sourceQuantityType(sourceQuantityType)
                .templateFieldMetadataJson("{\"fieldCode\":\"" + fieldCode + "\"}")
                .enabled(Boolean.TRUE)
                .build());
    }

    private MesProProcessPoolQuantityFragmentDO insertOutputFragment(Long eventId) {
        MesProProcessPoolQuantityFragmentDO fragment = MesProProcessPoolQuantityFragmentDO.builder()
                .poolId(10L)
                .eventId(eventId)
                .workOrderId(20L)
                .routeId(30L)
                .routeProcessId(40L)
                .processId(50L)
                .sourceQuantityType("OUTPUT")
                .qualityStatus("OUTPUT")
                .totalQuantity(new BigDecimal("50"))
                .allocatedQuantity(BigDecimal.ZERO)
                .availableQuantity(new BigDecimal("50"))
                .allocationStatus(MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE)
                .locked(Boolean.FALSE)
                .rawPayload("{\"sourceQuantityType\":\"OUTPUT\"}")
                .build();
        quantityFragmentMapper.insert(fragment);
        assertNotNull(fragment.getId());
        return fragment;
    }

    private MesProcessPoolReviewCopyFieldDO generateSingleField(String rawPressure) {
        Long eventId = insertEvent("{\"pressure\":" + rawPressure + "}");
        Long reviewCopyId = reviewCopyService.generateAndSubmitReviewCopy(validReq(eventId,
                List.of(mapping("pressure", "压力", "20", "40", false, null))));
        List<MesProcessPoolReviewCopyFieldDO> fields = reviewCopyFieldMapper.selectListByReviewCopyId(reviewCopyId);
        assertEquals(1, fields.size());
        return fields.get(0);
    }

    private Long insertEvent(String rawPayload) {
        Long actualEmployeeId = randomLongId();
        MesProProcessPoolEventDO event = MesProProcessPoolEventDO.builder()
                .poolId(10L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .workOrderId(20L)
                .routeId(30L)
                .routeProcessId(40L)
                .processId(50L)
                .actualEmployeeId(actualEmployeeId)
                .deviceAccountId(60L)
                .deviceId(70L)
                .workstationId(80L)
                .templateType("PRODUCTION_SIMPLE")
                .feedbackSourceType("MES_PRO_FEEDBACK")
                .feedbackSourceId(90L)
                .recordbookSourceType("MES_RECORDBOOK_ENTRY")
                .recordbookSourceId(100L)
                .rawPayload(rawPayload)
                .serverSubmitTime(java.time.LocalDateTime.now())
                .signatureId(randomLongId())
                .signatureUserId(actualEmployeeId)
                .signatureSnapshot("{\"signature\":\"submit\"}")
                .build();
        eventMapper.insert(event);
        assertNotNull(event.getId());
        return event.getId();
    }

    private MesProcessPoolReviewCopyGenerateReqDTO validReq(Long eventId,
                                                            List<MesProcessPoolReviewCopyFieldMappingDTO> mappings) {
        Long reviewerUserId = randomLongId();
        return MesProcessPoolReviewCopyGenerateReqDTO.builder()
                .eventId(eventId)
                .reviewerUserId(reviewerUserId)
                .reviewerSignatureId(randomLongId())
                .reviewerSignatureUserId(reviewerUserId)
                .reviewerSignatureSnapshot("{\"signature\":\"review\"}")
                .fieldMappings(mappings)
                .build();
    }

    private MesProcessPoolReviewCopyFieldMappingDTO mapping(String fieldCode, String fieldName, String lowerLimit,
                                                            String upperLimit, boolean affectsAllocation,
                                                            Long sourceQuantityFragmentId) {
        return MesProcessPoolReviewCopyFieldMappingDTO.builder()
                .fieldCode(fieldCode)
                .fieldName(fieldName)
                .lowerLimit(lowerLimit == null ? null : new BigDecimal(lowerLimit))
                .upperLimit(upperLimit == null ? null : new BigDecimal(upperLimit))
                .valueType("DECIMAL")
                .affectsAllocation(affectsAllocation)
                .allocationField(affectsAllocation ? MesProcessPoolFragmentOriginalField.OUTPUT_QUANTITY : null)
                .sourceQuantityFragmentId(sourceQuantityFragmentId)
                .templateFieldMetadataJson("{\"fieldCode\":\"" + fieldCode + "\"}")
                .build();
    }
}
