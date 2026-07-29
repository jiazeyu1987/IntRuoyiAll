package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolFifoTargetWorkOrder {

    private Long workOrderId;
    private String workOrderCode;
    private LocalDateTime plannedStartTime;
    private Long targetRouteProcessId;
    private Long targetProcessId;
    private BigDecimal requiredQuantity;
    private BigDecimal alreadyAllocatedQuantity;

}
