package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Canonical JSON contract for the independent completion source snapshot digest. */
public final class MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer {

    private MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer() {
    }

    public static String canonicalize(String json) {
        return canonicalNode(parseExact(json)).toString();
    }

    public static JsonNode parseExact(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Completion source JSON must not be blank");
        }
        final JsonNode parsed;
        try {
            ObjectMapper mapper = JsonUtils.getObjectMapper().copy()
                    .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true))
                    .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
            parsed = mapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Completion source JSON is invalid", exception);
        }
        if (parsed == null) {
            throw new IllegalArgumentException("Completion source JSON must contain a value");
        }
        return parsed;
    }

    public static String sourceSnapshotHash(String formalSourceSnapshotJson, String lossConditionFactsJson) {
        String canonicalFormal = canonicalize(formalSourceSnapshotJson);
        String canonicalLoss = canonicalize(lossConditionFactsJson);
        return DigestUtil.sha256Hex(DigestUtil.sha256Hex(canonicalFormal) + "|" + canonicalLoss);
    }

    private static JsonNode canonicalNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return JsonNodeFactory.instance.nullNode();
        }
        if (node.isObject()) {
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(Comparator.naturalOrder());
            for (String name : names) {
                result.set(name, canonicalNode(node.get(name)));
            }
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            node.elements().forEachRemaining(child -> result.add(canonicalNode(child)));
            return result;
        }
        if (node.isNumber()) {
            BigDecimal decimal = node.decimalValue().stripTrailingZeros();
            if (decimal.signum() == 0) {
                return JsonNodeFactory.instance.numberNode(BigInteger.ZERO);
            }
            try {
                return JsonNodeFactory.instance.numberNode(decimal.toBigIntegerExact());
            } catch (ArithmeticException ignored) {
                return JsonNodeFactory.instance.numberNode(decimal);
            }
        }
        return node;
    }
}
