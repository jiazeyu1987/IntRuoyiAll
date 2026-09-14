package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT;

@Service
public class MesReportAllocationQuantityFragmentService {

    private final MesProProcessPoolQuantityFragmentMapper fragmentMapper;
    private final MesProcessPoolFifoAllocationLineMapper lineMapper;
    private final MesProWorkOrderMapper workOrderMapper;

    public MesReportAllocationQuantityFragmentService(
            MesProProcessPoolQuantityFragmentMapper fragmentMapper,
            MesProcessPoolFifoAllocationLineMapper lineMapper,
            MesProWorkOrderMapper workOrderMapper) {
        this.fragmentMapper = fragmentMapper;
        this.lineMapper = lineMapper;
        this.workOrderMapper = workOrderMapper;
    }

    public void rebuildForVersion(MesProProcessPoolEventDO event, Integer version,
                                  Collection<MesProcessPoolReportAllocationDO> currentAllocations) {
        validateContext(event, version, currentAllocations);
        List<MesProcessPoolReportAllocationDO> allocations = List.copyOf(currentAllocations);
        List<MesProProcessPoolQuantityFragmentDO> fragments = fragmentMapper
                .selectOutputListByProductionSubmitEventIdForUpdate(event.getId());
        validateFragments(event, fragments);
        List<MesProcessPoolFifoAllocationLineDO> previousLines = lineMapper
                .selectListBySourceEventIdForUpdate(event.getId());
        Map<Long, MesProWorkOrderDO> workOrders = loadWorkOrders(allocations);
        BigDecimal required = allocations.stream().map(MesProcessPoolReportAllocationDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = fragments.stream().map(MesProProcessPoolQuantityFragmentDO::getTotalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (required.compareTo(available) > 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH,
                    available.stripTrailingZeros().toPlainString());
        }

        List<MesProcessPoolFifoAllocationLineDO> rebuilt = buildLines(event, version, allocations, fragments,
                workOrders);
        List<Long> previousIds = previousLines.stream().map(MesProcessPoolFifoAllocationLineDO::getId)
                .filter(Objects::nonNull).toList();
        if (!previousIds.isEmpty() && lineMapper.supersedeCurrentRows(previousIds, version) != previousIds.size()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_VERSION_CONFLICT,
                    event.getId(), version - 1, version);
        }
        if (!rebuilt.isEmpty() && !Boolean.TRUE.equals(lineMapper.insertBatch(rebuilt))) {
            throw new IllegalStateException("Failed to insert report allocation quantity fragment lines");
        }
        persistFragmentBalances(fragments, rebuilt);
    }

    private List<MesProcessPoolFifoAllocationLineDO> buildLines(
            MesProProcessPoolEventDO event, Integer version,
            List<MesProcessPoolReportAllocationDO> allocations,
            List<MesProProcessPoolQuantityFragmentDO> fragments,
            Map<Long, MesProWorkOrderDO> workOrders) {
        List<MesProcessPoolFifoAllocationLineDO> result = new ArrayList<>();
        int fragmentIndex = 0;
        BigDecimal fragmentRemaining = fragments.get(0).getTotalQuantity();
        for (MesProcessPoolReportAllocationDO allocation : allocations) {
            BigDecimal targetRemaining = allocation.getAllocatedQuantity();
            while (targetRemaining.compareTo(BigDecimal.ZERO) > 0) {
                while (fragmentRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    fragmentIndex++;
                    fragmentRemaining = fragments.get(fragmentIndex).getTotalQuantity();
                }
                MesProProcessPoolQuantityFragmentDO fragment = fragments.get(fragmentIndex);
                BigDecimal quantity = targetRemaining.min(fragmentRemaining);
                MesProWorkOrderDO workOrder = workOrders.get(allocation.getWorkOrderId());
                result.add(MesProcessPoolFifoAllocationLineDO.builder()
                        .allocationBatchNo("REPORT:" + event.getId() + ":V" + version)
                        .processPoolId(event.getPoolId()).sourceEventId(event.getId())
                        .reportAllocationVersion(version).sourceQuantityFragmentId(fragment.getId())
                        .sourceRouteProcessId(fragment.getRouteProcessId()).sourceProcessId(fragment.getProcessId())
                        .sourceFragmentQuantity(fragment.getTotalQuantity())
                        .targetWorkOrderId(allocation.getWorkOrderId()).targetWorkOrderCode(workOrder.getCode())
                        .targetRouteProcessId(allocation.getRouteProcessId()).targetProcessId(allocation.getProcessId())
                        .allocatedQuantity(quantity).allocationStatus(MesProcessPoolFifoAllocationLineDO.STATUS_ALLOCATED)
                        .lifecycleStatus(MesProcessPoolFifoAllocationLineMapper.LIFECYCLE_CURRENT).build());
                targetRemaining = targetRemaining.subtract(quantity);
                fragmentRemaining = fragmentRemaining.subtract(quantity);
            }
        }
        return result;
    }

