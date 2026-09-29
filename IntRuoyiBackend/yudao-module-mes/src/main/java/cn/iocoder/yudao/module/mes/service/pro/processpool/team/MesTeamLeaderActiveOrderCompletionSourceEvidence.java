package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;

/** Compare frozen inputs with live inputs without treating Tx-A's own outputs as new production evidence. */
final class MesTeamLeaderActiveOrderCompletionSourceEvidence {
    private static final List<String> COMPLETION_OUTPUT_FIELDS = List.of(
            "backfillStatus", "backfillExecutionId", "backfillError", "updateTime", "updater");

    private MesTeamLeaderActiveOrderCompletionSourceEvidence() {}

    static boolean matches(MesProcessPoolActiveOrderCompletionReceiptDO receipt,
                           MesTeamLeaderActiveOrderCompletionBackfillDraft current) {
        if (receipt == null || current == null || receipt.getBatchRecordId() == null
                || receipt.getReceiptHash() == null
                || !Objects.equals(receipt.getReceiptHash(), MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt))) {
            return false;
        }
        JsonNode frozenSource = requiredObject(receipt.getFormalSourceSnapshotJson());
        JsonNode liveSource = requiredObject(current.getFormalSourceSnapshotJson());
        JsonNode frozenSignatures = requiredObject(receipt.getSignatureSnapshotJson());
        JsonNode liveSignatures = requiredObject(current.getSignatureSnapshotJson());
        if (!normalizeCompletions(frozenSource.get("completions"), null)
                // Source input facts exclude writeback outputs; validate their signature snapshot bindings below.
                || !normalizeCompletions(liveSource.get("completions"), null)
                || !normalizeCompletions(frozenSignatures.get("productionCompletionSignatures"), null)
                || !normalizeCompletions(liveSignatures.get("productionCompletionSignatures"), receipt.getBatchRecordId())) {
            return false;
        }
        return canonicalJson(frozenSource).equals(canonicalJson(liveSource))
                && canonicalJson(frozenSignatures).equals(canonicalJson(liveSignatures))
                && canonicalJson(lossFacts(receipt.getLossConditionFactsJson()))
                .equals(canonicalJson(lossFacts(current.getLossConditionFactsJson())));
    }

    private static JsonNode requiredObject(String json) {
        if (json == null || json.isBlank()) throw new IllegalStateException("COMPLETION_SOURCE_EVIDENCE_MISSING");
        JsonNode node = MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.parseExact(json);
        if (node == null || !node.isObject()) throw new IllegalStateException("COMPLETION_SOURCE_EVIDENCE_INVALID");
        return node;
    }

    private static boolean normalizeCompletions(JsonNode rows, Long expectedBackfillId) {
        if (rows == null || !rows.isArray() || rows.isEmpty()) return false;
        for (JsonNode row : rows) {
            if (!(row instanceof ObjectNode object)) return false;
            // Exclude only verified writeback outputs. A missing/changed output binding remains a conflict.
            if (expectedBackfillId != null && (!"SUCCESS".equals(row.path("backfillStatus").asText())
                    || !matchesExpectedBackfillId(row.get("backfillExecutionId"), expectedBackfillId)
                    || (!row.path("backfillError").isMissingNode() && !row.path("backfillError").isNull()))) {
                return false;
            }
            object.remove(COMPLETION_OUTPUT_FIELDS);
        }
        return true;
    }

    private static boolean matchesExpectedBackfillId(JsonNode value, Long expected) {
        if (value == null || expected == null) return false;
        if (value.isIntegralNumber()) {
            return value.canConvertToLong() && value.longValue() == expected;
        }
        return value.isTextual() && Long.toString(expected).equals(value.textValue());
    }

    private static String canonicalJson(JsonNode node) {
        return MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(node.toString());
    }

    private static JsonNode lossFacts(String json) {
        if (json == null || json.isBlank()) throw new IllegalStateException("COMPLETION_LOSS_EVIDENCE_MISSING");
        JsonNode facts = MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.parseExact(json);
        if (facts == null || !facts.isArray() || facts.isEmpty()) {
            throw new IllegalStateException("COMPLETION_LOSS_EVIDENCE_INVALID");
        }
        for (JsonNode fact : facts) {
            if (!(fact instanceof ObjectNode object)) throw new IllegalStateException("COMPLETION_LOSS_EVIDENCE_INVALID");
            JsonNode confirmation = object.get("zeroLossConfirmationSnapshot");
            if (confirmation != null && confirmation.isTextual()) {
                object.set("zeroLossConfirmationSnapshot", requiredObject(confirmation.textValue()));
            }
        }
        return facts;
    }
}
