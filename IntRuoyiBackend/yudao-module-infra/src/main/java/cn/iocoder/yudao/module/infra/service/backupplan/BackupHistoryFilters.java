package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.time.format.DateTimeFormatter;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** NAS manifests are the authoritative source; filter them once before the response page is sliced. */
final class BackupHistoryFilters {
    private BackupHistoryFilters() {}
    private static final Map<String, Function<RuntimeControlBackupPointRespVO,String>> FIELDS = Map.of(
        "backupId", row -> row.getBackupId(),
        "generatedTime", BackupHistoryFilters::generatedTime,
        "backupMode", BackupHistoryFilters::backupType,
        "backupChain", BackupHistoryFilters::chain,
        "result", row -> recoverable(row) ? "成功" : "需检查",
        "recoverabilityStatus", row -> recoverable(row) ? "可恢复" : "不可恢复",
        "manifestPath", row -> row.getBackupId() == null || row.getBackupId().isEmpty() ? "备份包" : "备份包：" + row.getBackupId()
    );
    static List<RuntimeControlBackupPointRespVO> filter(List<RuntimeControlBackupPointRespVO> rows, String json) {
        if (json == null || json.isBlank()) return rows;
        JsonNode conditions;
        try { conditions = new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json); }
        catch (JsonProcessingException error) { throw invalidParamException("备份历史筛选条件必须是有效 JSON 数组"); }
        if (conditions == null || !conditions.isArray()) throw invalidParamException("备份历史筛选条件必须是数组");
        for (JsonNode condition : conditions) {
            if (!condition.isObject() || !FIELDS.containsKey(condition.path("key").asText()) || !condition.has("value"))
                throw invalidParamException("备份历史筛选字段无效");
            String operator = condition.path("operator").asText();
            if (!List.of("contains", "eq", "in").contains(operator)) throw invalidParamException("备份历史文本列不支持该运算符");
            if ("in".equals(operator) && !condition.get("value").isArray()) throw invalidParamException("备份历史多选条件必须是数组");
        }
        return rows.stream().filter(row -> {
            for (JsonNode condition : conditions) {
                String actual = normalize(FIELDS.get(condition.get("key").asText()).apply(row));
                JsonNode expected = condition.get("value");
                boolean matches = switch (condition.get("operator").asText()) {
                    case "contains" -> actual.contains(normalize(expected.asText()));
                    case "eq" -> actual.equals(normalize(expected.asText()));
                    case "in" -> { boolean found = false; for (JsonNode value : expected) if (actual.equals(normalize(value.asText()))) found=true; yield found; }
                    default -> throw invalidParamException("备份历史筛选运算符无效");
                };
                if (!matches) return false;
            }
            return true;
        }).toList();
    }
    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static boolean recoverable(RuntimeControlBackupPointRespVO row) { return "RECOVERABLE".equals(row.getRecoverabilityStatus()); }
    private static String generatedTime(RuntimeControlBackupPointRespVO row) {
        var match = Pattern.compile("(20\\d{2})[-_]?(\\d{2})[-_]?(\\d{2})[-_T]?(\\d{2})[-_]?(\\d{2})[-_]?(\\d{2})?").matcher(row.getBackupId() == null ? "" : row.getBackupId());
        if (match.find()) return match.group(1)+"-"+match.group(2)+"-"+match.group(3)+" "+match.group(4)+":"+match.group(5)+":"+(match.group(6)==null ? "00" : match.group(6));
        return row.getLastVerifiedAt() == null ? "--" : row.getLastVerifiedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    private static String backupType(RuntimeControlBackupPointRespVO row) {
        if ("FULL".equals(row.getBackupKind())) return "全量备份";
        if ("INCREMENTAL".equals(row.getBackupKind())) return "增量备份";
        if ("full".equals(row.getBackupMode()) || "baseline".equals(row.getDccBackupMode())) return "完整备份";
        if ("incremental".equals(row.getBackupMode()) || "incremental".equals(row.getDccBackupMode())) return "增量备份";
        return row.getBackupMode() != null && !row.getBackupMode().isEmpty() ? row.getBackupMode() : row.getDccBackupMode() != null && !row.getDccBackupMode().isEmpty() ? row.getDccBackupMode() : "备份包";
    }
    private static String chain(RuntimeControlBackupPointRespVO row) {
        if ("FULL".equals(row.getBackupKind())) return "基线 " + row.getBackupId();
        if ("INCREMENTAL".equals(row.getBackupKind())) return (row.getBaseBackupId() == null || row.getBaseBackupId().isEmpty() ? "基线缺失" : row.getBaseBackupId()) + " -> " + row.getBackupId() + "（直接父点 " + (row.getParentBackupId() == null || row.getParentBackupId().isEmpty() ? "父点缺失" : row.getParentBackupId()) + "）";
        return "链身份缺失";
    }
}
