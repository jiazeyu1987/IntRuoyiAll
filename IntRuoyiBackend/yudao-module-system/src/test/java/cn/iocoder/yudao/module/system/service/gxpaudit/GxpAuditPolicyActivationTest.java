package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GxpAuditPolicyActivationTest {

    @Test
    void buildMustExposeOneApprovedV2PolicyBundleAndSchema() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream policy = loader.getResourceAsStream("META-INF/gxp/gxp-audit-policy.yaml");
             InputStream schema = loader.getResourceAsStream("META-INF/gxp/gxp-audit-policy.schema.json")) {
            assertNotNull(policy, "唯一运行策略必须打包到 META-INF/gxp");
            assertNotNull(schema, "策略 schema 必须与唯一运行策略一起打包");
            String policyText = new String(policy.readAllBytes(), StandardCharsets.UTF_8);
            String schemaText = new String(schema.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(policyText.contains("schemaVersion: gxp-audit-policy.v2"));
            assertTrue(schemaText.contains("\"$id\": \"https://int.ruoyi.local/schema/gxp-audit-policy.v2\""));
            assertTrue(schemaText.contains("\"schemaVersion\": { \"const\": \"gxp-audit-policy.v2\" }"));
            assertTrue(schemaText.contains("\"subjectType\""));
            assertTrue(schemaText.contains("\"actionType\""));
            assertTrue(schemaText.contains("\"applicability\""));
            assertTrue(schemaText.contains("\"writeBoundaryScan\""));
        }
    }
}
