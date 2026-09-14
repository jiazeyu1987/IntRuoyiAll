package cn.iocoder.yudao.module.mes.service.pro.frontline;

public record MesFrontlineDeviceRouteBinding(Long loginUserId,
                                             Long routeId,
                                             String routeCode,
                                             String routeName,
                                             Long deviceId,
                                             String deviceCode,
                                             String deviceName,
                                             Long workstationId,
                                             String workstationCode,
                                             String workstationName) {
}
