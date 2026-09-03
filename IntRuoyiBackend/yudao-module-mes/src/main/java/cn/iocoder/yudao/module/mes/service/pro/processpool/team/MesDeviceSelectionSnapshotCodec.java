package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceSelectionGroup;
import cn.hutool.crypto.digest.DigestUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class MesDeviceSelectionSnapshotCodec {

    private MesDeviceSelectionSnapshotCodec() {
    }

    public static String canonicalize(List<MesProcessPoolTeamProcessDeviceDO> bindings, Long processId) {
        List<MesProcessPoolTeamProcessDeviceDO> exactBindings = (bindings == null
                ? List.<MesProcessPoolTeamProcessDeviceDO>of() : bindings)
                .stream()
                .filter(binding -> Boolean.TRUE.equals(binding.getEnabled()))
                .filter(binding -> Objects.equals(processId, binding.getProcessId()))
                .toList();
        for (MesProcessPoolTeamProcessDeviceDO binding : exactBindings) {
            if (binding.getDeviceId() == null || binding.getDeviceGroupKey() == null
                    || binding.getDeviceGroupKey().isBlank()) {
                throw new IllegalStateException("设备组快照身份不完整：processId=" + processId);
            }
        }
        List<MesFrontlineDeviceSelectionGroup> groups = exactBindings.stream()
                .collect(Collectors.groupingBy(MesProcessPoolTeamProcessDeviceDO::getDeviceGroupKey))
                .entrySet().stream()
                .map(entry -> toGroup(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MesFrontlineDeviceSelectionGroup::deviceGroupKey))
                .toList();
        return JsonUtils.toJsonString(groups);
    }

    public static String sha256(String snapshotJson) {
        return DigestUtil.sha256Hex(snapshotJson);
    }

    public static List<MesFrontlineDeviceSelectionGroup> parse(String snapshotJson) {
        if (snapshotJson == null) {
            throw new IllegalArgumentException("device selection snapshot JSON is required");
        }
        List<MesFrontlineDeviceSelectionGroup> groups = JsonUtils.parseArray(snapshotJson,
                MesFrontlineDeviceSelectionGroup.class);
        if (groups == null) {
            throw new IllegalArgumentException("device selection snapshot JSON must be an array");
        }
        return List.copyOf(groups);
    }

    private static MesFrontlineDeviceSelectionGroup toGroup(String groupKey,
                                                             List<MesProcessPoolTeamProcessDeviceDO> bindings) {
        if (groupKey == null || groupKey.isBlank() || bindings == null || bindings.isEmpty()) {
            throw new IllegalStateException("设备组快照身份不完整");
        }
        String selectionMode = bindings.get(0).getSelectionMode();
        if (!"SINGLE".equals(selectionMode) && !"MULTIPLE".equals(selectionMode)
                || bindings.stream().anyMatch(binding -> !Objects.equals(selectionMode, binding.getSelectionMode()))) {
            throw new IllegalStateException("设备组选择模式不完整或冲突：" + groupKey);
        }
        List<Long> deviceIds = bindings.stream().map(MesProcessPoolTeamProcessDeviceDO::getDeviceId)
                .filter(Objects::nonNull).distinct().sorted().toList();
        if (deviceIds.isEmpty()) {
            throw new IllegalStateException("设备组缺少设备：" + groupKey);
        }
        return new MesFrontlineDeviceSelectionGroup(groupKey, selectionMode, deviceIds);
    }
}
