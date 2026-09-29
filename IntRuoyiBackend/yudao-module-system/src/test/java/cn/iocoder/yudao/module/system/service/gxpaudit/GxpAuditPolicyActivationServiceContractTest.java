package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GxpAuditPolicyActivationServiceContractTest {

    @Test
    void activationServiceMustBeSpringTransactionalImplementationAndLoadV2Bundle() throws Exception {
        assertTrue(GxpAuditPolicyActivationService.class.isAssignableFrom(
                GxpAuditPolicyActivationServiceImpl.class));
        assertTrue(GxpAuditPolicyActivationServiceImpl.class.isAnnotationPresent(Service.class));
        Method activate = GxpAuditPolicyActivationServiceImpl.class.getMethod(
                "activate", GxpAuditPolicyActivationCommand.class);
        assertEquals(Transactional.class, activate.getAnnotation(Transactional.class).annotationType());

        GxpAuditPolicyBundle bundle = new GxpAuditPolicyBundleLoader().load();
        assertEquals("gxp-audit-policy.v2", bundle.schemaVersion());
        assertTrue(bundle.policyHash().matches("[0-9a-f]{64}"));
        assertTrue(bundle.artifactHash().matches("[0-9a-f]{64}"));
    }

    @Test
    void parseOperationsMustKeepEveryOperationBlock() throws Exception {
        GxpAuditPolicyBundle bundle = new GxpAuditPolicyBundleLoader().load();
        GxpAuditPolicyActivationServiceImpl service = new GxpAuditPolicyActivationServiceImpl();
        Method parseOperations = GxpAuditPolicyActivationServiceImpl.class.getDeclaredMethod(
                "parseOperations", Long.class, GxpAuditPolicyBundle.class);
        parseOperations.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Object> operations = (List<Object>) parseOperations.invoke(service, 1L, bundle);

        assertEquals(27, operations.size(), "策略包中的每个 operation 都必须保留");
        Field operationId = operations.get(operations.size() - 1).getClass().getDeclaredField("operationId");
        operationId.setAccessible(true);
        assertEquals("gxp.policy.activate", operationId.get(operations.get(operations.size() - 1)));
    }

    @Test
    void parseOperationsMustUseYamlScalarValueForQuotedSignaturePolicy() throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String policy;
        String schema;
        try (InputStream policyStream = loader.getResourceAsStream("META-INF/gxp/gxp-audit-policy.yaml");
             InputStream schemaStream = loader.getResourceAsStream("META-INF/gxp/gxp-audit-policy.schema.json")) {
            policy = new String(policyStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceFirst("signaturePolicy: REQUIRED", "signaturePolicy: \"REQUIRED\"");
            schema = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        GxpAuditPolicyBundle bundle = new GxpAuditPolicyBundleLoader().load(policy, schema);
        GxpAuditPolicyActivationServiceImpl service = new GxpAuditPolicyActivationServiceImpl();
        Method parseOperations = GxpAuditPolicyActivationServiceImpl.class.getDeclaredMethod(
                "parseOperations", Long.class, GxpAuditPolicyBundle.class);
        parseOperations.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Object> operations = (List<Object>) parseOperations.invoke(service, 1L, bundle);
        Object approvalOperation = operations.stream()
                .filter(operation -> {
                    try {
                        Field operationId = operation.getClass().getDeclaredField("operationId");
                        operationId.setAccessible(true);
                        return "mes.pqc.review.approve".equals(operationId.get(operation));
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError(exception);
                    }
                })
                .findFirst()
                .orElseThrow();
        Field signaturePolicy = approvalOperation.getClass().getDeclaredField("signaturePolicy");
        signaturePolicy.setAccessible(true);
        Field testIds = approvalOperation.getClass().getDeclaredField("testIds");
        testIds.setAccessible(true);

        assertEquals("REQUIRED", signaturePolicy.get(approvalOperation));
        assertEquals("[\"BDD-D05-APPROVE\",\"BDD-D05-IDEMPOTENCY\",\"BDD-D05-AUDIT-FAILURE\"]",
                testIds.get(approvalOperation));
    }
}
