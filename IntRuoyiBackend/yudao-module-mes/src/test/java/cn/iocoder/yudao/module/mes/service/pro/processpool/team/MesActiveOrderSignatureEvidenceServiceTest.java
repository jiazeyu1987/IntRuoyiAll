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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesActiveOrderSignatureEvidenceServiceTest {
    @Mock MesTeamLeaderActiveOrderDetailService teamDetailService;
    @Mock MesPqcReleaseOrderDetailService pqcDetailService;
    @Mock MesProEdhrBatchActiveOrderDetailService batchDetailService;
    @Mock ElectronicSignatureQueryService signatureQueryService;
    @Mock MesProProcessPoolEventMapper eventMapper;
    @Mock MesProcessPoolSubmissionReviewMapper reviewMapper;
    @InjectMocks MesActiveOrderSignatureEvidenceService service;

    @org.junit.jupiter.api.AfterEach void clearTenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }
    @BeforeEach void sourceForeignKeys() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        lenient().when(eventMapper.selectById(240L)).thenReturn(new MesProProcessPoolEventDO()
                .setId(240L).setSignatureId(742L).setSignatureUserId(342L).setDeviceAccountId(341L)
                .setRawPayload("{\"signatureIdentityDomain\":\"SYSTEM_USER\"}"));
        lenient().when(eventMapper.selectById(241L)).thenReturn(new MesProProcessPoolEventDO()
                .setId(241L).setSignatureId(744L).setSignatureUserId(344L).setDeviceAccountId(341L));
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
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(743L)).thenReturn(evidence(743L, "TEAM_LEADER_REVIEW", "PROCESS_POOL_EVENT", 888L));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 743L));
        verify(signatureQueryService, never()).verifyEvidence(anyLong());
    }

    @Test void newProductionIdentityMustMatchExactFormalSubmission() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(eventMapper.selectById(240L)).thenReturn(new MesProProcessPoolEventDO()
                .setId(240L).setSignatureId(742L).setSignatureUserId(342L).setDeviceAccountId(341L)
                .setRawPayload("{\"signatureIdentityDomain\":\"SYSTEM_USER\"}")
                .setRouteProcessId(520L).setProcessId(985L).setEventIdempotencyKey("submit-one"));
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
        verify(signatureQueryService, times(1)).verifyEvidence(742L);
    }

    private ElectronicSignatureEvidenceDTO productionEvidence(MesProductionSubmitSignatureContext context) {
        var original = evidence(742L, "PRODUCTION_SUBMIT", context.SOURCE_TYPE, context.activeOrderId());
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "PRODUCTION_SUBMIT",
                null, null, null, null, null, null, null, context.SOURCE_TYPE, context.activeOrderId(),
                context.sourceName(), null, null, null, null, null);
        var content = com.alibaba.fastjson.JSON.parseObject(original.canonicalContentJson());
        content.put("reviewSourceName", context.sourceName());
        return new ElectronicSignatureEvidenceDTO(original.id(), original.moduleCode(), original.actionCode(),
                original.subjectType(), subject, original.subjectVersion(), original.actorId(), original.meaningCode(),
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
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PQC_SUBMIT", null, null));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
        when(signatureQueryService.getById(742L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
        verify(signatureQueryService, never()).verifyEvidence(anyLong());
    }

    @Test void storedValidNeverMasksActualMismatch() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        stub(742L, "PRODUCTION_SUBMIT", null, null, "MISMATCH");
        var result = service.getTeam(341L, 409L, 742L);
        assertEquals("VALID", result.evidence().verificationStatus());
        assertEquals("MISMATCH", result.verification().verificationStatus());
    }

    @Test void sameNumericIdAndActionCannotLeakDifferentUnifiedActor() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PRODUCTION_SUBMIT", null, null));
        when(eventMapper.selectById(240L)).thenReturn(new MesProProcessPoolEventDO()
                .setId(240L).setSignatureId(742L).setSignatureUserId(999L));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
        verify(signatureQueryService, never()).verifyEvidence(anyLong());
    }

    @Test void historicalBatchUsesArchivedDetailAuthorizationNotActiveTeamOwner() {
        when(batchDetailService.getDetail(800L)).thenReturn(production());
        stub(742L, "PRODUCTION_SUBMIT", null, null, "VALID");
        var result = service.getBatch(800L, null, 742L);
        assertEquals(409L, result.activeOrderId());
        assertEquals("冻结签名者B", result.signerName());
        verifyNoInteractions(teamDetailService, pqcDetailService);
    }

    @Test void historicalActiveOrderIdentityIsExactAndCannotBeAmbiguous() {
        when(batchDetailService.getDetailByActiveOrderId(409L)).thenReturn(production());
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
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L).setProcesses(List.of(
                new MesTeamLeaderActiveOrderDetail.ProcessDetail().setPqcSubmissions(List.of(
                        new MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail().setPqcTaskIds(List.of(140L, 141L))
                                .setSubmittedEventIds(List.of(241L))
                                .setSubmitterSignatures(List.of(signature(744L, "PQC人员")))))));
        when(batchDetailService.getDetail(800L)).thenReturn(detail);
        stub(744L, "PQC_SUBMIT", "MES_PQC_INSPECTION_TASK", 141L, "VALID");
        assertEquals(744L, service.getBatch(800L, null, 744L).evidence().id());
    }

    @Test void marketReleaseHistoryUsesFormalTransactionSignatureAndActor() {
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(409L).setOperationFacts(List.of(
                new MesTeamLeaderActiveOrderDetail.OperationFact().setSignatureId(745L).setActorUserId(342L)
                        .setActorName("上市放行人员").setOperationType("BATCH_RECORD_RELEASE_APPROVED")));
        when(batchDetailService.getDetail(800L)).thenReturn(detail);
        stub(745L, "MARKET_RELEASE", "EDHR_MARKET_RELEASE", 901L, "VALID");
        assertEquals("MARKET_RELEASE", service.getBatch(800L, null, 745L).evidence().actionCode());
    }

    @Test void mismatchedVerificationResponseIsRejected() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PRODUCTION_SUBMIT", null, null));
        when(signatureQueryService.verifyEvidence(742L)).thenReturn(new ElectronicSignatureVerificationDTO(
                999L, "VALID", "content", "content", "evidence", "evidence", "SHA-256", "v1"));
        assertThrows(RuntimeException.class, () -> service.getTeam(341L, 409L, 742L));
    }

    @Test void claimedValidWithDifferentCalculatedHashesIsRejected() {
        when(teamDetailService.getDetail(341L, 409L)).thenReturn(production());
        when(signatureQueryService.getById(742L)).thenReturn(evidence(742L, "PRODUCTION_SUBMIT", null, null));
        when(signatureQueryService.verifyEvidence(742L)).thenReturn(new ElectronicSignatureVerificationDTO(
                742L, "VALID", "content", "changed", "evidence", "evidence", "SHA-256", "v1"));
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
                status.equals("VALID") ? "evidence" : "changed", "SHA-256", "v1"));
    }

    private ElectronicSignatureEvidenceDTO evidence(Long id, String action, String sourceType, Long sourceId) {
        String type = sourceType == null ? "" : sourceType;
        String source = sourceId == null ? "" : sourceId.toString();
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action, null, null,
                null, null, null, null, null, sourceType, sourceId, null, null, null, null, null, null);
        String content = "{\"executionId\":\"0\",\"actionType\":\"" + action
                + "\",\"reviewSourceType\":\"" + type + "\",\"reviewSourceId\":\"" + source + "\"}";
        if (java.util.Set.of("PRODUCTION_SUBMIT", "PQC_SUBMIT").contains(action)) {
            var json = com.alibaba.fastjson.JSON.parseObject(content);
            json.put("signatureIdentity", java.util.Map.of("domain", "SYSTEM_USER", "signerId",
                    id == 744L ? 344L : 342L, "operatorId", 341L, "tenantId", 1L, "displayName", "冻结签名者B"));
            content = json.toJSONString();
        }
        return new ElectronicSignatureEvidenceDTO(id, "MES", action, "MES_BATCH_RECORD", subject,
                "v1", id == 743L ? 341L : id == 744L ? 344L : 342L, action, "业务签名", "真实原因", LocalDateTime.of(2026, 10, 1, 10, 43, 46),
                "time", "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256", "v1", "p1", "VALID",
                null, null, null, null, content, null, null, null, "当前名称", "Asia/Shanghai");
    }
}
