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
                                                String inspectionType,
                                                LocalDate businessDate,
                                                String shiftCode,
                                                Integer roundNo,
                                                Integer plannedInspectionQuantity,
                                                List<MesFrontlinePqcInspectionItem> inspectionItems) {

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
                null, null, null, null, null, null, null, null, List.of());
    }
}
