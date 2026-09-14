package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public record MesFrontlineRuntimeConfig(Long routeId,
                                        Long routeProcessId,
                                        Long processId,
                                        List<MesFrontlineTeamEmployeeOption> employees,
                                        List<MesFrontlineTeamDeviceOption> devices,
                                        List<MesFrontlineDefectReasonOption> defectReasons,
                                        List<MesFrontlineProcessMaterial> materials,
                                        MesFrontlineProductionSubmitContext productionSubmitContext,
                                        List<MesFrontlineEmployeeSwitchResult> employeeSwitchSnapshots,
                                        String frontlineSessionSnapshotId,
                                        String frontlineSessionSnapshotHash,
                                        List<MesFrontlineProcessMaterial> inputMaterials) {

    public MesFrontlineRuntimeConfig(Long routeId, Long routeProcessId, Long processId,
                                    List<MesFrontlineTeamEmployeeOption> employees,
                                    List<MesFrontlineTeamDeviceOption> devices,
                                    List<MesFrontlineDefectReasonOption> defectReasons,
                                    List<MesFrontlineProcessMaterial> materials,
                                    MesFrontlineProductionSubmitContext productionSubmitContext,
                                    List<MesFrontlineEmployeeSwitchResult> employeeSwitchSnapshots,
                                    String frontlineSessionSnapshotId,
                                    String frontlineSessionSnapshotHash) {
        this(routeId, routeProcessId, processId, employees, devices, defectReasons, materials,
                productionSubmitContext, employeeSwitchSnapshots, frontlineSessionSnapshotId,
                frontlineSessionSnapshotHash, List.of());
    }
}
