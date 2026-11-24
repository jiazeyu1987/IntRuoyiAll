package cn.iocoder.yudao.module.erp.service.purchase.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Discover headers by immutable ID; read each document completely and reject concurrent edits. */
final class ErpKingdeeOrderWindowReader {
    private ErpKingdeeOrderWindowReader() {}

    static ArrayNode read(LocalDateTime start, LocalDateTime end, int limit, String formId,
                          String baseFilter, String fields, String entryField, int entryIndex, int modifyIndex,
                          Function<Map<String, Object>, JsonNode> execute) {
        Map<String, Object> window = ErpKingdeeIncrementalQuerySpec.builder().formId(formId)
                .fieldKeys("FID,FModifyDate").baseFilter(baseFilter).limit(limit).build().toQuery(start, end);
        return readAll(window, limit, formId, baseFilter, fields, entryField, entryIndex, modifyIndex, execute);
    }

    static ArrayNode readAll(Map<String, Object> window, int limit, String formId,
                            String baseFilter, String fields, String entryField, int entryIndex, int modifyIndex,
                            Function<Map<String, Object>, JsonNode> execute) {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        long lastId = 0;
        while (true) {
            Map<String, Object> discovery = new LinkedHashMap<>(window);
            discovery.put("FilterString", "(" + window.get("FilterString") + ") and FID > " + lastId);
            discovery.put("OrderString", "FID ASC");
            // One header at a time also works if the server repeats header projections per entry.
            discovery.put("Limit", 1);
            JsonNode headers = array(execute.apply(discovery));
            if (headers.isEmpty()) break;
            for (JsonNode header : headers) {
                if (!header.isArray() || header.size() != 2 || !header.path(0).isValueNode()) {
                    throw new IllegalStateException("Kingdee invalid header response: " + header);
                }
                long id = positiveId(header.path(0));
                if (id <= lastId) throw new IllegalStateException("Kingdee header cursor did not advance");
                String version = requiredVersion(header.path(1));
                String filter = "(" + baseFilter + ") and FID = " + id;
                long lastEntry = 0;
                int count = 0;
                while (true) {
                    Map<String, Object> detail = query(formId, fields, filter + " and " + entryField + " > " + lastEntry,
                            entryField + " ASC", limit);
                    JsonNode rows = array(execute.apply(detail));
                    if (rows.isEmpty()) break;
                    for (JsonNode row : rows) {
                        if (!row.isArray() || row.size() <= modifyIndex || positiveId(row.path(0)) != id
                                || !version.equals(requiredVersion(row.path(modifyIndex)))) {
                            throw new IllegalStateException("Kingdee document changed while reading: " + id);
                        }
                        long entry = positiveId(row.path(entryIndex));
                        if (entry <= lastEntry) throw new IllegalStateException("Kingdee entry cursor did not advance");
                        lastEntry = entry;
                        result.add(row);
                        count++;
                    }
                    if (rows.size() < limit) break;
                }
                JsonNode check = array(execute.apply(query(formId, "FID,FModifyDate", filter, "FID ASC", 1)));
                if (count == 0 || check.size() != 1 || !version.equals(requiredVersion(check.get(0).path(1)))) {
                    throw new IllegalStateException("Kingdee document changed or disappeared while reading: " + id);
                }
                lastId = id;
            }
        }
        return result;
    }

    private static Map<String, Object> query(String form, String fields, String filter, String order, int limit) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("FormId", form);
        query.put("FieldKeys", fields);
        query.put("FilterString", filter);
        query.put("OrderString", order);
        query.put("StartRow", 0);
        query.put("Limit", limit);
        return query;
    }

    private static JsonNode array(JsonNode value) {
        if (value == null || !value.isArray()) throw new IllegalStateException("Kingdee query is not an array");
        return value;
    }

    private static String requiredVersion(JsonNode value) {
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalStateException("Kingdee FModifyDate must be nonblank text");
        }
        return value.asText();
    }

    private static long positiveId(JsonNode value) {
        long id = Long.parseLong(value.asText());
        if (id <= 0) throw new IllegalStateException("Kingdee source ID must be positive");
        return id;
    }
}
