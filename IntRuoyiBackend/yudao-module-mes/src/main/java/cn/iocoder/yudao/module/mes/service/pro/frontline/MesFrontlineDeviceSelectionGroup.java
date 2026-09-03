package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.util.List;

public record MesFrontlineDeviceSelectionGroup(String deviceGroupKey, String selectionMode, List<Long> deviceIds) {
}
