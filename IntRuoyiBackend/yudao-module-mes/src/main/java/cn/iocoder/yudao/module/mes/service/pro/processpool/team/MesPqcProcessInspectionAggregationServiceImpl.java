package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_PQC_RECORD_REQUIRED;

@Service
@Validated
public class MesPqcProcessInspectionAggregationServiceImpl
        implements MesPqcProcessInspectionAggregationService {

    private final MesProProcessPoolPqcRecordMapper pqcRecordMapper;

    public MesPqcProcessInspectionAggregationServiceImpl(MesProProcessPoolPqcRecordMapper pqcRecordMapper) {
        this.pqcRecordMapper = pqcRecordMapper;
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

        int updated = pqcRecordMapper.updateProcessInspectionAggregatedIfPending(eventId, reviewId,
                LocalDateTime.now());
        if (updated != 1) {
            throw exception(PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED, eventId, reviewId);
        }
    }
}
