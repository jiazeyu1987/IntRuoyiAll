package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MesFrontlineActiveOrderCandidate(Long activeOrderId,
                                               Long workOrderId,
                                               String workOrderCode,
                                               String workOrderName,
                                               Long productId,
                                               String productCode,
                                               String productName,
                                               BigDecimal quantity,
                                               Long routeId,
                                               String routeCode,
                                               String routeName,
                                               LocalDateTime latestSubmitTime) {
}
