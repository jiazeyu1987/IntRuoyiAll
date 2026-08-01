package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED;

@Service
public class MesTeamLeaderOrderProcessCompletionService {

    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesTeamLeaderBatchRecordBackfillService backfillService;

    public MesTeamLeaderOrderProcessCompletionService(MesProcessPoolReportAllocationMapper allocationMapper,
                                                      MesProWorkOrderMapper workOrderMapper,
                                                      MesProcessPoolOrderProcessCompletionMapper completionMapper,
                                                      MesTeamLeaderBatchRecordBackfillService backfillService) {
        this.allocationMapper = allocationMapper;
        this.workOrderMapper = workOrderMapper;
        this.completionMapper = completionMapper;
        this.backfillService = backfillService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyConfirmedAllocations(MesProProcessPoolEventDO event,
                                          Collection<MesProcessPoolReportAllocationDO> confirmedAllocations) {
        if (event == null || event.getRouteProcessId() == null || event.getProcessId() == null
                || confirmedAllocations == null || confirmedAllocations.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "orderProcessCompletion");
        }
        List<Long> workOrderIds = confirmedAllocations.stream()
                .map(MesProcessPoolReportAllocationDO::getWorkOrderId)
                .distinct()
                .toList();
        Map<Long, MesProWorkOrderDO> workOrderMap = workOrderMapper.selectListByIdsForUpdate(workOrderIds)
                .stream()
                .collect(Collectors.toMap(MesProWorkOrderDO::getId, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        Map<Long, MesProcessPoolReportAllocationDO> representativeAllocations = confirmedAllocations.stream()
                .collect(Collectors.toMap(MesProcessPoolReportAllocationDO::getWorkOrderId, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        Map<Long, BigDecimal> confirmedByWorkOrder = allocationMapper
                .selectListByWorkOrderIdsAndProcessForUpdate(workOrderIds, event.getRouteProcessId(),
                        event.getProcessId())
                .stream()
                .collect(Collectors.groupingBy(MesProcessPoolReportAllocationDO::getWorkOrderId,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                MesProcessPoolReportAllocationDO::getAllocatedQuantity, BigDecimal::add)));

        for (Long workOrderId : workOrderIds) {
            MesProWorkOrderDO workOrder = requireWorkOrder(workOrderId, workOrderMap);
            BigDecimal confirmedQuantity = confirmedByWorkOrder.getOrDefault(workOrderId, BigDecimal.ZERO);
            MesProcessPoolOrderProcessCompletionDO completion =
                    completionMapper.selectByWorkOrderAndProcessForUpdate(workOrderId, event.getRouteProcessId(),
                            event.getProcessId());
            if (completion == null) {
                completion = new MesProcessPoolOrderProcessCompletionDO();
            }
            completion.setWorkOrderId(workOrderId)
                    .setRouteProcessId(event.getRouteProcessId())
                    .setProcessId(event.getProcessId())
                    .setTargetQuantity(workOrder.getQuantity())
                    .setConfirmedQuantity(confirmedQuantity)
                    .setLastEventId(event.getId())
                    .setLastReviewId(representativeAllocations.get(workOrderId).getReviewId());
            if (confirmedQuantity.compareTo(workOrder.getQuantity()) >= 0) {
                if (isCompletedAndBackfilled(completion)) {
                    completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED);
                } else {
                    completeAndBackfill(event, representativeAllocations.get(workOrderId), workOrder, completion);
                }
            } else {
                completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_IN_PROGRESS)
                        .setCompletedAt(null)
                        .setBackfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_NOT_REQUIRED)
                        .setBackfillExecutionId(null)
                        .setBackfillError(null);
            }
            if (completion.getId() == null) {
                completionMapper.insert(completion);
            } else {
                completionMapper.updateById(completion);
            }
        }
    }

    private boolean isCompletedAndBackfilled(MesProcessPoolOrderProcessCompletionDO completion) {
        return completion.getId() != null
                && MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED.equals(completion.getCompletionStatus())
                && MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS.equals(completion.getBackfillStatus())
                && completion.getBackfillExecutionId() != null;
    }

    private void completeAndBackfill(MesProProcessPoolEventDO event, MesProcessPoolReportAllocationDO allocation,
                                     MesProWorkOrderDO workOrder,
                                     MesProcessPoolOrderProcessCompletionDO completion) {
        MesTeamLeaderBatchRecordBackfillResult backfill = backfillService.backfillCompletedProcess(
                new MesTeamLeaderBatchRecordBackfillCommand()
                        .setEvent(event)
                        .setAllocation(allocation)
                        .setWorkOrder(workOrder));
        completion.setCompletionStatus(MesProcessPoolOrderProcessCompletionDO.STATUS_COMPLETED)
                .setCompletedAt(LocalDateTime.now())
                .setBackfillStatus(MesProcessPoolOrderProcessCompletionDO.BACKFILL_STATUS_SUCCESS)
                .setBackfillExecutionId(backfill.getExecutionId())
                .setBackfillError(null);
    }

    private MesProWorkOrderDO requireWorkOrder(Long workOrderId, Map<Long, MesProWorkOrderDO> workOrderMap) {
        MesProWorkOrderDO workOrder = workOrderMap.get(workOrderId);
        if (workOrder == null || workOrder.getQuantity() == null || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUANTITY_REQUIRED, workOrderId);
        }
        return workOrder;
    }
}
