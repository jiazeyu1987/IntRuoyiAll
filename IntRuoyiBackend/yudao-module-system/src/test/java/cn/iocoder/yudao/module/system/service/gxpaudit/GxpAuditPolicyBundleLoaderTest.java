package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.enums.ErrorCodeConstants;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GxpAuditPolicyBundleLoaderTest {

    @Test
    void rejectsPolicyWhenOperationRequiredFieldIsMissing() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json");
        ObjectNode invalid = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(policy);
        ((ObjectNode) invalid.path("operations").get(0)).remove("subjectType");
        String invalidPolicy = invalid.toString();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(invalidPolicy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsPolicyWhenOperationEnumIsInvalid() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json");
        ObjectNode invalid = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(policy);
        ((ObjectNode) invalid.path("operations").get(0)).put("signaturePolicy", "INVALID");
        String invalidPolicy = invalid.toString();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(invalidPolicy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsPolicyWhenOperationTestIdsAreMissing() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json");
        ObjectNode invalid = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(policy);
        ((ObjectNode) invalid.path("operations").get(0)).remove("testIds");
        String invalidPolicy = invalid.toString();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(invalidPolicy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsPolicyWhenSchemaIsNotAnObject() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(policy, "null"));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsPolicyWhenSchemaIsEmpty() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(policy, ""));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsSchemaWhenRootRequiredIsMissing() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json")
                .replace("\"schemaVersion\", \"policyVersion\", \"status\", \"approvalReference\", \"operations\", \"coverageScope\"",
                        "\"schemaVersion\", \"policyVersion\", \"status\", \"approvalReference\", \"operations\"");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(policy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsSchemaWhenRootPropertiesAreMissing() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json")
                .replace("\"coverageScope\": {", "\"writeBoundaryScanRemoved\": {");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(policy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsSchemaWhenIdIsNotTheApprovedV2Id() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json")
                .replace("https://int.ruoyi.local/schema/gxp-audit-policy.v2", "https://example.invalid/schema");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(policy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsSchemaWhenWriteBoundaryScanConstraintIsMissing() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String sourceSchema = resource("META-INF/gxp/gxp-audit-policy.schema.json");
        ObjectNode schemaObject = (ObjectNode) new ObjectMapper().readTree(sourceSchema);
        ObjectNode scan = (ObjectNode) schemaObject.path("properties").path("coverageScope")
                .path("properties").path("writeBoundaryScan");
        scan.putArray("required").add("registrationMode").add("approvedExclusionsFile");
        String schema = schemaObject.toString();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(policy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void rejectsPolicyWhenNumericMinimumIsViolated() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json");
        String invalidPolicy = policy.replaceFirst("expectedMinCandidates: 400", "expectedMinCandidates: 0");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(invalidPolicy, schema));

        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), exception.getCode());
    }

    // BDD: Given one valid object, When representation changes, Then only artifact hash changes.
    @Test
    void formattingDoesNotChangeSemanticPolicyHash() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        JsonNode object = new ObjectMapper(new YAMLFactory()).readTree(policy);
        assertEquivalentRepresentations(policy, new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValueAsString(object));
    }

    @Test
    void commentsDoNotChangeSemanticPolicyHash() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        assertEquivalentRepresentations(policy, "# representation-only test comment\n" + policy);
    }

    @Test
    void rootAndNestedKeyOrderDoNotChangeSemanticPolicyHash() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        ObjectMapper json = new ObjectMapper();
        ObjectNode object = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(policy);
        String original = json.writeValueAsString(object);
        JsonNode schemaVersion = object.remove("schemaVersion");
        object.set("schemaVersion", schemaVersion);
        ObjectNode operation = (ObjectNode) object.path("operations").get(0);
        JsonNode operationId = operation.remove("operationId");
        operation.set("operationId", operationId);
        assertEquivalentRepresentations(original, json.writeValueAsString(object));
    }

    // BDD: Given a valid policy, When operation semantics change, Then semantic hash changes.
    @Test
    void operationSemanticChangeChangesPolicyHash() throws IOException {
        String policy = resource("META-INF/gxp/gxp-audit-policy.yaml");
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json");
        ObjectMapper json = new ObjectMapper();
        ObjectNode changed = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(policy);
        ObjectNode operation = (ObjectNode) changed.path("operations").get(0);
        String oldAction = operation.path("actionType").asText();
        operation.put("actionType", oldAction + "_CHANGED");
        GxpAuditPolicyBundleLoader loader = new GxpAuditPolicyBundleLoader();
        GxpAuditPolicyBundle before = loader.load(policy, schema);
        GxpAuditPolicyBundle after = loader.load(json.writeValueAsString(changed), schema);
        assertNotEquals(before.policyHash(), after.policyHash());
        assertEquals(changed, json.readTree(after.canonicalPolicyJson()),
                "Canonical JSON must contain the complete validated object, not a metadata envelope");
    }

    private void assertEquivalentRepresentations(String first, String second) throws IOException {
        String schema = resource("META-INF/gxp/gxp-audit-policy.schema.json");
        GxpAuditPolicyBundleLoader loader = new GxpAuditPolicyBundleLoader();
        GxpAuditPolicyBundle left = loader.load(first, schema);
        GxpAuditPolicyBundle right = loader.load(second, schema);
        assertEquals(left.policyNode(), right.policyNode(), "Fixture must preserve every semantic value");
        assertNotEquals(first, second);
        assertEquals(DigestUtil.sha256Hex(first.getBytes(StandardCharsets.UTF_8)), left.artifactHash());
        assertEquals(DigestUtil.sha256Hex(second.getBytes(StandardCharsets.UTF_8)), right.artifactHash());
        assertNotEquals(left.artifactHash(), right.artifactHash());
        assertEquals(left.policyHash(), right.policyHash());
        assertEquals(left.canonicalPolicyJson(), right.canonicalPolicyJson());
        assertEquals(left.policyNode(), new ObjectMapper().readTree(left.canonicalPolicyJson()));
        assertEquals(DigestUtil.sha256Hex(left.canonicalPolicyJson().getBytes(StandardCharsets.UTF_8)),
                left.policyHash());
    }

    private String resource(String location) throws IOException {
        if (location.endsWith("gxp-audit-policy.yaml")) {
            return GxpAuditPolicyStrictLoaderTest.strictFixtureYaml();
        }
        return new String(new ClassPathResource(location).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
