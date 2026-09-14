package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public record MesFrontlineTeamDeviceOption(Long deviceId,
                                           String deviceCode,
                                           String deviceName,
                                           String deviceStatus,
                                           String deviceGroupKey,
                                           String selectionMode,
                                           List<MesFrontlineDeviceParameterOption> parameters) {

    public MesFrontlineTeamDeviceOption(Long deviceId, String deviceCode, String deviceName, String deviceStatus,
                                        List<MesFrontlineDeviceParameterOption> parameters) {
        this(deviceId, deviceCode, deviceName, deviceStatus, null, "SINGLE", parameters);
    }
}
