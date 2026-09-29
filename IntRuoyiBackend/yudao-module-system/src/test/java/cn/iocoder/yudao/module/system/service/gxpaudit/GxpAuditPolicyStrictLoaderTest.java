package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.enums.ErrorCodeConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** P2a only: real Loader, no activation, approval verification or persistence substitutes. */
class GxpAuditPolicyStrictLoaderTest {
    private static final String POLICY = "META-INF/gxp/gxp-audit-policy.yaml";
    private static final String SCHEMA = "META-INF/gxp/gxp-audit-policy.schema.json";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @TempDir
    Path temporaryDirectory;

    // Given the current packaged input, When loaded, Then retain the existing positive baseline.
    @Test
    void strictFixtureRemainsAnExplicitPositiveBaseline() throws IOException {
        String policy = resource(POLICY);
        GxpAuditPolicyBundle bundle = new GxpAuditPolicyBundleLoader().load(policy, resource(SCHEMA));
        assertEquals(YAML.readTree(policy), bundle.policyNode());
        assertEquals(bundle.policyNode(), JSON.readTree(bundle.canonicalPolicyJson()));
        assertEquals(DigestUtil.sha256Hex(policy.getBytes(StandardCharsets.UTF_8)), bundle.artifactHash());
    }

    // Given a test-only DRAFT v2 object, When loaded, Then preserve every field and both locators.
    @Test
    void acceptsStrictDraftWithoutInventingApprovalOrDroppingLocators() throws IOException {
        ObjectNode policy = strictPolicy();
        GxpAuditPolicyBundle bundle = new GxpAuditPolicyBundleLoader().load(policy.toString(), strictSchema());
        assertEquals(policy, bundle.policyNode());
        assertEquals(policy, JSON.readTree(bundle.canonicalPolicyJson()));
        assertEquals("DRAFT", bundle.status());
        assertEquals("TEST-ONLY-NOT-AN-APPROVAL", bundle.approvalReference());
        assertEquals(2, bundle.policyNode().path("operations").get(0).path("sourceLocators").size());
    }

    // These mutate the accepted existing input so an unrelated strict-root rejection cannot pass them.
    @ParameterizedTest(name = "rejects current-input contract violation: {0}")
    @ValueSource(strings = {"unknownRoot", "unknownOperation", "duplicateOperation", "reason",
            "state", "signature", "blankOperationId", "duplicateTestId", "blankTestId",
            "outOfScopeWithoutReason"})
    void rejectsContractViolationsOnReachableCurrentInput(String mutation) throws IOException {
        ObjectNode policy = (ObjectNode) YAML.readTree(resource(POLICY));
        String schema = resource(SCHEMA);
        new GxpAuditPolicyBundleLoader().load(policy.toString(), schema);
        ObjectNode operation = operation(policy);
        switch (mutation) {
            case "unknownRoot" -> policy.put("unrecognizedRoot", true);
            case "unknownOperation" -> operation.put("unrecognizedOperation", true);
            case "duplicateOperation" -> ((ArrayNode) policy.get("operations")).add(operation.deepCopy());
            case "reason" -> operation.put("reasonPolicy", "NOT_A_REASON_POLICY");
            case "state" -> operation.put("statePolicy", "NOT_A_STATE_POLICY");
            case "signature" -> operation.put("signaturePolicy", "NOT_A_SIGNATURE_POLICY");
            case "blankOperationId" -> operation.put("operationId", "   ");
            case "duplicateTestId" -> ((ArrayNode) operation.get("testIds"))
                    .add(operation.path("testIds").get(0).asText());
            case "blankTestId" -> operation.putArray("testIds").add("   ");
            case "outOfScopeWithoutReason" -> operation.put("applicability", "OUT_OF_RELEASE_SCOPE");
            default -> throw new AssertionError(mutation);
        }
        assertInvalid(policy.toString(), schema);
    }

