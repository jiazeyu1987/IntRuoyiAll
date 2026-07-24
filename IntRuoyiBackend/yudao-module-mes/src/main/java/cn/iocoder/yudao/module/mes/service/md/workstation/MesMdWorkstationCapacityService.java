package cn.iocoder.yudao.module.mes.service.md.workstation;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface MesMdWorkstationCapacityService {

    Map<Long, MesMdWorkstationCapacityMetrics> getCapacityMetrics(Collection<MesMdWorkstationDO> workstations,
                                                                  BigDecimal effectiveHours);

    Map<Long, MesMdWorkstationCapacityMetrics> getCapacityMetricsUsingShiftHours(
            Collection<MesMdWorkstationDO> workstations);

}
