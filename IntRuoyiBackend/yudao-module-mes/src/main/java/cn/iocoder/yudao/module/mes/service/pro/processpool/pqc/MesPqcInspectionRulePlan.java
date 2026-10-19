package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Task generation and release coverage use the same frozen QA stage rules. */
public final class MesPqcInspectionRulePlan {
    private static final List<String> KEYS = List.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL");
    private static final Map<String, String> TYPES = Map.of(
            "FIRST", "FIRST", "PATROL_AM", "PATROL", "PATROL_PM", "PATROL", "FINAL", "FINAL");
    private static final Map<String, String> SHIFTS = Map.of(
            "FIRST", "FIRST", "PATROL_AM", "AM", "PATROL_PM", "PM", "FINAL", "FINAL");
    private MesPqcInspectionRulePlan() {}

    public static List<Stage> require(MesQaInspectionRegulationVersionDO version) {
        if (version == null || version.getInspectionTypeRulesJson() == null
                || version.getInspectionTypeRulesJson().isBlank()) {
            throw new IllegalArgumentException("QA规程缺少检验类型规则");
        }
        JSONArray input;
        try {
            input = JSON.parseArray(version.getInspectionTypeRulesJson());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("QA规程检验类型规则无效", ex);
        }
        if (input == null || input.size() != KEYS.size()) {
            throw new IllegalArgumentException("QA规程检验类型规则无效");
        }
        Map<String, Stage> stages = new java.util.LinkedHashMap<>();
        for (int i = 0; i < input.size(); i++) {
            JSONObject rule = input.getJSONObject(i);
            String key = rule == null ? null : trim(rule.getString("key"));
            String type = rule == null ? null : normalizeType(rule.getString("inspectionType"));
            Boolean required = rule == null ? null : rule.getBoolean("required");
            if (key == null || !KEYS.contains(key) || required == null
                    || !Objects.equals(TYPES.get(key), type)
                    || stages.putIfAbsent(key, new Stage(key, type, SHIFTS.get(key), required)) != null) {
                throw new IllegalArgumentException("QA规程检验类型规则无效");
            }
        }
        if (version.getFinalInspectionApplicable() == null) {
            throw new IllegalArgumentException("QA规程末检适用性未配置");
        }
        if (Boolean.TRUE.equals(version.getFinalInspectionApplicable()) && !stages.get("FINAL").required()) {
            throw new IllegalArgumentException("QA规程末检适用但 FINAL 检验规则未启用");
        }
        if (Boolean.FALSE.equals(version.getFinalInspectionApplicable())
                && (version.getFinalInspectionNotApplicableReason() == null
                || version.getFinalInspectionNotApplicableReason().isBlank() || stages.get("FINAL").required())) {
            throw new IllegalArgumentException("QA规程末检不适用配置无效");
        }
        List<Stage> ordered = new ArrayList<>();
        KEYS.forEach(key -> ordered.add(stages.get(key)));
        return List.copyOf(ordered);
    }

    public static String normalizeType(String value) {
        String type = trim(value);
        return type != null && type.startsWith("PATROL") ? "PATROL" : type;
    }
    private static String trim(String value) { return value == null ? null : value.trim(); }
    public record Stage(String ruleKey, String inspectionType, String shiftCode, boolean required) {}
}