    // First prove the paired valid strict input loads; rejecting every v2 input is not success.
    @ParameterizedTest(name = "strict v2 paired rejection: {0}")
    @ValueSource(strings = {"coverageScope", "snapshotProfile", "ownerRole", "sourceLocators",
            "emptyLocators", "duplicateLocators", "blankLocator", "oldLocatorAlias", "oldOwnerAlias",
            "oldRootAlias", "unknownProfile", "oldSignatureAlias", "activationProfile", "activationReason"})
    void rejectsStrictMutationsOnlyAfterAcceptingPairedValidInput(String mutation) throws IOException {
        ObjectNode policy = strictPolicy();
        String schema = strictSchema();
        assertDoesNotThrow(() -> new GxpAuditPolicyBundleLoader().load(policy.toString(), schema),
                "The paired strict positive must load before this rejection can count");
        ObjectNode operation = operation(policy);
        switch (mutation) {
            case "coverageScope" -> policy.remove("coverageScope");
            case "snapshotProfile", "ownerRole", "sourceLocators" -> operation.remove(mutation);
            case "emptyLocators" -> operation.putArray("sourceLocators");
            case "duplicateLocators" -> ((ArrayNode) operation.get("sourceLocators"))
                    .add(operation.path("sourceLocators").get(0).asText());
            case "blankLocator" -> operation.putArray("sourceLocators").add("   ");
            case "oldLocatorAlias" -> operation.put("sourceLocator", "legacy#alias");
            case "oldOwnerAlias" -> operation.put("owner", "legacy-owner");
            case "oldRootAlias" -> policy.set("writeBoundaryScan",
                    policy.path("coverageScope").path("writeBoundaryScan").deepCopy());
            case "unknownProfile" -> operation.put("snapshotProfile", "NOT_A_PROFILE");
            case "oldSignatureAlias" -> operation.put("signaturePolicy", "NOT_REQUIRED");
            case "activationProfile", "activationReason" -> {
                operation.put("operationId", "gxp.policy.activate");
                operation.put("snapshotProfile", mutation.equals("activationProfile") ? "ORDER" : "POLICY");
                operation.put("reasonPolicy", mutation.equals("activationReason") ? "SYSTEM" : "APPROVAL_REFERENCE");
                operation.put("statePolicy", "ABSENT_OR_PRESENT_TO_PRESENT");
            }
            default -> throw new AssertionError(mutation);
        }
        assertInvalid(policy.toString(), schema);
    }

    @Test
    void sameLocatorMayBelongToDifferentOperations() throws IOException {
        ObjectNode policy = strictPolicy();
        ObjectNode second = operation(policy).deepCopy();
        second.put("operationId", "mes.active-order.restore");
        second.put("actionType", "RESTORE");
        second.put("statePolicy", "PRESENT_TO_PRESENT");
        ((ArrayNode) policy.get("operations")).add(second);
        assertEquals(policy, new GxpAuditPolicyBundleLoader().load(policy.toString(), strictSchema()).policyNode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORDER", "PRODUCTION", "ALLOCATION", "PRODUCTION_REVIEW", "PQC",
            "PQC_REVIEW", "COMPLETION", "APPLICATION", "PQC_RELEASE", "MARKET_RELEASE",
            "ATTACHMENT", "NCR", "ATTEMPT", "POLICY"})
    void declaredProfilesAreAcceptedButUnknownProfilesAreRejected(String profile) throws IOException {
        ObjectNode policy = strictPolicy();
        operation(policy).put("operationId", "test.profile.declaration");
        operation(policy).put("snapshotProfile", profile);
        String schema = strictSchema();
        assertDoesNotThrow(() -> new GxpAuditPolicyBundleLoader().load(policy.toString(), schema));
        operation(policy).put("snapshotProfile", "MES_" + profile);
        assertInvalid(policy.toString(), schema);
    }

