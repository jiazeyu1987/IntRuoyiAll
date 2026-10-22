package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundleLoader;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Pure resource contracts. No policy activation, JDBC, service calls or runtime environment changes. */
class MesEdhrRealSourceProducerPolicyContractTest {
    @Test
    void actualCompletePolicyCoversMainReleaseAndAllActualFragments() {
        JsonNode operations=new GxpAuditPolicyBundleLoader().load().policyNode().path("operations");
        JsonNode complete=null;
        for(JsonNode operation:operations) if("mes.active-order.complete".equals(operation.path("operationId").asText())) {
            assertNull(complete,"Completion policy must have exactly one operation");complete=operation;
        }
        assertNotNull(complete);assertEquals("SERVICE_METHOD",complete.path("sourceType").asText());
        assertEquals("MES",complete.path("domain").asText());
        assertEquals("ACTIVE_ORDER_COMPLETION",complete.path("subjectType").asText());
        assertEquals("COMPLETE",complete.path("actionType").asText());
        assertEquals("NONE",complete.path("signaturePolicy").asText());
        assertEquals("ABSENT_TO_PRESENT",complete.path("statePolicy").asText());
        var locators=new java.util.HashSet<String>();complete.path("sourceLocators").forEach(node->locators.add(node.asText()));
        for(String locator:List.of(
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionServiceImpl#complete",
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionServiceImpl#completeForRelease",
                "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesCompletionAffectedRowAudit#append"))
            assertTrue(locators.contains(locator),"Missing actual completion audit locator: "+locator);
    }

    @Test
    void independentProducerSchemaPreservesFrozenIdentityAndFiniteCleanup() throws Exception {
        byte[] schema;
        try(var in=getClass().getResourceAsStream("/sql/mes_edhr_completion_source_producer_tables.sql")) {
            assertNotNull(in);schema=in.readAllBytes();
        }
        // Lock the authorized R243 resource version; Git checkout line endings do not change its DDL.
        String ddl=new String(schema,StandardCharsets.UTF_8).replace("\r\n", "\n");
        byte[] canonicalSchema=ddl.getBytes(StandardCharsets.UTF_8);
        assertEquals(69010,canonicalSchema.length);
        assertEquals("0edc2f85d3c08700e6bf57a73941a4fd2f1913d52fd0290406b9cf87a9b0a3f0",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalSchema)));
        assertEquals(45,Pattern.compile("(?im)^CREATE TABLE ").matcher(ddl).results().count());
        assertEquals(81,Pattern.compile("(?im)^CREATE (?:UNIQUE )?INDEX ").matcher(ddl).results().count());
        String clean;
        try(var in=getClass().getResourceAsStream("/sql/mes_edhr_real_source_producer_clean.sql")) {
            assertNotNull(in);clean=new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
        var lines=clean.lines().filter(line->line.startsWith("DELETE FROM ")).toList();
        assertEquals(45,lines.size());
        assertEquals(45,lines.stream().map(line->line.split(" ")[2]).distinct().count());
        for(String line:lines) assertTrue(line.matches("DELETE FROM [a-z0-9_]+ WHERE (id|tenant_id) IN \\(:capturedOwnedPkList\\);"));
        assertFalse(clean.contains("TRUNCATE"));assertFalse(clean.contains("DROP TABLE"));
    }
}
