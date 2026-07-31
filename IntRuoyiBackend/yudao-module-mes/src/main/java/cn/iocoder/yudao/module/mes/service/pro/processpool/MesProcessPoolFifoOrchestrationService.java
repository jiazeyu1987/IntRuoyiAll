package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolQuantityFragmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID;

@Service
@RequiredArgsConstructor
public class MesProcessPoolFifoOrchestrationService {

    private final MesProProcessPoolQuantityFragmentMapper quantityFragmentMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolFifoAllocationLineMapper allocationLineMapper;
    private final MesProcessPoolFifoAllocationService allocationService;

    @Transactional(rollbackFor = Exception.class)
    public MesProcessPoolFifoAllocationResult allocateAvailableOutput(
            MesProcessPoolFifoOrchestrationCommand command) {
        validateCommand(command);
        List<MesProProcessPoolQuantityFragmentDO> fragmentDos =
                quantityFragmentMapper.selectAvailableOutputListForUpdate(command.getSourceProcessId());
        List<MesProWorkOrderDO> workOrders = loadTargetWorkOrders(command.getTargetWorkOrderIds());
        Map<Long, BigDecimal> alreadyAllocatedByWorkOrderId = loadExistingTargetAllocations(
                command.getTargetWorkOrderIds(), command.getTargetRouteProcessId());
        List<MesProcessPoolAllocatableQuantityFragment> fragments = fragmentDos.stream()
                .map(MesProcessPoolFifoOrchestrationService::toAllocatableFragment)
                .toList();
        List<MesProcessPoolFifoTargetWorkOrder> targets = workOrders.stream()
                .map(workOrder -> toTargetWorkOrder(command, workOrder,
                        alreadyAllocatedByWorkOrderId.getOrDefault(workOrder.getId(), BigDecimal.ZERO)))
                .toList();

        MesProcessPoolFifoAllocationResult result = allocationService.allocate(MesProcessPoolFifoAllocationCommand.of(
                command.getAllocationBatchNo(), fragments, targets));
        updateFragmentProgress(fragmentDos, result.getLines());
        return result;
    }

    private static void validateCommand(MesProcessPoolFifoOrchestrationCommand command) {
        Objects.requireNonNull(command, "command");
        requirePresent(command.getAllocationBatchNo(), "allocationBatchNo");
        requirePresent(command.getSourceProcessId(), "sourceProcessId");
        requirePresent(command.getTargetRouteProcessId(), "targetRouteProcessId");
        requirePresent(command.getTargetProcessId(), "targetProcessId");
        if (command.getTargetWorkOrderIds() == null || command.getTargetWorkOrderIds().isEmpty()) {
            throw exception(PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED, "targetWorkOrderIds");
        }
    }

    private List<MesProWorkOrderDO> loadTargetWorkOrders(List<Long> targetWorkOrderIds) {
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectListByIdsForUpdate(targetWorkOrderIds);
        Map<Long, MesProWorkOrderDO> workOrderById = workOrders.stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<Long> missingIds = targetWorkOrderIds.stream()
                .filter(id -> !workOrderById.containsKey(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED, "targetWorkOrderIds=" + missingIds);
        }
        List<MesProWorkOrderDO> ordered = new ArrayList<>(targetWorkOrderIds.size());
        for (Long targetWorkOrderId : targetWorkOrderIds) {
            ordered.add(workOrderById.get(targetWorkOrderId));
        }
        return ordered;
    }

    private Map<Long, BigDecimal> loadExistingTargetAllocations(List<Long> targetWorkOrderIds,
                                                                Long targetRouteProcessId) {
        return allocationLineMapper.selectListByTargetWorkOrderIdsAndRouteProcessIdForUpdate(
                        targetWorkOrderIds, targetRouteProcessId)
                .stream()
                .collect(Collectors.groupingBy(MesProcessPoolFifoAllocationLineDO::getTargetWorkOrderId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolFifoAllocationLineDO::getAllocatedQuantity,
                                BigDecimal::add)));
    }

    private static MesProcessPoolAllocatableQuantityFragment toAllocatableFragment(
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

    private static MesProcessPoolFifoTargetWorkOrder toTargetWorkOrder(
            MesProcessPoolFifoOrchestrationCommand command,
            MesProWorkOrderDO workOrder,
            BigDecimal alreadyAllocatedQuantity) {
        BigDecimal producedQuantity = nullToZero(workOrder.getQuantityProduced());
        BigDecimal requiredQuantity = requireQuantity(workOrder.getQuantity(), "workOrder.quantity")
                .subtract(producedQuantity);
        if (requiredQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID, "workOrder.quantityProduced=" + workOrder.getCode());
        }
        return MesProcessPoolFifoTargetWorkOrder.builder()
                .workOrderId(workOrder.getId())
                .workOrderCode(workOrder.getCode())
                .plannedStartTime(workOrder.getPlannedStartTime())
                .targetRouteProcessId(command.getTargetRouteProcessId())
                .targetProcessId(command.getTargetProcessId())
                .requiredQuantity(requiredQuantity)
                .alreadyAllocatedQuantity(alreadyAllocatedQuantity)
                .build();
    }

    private void updateFragmentProgress(Collection<MesProProcessPoolQuantityFragmentDO> fragments,
                                        List<MesProcessPoolFifoAllocationLineDO> lines) {
        if (lines.isEmpty()) {
            return;
        }
        Map<Long, MesProProcessPoolQuantityFragmentDO> fragmentById = fragments.stream()
                .collect(Collectors.toMap(MesProProcessPoolQuantityFragmentDO::getId, Function.identity()));
        Map<Long, BigDecimal> allocatedDeltaByFragmentId = lines.stream()
                .collect(Collectors.groupingBy(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolFifoAllocationLineDO::getAllocatedQuantity,
                                BigDecimal::add)));
        allocatedDeltaByFragmentId.forEach((fragmentId, allocatedDelta) -> updateSingleFragmentProgress(
                fragmentById.get(fragmentId), allocatedDelta));
    }

    private void updateSingleFragmentProgress(MesProProcessPoolQuantityFragmentDO fragment,
                                              BigDecimal allocatedDelta) {
        if (fragment == null) {
            throw exception(PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED, "sourceQuantityFragmentId");
        }
        BigDecimal newAllocatedQuantity = nullToZero(fragment.getAllocatedQuantity()).add(allocatedDelta);
        BigDecimal newAvailableQuantity = requireQuantity(fragment.getTotalQuantity(), "fragment.totalQuantity")
                .subtract(newAllocatedQuantity);
        if (newAvailableQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID, "fragment.availableQuantity");
        }
        String allocationStatus = newAvailableQuantity.compareTo(BigDecimal.ZERO) > 0
                ? MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_AVAILABLE
                : MesProProcessPoolQuantityFragmentDO.ALLOCATION_STATUS_ALLOCATED;
        int updated = quantityFragmentMapper.updateAllocationProgress(fragment.getId(), allocatedDelta,
                newAvailableQuantity, allocationStatus);
        if (updated != 1) {
            throw exception(PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED,
                    "sourceQuantityFragmentId=" + fragment.getId());
        }
    }

    private static BigDecimal requireQuantity(BigDecimal value, String fieldName) {
        requirePresent(value, fieldName);
        return value;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void requirePresent(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED, fieldName);
        }
    }
}
