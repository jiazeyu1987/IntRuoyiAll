package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_LOSS_REASON_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_LOSS_REASON_REQUIRED;

@Service
public class MesFrontlineLossReasonValidatorImpl implements MesFrontlineLossReasonValidator {

    private final MesProcessPoolDefectReasonMapper defectReasonMapper;

    public MesFrontlineLossReasonValidatorImpl(MesProcessPoolDefectReasonMapper defectReasonMapper) {
        this.defectReasonMapper = defectReasonMapper;
    }

    @Override
    public MesFrontlineLossReasonSnapshot requireEnabledLossReason(Long routeProcessId,
                                                                   Long lossReasonId,
                                                                   BigDecimal lossQuantity) {
        if (lossQuantity == null || lossQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (routeProcessId == null || lossReasonId == null) {
            throw exception(PRO_FRONTLINE_FEEDBACK_LOSS_REASON_REQUIRED);
        }
        MesProcessPoolDefectReasonDO reason = defectReasonMapper.selectById(lossReasonId);
        if (reason == null
                || !MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS.equals(reason.getReasonType())
                || !Boolean.TRUE.equals(reason.getEnabled())
                || !Objects.equals(routeProcessId, reason.getRouteProcessId())) {
            throw exception(PRO_FRONTLINE_FEEDBACK_LOSS_REASON_INVALID, lossReasonId);
        }
        return new MesFrontlineLossReasonSnapshot(reason.getId(), reason.getReasonCode(), reason.getReasonName());
    }

}
