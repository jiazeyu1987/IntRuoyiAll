package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProcessPoolFifoAllocationLineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_ALLOCATED_FRAGMENT_LOCKED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_WORK_ORDER_PLANNED_START_TIME_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_FIFO_WORK_ORDER_PLANNED_START_TIME_REQUIRED;

@Service
@RequiredArgsConstructor
public class MesProcessPoolFifoAllocationService {

    private final MesProcessPoolFifoAllocationLineMapper allocationLineMapper;

    @Transactional(rollbackFor = Exception.class)
    public synchronized MesProcessPoolFifoAllocationResult allocate(MesProcessPoolFifoAllocationCommand command) {
        validateCommand(command);
        List<MesProcessPoolAllocatableQuantityFragment> fragments = command.getFragments();
        List<MesProcessPoolFifoTargetWorkOrder> targets = sortTargetWorkOrders(command.getTargetWorkOrders());
        List<Long> sourceQuantityFragmentIds = fragments.stream()
                .map(MesProcessPoolAllocatableQuantityFragment::getSourceQuantityFragmentId)
                .distinct()
                .toList();
        Map<Long, BigDecimal> allocatedByFragmentId = allocationLineMapper
                .selectListBySourceQuantityFragmentIdsForUpdate(sourceQuantityFragmentIds)
                .stream()
                .collect(Collectors.groupingBy(MesProcessPoolFifoAllocationLineDO::getSourceQuantityFragmentId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolFifoAllocationLineDO::getAllocatedQuantity,
                                BigDecimal::add)));

        List<MesProcessPoolFifoAllocationLineDO> lines = new ArrayList<>();
        int fragmentIndex = 0;
        BigDecimal currentFragmentRemaining = BigDecimal.ZERO;
        for (MesProcessPoolFifoTargetWorkOrder target : targets) {
            BigDecimal targetRemaining = target.getRequiredQuantity().subtract(target.getAlreadyAllocatedQuantity());
            while (targetRemaining.compareTo(BigDecimal.ZERO) > 0 && fragmentIndex < fragments.size()) {
                MesProcessPoolAllocatableQuantityFragment fragment = fragments.get(fragmentIndex);
                if (currentFragmentRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    currentFragmentRemaining = remainingFragmentQuantity(fragment, allocatedByFragmentId);
                }
                if (currentFragmentRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    fragmentIndex++;
                    continue;
                }

                BigDecimal allocatedQuantity = currentFragmentRemaining.min(targetRemaining);
                lines.add(buildLine(command.getAllocationBatchNo(), fragment, target, allocatedQuantity));
                currentFragmentRemaining = currentFragmentRemaining.subtract(allocatedQuantity);
                targetRemaining = targetRemaining.subtract(allocatedQuantity);
                if (currentFragmentRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    fragmentIndex++;
                }
            }
        }

        if (!lines.isEmpty() && !Boolean.TRUE.equals(allocationLineMapper.insertBatch(lines))) {
            throw new IllegalStateException("Failed to insert MES process pool FIFO allocation lines");
        }
        BigDecimal totalAllocatedQuantity = lines.stream()
                .map(MesProcessPoolFifoAllocationLineDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return MesProcessPoolFifoAllocationResult.of(List.copyOf(lines), totalAllocatedQuantity);
    }

    public void validateOriginalFieldMutationAllowed(Long sourceQuantityFragmentId,
                                                     MesProcessPoolFragmentOriginalField field) {
        requirePresent(sourceQuantityFragmentId, "sourceQuantityFragmentId");
        Objects.requireNonNull(field, "field");
        Long allocationCount = allocationLineMapper.selectCountBySourceQuantityFragmentId(sourceQuantityFragmentId);
        if (field.isAllocationAffecting() && allocationCount > 0) {
            throw exception(PRO_PROCESS_POOL_FIFO_ALLOCATED_FRAGMENT_LOCKED, sourceQuantityFragmentId);
        }
    }

    private void validateCommand(MesProcessPoolFifoAllocationCommand command) {
        Objects.requireNonNull(command, "command");
        requirePresent(command.getAllocationBatchNo(), "allocationBatchNo");
        List<MesProcessPoolAllocatableQuantityFragment> fragments =
                Objects.requireNonNull(command.getFragments(), "fragments");
        List<MesProcessPoolFifoTargetWorkOrder> targets =
                Objects.requireNonNull(command.getTargetWorkOrders(), "targetWorkOrders");
        for (MesProcessPoolAllocatableQuantityFragment fragment : fragments) {
            validateFragment(fragment);
        }
        for (MesProcessPoolFifoTargetWorkOrder target : targets) {
            validateTargetWorkOrder(target);
        }
    }

