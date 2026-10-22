package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolWorkOrderAbnormalDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolWorkOrderAbnormalMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Objects;

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
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectByIdForUpdate(reqBO.getActiveOrderId());
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), reqBO.getMarkerUserId())
                || !MesTeamLeaderActiveOrderServiceImpl.STATUS_ACTIVE.equals(activeOrder.getActiveStatus())
                || activeOrder.getWorkOrderId() == null) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, reqBO.getActiveOrderId());
        }
        Long workOrderId = activeOrder.getWorkOrderId();
        if (abnormalStateService.hasOpenAbnormal(workOrderId)) {
            throw exception(PRO_PROCESS_POOL_WORK_ORDER_ABNORMAL_OPEN_EXISTS, workOrderId);
        }
        LocalDateTime now = LocalDateTime.now();
        MesProcessPoolWorkOrderAbnormalDO abnormal = MesProcessPoolWorkOrderAbnormalDO.builder()
                .workOrderId(workOrderId)
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
        if (reqBO == null || reqBO.getActiveOrderId() == null || reqBO.getMarkerUserId() == null
                || isBlank(reqBO.getAbnormalDescription())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "workOrderAbnormal");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
