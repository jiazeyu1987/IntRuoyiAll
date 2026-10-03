package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolPqcInspectionCorrectionCommand;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolPqcInspectionCorrectionService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the actual correction review/audit producer and signature adapter against the reader. */
@ExtendWith(MockitoExtension.class)
class MesPqcCorrectionSignatureEvidenceTest {
    @Mock MesTeamLeaderActiveOrderDetailService teamDetailService;
    @Mock MesPqcReleaseOrderDetailService pqcDetailService;
    @Mock MesProEdhrBatchActiveOrderDetailService batchDetailService;
    @Mock ElectronicSignatureQueryService signatureQueryService;
    @Mock MesProProcessPoolEventMapper eventMapper;
    @Mock MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock MesProProcessPoolEventRevisionMapper revisionMapper;
    @Mock GxpAuditEventMapper auditEventMapper;
    @InjectMocks MesActiveOrderSignatureEvidenceService service;

    private static final Long EVENT = 241L, ACTIVE_ORDER = 409L, REVISION = 801L, SIGNATURE = 746L;
    private static final String REASON = "补正已批准检验数量";
    private static final String SIGNED_PAYLOAD = "{\"activeOrderId\":409,\"pqcTaskId\":140,"
            + "\"actualInspectionQuantity\":9,\"supersededReviewId\":7000}";
    // A native JSON column can return the same document with another field order/whitespace.
    private static final String STORED_PAYLOAD = "{ \"pqcTaskId\": 140, \"supersededReviewId\": 7000,"
            + " \"actualInspectionQuantity\": 9, \"activeOrderId\": 409 }";
    private static final LocalDateTime SIGNED_AT = LocalDateTime.of(2026, 10, 3, 9, 20);
    private MesProProcessPoolEventDO event;
    private MesProcessPoolSubmissionReviewDO previous, correction;
    private MesProProcessPoolEventRevisionDO revision;
    private GxpAuditEventDO audit;
    private ElectronicSignatureEvidenceDTO evidence;

