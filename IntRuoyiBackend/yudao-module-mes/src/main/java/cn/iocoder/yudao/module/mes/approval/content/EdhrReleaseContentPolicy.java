package cn.iocoder.yudao.module.mes.approval.content;

import cn.iocoder.yudao.module.bpm.approval.content.ApprovalContentJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/** The closed accepted policy; unsupported schema constructs fail at validation. */
@Component
public final class EdhrReleaseContentPolicy {
    private final JsonNode schema;
    public EdhrReleaseContentPolicy() {
        try (var input = getClass().getResourceAsStream("/approval/edhr-release-content-v1.schema.json")) {
            if (input == null) throw new IllegalStateException("EDHR_CONTENT_POLICY_RESOURCE_MISSING");
            schema = ApprovalContentJson.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (java.io.IOException e) { throw new IllegalStateException("EDHR_CONTENT_POLICY_RESOURCE_UNREADABLE", e); }
    }
    public void validate(JsonNode value) { validate(schema, value, "$", true); }
    public void validateBatchGuard(JsonNode value) { validate(schema.at("/$defs/batchGuard"),value,"$.batchGuard",true); }
    private void validate(JsonNode spec, JsonNode value, String path, boolean enforce) {
        if (spec.has("$ref")) validate(schema.at(spec.get("$ref").asText().substring(1)), value, path, enforce);
        if (spec.has("allOf")) for (var item : spec.get("allOf")) validate(item, value, path, enforce);
        if (spec.has("if")) {
            String branch = matches(spec.get("if"), value, path) ? "then" : "else";
            if (spec.has(branch)) validate(spec.get(branch), value, path, enforce);
        }
        if (spec.has("anyOf")) {
            for (var item : spec.get("anyOf")) if (matches(item, value, path)) return;
            fail(path, "no permitted shape");
        }
        if (value == null || value.isMissingNode()) fail(path, "missing value");
        if (spec.has("const") && !spec.get("const").equals(value)) fail(path, "constant mismatch");
        if (spec.has("enum")) { boolean found = false; for (var item : spec.get("enum")) found |= item.equals(value); if (!found) fail(path, "enum mismatch"); }
        if (spec.has("type")) {
            boolean good = switch (spec.get("type").asText()) {
                case "object" -> value.isObject(); case "array" -> value.isArray(); case "string" -> value.isTextual();
                case "integer" -> value.isIntegralNumber(); case "boolean" -> value.isBoolean(); case "null" -> value.isNull();
                default -> throw new IllegalStateException("EDHR_CONTENT_POLICY_TYPE_UNSUPPORTED");
            }; if (!good) fail(path, "type mismatch");
        }
        if (value.isObject()) {
            if (spec.has("required")) for (var key : spec.get("required")) if (!value.has(key.asText())) fail(path + "." + key.asText(), "missing key");
            var fields = value.fields(); while (fields.hasNext()) {
                var field = fields.next(); var property = spec.path("properties").get(field.getKey());
                if (property == null && spec.path("additionalProperties").isBoolean() && !spec.path("additionalProperties").asBoolean()) fail(path + "." + field.getKey(), "unknown key");
                if (property != null) validate(property, field.getValue(), path + "." + field.getKey(), enforce);
            }
        }
        if (value.isTextual()) {
            String text = value.textValue();
            if (spec.has("pattern") && !text.matches(spec.get("pattern").asText())) fail(path, "pattern mismatch");
            if (spec.has("minLength") && text.length() < spec.get("minLength").asInt()) fail(path, "too short");
            if (spec.has("maxLength") && text.length() > spec.get("maxLength").asInt()) fail(path, "too long");
            if (spec.path("pattern").asText().equals("^[1-9][0-9]*$")) {
                try { if (Long.parseLong(text) <= 0) fail(path, "positive Long required"); }
                catch (NumberFormatException e) { fail(path, "Long overflow"); }
            }
        }
        if (value.isIntegralNumber() && spec.has("minimum") && value.bigIntegerValue().compareTo(spec.get("minimum").bigIntegerValue()) < 0) fail(path, "below minimum");
        if (value.isArray()) {
            if (spec.has("minItems") && value.size() < spec.get("minItems").asInt()) fail(path, "too few items");
            if (spec.has("maxItems") && value.size() > spec.get("maxItems").asInt()) fail(path, "too many items");
            var seen = new HashSet<JsonNode>(); for (int i = 0; i < value.size(); i++) {
                if (spec.path("uniqueItems").asBoolean() && !seen.add(value.get(i))) fail(path, "duplicate item");
                if (spec.has("items")) validate(spec.get("items"), value.get(i), path + "[" + i + "]", enforce);
            }
        }
    }
    private boolean matches(JsonNode spec, JsonNode value, String path) {
        try { validate(spec, value, path, true); return true; } catch (IllegalArgumentException e) { return false; }
    }
    private static void fail(String path, String reason) { throw new IllegalArgumentException("EDHR_CONTENT_INVALID: " + path + ": " + reason); }
}
