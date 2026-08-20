package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class MesDeviceParameterSnapshotCodec {

    public static final String STATE_FROZEN = "FROZEN";
    public static final String STATE_MISSING_LEGACY = "MISSING_LEGACY";
    public static final String SOURCE_CURRENT_ROUTE_PROCESS_AT_SUBMIT = "CURRENT_ROUTE_PROCESS_AT_SUBMIT";

    private MesDeviceParameterSnapshotCodec() {
    }

    public static String canonicalize(List<MesProcessPoolDeviceParameterRuleDO> rules,
                                      Long routeProcessId, Long processId) {
        List<MesProcessPoolDeviceParameterRuleDO> exactRules = rules == null ? List.of() : rules.stream()
                .filter(Objects::nonNull)
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> Objects.equals(routeProcessId, rule.getRouteProcessId()))
                .filter(rule -> Objects.equals(processId, rule.getProcessId()))
                .sorted(Comparator.comparing(MesProcessPoolDeviceParameterRuleDO::getDeviceId,
                                Comparator.nullsLast(Long::compareTo))
                        .thenComparing(rule -> normalizeCode(rule.getParameterCode()),
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(MesProcessPoolDeviceParameterRuleDO::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .toList();
        Set<String> canonicalKeys = new HashSet<>();
        List<MesDeviceParameterSnapshotRule> snapshotRules = new ArrayList<>(exactRules.size());
        for (MesProcessPoolDeviceParameterRuleDO rule : exactRules) {
            String parameterCode = normalizeCode(rule.getParameterCode());
            if (rule.getDeviceId() == null || parameterCode == null) {
                throw new IllegalStateException("Device parameter rule identity is incomplete for routeProcessId="
                        + routeProcessId + ", processId=" + processId);
            }
            String canonicalKey = rule.getDeviceId() + "|" + parameterCode;
            if (!canonicalKeys.add(canonicalKey)) {
                throw new IllegalStateException("Duplicate device parameter canonical key " + canonicalKey
                        + " for routeProcessId=" + routeProcessId + ", processId=" + processId);
            }
            snapshotRules.add(MesDeviceParameterSnapshotRule.builder()
                    .routeProcessId(routeProcessId)
                    .processId(processId)
                    .deviceId(rule.getDeviceId())
                    .parameterCode(parameterCode)
                    .parameterName(rule.getParameterName())
                    .unit(rule.getUnit())
                    .lowerLimit(rule.getLowerLimit())
                    .upperLimit(rule.getUpperLimit())
                    .defaultValue(rule.getDefaultValue())
                    .valueType(rule.getValueType())
                    .standardText(rule.getStandardText())
                    .optionValuesJson(rule.getOptionValuesJson())
                    .defaultText(rule.getDefaultText())
                    .decimalScale(rule.getDecimalScale())
                    .build());
        }
        return JsonUtils.toJsonString(snapshotRules);
    }

    public static List<MesDeviceParameterSnapshotRule> parse(String snapshotJson) {
        if (snapshotJson == null) {
            throw new IllegalArgumentException("parameter snapshot JSON is required");
        }
        List<MesDeviceParameterSnapshotRule> rules = JsonUtils.parseArray(snapshotJson,
                MesDeviceParameterSnapshotRule.class);
        if (rules == null) {
            throw new IllegalArgumentException("parameter snapshot JSON must be an array");
        }
        return List.copyOf(rules);
    }

    public static String sha256(String snapshotJson) {
        return DigestUtil.sha256Hex(snapshotJson);
    }

    public static String normalizeCode(String parameterCode) {
        if (parameterCode == null) {
            return null;
        }
        String normalized = parameterCode.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
