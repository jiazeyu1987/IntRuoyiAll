package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Full row preservation and transport size contract; real database persistence is covered separately. */
class MesCompletionAuditFragmentContractTest {
    @Test
    void largeCompletionRetainsAllRowsInBoundedHashLinkedAuditParts() {
        var before = rows(1140, "SUBMITTED");
        var after = rows(1140, "CONFIRMED");
        var commands = append(before, after);
        assertTrue(commands.size() > 2, "Large real-shaped row states must be partitioned, never dropped");
        var parent = commands.get(commands.size() - 1);
        var parentAfter = JsonUtils.parseTree(parent.getAfterState().getCanonicalJson());
        var manifest = parentAfter.path("affectedRowAuditManifest");
        assertEquals(commands.size() - 1, manifest.path("parts").size());
        assertEquals(DigestUtil.sha256Hex(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(JsonUtils.toJsonString(before))), manifest.path("beforeHash").asText());
        assertEquals(DigestUtil.sha256Hex(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(JsonUtils.toJsonString(after))), manifest.path("afterHash").asText());
        assertEquals(JsonUtils.parseTree(JsonUtils.toJsonString(before)), JsonUtils.parseTree(JsonUtils.toJsonString(reconstruct(commands, true))));
        assertEquals(JsonUtils.parseTree(JsonUtils.toJsonString(after)), JsonUtils.parseTree(JsonUtils.toJsonString(reconstruct(commands, false))));
        for (int i = 0; i < commands.size() - 1; i++) {
            var child = commands.get(i);
            assertEquals("mes.active-order.complete", child.getOperationId());
            assertEquals("ACTIVE_ORDER_COMPLETE:99:ROWS:" + i, child.getIdempotencyKey());
            assertTrue(mysqlEscapedAuditBytes(child) < 8 * 1024 * 1024,
                    "Conservative escaped event INSERT must stay below 8 MiB, well inside the real 64 MiB limit");
            assertEquals(i + 1, manifest.path("parts").get(i).path("eventId").asLong());
            assertEquals(commandHash(child), manifest.path("parts").get(i).path("eventHash").asText());
            assertTrue(child.getLinks().contains(new GxpAuditRelation("SOURCE", "COMPLETION_RECEIPT", "99", "3", "receipt-hash")));
            assertTrue(parent.getLinks().contains(new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", String.valueOf(i + 1), null, commandHash(child))));
        }
        assertFalse(parentAfter.has("formalSourceSnapshotJson"));
        assertFalse(parentAfter.has("signatureSnapshotJson"));
        assertEquals("receipt-hash", parentAfter.path("completionReceiptHash").asText());
        assertEquals("ACTIVE_ORDER_COMPLETE:99", parent.getIdempotencyKey());
    }

    @Test
    void emptyInsertedDeletedNullAndNestedRowsRemainReconstructable() {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("empty", List.of());
        before.put("records", List.of(new LinkedHashMap<>(Map.of("id", 1L, "raw_payload", "{\"selectedSignedAt\":null}"))));
        Map<String, Object> nullable = new LinkedHashMap<>(); nullable.put("id", 2L); nullable.put("value", null);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("empty", List.of()); after.put("records", List.of(nullable));
        var commands = append(before, after);
        assertTrue(commands.size() > 1, "Small audits use the same explicit fragment representation");
        assertEquals(JsonUtils.parseTree(JsonUtils.toJsonString(before)), JsonUtils.parseTree(JsonUtils.toJsonString(reconstruct(commands, true))));
        assertEquals(JsonUtils.parseTree(JsonUtils.toJsonString(after)), JsonUtils.parseTree(JsonUtils.toJsonString(reconstruct(commands, false))));
    }

    @Test
    void fullStateHashesCanBeRecomputedFromFragmentsWithExactDecimals() {
        Map<String,Object> row=new LinkedHashMap<>();row.put("id",1L);row.put("quantity",new BigDecimal("1.00"));
        Map<String,Object> state=new LinkedHashMap<>();state.put("records",List.of(row));
        var commands=append(state,state);
        var manifest=JsonUtils.parseTree(commands.get(commands.size()-1).getAfterState().getCanonicalJson()).path("affectedRowAuditManifest");
        String reconstructed=JsonUtils.toJsonString(reconstruct(commands,true));
        assertEquals(DigestUtil.sha256Hex(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(reconstructed)),
                manifest.path("beforeHash").asText(), "Manifest must be verifiable after JSON number/field serialization");
        assertEquals("MES_COMPLETION_SOURCE_V1",manifest.path("canonicalization").asText());
    }

    @Test
    void sourceMutationChangesManifestAndChildHash() {
        var before = rows(1, "SUBMITTED"); var after = rows(1, "CONFIRMED");
        var original = append(before, after);
        var changed = rows(1, "CONFIRMED");
        ((Map<String, Object>) ((List<?>) changed.get("records")).get(0)).put("raw_payload", "changed actual value");
        var mutated = append(before, changed);
        assertNotEquals(commandHash(original.get(0)), commandHash(mutated.get(0)));
        assertNotEquals(JsonUtils.parseTree(original.get(original.size()-1).getAfterState().getCanonicalJson()).path("affectedRowAuditManifest"),
                JsonUtils.parseTree(mutated.get(mutated.size()-1).getAfterState().getCanonicalJson()).path("affectedRowAuditManifest"));
    }

    private static List<GxpAuditCommand> append(Map<String, Object> before, Map<String, Object> after) {
        var audits = mock(GxpAuditService.class);
        List<GxpAuditCommand> result = new ArrayList<>();
        when(audits.append(any())).thenAnswer(inv -> {
            GxpAuditCommand c = inv.getArgument(0); result.add(c);
            return new GxpAuditAppendResult((long) result.size(), (long) result.size(), commandHash(c), false);
        });
        var collector = mock(MesReleaseAffectedStateCollector.class);
        when(collector.captureCompletion(10L,30L)).thenReturn(after);
        var service = new MesTeamLeaderActiveOrderCompletionServiceImpl(null,null,null,null,null,null,null);
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        ReflectionTestUtils.setField(service,"gxpAuditService",audits);
        ReflectionTestUtils.setField(service,"affectedStateCollector",collector);
        var order = MesProcessPoolActiveOrderDO.builder().id(10L).workOrderId(30L).activeStatus("ACTIVE").version(2).build();
        var receipt = MesProcessPoolActiveOrderCompletionReceiptDO.builder().id(99L).activeOrderId(10L).workOrderId(30L)
                .expectedVersion(2).completedVersion(3).receiptHash("receipt-hash").sourceSnapshotHash("source-hash")
                .formalSourceSnapshotJson("{\"source\":true}").signatureSnapshotJson("{\"signature\":true}")
                .lossQuantity(BigDecimal.ZERO).build();
        ReflectionTestUtils.invokeMethod(service,"appendCompletionGxpAudit",order,receipt,JsonUtils.toJsonString(before));
        return result;
    }

    private static Map<String,Object> reconstruct(List<GxpAuditCommand> commands, boolean before) {
        Map<String,Object> rebuilt = new LinkedHashMap<>();
        for (int i=0;i<commands.size()-1;i++) {
            var c=commands.get(i);
            var state=JsonUtils.parseTree(before ? c.getBeforeState().getCanonicalJson() : c.getAfterState().getCanonicalJson());
            state.path("affectedRows").fields().forEachRemaining(table -> {
                var rows=(List<Object>) rebuilt.computeIfAbsent(table.getKey(), key->new ArrayList<>());
                table.getValue().forEach(row->rows.add(row));
            });
        }
        return rebuilt;
    }

    private static Map<String,Object> rows(int count,String status) {
        List<Map<String,Object>> rows = new ArrayList<>();
        String body=JsonUtils.toJsonString(Map.of("qa", "标准值\\\"生产路径".repeat(650), "selectedSignedAt", "unchanged"));
        for(int i=0;i<count;i++) rows.add(new LinkedHashMap<>(Map.of("id",(long)i+1,"event_id",282240L+i,
                "tenant_id",1,"aggregation_status",status,"raw_payload",body)));
        Map<String,Object> result=new LinkedHashMap<>();result.put("records",rows);result.put("empty",List.of());return result;
    }
    private static String commandHash(GxpAuditCommand c){return DigestUtil.sha256Hex(JsonUtils.toJsonString(c));}
    private static long mysqlEscapedAuditBytes(GxpAuditCommand c) {
        var event=new GxpAuditEventDO();event.setBeforeStateJson(c.getBeforeState().getCanonicalJson());
        event.setAfterStateJson(c.getAfterState().getCanonicalJson());event.setServerOccurredAt(LocalDateTime.of(2026,10,3,2,52));
        event.setRelationManifestJson(JsonUtils.toJsonString(c.getLinks()));event.setEvidenceManifestJson(JsonUtils.toJsonString(c.getEvidences()));
        String canonical=ReflectionTestUtils.invokeMethod(new GxpAuditServiceImpl(),"canonicalEvent",event);
        long bytes=0;for(String s:List.of(event.getBeforeStateJson(),event.getAfterStateJson(),canonical,event.getRelationManifestJson(),event.getEvidenceManifestJson())){
            bytes+=s.getBytes(StandardCharsets.UTF_8).length;
            for(int i=0;i<s.length();i++)if(s.charAt(i)=='\\'||s.charAt(i)=='\''||s.charAt(i)=='"'||s.charAt(i)=='\n'||s.charAt(i)=='\r'||s.charAt(i)==0)bytes++;
        }
        return bytes+256*1024; // Conservative budget for all remaining event columns and SQL syntax.
    }
}