    @Test
    void scopeExclusionRequiresCompleteEvidenceAndFutureOwnership() throws IOException {
        ObjectNode policy = strictPolicy();
        ObjectNode exclusion = ((ArrayNode) policy.path("coverageScope").path("excludedReferences")).addObject();
        exclusion.put("reference", "test.excluded").put("reason", "test-only scope")
                .put("evidenceReference", "test-only-evidence").put("futureOwnerRole", "test-owner")
                .put("futureTaskReference", "test-task");
        String schema = strictSchema();
        assertDoesNotThrow(() -> new GxpAuditPolicyBundleLoader().load(policy.toString(), schema));
        exclusion.remove("evidenceReference");
        assertInvalid(policy.toString(), schema);
    }

    @Test
    void duplicateYamlKeyIsRejected() throws IOException {
        assertInvalid(resource(POLICY) + "\npolicyVersion: duplicate\n", resource(SCHEMA));
    }

    @Test
    void unsupportedSchemaConstraintCannotBeSilentlyIgnored() throws IOException {
        ObjectNode schema = (ObjectNode) JSON.readTree(resource(SCHEMA));
        // A standards validator rejects all objects via not:{}; a subset validator must reject
        // the unsupported keyword. Silently accepting it is wrong under either implementation.
        schema.set("not", JSON.createObjectNode());
        assertInvalid(resource(POLICY), schema.toString());
    }

    @Test
    void classpathArtifactHashUsesExactValidUtf8Bytes() throws IOException {
        byte[] bytes = ("# UTF-8 representation \u6d4b\u8bd5\r\n" + resource(POLICY))
                .getBytes(StandardCharsets.UTF_8);
        assertEquals(DigestUtil.sha256Hex(bytes), loadFromClasspath(bytes).artifactHash());
    }

