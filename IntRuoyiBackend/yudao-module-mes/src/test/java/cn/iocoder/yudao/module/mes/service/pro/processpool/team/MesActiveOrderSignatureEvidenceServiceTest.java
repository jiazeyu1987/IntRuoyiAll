package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSubmitSignatureContext;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Verification DTOs are consumer-port fixtures, not evidence of the creation-audit algorithm.
class MesActiveOrderSignatureEvidenceServiceTest {
    @Mock MesTeamLeaderActiveOrderDetailService teamDetailService;
    @Mock MesPqcReleaseOrderDetailService pqcDetailService;
    @Mock MesProEdhrBatchActiveOrderDetailService batchDetailService;
    @Mock ElectronicSignatureQueryService signatureQueryService;
    @Mock MesProProcessPoolEventMapper eventMapper;
    @Mock MesProcessPoolSubmissionReviewMapper reviewMapper;
    @Mock MesProductionSubmissionReadBinding productionBinding;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper batchMapper;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper revisionMapper;
    @InjectMocks MesActiveOrderSignatureEvidenceService service;

    @BeforeEach void verifiedPort() { cn.iocoder.yudao.module.mes.service.pro.MesVerifiedConsumerFixture.wire(service, signatureQueryService, 1L); }

    @ParameterizedTest
    @ValueSource(strings = {"VALID", "MISMATCH", "INCOMPLETE"})
    void v2PqcEvidenceUsesRegisteredProtocolAndPreservesDiagnosticStatus(String status) {
        var evidence = v2PqcEvidence(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence("正式放行意见", false));
        stubPqc(evidence);
        when(signatureQueryService.verifyEvidence(7701L)).thenReturn(v2PqcVerification(evidence, status));
        var result = service.getTeam(3001L, 8101L, 7701L);
        assertEquals(status, result.verification().verificationStatus());
        assertEquals("VALID".equals(status) ? "签署时正式姓名" : "签时姓名未核验", result.signerName());
        assertSame(evidence, result.evidence());
        var application = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcApplication();
        application.setPqcDecidedBy(3002L);
        assertThrows(RuntimeException.class, () -> MesActiveOrderSignatureEvidenceService.requirePqcReadBinding(
                MesTeamLeaderActiveOrderDetailServiceImplTest.pqcDetail(), application,
                MesTeamLeaderActiveOrderDetailServiceImplTest.pqcTask(), MesTeamLeaderActiveOrderDetailServiceImplTest.pqcBatch(),
                evidence, v2PqcVerification(evidence, status)));
    }

