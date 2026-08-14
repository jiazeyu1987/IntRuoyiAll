package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public record MesFrontlineSessionSnapshotContent(Long tenantId,
                                                 Long loginUserId,
                                                 Long routeId,
                                                 Long routeProcessId,
                                                 Long processId,
                                                 Long workstationId,
                                                 List<MesFrontlineEmployeeSwitchResult> employeeSwitchSnapshots,
                                                 List<MesFrontlineTeamDeviceOption> devices,
                                                 List<MesFrontlineDefectReasonOption> defectReasons,
                                                 MesFrontlineProductionSubmitContext productionSubmitContext) {
}
