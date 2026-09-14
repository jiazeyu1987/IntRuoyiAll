package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.util.List;

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
                                             BigDecimal targetQuantity,
                                             Boolean checkFlag) {

    public MesFrontlineActiveOrderProcess(Long activeOrderId,
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
        this(activeOrderId, routeId, routeVersionId, routeCode, routeName, routeProcessId, processId, processCode,
                processName, sort, workstationId, workstationCode, workstationName, productionQuantityFactor,
                targetQuantity, Boolean.FALSE);
    }

    public MesFrontlineRouteProcessCandidate toRouteProcessCandidate() {
        return new MesFrontlineRouteProcessCandidate(routeId, routeCode, routeName, routeProcessId, processId,
                processCode, processName, sort, null, null, null, workstationId, workstationCode, workstationName,
                activeOrderId, null, null, null, null, null, null, null, null, List.of(), List.of(), List.of(),
                MesFrontlineRouteProcessCandidate.CONTEXT_SOURCE_ROUTE_START_PRODUCTION_LEADER, checkFlag);
    }
}
