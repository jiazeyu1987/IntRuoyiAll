package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DccDirectoryAccessRuleCanonicalHash {

    private DccDirectoryAccessRuleCanonicalHash() {
    }

    static String directoryRulesHash(List<DccDirectoryAccessRuleDO> rules) {
        if (rules == null) {
            throw new IllegalStateException("directory access rules required");
        }
        List<Map<String, Object>> canonicalRules = rules.stream()
                .sorted(ruleComparator())
                .map(DccDirectoryAccessRuleCanonicalHash::rulePayload)
                .toList();
        return "sha256:" + sha256Hex(JsonUtils.toJsonString(canonicalRules));
    }

    static Map<String, Object> rulePayload(DccDirectoryAccessRuleDO rule) {
        if (rule == null) {
            throw new IllegalStateException("directory access rule required");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("directoryId", rule.getDirectoryId());
        payload.put("subjectType", rule.getSubjectType());
        payload.put("subjectId", rule.getSubjectId());
        payload.put("canQuery", rule.getCanQuery());
        payload.put("canPreview", rule.getCanPreview());
        payload.put("canDownload", rule.getCanDownload());
        payload.put("active", rule.getActive());
        payload.put("changeReason", rule.getChangeReason());
        return payload;
    }

    static Comparator<DccDirectoryAccessRuleDO> ruleComparator() {
        return Comparator.comparing(DccDirectoryAccessRuleDO::getDirectoryId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getSubjectType, Comparator.nullsLast(String::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getSubjectId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getCanQuery, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getCanPreview, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getCanDownload, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getActive, Comparator.nullsLast(Boolean::compareTo))
                .thenComparing(DccDirectoryAccessRuleDO::getChangeReason, Comparator.nullsLast(String::compareTo));
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
