package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Two fixed fixtures only; callers cannot select an arbitrary source order or tenant. */
enum MesFixedTestOrderDataSet {
    FIRST, SECOND;

    String workOrderCode() {
        return MesFixedTestOrderInitializer.WORK_ORDER_CODE + (this == SECOND ? "-2" : "");
    }

    Map<String, Object> workOrderOverrides() {
        if (this == FIRST) return Map.of();
        return Map.of("code", workOrderCode(), "batch_code", identity("batch:" + workOrderCode()));
    }

    Map<String, Object> document(Map<String, Object> source) {
        if (this == FIRST) return source;
        Map<String, Object> result = new LinkedHashMap<>(source);
        for (String field : List.of("source_fid", "source_bill_no", "source_entry_id", "source_line_key",
                "production_material_list_no")) {
            Object value = source.get(field);
            if (value != null && !value.toString().isBlank()) result.put(field, identity(value.toString()));
        }
        for (String field : List.of("production_order_no", "work_order_code")) {
            if (source.containsKey(field)) result.put(field, workOrderCode());
        }
        return result;
    }

    String snapshot(Map<String, Object> values, Map<String, Object> source) {
        Map<String, Object> document = new LinkedHashMap<>(values);
        document.remove("raw_payload");
        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("tenantId", MesFixedTestOrderReferenceGraph.SOURCE_TENANT);
        reference.put("recordId", source.get("id"));
        reference.put("rawPayload", source.get("raw_payload"));
        return JsonUtils.toJsonString(Map.of("testDataSet", 2, "workOrderCode", workOrderCode(),
                "document", document, "reference", reference));
    }

    private String identity(String source) {
        // Stable, within the 64-character ERP identity columns, and disjoint from the first fixture.
        return "FT2-" + UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }
}
