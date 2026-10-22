package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID;

@Component
public class GxpAuditPolicyBundleLoader {

    private static final String SCHEMA_VERSION = "gxp-audit-policy.v2";
    private static final String SCHEMA_ID = "https://int.ruoyi.local/schema/gxp-audit-policy.v2";
    private static final String[] ROOT_REQUIRED_FIELDS = {
            "schemaVersion", "policyVersion", "status", "approvalReference", "operations", "coverageScope"
    };
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(YAMLFactory.builder().build())
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public GxpAuditPolicyBundle load() {
        String yaml = read("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = read("META-INF/gxp/gxp-audit-policy.schema.json");
        return load(yaml, schema);
    }

    GxpAuditPolicyBundle load(String yaml, String schema) {
        JsonNode policyNode;
        try {
            policyNode = YAML_MAPPER.readTree(yaml);
            JsonNode schemaNode = JSON_MAPPER.readTree(schema);
            if (policyNode == null || !policyNode.isObject()) {
                throw new IllegalArgumentException("policy root must be an object");
            }
            if (schemaNode == null || !schemaNode.isObject()) {
                throw new IllegalArgumentException("schema root must be an object");
            }
            validateSchemaDefinition(schemaNode);
            validateAgainstSchema(policyNode, schemaNode, schemaNode, "$");
            validatePolicyContract(policyNode);
        } catch (Exception exception) {
            throw exception(GXP_AUDIT_POLICY_BUNDLE_INVALID, "policy-schema-validation:" + exception.getMessage());
        }
        String schemaVersion = text(policyNode, "schemaVersion");
        String policyVersion = text(policyNode, "policyVersion");
        String status = text(policyNode, "status");
        String approvalReference = text(policyNode, "approvalReference");
        if (!SCHEMA_VERSION.equals(schemaVersion)
                || policyVersion.isBlank() || status.isBlank() || approvalReference.isBlank()) {
            throw exception(GXP_AUDIT_POLICY_BUNDLE_INVALID, "schemaVersion/status/strategy schema");
        }
        String canonical = canonicalPolicyJson(policyNode);
        return new GxpAuditPolicyBundle(schemaVersion, policyVersion, status, approvalReference,
                DigestUtil.sha256Hex(canonical), DigestUtil.sha256Hex(yaml.getBytes(StandardCharsets.UTF_8)),
                canonical, yaml, schema, policyNode.deepCopy());
    }

    private void validateSchemaDefinition(JsonNode schema) {
        validateSupportedSchemaKeywords(schema);
        if (!SCHEMA_ID.equals(text(schema, "$id"))) {
            throw new IllegalArgumentException("schema $id must identify gxp-audit-policy.v2");
        }
        if (!"object".equals(text(schema, "type"))) {
            throw new IllegalArgumentException("schema root type must be object");
        }
        requireArrayValues(schema, "required", ROOT_REQUIRED_FIELDS, "schema root required");

        JsonNode properties = schema.get("properties");
        if (properties == null || !properties.isObject()) {
            throw new IllegalArgumentException("schema root properties must be an object");
        }
        for (String field : ROOT_REQUIRED_FIELDS) {
            if (!properties.has(field)) {
                throw new IllegalArgumentException("schema root properties missing " + field);
            }
        }

        JsonNode writeBoundaryScan = properties.path("coverageScope").path("properties").get("writeBoundaryScan");
        if (writeBoundaryScan == null || !writeBoundaryScan.isObject()
                || !"object".equals(text(writeBoundaryScan, "type"))) {
            throw new IllegalArgumentException("schema must define writeBoundaryScan as an object");
        }
        requireArrayValues(writeBoundaryScan, "required", new String[]{"registrationMode", "categories"},
                "writeBoundaryScan required");
        JsonNode boundaryProperties = writeBoundaryScan.get("properties");
        if (boundaryProperties == null || !boundaryProperties.isObject()
                || !boundaryProperties.has("registrationMode")
                || !boundaryProperties.has("categories")) {
            throw new IllegalArgumentException("writeBoundaryScan properties are incomplete");
        }

        JsonNode definitions = schema.get("$defs");
        if (definitions == null || !definitions.isObject()
                || !definitions.has("writeBoundaryCategory")
                || !definitions.has("unregisteredDisposition")) {
            throw new IllegalArgumentException("schema definitions are incomplete");
        }
    }

    private void validateSupportedSchemaKeywords(JsonNode schema) {
        Set<String> supported = Set.of("$schema", "$id", "title", "description", "$defs", "$ref",
                "type", "required", "properties", "additionalProperties", "const", "enum",
                "minimum", "maximum", "minLength", "maxLength", "pattern", "minItems", "maxItems",
                "uniqueItems", "items");
        if (!schema.isObject()) {
            throw new IllegalArgumentException("schema definition must be an object");
        }
        schema.fieldNames().forEachRemaining(keyword -> {
            if (!supported.contains(keyword)) {
                throw new IllegalArgumentException("unsupported schema keyword " + keyword);
            }
        });
        for (String container : new String[]{"properties", "$defs"}) {
            JsonNode children = schema.get(container);
            if (children != null) {
                if (!children.isObject()) {
                    throw new IllegalArgumentException(container + " must be an object");
                }
                children.elements().forEachRemaining(this::validateSupportedSchemaKeywords);
            }
        }
        if (schema.has("items")) {
            validateSupportedSchemaKeywords(schema.get("items"));
        }
        if (schema.has("additionalProperties") && !schema.get("additionalProperties").isBoolean()) {
            throw new IllegalArgumentException("unsupported additionalProperties constraint");
        }
        if (schema.has("uniqueItems") && !schema.get("uniqueItems").isBoolean()) {
            throw new IllegalArgumentException("uniqueItems must be boolean");
        }
        for (String keyword : new String[]{"minimum", "maximum"}) {
            if (schema.has(keyword) && !schema.get(keyword).isNumber()) {
                throw new IllegalArgumentException(keyword + " must be numeric");
            }
        }
        for (String keyword : new String[]{"required", "enum"}) {
            if (schema.has(keyword) && !schema.get(keyword).isArray()) {
                throw new IllegalArgumentException(keyword + " must be an array");
            }
        }
        if (schema.has("$ref") && (schema.size() != 1 || !schema.get("$ref").isTextual())) {
            throw new IllegalArgumentException("schema references must be standalone local references");
        }
        for (String keyword : new String[]{"minLength", "maxLength", "minItems", "maxItems"}) {
            if (schema.has(keyword) && (!schema.get(keyword).canConvertToInt()
                    || !schema.get(keyword).isIntegralNumber() || schema.get(keyword).intValue() < 0)) {
                throw new IllegalArgumentException(keyword + " must be a nonnegative integer");
            }
        }
    }

    private void requireArrayValues(JsonNode parent, String field, String[] expected, String description) {
        JsonNode values = parent.get(field);
        if (values == null || !values.isArray()) {
            throw new IllegalArgumentException(description + " must be an array");
        }
        for (String value : expected) {
            boolean present = false;
            for (JsonNode item : values) {
                if (value.equals(item.asText())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                throw new IllegalArgumentException(description + " missing " + value);
            }
        }
    }

    private String text(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        return value != null && value.isTextual() ? value.textValue() : "";
    }

    private void validateAgainstSchema(JsonNode value, JsonNode schema, JsonNode rootSchema, String path) {
        if (schema.has("$ref")) {
            String reference = schema.get("$ref").asText();
            if (!reference.startsWith("#/") || !reference.substring(2).startsWith("$defs/")) {
                throw new IllegalArgumentException(path + " uses unsupported schema reference " + reference);
            }
            String definition = reference.substring("#/$defs/".length());
            JsonNode definitions = rootSchema.path("$defs");
            if (!definitions.has(definition)) {
                throw new IllegalArgumentException(path + " references missing schema definition " + definition);
            }
            validateAgainstSchema(value, definitions.get(definition), rootSchema, path);
        }
        if (schema.has("const") && !schema.get("const").equals(value)) {
            throw new IllegalArgumentException(path + " must equal " + schema.get("const"));
        }
        if (schema.has("enum")) {
            boolean matches = false;
            for (JsonNode allowed : schema.get("enum")) {
                if (allowed.equals(value)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                throw new IllegalArgumentException(path + " has an unsupported enum value");
            }
        }
        if (schema.has("type") && !matchesType(value, schema.get("type").asText())) {
            throw new IllegalArgumentException(path + " has type " + value.getNodeType());
        }
        if (schema.has("minimum") && (!value.isNumber()
                || value.decimalValue().compareTo(schema.get("minimum").decimalValue()) < 0)) {
            throw new IllegalArgumentException(path + " is below minimum");
        }
        if (schema.has("maximum") && (!value.isNumber()
                || value.decimalValue().compareTo(schema.get("maximum").decimalValue()) > 0)) {
            throw new IllegalArgumentException(path + " is above maximum");
        }
        if (schema.has("minLength") && (!value.isTextual() || value.textValue().length() < schema.get("minLength").asInt())) {
            throw new IllegalArgumentException(path + " is shorter than minLength");
        }
        if (schema.path("minLength").asInt() > 0 && value.isTextual() && value.asText().isBlank()) {
            throw new IllegalArgumentException(path + " must not be blank");
        }
        if (schema.has("maxLength") && (!value.isTextual() || value.textValue().length() > schema.get("maxLength").asInt())) {
            throw new IllegalArgumentException(path + " is longer than maxLength");
        }
        if (schema.has("pattern") && (!value.isTextual() || !value.textValue().matches(schema.get("pattern").asText()))) {
            throw new IllegalArgumentException(path + " does not match pattern");
        }
        if (schema.has("minItems") && (!value.isArray() || value.size() < schema.get("minItems").asInt())) {
            throw new IllegalArgumentException(path + " has fewer items than minItems");
        }
        if (schema.has("maxItems") && (!value.isArray() || value.size() > schema.get("maxItems").asInt())) {
            throw new IllegalArgumentException(path + " has more items than maxItems");
        }
        if (schema.path("uniqueItems").asBoolean(false) && value.isArray()) {
            Set<JsonNode> values = new HashSet<>();
            for (JsonNode item : value) {
                if (!values.add(item)) throw new IllegalArgumentException(path + " contains duplicate items");
            }
        }
        if (value.isObject()) {
            JsonNode required = schema.path("required");
            for (JsonNode field : required) {
                if (!value.has(field.asText())) {
                    throw new IllegalArgumentException(path + " is missing required field " + field.asText());
                }
            }
            JsonNode properties = schema.path("properties");
            boolean rejectsUnknownFields = schema.has("additionalProperties")
                    && schema.get("additionalProperties").isBoolean()
                    && !schema.get("additionalProperties").asBoolean();
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode fieldSchema = properties.get(field.getKey());
                if (fieldSchema == null || fieldSchema.isMissingNode()) {
                    if (rejectsUnknownFields) {
                        throw new IllegalArgumentException(path + " contains unknown field " + field.getKey());
                    }
                    continue;
                }
                validateAgainstSchema(field.getValue(), fieldSchema, rootSchema, path + "." + field.getKey());
            }
        }
        if (value.isArray() && schema.has("items")) {
            for (int index = 0; index < value.size(); index++) {
                validateAgainstSchema(value.get(index), schema.get("items"), rootSchema, path + "[" + index + "]");
            }
        }
    }

    private boolean matchesType(JsonNode value, String type) {
        return switch (type) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "boolean" -> value.isBoolean();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            case "null" -> value.isNull();
            default -> throw new IllegalArgumentException("unsupported schema type " + type);
        };
    }

    private void validatePolicyContract(JsonNode policy) {
        Set<String> root = Set.of(ROOT_REQUIRED_FIELDS);
        policy.fieldNames().forEachRemaining(field -> {
            if (!root.contains(field)) throw new IllegalArgumentException("unknown root field " + field);
        });
        Set<String> ids = new HashSet<>();
        for (JsonNode operation : policy.path("operations")) {
            String id = text(operation, "operationId");
            if (id.isBlank() || !id.equals(id.strip()) || !ids.add(id)) {
                throw new IllegalArgumentException("operationId is blank, noncanonical or duplicated: " + id);
            }
            for (String field : new String[]{"ownerRole", "snapshotProfile", "domain", "subjectType",
                    "actionType", "retentionClass"}) {
                if (text(operation, field).isBlank()) throw new IllegalArgumentException(id + " missing " + field);
            }
            uniqueStrings(operation.path("sourceLocators"), id + ".sourceLocators");
            uniqueStrings(operation.path("testIds"), id + ".testIds");
            if ("OUT_OF_RELEASE_SCOPE".equals(text(operation, "applicability"))) {
                for (String field : new String[]{"scopeReason", "futureOwnerRole", "futureTaskReference"}) {
                    if (text(operation, field).isBlank()) throw new IllegalArgumentException(id + " missing " + field);
                }
            }
            if ("gxp.policy.activate".equals(id)) {
                requireOperationValue(operation, "snapshotProfile", "POLICY");
                requireOperationValue(operation, "reasonPolicy", "APPROVAL_REFERENCE");
                requireOperationValue(operation, "statePolicy", "ABSENT_OR_PRESENT_TO_PRESENT");
                requireOperationValue(operation, "signaturePolicy", "NONE");
            }
            if ("gxp.attempt.record".equals(id)) {
                requireOperationValue(operation, "snapshotProfile", "ATTEMPT");
                requireOperationValue(operation, "reasonPolicy", "SYSTEM_ERROR");
                requireOperationValue(operation, "statePolicy", "ABSENT_TO_PRESENT");
                requireOperationValue(operation, "signaturePolicy", "NONE");
            }
        }
    }

    private void uniqueStrings(JsonNode values, String path) {
        if (!values.isArray() || values.isEmpty()) throw new IllegalArgumentException(path + " must be nonempty");
        Set<String> seen = new HashSet<>();
        for (JsonNode value : values) {
            if (!value.isTextual() || value.asText().isBlank() || !seen.add(value.asText())) {
                throw new IllegalArgumentException(path + " contains blank, invalid or duplicate values");
            }
        }
    }

    private void requireOperationValue(JsonNode operation, String field, String expected) {
        if (!expected.equals(text(operation, field))) {
            throw new IllegalArgumentException(text(operation, "operationId") + "." + field + " must be " + expected);
        }
    }

    private String read(String location) {
        try {
            ClassPathResource resource = new ClassPathResource(location);
            if (!resource.exists()) {
                throw exception(GXP_AUDIT_POLICY_BUNDLE_INVALID, "missing:" + location);
            }
            try (InputStream input = resource.getInputStream()) {
                return StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(input.readAllBytes())).toString();
            }
        } catch (IOException exception) {
            throw exception(GXP_AUDIT_POLICY_BUNDLE_INVALID, location);
        }
    }

    private String canonicalPolicyJson(JsonNode policyNode) {
        return canonicalNode(policyNode).toString();
    }

    private JsonNode canonicalNode(JsonNode node) {
        if (node.isObject()) {
            Map<String, JsonNode> fields = new TreeMap<>();
            node.fields().forEachRemaining(field -> fields.put(field.getKey(), field.getValue()));
            ObjectNode result = JSON_MAPPER.createObjectNode();
            fields.forEach((name, value) -> result.set(name, canonicalNode(value)));
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = JSON_MAPPER.createArrayNode();
            for (JsonNode item : node) {
                result.add(canonicalNode(item));
            }
            return result;
        }
        return node.deepCopy();
    }
}
