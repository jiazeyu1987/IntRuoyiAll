package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolAllocatableQuantityFragment;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationResult;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoAllocationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFifoTargetWorkOrder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_MODE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_QUANTITY_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ROOT_EVENT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_CONFIRMATION_PRODUCTION_LEADER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ABNORMAL_ORDER_FORBIDDEN;

@Service
@Validated
public class MesTeamLeaderReportConfirmationServiceImpl implements MesTeamLeaderReportConfirmationService {

    private final MesTeamLeaderScopeService scopeService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;
    private final MesTeamLeaderFifoAllocationService fifoAllocationService;
    private final MesProcessPoolFifoAllocationService processPoolFifoAllocationService;
    private final MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    private final MesTeamLeaderOrderProcessCompletionService orderProcessCompletionService;
    private final MesWorkOrderAbnormalStateService abnormalStateService;

    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;

    public MesTeamLeaderReportConfirmationServiceImpl(MesTeamLeaderScopeService scopeService,
                                                      MesProProcessPoolEventMapper eventMapper,
                                                      MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                       MesProWorkOrderMapper workOrderMapper,
                                                       MesProcessPoolSubmissionReviewMapper reviewMapper,
                                                       MesProcessPoolReportAllocationMapper allocationMapper,
                                                       MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper,
                                                       MesProProcessPoolPqcRecordMapper pqcRecordMapper,
                                                       MesTeamLeaderFifoAllocationService fifoAllocationService,
                                                       MesProcessPoolFifoAllocationService processPoolFifoAllocationService,
                                                       MesPqcInspectionTaskMapper pqcTaskMapper,
                                                       MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper,
                                                       MesTeamLeaderOrderProcessTargetService orderProcessTargetService,
                                                       MesTeamLeaderOrderProcessCompletionService orderProcessCompletionService,
                                                       MesWorkOrderAbnormalStateService abnormalStateService) {
        this.scopeService = scopeService;
        this.eventMapper = eventMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.reviewMapper = reviewMapper;
        this.allocationMapper = allocationMapper;
        this.quantityFragmentMapper = quantityFragmentMapper;
        this.pqcRecordMapper = pqcRecordMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcPieceDetailMapper = pqcPieceDetailMapper;
        this.fifoAllocationService = fifoAllocationService;
        this.processPoolFifoAllocationService = processPoolFifoAllocationService;
        this.orderProcessTargetService = orderProcessTargetService;
        this.orderProcessCompletionService = orderProcessCompletionService;
        this.abnormalStateService = abnormalStateService;
    }

