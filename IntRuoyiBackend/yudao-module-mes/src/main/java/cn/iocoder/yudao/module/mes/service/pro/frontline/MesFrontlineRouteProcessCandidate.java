package cn.iocoder.yudao.module.mes.service.pro.frontline;

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
                                                String workstationName) {
}
