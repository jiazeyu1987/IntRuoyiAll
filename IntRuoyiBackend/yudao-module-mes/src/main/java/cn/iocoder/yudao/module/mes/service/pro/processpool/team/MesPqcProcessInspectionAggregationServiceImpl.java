package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RECORD_REQUIRED;

@Service
@Validated
public class MesPqcProcessInspectionAggregationServiceImpl
        implements MesPqcProcessInspectionAggregationService {

    private static final String PQC_INSPECTION_TASK_SOURCE_TYPE = "MES_PQC_INSPECTION_TASK";
    private static final String PQC_TASK_STATUS_SUBMITTED = "SUBMITTED";

    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcInspectionPieceDetailMapper pieceDetailMapper;
    private final MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper;

    public MesPqcProcessInspectionAggregationServiceImpl(MesProProcessPoolPqcRecordMapper pqcRecordMapper,
                                                         MesProProcessPoolEventMapper eventMapper,
                                                         MesPqcInspectionTaskMapper pqcTaskMapper,
                                                         MesPqcInspectionPieceDetailMapper pieceDetailMapper,
                                                         MesPqcProcessInspectionAggregateDetailMapper aggregateDetailMapper) {
        this.pqcRecordMapper = pqcRecordMapper;
        this.eventMapper = eventMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pieceDetailMapper = pieceDetailMapper;
        this.aggregateDetailMapper = aggregateDetailMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void aggregateApprovedPqcSubmission(Long eventId, Long reviewId) {
        if (eventId == null || reviewId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcProcessInspectionAggregation");
        }
        MesProProcessPoolPqcRecordDO record = pqcRecordMapper.selectByEventId(eventId);
        if (record == null) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, eventId);
        }
        if (!MesProProcessPoolPqcRecordDO.PROCESS_INSPECTION_AGGREGATION_STATUS_PENDING.equals(
                record.getProcessInspectionAggregationStatus())) {
            throw exception(PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED,
                    eventId, record.getProcessInspectionReviewId());
        }
        Long tenantId = requireTenantId(record, eventId);
        MesProProcessPoolEventDO event = eventMapper.selectById(eventId);
        requireFormalPqcEvent(record, event);
        MesPqcInspectionTaskDO task = pqcTaskMapper.selectById(event.getFeedbackSourceId());
        requireFormalSubmittedTask(record, event, task);
        List<MesPqcInspectionPieceDetailDO> pieceDetails = pieceDetailMapper.selectListByTaskId(task.getId());
        if (CollUtil.isEmpty(pieceDetails)) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, eventId);
        }

        LocalDateTime aggregatedAt = LocalDateTime.now();
        int updated = pqcRecordMapper.updateProcessInspectionAggregatedIfPending(tenantId, eventId, reviewId,
                aggregatedAt);
        if (updated != 1) {
            throw exception(PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED, eventId, reviewId);
        }
        List<MesPqcProcessInspectionAggregateDetailDO> aggregateRows = pieceDetails.stream()
                .map(detail -> toAggregateDetail(record, task, detail, reviewId, aggregatedAt))
                .toList();
        if (!Boolean.TRUE.equals(aggregateDetailMapper.insertBatch(aggregateRows))) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, eventId);
        }
    }

    private static Long requireTenantId(MesProProcessPoolPqcRecordDO record, Long eventId) {
        if (record.getTenantId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "pqcProcessInspectionAggregation.tenantId.eventId=" + eventId);
        }
        return record.getTenantId();
    }

    private static void requireFormalPqcEvent(MesProProcessPoolPqcRecordDO record, MesProProcessPoolEventDO event) {
        if (event == null || !Objects.equals(record.getEventId(), event.getId())
                || !Objects.equals(record.getTenantId(), event.getTenantId())
                || !MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION.equals(event.getEventType())
                || !Objects.equals(record.getWorkOrderId(), event.getWorkOrderId())
                || !Objects.equals(record.getRouteId(), event.getRouteId())
                || !Objects.equals(record.getRouteProcessId(), event.getRouteProcessId())
                || !Objects.equals(record.getProcessId(), event.getProcessId())
                || !PQC_INSPECTION_TASK_SOURCE_TYPE.equals(event.getFeedbackSourceType())
                || !PQC_INSPECTION_TASK_SOURCE_TYPE.equals(event.getRecordbookSourceType())
                || event.getFeedbackSourceId() == null
                || !Objects.equals(event.getFeedbackSourceId(), event.getRecordbookSourceId())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "pqcProcessInspectionAggregation.eventId=" + record.getEventId());
        }
    }

    private static void requireFormalSubmittedTask(MesProProcessPoolPqcRecordDO record,
                                                   MesProProcessPoolEventDO event,
                                                   MesPqcInspectionTaskDO task) {
        if (task == null || !Objects.equals(task.getId(), event.getFeedbackSourceId())
                || !Objects.equals(record.getTenantId(), task.getTenantId())
                || !Objects.equals(record.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(record.getRouteId(), task.getRouteId())
                || !Objects.equals(record.getRouteProcessId(), task.getRouteProcessId())
                || !Objects.equals(record.getProcessId(), task.getProcessId())
                || task.getRegulationVersionId() == null
                || StrUtil.isBlank(task.getInspectionType())
                || task.getBusinessDate() == null
                || StrUtil.isBlank(task.getShiftCode())
                || task.getRoundNo() == null
                || task.getActualInspectionQuantity() == null
                || task.getActualInspectionQuantity() <= 0
                || !PQC_TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, record.getEventId());
        }
    }

    private static MesPqcProcessInspectionAggregateDetailDO toAggregateDetail(MesProProcessPoolPqcRecordDO record,
                                                                              MesPqcInspectionTaskDO task,
                                                                              MesPqcInspectionPieceDetailDO detail,
                                                                              Long reviewId,
                                                                              LocalDateTime aggregatedAt) {
        requirePieceDetail(record, task, detail);
        MesPqcProcessInspectionAggregateDetailDO aggregate = MesPqcProcessInspectionAggregateDetailDO.builder()
                .sourcePqcRecordId(record.getId())
                .sourcePieceDetailId(detail.getId())
                .eventId(record.getEventId())
                .reviewId(reviewId)
                .productionSubmitEventId(record.getProductionSubmitEventId())
                .pqcTaskId(task.getId())
                .workOrderId(record.getWorkOrderId())
                .routeId(record.getRouteId())
                .routeProcessId(record.getRouteProcessId())
                .processId(record.getProcessId())
                .regulationVersionId(task.getRegulationVersionId())
                .inspectionType(task.getInspectionType())
                .businessDate(task.getBusinessDate())
                .shiftCode(task.getShiftCode())
                .roundNo(task.getRoundNo())
                .actualInspectionQuantity(task.getActualInspectionQuantity())
                .sampleNo(detail.getSampleNo())
                .itemCode(detail.getItemCode())
                .itemName(detail.getItemName())
                .inspectionMethod(detail.getInspectionMethod())
                .standardText(detail.getStandardText())
                .selectedEquipmentId(detail.getSelectedEquipmentId())
                .selectedEquipmentCode(detail.getSelectedEquipmentCode())
                .selectedEquipmentName(detail.getSelectedEquipmentName())
                .selectedEquipmentNumber(detail.getSelectedEquipmentNumber())
                .standardLowerLimit(detail.getStandardLowerLimit())
                .standardUpperLimit(detail.getStandardUpperLimit())
                .standardUnit(detail.getStandardUnit())
                .standardPrecision(detail.getStandardPrecision())
                .resultType(detail.getResultType())
                .itemResult(detail.getItemResult())
                .measuredValue(detail.getMeasuredValue())
                .judgement(detail.getJudgement())
                .aggregatedAt(aggregatedAt)
                .build();
        aggregate.setTenantId(record.getTenantId());
        return aggregate;
    }

    private static void requirePieceDetail(MesProProcessPoolPqcRecordDO record,
                                           MesPqcInspectionTaskDO task,
                                           MesPqcInspectionPieceDetailDO detail) {
        if (detail == null || detail.getId() == null
                || !Objects.equals(record.getTenantId(), detail.getTenantId())
                || !Objects.equals(task.getId(), detail.getTaskId())
                || detail.getSampleNo() == null
                || StrUtil.isBlank(detail.getItemCode())
                || StrUtil.isBlank(detail.getItemName())
                || StrUtil.isBlank(detail.getInspectionMethod())
                || StrUtil.isBlank(detail.getStandardText())
                || StrUtil.isBlank(detail.getResultType())
                || StrUtil.isBlank(detail.getMeasuredValue())
                || StrUtil.isBlank(detail.getJudgement())) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, record.getEventId());
        }
    }
}
