package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public MesTeamLeaderFifoAllocationService(MesProcessPoolActiveOrderMapper activeOrderMapper,
                                              MesProWorkOrderMapper workOrderMapper,
                                              MesProcessPoolReportAllocationMapper allocationMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.workOrderMapper = workOrderMapper;
        this.allocationMapper = allocationMapper;
    }

    public MesTeamLeaderReportAllocationPreview previewFifoAllocation(MesTeamLeaderFifoAllocationReqBO reqBO) {
        validateReq(reqBO);
        List<MesProcessPoolActiveOrderDO> activeOrders = sortedActiveOrders(reqBO.getLeaderUserId());
        if (activeOrders.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_ACTIVE_ORDER_REQUIRED, reqBO.getLeaderUserId());
        }
        List<Long> workOrderIds = activeOrders.stream()
                .map(MesProcessPoolActiveOrderDO::getWorkOrderId)
                .distinct()
                .toList();
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderMapper.selectListByIdsForUpdate(workOrderIds).stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        Map<Long, BigDecimal> allocatedByWorkOrder = allocatedByWorkOrder(workOrderIds,
                reqBO.getRouteProcessId(), reqBO.getProcessId());

        BigDecimal unallocated = reqBO.getConfirmQuantity();
        List<MesTeamLeaderReportAllocationPreviewLine> lines = new ArrayList<>();
        for (MesProcessPoolActiveOrderDO activeOrder : activeOrders) {
            if (unallocated.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            MesProWorkOrderDO workOrder = workOrderMap.get(activeOrder.getWorkOrderId());
            BigDecimal remaining = remainingQuantity(activeOrder, workOrder, allocatedByWorkOrder);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal allocated = remaining.min(unallocated);
            lines.add(MesTeamLeaderReportAllocationPreviewLine.builder()
                    .activeOrderId(activeOrder.getId())
                    .workOrderId(activeOrder.getWorkOrderId())
                    .workOrderCode(workOrder.getCode())
                    .allocatedQuantity(allocated)
                    .remainingQuantityBeforeAllocation(remaining)
                    .build());
            unallocated = unallocated.subtract(allocated);
        }
        if (unallocated.compareTo(BigDecimal.ZERO) > 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_REMAINING_NOT_ENOUGH, reqBO.getEventId());
        }
        BigDecimal total = lines.stream()
                .map(MesTeamLeaderReportAllocationPreviewLine::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return MesTeamLeaderReportAllocationPreview.builder()
                .totalAllocatedQuantity(total)
                .lines(List.copyOf(lines))
                .build();
    }

    List<MesProcessPoolActiveOrderDO> sortedActiveOrders(Long leaderUserId) {
        return activeOrderMapper.selectActiveListByLeader(leaderUserId).stream()
                .filter(activeOrder -> Objects.equals(activeOrder.getActiveStatus(), "ACTIVE"))
                .sorted(Comparator
                        .comparing(MesProcessPoolActiveOrderDO::getJoinedAt,
                                Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(MesProcessPoolActiveOrderDO::getWorkOrderId,
                                Comparator.nullsLast(Long::compareTo))
                        .thenComparing(MesProcessPoolActiveOrderDO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    Map<Long, BigDecimal> allocatedByWorkOrder(List<Long> workOrderIds, Long routeProcessId, Long processId) {
        return allocationMapper.selectListByWorkOrderIdsAndProcessForUpdate(workOrderIds, routeProcessId, processId)
                .stream()
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getWorkOrderId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolReportAllocationDO::getAllocatedQuantity,
                                BigDecimal::add)));
    }

    private BigDecimal remainingQuantity(MesProcessPoolActiveOrderDO activeOrder, MesProWorkOrderDO workOrder,
                                         Map<Long, BigDecimal> allocatedByWorkOrder) {
        if (workOrder == null || workOrder.getQuantity() == null
                || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, activeOrder.getWorkOrderId());
        }
        BigDecimal alreadyAllocated = allocatedByWorkOrder.getOrDefault(activeOrder.getWorkOrderId(), BigDecimal.ZERO);
        return workOrder.getQuantity().subtract(alreadyAllocated);
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
