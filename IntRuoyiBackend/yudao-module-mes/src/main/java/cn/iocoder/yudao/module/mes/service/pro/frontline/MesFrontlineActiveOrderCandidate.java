package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.LocalDateTime;

public record MesFrontlineActiveOrderCandidate(Long workOrderId,
                                               String workOrderCode,
                                               String workOrderName,
                                               Long productId,
                                               String productCode,
                                               String productName,
                                               Long routeId,
                                               String routeCode,
                                               String routeName,
                                               LocalDateTime latestSubmitTime) {
}

