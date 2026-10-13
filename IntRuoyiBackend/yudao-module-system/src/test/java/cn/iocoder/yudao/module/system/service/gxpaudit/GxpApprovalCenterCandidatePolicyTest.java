package cn.iocoder.yudao.module.system.service.gxpaudit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** The complete release input is parsed by the real loader; this does not approve or activate it. */
class GxpApprovalCenterCandidatePolicyTest {
    @Test void completeDraftPreservesApprovedOperationsAndAddsExactNewWriterSources() throws Exception {
        String candidate=Files.readString(Path.of("../config/gxp-audit-policy-approval-center-candidate.yaml"),StandardCharsets.UTF_8);
        String schema=resource("META-INF/gxp/gxp-audit-policy.schema.json");
        var loader=new GxpAuditPolicyBundleLoader();
        var draft=loader.load(candidate,schema);
        var baseline=loader.load(resource("META-INF/gxp/gxp-audit-policy.yaml"),schema);
        assertFalse(draft.approved());assertEquals("DRAFT",draft.status());
        assertNotEquals(baseline.policyVersion(),draft.policyVersion());
        assertNotEquals(baseline.approvalReference(),draft.approvalReference());
        assertEquals(baseline.policyNode().get("coverageScope"),draft.policyNode().get("coverageScope"));
        var operations=new HashMap<String,com.fasterxml.jackson.databind.JsonNode>();
        draft.policyNode().get("operations").forEach(o -> assertNull(operations.put(o.path("operationId").asText(),o)));
        for(var original:baseline.policyNode().get("operations")) {
            var current=(ObjectNode)operations.get(original.path("operationId").asText()).deepCopy();
            original.get("sourceLocators").forEach(source -> assertTrue(current.get("sourceLocators").toString().contains(source.asText())));
            current.set("sourceLocators",original.get("sourceLocators"));assertEquals(original,current);
        }
        Set<String> added=new HashSet<>(operations.keySet());baseline.policyNode().get("operations").forEach(o -> added.remove(o.path("operationId").asText()));
        assertEquals(Set.of("mes.independent-batch-prerequisite-receipt.issue","mes.independent-batch-prerequisite-receipt.revoke",
                "mes.edhr.material-gate.receipt.create","mes.market-release.manager-stage.initialize","APPROVAL_CENTER_REVIEW_COMMIT"),added);
        assertEquals("REQUIRED",operations.get("APPROVAL_CENTER_REVIEW_COMMIT").path("signaturePolicy").asText());
        assertTrue(operations.get("signature.record.create").path("sourceLocators").toString().contains("ElectronicSignatureTransactionExecutor.executeAuthenticated"));
        assertTrue(operations.get("mes.production.submit").path("sourceLocators").toString().contains("MesProFrontlineFeedbackSubmitServiceImpl#submitAuthenticated"));
    }
    private static String resource(String path) throws Exception {
        try(var in=new ClassPathResource(path).getInputStream()) {return new String(in.readAllBytes(),StandardCharsets.UTF_8);}
    }
}
