package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesWorkOrderAbnormalReportServiceImpl implements MesWorkOrderAbnormalReportService {

    private final MesProcessPoolWorkOrderAbnormalMapper abnormalMapper;

    public MesWorkOrderAbnormalReportServiceImpl(MesProcessPoolWorkOrderAbnormalMapper abnormalMapper) {
        this.abnormalMapper = abnormalMapper;
    }

    @Override
    public Long markAndReport(MesWorkOrderAbnormalReportReqBO reqBO) {
        validateReq(reqBO);
        LocalDateTime now = LocalDateTime.now();
        MesProcessPoolWorkOrderAbnormalDO abnormal = MesProcessPoolWorkOrderAbnormalDO.builder()
                .workOrderId(reqBO.getWorkOrderId())
                .routeProcessId(reqBO.getRouteProcessId())
                .processId(reqBO.getProcessId())
                .sourceEventId(reqBO.getSourceEventId())
                .abnormalReasonCode(reqBO.getAbnormalReasonCode())
                .abnormalDescription(reqBO.getAbnormalDescription())
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
                || isBlank(reqBO.getAbnormalReasonCode()) || isBlank(reqBO.getAbnormalDescription())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "workOrderAbnormal");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
