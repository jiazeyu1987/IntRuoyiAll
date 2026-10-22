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
    private final JsonNode activeOrderSchema;
    private final JsonNode activeOrderV2Schema;
    public EdhrReleaseContentPolicy() {
        schema=load("/approval/edhr-release-content-v1.schema.json");
        var changes=load("/approval/edhr-release-content-active-order-v1.schema.json");
        var active=(com.fasterxml.jackson.databind.node.ObjectNode)schema.deepCopy();
        active.set("properties",changes.get("properties"));
        var definitions=(com.fasterxml.jackson.databind.node.ObjectNode)active.get("$defs");
        changes.get("$defs").fields().forEachRemaining(field -> definitions.set(field.getKey(),field.getValue()));
        activeOrderSchema=active;
        var v2 = (com.fasterxml.jackson.databind.node.ObjectNode) active.deepCopy();
        var v2Changes = load("/approval/edhr-release-content-active-order-v2.schema.json");
        v2.set("properties", v2Changes.get("properties"));
        var v2Definitions = (com.fasterxml.jackson.databind.node.ObjectNode) v2.get("$defs");
        v2Changes.get("$defs").fields().forEachRemaining(field -> v2Definitions.set(field.getKey(), field.getValue()));
        activeOrderV2Schema = v2;
    }
    private JsonNode load(String path) {
        try (var input = getClass().getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("EDHR_CONTENT_POLICY_RESOURCE_MISSING");
            return ApprovalContentJson.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (java.io.IOException e) { throw new IllegalStateException("EDHR_CONTENT_POLICY_RESOURCE_UNREADABLE", e); }
    }
    public void validate(JsonNode value) {
        String version=value==null?null:value.path("contentPolicyVersion").asText();
        if("approval-content-v1".equals(version)) validate(schema,schema,value,"$",true);
        else if("approval-content-active-order-v1".equals(version)) validate(activeOrderSchema,activeOrderSchema,value,"$",true);
        else if("approval-content-active-order-v2".equals(version)) validate(activeOrderV2Schema,activeOrderV2Schema,value,"$",true);
        else fail("$","unregistered content policy");
    }
    public void validateBatchGuard(JsonNode value) { validate(schema,schema.at("/$defs/batchGuard"),value,"$.batchGuard",true); }

    private void validate(JsonNode definitions, JsonNode spec, JsonNode value, String path, boolean enforce) {
        if (spec.has("$ref")) {
            validate(definitions,definitions.at(spec.get("$ref").asText().substring(1)),value,path,enforce);
        }
        if (spec.has("allOf")) for (var item : spec.get("allOf")) validate(definitions,item, value, path, enforce);
        if (spec.has("if")) {
            String branch = matches(definitions,spec.get("if"), value, path) ? "then" : "else";
            if (spec.has(branch)) validate(definitions,spec.get(branch), value, path, enforce);
        }
        if (spec.has("anyOf")) {
            for (var item : spec.get("anyOf")) if (matches(definitions,item, value, path)) return;
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
                if (property != null) validate(definitions,property, field.getValue(), path + "." + field.getKey(), enforce);
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
                if (spec.has("items")) validate(definitions,spec.get("items"), value.get(i), path + "[" + i + "]", enforce);
            }
        }
    }
    private boolean matches(JsonNode definitions, JsonNode spec, JsonNode value, String path) {
        try { validate(definitions,spec, value, path, true); return true; } catch (IllegalArgumentException e) { return false; }
    }
    private static void fail(String path, String reason) { throw new IllegalArgumentException("EDHR_CONTENT_INVALID: " + path + ": " + reason); }
}