    @BeforeEach void formalCorrection() {
        TenantContextHolder.setTenantId(1L);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "correction-signature"),
                GxpAuditEventDO.class);
        event = new MesProProcessPoolEventDO().setId(EVENT).setEventType("PQC_INSPECTION")
                .setPoolId(610L).setWorkOrderId(990L).setRouteId(900L).setQaProcessId(986L)
                .setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(140L)
                .setRecordbookSourceType("MES_PQC_INSPECTION_TASK").setRecordbookSourceId(140L)
                .setRawPayload(STORED_PAYLOAD);
        event.setTenantId(1L);
        previous = new MesProcessPoolSubmissionReviewDO().setId(7000L).setEventId(EVENT)
                .setLeaderType("PQC").setLeaderUserId(341L).setReviewStatus("APPROVED")
                .setReviewSignatureId(743L).setReviewSignatureUserId(341L);
        previous.setTenantId(1L);
        var signature = new MesProBatchRecordExecutionFieldAuditSignatureResult().setSignatureId(SIGNATURE)
                .setActorId(344L).setActorName("正式补正人员").setSignedAt(SIGNED_AT);
        var writer = new MesProcessPoolPqcInspectionCorrectionService(null, null, null, null, null,
                null, null, null, null, null);
        var writerReviewMapper = mock(MesProcessPoolSubmissionReviewMapper.class);
        ReflectionTestUtils.setField(writer, "reviewMapper", writerReviewMapper);
        doAnswer(invocation -> {
            correction = invocation.getArgument(0);
            correction.setId(7001L);
            return 1;
        }).when(writerReviewMapper).insert(any(MesProcessPoolSubmissionReviewDO.class));
        Long reviewId = ReflectionTestUtils.invokeMethod(writer, "createCorrectionReview",
                event, previous, signature, REVISION, REASON, SIGNED_PAYLOAD);
        assertEquals(7001L, reviewId);
        revision = new MesProProcessPoolEventRevisionDO().setId(REVISION).setEventId(EVENT)
                .setPoolId(610L).setWorkOrderId(990L).setRouteId(900L).setRouteProcessId(520L).setProcessId(985L)
                .setAfterPayload(STORED_PAYLOAD).setChangeReason(REASON).setRevisionSignatureId(SIGNATURE)
                .setRevisionSignatureUserId(344L).setModifiedByUserId(344L)
                .setRevisionSignatureSnapshot(JsonUtils.toJsonString(signature)).setRevisionStatus("EFFECTIVE");
        revision.setTenantId(1L);
        String challenge = MesProBatchRecordExecutionFieldAuditHasher.sha256(EVENT + "|" + SIGNED_PAYLOAD + "|" + REASON);
        evidence = signatureEvidence(SIGNATURE, 344L, "FIELD_CHANGE", null, null, challenge, REASON);
        var task = new MesPqcInspectionTaskDO().setId(140L).setActiveOrderId(ACTIVE_ORDER).setWorkOrderId(990L);
        var state = GxpAuditStateEnvelope.builder().state("CORRECTED").objectVersion("version")
                .canonicalJson("{}").build();
        var command = new MesProcessPoolPqcInspectionCorrectionCommand().setEventId(EVENT)
                .setActorUserId(344L).setChangeReason(REASON);
        var writerAudit = mock(GxpAuditService.class);
        ReflectionTestUtils.setField(writer, "gxpAuditService", writerAudit);
        AtomicReference<GxpAuditCommand> appended = new AtomicReference<>();
        doAnswer(invocation -> { appended.set(invocation.getArgument(0)); return null; })
                .when(writerAudit).append(any(GxpAuditCommand.class));
        ReflectionTestUtils.invokeMethod(writer, "appendCorrectionAudit", command, event, task, REVISION,
                signature, previous, correction, state, state, SIGNED_PAYLOAD, evidence.contentHash());
        var saved = appended.get();
        audit = new GxpAuditEventDO();
        audit.setId(8801L); audit.setTenantId(1L); audit.setActorId(344L);
        audit.setOperationId(saved.getOperationId()); audit.setSubjectId(saved.getSubjectId());
        audit.setRequestId(saved.getRequestId()); audit.setResultStatus(saved.getResultStatus());
        audit.setReason(saved.getReason()); audit.setReasonCode(saved.getReasonCode());
        audit.setReasonSource(saved.getReasonSource()); audit.setSourceType(saved.getSourceType());
        audit.setSourceLocator(saved.getSourceLocator()); audit.setSignatureRecordId(saved.getSignatureRecordId());
        audit.setSignatureContentHash(saved.getSignatureContentHash());
        audit.setRelationManifestJson(JsonUtils.toJsonString(saved.getLinks()));
        audit.setEvidenceManifestJson(JsonUtils.toJsonString(saved.getEvidences()));
        audit.setEventSchemaVersion(2);

        lenient().when(teamDetailService.getDetail(341L, ACTIVE_ORDER)).thenReturn(detail());
        lenient().when(batchDetailService.getDetail(800L)).thenReturn(detail());
        lenient().when(eventMapper.selectById(EVENT)).thenReturn(event);
        lenient().when(reviewMapper.selectListByEventId(EVENT)).thenReturn(List.of(previous, correction));
        lenient().when(revisionMapper.selectById(REVISION)).thenReturn(revision);
        lenient().when(auditEventMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(audit));
        lenient().when(signatureQueryService.getById(SIGNATURE)).thenAnswer(invocation -> evidence);
        lenient().when(signatureQueryService.verifyEvidence(SIGNATURE)).thenAnswer(invocation ->
                new ElectronicSignatureVerificationDTO(SIGNATURE, "VALID", evidence.contentHash(), evidence.contentHash(),
                        evidence.evidenceHash(), evidence.evidenceHash(), "SHA-256", "v1"));
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void approvedPqcCorrectionReadsFieldChangeThroughFormalRevisionAndAudit() {
        assertNotEquals(MesProBatchRecordExecutionFieldAuditHasher.sha256(EVENT + "|" + STORED_PAYLOAD + "|" + REASON),
                com.alibaba.fastjson.JSON.parseObject(evidence.canonicalContentJson()).getString("signatureChallengeHash"));
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(SIGNED_PAYLOAD),
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(STORED_PAYLOAD));
        var result = service.getTeam(341L, ACTIVE_ORDER, SIGNATURE);
        assertEquals("FIELD_CHANGE", result.evidence().actionCode());
        assertEquals("VALID", result.verification().verificationStatus());
        assertEquals("正式补正人员", result.signerName());
    }

    @Test void historicalCorrectionDoesNotBindToLaterEventPayload() {
        event.setRawPayload("{\"activeOrderId\":409,\"actualInspectionQuantity\":10}");
        assertEquals(SIGNATURE, service.getBatch(800L, null, SIGNATURE).evidence().id());
    }

    @Test void originalTeamLeaderReviewRemainsReadableAfterCorrection() {
        var original = signatureEvidence(743L, 341L, "TEAM_LEADER_REVIEW", "PROCESS_POOL_EVENT", EVENT, null, "批准");
        when(signatureQueryService.getById(743L)).thenReturn(original);
        when(signatureQueryService.verifyEvidence(743L)).thenReturn(new ElectronicSignatureVerificationDTO(
                743L, "VALID", original.contentHash(), original.contentHash(), original.evidenceHash(), original.evidenceHash(), "SHA-256", "v1"));
        assertEquals(743L, service.getTeam(341L, ACTIVE_ORDER, 743L).evidence().id());
    }

    @Test void deniesEvidenceBeforeDetailAuthorization() {
        when(teamDetailService.getDetail(341L, ACTIVE_ORDER)).thenThrow(new IllegalStateException("not owner"));
        assertThrows(IllegalStateException.class, () -> service.getTeam(341L, ACTIVE_ORDER, SIGNATURE));
        verifyNoInteractions(signatureQueryService);
    }

    @Test void fieldChangeWithoutCorrectionReviewContractIsRejected() {
        correction.setReviewSignatureSnapshotJson("{\"actionType\":\"FIELD_CHANGE\"}"); rejected();
    }

    @Test void anotherRevisionEventIsRejected() { revision.setEventId(242L); rejected(); }
    @Test void anotherRevisionSignatureIsRejected() { revision.setRevisionSignatureId(747L); rejected(); }
    @Test void anotherFormalActorIsRejected() { revision.setModifiedByUserId(345L); rejected(); }
    @Test void crossTenantRevisionIsRejected() { revision.setTenantId(2L); rejected(); }
    @Test void crossTenantEventIsRejected() { event.setTenantId(2L); rejected(); }
    @Test void crossTenantReviewIsRejected() { correction.setTenantId(2L); rejected(); }
    @Test void anotherApprovedReviewIsRejected() { previous.setEventId(242L); rejected(); }
    @Test void changedRevisionPayloadIsRejected() {
        revision.setAfterPayload(STORED_PAYLOAD.replace("9,", "8,")); rejected();
    }
    @Test void anotherActiveOrderPayloadIsRejected() {
        revision.setAfterPayload(STORED_PAYLOAD.replace("409", "410")); rejected();
    }
    @Test void anotherSignedChallengeIsRejected() {
        evidence = signatureEvidence(SIGNATURE, 344L, "FIELD_CHANGE", null, null, "different-challenge", REASON); rejected();
    }
    @Test void absentCorrectionAuditIsRejected() {
        lenient().when(auditEventMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of()); rejected();
    }
    @Test void unrelatedAuditManifestIsRejected() { audit.setRelationManifestJson("[]"); rejected(); }
    @Test void crossTenantCorrectionAuditIsRejected() { audit.setTenantId(2L); rejected(); }

    @ParameterizedTest(name = "reject changed correction audit {0}")
    @MethodSource("wrongAuditFields")
    void changedFormalAuditFieldIsRejected(String field, Object wrongValue) {
        ReflectionTestUtils.setField(audit, field, wrongValue); rejected();
    }

    static Stream<Arguments> wrongAuditFields() {
        return Stream.of(
                Arguments.of("operationId", "mes.production-report.correct"),
                Arguments.of("requestId", "MES-PQC-CORRECTION:802"),
                Arguments.of("subjectId", "MES_PROCESS_POOL_EVENT:242"),
                Arguments.of("resultStatus", "FAILED"),
                Arguments.of("actorId", 345L),
                Arguments.of("signatureRecordId", "747"),
                Arguments.of("signatureContentHash", "other-signed-content"),
                Arguments.of("reason", "另一次更正"),
                Arguments.of("reasonCode", "OTHER_CORRECTION"),
                Arguments.of("reasonSource", "SYSTEM"),
                Arguments.of("sourceType", "API"),
                Arguments.of("sourceLocator", "other.Service#correct"),
                Arguments.of("eventSchemaVersion", 1));
    }

    @ParameterizedTest(name = "reject missing formal correction relation {0}")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void eachMissingAuditRelationIsRejected(int index) {
        var relations = new ArrayList<>(JsonUtils.parseArray(audit.getRelationManifestJson(), GxpAuditRelation.class));
        relations.remove(index); audit.setRelationManifestJson(JsonUtils.toJsonString(relations)); rejected();
    }

    @ParameterizedTest(name = "reject unrelated target in correction relation {0}")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void eachWrongAuditRelationTargetIsRejected(int index) {
        var relations = new ArrayList<>(JsonUtils.parseArray(audit.getRelationManifestJson(), GxpAuditRelation.class));
        var original = relations.get(index);
        relations.set(index, new GxpAuditRelation(original.relationType(), original.objectType(), "999999",
                original.objectVersion(), original.objectHash()));
        audit.setRelationManifestJson(JsonUtils.toJsonString(relations)); rejected();
    }

    @ParameterizedTest(name = "reject mismatched correction evidence hash {0}")
    @ValueSource(ints = {0, 1, 2})
    void eachWrongAuditEvidenceHashIsRejected(int index) {
        var evidences = new ArrayList<>(JsonUtils.parseArray(audit.getEvidenceManifestJson(), GxpAuditEvidence.class));
        var original = evidences.get(index);
        evidences.set(index, new GxpAuditEvidence(original.evidenceType(), original.sourceId(), original.sourceVersion(),
                "unrelated-hash", original.role()));
        audit.setEvidenceManifestJson(JsonUtils.toJsonString(evidences)); rejected();
    }

    @ParameterizedTest(name = "reject unrelated correction evidence source {0}")
    @ValueSource(ints = {0, 1, 2})
    void eachWrongAuditEvidenceSourceIsRejected(int index) {
        var evidences = new ArrayList<>(JsonUtils.parseArray(audit.getEvidenceManifestJson(), GxpAuditEvidence.class));
        var original = evidences.get(index);
        evidences.set(index, new GxpAuditEvidence(original.evidenceType(), "999999", original.sourceVersion(),
                original.sha256(), original.role()));
        audit.setEvidenceManifestJson(JsonUtils.toJsonString(evidences)); rejected();
    }

    @Test void auditLookupHasExactTenantOperationSubjectRevisionRequestAndSignaturePredicates() {
        service.getTeam(341L, ACTIVE_ORDER, SIGNATURE);
        ArgumentCaptor<LambdaQueryWrapper<GxpAuditEventDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(auditEventMapper).selectList(captor.capture());
        var query = captor.getValue();
        String sql = query.getSqlSegment().replace("`", "").replaceAll("\\s+", " ");
        assertEquals(5, query.getParamNameValuePairs().size());
        assertEquals(Set.of(1L, "mes.pqc-inspection.correct", "MES_PROCESS_POOL_EVENT:241",
                "MES-PQC-CORRECTION:801", "746"), new HashSet<>(query.getParamNameValuePairs().values()));
        for (String field : List.of("tenant_id", "operation_id", "subject_id", "request_id", "signature_record_id")) {
            assertTrue(sql.contains(field + " = #{"), sql);
        }
        assertFalse(sql.contains(" OR "), sql);
        assertFalse(sql.contains("LIMIT"), sql);
    }

    @Test void duplicateCorrectionAuditCannotChooseOneArbitraryMatch() {
        lenient().when(auditEventMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(audit, audit)); rejected();
    }

    @ParameterizedTest
    @ValueSource(strings = {"reviewStatus", "leaderType", "reviewRemark", "revisionStatus", "changeReason"})
    void correctionReviewAndRevisionMustRetainTheirFormalStateAndReason(String field) {
        if (Set.of("reviewStatus", "leaderType", "reviewRemark").contains(field)) {
            ReflectionTestUtils.setField(correction, field, "UNRELATED");
        } else ReflectionTestUtils.setField(revision, field, "UNRELATED");
        rejected();
    }

    @Test void revisionSignatureSnapshotActorMustMatchTheUnifiedSignature() {
        revision.setRevisionSignatureSnapshot(revision.getRevisionSignatureSnapshot().replace("344", "345")); rejected();
    }

    @Test void correctedReviewCannotClaimAnotherSignatureActor() {
        correction.setLeaderUserId(345L); rejected();
    }

    @Test void plainProductionReviewCannotUseCorrectionFieldChangeContract() {
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(ACTIVE_ORDER).setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setSubmissions(List.of(
                        new MesTeamLeaderActiveOrderDetail.SubmissionDetail().setEventId(EVENT)
                                .setReviewerSignature(new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(SIGNATURE))))));
        when(teamDetailService.getDetail(341L, ACTIVE_ORDER)).thenReturn(detail); rejected();
    }

    @Test void correctionCannotInventReviewSourceOnFieldChangeSubject() {
        evidence = signatureEvidence(SIGNATURE, 344L, "FIELD_CHANGE", "PROCESS_POOL_EVENT", EVENT,
                com.alibaba.fastjson.JSON.parseObject(evidence.canonicalContentJson()).getString("signatureChallengeHash"), REASON);
        rejected();
    }

    @Test void unifiedMismatchIsReturnedInsteadOfAssumedValidForCorrection() {
        when(signatureQueryService.verifyEvidence(SIGNATURE)).thenReturn(new ElectronicSignatureVerificationDTO(
                SIGNATURE, "MISMATCH", evidence.contentHash(), "changed", evidence.evidenceHash(), "changed", "SHA-256", "v1"));
        assertEquals("MISMATCH", service.getTeam(341L, ACTIVE_ORDER, SIGNATURE).verification().verificationStatus());
    }


    @Test void actualPersisted15059NullBearingSnapshotsRemainReadable() {
        loadActualPersistedCorrection15059();
        var result = service.getTeam(9908090341L, 1009200409L, 15059L);
        assertEquals(15059L, result.evidence().id());
        assertEquals("FIELD_CHANGE", result.evidence().actionCode());
        assertEquals("VALID", result.verification().verificationStatus());
        assertEquals("eDHR测试PQC组长", result.signerName());
        verify(signatureQueryService).verifyEvidence(15059L);
    }

    @Test void actualPersisted15059ContainsEqualNullBearingSnapshotsAndStableAuditHash() {
        loadActualPersistedCorrection15059();
        var storedReviewSignature = JsonUtils.parseTree(correction.getReviewSignatureSnapshotJson()).get("signature");
        var storedRevisionSignature = JsonUtils.parseTree(revision.getRevisionSignatureSnapshot());
        assertEquals(storedReviewSignature, storedRevisionSignature);
        assertTrue(storedReviewSignature.has("selectedSignedAt"));
        assertTrue(storedReviewSignature.get("selectedSignedAt").isNull());
        // The persisted records agree. FastJSON's intermediate text removes this explicit null.
        var lossySignature = com.alibaba.fastjson.JSON.parseObject(correction.getReviewSignatureSnapshotJson())
                .getJSONObject("signature").toJSONString();
        assertFalse(JsonUtils.parseTree(lossySignature).has("selectedSignedAt"));
        assertNotEquals(storedRevisionSignature, JsonUtils.parseTree(lossySignature));
        assertEquals("3b29b9c07ad1f1b4134d6e8c863156ca448d4546ad062669496824daf4c8994f",
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()));
        assertEquals("0ad98b43ee613411447e677501301406deb494f79f4b3e51227988265d8a1188",
                MesProBatchRecordExecutionFieldAuditHasher.sha256(evidence.subjectId()));
    }

    @Test void actualPersisted15059OneSidedNullRemovalIsRejected() {
        loadActualPersistedCorrection15059();
        var snapshot = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(revision.getRevisionSignatureSnapshot());
        snapshot.remove("selectedSignedAt");
        revision.setRevisionSignatureSnapshot(snapshot.toString());
        actualReadbackRejected();
    }

    @Test void actualPersisted15059DifferentSelectedTimePolicyIsRejected() {
        loadActualPersistedCorrection15059();
        var snapshot = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(revision.getRevisionSignatureSnapshot());
        snapshot.put("selectedTimePolicyVersion", "UNRELATED_TIME_POLICY");
        revision.setRevisionSignatureSnapshot(snapshot.toString());
        actualReadbackRejected();
    }

    private void actualReadbackRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getTeam(9908090341L, 1009200409L, 15059L));
        assertEquals(1_040_760_452, ex.getCode());
        verify(signatureQueryService, never()).verifyEvidence(anyLong());
    }

    private void loadActualPersistedCorrection15059() {
        var facts = JsonUtils.parseTree(ACTUAL_PERSISTED_CORRECTION_15059);
        event = JsonUtils.parseObject(facts.get("event").toString(), MesProProcessPoolEventDO.class);
        var reviewRows = new ArrayList<MesProcessPoolSubmissionReviewDO>();
        for (var row : facts.get("reviews")) {
            var copy = (com.fasterxml.jackson.databind.node.ObjectNode) row.deepCopy();
            copy.put("reviewSignatureSnapshotJson", row.get("reviewSignatureSnapshotJson").toString());
            reviewRows.add(JsonUtils.parseObject(copy.toString(), MesProcessPoolSubmissionReviewDO.class));
        }
        previous = reviewRows.get(0);
        correction = reviewRows.get(1);
        var revisionRow = (com.fasterxml.jackson.databind.node.ObjectNode) facts.get("revision").deepCopy();
        revisionRow.put("afterPayload", facts.get("revision").get("afterPayload").toString());
        revisionRow.put("revisionSignatureSnapshot", facts.get("revision").get("revisionSignatureSnapshot").toString());
        revision = JsonUtils.parseObject(revisionRow.toString(), MesProProcessPoolEventRevisionDO.class);
        var correctionAudits = new ArrayList<GxpAuditEventDO>();
        for (var row : facts.get("audits")) {
            if ("mes.pqc-inspection.correct".equals(row.get("operationId").asText())) {
                correctionAudits.add(JsonUtils.parseObject(row.toString(), GxpAuditEventDO.class));
            }
        }
        assertEquals(1, correctionAudits.size());
        audit = correctionAudits.get(0);
        var signatureRow = (com.fasterxml.jackson.databind.node.ObjectNode) facts.get("signature").deepCopy();
        signatureRow.put("canonicalContentJson", facts.get("signature").get("canonicalContentJson").toString());
        evidence = JsonUtils.parseObject(signatureRow.toString(), ElectronicSignatureEvidenceDTO.class);
        var actualDetail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(1009200409L).setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setPqcSubmissions(List.of(
                        new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail().setPqcTaskIds(List.of(1009622119L))
                                .setSubmittedEventIds(List.of(282246L)).setReviewerSignatures(List.of(
                                        new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(15059L)
                                                .setSignerName("eDHR测试PQC组长")))))));
        lenient().when(teamDetailService.getDetail(9908090341L, 1009200409L)).thenReturn(actualDetail);
        lenient().when(eventMapper.selectById(282246L)).thenReturn(event);
        lenient().when(reviewMapper.selectListByEventId(282246L)).thenReturn(reviewRows);
        lenient().when(revisionMapper.selectById(42L)).thenReturn(revision);
        lenient().when(auditEventMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(correctionAudits);
        lenient().when(signatureQueryService.getById(15059L)).thenAnswer(invocation -> evidence);
        lenient().when(signatureQueryService.verifyEvidence(15059L)).thenAnswer(invocation ->
                new ElectronicSignatureVerificationDTO(15059L, "VALID", evidence.contentHash(), evidence.contentHash(),
                        evidence.evidenceHash(), evidence.evidenceHash(), "SHA-256", "v1"));
    }

    // Frozen exact read-only MySQL facts from root-correction15059-contract-read.json.
    // No database/file-system dependency; neither signatures nor audit hashes are generated by this fixture.
    private static final String ACTUAL_PERSISTED_CORRECTION_15059 = """
            {
              "event": {
                "id": 282246,
                "poolId": 8188,
                "routeId": 980091,
                "tenantId": 1,
                "eventType": "PQC_INSPECTION",
                "processId": null,
                "qaProcessId": 168,
                "workOrderId": 990274,
                "routeProcessId": null,
                "feedbackSourceId": 1009622119,
                "feedbackSourceType": "MES_PQC_INSPECTION_TASK",
                "recordbookSourceId": 1009622119,
                "recordbookSourceType": "MES_PQC_INSPECTION_TASK"
              },
              "reviews": [
                {
                  "id": 281244,
                  "eventId": 282246,
                  "tenantId": 1,
                  "leaderType": "PQC",
                  "reviewedAt": "2026-10-02 17:54:45.000000",
                  "leaderUserId": 9908090343,
                  "reviewRemark": null,
                  "reviewStatus": "APPROVED",
                  "reviewSignatureId": 12753,
                  "reviewSignatureUserId": 9908090343,
                  "reviewSignatureSnapshotJson": {
                    "actorId": 9908090343,
                    "eventType": "PQC_INSPECTION",
                    "actionType": "TEAM_LEADER_REVIEW",
                    "leaderType": "PQC",
                    "payloadHash": "e852ac07d944dbf70f9954502e8543fa422899e0a5a83cdb5766cc9aa7216ccd",
                    "signatureId": 12753,
                    "reviewStatus": "APPROVED",
                    "processPoolEventId": 282246
                  }
                },
                {
                  "id": 282397,
                  "eventId": 282246,
                  "tenantId": 1,
                  "leaderType": "PQC",
                  "reviewedAt": "2026-10-03 09:00:18.000000",
                  "leaderUserId": 9908090343,
                  "reviewRemark": "七角色E2E已批准补正签名取证：正式事件5件修改为4件及逐件样本，验证正式补正复核签名，随后通过前端恢复正式事件5件。",
                  "reviewStatus": "APPROVED",
                  "reviewSignatureId": 15059,
                  "reviewSignatureUserId": 9908090343,
                  "reviewSignatureSnapshotJson": {
                    "actorId": 9908090343,
                    "signature": {
                      "actorId": 9908090343,
                      "signedAt": 1790989217665,
                      "actorName": "eDHR测试PQC组长",
                      "signatureId": 15059,
                      "selectedSignedAt": null,
                      "selectedTimeZone": "Asia/Shanghai",
                      "signatureTimeMode": "SERVER_TIME",
                      "selectedTimeReason": "",
                      "signatureDisplayAt": 1790989217665,
                      "selectedTimeAuditHash": "3e7a32dc42dfa9f5fad980229c7efb0f1ededd8310a95cfa5a8af64e347268db",
                      "selectedTimePolicyVersion": "EDHR_SIGNATURE_TIME_V1"
                    },
                    "actionType": "PQC_INSPECTION_CORRECTION",
                    "revisionId": 42,
                    "payloadHash": "3b29b9c07ad1f1b4134d6e8c863156ca448d4546ad062669496824daf4c8994f",
                    "signatureId": 15059,
                    "processPoolEventId": 282246,
                    "supersededReviewId": 281244
                  }
                }
              ],
              "revision": {
                "id": 42,
                "poolId": 8188,
                "eventId": 282246,
                "routeId": 980091,
                "tenantId": 1,
                "processId": 922986,
                "workOrderId": 990274,
                "afterPayload": {
                  "roundNo": 1,
                  "routeId": 980091,
                  "pqcDraft": {
                    "patrolRound": 1,
                    "scrapQuantity": 0,
                    "inspectionType": "FIRST",
                    "inspectionQuantity": 5
                  },
                  "pqcTaskId": 1009622119,
                  "shiftCode": "FIRST",
                  "fieldValues": {
                    "PQC_RESULT": "DETECTION_SUCCESS"
                  },
                  "itemResults": [
                    {
                      "itemCode": "PQC-IDI-001-I004",
                      "sampleValues": [
                        "合格",
                        "合格",
                        "合格",
                        "合格",
                        "合格"
                      ],
                      "selectedEquipmentId": 980012,
                      "selectedEquipmentNumber": "C01017"
                    }
                  ],
                  "qaProcessId": 168,
                  "workOrderId": 990274,
                  "businessDate": [
                    2026,
                    10,
                    1
                  ],
                  "activeOrderId": 1009200409,
                  "scrapQuantity": 0,
                  "inspectionType": "FIRST",
                  "pqcItemDetails": [
                    {
                      "itemCode": "PQC-IDI-001-I004",
                      "itemName": "无跳压",
                      "judgement": "SUCCESS",
                      "resultType": "BOOLEAN",
                      "sampleValues": [
                        "合格",
                        "合格",
                        "合格",
                        "合格"
                      ],
                      "standardText": "20atm压力打至20atm应无跳压现象； 30atm压力打至30atm应无跳压现象； 40atm压力泵需打压至40atm无跳压现象。",
                      "standardUnit": null,
                      "inspectionMethod": "将推杆装到检测专用的泵筒(吸入10ML水)上，将压力打至20atm/30atm/40atm应无跳压现象。",
                      "standardPrecision": null,
                      "standardLowerLimit": null,
                      "standardUpperLimit": null,
                      "selectedEquipmentId": 980012,
                      "selectedEquipmentCode": "C01017",
                      "selectedEquipmentName": "撤压机",
                      "selectedEquipmentNumber": "C01017"
                    }
                  ],
                  "pqcPieceValues": {
                    "PQC-IDI-001-I004": [
                      "合格",
                      "合格",
                      "合格",
                      "合格",
                      "合格"
                    ]
                  },
                  "selectedProcess": {
                    "routeId": 980091,
                    "routeCode": "RT000028-IDI",
                    "routeName": "按压式球囊扩充压力泵",
                    "qaProcessId": 168,
                    "taskSummary": {
                      "state": "MIXED",
                      "totalCount": 90,
                      "pendingCount": 88,
                      "cancelledCount": 0,
                      "confirmedCount": 1,
                      "submittedCount": 1
                    },
                    "regulationId": 60,
                    "activeOrderId": 1009200409,
                    "qaProcessCode": "PQC-IDI-001-P003",
                    "qaProcessName": "组装螺杆八组件",
                    "qaProcessSort": 3,
                    "pqcTaskOptions": [
                      {
                        "roundNo": 1,
                        "ruleSort": 10,
                        "pqcTaskId": 1009622119,
                        "shiftCode": "FIRST",
                        "qaItemCode": "PQC-IDI-001-I004",
                        "taskStatus": "PENDING",
                        "qaProcessId": 168,
                        "businessDate": "2026-10-01",
                        "inspectionType": "FIRST",
                        "inspectionItems": [
                          {
                            "critical": false,
                            "itemCode": "PQC-IDI-001-I004",
                            "itemName": "无跳压",
                            "itemSort": 2,
                            "resultType": "BOOLEAN",
                            "sourceNote": "导入自 QA 模板：PQC-IDI-001（B 2）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.docx",
                            "failureRule": null,
                            "standardText": "20atm压力打至20atm应无跳压现象； 30atm压力打至30atm应无跳压现象； 40atm压力泵需打压至40atm无跳压现象。",
                            "standardUnit": null,
                            "inspectionTool": "检测专用泵筒",
                            "equipmentOptions": [
                              {
                                "sort": 0,
                                "parameters": [],
                                "defaultFlag": true,
                                "equipmentId": 980012,
                                "equipmentCode": "C01017",
                                "equipmentName": "撤压机",
                                "equipmentNumber": "C01017"
                              }
                            ],
                            "inspectionMethod": "将推杆装到检测专用的泵筒(吸入10ML水)上，将压力打至20atm/30atm/40atm应无跳压现象。",
                            "samplingPlanText": "首件：5件 GB/T 2828.1，S-3，AQL=0.4",
                            "equipmentRequired": true,
                            "standardPrecision": null,
                            "sourceOriginalItem": "组装螺杆八组件 / 无跳压",
                            "sourceOriginalPage": null,
                            "standardLowerLimit": null,
                            "standardUpperLimit": null,
                            "sourceOriginalMethod": "将推杆装到检测专用的泵筒(吸入10ML水)上，将压力打至20atm/30atm/40atm应无跳压现象。",
                            "patrolInspectionRatio": 0.4,
                            "sourceOriginalExcerpt": "20atm压力打至20atm应无跳压现象； 30atm压力打至30atm应无跳压现象； 40atm压力泵需打压至40atm无跳压现象。",
                            "firstInspectionQuantity": 5,
                            "lastSelectedEquipmentId": 980012,
                            "applicableInspectionTypes": [
                              "FIRST",
                              "PATROL"
                            ],
                            "lastSelectedEquipmentNumber": "C01017"
                          }
                        ],
                        "inspectionRuleKey": "FIRST",
                        "inspectionTypeRule": {
                          "key": "FIRST",
                          "label": "首检",
                          "required": true,
                          "taskRule": "按发布规程固定数量生成首检任务",
                          "roundLabel": null,
                          "releaseGate": null,
                          "fixedQuantity": null,
                          "inspectionType": "FIRST",
                          "notApplicableReason": null
                        },
                        "regulationVersionId": 70,
                        "finalInspectionApplicable": false,
                        "plannedInspectionQuantity": 5
                      }
                    ],
                    "regulationCode": "PQC-IDI-001",
                    "regulationName": "按压式球囊扩充压力泵组装过程检验规程",
                    "inspectionItems": [
                      {
                        "critical": false,
                        "itemCode": "PQC-IDI-001-I004",
                        "itemName": "无跳压",
                        "itemSort": 2,
                        "resultType": "BOOLEAN",
                        "sourceNote": "导入自 QA 模板：PQC-IDI-001（B 2）按压式球囊扩充压力泵组装过程检验规程--2026.01.04生效.docx",
                        "failureRule": null,
                        "standardText": "20atm压力打至20atm应无跳压现象； 30atm压力打至30atm应无跳压现象； 40atm压力泵需打压至40atm无跳压现象。",
                        "standardUnit": null,
                        "inspectionTool": "检测专用泵筒",
                        "equipmentOptions": [
                          {
                            "sort": 0,
                            "parameters": [],
                            "defaultFlag": true,
                            "equipmentId": 980012,
                            "equipmentCode": "C01017",
                            "equipmentName": "撤压机",
                            "equipmentNumber": "C01017"
                          }
                        ],
                        "inspectionMethod": "将推杆装到检测专用的泵筒(吸入10ML水)上，将压力打至20atm/30atm/40atm应无跳压现象。",
                        "samplingPlanText": "首件：5件 GB/T 2828.1，S-3，AQL=0.4",
                        "equipmentRequired": true,
                        "standardPrecision": null,
                        "sourceOriginalItem": "组装螺杆八组件 / 无跳压",
                        "sourceOriginalPage": null,
                        "standardLowerLimit": null,
                        "standardUpperLimit": null,
                        "sourceOriginalMethod": "将推杆装到检测专用的泵筒(吸入10ML水)上，将压力打至20atm/30atm/40atm应无跳压现象。",
                        "patrolInspectionRatio": 0.4,
                        "sourceOriginalExcerpt": "20atm压力打至20atm应无跳压现象； 30atm压力打至30atm应无跳压现象； 40atm压力泵需打压至40atm无跳压现象。",
                        "firstInspectionQuantity": 5,
                        "lastSelectedEquipmentId": 980012,
                        "applicableInspectionTypes": [
                          "FIRST",
                          "PATROL"
                        ],
                        "lastSelectedEquipmentNumber": "C01017"
                      }
                    ],
                    "dccProjectCodeId": 129,
                    "inspectionTypeRules": [
                      {
                        "key": "FIRST",
                        "label": "首检",
                        "required": true,
                        "taskRule": "按发布规程固定数量生成首检任务",
                        "roundLabel": null,
                        "releaseGate": null,
                        "fixedQuantity": null,
                        "inspectionType": "FIRST",
                        "notApplicableReason": null
                      }
                    ],
                    "regulationVersionId": 70,
                    "regulationSourceType": "PRODUCT_QA",
                    "finalInspectionApplicable": false,
                    "productionSubmitCandidates": [
                      {
                        "eventId": 282244,
                        "processId": 922987,
                        "activeOrderId": 1009200409,
                        "routeProcessId": 9908090522,
                        "serverSubmitTime": 1790933833000
                      },
                      {
                        "eventId": 282241,
                        "processId": 922986,
                        "activeOrderId": 1009200409,
                        "routeProcessId": 9908090521,
                        "serverSubmitTime": 1790916418000
                      },
                      {
                        "eventId": 282240,
                        "processId": 922985,
                        "activeOrderId": 1009200409,
                        "routeProcessId": 9908090520,
                        "serverSubmitTime": 1790822627000
                      }
                    ]
                  },
                  "inspectionResult": "SUCCESS",
                  "pieceDetailCount": 4,
                  "selectedEmployee": {
                    "userId": 9908090344,
                    "nickname": "eDHR测试一线PQC",
                    "username": "edhrTestPqc"
                  },
                  "inspectionQuantity": 4,
                  "supersededReviewId": 281244,
                  "regulationVersionId": 70,
                  "selectedActiveOrder": {
                    "routeId": 980091,
                    "quantity": 10,
                    "batchCode": "SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891",
                    "productId": 924008,
                    "routeCode": "RT000028-IDI",
                    "routeName": "按压式球囊扩充压力泵",
                    "productCode": "IDI",
                    "productName": "按压式球囊扩充压力泵",
                    "readBlocked": null,
                    "workOrderId": 990274,
                    "activeOrderId": 1009200409,
                    "workOrderCode": "SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891",
                    "workOrderName": "PQC single-process order 05-测试复制-OPYAO451788352161891",
                    "routeVersionId": 746,
                    "routeVersionNo": null,
                    "readBlockReason": null,
                    "latestSubmitTime": 1790818966000
                  },
                  "pqcSubmissionGroupId": "PQC-1f42cf84-1ca2-43a7-912a-27e512816507",
                  "productionSubmitEventId": null,
                  "actualInspectionQuantity": 4
                },
                "changeReason": "七角色E2E已批准补正签名取证：正式事件5件修改为4件及逐件样本，验证正式补正复核签名，随后通过前端恢复正式事件5件。",
                "revisionStatus": "EFFECTIVE",
                "routeProcessId": 9908090521,
                "modifiedByUserId": 9908090343,
                "revisionSignatureId": 15059,
                "revisionSignatureUserId": 9908090343,
                "revisionSignatureSnapshot": {
                  "actorId": 9908090343,
                  "signedAt": 1790989217665,
                  "actorName": "eDHR测试PQC组长",
                  "signatureId": 15059,
                  "selectedSignedAt": null,
                  "selectedTimeZone": "Asia/Shanghai",
                  "signatureTimeMode": "SERVER_TIME",
                  "selectedTimeReason": "",
                  "signatureDisplayAt": 1790989217665,
                  "selectedTimeAuditHash": "3e7a32dc42dfa9f5fad980229c7efb0f1ededd8310a95cfa5a8af64e347268db",
                  "selectedTimePolicyVersion": "EDHR_SIGNATURE_TIME_V1"
                }
              },
              "signature": {
                "id": 15059,
                "reason": "七角色E2E已批准补正签名取证：正式事件5件修改为4件及逐件样本，验证正式补正复核签名，随后通过前端恢复正式事件5件。",
                "actorId": 9908090343,
                "signedAt": "2026-10-03 09:00:18.000000",
                "tenantId": 1,
                "subjectId": "MApGSUVMRF9DSEFOR0UKCgoKCgoKCgoKCgoKCgo3NTgzZDcyY2I1MjVjZWY3OGNmMGM3NjdhODY2MDkwZTExMTJiZTRhZTQ3ZmE2Y2MzMTYyMzMxNDVlM2U2OGEx",
                "actionCode": "FIELD_CHANGE",
                "moduleCode": "MES",
                "contentHash": "b059cdf7ee29ef0723baa1c4ecd64840655a10a9050b72cb54d55bf3228abce7",
                "subjectType": "MES_BATCH_RECORD",
                "evidenceHash": "bb1ae2eb827330c8c7c34d2004ddb9094c14b3929233e0c63b1a786dc951ef6b",
                "subjectVersion": "0ad98b43ee613411447e677501301406deb494f79f4b3e51227988265d8a1188",
                "verificationStatus": "VALID",
                "canonicalContentJson": {
                  "reason": "七角色E2E已批准补正签名取证：正式事件5件修改为4件及逐件样本，验证正式补正复核签名，随后通过前端恢复正式事件5件。",
                  "adapter": "MES_BATCH_RECORD",
                  "bpmTaskId": "",
                  "actionType": "FIELD_CHANGE",
                  "bpmTaskName": "",
                  "executionId": "0",
                  "approvalResult": "",
                  "cellValuesHash": "",
                  "reviewSourceId": "",
                  "reviewSourceName": "",
                  "reviewSourceType": "",
                  "signatureCellKey": "",
                  "processInstanceId": "",
                  "signatureRowIndex": "",
                  "fieldAuditHeadHash": "",
                  "fieldAuditRevision": "",
                  "bpmTaskDefinitionKey": "",
                  "signatureColumnIndex": "",
                  "signatureChallengeHash": "7583d72cb525cef78cf0c767a866090e1112be4ae47fa6cc316233145e3e68a1"
                }
              },
              "audits": [
                {
                  "id": 18927,
                  "reason": "七角色E2E已批准补正签名取证：正式事件5件修改为4件及逐件样本，验证正式补正复核签名，随后通过前端恢复正式事件5件。",
                  "actorId": 9908090343,
                  "tenantId": 1,
                  "requestId": "MES-PQC-CORRECTION:42",
                  "subjectId": "MES_PROCESS_POOL_EVENT:282246",
                  "reasonCode": "MES_PQC_INSPECTION_CORRECT",
                  "sourceType": "SERVICE_METHOD",
                  "operationId": "mes.pqc-inspection.correct",
                  "reasonSource": "USER",
                  "resultStatus": "SUCCESS",
                  "sourceLocator": "cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolPqcInspectionCorrectionService#correct",
                  "transactionId": "b0ad8c9e-9ead-44ca-b703-57ac858deff0",
                  "signatureRecordId": "15059",
                  "eventSchemaVersion": 2,
                  "evidenceManifestJson": "[{\\"evidenceType\\":\\"SIGNATURE\\",\\"sourceId\\":\\"15059\\",\\"sourceVersion\\":null,\\"sha256\\":\\"b059cdf7ee29ef0723baa1c4ecd64840655a10a9050b72cb54d55bf3228abce7\\",\\"role\\":\\"PQC_INSPECTION_CORRECTION\\"},{\\"evidenceType\\":\\"SIGNATURE_CHALLENGE\\",\\"sourceId\\":\\"15059\\",\\"sourceVersion\\":null,\\"sha256\\":\\"7583d72cb525cef78cf0c767a866090e1112be4ae47fa6cc316233145e3e68a1\\",\\"role\\":\\"SIGNED_CORRECTION_REQUEST\\"},{\\"evidenceType\\":\\"REVISION\\",\\"sourceId\\":\\"42\\",\\"sourceVersion\\":null,\\"sha256\\":\\"3b29b9c07ad1f1b4134d6e8c863156ca448d4546ad062669496824daf4c8994f\\",\\"role\\":\\"CORRECTED_PAYLOAD\\"}]",
                  "relationManifestJson": "[{\\"relationType\\":\\"SUBJECT\\",\\"objectType\\":\\"PROCESS_POOL_EVENT\\",\\"objectId\\":\\"282246\\",\\"objectVersion\\":null,\\"objectHash\\":null},{\\"relationType\\":\\"SOURCE\\",\\"objectType\\":\\"PQC_INSPECTION_TASK\\",\\"objectId\\":\\"1009622119\\",\\"objectVersion\\":null,\\"objectHash\\":null},{\\"relationType\\":\\"SOURCE\\",\\"objectType\\":\\"ACTIVE_ORDER\\",\\"objectId\\":\\"1009200409\\",\\"objectVersion\\":null,\\"objectHash\\":null},{\\"relationType\\":\\"SOURCE\\",\\"objectType\\":\\"WORK_ORDER\\",\\"objectId\\":\\"990274\\",\\"objectVersion\\":null,\\"objectHash\\":null},{\\"relationType\\":\\"REVISION\\",\\"objectType\\":\\"REVISION\\",\\"objectId\\":\\"42\\",\\"objectVersion\\":null,\\"objectHash\\":null},{\\"relationType\\":\\"SIGNATURE\\",\\"objectType\\":\\"SIGNATURE\\",\\"objectId\\":\\"15059\\",\\"objectVersion\\":null,\\"objectHash\\":null},{\\"relationType\\":\\"PREVIOUS_REVIEW\\",\\"objectType\\":\\"PQC_REVIEW\\",\\"objectId\\":\\"281244\\",\\"objectVersion\\":null,\\"objectHash\\":null},{\\"relationType\\":\\"CORRECTION_REVIEW\\",\\"objectType\\":\\"PQC_REVIEW\\",\\"objectId\\":\\"282397\\",\\"objectVersion\\":null,\\"objectHash\\":null}]",
                  "signatureContentHash": "b059cdf7ee29ef0723baa1c4ecd64840655a10a9050b72cb54d55bf3228abce7"
                }
              ]
            }
            """;


    private void rejected() {
        ServiceException ex = assertThrows(ServiceException.class, () -> service.getTeam(341L, ACTIVE_ORDER, SIGNATURE));
        assertEquals(1_040_760_452, ex.getCode());
        verify(signatureQueryService, never()).verifyEvidence(anyLong());
    }

    private MesTeamLeaderActiveOrderDetail detail() {
        return new MesTeamLeaderActiveOrderDetail().setActiveOrderId(ACTIVE_ORDER).setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setPqcSubmissions(List.of(
                        new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail().setPqcTaskIds(List.of(140L))
                                .setSubmittedEventIds(List.of(EVENT)).setReviewerSignatures(List.of(
                                        new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(743L).setSignerName("原复核人员"),
                                        new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(SIGNATURE).setSignerName("正式补正人员")))))));
    }

    private ElectronicSignatureEvidenceDTO signatureEvidence(Long id, Long actor, String action, String sourceType,
                                                              Long sourceId, String challenge, String reason) {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action, null, null,
                null, null, null, null, null, sourceType, sourceId, null, null, null, null, null, challenge);
        String version = MesProBatchRecordExecutionFieldAuditHasher.sha256(subject);
        var snapshot = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                actor, "MES", action, "MES_BATCH_RECORD", subject, version, reason));
        String hash = MesProBatchRecordExecutionFieldAuditHasher.sha256(snapshot.canonicalContentJson());
        return new ElectronicSignatureEvidenceDTO(id, "MES", action, "MES_BATCH_RECORD", subject, version,
                actor, action, "业务签名", reason, SIGNED_AT, "time", "SESSION_PLUS_PASSWORD", hash,
                "evidence-" + id, "SHA-256", "v1", "p1", "VALID", null, null, null, null,
                snapshot.canonicalContentJson(), null, null, null, "正式补正人员", "Asia/Shanghai");
    }
}
