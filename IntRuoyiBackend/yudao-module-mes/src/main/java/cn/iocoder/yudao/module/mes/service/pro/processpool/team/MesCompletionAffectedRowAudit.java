package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Lossless row-state fragments, committed atomically with the completion receipt and parent audit. */
final class MesCompletionAffectedRowAudit {
    // Leaves ample room for the kernel's canonical-event copy and Connector/J SQL escaping.
    private static final int ROW_BYTES_PER_PART = 512 * 1024;
    private MesCompletionAffectedRowAudit() { }

    static Map<String, Object> append(GxpAuditService audit,
                                     MesProcessPoolActiveOrderCompletionReceiptDO receipt,
                                     String beforeJson, String afterJson) {
        var before = tables(beforeJson);
        var after = tables(afterJson);
        List<String> tableNames = new ArrayList<>(before.keySet());
        after.keySet().forEach(table -> { if (!tableNames.contains(table)) tableNames.add(table); });
        List<Part> parts = new ArrayList<>();
        Part current = new Part(tableNames);
        for (boolean old : List.of(true, false)) {
            var state = old ? before : after;
            for (String table : tableNames) {
                for (JsonNode row : state.getOrDefault(table, List.of())) {
                    int bytes = row.toString().getBytes(StandardCharsets.UTF_8).length + 1;
                    if (bytes > ROW_BYTES_PER_PART) {
                        throw new IllegalStateException("Completion affected-row audit element exceeds "
                                + ROW_BYTES_PER_PART + " UTF-8 bytes: " + table);
                    }
                    if (current.bytes + bytes > ROW_BYTES_PER_PART) {
                        parts.add(current);
                        current = new Part(tableNames);
                    }
                    (old ? current.before : current.after).get(table).add(row);
                    current.bytes += bytes;
                }
            }
        }
        parts.add(current); // Empty states also use the same explicit fragment contract.
        List<Map<String, Object>> manifestParts = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            GxpAuditAppendResult result = audit.append(GxpAuditCommand.builder()
                    .eventSchemaVersion(2).operationId("mes.active-order.complete")
                    .subjectId("ACTIVE_ORDER:" + receipt.getActiveOrderId())
                    .subjectVersion(String.valueOf(receipt.getCompletedVersion()))
                    .reason("活跃订单完工受影响明细完整分段记录")
                    .reasonCode("MES_ACTIVE_ORDER_COMPLETE").reasonSource("SYSTEM")
                    .beforeState(envelope(receipt, i, part.before, true))
                    .afterState(envelope(receipt, i, part.after, false))
                    .idempotencyKey("ACTIVE_ORDER_COMPLETE:" + receipt.getId() + ":ROWS:" + i)
                    .requestId("MES-ACTIVE-ORDER-COMPLETE:" + receipt.getId() + ":ROWS:" + i)
                    .resultStatus("SUCCESS").sourceType("SERVICE_METHOD")
                    .sourceLocator(MesCompletionAffectedRowAudit.class.getName() + "#append")
                    .links(List.of(new GxpAuditRelation("SUBJECT", "ACTIVE_ORDER",
                                    String.valueOf(receipt.getActiveOrderId()), String.valueOf(receipt.getCompletedVersion()), null),
                            new GxpAuditRelation("SOURCE", "COMPLETION_RECEIPT", String.valueOf(receipt.getId()),
                                    String.valueOf(receipt.getCompletedVersion()), receipt.getReceiptHash())))
                    .evidences(List.of(new GxpAuditEvidence("FORMAL_COMPLETION_RECEIPT", String.valueOf(receipt.getId()),
                            String.valueOf(receipt.getCompletedVersion()), receipt.getReceiptHash(), "COMPLETION")))
                    .build());
            if (result == null || result.eventId() == null || result.ledgerSequence() == null
                    || result.eventHash() == null || result.eventHash().isBlank()) {
                throw new IllegalStateException("Completion affected-row audit did not return a persisted event binding");
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("partIndex", i); item.put("eventId", result.eventId());
            item.put("ledgerSequence", result.ledgerSequence()); item.put("eventHash", result.eventHash());
            item.put("beforeRowCount", count(part.before)); item.put("afterRowCount", count(part.after));
            manifestParts.add(item);
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", "COMPLETION_AFFECTED_ROWS_V1");
        manifest.put("canonicalization", "MES_COMPLETION_SOURCE_V1");
        manifest.put("beforeHash", DigestUtil.sha256Hex(
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(beforeJson)));
        manifest.put("afterHash", DigestUtil.sha256Hex(
                MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(afterJson)));
        manifest.put("tables", tableNames);
        manifest.put("beforeRowCount", count(before)); manifest.put("afterRowCount", count(after));
        manifest.put("parts", manifestParts);
        return manifest;
    }

    private static GxpAuditStateEnvelope envelope(MesProcessPoolActiveOrderCompletionReceiptDO receipt, int index,
                                                   Map<String, List<JsonNode>> rows, boolean before) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("schema", "COMPLETION_AFFECTED_ROWS_V1"); state.put("partIndex", index);
        state.put("completionReceiptId", receipt.getId()); state.put("completionReceiptHash", receipt.getReceiptHash());
        state.put("affectedRows", rows);
        return GxpAuditStateEnvelope.builder().state(before ? "ACTIVE" : "COMPLETED")
                .objectVersion(String.valueOf(before ? receipt.getExpectedVersion() : receipt.getCompletedVersion()))
                .canonicalJson(JsonUtils.toJsonString(state)).build();
    }

    private static Map<String, List<JsonNode>> tables(String json) {
        JsonNode node = MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.parseExact(json);
        if (!node.isObject()) throw new IllegalStateException("Completion affected-row state must be an object");
        Map<String, List<JsonNode>> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(table -> {
            if (!table.getValue().isArray()) throw new IllegalStateException("Completion affected table must contain rows: " + table.getKey());
            List<JsonNode> rows = new ArrayList<>();
            table.getValue().forEach(row -> {
                if (!row.isObject()) throw new IllegalStateException("Completion affected row must be an object: " + table.getKey());
                rows.add(row);
            });
            result.put(table.getKey(), rows);
        });
        return result;
    }
    private static long count(Map<String, ? extends List<?>> tables) { return tables.values().stream().mapToLong(List::size).sum(); }
    private static final class Part {
        private final Map<String, List<JsonNode>> before = new LinkedHashMap<>();
        private final Map<String, List<JsonNode>> after = new LinkedHashMap<>();
        private int bytes;
        private Part(List<String> tables) { tables.forEach(table -> { before.put(table, new ArrayList<>()); after.put(table, new ArrayList<>()); }); }
    }
}
