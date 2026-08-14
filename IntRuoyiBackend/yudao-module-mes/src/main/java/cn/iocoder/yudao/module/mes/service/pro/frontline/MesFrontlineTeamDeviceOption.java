package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public record MesFrontlineTeamDeviceOption(Long deviceId,
                                           String deviceCode,
                                           String deviceName,
                                           String deviceStatus,
                                           List<MesFrontlineDeviceParameterOption> parameters) {
}