    @Override
    public MesTeamLeaderReportAllocationPreview previewFifoAllocation(MesTeamLeaderReportAllocationPreviewReqBO reqBO) {
        if (reqBO == null || reqBO.getEventId() == null || reqBO.getLeaderUserId() == null
                || StrUtil.isBlank(reqBO.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "reportAllocationPreview");
        }
        MesProProcessPoolEventDO event = requireEvent(reqBO.getEventId());
        scopeService.assertCanAccessEmployee(reqBO.getLeaderUserId(), reqBO.getLeaderType(),
                event.getActualEmployeeId());
        return fifoAllocationService.previewFifoAllocation(MesTeamLeaderFifoAllocationReqBO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .eventId(event.getId())
                .routeProcessId(event.getRouteProcessId())
                .processId(event.getProcessId())
                .confirmQuantity(extractSubmittedQuantity(event))
                .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmSubmission(MesTeamLeaderReportConfirmationReqBO reqBO) {
        validateConfirmReq(reqBO);
        MesProProcessPoolEventDO event = requireEventForUpdate(reqBO.getEventId());
        scopeService.assertCanAccessEmployee(reqBO.getLeaderUserId(), reqBO.getLeaderType(),
                event.getActualEmployeeId());
        MesProcessPoolSubmissionReviewDO existingReview =
                reviewMapper.selectLatestByEventIdForUpdate(event.getId());
        if (existingReview != null) {
            throw exception(PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS,
                    event.getId(), existingReview.getReviewStatus());
        }
        if (!allocationMapper.selectListByEventIdForUpdate(event.getId()).isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE, event.getId());
        }
        BigDecimal submittedQuantity = extractSubmittedQuantity(event);
        validatePqcQualityGate(event, submittedQuantity);
        List<MesTeamLeaderReportAllocationLineReqBO> allocationRequests = resolveAllocationRequests(reqBO, event,
                submittedQuantity);
        List<PreparedAllocationLine> preparedLines = validateAndPrepareLines(reqBO, event, allocationRequests,
                submittedQuantity);
        persistFifoConsumptionIfRequired(reqBO, event, preparedLines, submittedQuantity);
        ReviewSignaturePayload reviewSignature = resolveReviewSignaturePayload(reqBO, event);

        MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                .eventId(event.getId())
                .leaderUserId(reqBO.getLeaderUserId())
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark(reqBO.getReviewRemark())
                .reviewedAt(LocalDateTime.now())
                .reviewSignatureId(reviewSignature == null ? null : reviewSignature.reviewSignatureId())
                .reviewSignatureUserId(reviewSignature == null ? null : reviewSignature.reviewSignatureUserId())
                .reviewSignatureSnapshotJson(
                        reviewSignature == null ? null : reviewSignature.reviewSignatureSnapshotJson())
                .build();
        reviewMapper.insert(review);

        LocalDateTime confirmedAt = LocalDateTime.now();
        List<MesProcessPoolReportAllocationDO> rows = preparedLines.stream()
                .map(line -> MesProcessPoolReportAllocationDO.builder()
                        .eventId(event.getId())
                        .reviewId(review.getId())
                        .leaderUserId(reqBO.getLeaderUserId())
                        .activeOrderId(line.activeOrder().getId())
                        .workOrderId(line.activeOrder().getWorkOrderId())
                        .routeProcessId(event.getRouteProcessId())
                        .processId(event.getProcessId())
                        .allocatedQuantity(line.quantity())
                        .allocationMode(reqBO.getAllocationMode())
                        .confirmedAt(confirmedAt)
                        .build())
                .toList();
        if (!Boolean.TRUE.equals(allocationMapper.insertBatch(rows))) {
            throw new IllegalStateException("Failed to insert MES team leader report allocation lines");
        }
        orderProcessCompletionService.applyConfirmedAllocations(event, rows);
        return review.getId();
    }

    private List<MesTeamLeaderReportAllocationLineReqBO> resolveAllocationRequests(
            MesTeamLeaderReportConfirmationReqBO reqBO, MesProProcessPoolEventDO event,
            BigDecimal submittedQuantity) {
        if (CollUtil.isNotEmpty(reqBO.getAllocations())) {
            return reqBO.getAllocations();
        }
        if (MesProcessPoolReportAllocationDO.MODE_FIFO.equals(reqBO.getAllocationMode())) {
            return fifoAllocationService.previewFifoAllocation(MesTeamLeaderFifoAllocationReqBO.builder()
                            .leaderUserId(reqBO.getLeaderUserId())
                            .eventId(event.getId())
                            .routeProcessId(event.getRouteProcessId())
                            .processId(event.getProcessId())
                            .confirmQuantity(submittedQuantity)
                            .build())
                    .getLines()
                    .stream()
                    .map(line -> MesTeamLeaderReportAllocationLineReqBO.builder()
                            .activeOrderId(line.getActiveOrderId())
                            .allocatedQuantity(line.getAllocatedQuantity())
                            .build())
                    .toList();
        }
        throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, event.getId());
    }

