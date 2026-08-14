package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public record MesFrontlineRuntimeConfig(Long routeId,
                                        Long routeProcessId,
                                        Long processId,
                                        List<MesFrontlineTeamEmployeeOption> employees,
                                        List<MesFrontlineTeamDeviceOption> devices,
                                        List<MesFrontlineDefectReasonOption> defectReasons,
                                        MesFrontlineProductionSubmitContext productionSubmitContext) {
}
