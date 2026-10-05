package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Completion audit payload contract only; no DB, server, transport or business E2E proof. */
class MesCompletionAuditPayloadContractTest {
    private MesTeamLeaderActiveOrderCompletionServiceImpl service;
    private GxpAuditService audits;
    private MesReleaseAffectedStateCollector affected;
    private MesProcessPoolActiveOrderDO order;
    private Map<String, Object> beforeRows;
    private Map<String, Object> afterRows;
    private List<GxpAuditCommand> lastCommands;

    @BeforeEach
    void setUp() {
        audits = mock(GxpAuditService.class);
        when(audits.append(any())).thenReturn(new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult(
                881L, 1L, "fragment-audit-hash", false));
        affected = mock(MesReleaseAffectedStateCollector.class);
        service = new MesTeamLeaderActiveOrderCompletionServiceImpl(mock(MesProcessPoolActiveOrderMapper.class),
                mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class),
                mock(MesTeamLeaderActiveOrderCompletionProgressPort.class),
                mock(MesTeamLeaderActiveOrderCompletionBackfillPort.class),
                mock(MesTeamLeaderActiveOrderPickListCompletionSourceService.class),
                mock(MesActiveOrderTransferTraceService.class), mock(MesPqcProcessInspectionAggregationService.class));
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        ReflectionTestUtils.setField(service, "gxpAuditService", audits);
        ReflectionTestUtils.setField(service, "affectedStateCollector", affected);
        order = MesProcessPoolActiveOrderDO.builder().id(10L).workOrderId(30L)
                .activeStatus("ACTIVE").businessStatus("PRODUCING").version(2).build();
        beforeRows = affectedRows("SUBMITTED", false);
        afterRows = affectedRows("CONFIRMED", true);
        when(affected.captureCompletion(10L, 30L)).thenReturn(afterRows);
    }

    @Test
    void auditReferencesTheWholeImmutableReceiptWithoutReembeddingItsStoredSnapshots() {
        var receipt = receipt(4096);
        var command = append(receipt);
        JsonNode after = JsonUtils.parseTree(command.getAfterState().getCanonicalJson());

        assertAll(
                () -> assertFalse(after.has("formalSourceSnapshotJson"), "The formal receipt already owns this complete JSON"),
                () -> assertFalse(after.has("signatureSnapshotJson"), "The formal receipt already owns this complete JSON"),
                () -> assertEquals(receipt.getReceiptHash(), after.path("completionReceiptHash").asText()),
                () -> assertEquals(receipt.getSourceSnapshotHash(), after.path("sourceSnapshotHash").asText()),
                () -> assertTrue(command.getLinks().contains(new GxpAuditRelation("SOURCE", "COMPLETION_RECEIPT",
                        "99", "3", receipt.getReceiptHash()))),
                () -> assertTrue(command.getEvidences().contains(new GxpAuditEvidence("FORMAL_COMPLETION_RECEIPT",
                        "99", "3", receipt.getReceiptHash(), "COMPLETION"))),
                () -> assertEquals(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt), receipt.getReceiptHash())
        );
    }

    @Test
    void auditRetainsEveryBeforeAndAfterAffectedRowIncludingNestedJsonAndAggregationChanges() {
        var command = append(receipt(128));
        assertEquals(2, lastCommands.size());
        var fragment = lastCommands.get(0);
        var before = JsonUtils.parseTree(fragment.getBeforeState().getCanonicalJson());
        var after = JsonUtils.parseTree(fragment.getAfterState().getCanonicalJson());

        assertEquals(JsonUtils.parseTree(JsonUtils.toJsonString(beforeRows)), before.path("affectedRows"));
        assertEquals(JsonUtils.parseTree(JsonUtils.toJsonString(afterRows)), after.path("affectedRows"));
        assertEquals(2, after.path("affectedRows").path("mes_pqc_inspection_task").size());
        assertEquals(2, after.path("affectedRows").path("mes_pro_process_pool_pqc_record").size());
        assertEquals(2, after.path("affectedRows").path("mes_pqc_process_inspection_aggregate_detail").size());
        assertEquals(2, after.path("affectedRows").path("mes_pro_process_pool_order_process_completion").size());
        assertEquals("CONFIRMED", after.path("affectedRows").path("mes_pqc_inspection_task").get(1).path("status").asText());
        assertEquals("ACTIVE", command.getBeforeState().getState());
        assertEquals("COMPLETED", command.getAfterState().getState());
        verify(affected).captureCompletion(10L, 30L);
    }

    @Test
    void auditInsertPayloadDoesNotGrowWithAlreadyStoredReceiptSnapshotSize() {
        var small = append(receipt(64));
        var large = append(receipt(128 * 1024));
        long smallBytes = stateAndCanonicalEventBytes(small);
        long largeBytes = stateAndCanonicalEventBytes(large);

        assertEquals(smallBytes, largeBytes,
                "Before/after and the real GxP canonical event must reference the same-size receipt hash; "
                        + "snapshot copies make the audit INSERT grow despite identical affected rows");
        assertEquals(JsonUtils.parseTree(small.getAfterState().getCanonicalJson()).path("affectedRowAuditManifest"),
                JsonUtils.parseTree(large.getAfterState().getCanonicalJson()).path("affectedRowAuditManifest"));
    }

    private GxpAuditCommand append(MesProcessPoolActiveOrderCompletionReceiptDO receipt) {
        clearInvocations(audits);
        ReflectionTestUtils.invokeMethod(service, "appendCompletionGxpAudit", order, receipt,
                JsonUtils.toJsonString(beforeRows));
        var captor = ArgumentCaptor.forClass(GxpAuditCommand.class);
        verify(audits, times(2)).append(captor.capture());
        lastCommands = captor.getAllValues();
        return captor.getValue();
    }

    /** Calls the actual kernel serializer so its nested JSON escaping is part of the size contract. */
    private static long stateAndCanonicalEventBytes(GxpAuditCommand command) {
        var event = new GxpAuditEventDO();
        event.setBeforeState(command.getBeforeState().getState());
        event.setBeforeObjectVersion(command.getBeforeState().getObjectVersion());
        event.setBeforeStateJson(command.getBeforeState().getCanonicalJson());
        event.setAfterState(command.getAfterState().getState());
        event.setAfterObjectVersion(command.getAfterState().getObjectVersion());
        event.setAfterStateJson(command.getAfterState().getCanonicalJson());
        event.setServerOccurredAt(LocalDateTime.of(2026, 10, 3, 2, 52, 41));
        event.setRelationManifestJson(JsonUtils.toJsonString(command.getLinks()));
        event.setEvidenceManifestJson(JsonUtils.toJsonString(command.getEvidences()));
        String canonical = ReflectionTestUtils.invokeMethod(new GxpAuditServiceImpl(), "canonicalEvent", event);
        return utf8(event.getBeforeStateJson()) + utf8(event.getAfterStateJson()) + utf8(canonical);
    }

    private static long utf8(String value) { return value.getBytes(StandardCharsets.UTF_8).length; }

    private static MesProcessPoolActiveOrderCompletionReceiptDO receipt(int payloadSize) {
        String inner = JsonUtils.toJsonString(Map.of("value", "\\\"\\路径生产签名".repeat(payloadSize), "eventId", 282240L));
        String formal = JsonUtils.toJsonString(Map.of("sources", List.of(Map.of("rawPayload", inner))));
        String signature = JsonUtils.toJsonString(Map.of("signatureId", 12742L, "canonicalContentJson", inner));
        var receipt = MesProcessPoolActiveOrderCompletionReceiptDO.builder().id(99L).activeOrderId(10L).build();
        receipt.setWorkOrderId(30L); receipt.setExpectedVersion(2); receipt.setCompletedVersion(3);
        receipt.setCompletionStatus("SUCCESS"); receipt.setBatchRecordStatus("SUCCESS");
        receipt.setProcessInspectionStatus("SUCCESS"); receipt.setBatchRecordId(101L); receipt.setProcessInspectionId(102L);
        receipt.setLossReportStatus("NOT_REQUIRED"); receipt.setHasActualLoss(false); receipt.setLossQuantity(BigDecimal.ZERO);
        receipt.setSourceSnapshotHash("source-hash"); receipt.setFormalSourceSnapshotJson(formal);
        receipt.setSignatureSnapshotJson(signature);
        receipt.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(receipt));
        return receipt;
    }

    private static Map<String, Object> affectedRows(String status, boolean aggregated) {
        String nested = JsonUtils.toJsonString(Map.of("fieldValues", List.of(Map.of("label", "原始测量值", "value", "\\\"1.5\\\""))));
        return Map.of(
                "mes_pqc_inspection_task", List.of(Map.of("id", 11L, "status", status, "qa_snapshot_json", nested),
                        Map.of("id", 12L, "status", status, "qa_snapshot_json", nested)),
                "mes_pro_process_pool_pqc_record", List.of(Map.of("id", 21L, "event_id", 101L, "raw_payload", nested),
                        Map.of("id", 22L, "event_id", 102L, "raw_payload", nested)),
                "mes_pqc_process_inspection_aggregate_detail", List.of(Map.of("id", 31L, "aggregated", aggregated, "payload", nested),
                        Map.of("id", 32L, "aggregated", aggregated, "payload", nested)),
                "mes_pro_process_pool_order_process_completion", List.of(Map.of("id", 41L, "completed", aggregated),
                        Map.of("id", 42L, "completed", aggregated)));
    }
}