    private List<PreparedAllocationLine> validateAndPrepareLines(MesTeamLeaderReportConfirmationReqBO reqBO,
                                                                 MesProProcessPoolEventDO event,
                                                                 List<MesTeamLeaderReportAllocationLineReqBO> lines,
                                                                 BigDecimal submittedQuantity) {
        if (CollUtil.isEmpty(lines)) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, event.getId());
        }
        Map<Long, MesProcessPoolActiveOrderDO> activeOrderMap = activeOrderMapper
                .selectActiveListByLeader(reqBO.getLeaderUserId())
                .stream()
                .filter(activeOrder -> "ACTIVE".equals(activeOrder.getActiveStatus()))
                .collect(Collectors.toMap(MesProcessPoolActiveOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        List<RequestedActiveLine> requestedActiveLines = new ArrayList<>();
        for (MesTeamLeaderReportAllocationLineReqBO line : lines) {
            if (line == null || line.getActiveOrderId() == null || line.getAllocatedQuantity() == null
                    || line.getAllocatedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
            }
            MesProcessPoolActiveOrderDO activeOrder = activeOrderMap.get(line.getActiveOrderId());
            if (activeOrder == null) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, line.getActiveOrderId());
            }
            requestedActiveLines.add(new RequestedActiveLine(line, activeOrder));
        }
        List<Long> workOrderIds = requestedActiveLines.stream()
                .map(RequestedActiveLine::activeOrder)
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .distinct()
                .toList();
        Set<Long> openAbnormalWorkOrderIds = abnormalStateService.findOpenWorkOrderIds(workOrderIds);
        if (!openAbnormalWorkOrderIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ABNORMAL_ORDER_FORBIDDEN,
                    openAbnormalWorkOrderIds.iterator().next());
        }
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderMapper.selectListByIdsForUpdate(workOrderIds)
                .stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        Map<Long, BigDecimal> existingAllocated = allocationMapper
                .selectListByWorkOrderIdsAndProcessForUpdate(workOrderIds, event.getRouteProcessId(),
                        event.getProcessId())
                .stream()
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getWorkOrderId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolReportAllocationDO::getAllocatedQuantity,
                                BigDecimal::add)));

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, BigDecimal> requestedByWorkOrder = new LinkedHashMap<>();
        List<PreparedAllocationLine> prepared = new ArrayList<>();
        for (RequestedActiveLine requestedActiveLine : requestedActiveLines) {
            MesTeamLeaderReportAllocationLineReqBO line = requestedActiveLine.line();
            MesProcessPoolActiveOrderDO activeOrder = requestedActiveLine.activeOrder();
            MesProWorkOrderDO workOrder = workOrderMap.get(activeOrder.getWorkOrderId());
            MesTeamLeaderOrderProcessTarget target = requireTarget(activeOrder, workOrder, event.getRouteProcessId(),
                    event.getProcessId());
            BigDecimal alreadyAllocated = existingAllocated.getOrDefault(activeOrder.getWorkOrderId(),
                    BigDecimal.ZERO);
            BigDecimal remaining = target.plannedQuantity().subtract(alreadyAllocated);
            BigDecimal requestedForOrder = requestedByWorkOrder.merge(activeOrder.getWorkOrderId(),
                    line.getAllocatedQuantity(), BigDecimal::add);
            if (requestedForOrder.compareTo(remaining) > 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH, activeOrder.getWorkOrderId());
            }
            total = total.add(line.getAllocatedQuantity());
            prepared.add(new PreparedAllocationLine(activeOrder, workOrder, target, alreadyAllocated,
                    line.getAllocatedQuantity()));
        }
        if (total.compareTo(submittedQuantity) != 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH,
                    submittedQuantity.stripTrailingZeros().toPlainString());
        }
        return prepared;
    }

    private MesTeamLeaderOrderProcessTarget requireTarget(MesProcessPoolActiveOrderDO activeOrder,
                                                          MesProWorkOrderDO workOrder,
                                                          Long routeProcessId,
                                                          Long processId) {
        if (workOrder == null || workOrder.getQuantity() == null
                || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, activeOrder.getWorkOrderId());
        }
        return orderProcessTargetService.requireTarget(activeOrder, routeProcessId, processId);
    }

    private void persistFifoConsumptionIfRequired(MesTeamLeaderReportConfirmationReqBO reqBO,
                                                  MesProProcessPoolEventDO event,
                                                  List<PreparedAllocationLine> preparedLines,
                                                  BigDecimal submittedQuantity) {
        if (!MesProcessPoolReportAllocationDO.MODE_FIFO.equals(reqBO.getAllocationMode())) {
            return;
        }
        List<MesProcessPoolAllocatableQuantityFragment> fragments = quantityFragmentMapper
                .selectListByProductionSubmitEventIdForUpdate(event.getId())
                .stream()
                .map(this::toAllocatableFragment)
                .toList();
        List<MesProcessPoolFifoTargetWorkOrder> targetWorkOrders = preparedLines.stream()
                .map(line -> MesProcessPoolFifoTargetWorkOrder.builder()
                        .workOrderId(line.activeOrder().getWorkOrderId())
                        .workOrderCode(line.workOrder().getCode())
                        .plannedStartTime(line.workOrder().getPlannedStartTime())
                        .targetRouteProcessId(line.target().routeProcessId())
                        .targetProcessId(line.target().processId())
                        .requiredQuantity(line.quantity())
                        .alreadyAllocatedQuantity(line.alreadyAllocatedQuantity())
                        .build())
                .toList();
        MesProcessPoolFifoAllocationResult result = Objects.requireNonNull(
                processPoolFifoAllocationService.allocate(MesProcessPoolFifoAllocationCommand.of(
                        "PROCESS_POOL_EVENT_" + event.getId(), fragments, targetWorkOrders)),
                "processPoolFifoAllocationResult");
        BigDecimal fifoAllocatedQuantity = Objects.requireNonNull(result.getTotalAllocatedQuantity(),
                "processPoolFifoAllocatedQuantity");
        if (fifoAllocatedQuantity.compareTo(submittedQuantity) != 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_QUANTITY_MISMATCH,
                    event.getId(),
                    quantityText(submittedQuantity),
                    quantityText(submittedQuantity),
                    quantityText(fifoAllocatedQuantity),
                    quantityText(submittedQuantity.subtract(fifoAllocatedQuantity)));
        }
    }

    private MesProcessPoolAllocatableQuantityFragment toAllocatableFragment(
            MesProProcessPoolQuantityFragmentDO fragment) {
        return MesProcessPoolAllocatableQuantityFragment.builder()
                .processPoolId(fragment.getPoolId())
                .sourceEventId(fragment.getEventId())
                .sourceQuantityFragmentId(fragment.getId())
                .sourceRouteProcessId(fragment.getRouteProcessId())
                .sourceProcessId(fragment.getProcessId())
                .quantity(fragment.getTotalQuantity())
                .build();
    }

    private MesProProcessPoolEventDO requireEvent(Long eventId) {
        MesProProcessPoolEventDO event = eventMapper.selectById(eventId);
        return validateEvent(eventId, event);
    }

    private MesProProcessPoolEventDO requireEventForUpdate(Long eventId) {
        MesProProcessPoolEventDO event = eventMapper.selectByIdForUpdate(eventId);
        return validateEvent(eventId, event);
    }

    private MesProProcessPoolEventDO validateEvent(Long eventId, MesProProcessPoolEventDO event) {
        if (event == null) {
            throw exception(PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS, eventId);
        }
        if (event.getRouteProcessId() == null || event.getProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "event.process");
        }
        return event;
    }

    private BigDecimal extractSubmittedQuantity(MesProProcessPoolEventDO event) {
        if (StrUtil.isBlank(event.getRawPayload())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
        }
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(event.getRawPayload());
            JsonNode quantityNode = root.get("outputQuantity");
            if (quantityNode == null || !quantityNode.isNumber()) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
            }
            BigDecimal quantity = quantityNode.decimalValue();
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
            }
            return quantity;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
        }
    }

    private void validateConfirmReq(MesTeamLeaderReportConfirmationReqBO reqBO) {
        if (reqBO == null || reqBO.getEventId() == null || reqBO.getLeaderUserId() == null
                || StrUtil.isBlank(reqBO.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "reportConfirmation");
        }
        if (!MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION.equals(reqBO.getLeaderType())) {
            throw exception(PRO_PROCESS_POOL_REPORT_CONFIRMATION_PRODUCTION_LEADER_REQUIRED,
                    reqBO.getEventId(), reqBO.getLeaderType());
        }
        if (!MesProcessPoolReportAllocationDO.MODE_FIFO.equals(reqBO.getAllocationMode())
                && !MesProcessPoolReportAllocationDO.MODE_MANUAL.equals(reqBO.getAllocationMode())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_MODE_INVALID, reqBO.getAllocationMode());
        }
    }

    private void validatePqcQualityGate(MesProProcessPoolEventDO event, BigDecimal confirmQuantity) {
        if (!Objects.equals(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT, event.getEventType())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ROOT_EVENT_REQUIRED, event.getId());
        }
        List<MesProProcessPoolPqcRecordDO> pqcRecords =
                pqcRecordMapper.selectListByProductionSubmitEventId(event.getId());
        if (pqcRecords == null || pqcRecords.size() != 1) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        MesProProcessPoolPqcRecordDO pqcRecord = pqcRecords.get(0);
        if (!Objects.equals(pqcRecord.getProductionSubmitEventId(), event.getId())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        if (!MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS.equals(pqcRecord.getInspectionResult())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE,
                    event.getId(), pqcRecord.getInspectionResult());
        }
        validatePqcQuantityCoverage(event, pqcRecord, confirmQuantity);
    }

    private void validatePqcQuantityCoverage(MesProProcessPoolEventDO event,
                                             MesProProcessPoolPqcRecordDO pqcRecord,
                                             BigDecimal confirmQuantity) {
        MesProProcessPoolEventDO pqcEvent = eventMapper.selectByIdForUpdate(pqcRecord.getEventId());
        if (pqcEvent == null
                || !Objects.equals(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION, pqcEvent.getEventType())
                || !Objects.equals(event.getWorkOrderId(), pqcEvent.getWorkOrderId())
                || !Objects.equals(event.getRouteProcessId(), pqcEvent.getRouteProcessId())
                || !Objects.equals(event.getProcessId(), pqcEvent.getProcessId())
                || pqcEvent.getFeedbackSourceId() == null) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        MesPqcInspectionTaskDO task = pqcTaskMapper.selectByIdForUpdate(pqcEvent.getFeedbackSourceId());
        if (task == null
                || !Objects.equals(task.getId(), pqcEvent.getFeedbackSourceId())
                || !Objects.equals(event.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(event.getRouteProcessId(), task.getRouteProcessId())
                || !Objects.equals(event.getProcessId(), task.getProcessId())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        BigDecimal qualifiedQuantity = calculateQualifiedQuantity(task,
                pqcPieceDetailMapper.selectListByTaskId(task.getId()));
        BigDecimal consumedQuantity = BigDecimal.ZERO;
        BigDecimal allocatableQuantity = qualifiedQuantity.subtract(consumedQuantity);
        if (confirmQuantity == null || confirmQuantity.compareTo(BigDecimal.ZERO) <= 0
                || allocatableQuantity.compareTo(confirmQuantity) < 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_QUANTITY_MISMATCH,
                    event.getId(),
                    quantityText(confirmQuantity),
                    quantityText(qualifiedQuantity),
                    quantityText(consumedQuantity),
                    quantityText(allocatableQuantity));
        }
    }

    private BigDecimal calculateQualifiedQuantity(MesPqcInspectionTaskDO task,
                                                  List<MesPqcInspectionPieceDetailDO> pieceDetails) {
        Integer actualInspectionQuantity = task.getActualInspectionQuantity();
        if (actualInspectionQuantity == null || actualInspectionQuantity <= 0 || CollUtil.isEmpty(pieceDetails)) {
            return BigDecimal.ZERO;
        }
        Map<Integer, List<MesPqcInspectionPieceDetailDO>> detailsBySample = pieceDetails.stream()
                .filter(detail -> detail != null && detail.getSampleNo() != null)
                .collect(Collectors.groupingBy(MesPqcInspectionPieceDetailDO::getSampleNo,
                        LinkedHashMap::new, Collectors.toList()));
        long qualifiedCount = 0;
        for (int sampleNo = 1; sampleNo <= actualInspectionQuantity; sampleNo += 1) {
            List<MesPqcInspectionPieceDetailDO> sampleDetails = detailsBySample.get(sampleNo);
            if (CollUtil.isNotEmpty(sampleDetails)
                    && sampleDetails.stream().allMatch(this::isQualifiedPqcPiece)) {
                qualifiedCount += 1;
            }
        }
        return BigDecimal.valueOf(qualifiedCount);
    }

    private boolean isQualifiedPqcPiece(MesPqcInspectionPieceDetailDO detail) {
        return detail != null
                && MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS.equals(detail.getJudgement());
    }

    private String quantityText(BigDecimal quantity) {
        return quantity == null ? "null" : quantity.stripTrailingZeros().toPlainString();
    }

    private ReviewSignaturePayload resolveReviewSignaturePayload(MesTeamLeaderReportConfirmationReqBO reqBO,
                                                                 MesProProcessPoolEventDO event) {
        if (StrUtil.isBlank(reqBO.getSignaturePassword())) {
            return null;
        }
        Long signatureId = signatureService.recordTeamLeaderReviewSignature(
                reqBO.getLeaderUserId(),
                reqBO.getSignaturePassword(),
                buildReviewSignatureComment(reqBO, event));
        return new ReviewSignaturePayload(
                signatureId,
                reqBO.getLeaderUserId(),
                buildReviewSignatureSnapshot(reqBO, event, signatureId));
    }

    private String buildReviewSignatureComment(MesTeamLeaderReportConfirmationReqBO reqBO,
                                               MesProProcessPoolEventDO event) {
        return "组长报工确认复核:" + reqBO.getLeaderType() + ":" + event.getId() + ":APPROVED";
    }

    private String buildReviewSignatureSnapshot(MesTeamLeaderReportConfirmationReqBO reqBO,
                                                MesProProcessPoolEventDO event,
                                                Long signatureId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signatureId", signatureId);
        payload.put("actorId", reqBO.getLeaderUserId());
        payload.put("actionType", MesProBatchRecordExecutionSignatureService.ACTION_TEAM_LEADER_REVIEW);
        payload.put("processPoolEventId", event.getId());
        payload.put("eventType", event.getEventType());
        payload.put("leaderType", reqBO.getLeaderType());
        payload.put("reviewStatus", MesProcessPoolSubmissionReviewDO.STATUS_APPROVED);
        return JsonUtils.toJsonString(payload);
    }


    private record PreparedAllocationLine(MesProcessPoolActiveOrderDO activeOrder,
                                          MesProWorkOrderDO workOrder,
                                          MesTeamLeaderOrderProcessTarget target,
                                          BigDecimal alreadyAllocatedQuantity,
                                          BigDecimal quantity) {
    }

    private record RequestedActiveLine(MesTeamLeaderReportAllocationLineReqBO line,
                                       MesProcessPoolActiveOrderDO activeOrder) {
    }
    private record ReviewSignaturePayload(Long reviewSignatureId,
                                          Long reviewSignatureUserId,
                                          String reviewSignatureSnapshotJson) {
    }

}
