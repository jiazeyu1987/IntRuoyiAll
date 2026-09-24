package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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
                || !normalizeCompletions(liveSource.get("completions"), receipt.getBatchRecordId())
                || !normalizeCompletions(frozenSignatures.get("productionCompletionSignatures"), null)
                || !normalizeCompletions(liveSignatures.get("productionCompletionSignatures"), receipt.getBatchRecordId())) {
            return false;
        }
        return frozenSource.equals(liveSource) && frozenSignatures.equals(liveSignatures)
                && lossFacts(receipt.getLossConditionFactsJson()).equals(lossFacts(current.getLossConditionFactsJson()));
    }

    private static JsonNode requiredObject(String json) {
        if (json == null || json.isBlank()) throw new IllegalStateException("COMPLETION_SOURCE_EVIDENCE_MISSING");
        JsonNode node = JsonUtils.parseTree(json);
        if (node == null || !node.isObject()) throw new IllegalStateException("COMPLETION_SOURCE_EVIDENCE_INVALID");
        return node;
    }

    private static boolean normalizeCompletions(JsonNode rows, Long expectedBackfillId) {
        if (rows == null || !rows.isArray() || rows.isEmpty()) return false;
        for (JsonNode row : rows) {
            if (!(row instanceof ObjectNode object)) return false;
            // Exclude only verified writeback outputs. A missing/changed output binding remains a conflict.
            if (expectedBackfillId != null && (!"SUCCESS".equals(row.path("backfillStatus").asText())
                    || !row.path("backfillExecutionId").isIntegralNumber()
                    || row.path("backfillExecutionId").asLong() != expectedBackfillId
                    || (!row.path("backfillError").isMissingNode() && !row.path("backfillError").isNull()))) {
                return false;
            }
            object.remove(COMPLETION_OUTPUT_FIELDS);
        }
        return true;
    }

    private static JsonNode lossFacts(String json) {
        if (json == null || json.isBlank()) throw new IllegalStateException("COMPLETION_LOSS_EVIDENCE_MISSING");
        JsonNode facts = JsonUtils.parseTree(json);
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
