package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;

import java.math.BigDecimal;
import java.util.List;

public interface MesFrontlineLossReasonValidator {

    MesFrontlineLossReasonSnapshot requireEnabledLossReason(Long routeProcessId,
                                                            Long lossReasonId,
                                                            BigDecimal lossQuantity);

    List<MesFrontlineLossReasonSnapshot> requireEnabledLossReasons(
            Long routeProcessId,
            List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails,
            BigDecimal lossQuantity);

}
