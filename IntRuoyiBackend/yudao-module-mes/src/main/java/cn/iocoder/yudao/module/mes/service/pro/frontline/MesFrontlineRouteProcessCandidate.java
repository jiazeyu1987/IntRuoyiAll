package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.LocalDate;
import java.util.List;

public record MesFrontlineRouteProcessCandidate(Long routeId,
                                                String routeCode,
                                                String routeName,
                                                Long routeProcessId,
                                                Long processId,
                                                String processCode,
                                                String processName,
                                                Integer sort,
                                                Long deviceId,
                                                String deviceCode,
                                                String deviceName,
                                                Long workstationId,
                                                String workstationCode,
                                                String workstationName,
                                                Long activeOrderId,
                                                Long pqcTaskId,
                                                Long regulationVersionId,
                                                Boolean finalInspectionApplicable,
                                                String inspectionType,
                                                LocalDate businessDate,
                                                String shiftCode,
                                                Integer roundNo,
                                                Integer plannedInspectionQuantity,
                                                List<MesFrontlinePqcInspectionItem> inspectionItems,
                                                List<MesFrontlinePqcTaskOption> pqcTaskOptions,
                                                List<MesFrontlineProductionSubmitCandidate> productionSubmitCandidates,
                                                String contextSource) {

    public static final String CONTEXT_SOURCE_POST_BINDING = "POST_BINDING";
    public static final String CONTEXT_SOURCE_ROUTE_START_PRODUCTION_LEADER =
            "ROUTE_START_PRODUCTION_LEADER";
    public static final String CONTEXT_SOURCE_PQC_ACTIVE_ORDER = "PQC_ACTIVE_ORDER";

    public MesFrontlineRouteProcessCandidate(Long routeId,
                                             String routeCode,
                                             String routeName,
                                             Long routeProcessId,
                                             Long processId,
                                             String processCode,
                                             String processName,
                                             Integer sort,
                                             Long deviceId,
                                             String deviceCode,
                                             String deviceName,
                                             Long workstationId,
                                             String workstationCode,
                                             String workstationName) {
        this(routeId, routeCode, routeName, routeProcessId, processId, processCode, processName, sort,
                deviceId, deviceCode, deviceName, workstationId, workstationCode, workstationName,
                null, null, null, null, null, null, null, null, null, List.of(), List.of(), List.of(), null);
    }

    public MesFrontlineRouteProcessCandidate(Long routeId,
                                             String routeCode,
                                             String routeName,
                                             Long routeProcessId,
                                             Long processId,
                                             String processCode,
                                             String processName,
                                             Integer sort,
                                             Long deviceId,
                                             String deviceCode,
                                             String deviceName,
                                             Long workstationId,
                                             String workstationCode,
                                             String workstationName,
                                             String contextSource) {
        this(routeId, routeCode, routeName, routeProcessId, processId, processCode, processName, sort,
                deviceId, deviceCode, deviceName, workstationId, workstationCode, workstationName,
                null, null, null, null, null, null, null, null, null, List.of(), List.of(), List.of(), contextSource);
    }
}
