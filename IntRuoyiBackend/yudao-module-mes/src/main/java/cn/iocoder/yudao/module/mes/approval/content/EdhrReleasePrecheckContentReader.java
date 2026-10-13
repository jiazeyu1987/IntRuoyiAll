package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseCheckItemMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

@Component
public class EdhrReleasePrecheckContentReader {
    private final MesProEdhrReleaseCheckItemMapper mapper;
    public EdhrReleasePrecheckContentReader(MesProEdhrReleaseCheckItemMapper mapper) { this.mapper = mapper; }
    public ObjectNode read(MesProEdhrReleaseTransactionDO release, boolean locked) {
        var raw = ApprovalContentJson.parse(release.getPrecheckSnapshotJson());
        Set<String> allowed = Set.of("batchExecutionId", "releaseStatus", "checkedAt", "materialGateManifestHash", "items", "batchExecutionCode", "workOrderCode", "batchCode", "productCode");
        raw.fieldNames().forEachRemaining(key -> { if (!allowed.contains(key)) fail("unknown precheck key " + key); });
        for (String key : Set.of("batchExecutionId", "releaseStatus", "checkedAt", "materialGateManifestHash", "items")) if (!raw.has(key)) fail("missing precheck key " + key);
        if (!raw.path("batchExecutionId").isIntegralNumber() || !raw.path("batchExecutionId").canConvertToLong() || !Objects.equals(raw.path("batchExecutionId").longValue(), release.getBatchExecutionId())) fail("precheck batch");
        String time = raw.path("checkedAt").asText(); if (!time.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}")) fail("precheck time format");
        LocalDateTime checked;
        try { checked = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)); }
        catch (java.time.DateTimeException e) { throw new IllegalArgumentException("EDHR_PRECHECK_CALENDAR_INVALID", e); }
        if (checked.getYear() < 1 || !checked.equals(release.getLastPrecheckAt())) fail("lastPrecheckAt mismatch");
        var rows = mapper.selectCurrentOpen(release.getId(), locked); if (rows.size() != 10 || raw.path("items").size() != 10) fail("ten current checks required");
        long failed=rows.stream().filter(r->"FAIL".equals(r.getCheckResult()) || "BLOCKER".equals(r.getCheckResult()) || "BLOCKER".equals(r.getSeverity())).count();
        long blocking=rows.stream().filter(r->"BLOCKER".equals(r.getCheckResult()) || "BLOCKER".equals(r.getSeverity())).count();
        if(!Objects.equals(release.getRequiredCheckCount(),10) || !Objects.equals(release.getFailedCheckCount(),(int)failed) || !Objects.equals(release.getBlockingCheckCount(),(int)blocking)) fail("formal check count mismatch");
        var snapshots = new HashMap<String, com.fasterxml.jackson.databind.JsonNode>();
        for (var item : raw.path("items")) {
            Set<String> keys = new HashSet<>(); item.fieldNames().forEachRemaining(keys::add);
            if (!keys.equals(Set.of("checkCode", "checkResult", "severity", "responsibilityModule", "sourceObjectCode"))) fail("precheck item shape");
            for (var value : item) if (!value.isTextual()) fail("precheck item type");
            if (snapshots.put(item.path("checkCode").textValue(), item) != null) fail("duplicate precheck code");
        }
        var result = JsonNodeFactory.instance.objectNode(); var items = result.putArray("items");
        rows.stream().sorted(Comparator.comparing(r -> r.getCheckCode())).forEach(row -> {
            var saved = snapshots.remove(row.getCheckCode());
            if (saved == null || !Objects.equals(checked, row.getCheckedAt()) || !Objects.equals(row.getReleaseTransactionId(), release.getId())
                    || !Objects.equals(saved.path("checkResult").textValue(), row.getCheckResult()) || !Objects.equals(saved.path("severity").textValue(), row.getSeverity())
                    || !Objects.equals(saved.path("responsibilityModule").textValue(), row.getResponsibilityModule()) || !Objects.equals(saved.path("sourceObjectCode").textValue(), row.getSourceObjectCode())) fail("check row snapshot mismatch");
            var item = items.addObject(); item.put("code", row.getCheckCode()); item.put("result", row.getCheckResult()); var ref = item.putArray("evidenceRefs").addObject();
            ref.put("checkItemId", requiredId(row.getId())); ref.put("releaseTransactionId", requiredId(row.getReleaseTransactionId())); ref.put("sourceObjectType", row.getSourceObjectType()); ref.put("sourceObjectId", row.getSourceObjectId()); ref.put("evidenceHash", row.getEvidenceHash());
        });
        if (!snapshots.isEmpty()) fail("unmatched precheck items"); return result;
    }
    private static String requiredId(Long value) { if (value == null || value <= 0) fail("check ID missing"); return value.toString(); }
    private static void fail(String reason) { throw new IllegalArgumentException("EDHR_PRECHECK_CONTENT_INVALID: " + reason); }
}