    static ElectronicSignatureEvidenceDTO v2PqcEvidence(ElectronicSignatureEvidenceDTO original) {
        var content = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(original.canonicalContentJson());
        content.put("evidenceFormatVersion", "V2"); content.put("commandHashVersion", "V2");
        var identity = content.putObject("signatureIdentity");
        identity.put("domain", "SYSTEM_USER"); identity.put("signerId", original.actorId().toString());
        identity.put("operatorId", original.actorId().toString()); identity.put("tenantId", "1"); identity.put("displayName", "签署时正式姓名");
        var time = content.putObject("signatureTime");
        time.put("localDateTime", original.signedAt().toString()); time.put("zoneId", "Asia/Shanghai");
        time.put("precision", "SECOND"); time.put("source", "SERVER_CLOCK");
        content.putObject("signatureImage").put("state", "NOT_REQUIRED").putNull("snapshot");
        var position = content.putObject("nodePosition");position.put("applicability", "NOT_APPLICABLE");
        position.putNull("taskId"); position.putNull("nodeCode"); position.putNull("processInstanceId");
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(original));
        json.put("canonicalContentJson", content.toString());json.put("actorDisplayName", "签署时正式姓名");
        json.put("identitySnapshotStatus", "SNAPSHOT");json.put("keyVersion", "digest-format-v2");
        return cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class);
    }
    static ElectronicSignatureVerificationDTO v2PqcVerification(ElectronicSignatureEvidenceDTO evidence, String status) {
        return new ElectronicSignatureVerificationDTO(evidence.id(), status, evidence.contentHash(), evidence.contentHash(),
                evidence.evidenceHash(), evidence.evidenceHash(), evidence.algorithm(), evidence.keyVersion(),
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "VALID".equals(status) ? "CONSISTENT" : "MISMATCH".equals(status) ? "MISMATCH" : "MISSING",
                "VALID".equals(status) ? List.of() : List.of("CONSUMER_PORT_FAILURE_FIXTURE"), LocalDateTime.now(), "V2");
    }
    @ParameterizedTest @NullSource
    @ValueSource(strings = {"PQC_RELEASE", "APPROVE", "首行\n次行\r\n制表\t引号\"路径\\"})
    void explicitPqcUserAndSystemSourceReadThroughFormalBinding(String opinion) {
        var evidence = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence(opinion, false);
        stubPqc(evidence);
        var result = service.getTeam(3001L, 8101L, 7701L);
        assertSame(evidence, result.evidence());
        assertEquals("VALID", result.verification().verificationStatus());
        assertEquals(opinion, MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(evidence.subjectId(), "PQC_RELEASE", evidence.reason()));
    }

    @Test void legacyPqcSubjectContentHashesAndClockAreNotRewrittenOrClassified() {
        var evidence = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence("PQC_RELEASE", true);
        stubPqc(evidence);
        var result = service.getTeam(3001L, 8101L, 7701L);
        assertSame(evidence, result.evidence());
        assertNull(MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(evidence.subjectId(), "PQC_RELEASE", evidence.reason()));
        assertEquals(evidence.canonicalContentJson(), result.evidence().canonicalContentJson());
        assertEquals(evidence.contentHash(), result.evidence().contentHash());
        assertEquals(evidence.evidenceHash(), result.evidence().evidenceHash());
        assertEquals(evidence.signedAt(), result.evidence().signedAt());
    }

    @Test void absentPqcSigningTimeStaysNullAndDoesNotHideRelatedRecord() {
        var original = MesTeamLeaderActiveOrderDetailServiceImplTest.withSystemUserSnapshot(
                MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence("历史意见", true));
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(original));
        json.putNull("signedAt"); json.putNull("timeEvidenceId");
        var evidence = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class);
        stubPqc(evidence);
        when(signatureQueryService.verifyEvidence(7701L)).thenReturn(new ElectronicSignatureVerificationDTO(
                7701L, "INCOMPLETE", evidence.contentHash(), evidence.contentHash(), evidence.evidenceHash(), evidence.evidenceHash(),
                evidence.algorithm(), evidence.keyVersion(), "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "MISSING", "UNKNOWN",
                "NOT_BOUND", "CONSISTENT", List.of("SIGNED_AT_MISSING"), LocalDateTime.of(2026, 10, 7, 10, 0), "V1"));
        var result = service.getTeam(3001L, 8101L, 7701L);
        assertNull(result.evidence().signedAt());
        assertEquals("INCOMPLETE", result.verification().verificationStatus());
        assertEquals("签时姓名未核验", result.signerName());
    }

    @Test void onlyExactSystemUserSnapshotBoundToCanonicalActorTenantAndAuthenticationIsAccepted() {
        var original = MesTeamLeaderActiveOrderDetailServiceImplTest.withSystemUserSnapshot(
                MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence("签时意见", true));
        stubPqc(original);
        assertEquals("VALID", service.getTeam(3001L, 8101L, 7701L).verification().verificationStatus());
        for (String field : List.of("signerId", "tenantId", "operatorId", "domain", "displayName", "extra", "canonicalExtra")) {
            var content = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(original.canonicalContentJson());
            var identity = (com.fasterxml.jackson.databind.node.ObjectNode) content.get("signatureIdentity");
            if (field.equals("canonicalExtra")) content.put(field, "unrelated");
            else if (field.equals("domain")) identity.put(field, "MES_EMPLOYEE_PROFILE");
            else if (field.equals("displayName")) identity.put(field, "");
            else identity.put(field, field.equals("operatorId") ? 0L : 2L);
            var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(original));
            json.put("canonicalContentJson", content.toString());
            when(signatureQueryService.getById(7701L)).thenReturn(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class));
            assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L), field);
        }
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(original));
        json.put("authenticationMethod", "SESSION_PLUS_PASSWORD");
        when(signatureQueryService.getById(7701L)).thenReturn(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class));
        assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L));
    }

    @Test void digestConsistentButMissingAuditOrTimeIsIncompleteAndCannotClaimValid() {
        var evidence = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence(null, false);
        stubPqc(evidence);
        for (var missing : List.of(new String[]{"MISSING", "CONSISTENT"}, new String[]{"CONSISTENT", "MISSING"})) {
            var incomplete = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcVerification(evidence, "INCOMPLETE", "CONSISTENT", missing[0], missing[1]);
            when(signatureQueryService.verifyEvidence(7701L)).thenReturn(incomplete);
            var result = service.getTeam(3001L, 8101L, 7701L);
            assertSame(incomplete, result.verification());
            assertEquals("INCOMPLETE", result.verification().verificationStatus());
            assertEquals("NOT_BOUND", result.verification().timeTrustStatus());
            when(signatureQueryService.verifyEvidence(7701L)).thenReturn(
                    MesTeamLeaderActiveOrderDetailServiceImplTest.pqcVerification(evidence, "VALID", "CONSISTENT", missing[0], missing[1]));
            assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L));
        }
    }

    @Test void allOriginalIdentitySourceReceiptAndHashMismatchesAreRejected() {
        var evidence = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcEvidence("PQC_RELEASE", false);
        stubPqc(evidence);
        for (String field : List.of("tenantId", "activeOrderId", "workOrderId", "batchExecutionId", "pqcReleaseWorkTaskId", "pqcDecidedBy")) {
            var application = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcApplication();
            org.springframework.test.util.ReflectionTestUtils.setField(application, field, 2L);
            when(applicationMapper.selectOne(any())).thenReturn(application);
            assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L), field);
        }
        var application = MesTeamLeaderActiveOrderDetailServiceImplTest.pqcApplication();
        when(applicationMapper.selectOne(any())).thenReturn(application);
        application.setDossierSummaryJson(application.getDossierSummaryJson().replace("7701", "7702"));
        assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L));
        when(applicationMapper.selectOne(any())).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcApplication());
        when(workTaskMapper.selectOne(any())).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcTask().setBusinessScopeId(8802L));
        assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L));
        when(workTaskMapper.selectOne(any())).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcTask());
        for (String field : List.of("subjectVersion", "contentHash", "evidenceHash", "canonicalContentJson")) {
            var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(evidence));
            json.put(field, field.equals("canonicalContentJson") ? "{}" : "foreign");
            when(signatureQueryService.getById(7701L)).thenReturn(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class));
            assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L), field);
        }
        String raw = new String(java.util.Base64.getUrlDecoder().decode(evidence.subjectId()), java.nio.charset.StandardCharsets.UTF_8);
        for (String source : List.of("MES_OPINION_SOURCE_V2:USER", "MES_OPINION_SOURCE_V1:SYSTEM")) {
            String subject = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw.replace("MES_OPINION_SOURCE_V1:USER", source).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(evidence));
            json.put("subjectId", subject); json.put("subjectVersion", MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject));
            when(signatureQueryService.getById(7701L)).thenReturn(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class));
            assertThrows(RuntimeException.class, () -> service.getTeam(3001L, 8101L, 7701L), source);
        }
    }

    private void stubPqc(ElectronicSignatureEvidenceDTO evidence) {
        when(teamDetailService.getDetail(3001L, 8101L)).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcDetail());
        lenient().when(applicationMapper.selectOne(any())).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcApplication());
        lenient().when(workTaskMapper.selectOne(any())).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcTask());
        lenient().when(batchMapper.selectOne(any())).thenReturn(MesTeamLeaderActiveOrderDetailServiceImplTest.pqcBatch());
        when(signatureQueryService.getById(7701L)).thenReturn(evidence);
        lenient().when(signatureQueryService.verifyEvidence(7701L)).thenReturn(
                MesTeamLeaderActiveOrderDetailServiceImplTest.pqcVerification(evidence, "VALID", "CONSISTENT", "CONSISTENT", "CONSISTENT"));
    }

    @org.junit.jupiter.api.AfterEach void clearTenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }
    @BeforeEach void sourceForeignKeys() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        lenient().when(productionBinding.require(any(), any())).thenReturn(
                new MesProductionSubmitSignatureContext(409L,520L,985L,"submit-one"));
        MesProProcessPoolEventDO productionEvent = new MesProProcessPoolEventDO()
                .setId(240L).setSignatureId(742L).setSignatureUserId(342L).setDeviceAccountId(341L)
                .setRawPayload("{\"signatureIdentityDomain\":\"SYSTEM_USER\"}");
        productionEvent.setTenantId(1L);
        lenient().when(eventMapper.selectById(240L)).thenReturn(productionEvent);
        MesProProcessPoolEventDO reviewEvent = new MesProProcessPoolEventDO()
                .setId(241L).setSignatureId(744L).setSignatureUserId(344L).setDeviceAccountId(341L);
        reviewEvent.setTenantId(1L);
        lenient().when(eventMapper.selectById(241L)).thenReturn(reviewEvent);
        lenient().when(reviewMapper.selectListByEventId(240L)).thenReturn(List.of(
                new MesProcessPoolSubmissionReviewDO().setEventId(240L).setReviewSignatureId(743L).setReviewSignatureUserId(341L)));
    }

    @Test void nonOwnerCannotReadAnySignature() {
        when(teamDetailService.getDetail(399L, 409L)).thenThrow(new IllegalStateException("owner denied"));
        assertThrows(IllegalStateException.class, () -> service.getTeam(399L, 409L, 742L));
        verifyNoInteractions(signatureQueryService);
    }

    @Test void unrelatedIdCannotBecomeARecordLookup() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 999L));
        verifyNoInteractions(signatureQueryService);
    }

    @Test void productionForeignKeyReadsUnifiedEvidenceAndActualSignatureTime() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        stub(742L, "PRODUCTION_SUBMIT", null, null, "VALID");
        var result = service.getTeam(341L, 409L, 742L);
        assertEquals(742L, result.evidence().id());
        assertEquals("冻结签名者B", result.signerName());
        // The formal detail's submittedAt is not the signature clock: do not invent a time identity.
        assertEquals(LocalDateTime.of(2026, 10, 1, 10, 43, 46), result.evidence().signedAt());
        assertEquals("VALID", result.verification().verificationStatus());
        verify(signatureQueryService).verifyEvidence(742L);
    }

    @Test void reviewRequiresExactFormalEventSource() {
        when(signatureQueryService.verifyEvidence(anyLong())).thenAnswer(call -> {
            Long id = call.getArgument(0); return new ElectronicSignatureVerificationDTO(id,"VALID","content","content","evidence","evidence","SHA-256","v1",
                    "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT","CONSISTENT","CONSISTENT","SERVER_CLOCK","NOT_BOUND","CONSISTENT",List.of(),LocalDateTime.of(2026,10,7,12,0),"V1");
        });
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(743L)).thenReturn(evidence(743L, "TEAM_LEADER_REVIEW", "PROCESS_POOL_EVENT", 888L));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 743L));
        verify(signatureQueryService, times(1)).verifyEvidence(anyLong());
    }

    @Test void newProductionIdentityMustMatchExactFormalSubmission() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        MesProProcessPoolEventDO productionEvent = new MesProProcessPoolEventDO()
                .setId(240L).setSignatureId(742L).setSignatureUserId(342L).setDeviceAccountId(341L)
                .setRawPayload("{\"signatureIdentityDomain\":\"SYSTEM_USER\"}")
                .setRouteProcessId(520L).setProcessId(985L).setEventIdempotencyKey("submit-one");
        productionEvent.setTenantId(1L);
        when(eventMapper.selectById(240L)).thenReturn(productionEvent);
        stub(742L, "PRODUCTION_SUBMIT", null, null, "VALID");
        when(signatureQueryService.getById(742L)).thenReturn(productionEvidence(
                new MesProductionSubmitSignatureContext(409L, 520L, 985L, "submit-one")));
        assertEquals("VALID", service.getTeam(341L, 409L, 742L).verification().verificationStatus());
        for (var context : List.of(new MesProductionSubmitSignatureContext(410L, 520L, 985L, "submit-one"),
                new MesProductionSubmitSignatureContext(409L, 521L, 985L, "submit-one"),
                new MesProductionSubmitSignatureContext(409L, 520L, 986L, "submit-one"),
                new MesProductionSubmitSignatureContext(409L, 520L, 985L, "submit-two"))) {
            when(signatureQueryService.getById(742L)).thenReturn(productionEvidence(context));
            assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
        }
        verify(signatureQueryService, times(5)).verifyEvidence(742L);
    }

    private ElectronicSignatureEvidenceDTO productionEvidence(MesProductionSubmitSignatureContext context) {
        var original = evidence(742L, "PRODUCTION_SUBMIT", context.SOURCE_TYPE, context.activeOrderId());
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "PRODUCTION_SUBMIT",
                null, null, null, null, null, null, null, context.SOURCE_TYPE, context.activeOrderId(),
                context.sourceName(), null, null, null, null, null);
        var content = com.alibaba.fastjson.JSON.parseObject(original.canonicalContentJson());
        content.put("reviewSourceName", context.sourceName());
        return new ElectronicSignatureEvidenceDTO(original.id(), original.moduleCode(), original.actionCode(),
                original.subjectType(), subject, MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), original.actorId(), original.meaningCode(),
                original.meaningLabel(), original.reason(), original.signedAt(), original.timeEvidenceId(),
                original.authenticationMethod(), original.contentHash(), original.evidenceHash(), original.algorithm(),
                original.keyVersion(), original.policyVersion(), original.verificationStatus(), original.processInstanceId(),
                original.taskId(), original.nodeCode(), original.nodeOrder(), content.toJSONString(),
                original.beforeContentJson(), original.afterContentJson(), original.fieldDiffJson(),
                original.actorDisplayName(), original.timeZone());
    }

    @Test void exactReviewEventIsAccepted() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        stub(743L, "TEAM_LEADER_REVIEW", "PROCESS_POOL_EVENT", 240L, "VALID");
        assertEquals(743L, service.getTeam(341L, 409L, 743L).evidence().id());
    }

    @Test void wrongActionAndMissingUnifiedTenantRecordAreRejected() {
        when(signatureQueryService.verifyEvidence(anyLong())).thenAnswer(call -> {
            Long id = call.getArgument(0); return new ElectronicSignatureVerificationDTO(id,"VALID","content","content","evidence","evidence","SHA-256","v1",
                    "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT","CONSISTENT","CONSISTENT","SERVER_CLOCK","NOT_BOUND","CONSISTENT",List.of(),LocalDateTime.of(2026,10,7,12,0),"V1");
        });
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PQC_SUBMIT", null, null));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
        when(signatureQueryService.getById(742L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
        verify(signatureQueryService, times(1)).verifyEvidence(anyLong());
    }

    @Test void storedValidNeverMasksActualMismatch() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        stub(742L, "PRODUCTION_SUBMIT", null, null, "MISMATCH");
        var result = service.getTeam(341L, 409L, 742L);
        assertEquals("VALID", result.evidence().verificationStatus());
        assertEquals("MISMATCH", result.verification().verificationStatus());
    }

    @Test void sameNumericIdAndActionCannotLeakDifferentUnifiedActor() {
        when(signatureQueryService.verifyEvidence(anyLong())).thenAnswer(call -> {
            Long id = call.getArgument(0); return new ElectronicSignatureVerificationDTO(id,"VALID","content","content","evidence","evidence","SHA-256","v1",
                    "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT","CONSISTENT","CONSISTENT","SERVER_CLOCK","NOT_BOUND","CONSISTENT",List.of(),LocalDateTime.of(2026,10,7,12,0),"V1");
        });
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PRODUCTION_SUBMIT", null, null));
        when(eventMapper.selectById(240L)).thenReturn(new MesProProcessPoolEventDO()
                .setId(240L).setSignatureId(742L).setSignatureUserId(999L));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
        verify(signatureQueryService, times(1)).verifyEvidence(anyLong());
    }

    @Test void historicalBatchUsesArchivedDetailAuthorizationNotActiveTeamOwner() {
        when(batchDetailService.getSignatureDetail(800L, 742L)).thenReturn(production());
        stub(742L, "PRODUCTION_SUBMIT", null, null, "VALID");
        var result = service.getBatch(800L, null, 742L);
        assertEquals(409L, result.activeOrderId());
        assertEquals("冻结签名者B", result.signerName());
        verifyNoInteractions(teamDetailService, pqcDetailService);
    }

    @Test void historicalActiveOrderIdentityIsExactAndCannotBeAmbiguous() {
        when(batchDetailService.getSignatureDetailByActiveOrderId(409L, 742L)).thenReturn(production());
        stub(742L, "PRODUCTION_SUBMIT", null, null, "VALID");
        service.getBatch(null, 409L, 742L);
        assertThrows(RuntimeException.class, () -> service.getBatch(800L, 409L, 742L));
        assertThrows(RuntimeException.class, () -> service.getBatch(null, null, 742L));
        verify(batchDetailService, never()).getDetail(anyLong());
    }

    @Test void pqcApplicationMustBeAuthorizedBeforeEvidence() {
        when(pqcDetailService.get(344L, 900L)).thenThrow(new IllegalStateException("frozen assignee denied"));
        assertThrows(IllegalStateException.class, () -> service.getPqc(344L, 900L, 742L));
        verifyNoInteractions(signatureQueryService);
    }

    @Test void pqcHistoricalDetailReadsFormalForeignKey() {
        when(pqcDetailService.get(344L, 900L)).thenReturn(new MesPqcReleaseOrderDetailService.Result(production(), List.of()));
        stub(743L, "TEAM_LEADER_REVIEW", "PROCESS_POOL_EVENT", 240L, "VALID");
        assertEquals(743L, service.getPqc(344L, 900L, 743L).evidence().id());
        verifyNoInteractions(teamDetailService, batchDetailService);
    }

    @Test void pqcSubmitRequiresExactTaskMembership() {
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L).setPqcSubmissions(List.of(
                        new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail().setPqcTaskIds(List.of(140L, 141L))
                                .setSubmittedEventIds(List.of(241L))
                                .setSubmitterSignatures(List.of(signature(744L, "PQC人员")))));
        when(batchDetailService.getSignatureDetail(800L, 744L)).thenReturn(detail);
        stub(744L, "PQC_SUBMIT", "MES_PQC_INSPECTION_TASK", 141L, "VALID");
        assertEquals(744L, service.getBatch(800L, null, 744L).evidence().id());
    }

    @Test void marketReleaseHistoryUsesFormalTransactionSignatureAndActor() {
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L).setOperationFacts(List.of(
                new MesTeamLeaderActiveOrderDetail.OperationFact().setSignatureId(745L).setActorUserId(342L)
                        .setActorName("上市放行人员").setOperationType("BATCH_RECORD_RELEASE_APPROVED")));
        when(batchDetailService.getSignatureDetail(800L, 745L)).thenReturn(detail);
        stub(745L, "MARKET_RELEASE", "EDHR_MARKET_RELEASE", 901L, "VALID");
        assertEquals("MARKET_RELEASE", service.getBatch(800L, null, 745L).evidence().actionCode());
    }

    @Test void mismatchedVerificationResponseIsRejected() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PRODUCTION_SUBMIT", null, null));
        when(signatureQueryService.verifyEvidence(742L)).thenReturn(new ElectronicSignatureVerificationDTO(
                999L, "VALID", "content", "content", "evidence", "evidence", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "CONSISTENT", List.of(), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
    }

    @Test void claimedValidWithDifferentCalculatedHashesIsRejected() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PRODUCTION_SUBMIT", null, null));
        when(signatureQueryService.verifyEvidence(742L)).thenReturn(new ElectronicSignatureVerificationDTO(
                742L, "VALID", "content", "changed", "evidence", "evidence", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "MISMATCH", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND",
                "CONSISTENT", List.of("DIGEST_MISMATCH"), LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
    }

    private MesTeamLeaderActiveOrderDetail production() {
        return new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L).setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setSubmissions(List.of(
                        new MesTeamLeaderActiveOrderDetail.SubmissionDetail().setEventId(240L)
                                .setSubmitterSignature(signature(742L, "正式生产人员"))
                                .setReviewerSignature(signature(743L, "正式复核人员"))))));
    }

    private MesTeamLeaderActiveOrderDetail.SignatureDetail signature(Long id, String name) {
        return new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(id).setSignerName(name)
                .setSignedAt(LocalDateTime.of(2026, 10, 1, 10, 43, 47));
    }

    private void stub(Long id, String action, String sourceType, Long sourceId, String status) {
        when(signatureQueryService.getById(id)).thenReturn(evidence(id, action, sourceType, sourceId));
        when(signatureQueryService.verifyEvidence(id)).thenReturn(new ElectronicSignatureVerificationDTO(
                id, status, "content", status.equals("VALID") ? "content" : "changed", "evidence",
                status.equals("VALID") ? "evidence" : "changed", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", status.equals("VALID") ? "CONSISTENT" : "MISMATCH",
                "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND", "CONSISTENT",
                status.equals("VALID") ? List.of() : List.of("DIGEST_MISMATCH"),
                LocalDateTime.of(2026, 10, 7, 12, 0), "V1"));
    }

    private ElectronicSignatureEvidenceDTO evidence(Long id, String action, String sourceType, Long sourceId) {
        String type = sourceType == null ? "" : sourceType;
        String source = sourceId == null ? "" : sourceId.toString();
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action, null, null,
                null, null, null, null, null, sourceType, sourceId, null, null, null, null, null, null);
        String content = MesBatchRecordSignatureSubjectAdapter.canonicalBusinessContent(subject, action, "真实原因");
        if (java.util.Set.of("PRODUCTION_SUBMIT", "PQC_SUBMIT").contains(action)) {
            var json = com.alibaba.fastjson.JSON.parseObject(content);
            json.put("signatureIdentity", java.util.Map.of("domain", "SYSTEM_USER", "signerId",
                    id == 744L ? 344L : 342L, "operatorId", 341L, "tenantId", 1L, "displayName", "冻结签名者B"));
            content = json.toJSONString();
        }
        return new ElectronicSignatureEvidenceDTO(id, "MES", action, "MES_BATCH_RECORD", subject,
                MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), id == 743L ? 341L : id == 744L ? 344L : 342L, action, "业务签名", "真实原因", LocalDateTime.of(2026, 10, 1, 10, 43, 46),
                "time", java.util.Set.of("PRODUCTION_SUBMIT", "PQC_SUBMIT").contains(action) ? "SESSION_PLUS_SELECTED_USER_PASSWORD" : "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256", "v1", "p1", "VALID",
                null, null, null, null, content, null, null, null, "当前名称", "Asia/Shanghai");
    }
}
