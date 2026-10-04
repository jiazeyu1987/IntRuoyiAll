package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditRelation;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditEvidence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MesProductionSignatureEvidenceServiceTest {
    private static final LocalDateTime SIGNED_AT = LocalDateTime.of(2026, 10, 1, 10, 43, 46);
    private static final MesProductionSubmitSignatureContext CONTEXT =
            new MesProductionSubmitSignatureContext(10L, 101L, 1L, "production-submit-1001");
    @Mock private ElectronicSignatureQueryService signatures;
    @Mock private MesProBatchRecordExecutionSignatureMapper projections;
    @Mock private GxpAuditEventMapper audits;
    @Mock private MesProcessPoolActiveOrderMapper activeOrders;
    @Mock private MesProProcessPoolEventMapper events;
    private MesProductionSignatureEvidenceService service;
    private MesProProcessPoolEventDO event;
    private GxpAuditEventDO productionAudit;
    private GxpAuditEventDO signatureAudit;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        service = new MesProductionSignatureEvidenceService(signatures, projections, audits, activeOrders, events);
        event = MesProProcessPoolEventDO.builder().id(1001L)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .eventIdempotencyKey(CONTEXT.submissionIdempotencyKey())
                .workOrderId(30L).routeId(40L).routeProcessId(101L).processId(1L)
                .actualEmployeeId(2101L).signatureId(1101L).signatureUserId(2101L)
                .serverSubmitTime(SIGNED_AT.plusSeconds(1)).rawPayload("{\"activeOrderId\":10}")
                .feedbackSourceType("MES_PRO_FEEDBACK").feedbackSourceId(501L).build();
        when(activeOrders.selectById(10L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(10L).workOrderId(30L).routeId(40L).build());
        when(events.selectList(any())).thenReturn(List.of(event));
        productionAudit = productionAudit();
        stubUnified(CONTEXT, 2101L);
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void exactAuthenticatedSignatureAndOriginalAuditAcceptMissingEventCache() {
        assertTrue(service.isValidForEvent(event));
        assertNull(event.getSignatureSnapshot(), "Read-only evidence resolution must preserve the original event");
        verify(signatures).verifyEvidence(1101L);
        verify(events, never()).updateById(any(MesProProcessPoolEventDO.class));
    }

    @Test void originalUnscopedSignatureUsesTheSameMandatoryFormalTransactionAssociation() {
        stubUnified(null, 2101L);
        assertTrue(service.isValidForEvent(event));
        assertNull(event.getSignatureSnapshot());
    }

    @Test void newSubmissionCannotCreateAnUnscopedSignatureSnapshot() {
        stubUnified(null, 2101L);
        assertThrows(ServiceException.class, () -> service.snapshotForSubmission(2101L, 1101L, CONTEXT));
    }

    @Test void missingOriginalProductionAuditRejectsEvenAnAuthenticScopedSignature() {
        when(audits.selectList(any())).thenReturn(List.of(signatureAudit), List.of());
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void unscopedSignatureCannotPassWithoutItsExactOriginalProductionAudit() {
        stubUnified(null, 2101L);
        when(audits.selectList(any())).thenReturn(List.of(signatureAudit), List.of());
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void verifiedCachedSnapshotMatchesTheFormalLedger() {
        when(audits.selectList(any())).thenReturn(List.of(signatureAudit), List.of(signatureAudit), List.of(productionAudit));
        event.setSignatureSnapshot(service.snapshotForSubmission(2101L, 1101L, CONTEXT));
        assertTrue(service.isValidForEvent(event));
    }

    @Test void arbitraryNonemptyCachedSnapshotIsRejected() {
        event.setSignatureSnapshot("{\"signedAt\":\"2026-10-01T10:43:46\"}");
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void missingFormalSignatureIsRejected() {
        when(signatures.getById(1101L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void wrongFormalActorIsRejected() {
        stubUnified(CONTEXT, 2102L);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void wrongActiveOrderInSignedSubjectIsRejected() {
        stubUnified(new MesProductionSubmitSignatureContext(11L, 101L, 1L, CONTEXT.submissionIdempotencyKey()), 2101L);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void wrongProcessInSignedSubjectIsRejected() {
        stubUnified(new MesProductionSubmitSignatureContext(10L, 102L, 2L, CONTEXT.submissionIdempotencyKey()), 2101L);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void wrongSubmissionKeyInSignedSubjectIsRejected() {
        stubUnified(new MesProductionSubmitSignatureContext(10L, 101L, 1L, "another-submit"), 2101L);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void wrongWorkOrderAgainstFormalActiveOrderIsRejected() {
        event.setWorkOrderId(31L);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void missingSignatureCreationAuditIsRejectedEvenWithAValidProductionAudit() {
        when(audits.selectList(any())).thenReturn(List.of());
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void authenticSignatureFromAnotherTransactionIsRejected() {
        signatureAudit.setTransactionId("another-production-transaction");
        seal(signatureAudit);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void corruptedSignatureCreationAuditIsRejected() {
        signatureAudit.setEventHash("corrupted");
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void wrongFormalSubjectTypeIsRejected() {
        var evidence = stubUnified(CONTEXT, 2101L);
        when(signatures.getById(1101L)).thenReturn(replaceSubject(evidence, "OTHER_SUBJECT", evidence.subjectVersion(),
                evidence.canonicalContentJson()));
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void wrongFormalSubjectVersionIsRejected() {
        var evidence = stubUnified(CONTEXT, 2101L);
        when(signatures.getById(1101L)).thenReturn(replaceSubject(evidence, evidence.subjectType(), "another-version",
                evidence.canonicalContentJson()));
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void canonicalContentCannotDisagreeWithTheEncodedSubject() {
        var evidence = stubUnified(CONTEXT, 2101L);
        Map<String, Object> content = JsonUtils.parseObject(evidence.canonicalContentJson(), Map.class);
        content.put("reviewSourceId", "11");
        when(signatures.getById(1101L)).thenReturn(replaceSubject(evidence, evidence.subjectType(), evidence.subjectVersion(),
                JsonUtils.toJsonString(content)));
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void freshHashMismatchIsRejectedDespiteStoredValidStatus() {
        when(signatures.verifyEvidence(1101L)).thenReturn(new ElectronicSignatureVerificationDTO(1101L,
                "MISMATCH", "content-hash", "tampered", "evidence-hash", "evidence-hash", "SHA-256", "v1"));
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void corruptedOriginalAuditHashIsRejected() {
        productionAudit.setEventHash("corrupted");
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void copiedSignatureFkOnASecondProductionEventIsRejected() {
        when(events.selectList(any())).thenReturn(List.of(event, event));
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void auditColumnsCannotBeChangedOutsideTheirHashBearingCanonicalEvent() {
        productionAudit.setSignatureRecordId("1102");
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void auditedWrongProcessIsRejectedEvenWhenItsOwnHashIsValid() {
        var after = JsonUtils.parseObject(productionAudit.getAfterStateJson(), Map.class);
        after.put("processId", 2L);
        productionAudit.setAfterStateJson(JsonUtils.toJsonString(after));
        seal(productionAudit);
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }

    @Test void unifiedProfileEvidenceKeepsIdentityDomainThroughSnapshotAndEventVerification() {
        stubUnifiedProfile();
        var snapshot = JsonUtils.parseObject(service.snapshotForSubmission(2101L, 1101L, CONTEXT), Map.class);
        assertEquals("UNIFIED_EMPLOYEE_PROFILE", snapshot.get("authority"));
        when(audits.selectList(any())).thenReturn(List.of(signatureAudit), List.of(productionAudit));
        assertTrue(service.isValidForEvent(event));
        verifyNoInteractions(projections);
    }
    @Test void sameNumericSignerWithWrongEventDomainIsRejected() {
        stubUnifiedProfile();
        var payload = com.alibaba.fastjson.JSON.parseObject(event.getRawPayload());
        payload.put("signatureIdentityDomain", "SYSTEM_USER"); event.setRawPayload(payload.toJSONString());
        assertThrows(ServiceException.class, () -> service.isValidForEvent(event));
    }
    private void stubUnifiedProfile() {
        var evidence = stubUnified(CONTEXT, 2101L);
        var content = com.alibaba.fastjson.JSON.parseObject(evidence.canonicalContentJson());
        content.put("signatureIdentity", Map.of("domain", "MES_EMPLOYEE_PROFILE", "signerId", 2101L,
                "operatorId", 9001L, "tenantId", 1L, "displayName", "临时员工"));
        var profile = new ElectronicSignatureEvidenceDTO(evidence.id(), evidence.moduleCode(), evidence.actionCode(),
                evidence.subjectType(), evidence.subjectId(), evidence.subjectVersion(), evidence.actorId(),
                evidence.meaningCode(), evidence.meaningLabel(), evidence.reason(), evidence.signedAt(), evidence.timeEvidenceId(),
                "SESSION_PLUS_EMPLOYEE_PROFILE_PASSWORD", evidence.contentHash(), evidence.evidenceHash(), evidence.algorithm(),
                evidence.keyVersion(), evidence.policyVersion(), evidence.verificationStatus(), evidence.processInstanceId(),
                evidence.taskId(), evidence.nodeCode(), evidence.nodeOrder(), content.toJSONString(), evidence.beforeContentJson(),
                evidence.afterContentJson(), evidence.fieldDiffJson(), "临时员工", evidence.timeZone());
        when(signatures.getById(1101L)).thenReturn(profile);
        signatureAudit = signatureAudit(profile);
        productionAudit.setPerformedByJson(JsonUtils.toJsonString(Map.of("actorId", 2101L, "actorType", "MES_EMPLOYEE_PROFILE")));
        seal(productionAudit);
        event.setDeviceAccountId(9001L); event.setTenantId(1L);
        var payload = com.alibaba.fastjson.JSON.parseObject(event.getRawPayload());
        payload.put("signatureIdentityDomain", "MES_EMPLOYEE_PROFILE"); event.setRawPayload(payload.toJSONString());
        when(audits.selectList(any())).thenReturn(List.of(signatureAudit), List.of(productionAudit));
    }

    @Test void legacyProfileProjectionCannotMasqueradeAsUnifiedSignature() {
        when(signatures.getById(1101L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.snapshotForSubmission(2101L, 1101L, CONTEXT));
        verifyNoInteractions(projections);
    }

    @Test void numericCollisionInLegacyTableDoesNotChangeUnifiedAuthority() {
        String snapshot = service.snapshotForSubmission(2101L, 1101L, CONTEXT);
        assertEquals("UNIFIED_SYSTEM_USER", JsonUtils.parseObject(snapshot, Map.class).get("authority"));
        verifyNoInteractions(projections);
    }

    private MesProBatchRecordExecutionSignatureDO profileProjection() {
        return MesProBatchRecordExecutionSignatureDO.builder().id(1101L).executionId(0L).actorId(2101L)
                .actionType("PRODUCTION_SUBMIT").signedAt(SIGNED_AT).passwordVerified(true)
                .signatureMode("PASSWORD").authenticationMethod("PASSWORD")
                .authorizationBasis("生产人员档案电子签名密码已验证")
                .reviewSourceType(MesProductionSubmitSignatureContext.SOURCE_TYPE).reviewSourceId(10L)
                .reviewSourceName(CONTEXT.projectionSourceName()).actorName("正式员工")
                .actorUsernameSnapshot("EMP-2101").actorNicknameSnapshot("正式员工").signaturePurpose("一线生产报工提交").build();
    }

    private ElectronicSignatureEvidenceDTO stubUnified(MesProductionSubmitSignatureContext context, Long actorId) {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "PRODUCTION_SUBMIT", null,
                null, null, null, null, null, null, context == null ? null : MesProductionSubmitSignatureContext.SOURCE_TYPE,
                context == null ? null : context.activeOrderId(), context == null ? null : context.sourceName(),
                null, null, null, null, null);
        var snapshot = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                actorId, "MES", "PRODUCTION_SUBMIT", "MES_BATCH_RECORD", subject, DigestUtil.sha256Hex(subject), "生产提交"));
        var evidence = new ElectronicSignatureEvidenceDTO(1101L, "MES", "PRODUCTION_SUBMIT", "MES_BATCH_RECORD",
                subject, DigestUtil.sha256Hex(subject), actorId, "PRODUCTION_SUBMIT", "生产提交", "生产提交", SIGNED_AT,
                "SERVER_CLOCK:" + SIGNED_AT, "SESSION_PLUS_PASSWORD", "content-hash", "evidence-hash", "SHA-256", "v1",
                "mes-batch-record-signature-v1", "VALID", null, null, null, null, snapshot.canonicalContentJson(),
                null, null, null, "正式员工");
        when(signatures.getById(1101L)).thenReturn(evidence);
        when(signatures.verifyEvidence(1101L)).thenReturn(new ElectronicSignatureVerificationDTO(1101L, "VALID",
                "content-hash", "content-hash", "evidence-hash", "evidence-hash", "SHA-256", "v1"));
        signatureAudit = signatureAudit(evidence);
        when(audits.selectList(any())).thenReturn(List.of(signatureAudit), List.of(productionAudit));
        return evidence;
    }

    private static ElectronicSignatureEvidenceDTO replaceSubject(ElectronicSignatureEvidenceDTO evidence,
                                                                 String type, String version, String canonical) {
        return new ElectronicSignatureEvidenceDTO(evidence.id(), evidence.moduleCode(), evidence.actionCode(), type,
                evidence.subjectId(), version, evidence.actorId(), evidence.meaningCode(), evidence.meaningLabel(),
                evidence.reason(), evidence.signedAt(), evidence.timeEvidenceId(), evidence.authenticationMethod(),
                evidence.contentHash(), evidence.evidenceHash(), evidence.algorithm(), evidence.keyVersion(),
                evidence.policyVersion(), evidence.verificationStatus(), evidence.processInstanceId(), evidence.taskId(),
                evidence.nodeCode(), evidence.nodeOrder(), canonical, evidence.beforeContentJson(), evidence.afterContentJson(),
                evidence.fieldDiffJson(), evidence.actorDisplayName(), evidence.timeZone());
    }

    private static GxpAuditEventDO profileAudit(MesProBatchRecordExecutionSignatureDO projection) {
        var audit = baseAudit("mes.employee-production-signature.create", "MES_SIGNATURE_PROJECTION:1101");
        audit.setRequestId("MES-SIG:1101");
        audit.setAfterStateJson(JsonUtils.toJsonString(projection));
        audit.setAfterObjectVersion(DigestUtil.sha256Hex(audit.getAfterStateJson()));
        audit.setSubjectVersion(audit.getAfterObjectVersion());
        audit.setSourceLocator(MesProBatchRecordExecutionSignatureService.class.getName() + "#recordProductionSubmitSignature");
        audit.setPerformedByJson(JsonUtils.toJsonString(Map.of("identityDomain", "MES_EMPLOYEE_PROFILE", "id", 2101L, "tenantId", 1L)));
        seal(audit);
        return audit;
    }

    private static GxpAuditEventDO signatureAudit(ElectronicSignatureEvidenceDTO evidence) {
        var audit = baseAudit("signature.record.create", evidence.subjectType() + ":" + evidence.subjectId());
        audit.setId(14279L);
        audit.setActorId(evidence.actorId());
        audit.setSubjectVersion(evidence.subjectVersion());
        audit.setSignatureRecordId(evidence.id().toString());
        audit.setSignatureContentHash(evidence.contentHash());
        audit.setBeforeState("NO_SIGNATURE_RECORD");
        audit.setAfterState("ELECTRONIC_SIGNATURE_RECORDED");
        audit.setSourceLocator("ElectronicSignatureServiceImpl.sign");
        audit.setAfterStateJson(JsonUtils.toJsonString(Map.of("signatureId", evidence.id(), "actorId", evidence.actorId(),
                "moduleCode", evidence.moduleCode(), "actionCode", evidence.actionCode(), "subjectType", evidence.subjectType(),
                "subjectId", evidence.subjectId(), "subjectVersion", evidence.subjectVersion(), "contentHash", evidence.contentHash(),
                "evidenceHash", evidence.evidenceHash(), "verificationStatus", evidence.verificationStatus())));
        seal(audit);
        return audit;
    }

    private GxpAuditEventDO productionAudit() {
        var audit = baseAudit("mes.production.submit", "MES_PROCESS_POOL_EVENT:1001");
        audit.setSubjectVersion("1001"); audit.setSignatureRecordId("1101");
        audit.setIdempotencyKey("PRODUCTION_SUBMIT_EVENT:1001"); audit.setRequestId("MES-PRODUCTION-SUBMIT:1001");
        audit.setReasonCode("MES_PRODUCTION_SUBMIT"); audit.setReasonSource("SYSTEM");
        audit.setSourceLocator("cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackSubmitServiceImpl#submit");
        audit.setAfterStateJson(JsonUtils.toJsonString(Map.of("profile", "PRODUCTION", "eventId", 1001L,
                "feedbackId", 501L, "activeOrderId", 10L, "routeProcessId", 101L, "processId", 1L,
                "actualEmployeeId", 2101L, "signatureEmployeeId", 2101L, "signatureId", 1101L)));
        Map<String, Object> after = JsonUtils.parseObject(audit.getAfterStateJson(), Map.class);
        after.put("formalSourceSnapshot", Map.of("snapshotId", "formal-snapshot-1001", "snapshotHash", "formal-source-hash",
                "content", Map.of("routeProcessId", 101L, "processId", 1L)));
        after.put("formalSourceSnapshotHash", "formal-source-hash");
        audit.setAfterStateJson(JsonUtils.toJsonString(after));
        audit.setPerformedByJson(JsonUtils.toJsonString(Map.of("actorId", 2101L, "actorType", "SYSTEM_USER")));
        audit.setEvidenceManifestJson(JsonUtils.toJsonString(List.of(new GxpAuditEvidence("FORMAL_SOURCE_SNAPSHOT",
                "formal-snapshot-1001", null, "formal-source-hash", "PRODUCTION"))));
        audit.setRelationManifestJson(JsonUtils.toJsonString(List.of(
                new GxpAuditRelation("SUBJECT", "ACTIVE_ORDER", "10", null, null),
                new GxpAuditRelation("SOURCE", "FEEDBACK", "501", null, null),
                new GxpAuditRelation("SOURCE", "PROCESS_POOL_EVENT", "1001", "1001", null),
                new GxpAuditRelation("SIGNATURE", "SIGNATURE", "1101", null, null))));
        seal(audit);
        return audit;
    }

    private static GxpAuditEventDO baseAudit(String operation, String subject) {
        var audit = new GxpAuditEventDO();
        audit.setId(14281L); audit.setTenantId(1L); audit.setActorId(2101L);
        audit.setOperationId(operation); audit.setSubjectId(subject); audit.setTransactionId("formal-production-transaction");
        audit.setBeforeState("ABSENT"); audit.setAfterState("PRESENT"); audit.setBeforeStateJson("{}");
        audit.setEventSchemaVersion(2); audit.setAlgorithm("SHA-256");
        audit.setCanonicalizationVersion("GXP_CANONICAL_V2"); audit.setResultStatus("SUCCESS");
        audit.setSourceType("SERVICE_METHOD");
        return audit;
    }

    private static void seal(GxpAuditEventDO audit) {
        audit.setStatePayloadHash(DigestUtil.sha256Hex(audit.getBeforeStateJson() + "\u001f" + audit.getAfterStateJson()));
        Map<String, Object> canonical = new TreeMap<>();
        var bean = new BeanWrapperImpl(audit);
        for (var descriptor : bean.getPropertyDescriptors()) {
            if (!List.of("class", "canonicalEventJson", "eventHash").contains(descriptor.getName()))
                canonical.put(descriptor.getName(), bean.getPropertyValue(descriptor.getName()));
        }
        audit.setCanonicalEventJson(JsonUtils.toJsonString(canonical));
        audit.setEventHash(DigestUtil.sha256Hex(audit.getCanonicalEventJson()));
    }
}
