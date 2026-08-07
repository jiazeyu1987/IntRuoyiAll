package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MesFrontlineProductionSubmitContext(Long workOrderId,
                                                  String workOrderCode,
                                                  String workOrderName,
                                                  Long taskId,
                                                  Long routeId,
                                                  Long routeProcessId,
                                                  Long processId,
                                                  Long workstationId,
                                                  Long itemId,
                                                  Long approveUserId,
                                                  Long recordbookId,
                                                  BigDecimal scheduledQuantity,
                                                  LocalDateTime expireDate) {
}
