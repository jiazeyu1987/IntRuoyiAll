package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_COMPLETION_TRACE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_TRACE_REQUIRED;

@Service
public class MesTeamLeaderTraceServiceImpl implements MesTeamLeaderTraceService {

    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesProcessPoolOrderProcessCompletionMapper completionMapper;
    private final MesProBatchRecordExecutionMapper executionMapper;
    private final MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper;

    public MesTeamLeaderTraceServiceImpl(MesProcessPoolReportAllocationMapper allocationMapper,
                                         MesProcessPoolOrderProcessCompletionMapper completionMapper,
                                         MesProBatchRecordExecutionMapper executionMapper,
                                         MesProBatchRecordExecutionFieldAuditItemMapper auditItemMapper) {
        this.allocationMapper = allocationMapper;
        this.completionMapper = completionMapper;
        this.executionMapper = executionMapper;
        this.auditItemMapper = auditItemMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public MesTeamLeaderAllocationTraceRespVO getAllocationTrace(Long eventId, Long workOrderId,
                                                                 Long routeProcessId, Long processId) {
        requireContext(eventId, workOrderId, routeProcessId, processId, "allocationTrace");
        List<MesProcessPoolReportAllocationDO> allocations =
                allocationMapper.selectListByTrace(eventId, workOrderId, routeProcessId, processId);
        if (allocations.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_TRACE_REQUIRED,
                    eventId, workOrderId, routeProcessId, processId);
        }
        BigDecimal total = allocations.stream()
                .map(MesProcessPoolReportAllocationDO::getAllocatedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MesTeamLeaderAllocationTraceRespVO()
                .setEventId(eventId)
                .setWorkOrderId(workOrderId)
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setTotalAllocatedQuantity(total)
                .setLines(allocations.stream()
                        .map(MesTeamLeaderTraceServiceImpl::toAllocationLine)
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MesTeamLeaderOrderProcessTraceRespVO getOrderProcessTrace(Long workOrderId,
                                                                     Long routeProcessId, Long processId) {
        MesProcessPoolOrderProcessCompletionDO completion =
                requireCompletion(workOrderId, routeProcessId, processId);
        return toOrderProcessTrace(completion);
    }

    @Override
    @Transactional(readOnly = true)
    public MesTeamLeaderBatchRecordTraceRespVO getBatchRecordTrace(Long workOrderId,
                                                                   Long routeProcessId, Long processId) {
        MesProcessPoolOrderProcessCompletionDO completion =
                requireCompletion(workOrderId, routeProcessId, processId);
        if (completion.getBackfillExecutionId() == null) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED, workOrderId, routeProcessId, processId);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(completion.getBackfillExecutionId());
        if (execution == null || execution.getFieldAuditLastBatchId() == null) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED, workOrderId, routeProcessId, processId);
        }
        List<MesProBatchRecordExecutionFieldAuditItemDO> items =
                auditItemMapper.selectListByBatchId(execution.getFieldAuditLastBatchId());
        if (items.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_BATCH_RECORD_TRACE_REQUIRED, workOrderId, routeProcessId, processId);
        }
        return new MesTeamLeaderBatchRecordTraceRespVO()
                .setWorkOrderId(workOrderId)
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setExecutionId(execution.getId())
                .setExecutionCode(execution.getExecutionCode())
                .setWorkOrderCode(execution.getWorkOrderCode())
                .setBatchRecordReportId(execution.getBatchRecordReportId())
                .setBatchRecordDefinitionId(execution.getBatchRecordDefinitionId())
                .setBatchRecordVersionId(execution.getBatchRecordVersionId())
                .setFieldAuditRevision(execution.getFieldAuditRevision())
                .setFieldAuditHeadHash(execution.getFieldAuditHeadHash())
                .setFieldAuditLastBatchId(execution.getFieldAuditLastBatchId())
                .setCellValuesJson(execution.getCellValuesJson())
                .setCells(items.stream()
                        .map(MesTeamLeaderTraceServiceImpl::toCell)
                        .toList());
    }

    private MesProcessPoolOrderProcessCompletionDO requireCompletion(Long workOrderId,
                                                                     Long routeProcessId, Long processId) {
        requireContext(workOrderId, routeProcessId, processId, "orderProcessTrace");
        MesProcessPoolOrderProcessCompletionDO completion =
                completionMapper.selectByWorkOrderAndProcess(workOrderId, routeProcessId, processId);
        if (completion == null) {
            throw exception(PRO_PROCESS_POOL_ORDER_PROCESS_COMPLETION_TRACE_REQUIRED,
                    workOrderId, routeProcessId, processId);
        }
        return completion;
    }

    private static void requireContext(Object first, Object second, Object third, String context) {
        if (first == null || second == null || third == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, context);
        }
    }

    private static void requireContext(Object first, Object second, Object third, Object fourth, String context) {
        if (first == null || second == null || third == null || fourth == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, context);
        }
    }

    private static MesTeamLeaderAllocationTraceRespVO.Line toAllocationLine(
            MesProcessPoolReportAllocationDO allocation) {
        return new MesTeamLeaderAllocationTraceRespVO.Line()
                .setAllocationId(allocation.getId())
                .setReviewId(allocation.getReviewId())
                .setLeaderUserId(allocation.getLeaderUserId())
                .setActiveOrderId(allocation.getActiveOrderId())
                .setWorkOrderId(allocation.getWorkOrderId())
                .setRouteProcessId(allocation.getRouteProcessId())
                .setProcessId(allocation.getProcessId())
                .setAllocatedQuantity(allocation.getAllocatedQuantity())
                .setAllocationMode(allocation.getAllocationMode())
                .setConfirmedAt(allocation.getConfirmedAt());
    }

    private static MesTeamLeaderOrderProcessTraceRespVO toOrderProcessTrace(
            MesProcessPoolOrderProcessCompletionDO completion) {
        return new MesTeamLeaderOrderProcessTraceRespVO()
                .setWorkOrderId(completion.getWorkOrderId())
                .setRouteProcessId(completion.getRouteProcessId())
                .setProcessId(completion.getProcessId())
                .setTargetQuantity(completion.getTargetQuantity())
                .setConfirmedQuantity(completion.getConfirmedQuantity())
                .setCompletionStatus(completion.getCompletionStatus())
                .setCompletedAt(completion.getCompletedAt())
                .setBackfillStatus(completion.getBackfillStatus())
                .setBackfillExecutionId(completion.getBackfillExecutionId())
                .setBackfillError(completion.getBackfillError())
                .setLastEventId(completion.getLastEventId())
                .setLastReviewId(completion.getLastReviewId());
    }

    private static MesTeamLeaderBatchRecordTraceRespVO.Cell toCell(
            MesProBatchRecordExecutionFieldAuditItemDO item) {
        return new MesTeamLeaderBatchRecordTraceRespVO.Cell()
                .setAuditItemId(item.getId())
                .setAuditBatchId(item.getAuditBatchId())
                .setExecutionId(item.getExecutionId())
                .setFieldAuditRevision(item.getFieldAuditRevision())
                .setFieldPath(item.getFieldPath())
                .setFieldKey(item.getFieldKey())
                .setRowIndex(item.getRowIndex())
                .setColumnIndex(item.getColumnIndex())
                .setValueType(item.getValueType())
                .setValueJson(item.getNewValueJson())
                .setValueDisplay(item.getNewValueDisplay());
    }
}