    private static void validateFragment(MesProcessPoolAllocatableQuantityFragment fragment) {
        Objects.requireNonNull(fragment, "fragment");
        requirePresent(fragment.getProcessPoolId(), "processPoolId");
        requirePresent(fragment.getSourceEventId(), "sourceEventId");
        requirePresent(fragment.getSourceQuantityFragmentId(), "sourceQuantityFragmentId");
        requirePresent(fragment.getSourceRouteProcessId(), "sourceRouteProcessId");
        requirePresent(fragment.getSourceProcessId(), "sourceProcessId");
        requirePositive(fragment.getQuantity(), "fragment.quantity");
    }

    private static void validateTargetWorkOrder(MesProcessPoolFifoTargetWorkOrder target) {
        Objects.requireNonNull(target, "targetWorkOrder");
        requirePresent(target.getWorkOrderId(), "workOrderId");
        requirePresent(target.getWorkOrderCode(), "workOrderCode");
        requirePresent(target.getTargetRouteProcessId(), "targetRouteProcessId");
        requirePresent(target.getTargetProcessId(), "targetProcessId");
        requirePositive(target.getRequiredQuantity(), "requiredQuantity");
        requirePresent(target.getAlreadyAllocatedQuantity(), "alreadyAllocatedQuantity");
        if (target.getAlreadyAllocatedQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw exception(PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID, "alreadyAllocatedQuantity");
        }
        if (target.getPlannedStartTime() == null) {
            throw exception(PRO_PROCESS_POOL_FIFO_WORK_ORDER_PLANNED_START_TIME_REQUIRED,
                    target.getWorkOrderCode());
        }
    }

    private static List<MesProcessPoolFifoTargetWorkOrder> sortTargetWorkOrders(
            List<MesProcessPoolFifoTargetWorkOrder> targets) {
        Map<LocalDateTime, List<String>> workOrderCodesByPlannedStartTime = targets.stream()
                .collect(Collectors.groupingBy(MesProcessPoolFifoTargetWorkOrder::getPlannedStartTime,
                        LinkedHashMap::new,
                        Collectors.mapping(MesProcessPoolFifoTargetWorkOrder::getWorkOrderCode,
                                Collectors.toList())));
        workOrderCodesByPlannedStartTime.forEach((plannedStartTime, workOrderCodes) -> {
            if (workOrderCodes.size() > 1) {
                throw exception(PRO_PROCESS_POOL_FIFO_WORK_ORDER_PLANNED_START_TIME_DUPLICATE,
                        plannedStartTime + "=" + String.join(",", workOrderCodes));
            }
        });
        return targets.stream()
                .sorted(Comparator.comparing(MesProcessPoolFifoTargetWorkOrder::getPlannedStartTime))
                .toList();
    }

    private static BigDecimal remainingFragmentQuantity(MesProcessPoolAllocatableQuantityFragment fragment,
                                                        Map<Long, BigDecimal> allocatedByFragmentId) {
        BigDecimal alreadyAllocated = allocatedByFragmentId.getOrDefault(
                fragment.getSourceQuantityFragmentId(), BigDecimal.ZERO);
        return fragment.getQuantity().subtract(alreadyAllocated);
    }

    private static MesProcessPoolFifoAllocationLineDO buildLine(String allocationBatchNo,
                                                               MesProcessPoolAllocatableQuantityFragment fragment,
                                                               MesProcessPoolFifoTargetWorkOrder target,
                                                               BigDecimal allocatedQuantity) {
        return MesProcessPoolFifoAllocationLineDO.builder()
                .allocationBatchNo(allocationBatchNo)
                .processPoolId(fragment.getProcessPoolId())
                .sourceEventId(fragment.getSourceEventId())
                .sourceQuantityFragmentId(fragment.getSourceQuantityFragmentId())
                .sourceRouteProcessId(fragment.getSourceRouteProcessId())
                .sourceProcessId(fragment.getSourceProcessId())
                .sourceFragmentQuantity(fragment.getQuantity())
                .targetWorkOrderId(target.getWorkOrderId())
                .targetWorkOrderCode(target.getWorkOrderCode())
                .targetRouteProcessId(target.getTargetRouteProcessId())
                .targetProcessId(target.getTargetProcessId())
                .allocatedQuantity(allocatedQuantity)
                .allocationStatus(MesProcessPoolFifoAllocationLineDO.STATUS_ALLOCATED)
                .build();
    }

    private static void requirePresent(Object value, String fieldName) {
        if (value == null) {
            throw exception(PRO_PROCESS_POOL_FIFO_CONTEXT_REQUIRED, fieldName);
        }
    }

    private static void requirePositive(BigDecimal quantity, String fieldName) {
        requirePresent(quantity, fieldName);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_FIFO_QUANTITY_INVALID, fieldName);
        }
    }

}
