package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_WORK_ORDER_ABNORMAL_OPEN_EXISTS;

@Service
@Validated
public class MesWorkOrderAbnormalReportServiceImpl implements MesWorkOrderAbnormalReportService {

    private static final String ACTIVE_ORDER_ABNORMAL_REASON_CODE = "ACTIVE_ORDER_ABNORMAL";

    private final MesProcessPoolWorkOrderAbnormalMapper abnormalMapper;
    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesWorkOrderAbnormalStateService abnormalStateService;

    public MesWorkOrderAbnormalReportServiceImpl(MesProcessPoolWorkOrderAbnormalMapper abnormalMapper,
                                                 MesProcessPoolActiveOrderMapper activeOrderMapper,
                                                 MesWorkOrderAbnormalStateService abnormalStateService) {
        this.abnormalMapper = abnormalMapper;
        this.activeOrderMapper = activeOrderMapper;
        this.abnormalStateService = abnormalStateService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long markAndReport(MesWorkOrderAbnormalReportReqBO reqBO) {
        validateReq(reqBO);
        if (activeOrderMapper.selectActiveByLeaderAndWorkOrderForUpdate(
                reqBO.getMarkerUserId(), reqBO.getWorkOrderId())
                == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, reqBO.getWorkOrderId());
        }
        if (abnormalStateService.hasOpenAbnormal(reqBO.getWorkOrderId())) {
            throw exception(PRO_PROCESS_POOL_WORK_ORDER_ABNORMAL_OPEN_EXISTS, reqBO.getWorkOrderId());
        }
        LocalDateTime now = LocalDateTime.now();
        MesProcessPoolWorkOrderAbnormalDO abnormal = MesProcessPoolWorkOrderAbnormalDO.builder()
                .workOrderId(reqBO.getWorkOrderId())
                .abnormalReasonCode(ACTIVE_ORDER_ABNORMAL_REASON_CODE)
                .abnormalDescription(reqBO.getAbnormalDescription().trim())
                .reportStatus(MesProcessPoolWorkOrderAbnormalDO.REPORT_STATUS_REPORTED)
                .markerUserId(reqBO.getMarkerUserId())
                .markedAt(now)
                .reporterUserId(reqBO.getMarkerUserId())
                .reportedAt(now)
                .build();
        abnormalMapper.insert(abnormal);
        return abnormal.getId();
    }

    private void validateReq(MesWorkOrderAbnormalReportReqBO reqBO) {
        if (reqBO == null || reqBO.getWorkOrderId() == null || reqBO.getMarkerUserId() == null
                || isBlank(reqBO.getAbnormalDescription())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "workOrderAbnormal");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
