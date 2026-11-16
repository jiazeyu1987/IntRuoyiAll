package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Explicit local-only policy source. Never creates approval or activation records. */
public class GxpLocalDevelopmentPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(GxpLocalDevelopmentPolicy.class);
    private static final Pattern LOCAL_MYSQL = Pattern.compile(
            "^jdbc:mysql://(?:localhost|127\\.0\\.0\\.[12]|\\[::1\\])(?::[0-9]+)?/[^/?]+(?:\\?.*)?$");
    private final GxpAuditPolicyBundle bundle;
    private final Map<String, JsonNode> operations;

    public GxpLocalDevelopmentPolicy(Environment environment, GxpAuditPolicyBundleLoader loader) {
        if (!Arrays.equals(environment.getActiveProfiles(), new String[]{"local"})) {
            throw new IllegalStateException("LOCAL_POLICY requires only the local Spring profile");
        }
        requireLocalDatabase(environment, "master", true);
        requireLocalDatabase(environment, "slave", false);
        bundle = loader.load(); // Keep schema, required fields and operation uniqueness validation.
        Map<String, JsonNode> entries = new LinkedHashMap<>();
        bundle.policyNode().path("operations").forEach(node ->
                entries.put(node.path("operationId").asText(), node.deepCopy()));
        operations = Map.copyOf(entries);
        LOG.warn("LOCAL DEVELOPMENT GxP policy enabled: version={}, status={}, policyHash={}, operations={}. "
                        + "Not formally activated; audit events use {}. Existing approvals and history are preserved.",
                bundle.policyVersion(), bundle.status(), bundle.policyHash(), operations.size(), eventPolicyVersion());
    }

    private static void requireLocalDatabase(Environment environment, String name, boolean required) {
        String url = environment.getProperty("spring.datasource.dynamic.datasource." + name + ".url");
        if ((!required && url == null)) return;
        if (url == null || !LOCAL_MYSQL.matcher(url).matches()) {
            // Do not expose JDBC options, which can contain credentials.
            throw new IllegalStateException("LOCAL_POLICY requires a loopback MySQL " + name + " datasource");
        }
    }

    public String eventPolicyVersion() {
        return "LOCAL-DEV-" + bundle.policyHash();
    }

    public void verifyBundle(GxpAuditPolicyBundle current) {
        if (!bundle.policyHash().equals(current.policyHash())) {
            throw new IllegalStateException("Local development policy changed after initialization");
        }
    }

    public GxpAuditPolicyOperationDO findOperation(Long tenantId, String operationId) {
        JsonNode node = operations.get(operationId);
        if (node == null) return null;
        GxpAuditPolicyOperationDO operation = new GxpAuditPolicyOperationDO();
        operation.setTenantId(tenantId);
        operation.setPolicyVersion(eventPolicyVersion());
        operation.setOperationId(operationId);
        operation.setSourceType(node.path("sourceType").asText());
        operation.setSourceLocator(node.path("sourceLocators").toString());
        operation.setDomain(node.path("domain").asText());
        operation.setSubjectType(node.path("subjectType").asText());
        operation.setActionType(node.path("actionType").asText());
        operation.setReasonPolicy(node.path("reasonPolicy").asText());
        operation.setSignaturePolicy(node.path("signaturePolicy").asText());
        operation.setStatePolicy(node.path("statePolicy").asText());
        operation.setRetentionClass(node.path("retentionClass").asText());
        operation.setApplicability(node.path("applicability").asText());
        operation.setActive(true);
        return operation;
    }
}
