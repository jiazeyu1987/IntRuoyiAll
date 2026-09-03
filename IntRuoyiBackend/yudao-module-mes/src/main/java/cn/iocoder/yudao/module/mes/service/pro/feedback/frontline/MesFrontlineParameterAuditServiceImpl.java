package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineProcessPoolContextReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotCodec;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDeviceParameterSnapshotRule;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MesFrontlineParameterAuditServiceImpl implements MesFrontlineParameterAuditService {

    public static final String REASON_DEVICE_ID_MISSING = "DEVICE_ID_MISSING";
    public static final String REASON_PARAMETER_CODE_MISSING = "PARAMETER_CODE_MISSING";
    public static final String REASON_SELECTED_DEVICE_ID_MISSING = "SELECTED_DEVICE_ID_MISSING";
    public static final String REASON_DEVICE_MISMATCH = "DEVICE_MISMATCH";
    public static final String REASON_DUPLICATE_PARAMETER = "DUPLICATE_PARAMETER";
    public static final String REASON_RULE_NOT_FOUND = "RULE_NOT_FOUND";
    public static final String REASON_CONTEXT_MISMATCH = "CONTEXT_MISMATCH";
    public static final String REASON_SNAPSHOT_MISSING_LEGACY = "SNAPSHOT_MISSING_LEGACY";
    public static final String REASON_SNAPSHOT_HASH_MISMATCH = "SNAPSHOT_HASH_MISMATCH";

    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;

    public MesFrontlineParameterAuditServiceImpl(MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper,
                                                 MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper) {
        this.snapshotMapper = snapshotMapper;
        this.parameterRuleMapper = parameterRuleMapper;
    }

    @Override
    public MesFrontlineParameterAuditResult resolveAndApply(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        MesProFrontlineFeedbackPayloadReqVO payload = reqVO.getFeedbackPayload();
        List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> readings =
                payload.getDeviceParameterReadings();
        if (readings == null || readings.isEmpty()) {
            return MesFrontlineParameterAuditResult.empty();
        }
        ParameterSource source = resolveSource(reqVO);
        Set<Long> selectedDeviceIds = payload.getSelectedDevices() == null ? Set.of()
                : payload.getSelectedDevices().stream()
                .filter(Objects::nonNull)
                .map(MesProFrontlineFeedbackPayloadReqVO.SelectedDeviceReqVO::getDeviceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Integer> readingKeyCounts = countReadingKeys(readings);
        Map<String, List<MesDeviceParameterSnapshotRule>> rulesByKey = indexRules(source.rules());

        List<MesFrontlineParameterAuditItem> auditItems = new ArrayList<>(readings.size());
        for (int index = 0; index < readings.size(); index++) {
            MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading = readings.get(index);
            String reasonCode = source.reasonCode();
            String normalizedCode = reading == null ? null
                    : MesDeviceParameterSnapshotCodec.normalizeCode(reading.getParameterCode());
            if (reasonCode == null) {
                reasonCode = resolveReadingReason(reading, normalizedCode, selectedDeviceIds,
                        readingKeyCounts, rulesByKey);
            }
            MesDeviceParameterSnapshotRule rule = reasonCode == null
                    ? rulesByKey.get(readingKey(reading.getDeviceId(), normalizedCode)).get(0) : null;
            if (rule != null) {
                applyServerStandard(reading, normalizedCode, rule);
            }
            auditItems.add(toAuditItem(index, reading, reasonCode, source.snapshotSource()));
        }
        int unresolvedCount = (int) auditItems.stream()
                .filter(item -> MesFrontlineParameterAuditResult.STATUS_UNRESOLVED.equals(
                        item.getResolutionStatus()))
                .count();
        return new MesFrontlineParameterAuditResult()
                .setParameterAuditStatus(unresolvedCount == 0
                        ? MesFrontlineParameterAuditResult.STATUS_RESOLVED
                        : MesFrontlineParameterAuditResult.STATUS_UNRESOLVED)
                .setTotalCount(auditItems.size())
                .setResolvedCount(auditItems.size() - unresolvedCount)
                .setUnresolvedCount(unresolvedCount)
                .setAuditItems(List.copyOf(auditItems));
    }

    private ParameterSource resolveSource(MesProFrontlineFeedbackSubmitReqVO reqVO) {
        MesProFrontlineFeedbackPayloadReqVO payload = reqVO.getFeedbackPayload();
        MesProFrontlineProcessPoolContextReqVO context = reqVO.getProcessPoolContext();
        if (payload.getWorkOrderId() == null && context.getWorkOrderId() == null) {
            List<MesProcessPoolDeviceParameterRuleDO> currentRules = parameterRuleMapper.selectList(
                    new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                            .eq(MesProcessPoolDeviceParameterRuleDO::getRouteProcessId, context.getRouteProcessId())
                            .eq(MesProcessPoolDeviceParameterRuleDO::getProcessId, context.getProcessId())
                            .eq(MesProcessPoolDeviceParameterRuleDO::getEnabled, Boolean.TRUE));
            return new ParameterSource(MesDeviceParameterSnapshotCodec.SOURCE_CURRENT_ROUTE_PROCESS_AT_SUBMIT,
                    toSnapshotRules(currentRules, context.getRouteProcessId(), context.getProcessId()), null);
        }
        if (payload.getActiveOrderProcessSnapshotId() == null) {
            return missingLegacySource();
        }
        MesProcessPoolActiveOrderProcessSnapshotDO snapshot = snapshotMapper.selectById(
                payload.getActiveOrderProcessSnapshotId());
        if (snapshot == null) {
            return missingLegacySource();
        }
        if (!snapshotContextMatches(snapshot, context, payload)) {
            return new ParameterSource(MesDeviceParameterSnapshotCodec.STATE_FROZEN, List.of(),
                    REASON_CONTEXT_MISMATCH);
        }
        if (!MesDeviceParameterSnapshotCodec.STATE_FROZEN.equals(snapshot.getParameterSnapshotState())) {
            return missingLegacySource();
        }
        String json = snapshot.getParameterSnapshotJson();
        String expectedHash = snapshot.getParameterSnapshotSha256();
        if (StrUtil.isBlank(json) || StrUtil.isBlank(expectedHash)
                || !Objects.equals(expectedHash, MesDeviceParameterSnapshotCodec.sha256(json))) {
            return new ParameterSource(MesDeviceParameterSnapshotCodec.STATE_FROZEN, List.of(),
                    REASON_SNAPSHOT_HASH_MISMATCH);
        }
        try {
            return new ParameterSource(MesDeviceParameterSnapshotCodec.STATE_FROZEN,
                    MesDeviceParameterSnapshotCodec.parse(json), null);
        } catch (RuntimeException ex) {
            return new ParameterSource(MesDeviceParameterSnapshotCodec.STATE_FROZEN, List.of(),
                    REASON_SNAPSHOT_HASH_MISMATCH);
        }
    }

    private static ParameterSource missingLegacySource() {
        return new ParameterSource(MesDeviceParameterSnapshotCodec.STATE_MISSING_LEGACY, List.of(),
                REASON_SNAPSHOT_MISSING_LEGACY);
    }

    private static boolean snapshotContextMatches(MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                                  MesProFrontlineProcessPoolContextReqVO context,
                                                  MesProFrontlineFeedbackPayloadReqVO payload) {
        return Objects.equals(snapshot.getActiveOrderId(), context.getActiveOrderId())
                && Objects.equals(snapshot.getWorkOrderId(), context.getWorkOrderId())
                && Objects.equals(snapshot.getWorkOrderId(), payload.getWorkOrderId())
                && Objects.equals(snapshot.getRouteId(), context.getRouteId())
                && Objects.equals(snapshot.getRouteId(), payload.getRouteId())
                && Objects.equals(snapshot.getRouteProcessId(), context.getRouteProcessId())
                && Objects.equals(snapshot.getProcessId(), context.getProcessId())
                && Objects.equals(snapshot.getProcessId(), payload.getProcessId());
    }

    private static String resolveReadingReason(
            MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading,
            String normalizedCode, Set<Long> selectedDeviceIds, Map<String, Integer> readingKeyCounts,
            Map<String, List<MesDeviceParameterSnapshotRule>> rulesByKey) {
        if (reading == null || reading.getDeviceId() == null) {
            return REASON_DEVICE_ID_MISSING;
        }
        if (normalizedCode == null) {
            return REASON_PARAMETER_CODE_MISSING;
        }
        if (selectedDeviceIds == null || selectedDeviceIds.isEmpty()) {
            return REASON_SELECTED_DEVICE_ID_MISSING;
        }
        if (!selectedDeviceIds.contains(reading.getDeviceId())) {
            return REASON_DEVICE_MISMATCH;
        }
        String key = readingKey(reading.getDeviceId(), normalizedCode);
        if (readingKeyCounts.getOrDefault(key, 0) > 1 || rulesByKey.getOrDefault(key, List.of()).size() > 1) {
            return REASON_DUPLICATE_PARAMETER;
        }
        if (!rulesByKey.containsKey(key)) {
            return REASON_RULE_NOT_FOUND;
        }
        return null;
    }

    private static void applyServerStandard(
            MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading,
            String normalizedCode, MesDeviceParameterSnapshotRule rule) {
        reading.setDeviceId(rule.getDeviceId())
                .setParameterCode(normalizedCode)
                .setParameterName(rule.getParameterName())
                .setUnit(rule.getUnit())
                .setLowerLimit(rule.getLowerLimit())
                .setUpperLimit(rule.getUpperLimit())
                .setParameterStatus(resolveParameterStatus(reading.getValue(), rule));
    }

    private static String resolveParameterStatus(BigDecimal value, MesDeviceParameterSnapshotRule rule) {
        if (value == null) {
            return null;
        }
        if (rule.getLowerLimit() != null && value.compareTo(rule.getLowerLimit()) < 0) {
            return "BELOW_LOWER";
        }
        if (rule.getUpperLimit() != null && value.compareTo(rule.getUpperLimit()) > 0) {
            return "ABOVE_UPPER";
        }
        return "NORMAL";
    }

    private static MesFrontlineParameterAuditItem toAuditItem(
            int readingIndex, MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading,
            String reasonCode, String snapshotSource) {
        return new MesFrontlineParameterAuditItem()
                .setReadingIndex(readingIndex)
                .setDeviceId(reading == null ? null : reading.getDeviceId())
                .setParameterCode(reading == null ? null : reading.getParameterCode())
                .setParameterName(reading == null ? null : reading.getParameterName())
                .setUnit(reading == null ? null : reading.getUnit())
                .setValue(reading == null ? null : reading.getValue())
                .setTextValue(reading == null ? null : reading.getTextValue())
                .setLowerLimit(reading == null ? null : reading.getLowerLimit())
                .setUpperLimit(reading == null ? null : reading.getUpperLimit())
                .setParameterStatus(reading == null ? null : reading.getParameterStatus())
                .setResolutionStatus(reasonCode == null
                        ? MesFrontlineParameterAuditResult.STATUS_RESOLVED
                        : MesFrontlineParameterAuditResult.STATUS_UNRESOLVED)
                .setReasonCode(reasonCode)
                .setSnapshotSource(snapshotSource);
    }

    private static Map<String, Integer> countReadingKeys(
            List<MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO> readings) {
        Map<String, Integer> counts = new HashMap<>();
        for (MesProFrontlineFeedbackPayloadReqVO.DeviceParameterReadingReqVO reading : readings) {
            if (reading == null || reading.getDeviceId() == null) {
                continue;
            }
            String code = MesDeviceParameterSnapshotCodec.normalizeCode(reading.getParameterCode());
            if (code != null) {
                counts.merge(readingKey(reading.getDeviceId(), code), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Map<String, List<MesDeviceParameterSnapshotRule>> indexRules(
            List<MesDeviceParameterSnapshotRule> rules) {
        Map<String, List<MesDeviceParameterSnapshotRule>> byKey = new LinkedHashMap<>();
        for (MesDeviceParameterSnapshotRule rule : rules) {
            if (rule == null || rule.getDeviceId() == null) {
                continue;
            }
            String code = MesDeviceParameterSnapshotCodec.normalizeCode(rule.getParameterCode());
            if (code != null) {
                byKey.computeIfAbsent(readingKey(rule.getDeviceId(), code), ignored -> new ArrayList<>())
                        .add(rule);
            }
        }
        return byKey;
    }

    private static List<MesDeviceParameterSnapshotRule> toSnapshotRules(
            List<MesProcessPoolDeviceParameterRuleDO> rules, Long routeProcessId, Long processId) {
        if (rules == null) {
            return List.of();
        }
        return rules.stream()
                .filter(Objects::nonNull)
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> Objects.equals(routeProcessId, rule.getRouteProcessId()))
                .filter(rule -> Objects.equals(processId, rule.getProcessId()))
                .map(rule -> MesDeviceParameterSnapshotRule.builder()
                        .routeProcessId(routeProcessId)
                        .processId(processId)
                        .deviceId(rule.getDeviceId())
                        .parameterCode(MesDeviceParameterSnapshotCodec.normalizeCode(rule.getParameterCode()))
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
                        .build())
                .toList();
    }

    private static String readingKey(Long deviceId, String normalizedCode) {
        return deviceId + "|" + normalizedCode;
    }

    private record ParameterSource(String snapshotSource, List<MesDeviceParameterSnapshotRule> rules,
                                   String reasonCode) {
    }
}
