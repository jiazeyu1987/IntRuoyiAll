package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolProductionReportCorrectionService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolPqcInspectionCorrectionService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesCorrectionSignatureEvidenceReaderTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void immutableHistoricalRevisionReadsFrozenSignerWithoutCurrentAccountOrCurrentPayload() {
        var f = new Fixture();
        f.event.setRawPayload("{\"outputQuantity\":99}");
        var read = f.reader.read(f.event, f.revision);
        assertEquals("冻结本人", read.signerName()); assertEquals(11L, read.evidence().id());
        assertEquals("10", read.changes().get(0).getBeforeValue()); assertEquals("11", read.changes().get(0).getAfterValue());
        assertEquals("VALID", read.verification().verificationStatus());
        verify(f.diffs, never()).insert(any(MesProProcessPoolEventRevisionDiffDO.class));
        verify(f.audits, never()).insert(any(GxpAuditEventDO.class));
    }
    @ParameterizedTest @ValueSource(strings = {"tenant", "event", "actor", "snapshot", "missingSignature", "canonical", "hash", "auditTenant", "auditSource", "auditSignature", "auditPayload", "duplicateAudit", "diffTenant", "diffEvent", "noDiff", "unchanged", "challenge", "duplicateDiff", "simulated"})
    void rejectsBrokenFormalAssociation(String broken) {
        var f = new Fixture();
        switch (broken) {
            case "tenant" -> f.revision.setTenantId(2L);
            case "event" -> f.revision.setEventId(888L);
            case "actor" -> f.revision.setModifiedByUserId(999L);
            case "snapshot" -> f.revision.setRevisionSignatureSnapshot("{}");
            case "missingSignature" -> when(f.signatures.getById(11L)).thenReturn(null);
            case "canonical" -> f.stubEvidence("{\"actionType\":\"PQC_SUBMIT\"}");
            case "hash" -> when(f.signatures.verifyEvidence(11L)).thenReturn(new ElectronicSignatureVerificationDTO(11L,"VALID","content","changed","evidence","evidence","SHA-256","1"));
            case "auditTenant" -> f.audit.setTenantId(2L);
            case "auditSource" -> f.audit.setSourceLocator("another.writer#correct");
            case "auditSignature" -> f.audit.setSignatureRecordId("12");
            case "auditPayload" -> f.revision.setAfterPayload("{\"outputQuantity\":12}");
            case "duplicateAudit" -> when(f.audits.selectList(any())).thenReturn(List.of(f.audit,f.audit));
            case "diffTenant" -> f.diff.setTenantId(2L);
            case "diffEvent" -> f.diff.setEventId(888L);
            case "noDiff" -> when(f.diffs.selectListByRevisionIds(List.of(10L))).thenReturn(List.of());
            case "unchanged" -> f.revision.setBeforePayload(f.revision.getAfterPayload());
            case "challenge" -> f.audit.setEvidenceManifestJson(f.audit.getEvidenceManifestJson().replace(f.challenge, "b".repeat(64)));
            case "duplicateDiff" -> when(f.diffs.selectListByRevisionIds(List.of(10L))).thenReturn(List.of(f.diff,f.diff));
            case "simulated" -> f.event.setSimulated(true);
            default -> throw new AssertionError(broken);
        }
        var failure = assertThrows(IllegalStateException.class, () -> f.reader.require(f.event, f.revision));
        assertTrue(failure.getMessage().startsWith("CORRECTION_EVIDENCE_INVALID:"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void canonicalChallengeSurvivesPersistedObjectKeyOrderAndWhitespace(boolean pqc) {
        String signed = "{\"z\":3,\"a\":2,\"outputQuantity\":11,\"ordered\":[1,2],\"meta\":{\"zz\":\"中文\",\"a\":true}}";
        var f = new Fixture(signed, pqc);
        String persisted = "{\n  \"meta\": {\"a\": true, \"zz\": \"中文\"}, \"ordered\": [1, 2],"
                + "\n  \"outputQuantity\": 11, \"a\": 2, \"z\": 3\n}";
        assertNotEquals(signed, persisted);
        assertEquals(JsonUtils.parseTree(signed), JsonUtils.parseTree(persisted));
        f.revision.setAfterPayload(persisted);

        assertEquals("冻结本人", f.reader.read(f.event, f.revision).signerName());
        assertEquals(f.challenge, MesProBatchRecordExecutionFieldAuditHasher.sha256("1|"
                + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(persisted) + "|核对记录"));
        verify(f.signatures).verifyEvidence(11L);
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("canonicalPayloadChanges")
    void canonicalChallengeRejectsRealJsonFactChangesEvenWithMatchingPayloadAudit(boolean pqc, String change) {
        var f = new Fixture("{\"z\":3,\"a\":2,\"outputQuantity\":11,\"ordered\":[1,2]}", pqc);
        var changed = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(f.revision.getAfterPayload());
        switch (change) {
            case "VALUE" -> changed.put("z", 4);
            case "TYPE" -> changed.put("z", "3");
            case "ARRAY_ORDER" -> changed.set("ordered", JsonUtils.parseTree("[2,1]"));
            case "MISSING" -> changed.remove("a");
            case "EXTRA" -> changed.put("unrequestedFact", true);
            case "NULL" -> changed.putNull("z");
            default -> throw new AssertionError(change);
        }
        String oldPayloadHash = MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(f.revision.getAfterPayload());
        f.revision.setAfterPayload(changed.toString());
        f.audit.setEvidenceManifestJson(f.audit.getEvidenceManifestJson().replace(oldPayloadHash,
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(f.revision.getAfterPayload())));

        var failure = assertThrows(IllegalStateException.class, () -> f.reader.require(f.event, f.revision));
        assertTrue(failure.getMessage().contains("修订签名挑战与正式修订正文不一致"));
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> canonicalPayloadChanges() {
        return java.util.stream.Stream.of(false, true).flatMap(pqc ->
                java.util.stream.Stream.of("VALUE", "TYPE", "ARRAY_ORDER", "MISSING", "EXTRA", "NULL")
                        .map(change -> org.junit.jupiter.params.provider.Arguments.of(pqc, change)));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void rejectsUncanonicalByteChallengeWithoutCompatibilityPath(boolean pqc) {
        String signed = "{\"z\":3,\"a\":2,\"outputQuantity\":11}";
        var f = new Fixture(signed, pqc);
        String rawChallenge = MesProBatchRecordExecutionFieldAuditHasher.sha256("1|" + signed + "|核对记录");
        assertNotEquals(f.challenge, rawChallenge);
        f.audit.setEvidenceManifestJson(f.audit.getEvidenceManifestJson().replace(f.challenge, rawChallenge));
        var failure = assertThrows(IllegalStateException.class, () -> f.reader.require(f.event, f.revision));
        assertTrue(failure.getMessage().contains("修订签名挑战与正式修订正文不一致"));
    }

    static class Fixture {
        final LocalDateTime time = LocalDateTime.of(2026,10,5,2,0);
        final MesProProcessPoolEventRevisionDiffMapper diffs = mock(MesProProcessPoolEventRevisionDiffMapper.class);
        final ElectronicSignatureQueryService signatures = mock(ElectronicSignatureQueryService.class);
        final GxpAuditEventMapper audits = mock(GxpAuditEventMapper.class);
        final MesCorrectionSignatureEvidenceReader reader = new MesCorrectionSignatureEvidenceReader(diffs,signatures,audits);
        final MesProProcessPoolEventDO event = new MesProProcessPoolEventDO().setId(1L).setPoolId(2L).setWorkOrderId(3L)
                .setRouteId(4L).setRouteProcessId(5L).setProcessId(6L).setFeedbackSourceId(31L).setEventType("PRODUCTION_SUBMIT");
        final MesProProcessPoolEventRevisionDO revision = new MesProProcessPoolEventRevisionDO().setId(10L).setEventId(1L)
                .setPoolId(2L).setWorkOrderId(3L).setRouteId(4L).setRouteProcessId(5L).setProcessId(6L)
                .setBeforePayload("{\"outputQuantity\":10}").setAfterPayload("{\"outputQuantity\":11}")
                .setRevisionStatus("EFFECTIVE").setServerRevisionTime(time).setModifiedByUserId(7L).setRevisionSignatureUserId(7L)
                .setRevisionSignatureId(11L).setChangeReason("核对记录");
        final MesProProcessPoolEventRevisionDiffDO diff = new MesProProcessPoolEventRevisionDiffDO().setId(20L)
                .setEventId(1L).setRevisionId(10L).setFieldCode("outputQuantity").setFieldName("完成数量").setBeforeValue("10").setAfterValue("11");
        final GxpAuditEventDO audit = new GxpAuditEventDO();
        final String challenge;
        final String subject;
        Fixture() { this("{\"outputQuantity\":11}", false); }
        Fixture(String afterPayload, boolean pqc) {
            revision.setAfterPayload(afterPayload);
            var before = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(afterPayload);
            before.put("outputQuantity", 10);
            revision.setBeforePayload(before.toString());
            challenge = MesProBatchRecordExecutionFieldAuditHasher.sha256("1|"
                    + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(afterPayload) + "|核对记录");
            subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L,"FIELD_CHANGE",null,null,null,null,null,null,null,null,null,null,null,null,null,null,challenge);
            TenantContextHolder.setTenantId(1L); event.setTenantId(1L); revision.setTenantId(1L); diff.setTenantId(1L);
            revision.setRevisionSignatureSnapshot(JsonUtils.toJsonString(new MesProBatchRecordExecutionFieldAuditSignatureResult()
                    .setSignatureId(11L).setActorId(7L).setActorName("冻结本人").setSignedAt(time)));
            when(diffs.selectListByRevisionIds(List.of(10L))).thenReturn(List.of(diff));
            var canonical = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(7L,"MES","FIELD_CHANGE","MES_BATCH_RECORD",subject,MesProBatchRecordExecutionFieldAuditHasher.sha256(subject),"核对记录")).canonicalContentJson();
            stubEvidence(canonical);
            when(signatures.verifyEvidence(11L)).thenReturn(new ElectronicSignatureVerificationDTO(11L,"VALID","content","content","evidence","evidence","SHA-256","1"));
            audit.setId(40L);audit.setTenantId(1L);audit.setOperationId("mes.production-report.correct");audit.setSubjectId("MES_PROCESS_POOL_EVENT:1");
            audit.setRequestId("MES-PRODUCTION-CORRECTION:10");audit.setActorId(7L);audit.setSignatureRecordId("11");audit.setSignatureContentHash("content");
            audit.setReason("核对记录");audit.setReasonSource("USER");audit.setReasonCode("MES_PRODUCTION_REPORT_CORRECT");audit.setResultStatus("SUCCESS");audit.setEventSchemaVersion(2);
            audit.setSourceType("SERVICE_METHOD");audit.setSourceLocator(MesProcessPoolProductionReportCorrectionService.class.getName()+"#correct");
            audit.setEvidenceManifestJson(JsonUtils.toJsonString(List.of(new GxpAuditEvidence("SIGNATURE","11",null,"content","PRODUCTION_REPORT_CORRECTION"),
                    new GxpAuditEvidence("SIGNATURE_CHALLENGE","11",null,challenge,"SIGNED_CORRECTION_REQUEST"),
                    new GxpAuditEvidence("REVISION","10",null,MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()),"CORRECTED_PAYLOAD"))));
            audit.setRelationManifestJson(JsonUtils.toJsonString(List.of(new GxpAuditRelation("SUBJECT","PROCESS_POOL_EVENT","1",null,null),
                    new GxpAuditRelation("SOURCE","WORK_ORDER","3",null,null),new GxpAuditRelation("SOURCE","FEEDBACK","31",null,null),
                    new GxpAuditRelation("REVISION","REVISION","10",null,null),new GxpAuditRelation("SIGNATURE","SIGNATURE","11",null,null))));
            when(audits.selectList(any())).thenReturn(List.of(audit));
            if (pqc) pqc();
        }
        void stubEvidence(String canonical) {
            when(signatures.getById(11L)).thenReturn(new ElectronicSignatureEvidenceDTO(11L,"MES","FIELD_CHANGE","MES_BATCH_RECORD",subject,
                    MesProBatchRecordExecutionFieldAuditHasher.sha256(subject),7L,"CHANGE","更正","核对记录",time,null,"PASSWORD","content","evidence","SHA-256","1","1","VALID",
                    null,null,null,null,canonical,null,null,null,"当前名字不同"));
        }
        void pqc() {
            event.setEventType("PQC_INSPECTION");
            audit.setOperationId("mes.pqc-inspection.correct");
            audit.setRequestId("MES-PQC-CORRECTION:10");
            audit.setReasonCode("MES_PQC_INSPECTION_CORRECT");
            audit.setSourceLocator(MesProcessPoolPqcInspectionCorrectionService.class.getName()+"#correct");
            audit.setEvidenceManifestJson(audit.getEvidenceManifestJson().replace("PRODUCTION_REPORT_CORRECTION", "PQC_INSPECTION_CORRECTION"));
            audit.setRelationManifestJson(audit.getRelationManifestJson().replace("FEEDBACK", "PQC_INSPECTION_TASK"));
        }
    }
}
