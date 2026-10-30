package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionReceiptHash;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceOwnerIntegrity.sha;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Protocol support fixtures from their first construction, never real MES producer evidence.
 * Only mapper query inputs are substituted; shared, owner, store, registry and codec are actual.
 * No completion main/part command is fabricated. Original joint gate #6 remains HARD_NOT_VERIFIED.
 */
class MesEdhrSourceCreationVerificationServiceTest {
    private MesEdhrSourceCreationBindingDO materialBinding;
    private MesProEdhrMaterialGateReceiptDO material;
    private MesProEdhrMaterialGateReceiptMapper materials;
    private MesEdhrSourceCreationBindingMapper bindingMapper;
    private GxpAuditEventMapper events;
    private GxpAuditEventRelationMapper relationMapper;
    private MesEdhrSourceCreationVerificationService service;
    private MesEdhrSourceOwnerIntegrity owner;
    private GxpCreationAuditVerificationService shared;
    private final Map<Long, GxpAuditEventDO> eventRows = new HashMap<>();
    private final Map<Long, List<GxpAuditRelation>> links = new HashMap<>();

    @BeforeAll static void processIdentity() {
        System.out.println("AC02_PROCESS class=" + MesEdhrSourceCreationVerificationServiceTest.class.getName()
                + " pid=" + ProcessHandle.current().pid() + " jvmStartTime="
                + java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
    }
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L); TenantContextHolder.setIgnore(false);
        materials = mock(MesProEdhrMaterialGateReceiptMapper.class); bindingMapper = mock(MesEdhrSourceCreationBindingMapper.class);
        events = mock(GxpAuditEventMapper.class); relationMapper = mock(GxpAuditEventRelationMapper.class);
        shared = new GxpCreationAuditVerificationService();
        // Wiring query ports only, never overriding requireVerifiedEvent or constructing VALID.
        ReflectionTestUtils.setField(shared, "auditEventMapper", events);
        ReflectionTestUtils.setField(shared, "eventRelationMapper", relationMapper);
        owner = new MesEdhrSourceOwnerIntegrity();
        service = new MesEdhrSourceCreationVerificationService(materials, mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class), owner,
                new MesEdhrSourceCreationBindingStore(bindingMapper), shared, new MesEdhrSourceCreationProtocolRegistry());
        ReflectionTestUtils.setField(service,"managedAssociations",mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationMapper.class));
        ReflectionTestUtils.setField(service,"submissionAssociations",mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrReleaseSubmissionMaterialAssociationMapper.class));
        material = new MesProEdhrMaterialGateReceiptDO(); material.setId(91L); material.setTenantId(1L);
        material.setBatchExecutionId(92L); material.setReceiptId("TEST_ONLY_MATERIAL"); material.setVersion(1);
        material.setIssuedBy(7L); material.setGateStatus("MATERIALS_READY"); material.setAuditEventId("TEST_ONLY_LEGACY_TOKEN");
        material.setMaterialTypeKeysJson("[\"FINISHED_PRODUCT_INSPECTION_RECORD\",\"FINISHED_PRODUCT_INSPECTION_REPORT\",\"INCOMING_INSPECTION_REPORT\",\"STERILIZATION_REPORT\"]");
        material.setManifestHash("a".repeat(64)); material.setSourceSnapshotHash("b".repeat(64)); material.setMaterialVersionSetHash("c".repeat(64));
        material.setReceiptHash(sha("1|92|TEST_ONLY_MATERIAL|MATERIALS_READY|" + material.getMaterialTypeKeysJson() + "|"
                + material.getManifestHash() + "|" + material.getSourceSnapshotHash() + "|" + material.getMaterialVersionSetHash() + "|7|TEST_ONLY_LEGACY_TOKEN|1"));
        when(materials.selectByReceiptId(1L, 92L, "TEST_ONLY_MATERIAL")).thenReturn(material);
        when(events.selectByTenantIdAndId(eq(1L), anyLong())).thenAnswer(i -> eventRows.get(i.getArgument(1)));
        when(relationMapper.selectListByTenantIdAndEventId(eq(1L), anyLong())).thenAnswer(i -> physical(i.getArgument(1)));
        installCreation();
    }
    @AfterEach void clear() { TenantContextHolder.clear(); eventRows.clear(); links.clear(); }

    @Test void developmentSkipsAuditPersistenceButRejectsCorruptedBusinessReceipt() {
        var env = new org.springframework.mock.env.MockEnvironment()
                .withProperty("spring.datasource.dynamic.datasource.master.url", "jdbc:mysql://127.0.0.1/test");
        env.setActiveProfiles("local");
        try {
            new GxpAuditDevelopmentMode(env, cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.DEVELOPMENT_DISABLED);
            clearInvocations(events, relationMapper, bindingMapper);
            var proof = read();
            assertEquals(-91L, proof.creationEventId());
            assertEquals(GxpAuditDevelopmentMode.STATUS, proof.creationEventHash());
            assertEquals(material.getReceiptHash(), proof.sourceHash());
            verifyNoInteractions(events, relationMapper, bindingMapper);
            material.setManifestHash("d".repeat(64));
            assertThrows(IllegalStateException.class, this::read);
            verifyNoInteractions(events, relationMapper, bindingMapper);
        } finally { new GxpAuditDevelopmentMode(new org.springframework.mock.env.MockEnvironment(), cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode.FORMAL); }
    }

    private MesEdhrSourceCreationEvidence read() { return service.requireMaterialCreation(1L, 92L, "TEST_ONLY_MATERIAL"); }
    private GxpAuditRelation materialLink() { return new GxpAuditRelation("SOURCE", "EDHR_MATERIAL_GATE_RECEIPT", "91", "1", material.getReceiptHash()); }
    private void installCreation() {
        ObjectNode request = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1")
                .put("requestKind", "MATERIAL_READY_REQUEST_V1").put("tenantId", 1L).put("batchExecutionId", 92L)
                .put("version", 1).put("issuedBy", 7L).put("sourceSnapshotHash", material.getSourceSnapshotHash())
                .put("manifestHash", material.getManifestHash()).put("materialVersionSetHash", material.getMaterialVersionSetHash());
        request.set("materialTypeKeys", MesEdhrSourceOwnerIntegrity.strict(material.getMaterialTypeKeysJson()));
        var encoded = MesEdhrSourceCreationCodec.encodeMaterialReadyRequestV1(request);
        request = MesEdhrSourceCreationCodec.decodeMaterialReadyRequestV1(encoded.utf8Bytes());
        ObjectNode state = JsonNodeFactory.instance.objectNode().put("codecVersion", "MES-OC-JSON-V1").put("stateKind", "MATERIAL_READY_STATE_V1")
                .put("sourceRowId", 91L).put("receiptId", material.getReceiptId()).put("gateStatus", "MATERIALS_READY")
                .put("materialTypeKeysJsonRaw", material.getMaterialTypeKeysJson()).put("legacyOwnerAuditToken", material.getAuditEventId())
                .put("receiptHash", material.getReceiptHash()).put("requestHashVersion", "MATERIAL_READY_REQUEST_V1").put("requestHash", encoded.sha256());
        state.set("request", request);
        String key = MesEdhrSourceCreationCodec.materialCreateAuditKey(1L, 92L, 1);
        var event = base(200L, "mes.edhr.material-gate.receipt.create", "EDHR_MATERIAL_GATE_RECEIPT", "CREATE",
                "cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesReleaseMaterialGateReceiptPortImpl#persistReady");
        event.setSubjectId("MATERIAL_GATE_RECEIPT:TEST_ONLY_MATERIAL"); event.setBeforeState("ABSENT"); event.setAfterState("MATERIALS_READY");
        event.setBeforeStateJson("{}"); event.setAfterStateJson(new String(MesEdhrSourceCreationCodec.encodeMaterialReadyStateV1(state).utf8Bytes(), java.nio.charset.StandardCharsets.UTF_8));
        event.setReason("四材料门禁就绪回执创建"); event.setReasonCode("MES_EDHR_MATERIALS_READY");
        event.setIdempotencyKey(key); event.setRequestId(key);
        storeEvent(event, List.of(materialLink()), List.of(new GxpAuditEvidence("MATERIAL_GATE_RECEIPT", material.getReceiptId(), "1", material.getReceiptHash(), "CREATION")));
        var row = new MesEdhrSourceCreationBindingDO().setId(300L).setTenantId(1L).setOwnerProtocol("MATERIAL_READY_V1")
                .setSourceRowId(91L).setSourceReceiptId(material.getReceiptId()).setSourceVersion("1").setSourceHash(material.getReceiptHash())
                .setRequestHashVersion("MATERIAL_READY_REQUEST_V1").setRequestHash(encoded.sha256()).setCreationEventId(200L).setCreationEventHash(event.getEventHash());
        when(bindingMapper.selectBySourceRow(1L, "MATERIAL_READY_V1", 91L)).thenReturn(List.of(row));
        when(bindingMapper.selectByReceiptVersion(1L, "MATERIAL_READY_V1", material.getReceiptId(), "1")).thenReturn(List.of(row));
        when(bindingMapper.selectByCreationEvent(1L, 200L)).thenReturn(List.of(row));
        materialBinding = row;
        candidates(200L);
    }
    private GxpAuditEventDO base(long id, String operation, String subject, String action, String locator) {
        var e = new GxpAuditEventDO(); e.setId(id); e.setTenantId(1L); e.setLedgerSequence(id - 199);
        e.setOperationId(operation); e.setDomain("MES"); e.setSubjectType(subject); e.setAction(action); e.setSubjectVersion("1");
        e.setBeforeObjectVersion("1"); e.setAfterObjectVersion("1"); e.setActorId(7L); e.setActorUsername("TEST_ONLY_A"); e.setActorDisplayName("TEST_ONLY_A");
        e.setServerOccurredAt(LocalDateTime.of(2026, 10, 10, 0, 0)); e.setPolicyVersion("TEST_ONLY_POLICY"); e.setIdempotencyPayloadHash("d".repeat(64));
        e.setAlgorithm("SHA-256"); e.setEventSchemaVersion(2); e.setCanonicalizationVersion("GXP_CANONICAL_V2"); e.setResultStatus("SUCCESS");
        e.setReasonSource("SYSTEM"); e.setTransactionId("TEST_ONLY_SUPPORT_TX");
        e.setAuthenticatedActorJson(JsonUtils.toJsonString(Map.of("actorId", 7L, "tenantId", 1L)));
        e.setPerformedByJson(e.getAuthenticatedActorJson()); e.setSourceType("SERVICE_METHOD"); e.setSourceLocator(locator); return e;
    }
    private void storeEvent(GxpAuditEventDO e, List<GxpAuditRelation> relations, List<GxpAuditEvidence> evidence) {
        e.setRelationManifestJson(JsonUtils.toJsonString(relations)); e.setEvidenceManifestJson(JsonUtils.toJsonString(evidence));
        rehash(e); eventRows.put(e.getId(), e); links.put(e.getId(), relations);
    }
    private void rehash(GxpAuditEventDO e) {
        e.setStatePayloadHash(sha(e.getBeforeStateJson() + "\u001f" + e.getAfterStateJson()));
        TreeMap<String, Object> raw = new TreeMap<>(); var bean = new BeanWrapperImpl(e);
        for (var field : GxpCreationAuditVerificationService.ProtectedColumns.class.getRecordComponents()) {
            Object value = bean.getPropertyValue(field.getName());
            raw.put(field.getName(), field.getName().equals("serverOccurredAt") ? e.getServerOccurredAt().toString() : value);
        }
        e.setCanonicalEventJson(JsonUtils.toJsonString(raw)); e.setEventHash(sha(e.getCanonicalEventJson()));
    }
    private List<GxpAuditEventRelationDO> physical(long event) {
        List<GxpAuditEventRelationDO> result = new ArrayList<>(); long id = event * 100;
        for (var relation : links.get(event)) {
            var row = new GxpAuditEventRelationDO(); row.setId(++id); row.setTenantId(1L); row.setEventId(event);
            row.setRelationType(relation.relationType()); row.setTargetType(relation.objectType()); row.setTargetId(relation.objectId());
            row.setTargetVersion(relation.objectVersion()); row.setTargetHash(relation.objectHash()); result.add(row);
        }
        return result;
    }
    private void candidates(Long... ids) { when(relationMapper.selectEventIdsByTargets(1L, List.of("EDHR_MATERIAL_GATE_RECEIPT"), "91")).thenReturn(List.of(ids)); }

    @Test void uniqueMaterialCreationReturnsSevenImmutableHistoricalScalars() {
        var proof = read(); assertEquals(new MesEdhrSourceCreationEvidence("MATERIAL_READY_V1", 91L, material.getReceiptId(), "1",
                material.getReceiptHash(), 200L, eventRows.get(200L).getEventHash()), proof);
        assertEquals(7, MesEdhrSourceCreationEvidence.class.getRecordComponents().length);
    }
    @Test void zeroRelatedEventsAreValidHistoricalCreation() { assertEquals(200L, read().creationEventId()); }
    @ParameterizedTest @ValueSource(strings = {"operation", "locator", "domain", "subject", "action", "sourceType"})
    void closedRegistryRejectsUnknownCandidateAfterActualSharedVerification(String field) {
        var e = base(201L, "mes.market-release.submit", "RELEASE_TRANSACTION", "SUBMIT",
                "cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl#submit");
        e.setSubjectId("RELEASE_TRANSACTION:21"); e.setBeforeState("PRECHECK_PASSED"); e.setAfterState("PENDING_APPROVAL");
        e.setBeforeStateJson("{}"); e.setAfterStateJson("{}"); e.setReason("TEST_ONLY_EFFECTIVE_REASON"); e.setReasonCode("MES_RELEASE_PREPARATION");
        e.setRequestId("key"); e.setIdempotencyKey("RELEASE_PREP:" + "a".repeat(64));
        switch (field) { case "operation" -> e.setOperationId("unknown"); case "locator" -> e.setSourceLocator("wrong#method");
            case "domain" -> e.setDomain("OTHER"); case "subject" -> e.setSubjectType("OTHER"); case "action" -> e.setAction("OTHER"); case "sourceType" -> e.setSourceType("OTHER"); }
        storeEvent(e, List.of(materialLink()), List.of()); candidates(200L, 201L);
        assertEquals("UNKNOWN_CANDIDATE_PROTOCOL", assertThrows(IllegalStateException.class, this::read).getMessage());
        verify(events).selectByTenantIdAndId(1L, 201L);
    }
    @Test void sharedProtectedColumnContradictionPreventsClassification() {
        eventRows.get(200L).setActorUsername("tampered");
        assertEquals("AUDIT_INTEGRITY_FAILED", assertThrows(IllegalStateException.class, this::read).getMessage());
    }
    private GxpAuditEventDO managed(boolean unicode) {
        String escapedKey = unicode ? "中😀\\u0000:\\n\\\\\\\"" : "key";
        String request = "{\"applicationId\":11,\"batchExecutionId\":92,\"codecVersion\":\"MES-OC-JSON-V1\","
                + "\"initializedApplicationVersion\":1,\"reportEvidenceOwnerHashes\":[],\"reportSnapshotHash\":\"TEST_ONLY_REPORT\","
                + "\"sourceActorId\":7,\"sourceExpectedApplicationVersion\":0,\"sourceIdempotencyKey\":\"" + escapedKey
                + "\",\"sourceMethod\":\"MANAGED_PQC_APPROVE\",\"sourcePayloadHash\":\"" + "f".repeat(64)
                + "\",\"sourceWorkTaskId\":12,\"tenantId\":1}";
        // Independently frozen literal ASCII key order and escape vector, not the reader encoder.
        String p = "{\"applicationId\":11,\"associationId\":\"12345678-1234-1234-1234-123456789abc\","
                + "\"associationProtocol\":\"MANAGED_HANDOFF_MATERIAL_ASSOCIATION_V1\",\"associationVersion\":1,\"batchExecutionId\":92,"
                + "\"businessReadinessSnapshotHash\":\"" + sha("TEST_ONLY_RAW") + "\",\"businessReadinessSnapshotJsonRaw\":\"TEST_ONLY_RAW\","
                + "\"codecVersion\":\"MES-OC-JSON-V1\",\"handoffRequestHash\":\"" + sha(request) + "\",\"initializedApplicationVersion\":1,"
                + "\"managerCandidateSnapshotHash\":\"TEST_ONLY_MANAGER\",\"managerWorkTaskId\":13,\"materialCreationEventHash\":\"" + eventRows.get(200L).getEventHash()
                + "\",\"materialCreationEventId\":200,\"materialManifestHash\":\"" + material.getManifestHash() + "\",\"materialReceiptHash\":\"" + material.getReceiptHash()
                + "\",\"materialReceiptId\":\"TEST_ONLY_MATERIAL\",\"materialSourceRowId\":91,\"materialVersion\":1,\"precheckSnapshotHash\":\"" + "e".repeat(64)
                + "\",\"releaseTransactionId\":21,\"releaseTransactionVersion\":1,\"reportSnapshotHash\":\"TEST_ONLY_REPORT\",\"sourceActorId\":7,"
                + "\"sourceExpectedApplicationVersion\":0,\"sourceIdempotencyKey\":\"" + escapedKey + "\",\"sourceMethod\":\"MANAGED_PQC_APPROVE\","
                + "\"sourcePayloadHash\":\"" + "f".repeat(64) + "\",\"sourceSnapshotHash\":\"" + material.getSourceSnapshotHash() + "\",\"sourceWorkTaskId\":12,\"tenantId\":1}";
        String hash = sha(p), id = "12345678-1234-1234-1234-123456789abc";
        var e = base(201L, "mes.market-release.manager-stage.initialize", "RELEASE_TRANSACTION", "INITIALIZE_MANAGER_STAGE",
                "cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseManagerStageInitializerImpl#initializeManagerReleaseStage");
        e.setSubjectId("RELEASE_TRANSACTION:21"); e.setBeforeState("ABSENT"); e.setBeforeObjectVersion(null); e.setAfterState("PENDING_APPROVAL");
        e.setBeforeStateJson("{\"codecVersion\":\"MES-OC-JSON-V1\",\"priorReleaseStateRaw\":null}");
        e.setAfterStateJson("{\"associationHash\":\"" + hash + "\",\"associationPayload\":" + p + ",\"codecVersion\":\"MES-OC-JSON-V1\"}");
        e.setReason("正式上游证据移交管理者审批"); e.setReasonCode("MES_MANAGER_STAGE_INITIALIZE");
        String key = unicode ? "MH1:bac3162e324b9ac63943db68a176220eaeb4c47b152e6c5e951394ef93d8a1f5"
                : "MH1:892b4d0b927c1e9033df6124f2da62232e946292f7645f66eec9873590573c69";
        e.setRequestId(key); e.setIdempotencyKey(key);
        storeEvent(e, List.of(new GxpAuditRelation("SUBJECT", "RELEASE_TRANSACTION", "21", "1", null),
                new GxpAuditRelation("SOURCE", "RELEASE_APPLICATION", "11", "1", "TEST_ONLY_REPORT"),
                new GxpAuditRelation("SOURCE", "WORK_TASK", "12", null, "f".repeat(64)),
                new GxpAuditRelation("SOURCE", "MANAGER_RELEASE_WORK_TASK", "13", null, "TEST_ONLY_MANAGER"),
                materialLink(), new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", "200", null, eventRows.get(200L).getEventHash()),
                new GxpAuditRelation("SOURCE", "MANAGED_HANDOFF_MATERIAL_ASSOCIATION", id, "1", hash)),
                List.of(new GxpAuditEvidence("MANAGED_HANDOFF_MATERIAL_ASSOCIATION", id, "1", hash, "MANAGER_HANDOFF")));
        var association = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationDO()
                .setTenantId(1L).setHandoffKey(key).setAssociationId(id).setAssociationPayloadJson(p)
                .setAssociationHash(hash).setHandoffRequestJson(request).setHandoffAuditEventId(201L).setHandoffAuditEventHash(e.getEventHash());
        var associationRows = (cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationMapper)
                ReflectionTestUtils.getField(service, "managedAssociations");
        when(associationRows.selectByKey(1L, key)).thenReturn(List.of(association));
        candidates(200L, 201L); return e;
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void dedicatedManagedNewUsesLiteralUtf8NulLengthEscapeAndSupplementaryVectors(boolean unicode) {
        managed(unicode); assertEquals(200L, read().creationEventId());
        assertEquals(7, links.get(201L).size());
        assertTrue(links.get(201L).stream().noneMatch(r -> "BATCH_EXECUTION".equals(r.objectType())));
    }
    private GxpAuditEventDO submission(String method) {
        var e = base(201L, "mes.market-release.submit", "RELEASE_TRANSACTION", "SUBMIT",
                "cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl#" + method);
        e.setSubjectId("RELEASE_TRANSACTION:21"); e.setBeforeState("PRECHECK_PASSED"); e.setAfterState("PENDING_APPROVAL");
        e.setBeforeStateJson(submissionState(false)); e.setAfterStateJson(submissionState(true));
        e.setReason("TEST_ONLY_EFFECTIVE_REASON\n\"\\" + "长".repeat(470)); e.setReasonCode("MES_RELEASE_PREPARATION");
        e.setRequestId("key"); e.setIdempotencyKey("RELEASE_PREP:" + "a".repeat(64));
        String id = "12345678-1234-1234-1234-123456789abc", hash = "d".repeat(64);
        storeEvent(e, List.of(new GxpAuditRelation("SOURCE", "BATCH_EXECUTION", "92", null, null),
                new GxpAuditRelation("SOURCE", "RELEASE_SUBMISSION_EVENT", "22", null, "e".repeat(64)),
                new GxpAuditRelation("SOURCE", "RELEASE_SUBMISSION_MATERIAL_ASSOCIATION", id, "1", hash), materialLink(),
                new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", "200", null, eventRows.get(200L).getEventHash())),
                List.of(new GxpAuditEvidence("RELEASE_SUBMISSION_MATERIAL_ASSOCIATION", id, "1", hash, "SUBMISSION_MATERIAL")));
        candidates(200L, 201L); return e;
    }
    private String submissionState(boolean submitted) {
        String transaction = "{\"id\":21,\"batchExecutionId\":92,\"version\":1,\"releaseStatus\":\""
                + (submitted ? "PENDING_APPROVAL" : "PRECHECK_PASSED") + "\""
                + (submitted ? ",\"submitIdempotencyKey\":\"key\",\"submittedBy\":7,\"submittedAt\":1791590400000" : "") + "}";
        return "{\"transaction\":" + transaction + ",\"checkItems\":[],\"affectedState\":{\"batches\":[{\"id\":92}],"
                + "\"releaseApplications\":[],\"releaseTransactions\":[" + transaction + "],\"workTasks\":[],\"workOrders\":[],\"productionTasks\":[]}}";
    }
    @ParameterizedTest @ValueSource(strings = {"submit", "submitForApproval"})
    void bothSubmissionShapesRefuseUnknownHistoricalProfileEvenIfCurrentMapperWorks(String method) {
        submission(method);
        assertEquals("SOURCE_SERIALIZATION_PROFILE_UNPROVEN", assertThrows(IllegalStateException.class, this::read).getMessage());
        // This fails deliberately, without trying current/default mapper to certify unknown history.
    }
    @ParameterizedTest @ValueSource(strings = {"root", "unknownDo", "scalar", "checks", "affected", "beforeState", "version", "actor", "creatorLink", "evidence"})
    void submissionMalformedStateOrLinksFailBeforeHistoricalProfileGate(String variant) {
        var e = submission("submit"); String raw = e.getAfterStateJson();
        switch (variant) {
            case "root" -> raw = raw.substring(0, raw.length() - 1) + ",\"unknown\":1}";
            case "unknownDo" -> raw = raw.replace("\"id\":21", "\"id\":21,\"unknown\":1");
            case "scalar" -> raw = raw.replace("\"submittedBy\":7", "\"submittedBy\":\"7\"");
            case "checks" -> raw = raw.replace("\"checkItems\":[]", "\"checkItems\":[{\"id\":1,\"releaseTransactionId\":999}]");
            case "affected" -> raw = raw.replace("\"productionTasks\":[]", "\"productionTasks\":[],\"formInstances\":[]");
            case "beforeState" -> e.setBeforeState("PRECHECK_REQUIRED"); case "version" -> e.setAfterObjectVersion("2");
            case "actor" -> raw = raw.replace("\"submittedBy\":7", "\"submittedBy\":8");
            case "creatorLink" -> { var list = new ArrayList<>(links.get(201L)); list.set(4, new GxpAuditRelation("SOURCE", "GXP_AUDIT_EVENT", "201", null, eventRows.get(200L).getEventHash()));
                links.put(201L, list); e.setRelationManifestJson(JsonUtils.toJsonString(list)); }
            case "evidence" -> e.setEvidenceManifestJson("[]");
        }
        e.setAfterStateJson(raw); rehash(e);
        assertNotEquals("SOURCE_SERIALIZATION_PROFILE_UNPROVEN", assertThrows(IllegalStateException.class, this::read).getMessage());
    }
    @ParameterizedTest @ValueSource(strings = {"duplicate", "trailing", "unknown", "whitespace", "exponent", "quotedLong", "overflow", "key", "reason", "actor", "creator", "extraBatch", "evidence"})
    void managedCorruptionRejectsWholeProvenanceAfterActualShared(String variant) {
        var e = managed(false); String raw = e.getAfterStateJson();
        switch (variant) {
            case "duplicate" -> raw = raw.replace("\"tenantId\":1", "\"tenantId\":1,\"tenantId\":1");
            case "trailing" -> raw += " {}"; case "unknown" -> raw = raw.replace("\"tenantId\":1", "\"tenantId\":1,\"unknown\":1");
            case "whitespace" -> raw = " " + raw; case "exponent" -> raw = raw.replace("\"tenantId\":1", "\"tenantId\":1e0");
            case "quotedLong" -> raw = raw.replace("\"tenantId\":1", "\"tenantId\":\"1\"");
            case "overflow" -> raw = raw.replace("\"tenantId\":1", "\"tenantId\":9223372036854775808");
            case "key" -> e.setRequestId("MH1:" + "0".repeat(64)); case "reason" -> e.setReason("wrong");
            case "actor" -> raw = raw.replace("\"sourceActorId\":7", "\"sourceActorId\":8");
            case "creator" -> raw = raw.replace("\"materialCreationEventId\":200", "\"materialCreationEventId\":201");
            case "extraBatch" -> { var list = new ArrayList<>(links.get(201L)); list.add(new GxpAuditRelation("SOURCE", "BATCH_EXECUTION", "92", null, null));
                links.put(201L, list); e.setRelationManifestJson(JsonUtils.toJsonString(list)); }
            case "evidence" -> e.setEvidenceManifestJson("[]");
        }
        e.setAfterStateJson(raw); rehash(e); assertThrows(IllegalStateException.class, this::read);
    }
    @ParameterizedTest @ValueSource(strings = {"duplicateCandidate", "missingCandidate", "emptyCreation", "bindingMissing", "reverseMissing", "queryException"})
    void badCandidateOrBindingCannotBeIgnored(String variant) {
        IllegalStateException failure = new IllegalStateException("query failed");
        switch (variant) {
            case "duplicateCandidate" -> candidates(200L, 200L); case "missingCandidate" -> candidates(200L, 201L); case "emptyCreation" -> candidates();
            case "bindingMissing" -> when(bindingMapper.selectBySourceRow(1L, "MATERIAL_READY_V1", 91L)).thenReturn(List.of());
            case "reverseMissing" -> when(bindingMapper.selectByCreationEvent(1L, 200L)).thenReturn(List.of());
            case "queryException" -> when(events.selectByTenantIdAndId(1L, 200L)).thenThrow(failure);
        }
        var actual = assertThrows(IllegalStateException.class, this::read); if (variant.equals("queryException")) assertSame(failure, actual);
    }
    private MesProcessPoolActiveOrderCompletionReceiptDO completion(boolean loss) {
        var row = new MesProcessPoolActiveOrderCompletionReceiptDO(); row.setHasActualLoss(loss); row.setLossQuantity(loss ? new BigDecimal("2.50") : BigDecimal.ZERO);
        row.setExpectedVersion(2); row.setCompletedVersion(3); row.setReceiptStatus("BACKFILL_SUCCEEDED"); row.setCompletionStatus("SUCCESS");
        row.setBatchRecordStatus("SUCCESS"); row.setProcessInspectionStatus("SUCCESS"); row.setProvisionHandoff("PENDING_FLOW6");
        row.setFormalSourceSnapshotJson("{\"original\":\"TEST_ONLY_PROTOCOL_FIXTURE\"}"); row.setSignatureSnapshotJson("{\"signature\":\"TEST_ONLY\"}");
        row.setLossConditionFactsJson(loss ? "[{\"quantity\":2.50,\"source\":\"TEST_ONLY\"}]" : "[]");
        row.setBatchRecordSourceIdsJson("[11,12]"); row.setProcessInspectionSourceIdsJson("[13]");
        row.setLossReportStatus(loss ? "SUCCESS" : "NOT_REQUIRED"); row.setLossRecordId(loss ? 14L : null);
        row.setZeroLossConfirmationSnapshot(loss ? null : "{\"confirmed\":true}");
        if (loss) row.setLossSourceHash(sha(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.canonicalize(row.getLossConditionFactsJson())));
        row.setSourceSnapshotHash(MesTeamLeaderActiveOrderCompletionSourceSnapshotCanonicalizer.sourceSnapshotHash(row.getFormalSourceSnapshotJson(), row.getLossConditionFactsJson()));
        row.setReceiptHash(MesTeamLeaderActiveOrderCompletionReceiptHash.compute(row)); return row;
    }
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void realOwnerAlgorithmsAcceptOriginalLossArrayWithoutRewritingRaw(boolean loss) {
        var row = completion(loss); String raw = row.getLossConditionFactsJson(), hash = row.getReceiptHash();
        owner.completion(row); assertEquals(raw, row.getLossConditionFactsJson()); assertEquals(hash, row.getReceiptHash());
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "null", "\"text\"", "[1]", "[{\"x\":1,\"x\":1}]", "[] {}", "[true]"})
    void ownerRejectsWrongLossRootBadItemsDuplicateAndTrailing(String raw) {
        var row = completion(false); row.setLossConditionFactsJson(raw);
        assertThrows(IllegalStateException.class, () -> owner.completion(row));
    }
    @Test void completionProofLiteralHasNoAliasInHistoricalRecordContract() {
        var proof = new MesEdhrSourceCreationEvidence("COMPLETION_MAIN_V1", 1L, "1", "3", "a".repeat(64), 2L, "b".repeat(64));
        assertEquals("COMPLETION_MAIN_V1", proof.ownerProtocol()); assertEquals(7, proof.getClass().getRecordComponents().length);
        // This record assertion is not public complete→actual main/parts success evidence.
    }
    private void corruptBinding(MesEdhrSourceCreationBindingDO row, String field) {
        switch(field) {
            case "id" -> row.setId(399L); case "tenantId" -> row.setTenantId(2L); case "ownerProtocol" -> row.setOwnerProtocol("UNKNOWN_PROTOCOL");
            case "sourceRowId" -> row.setSourceRowId(82L); case "sourceReceiptId" -> row.setSourceReceiptId("TEST_ONLY_OTHER"); case "sourceVersion" -> row.setSourceVersion("2");
            case "sourceHash" -> row.setSourceHash("0".repeat(64)); case "requestHashVersion" -> row.setRequestHashVersion("UNKNOWN_VERSION");
            case "requestHash" -> row.setRequestHash("0".repeat(64)); case "creationEventId" -> row.setCreationEventId(299L); case "creationEventHash" -> row.setCreationEventHash("0".repeat(64));
            default -> throw new IllegalArgumentException(field);
        }
    }
    @ParameterizedTest @ValueSource(strings = {"count0", "pkNull", "pkZero", "pkNegative"})
    void bindingInsertRejectsUncheckedCountOrGeneratedIdentity(String variant) {
        installCreation(); var store=new MesEdhrSourceCreationBindingStore(bindingMapper);
        when(bindingMapper.insert(any(MesEdhrSourceCreationBindingDO.class))).thenAnswer(i -> {var row=(MesEdhrSourceCreationBindingDO)i.getArgument(0);row.setId(switch(variant){case "pkZero"->0L;case "pkNegative"->-1L;case "pkNull"->null;default->301L;});return variant.equals("count0")?0:1;});
        assertEquals("SOURCE_BINDING_INSERT_FAILED",assertThrows(IllegalStateException.class,()->store.insert(materialFact())).getMessage());
        verify(bindingMapper,never()).selectByCreationEvent(anyLong(),anyLong());
    }
    private MesEdhrSourceCreationBindingStore.BindingFact materialFact() {
        return new MesEdhrSourceCreationBindingStore.BindingFact(1L,"MATERIAL_READY_V1",91L,material.getReceiptId(),"1",material.getReceiptHash(),"MATERIAL_READY_REQUEST_V1",materialBinding.getRequestHash(),200L,eventRows.get(200L).getEventHash());
    }
    private void stubMaterialBinding(List<MesEdhrSourceCreationBindingDO> source, List<MesEdhrSourceCreationBindingDO> receipt, List<MesEdhrSourceCreationBindingDO> reverse) {
        when(bindingMapper.selectBySourceRow(1L,"MATERIAL_READY_V1",91L)).thenReturn(source);
        when(bindingMapper.selectByReceiptVersion(1L,"MATERIAL_READY_V1",material.getReceiptId(),"1")).thenReturn(receipt);
        when(bindingMapper.selectByCreationEvent(1L,200L)).thenReturn(reverse);
    }
    @ParameterizedTest @ValueSource(strings = {"source:id", "source:tenantId", "source:ownerProtocol", "source:sourceRowId", "source:sourceReceiptId", "source:sourceVersion", "source:sourceHash", "source:requestHashVersion", "source:requestHash", "source:creationEventId", "source:creationEventHash", "receipt:id", "receipt:tenantId", "receipt:ownerProtocol", "receipt:sourceRowId", "receipt:sourceReceiptId", "receipt:sourceVersion", "receipt:sourceHash", "receipt:requestHashVersion", "receipt:requestHash", "receipt:creationEventId", "receipt:creationEventHash", "reverse:id", "reverse:tenantId", "reverse:ownerProtocol", "reverse:sourceRowId", "reverse:sourceReceiptId", "reverse:sourceVersion", "reverse:sourceHash", "reverse:requestHashVersion", "reverse:requestHash", "reverse:creationEventId", "reverse:creationEventHash"})
    void bindingInsertRejectsEveryPostInsertReadbackFieldInEachDirection(String vector) {
        String[] parts=vector.split(":",2); String field=parts[1]; int direction=parts[0].equals("source")?0:parts[0].equals("receipt")?1:2;
        installCreation(); var bad=bindingCopy(materialBinding);corruptBinding(bad,field);
        when(bindingMapper.insert(any(MesEdhrSourceCreationBindingDO.class))).thenAnswer(i->{((MesEdhrSourceCreationBindingDO)i.getArgument(0)).setId(300L);return 1;});
        var store=new MesEdhrSourceCreationBindingStore(bindingMapper);
        stubMaterialBinding(direction==0?List.of(bad):List.of(materialBinding),direction==1?List.of(bad):List.of(materialBinding),direction==2?List.of(bad):List.of(materialBinding));assertThrows(RuntimeException.class,()->store.insert(materialFact()),"direction "+direction+" field "+field);
    }
    @Test void bindingInsertAcceptsCheckedIdentityAndThreeExactReadbacks() {
        installCreation();
        when(bindingMapper.insert(any(MesEdhrSourceCreationBindingDO.class))).thenAnswer(i->{((MesEdhrSourceCreationBindingDO)i.getArgument(0)).setId(300L);return 1;});
        var value=new MesEdhrSourceCreationBindingStore(bindingMapper).insert(materialFact());
        assertEquals(300L,value.id());assertEquals(materialFact(),value.fact());
        verify(bindingMapper).selectBySourceRow(1L,"MATERIAL_READY_V1",91L);
        verify(bindingMapper).selectByReceiptVersion(1L,"MATERIAL_READY_V1",material.getReceiptId(),"1");
        verify(bindingMapper).selectByCreationEvent(1L,200L);
        // Direct store unit exercises checked mapper results; physical MANDATORY remains real-proxy persistence coverage.
    }
    @ParameterizedTest @ValueSource(strings={"sourceNull","receiptNull","reverseNull","source0","receipt0","reverse0","source2","receipt2","reverse2"})
    void bindingInsertRejectsNullMissingOrMultiplePostInsertReadbacks(String variant) {
        installCreation();
        when(bindingMapper.insert(any(MesEdhrSourceCreationBindingDO.class))).thenAnswer(i->{((MesEdhrSourceCreationBindingDO)i.getArgument(0)).setId(300L);return 1;});
        List<MesEdhrSourceCreationBindingDO> bad=variant.endsWith("Null")?null:variant.endsWith("0")?List.of():List.of(materialBinding,bindingCopy(materialBinding).setId(398L));
        stubMaterialBinding(variant.startsWith("source")?bad:List.of(materialBinding),variant.startsWith("receipt")?bad:List.of(materialBinding),variant.startsWith("reverse")?bad:List.of(materialBinding));
        assertThrows(RuntimeException.class,()->new MesEdhrSourceCreationBindingStore(bindingMapper).insert(materialFact()));
    }
}
