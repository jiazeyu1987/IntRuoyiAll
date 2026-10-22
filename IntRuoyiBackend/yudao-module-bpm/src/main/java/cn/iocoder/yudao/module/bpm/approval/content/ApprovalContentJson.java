package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeMap;

/** Strict JSON input and the approved recursively sorted R5 representation. */
public final class ApprovalContentJson {
    private static final ObjectMapper READER = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
    private ApprovalContentJson() { }
    public static JsonNode parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("APPROVAL_CONTENT_JSON_REQUIRED");
        try { return READER.readValue(value, JsonNode.class); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("APPROVAL_CONTENT_JSON_INVALID", e); }
    }
    public static String canonical(JsonNode node) {
        if (node == null) throw new IllegalArgumentException("APPROVAL_CONTENT_REQUIRED");
        return JsonUtils.toJsonString(sorted(node));
    }
    private static JsonNode sorted(JsonNode node) {
        if (node.isObject()) {
            var values = new TreeMap<String, JsonNode>(); node.fields().forEachRemaining(e -> values.put(e.getKey(), e.getValue()));
            var result = JsonNodeFactory.instance.objectNode(); values.forEach((key, value) -> result.set(key, sorted(value))); return result;
        }
        if (node.isArray()) { var result = JsonNodeFactory.instance.arrayNode(); node.forEach(value -> result.add(sorted(value))); return result; }
        if (node.isFloatingPointNumber()) throw new IllegalArgumentException("APPROVAL_CONTENT_DECIMAL_MUST_BE_TEXT");
        return node.deepCopy();
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String hash(JsonNode value) { return hash(canonical(value)); }
}
