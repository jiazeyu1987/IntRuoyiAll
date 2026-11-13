package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import java.util.Map;
import java.util.Objects;

/** Historical copied replenishments are not evidence that the fixed test order needed replenishment. */
final class MesFixedTestOrderReplenishmentPolicy {
    private MesFixedTestOrderReplenishmentPolicy() { }

    static boolean isHistoricalCopy(Map<String, Object> row) {
        String raw = Objects.toString(row.get("raw_payload"), "");
        if (!raw.contains("simulatedCopyFrom")) return false;
        var payload = JsonUtils.parseTree(raw);
        return payload != null && payload.path("simulatedCopyFrom").isTextual()
                && !payload.path("simulatedCopyFrom").asText().isBlank()
                && MesFixedTestOrderInitializer.WORK_ORDER_CODE.equals(
                        payload.path("targetProductionOrderNo").asText())
                && Objects.toString(row.get("source_bill_no"), "").startsWith(
                        "SIM-COPY-" + payload.path("simulatedCopyFrom").asText() + "-");
    }
}
