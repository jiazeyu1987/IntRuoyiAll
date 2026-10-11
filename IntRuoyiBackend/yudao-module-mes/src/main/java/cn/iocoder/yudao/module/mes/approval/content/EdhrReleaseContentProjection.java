package cn.iocoder.yudao.module.mes.approval.content;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

/** Explicit whitelist builder. No current-directory enrichment or inferred identities. */
final class EdhrReleaseContentProjection {
    private EdhrReleaseContentProjection() { }
    static ObjectNode object(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("CONTENT_PAIRS_INVALID");
        var result = JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i]; Object value = pairs[i + 1];
            if (value == null) result.putNull(key);
            else if (value instanceof com.fasterxml.jackson.databind.JsonNode node) result.set(key, node);
            else if (value instanceof Long id) result.put(key, id.toString());
            else if (value instanceof BigDecimal decimal) result.put(key, decimal.signum() == 0 ? "0" : decimal.stripTrailingZeros().toPlainString());
            else if (value instanceof LocalDateTime date) { if (date.getNano() != 0) throw new IllegalArgumentException("CONTENT_TIME_NOT_SECONDS"); result.put(key, date.format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")) + "+08:00"); }
            else if (value instanceof Boolean flag) result.put(key, flag);
            else if (value instanceof Integer count) result.put(key, count);
            else if (value instanceof String text) result.put(key, text);
            else throw new IllegalArgumentException("CONTENT_TYPE_UNREGISTERED: " + key);
        }
        return result;
    }
    static String version(Number value) { return value == null ? null : value.toString(); }
}
