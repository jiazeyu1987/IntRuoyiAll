package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_MODE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REVISION_EVENT_NOT_EXISTS;

@Service
@Validated
public class MesTeamLeaderReportConfirmationServiceImpl implements MesTeamLeaderReportConfirmationService {

    private final MesTeamLeaderScopeService scopeService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolSubmissionReviewMapper reviewMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesTeamLeaderFifoAllocationService fifoAllocationService;
    private final MesTeamLeaderOrderProcessCompletionService orderProcessCompletionService;

    public MesTeamLeaderReportConfirmationServiceImpl(MesTeamLeaderScopeService scopeService,
                                                      MesProProcessPoolEventMapper eventMapper,
                                                      MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                      MesProWorkOrderMapper workOrderMapper,
                                                      MesProcessPoolSubmissionReviewMapper reviewMapper,
                                                      MesProcessPoolReportAllocationMapper allocationMapper,
                                                      MesTeamLeaderFifoAllocationService fifoAllocationService,
                                                      MesTeamLeaderOrderProcessCompletionService orderProcessCompletionService) {
        this.scopeService = scopeService;
        this.eventMapper = eventMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.reviewMapper = reviewMapper;
        this.allocationMapper = allocationMapper;
        this.fifoAllocationService = fifoAllocationService;
        this.orderProcessCompletionService = orderProcessCompletionService;
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
        if (!allocationMapper.selectListByEventId(event.getId()).isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE, event.getId());
        }

        BigDecimal submittedQuantity = extractSubmittedQuantity(event);
        List<MesTeamLeaderReportAllocationLineReqBO> allocationRequests = resolveAllocationRequests(reqBO, event,
                submittedQuantity);
        List<PreparedAllocationLine> preparedLines = validateAndPrepareLines(reqBO, event, allocationRequests,
                submittedQuantity);

        MesProcessPoolSubmissionReviewDO review = MesProcessPoolSubmissionReviewDO.builder()
                .eventId(event.getId())
                .leaderUserId(reqBO.getLeaderUserId())
                .reviewStatus(MesProcessPoolSubmissionReviewDO.STATUS_APPROVED)
                .reviewRemark(reqBO.getReviewRemark())
                .reviewedAt(LocalDateTime.now())
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
            BigDecimal remaining = remainingQuantity(activeOrder, workOrder, existingAllocated);
            BigDecimal requestedForOrder = requestedByWorkOrder.merge(activeOrder.getWorkOrderId(),
                    line.getAllocatedQuantity(), BigDecimal::add);
            if (requestedForOrder.compareTo(remaining) > 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH, activeOrder.getWorkOrderId());
            }
            total = total.add(line.getAllocatedQuantity());
            prepared.add(new PreparedAllocationLine(activeOrder, line.getAllocatedQuantity()));
        }
        if (total.compareTo(submittedQuantity) != 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH,
                    submittedQuantity.stripTrailingZeros().toPlainString());
        }
        return prepared;
    }

    private BigDecimal remainingQuantity(MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder,
                                         Map<Long, BigDecimal> existingAllocated) {
        if (workOrder == null || workOrder.getQuantity() == null
                || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, activeOrder.getWorkOrderId());
        }
        return workOrder.getQuantity()
                .subtract(existingAllocated.getOrDefault(activeOrder.getWorkOrderId(), BigDecimal.ZERO));
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
        if (!MesProcessPoolReportAllocationDO.MODE_FIFO.equals(reqBO.getAllocationMode())
                && !MesProcessPoolReportAllocationDO.MODE_MANUAL.equals(reqBO.getAllocationMode())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_MODE_INVALID, reqBO.getAllocationMode());
        }
    }

    private record PreparedAllocationLine(MesProcessPoolActiveOrderDO activeOrder, BigDecimal quantity) {
    }

    private record RequestedActiveLine(MesTeamLeaderReportAllocationLineReqBO line,
                                       MesProcessPoolActiveOrderDO activeOrder) {
    }
}
