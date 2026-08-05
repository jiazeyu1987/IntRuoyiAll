package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import java.math.BigDecimal;

public interface MesFrontlineLossReasonValidator {

    MesFrontlineLossReasonSnapshot requireEnabledLossReason(Long routeProcessId,
                                                            Long lossReasonId,
                                                            BigDecimal lossQuantity);

}