    private void persistFragmentBalances(List<MesProProcessPoolQuantityFragmentDO> fragments,
                                         List<MesProcessPoolFifoAllocationLineDO> lines) {
        Map<Long, BigDecimal> allocatedByFragment = lines.stream().collect(Collectors.groupingBy(
                MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId, LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, MesProcessPoolFifoAllocationLineDO::getAllocatedQuantity,
                        BigDecimal::add)));
        for (MesProProcessPoolQuantityFragmentDO fragment : fragments) {
            BigDecimal allocated = allocatedByFragment.getOrDefault(fragment.getId(), BigDecimal.ZERO);
            BigDecimal remaining = fragment.getTotalQuantity().subtract(allocated);
            fragment.setAllocatedQuantity(allocated).setAvailableQuantity(remaining)
                    .setAllocationStatus(remaining.compareTo(BigDecimal.ZERO) > 0
                            ? MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE
                            : MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_ALLOCATED);
            if (fragmentMapper.updateById(fragment) != 1) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "reportAllocationFragment.update");
            }
        }
    }

    private Map<Long, MesProWorkOrderDO> loadWorkOrders(
            List<MesProcessPoolReportAllocationDO> allocations) {
        List<Long> ids = allocations.stream().map(MesProcessPoolReportAllocationDO::getWorkOrderId)
                .distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, MesProWorkOrderDO> result = workOrderMapper.selectListByIds(ids).stream().collect(Collectors.toMap(
                MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        for (Long id : ids) {
            MesProWorkOrderDO workOrder = result.get(id);
            if (workOrder == null || StrUtil.isBlank(workOrder.getCode())) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "reportAllocationWorkOrder.code");
            }
        }
        return result;
    }

    private void validateContext(MesProProcessPoolEventDO event, Integer version,
                                 Collection<MesProcessPoolReportAllocationDO> allocations) {
        if (event == null || event.getId() == null || event.getPoolId() == null
                || event.getRouteProcessId() == null || event.getProcessId() == null
                || version == null || version <= 0 || allocations == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "reportAllocationFragments");
        }
        for (MesProcessPoolReportAllocationDO allocation : allocations) {
            if (allocation == null || allocation.getWorkOrderId() == null || allocation.getRouteProcessId() == null
                    || allocation.getProcessId() == null || allocation.getAllocatedQuantity() == null
                    || allocation.getAllocatedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, event.getId());
            }
        }
    }

    private void validateFragments(MesProProcessPoolEventDO event,
                                   List<MesProProcessPoolQuantityFragmentDO> fragments) {
        if (fragments == null || fragments.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionSubmitQuantityFragments");
        }
        for (MesProProcessPoolQuantityFragmentDO fragment : fragments) {
            if (fragment == null || fragment.getId() == null || fragment.getTotalQuantity() == null
                    || fragment.getTotalQuantity().compareTo(BigDecimal.ZERO) <= 0
                    || !Objects.equals(fragment.getProductionSubmitEventId(), event.getId())
                    || !MesProProcessPoolQuantityFragmentDO.SOURCE_QUANTITY_TYPE_OUTPUT.equals(
                            fragment.getSourceQuantityType())
                    || fragment.getRouteProcessId() == null || fragment.getProcessId() == null) {
                throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "productionSubmitQuantityFragment");
            }
        }
    }
}
