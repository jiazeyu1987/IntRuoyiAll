package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

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
        validatePqcEvent(record, event, eventId);
        MesPqcInspectionTaskDO task = pqcTaskMapper.selectById(event.getFeedbackSourceId());
        validatePqcTask(record, event, task, eventId);
        List<MesPqcInspectionPieceDetailDO> pieceDetails = pieceDetailMapper.selectListByTaskId(task.getId());
        validatePieceDetails(record, task, pieceDetails, eventId);

        LocalDateTime aggregatedAt = LocalDateTime.now();
        int updated = pqcRecordMapper.updateProcessInspectionAggregatedIfPending(tenantId, eventId, reviewId,
                aggregatedAt);
        if (updated != 1) {
            throw exception(PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED, eventId, reviewId);
        }
        int taskUpdated = pqcTaskMapper.updateConfirmedIfSubmitted(task.getId(),
                MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED,
                MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED);
        if (taskUpdated != 1) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, eventId);
        }
        List<MesPqcProcessInspectionAggregateDetailDO> aggregateRows = pieceDetails.stream()
                .map(pieceDetail -> buildAggregateDetail(record, event, task, pieceDetail, reviewId, aggregatedAt))
                .toList();
        if (!Boolean.TRUE.equals(aggregateDetailMapper.insertBatch(aggregateRows))) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcProcessInspectionAggregateDetail");
        }
    }

    private static Long requireTenantId(MesProProcessPoolPqcRecordDO record, Long eventId) {
        if (record.getTenantId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,
                    "pqcProcessInspectionAggregation.tenantId.eventId=" + eventId);
        }
        return record.getTenantId();
    }

    private static void validatePqcEvent(MesProProcessPoolPqcRecordDO record, MesProProcessPoolEventDO event,
                                         Long eventId) {
        if (event == null
                || !Objects.equals(record.getEventId(), event.getId())
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
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcProcessInspectionEvent:" + eventId);
        }
    }

    private static void validatePqcTask(MesProProcessPoolPqcRecordDO record, MesProProcessPoolEventDO event,
                                        MesPqcInspectionTaskDO task, Long eventId) {
        if (task == null
                || !Objects.equals(record.getTenantId(), task.getTenantId())
                || !Objects.equals(event.getFeedbackSourceId(), task.getId())
                || !Objects.equals(record.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(record.getRouteId(), task.getRouteId())
                || !Objects.equals(record.getRouteProcessId(), task.getRouteProcessId())
                || !Objects.equals(record.getProcessId(), task.getProcessId())
                || task.getRouteVersionId() == null
                || task.getRegulationVersionId() == null
                || StrUtil.isBlank(task.getInspectionType())
                || task.getBusinessDate() == null
                || StrUtil.isBlank(task.getShiftCode())
                || task.getRoundNo() == null
                || task.getActualInspectionQuantity() == null
                || task.getActualInspectionQuantity() <= 0
                || !MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED.equals(task.getTaskStatus())) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, eventId);
        }
    }

    private static void validatePieceDetails(MesProProcessPoolPqcRecordDO record, MesPqcInspectionTaskDO task,
                                             List<MesPqcInspectionPieceDetailDO> pieceDetails, Long eventId) {
        if (pieceDetails == null || pieceDetails.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, eventId);
        }
        if (pieceDetails.stream().anyMatch(pieceDetail -> isInvalidPieceDetail(record, task, pieceDetail))) {
            throw exception(PRO_PROCESS_POOL_PQC_RECORD_REQUIRED, eventId);
        }
    }

    private static boolean isInvalidPieceDetail(MesProProcessPoolPqcRecordDO record,
                                                MesPqcInspectionTaskDO task,
                                                MesPqcInspectionPieceDetailDO pieceDetail) {
        return pieceDetail == null
                || pieceDetail.getId() == null
                || !Objects.equals(record.getTenantId(), pieceDetail.getTenantId())
                || !Objects.equals(task.getId(), pieceDetail.getTaskId())
                || pieceDetail.getSampleNo() == null
                || StrUtil.isBlank(pieceDetail.getItemCode())
                || StrUtil.isBlank(pieceDetail.getItemName())
                || StrUtil.isBlank(pieceDetail.getInspectionMethod())
                || StrUtil.isBlank(pieceDetail.getStandardText())
                || StrUtil.isBlank(pieceDetail.getResultType())
                || StrUtil.isBlank(pieceDetail.getMeasuredValue())
                || StrUtil.isBlank(pieceDetail.getJudgement());
    }

    private static MesPqcProcessInspectionAggregateDetailDO buildAggregateDetail(
            MesProProcessPoolPqcRecordDO record,
            MesProProcessPoolEventDO event,
            MesPqcInspectionTaskDO task,
            MesPqcInspectionPieceDetailDO pieceDetail,
            Long reviewId,
            LocalDateTime aggregatedAt) {
        MesPqcProcessInspectionAggregateDetailDO aggregateDetail =
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .sourcePqcRecordId(record.getId())
                        .sourcePieceDetailId(pieceDetail.getId())
                        .eventId(event.getId())
                        .reviewId(reviewId)
                        .productionSubmitEventId(record.getProductionSubmitEventId())
                        .pqcTaskId(task.getId())
                        .activeOrderId(task.getActiveOrderId())
                        .workOrderId(task.getWorkOrderId())
                        .routeId(task.getRouteId())
                        .routeVersionId(task.getRouteVersionId())
                        .routeProcessId(task.getRouteProcessId())
                        .processId(task.getProcessId())
                        .regulationVersionId(task.getRegulationVersionId())
                        .inspectionType(task.getInspectionType())
                        .businessDate(task.getBusinessDate())
                        .shiftCode(task.getShiftCode())
                        .roundNo(task.getRoundNo())
                        .actualInspectionQuantity(task.getActualInspectionQuantity())
                        .sampleNo(pieceDetail.getSampleNo())
                        .itemCode(pieceDetail.getItemCode())
                        .itemName(pieceDetail.getItemName())
                        .inspectionMethod(pieceDetail.getInspectionMethod())
                        .standardText(pieceDetail.getStandardText())
                        .selectedEquipmentId(pieceDetail.getSelectedEquipmentId())
                        .selectedEquipmentCode(pieceDetail.getSelectedEquipmentCode())
                        .selectedEquipmentName(pieceDetail.getSelectedEquipmentName())
                        .selectedEquipmentNumber(pieceDetail.getSelectedEquipmentNumber())
                        .standardLowerLimit(pieceDetail.getStandardLowerLimit())
                        .standardUpperLimit(pieceDetail.getStandardUpperLimit())
                        .standardUnit(pieceDetail.getStandardUnit())
                        .standardPrecision(pieceDetail.getStandardPrecision())
                        .resultType(pieceDetail.getResultType())
                        .itemResult(pieceDetail.getItemResult())
                        .measuredValue(pieceDetail.getMeasuredValue())
                        .judgement(pieceDetail.getJudgement())
                        .aggregatedAt(aggregatedAt)
                        .build();
        aggregateDetail.setTenantId(record.getTenantId());
        return aggregateDetail;
    }
}
