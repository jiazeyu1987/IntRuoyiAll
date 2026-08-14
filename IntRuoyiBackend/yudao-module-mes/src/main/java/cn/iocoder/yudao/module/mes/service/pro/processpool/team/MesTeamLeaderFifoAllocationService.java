package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH;

@Service
public class MesTeamLeaderFifoAllocationService {

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesTeamLeaderOrderProcessTargetService orderProcessTargetService;
    private final MesWorkOrderAbnormalStateService abnormalStateService;

    public MesTeamLeaderFifoAllocationService(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                              MesProWorkOrderMapper workOrderMapper,
                                              MesProcessPoolReportAllocationMapper allocationMapper,
                                              MesTeamLeaderOrderProcessTargetService orderProcessTargetService,
                                              MesWorkOrderAbnormalStateService abnormalStateService) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.allocationMapper = allocationMapper;
        this.orderProcessTargetService = orderProcessTargetService;
        this.abnormalStateService = abnormalStateService;
    }

    public MesTeamLeaderReportAllocationPreview previewFifoAllocation(MesTeamLeaderFifoAllocationReqBO reqBO) {
        validateReq(reqBO);
        List<MesProcessPoolActiveOrderDO> activeOrders = sortedActiveOrders(reqBO.getLeaderUserId());
        if (reqBO.getExcludedActiveOrderIds() != null && !reqBO.getExcludedActiveOrderIds().isEmpty()) {
            activeOrders = activeOrders.stream()
                    .filter(order -> !reqBO.getExcludedActiveOrderIds().contains(order.getId()))
                    .toList();
        }
        if (activeOrders.isEmpty()) {
            return MesTeamLeaderReportAllocationPreview.builder()
                    .poolQuantity(reqBO.getConfirmQuantity())
                    .totalAllocatedQuantity(BigDecimal.ZERO)
                    .unallocatedQuantity(reqBO.getConfirmQuantity())
                    .lines(List.of())
                    .build();
        }
        List<Long> workOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .distinct()
                .toList();
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderMapper.selectListByIdsForUpdate(workOrderIds).stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        List<Long> activeOrderIds = activeOrders.stream().map(MesProcessPoolActiveOrderDO::getId).toList();
        Map<Long, BigDecimal> allocatedByActiveOrder = allocatedByActiveOrder(activeOrderIds, reqBO.getProcessId(),
                reqBO.getExcludedEventId());

        BigDecimal unallocated = reqBO.getConfirmQuantity();
        List<MesTeamLeaderReportAllocationPreviewLine> lines = new ArrayList<>();
        for (MesProcessPoolActiveOrderDO activeOrder : activeOrders) {
            if (unallocated.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            MesProWorkOrderDO workOrder = workOrderMap.get(activeOrder.getWorkOrderId());
            Optional<MesTeamLeaderOrderProcessTarget> targetOptional = orderProcessTargetService
                    .findUniqueTargetForProcess(activeOrder, reqBO.getProcessId());
            if (targetOptional.isEmpty()) {
                continue;
            }
            MesTeamLeaderOrderProcessTarget target = targetOptional.get();
            BigDecimal remaining = remainingQuantity(activeOrder, workOrder, allocatedByActiveOrder, target);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocated = remaining.min(unallocated);
            lines.add(MesTeamLeaderReportAllocationPreviewLine.builder()
                    .activeOrderId(activeOrder.getId())
                    .workOrderId(activeOrder.getWorkOrderId())
                    .workOrderCode(workOrder.getCode())
                    .routeProcessId(target.routeProcessId())
                    .processId(target.processId())
                    .allocatedQuantity(allocated)
                    .remainingQuantityBeforeAllocation(remaining)
                    .build());
            unallocated = unallocated.subtract(allocated);
        }
        BigDecimal total = lines.stream()
                .map(MesTeamLeaderReportAllocationPreviewLine::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return MesTeamLeaderReportAllocationPreview.builder()
                .poolQuantity(reqBO.getConfirmQuantity())
                .totalAllocatedQuantity(total)
                .unallocatedQuantity(unallocated)
                .lines(List.copyOf(lines))
                .build();
    }

    List<MesProcessPoolActiveOrderDO> sortedActiveOrders(Long leaderUserId) {
        return activeOrderMapper.selectActiveListByLeader(leaderUserId)
                .stream()
                .filter(activeOrder -> Objects.equals(activeOrder.getActiveStatus(), "ACTIVE"))
                .toList();
    }

    Map<Long, BigDecimal> allocatedByActiveOrder(List<Long> activeOrderIds, Long processId, Long excludedEventId) {
        return allocationMapper.selectListByActiveOrderIdsAndProcessForUpdate(activeOrderIds, processId)
                .stream()
                .filter(allocation -> excludedEventId == null
                        || !Objects.equals(allocation.getEventId(), excludedEventId))
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getActiveOrderId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolReportAllocationDO::getAllocatedQuantity,
                                BigDecimal::add)));
    }

    private BigDecimal remainingQuantity(MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder,
                                         Map<Long, BigDecimal> allocatedByActiveOrder,
                                         MesTeamLeaderOrderProcessTarget target) {
        if (workOrder == null || workOrder.getQuantity() == null
                || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, activeOrder.getWorkOrderId());
        }
        BigDecimal alreadyAllocated = allocatedByActiveOrder.getOrDefault(activeOrder.getId(), BigDecimal.ZERO);
        return target.plannedQuantity().subtract(alreadyAllocated);
    }

    private void validateReq(MesTeamLeaderFifoAllocationReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getEventId() == null
                || reqBO.getRouteProcessId() == null || reqBO.getProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "fifoReportAllocation");
        }
        if (reqBO.getConfirmQuantity() == null || reqBO.getConfirmQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, reqBO.getEventId());
        }
    }
}
