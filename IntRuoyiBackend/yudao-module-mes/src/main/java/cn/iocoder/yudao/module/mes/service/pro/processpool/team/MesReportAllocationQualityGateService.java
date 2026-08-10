package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE;

@Service
public class MesReportAllocationQualityGateService {

    private final MesReportAllocationPoolQuantityService poolQuantityService;
    private final MesProProcessPoolEventMapper eventMapper;
    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper;

    public MesReportAllocationQualityGateService(MesReportAllocationPoolQuantityService poolQuantityService,
                                                 MesProProcessPoolEventMapper eventMapper,
                                                 MesProProcessPoolPqcRecordMapper pqcRecordMapper,
                                                 MesPqcInspectionTaskMapper pqcTaskMapper,
                                                 MesPqcInspectionPieceDetailMapper pqcPieceDetailMapper) {
        this.poolQuantityService = poolQuantityService;
        this.eventMapper = eventMapper;
        this.pqcRecordMapper = pqcRecordMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.pqcPieceDetailMapper = pqcPieceDetailMapper;
    }

    public BigDecimal requireAllocatablePoolQuantity(MesProProcessPoolEventDO event) {
        BigDecimal outputQuantity = poolQuantityService.requirePoolQuantity(event);
        List<MesProProcessPoolPqcRecordDO> records =
                pqcRecordMapper.selectListByProductionSubmitEventId(event.getId());
        if (records == null || records.size() != 1) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        MesProProcessPoolPqcRecordDO record = records.get(0);
        if (!Objects.equals(record.getProductionSubmitEventId(), event.getId())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        if (!MesProProcessPoolPqcRecordDO.INSPECTION_RESULT_SUCCESS.equals(record.getInspectionResult())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE,
                    event.getId(), record.getInspectionResult());
        }
        requireCompleteSuccessfulSampling(event, record);
        return outputQuantity;
    }

    private void requireCompleteSuccessfulSampling(MesProProcessPoolEventDO event,
                                                   MesProProcessPoolPqcRecordDO record) {
        MesProProcessPoolEventDO pqcEvent = eventMapper.selectByIdForUpdate(record.getEventId());
        if (pqcEvent == null
                || !Objects.equals(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION, pqcEvent.getEventType())
                || !Objects.equals(event.getWorkOrderId(), pqcEvent.getWorkOrderId())
                || !Objects.equals(event.getRouteProcessId(), pqcEvent.getRouteProcessId())
                || !Objects.equals(event.getProcessId(), pqcEvent.getProcessId())
                || pqcEvent.getFeedbackSourceId() == null) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        MesPqcInspectionTaskDO task = pqcTaskMapper.selectByIdForUpdate(pqcEvent.getFeedbackSourceId());
        if (task == null
                || !Objects.equals(task.getId(), pqcEvent.getFeedbackSourceId())
                || !Objects.equals(event.getWorkOrderId(), task.getWorkOrderId())
                || !Objects.equals(event.getRouteProcessId(), task.getRouteProcessId())
                || !Objects.equals(event.getProcessId(), task.getProcessId())) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_BINDING_REQUIRED, event.getId());
        }
        Integer sampleCount = task.getActualInspectionQuantity();
        List<MesPqcInspectionPieceDetailDO> details = pqcPieceDetailMapper.selectListByTaskId(task.getId());
        if (sampleCount == null || sampleCount <= 0 || details == null || details.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE,
                    event.getId(), "SAMPLE_COVERAGE_MISSING");
        }
        Map<Integer, List<MesPqcInspectionPieceDetailDO>> bySample = details.stream()
                .filter(detail -> detail != null && detail.getSampleNo() != null)
                .collect(Collectors.groupingBy(MesPqcInspectionPieceDetailDO::getSampleNo,
                        LinkedHashMap::new, Collectors.toList()));
        for (int sampleNo = 1; sampleNo <= sampleCount; sampleNo++) {
            List<MesPqcInspectionPieceDetailDO> sampleDetails = bySample.get(sampleNo);
            if (sampleDetails == null || sampleDetails.isEmpty()
                    || sampleDetails.stream().anyMatch(detail -> !MesProProcessPoolPqcRecordDO
                    .INSPECTION_RESULT_SUCCESS.equals(detail.getJudgement()))) {
                throw exception(PRO_PROCESS_POOL_REPORT_ALLOCATION_QUALITY_NOT_ALLOCATABLE,
                        event.getId(), "SAMPLE_" + sampleNo + "_NOT_SUCCESS");
            }
        }
    }

}
