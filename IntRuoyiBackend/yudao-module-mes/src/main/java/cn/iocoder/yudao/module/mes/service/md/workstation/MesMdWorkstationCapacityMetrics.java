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

    BigDecimal todayCapacity;

}
