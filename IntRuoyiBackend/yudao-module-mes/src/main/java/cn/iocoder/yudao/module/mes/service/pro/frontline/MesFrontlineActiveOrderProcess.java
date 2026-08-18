package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;

public record MesFrontlineActiveOrderProcess(Long activeOrderId,
                                             Long routeId,
                                             Long routeVersionId,
                                             String routeCode,
                                             String routeName,
                                             Long routeProcessId,
                                             Long processId,
                                             String processCode,
                                             String processName,
                                             Integer sort,
                                             Long workstationId,
                                             String workstationCode,
                                             String workstationName,
                                             BigDecimal productionQuantityFactor,
                                             BigDecimal targetQuantity) {

    public MesFrontlineRouteProcessCandidate toRouteProcessCandidate() {
        return new MesFrontlineRouteProcessCandidate(routeId, routeCode, routeName, routeProcessId, processId,
                processCode, processName, sort, null, null, null, workstationId, workstationCode, workstationName,
                MesFrontlineRouteProcessCandidate.CONTEXT_SOURCE_ROUTE_START_PRODUCTION_LEADER);
    }
}