    @Test
    void classpathMalformedUtf8IsRejectedRatherThanReplacedInsideComment() throws IOException {
        byte[] content = resource(POLICY).getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[content.length + 5];
        bytes[0] = '#'; bytes[1] = ' '; bytes[2] = (byte) 0xc3; bytes[3] = '('; bytes[4] = '\n';
        System.arraycopy(content, 0, bytes, 5, content.length);
        ServiceException error = assertThrows(ServiceException.class, () -> loadFromClasspath(bytes));
        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), error.getCode());
    }

    private GxpAuditPolicyBundle loadFromClasspath(byte[] bytes) throws IOException {
        Path policy = temporaryDirectory.resolve("p.yaml");
        Path schema = temporaryDirectory.resolve("s.json");
        Files.write(policy, bytes);
        Files.writeString(schema, resource(SCHEMA), StandardCharsets.UTF_8);
        URL policyUrl = policy.toUri().toURL();
        URL schemaUrl = schema.toUri().toURL();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader isolated = new ClassLoader(original) {
            @Override
            public URL getResource(String name) {
                if (POLICY.equals(name)) return policyUrl;
                if (SCHEMA.equals(name)) return schemaUrl;
                return super.getResource(name);
            }
            @Override
            public InputStream getResourceAsStream(String name) {
                if (POLICY.equals(name) || SCHEMA.equals(name)) {
                    try {
                        return getResource(name).openStream();
                    } catch (IOException exception) {
                        throw new java.io.UncheckedIOException(exception);
                    }
                }
                return super.getResourceAsStream(name);
            }
        };
        try {
            Thread.currentThread().setContextClassLoader(isolated);
            return new GxpAuditPolicyBundleLoader().load();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    // SL-PARSE-01: every rejection is paired with an accepted complete document.
    @ParameterizedTest(name = "whole-input policy parsing: {0}")
    @ValueSource(strings = {"secondYamlDocument", "secondJsonValue", "invalidJsonTail"})
    void rejectsUnconsumedPolicyInput(String mutation) throws IOException {
        ObjectNode expected = strictPolicy();
        String schema = strictSchema();
        String valid = mutation.equals("secondYamlDocument")
                ? strictFixtureYaml() : expected.toString();
        String validTail = mutation.equals("secondYamlDocument") ? "\n# legal trailing comment\n" : " \r\n  \n";
        GxpAuditPolicyBundle accepted = new GxpAuditPolicyBundleLoader().load(valid + validTail, schema);
        assertEquals(expected, accepted.policyNode());
        String suffix = switch (mutation) {
            case "secondYamlDocument" -> "\n---\nunvalidated: true\n";
            case "secondJsonValue" -> "\n{\"unvalidated\":true}";
            case "invalidJsonTail" -> "\n{\"unvalidated\":";
            default -> throw new AssertionError(mutation);
        };
        assertInvalid(valid + suffix, schema);
    }

    @ParameterizedTest(name = "whole-input schema parsing: {0}")
    @ValueSource(strings = {"secondValue", "invalidTail", "duplicateRootKey", "duplicateNestedKey"})
    void rejectsUnconsumedOrDuplicateSchemaInput(String mutation) throws IOException {
        String policy = strictFixtureYaml();
        String schema = strictSchema();
        assertEquals(strictPolicy(), new GxpAuditPolicyBundleLoader()
                .load(policy + "\n# legal trailing comment\n", schema + " \r\n\t").policyNode());
        String invalid;
        switch (mutation) {
            case "secondValue" -> invalid = schema + "\n{\"unvalidated\":true}";
            case "invalidTail" -> invalid = schema + "\n{\"unvalidated\":";
            case "duplicateRootKey" -> {
                int end = schema.lastIndexOf('}');
                assertTrue(end > 0);
                assertTrue(schema.contains("\"additionalProperties\": false"));
                invalid = schema.substring(0, end) + ",\"additionalProperties\":true" + schema.substring(end);
            }
            case "duplicateNestedKey" -> {
                String original = "\"policyVersion\": { \"type\": \"string\", \"minLength\": 1";
                assertTrue(schema.contains(original), "Mutation must target the existing nested constraint");
                invalid = schema.replace(original,
                        "\"policyVersion\": { \"type\": \"string\", \"minLength\": 0, \"minLength\": 1");
            }
            default -> throw new AssertionError(mutation);
        }
        assertNotEquals(schema, invalid);
        assertInvalid(policy, invalid);
    }

    private static ObjectNode strictPolicy() throws IOException {
        ObjectNode existing = (ObjectNode) YAML.readTree(rawResource(POLICY));
        ObjectNode policy = existing.deepCopy();
        policy.put("policyVersion", "test-only-strict-v2");
        policy.put("status", "DRAFT");
        policy.put("approvalReference", "TEST-ONLY-NOT-AN-APPROVAL");
        ObjectNode scope = policy.putObject("coverageScope");
        scope.put("scopeId", "TEST-ONLY-R1");
        scope.put("mode", "R1");
        scope.putArray("includedReferences").add("mes.active-order.add");
        scope.putArray("excludedReferences");
        scope.set("writeBoundaryScan", existing.path("coverageScope").path("writeBoundaryScan").deepCopy());
        ObjectNode operation = operation(existing).deepCopy();
        String locator = operation.path("sourceLocators").get(0).asText();
        operation.putArray("sourceLocators").add(locator).add("test.fixture.StrictOrder#add");
        operation.put("ownerRole", "TEST_ONLY_OWNER");
        operation.put("snapshotProfile", "ORDER");
        operation.put("reasonPolicy", "SYSTEM");
        operation.put("signaturePolicy", "NONE");
        policy.putArray("operations").add(operation);
        return policy;
    }

    private static String strictSchema() throws IOException {
        return resource(SCHEMA);
    }

    private static ObjectNode operation(ObjectNode policy) {
        return (ObjectNode) policy.path("operations").get(0);
    }

    private static void assertInvalid(String policy, String schema) {
        ServiceException error = assertThrows(ServiceException.class,
                () -> new GxpAuditPolicyBundleLoader().load(policy, schema));
        assertEquals(ErrorCodeConstants.GXP_AUDIT_POLICY_BUNDLE_INVALID.getCode(), error.getCode());
    }

    static String strictFixtureYaml() throws IOException {
        return YAML.writeValueAsString(strictPolicy());
    }

    private static String resource(String name) throws IOException {
        return POLICY.equals(name) ? strictFixtureYaml() : rawResource(name);
    }

    private static String rawResource(String name) throws IOException {
        try (InputStream input = new ClassPathResource(name).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
