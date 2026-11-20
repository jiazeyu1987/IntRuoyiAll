package cn.iocoder.yudao.module.mes.service.md.workstation;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class MesMdWorkstationCapacityMetrics {

    Integer configuredWorkerCount;

    Integer currentWorkerCount;

    BigDecimal machineryStandardHourlyCapacity;

    /** 班次产能；列表中缺少有效班次小时的工作站为 null，表示未计算。 */
    BigDecimal todayCapacity;

}
