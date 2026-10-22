package cn.iocoder.yudao.module.system.service.gxpaudit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GxpAuditManagerStagePolicyTest {
    @Test
    void approvedPackagedPolicyRegistersTheActualManagerStageWriter() {
        var bundle = new GxpAuditPolicyBundleLoader().load();
        assertTrue(bundle.approved());
        var operations = bundle.policyNode().path("operations");
        var matches = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        operations.forEach(operation -> {
            if ("mes.market-release.manager-stage.initialize".equals(operation.path("operationId").asText())) {
                matches.add(operation);
            }
        });
        assertEquals(1, matches.size());
        var operation = matches.get(0);
        assertEquals("SERVICE_METHOD", operation.path("sourceType").asText());
        assertEquals(1, operation.path("sourceLocators").size());
        assertEquals("cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager."
                + "MesProductionReleaseManagerStageInitializerImpl#initializeManagerReleaseStage",
                operation.path("sourceLocators").get(0).asText());
        assertEquals("RELEASE_TRANSACTION", operation.path("subjectType").asText());
        assertEquals("INITIALIZE_MANAGER_STAGE", operation.path("actionType").asText());
        assertEquals("SYSTEM", operation.path("reasonPolicy").asText());
        assertEquals("NONE", operation.path("signaturePolicy").asText());
        assertEquals("ABSENT_OR_PRESENT_TO_PRESENT", operation.path("statePolicy").asText());
        assertEquals("MARKET_RELEASE", operation.path("snapshotProfile").asText());
        assertEquals("GXP", operation.path("applicability").asText());
    }
}
