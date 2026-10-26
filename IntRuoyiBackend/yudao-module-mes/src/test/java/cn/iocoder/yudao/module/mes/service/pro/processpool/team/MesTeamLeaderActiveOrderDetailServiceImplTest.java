package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderDetailReadMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderEventPartyReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesTeamLeaderActiveOrderDetailReadDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verification DTOs are consumer-port fixtures, not evidence of the creation-audit algorithm.
class MesTeamLeaderActiveOrderDetailServiceImplTest {
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @org.junit.jupiter.api.AfterEach void clearPqcTenant() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();
    }

    @ParameterizedTest @ValueSource(strings = {"VALID", "MISMATCH", "INCOMPLETE"})
    void v2PqcSummaryKeepsVerifiedProtocolAndReadDiagnosticStatus(String status) throws Exception {
        var evidence = MesActiveOrderSignatureEvidenceServiceTest.v2PqcEvidence(pqcEvidence("正式放行意见", false));
        stubPqc(evidence);
        when(signatureQueryService.verifyEvidence(7701L)).thenReturn(MesActiveOrderSignatureEvidenceServiceTest.v2PqcVerification(evidence, status));
        var purposeType = Class.forName(MesTeamLeaderActiveOrderDetailServiceImpl.class.getName()+"$PqcSignaturePurpose");
        Object purpose = java.util.Arrays.stream(purposeType.getEnumConstants()).filter(v->v.toString().equals("READ_PROJECTION")).findFirst().orElseThrow();
        var detail = pqcDetail();
        if ("INCOMPLETE".equals(status)) {
            // ElectronicSignatureVerifiedFactService.evaluateV2 emits only VALID/MISMATCH;
            // MesActiveOrderSignatureEvidenceService.requireVerification enforces that rule.
            // V2 collapses every non-consistent dimension to MISMATCH. An INCOMPLETE
            // overall status is an inconsistent consumer-port DTO, not a readable diagnostic.
            assertThrows(ServiceException.class, () -> org.springframework.test.util.ReflectionTestUtils
                    .invokeMethod(service, "attachPqcProductionRelease", detail, pqcApplication(), purpose));
            return;
        }
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "attachPqcProductionRelease", detail, pqcApplication(), purpose);
        var projection = detail.getPqcProductionRelease().getSignature();
        assertEquals(status, projection.getVerification().verificationStatus());
        assertEquals("VALID".equals(status) ? "签署时正式姓名" : "签时姓名未核验", projection.getSignerName());
    }

    @ParameterizedTest @NullSource
    @ValueSource(strings = {"PQC_RELEASE", "APPROVE", "首行\n次行\r\n制表\t引号\"路径\\"})
    void pqcSummaryAcceptsExplicitSourceAndKeepsExactSignedContent(String opinion) {
        var application = pqcApplication();
        var evidence = pqcEvidence(opinion, false);
        var detail = pqcDetail();
        stubPqc(evidence);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "attachPqcProductionRelease", detail, application, formalPqcPurpose());
        assertEquals(evidence.signedAt(), detail.getPqcProductionRelease().getSignature().getSignedAt());
        assertEquals(evidence.id(), detail.getPqcProductionRelease().getSignature().getSignatureId());
        assertEquals("未记录", detail.getPqcProductionRelease().getSignature().getSignerName());
        org.mockito.Mockito.verifyNoInteractions(adminUserService);
        assertEquals(opinion, MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(evidence.subjectId(), "PQC_RELEASE", evidence.reason()));
        assertEquals(opinion == null ? "SYSTEM" : "USER", JSON.parseObject(evidence.canonicalContentJson()).getString("opinionSource"));
    }

    @Test void pqcSummaryRejectsForeignActorReceiptTaskTenantAndHashes() {
        var evidence = pqcEvidence("PQC_RELEASE", false);
        var application = pqcApplication();
        stubPqc(evidence);
        application.setPqcDecidedBy(399L);
        assertThrows(RuntimeException.class, () -> attachPqc(application));
        application.setPqcDecidedBy(3001L).setDossierSummaryJson(application.getDossierSummaryJson().replace("7701", "7702"));
        assertThrows(RuntimeException.class, () -> attachPqc(application));
        var finalApplication = pqcApplication();
        finalApplication.setTenantId(2L);
        assertThrows(RuntimeException.class, () -> attachPqc(finalApplication));
        finalApplication.setTenantId(1L);
        when(workTaskMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(pqcTask().setBusinessScopeId(8802L));
        assertThrows(RuntimeException.class, () -> attachPqc(finalApplication));
        when(workTaskMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(pqcTask());
        when(signatureQueryService.verifyEvidence(7701L)).thenReturn(pqcVerification(evidence, "VALID", "MISMATCH", "CONSISTENT", "CONSISTENT"));
        assertThrows(RuntimeException.class, () -> attachPqc(finalApplication));
    }

    @Test void pqcSummaryDoesNotAcceptDigestOnlyValidWithMissingAuditOrTime() {
        var evidence = pqcEvidence(null, false);
        stubPqc(evidence);
        for (var dimensions : List.of(new String[]{"MISSING", "CONSISTENT"}, new String[]{"CONSISTENT", "MISSING"})) {
            when(signatureQueryService.verifyEvidence(7701L)).thenReturn(pqcVerification(evidence, "INCOMPLETE", "CONSISTENT", dimensions[0], dimensions[1]));
            assertThrows(RuntimeException.class, () -> attachPqc(pqcApplication()));
            when(signatureQueryService.verifyEvidence(7701L)).thenReturn(pqcVerification(evidence, "VALID", "CONSISTENT", dimensions[0], dimensions[1]));
            assertThrows(RuntimeException.class, () -> attachPqc(pqcApplication()));
        }
    }

    @Test void pqcSummaryRejectsUnknownSourceAndCanonicalOrStoredHashMismatch() {
        var evidence = pqcEvidence("PQC_RELEASE", false);
        stubPqc(evidence);
        for (String field : List.of("subjectVersion", "canonicalContentJson", "contentHash", "evidenceHash")) {
            var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(evidence));
            json.put(field, field.equals("canonicalContentJson") ? "{}" : "foreign");
            when(signatureQueryService.getById(7701L)).thenReturn(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class));
            assertThrows(RuntimeException.class, () -> attachPqc(pqcApplication()), field);
        }
        String raw = new String(java.util.Base64.getUrlDecoder().decode(evidence.subjectId()), java.nio.charset.StandardCharsets.UTF_8);
        String subject = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                raw.replace("MES_OPINION_SOURCE_V1:USER", "MES_OPINION_SOURCE_V2:USER").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var json = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(evidence));
        json.put("subjectId", subject); json.put("subjectVersion", MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject));
        when(signatureQueryService.getById(7701L)).thenReturn(cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(json.toString(), ElectronicSignatureEvidenceDTO.class));
        assertThrows(RuntimeException.class, () -> attachPqc(pqcApplication()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RENAMED", "DELETED", "DISABLED", "UNAVAILABLE"})
    void ordinaryV1MissingNameRemainsUnrecordedRegardlessOfCurrentDirectory(String directoryState) {
        var evidence = pqcEvidence("历史意见", true);
        stubPqc(evidence);
        if (directoryState.equals("DELETED")) {
            lenient().doReturn(null).when(adminUserService).getUser(3001L);
        } else if (!directoryState.equals("UNAVAILABLE")) {
            lenient().doReturn(new AdminUserDO().setId(3001L)
                    .setNickname("当前新名").setStatus(directoryState.equals("DISABLED") ? 1 : 0)).when(adminUserService).getUser(3001L);
        }
        var detail = pqcDetail();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "attachPqcProductionRelease", detail, pqcApplication(), formalPqcPurpose());
        assertEquals("未记录", detail.getPqcProductionRelease().getSignature().getSignerName());
        org.mockito.Mockito.verifyNoInteractions(adminUserService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RENAMED", "DELETED", "DISABLED", "UNAVAILABLE"})
    void systemUserV1SnapshotKeepsSignedNameRegardlessOfCurrentDirectory(String directoryState) {
        var evidence = withSystemUserSnapshot(pqcEvidence("历史意见", true));
        stubPqc(evidence);
        if (directoryState.equals("DELETED")) lenient().doReturn(null).when(adminUserService).getUser(3001L);
        else if (!directoryState.equals("UNAVAILABLE")) lenient().doReturn(new AdminUserDO().setId(3001L)
                .setNickname("当前新名").setStatus(directoryState.equals("DISABLED") ? 1 : 0)).when(adminUserService).getUser(3001L);
        var detail = pqcDetail();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "attachPqcProductionRelease", detail, pqcApplication(), formalPqcPurpose());
        assertEquals("签署时正式姓名", detail.getPqcProductionRelease().getSignature().getSignerName());
        org.mockito.Mockito.verifyNoInteractions(adminUserService);
    }

    static ElectronicSignatureEvidenceDTO withSystemUserSnapshot(ElectronicSignatureEvidenceDTO original) {
        var content = (com.fasterxml.jackson.databind.node.ObjectNode) cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(original.canonicalContentJson());
        content.set("signatureIdentity", cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Map.of(
                        "domain", "SYSTEM_USER", "signerId", original.actorId(), "operatorId", 3001L,
                        "tenantId", 1L, "displayName", "签署时正式姓名"))));
        return new ElectronicSignatureEvidenceDTO(original.id(), original.moduleCode(), original.actionCode(), original.subjectType(),
                original.subjectId(), original.subjectVersion(), original.actorId(), original.meaningCode(), original.meaningLabel(),
                original.reason(), original.signedAt(), original.timeEvidenceId(), "SESSION_PLUS_SELECTED_USER_PASSWORD",
                original.contentHash(), original.evidenceHash(), original.algorithm(), original.keyVersion(), original.policyVersion(),
                original.verificationStatus(), original.processInstanceId(), original.taskId(), original.nodeCode(), original.nodeOrder(),
                content.toString(), original.beforeContentJson(), original.afterContentJson(), original.fieldDiffJson(),
                "签署时正式姓名", original.timeZone());
    }

    private static Object formalPqcPurpose() {
        try {
            Class<?> type = Class.forName(MesTeamLeaderActiveOrderDetailServiceImpl.class.getName() + "$PqcSignaturePurpose");
            return java.util.Arrays.stream(type.getEnumConstants()).filter(value -> value.toString().equals("FORMAL_CONSUMPTION"))
                    .findFirst().orElseThrow();
        } catch (ClassNotFoundException ex) { throw new AssertionError(ex); }
    }
    private void attachPqc(MesProcessPoolActiveOrderReleaseApplicationDO application) {
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "attachPqcProductionRelease", pqcDetail(), application, formalPqcPurpose());
    }
    private void stubPqc(ElectronicSignatureEvidenceDTO evidence) {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "workTaskMapper", workTaskMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchExecutionMapper", batchExecutionMapper);
        when(signatureQueryService.getById(7701L)).thenReturn(evidence);
        lenient().when(signatureQueryService.verifyEvidence(7701L)).thenReturn(pqcVerification(evidence, "VALID", "CONSISTENT", "CONSISTENT", "CONSISTENT"));
        lenient().when(workTaskMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(pqcTask());
        lenient().when(batchExecutionMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(pqcBatch());
        lenient().when(adminUserService.getUser(3001L)).thenThrow(new IllegalStateException("当前目录不可用"));
    }
    static MesTeamLeaderActiveOrderDetail pqcDetail() {
        return new MesTeamLeaderActiveOrderDetail().setActiveOrderId(8101L).setWorkOrderId(9001L)
                .setPqcProductionRelease(new MesTeamLeaderActiveOrderDetail.PqcProductionReleaseSummary()
                        .setSignature(new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(7701L).setSignerName("PQC组长")));
    }
    static MesProcessPoolActiveOrderReleaseApplicationDO pqcApplication() {
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(8801L).setActiveOrderId(8101L)
                .setWorkOrderId(9001L).setBatchExecutionId(9901L).setPqcReleaseWorkTaskId(8803L)
                .setPqcDecision("APPROVE").setPqcDecidedBy(3001L).setPqcDecidedAt(LocalDateTime.of(2026, 9, 18, 21, 23))
                .setApplicationStatus("REPORT_UPLOAD_PENDING").setDossierSummaryJson(JSON.toJSONString(java.util.Map.of(
                        "applicationId", 8801L, "pqcReleaseWorkTaskId", 8803L, "batchExecutionId", 9901L,
                        "decidedBy", 3001L, "decision", "APPROVE", "signatureId", 7701L)));
        application.setTenantId(1L);
        return application;
    }
    static cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO pqcTask() {
        return new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO().setId(8803L)
                .setTaskType("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(8801L)
                .setBatchExecutionId(9901L).setWorkOrderId(9001L).setStatus("DONE").setReason("APPROVE")
                .setCompletedAt(LocalDateTime.of(2026, 9, 18, 21, 23));
    }
    static cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO pqcBatch() {
        return new cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO()
                .setId(9901L).setWorkOrderId(9001L).setTenantId(1L);
    }
    static ElectronicSignatureEvidenceDTO pqcEvidence(String opinion, boolean legacy) {
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(9901L, "PQC_RELEASE", null, null,
                null, null, null, null, null, "PQC_RELEASE_APPLICATION", 8801L, "PQC生产放行", "PQC_RELEASE", null, null, null, null);
        if (!legacy) subject = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(subject, opinion);
        String reason = opinion == null ? "PQC_RELEASE" : opinion;
        String version = MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject);
        String content = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(3001L, "MES", "PQC_RELEASE", "MES_BATCH_RECORD", subject, version, reason)).canonicalContentJson();
        return new ElectronicSignatureEvidenceDTO(7701L, "MES", "PQC_RELEASE", "MES_BATCH_RECORD", subject, version, 3001L,
                "PQC_RELEASE", "PQC生产放行", reason, LocalDateTime.of(2026, 9, 18, 21, 22, 52),
                "SERVER_CLOCK:2026-09-18T21:22:52", "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256", "v1", "p1", "VALID",
                null, null, null, null, content, null, null, null, "PQC组长", "Asia/Shanghai");
    }
    static ElectronicSignatureVerificationDTO pqcVerification(ElectronicSignatureEvidenceDTO evidence, String overall, String digest, String time, String audit) {
        return new ElectronicSignatureVerificationDTO(evidence.id(), overall, evidence.contentHash(), evidence.contentHash(),
                evidence.evidenceHash(), evidence.evidenceHash(), evidence.algorithm(), evidence.keyVersion(),
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", digest, time, "SERVER_CLOCK", "NOT_BOUND", audit,
                overall.equals("VALID") ? List.of() : List.of("CONSUMER_PORT_FAILURE_FIXTURE"), LocalDateTime.now(), "V1");
    }

    @Test
    void deviationSourcesDoNotMasqueradeAsReviewMaterialFiles() {
        var metadata = JSON.parseObject("{\"sourceDeviationIds\":[3001,3002]}");
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "reviewMaterialFileIdOf", metadata));
        metadata.put("reviewMaterialsJson", "{\"activeMaterials\":[{\"fileId\":8001}]}");
        Long materialId = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service, "reviewMaterialFileIdOf", metadata);
        assertEquals(8001L, materialId);
    }

    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper backfillMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingMapper bindingMapper;
    @Mock
    private MesProcessPoolActiveOrderPickListBindingItemMapper bindingItemMapper;

    @Mock
    private ErpKingdeeProductionPickListMapper pickListMapper;
    @Mock
    private ErpKingdeeProductionPickListItemMapper pickListItemMapper;

    @Test
    void completedDetailReadsBatchesFromCompletionSnapshot() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of(
                new MesFrontlineProcessMaterial(3101L, "MAT-A", "物料A", "S1", "INPUT", null,
                        List.of(), null, null, null, List.of(), List.of(), List.of(), null)));
        String seed = """
                {"activeOrderBinding":{"id":8101,"workOrderId":9001},
                 "pickListBindings":[{"id":11,"pickListId":21,"sourceBillNo":"LL-21"},
                                     {"id":12,"pickListId":22,"sourceBillNo":"LL-22"}],
                 "pickListBindingItems":{"11":[{"pickListItemId":31,"materialNumber":"MAT-A","lotNumber":"LOT-A","requestedQuantity":6,"actualQuantity":5,"baseActualQuantity":5}],
                                         "12":[{"pickListItemId":32,"materialNumber":"MAT-A","lotNumber":"LOT-B","requestedQuantity":4,"actualQuantity":4,"baseActualQuantity":4}]}}
                """;
        var backfill = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO.builder()
                .id(71L).activeOrderId(8101L).workOrderId(9001L).backfillType("BATCH_RECORD").status("SUCCESS")
                .sourceSnapshotHash("completion-hash")
                .payloadJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Map.of(
                        "type", "BATCH_RECORD", "status", "SUCCESS", "sourceSnapshotHash", "completion-hash",
                        "formalSourceSnapshot", seed))).build();
        lenient().when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(backfill);

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);
        var material = detail.getProcesses().get(0).getInputMaterials().get(0);
        assertEquals(List.of("LOT-A", "LOT-B"), material.getBatchCodes());
        assertEquals(List.of("LL-21", "LL-22"), material.getSourcePickListNos());
        assertEquals(List.of(21L, 22L), material.getSourcePickListIds());
        assertEquals(List.of(31L, 32L), material.getSourcePickListItemIds());
        assertEquals(new BigDecimal("9"), material.getActualQuantity());
        assertEquals("completion-hash", material.getSourceSnapshotHash());
        assertEquals(1, detail.getInputMaterialUsages().size());
        assertEquals(List.of("LOT-A", "LOT-B"),
                detail.getInputMaterialUsages().get(0).getBatchCodes());
    }

    @Test
    void activeDetailDoesNotReadPickListBatchesBeforeCompletionBackfill() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of(
                new MesFrontlineProcessMaterial(3101L, "MAT-A", "物料A", "S1", "INPUT", null,
                        List.of(), null, null, null, List.of(), List.of(), List.of(), null)));
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        lenient().when(bindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingDO.builder()
                        .id(7001L).activeOrderId(8101L).workOrderId(9001L).pickListId(8001L)
                        .sourceBillNo("REAL-PICK-001").sourceSnapshotHash("binding-hash").build()));
        lenient().when(bindingItemMapper.selectListByBindingId(7001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .id(9101L).bindingId(7001L).pickListItemId(9201L).materialNumber("MAT-A")
                        .lotNumber("LOT-REAL-001").requestedQuantity(new BigDecimal("6"))
                        .actualQuantity(new BigDecimal("5")).baseActualQuantity(new BigDecimal("5")).build()));

        var material = service.getDetail(3001L, 8101L).getProcesses().get(0).getInputMaterials().get(0);

        assertEquals(List.of(), material.getBatchCodes());
        assertEquals(List.of(), material.getSourcePickListNos());
        assertEquals(List.of(), material.getSourcePickListIds());
        assertEquals(List.of(), material.getSourcePickListItemIds());
        assertNull(material.getActualQuantity());
        assertNull(material.getRequestedQuantity());
        assertNull(material.getSourceSnapshotHash());
    }

    @Test
    void activeDetailDoesNotExposeOrderInputMaterialUsagesBeforeCompletionBackfill() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        lenient().when(bindingMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingDO.builder()
                        .id(7001L).activeOrderId(8101L).workOrderId(9001L).pickListId(8001L)
                        .sourceBillNo("REAL-PICK-001").sourceSnapshotHash("binding-hash").build()));
        lenient().when(bindingItemMapper.selectListByBindingId(7001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .id(9101L).bindingId(7001L).pickListItemId(9201L).materialNumber("MAT-PML-001")
                        .materialName("用料清单物料").materialSpecification("S1").lotNumber("LOT-LOW")
                        .requestedQuantity(new BigDecimal("6")).actualQuantity(new BigDecimal("5"))
                        .baseActualQuantity(new BigDecimal("5")).build(),
                MesProcessPoolActiveOrderPickListBindingItemDO.builder()
                        .id(9102L).bindingId(7001L).pickListItemId(9202L).materialNumber("MAT-PML-001")
                        .materialName("用料清单物料").materialSpecification("S1").lotNumber("LOT-HIGH")
                        .requestedQuantity(new BigDecimal("8")).actualQuantity(new BigDecimal("7"))
                        .baseActualQuantity(new BigDecimal("7")).build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(0, detail.getProcesses().get(0).getInputMaterials().size());
        assertEquals(0, detail.getInputMaterialUsages().size());
    }

    @Test
    void formalDetailIncludesPqcReleaseSignatureFromTheSameActiveOrderFact() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(releaseApplicationMapper.selectLatestByActiveOrderId(8101L)).thenReturn(pqcApplication());
        stubPqc(pqcEvidence("历史意见", true));

        MesTeamLeaderActiveOrderDetail detail = service.getFormalDetail(8101L);

        assertEquals("已生产放行", detail.getPqcProductionRelease().getStatusLabel());
        assertEquals(7701L, detail.getPqcProductionRelease().getSignature().getSignatureId());
        assertEquals("未记录", detail.getPqcProductionRelease().getSignature().getSignerName());
        org.mockito.Mockito.verifyNoInteractions(adminUserService);
    }

    @Test void readDetailKeepsIncompleteSignatureWhileFormalConsumptionRejectsIt() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(releaseApplicationMapper.selectLatestByActiveOrderId(8101L)).thenReturn(pqcApplication());
        var evidence = withSystemUserSnapshot(pqcEvidence("历史意见", true));
        stubPqc(evidence);
        when(signatureQueryService.verifyEvidence(7701L)).thenReturn(pqcVerification(evidence, "INCOMPLETE", "CONSISTENT", "CONSISTENT", "MISSING"));
        var signature = service.getDetail(3001L, 8101L).getPqcProductionRelease().getSignature();
        assertEquals(7701L, signature.getSignatureId());
        assertEquals("INCOMPLETE", signature.getVerification().verificationStatus());
        assertEquals("UNVERIFIED_SNAPSHOT", signature.getSignerNameStatus());
        assertEquals("签时姓名未核验", signature.getSignerName());
        assertThrows(RuntimeException.class, () -> service.getFormalDetail(8101L));
        org.mockito.Mockito.verifyNoInteractions(adminUserService);
    }

    @Test
    void archivedFormalDetailReadsClosedOrArchivedSourceOrder() {
        when(activeOrderMapper.selectByIdIgnoreDeleted(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listArchivedFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);

        MesTeamLeaderActiveOrderDetail detail = service.getArchivedFormalDetail(8101L);

        assertEquals(8101L, detail.getActiveOrderId());
        assertEquals("881MO090889", detail.getWorkOrderCode());
    }

    @ParameterizedTest
    @ValueSource(longs = {17001L, 27001L, 37001L})
    void signatureProjectionVerifiesOnlyItsFormalProductionSource(Long signatureId) {
        stubSignatureProjection();
        var first = row(9101L, 5001L, 6001L, "粗洗", "100", 7001L, "10", "员工甲", "组长甲", "2026-08-13T08:10:00")
                .setReviewerSignatureId(27001L);
        var unrelated = row(9101L, 5001L, 6001L, "粗洗", "100", 7002L, "10", "员工乙", "组长甲", "2026-08-13T09:10:00");
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(first, unrelated));
        if (signatureId == 37001L) when(eventRevisionMapper.selectBySignatureId(signatureId)).thenReturn(
                new MesProProcessPoolEventRevisionDO().setEventId(7001L).setRevisionStatus(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE));
        var detail = service.getArchivedSignatureDetail(8101L, signatureId);
        assertEquals(List.of(7001L), detail.getProcesses().get(0).getSubmissions().stream()
                .map(MesTeamLeaderActiveOrderDetail.SubmissionDetail::getEventId).toList());
        org.mockito.Mockito.verify(submissionSignatureIdentityReader).read(17001L, 7001L, 8101L, "PRODUCTION_SUBMIT");
        org.mockito.Mockito.verify(submissionSignatureIdentityReader, org.mockito.Mockito.never())
                .read(17002L, 7002L, 8101L, "PRODUCTION_SUBMIT");
    }

    @ParameterizedTest
    @ValueSource(longs = {18869L, 28869L, 38869L})
    void signatureProjectionKeepsExactPqcTaskEventAndCorrectionMembership(Long signatureId) {
        stubSignatureProjection();
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(signaturePqcTask(4101L, 8869L), signaturePqcTask(4102L, 8872L)));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(8869L, 8872L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO().setEventId(8869L).setSubmitterName("PQC甲")
                        .setSubmitterSignatureId(18869L).setReviewerSignatureId(28869L).setReviewerName("PQC组长"),
                new MesTeamLeaderActiveOrderEventPartyReadDO().setEventId(8872L).setSubmitterName("PQC乙")
                        .setSubmitterSignatureId(18872L).setReviewerSignatureId(28872L).setReviewerName("PQC组长")));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(MesQaInspectionRegulationProcessDO.builder()
                .id(5101L).regulationVersionId(5201L).processName("粗洗检验").processCode("QA1").sort(1).build()));
        if (signatureId == 38869L) when(eventRevisionMapper.selectBySignatureId(signatureId)).thenReturn(
                new MesProProcessPoolEventRevisionDO().setEventId(8869L).setRevisionStatus(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE));
        var detail = service.getArchivedSignatureDetail(8101L, signatureId);
        var submissions = detail.getPqcSubmissions();
        assertEquals(1, submissions.size());
        assertEquals(List.of(4101L), submissions.get(0).getPqcTaskIds());
        assertEquals(List.of(8869L), submissions.get(0).getSubmittedEventIds());
        org.mockito.Mockito.verify(eventMapper).selectBatchIds(List.of(8869L));
        org.mockito.Mockito.verify(eventRevisionMapper).selectListByEventId(8869L);
        org.mockito.Mockito.verify(eventRevisionMapper, org.mockito.Mockito.never()).selectListByEventId(8872L);
        org.mockito.Mockito.verify(submissionSignatureIdentityReader).read(18869L, 8869L, 8101L, "PQC_SUBMIT");
        org.mockito.Mockito.verify(submissionSignatureIdentityReader, org.mockito.Mockito.never())
                .read(18872L, 8872L, 8101L, "PQC_SUBMIT");
    }

    @Test
    void foreignCorrectionCannotCreateOrderMembershipAndTargetValidationIsNotSkipped() {
        stubSignatureProjection();
        var row = row(9101L, 5001L, 6001L, "粗洗", "100", 7001L, "10", "员工甲", null, "2026-08-13T08:10:00");
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(row));
        when(eventRevisionMapper.selectBySignatureId(37001L)).thenReturn(new MesProProcessPoolEventRevisionDO()
                .setEventId(99999L).setRevisionStatus(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE));
        assertEquals(0, service.getArchivedSignatureDetail(8101L, 37001L).getProcesses().get(0).getSubmissions().size());
        org.mockito.Mockito.verifyNoInteractions(submissionSignatureIdentityReader);
        when(submissionSignatureIdentityReader.read(17001L, 7001L, 8101L, "PRODUCTION_SUBMIT"))
                .thenThrow(new IllegalStateException("target signature invalid"));
        assertThrows(IllegalStateException.class, () -> service.getArchivedSignatureDetail(8101L, 17001L));
    }

    private void stubSignatureProjection() {
        when(activeOrderMapper.selectByIdIgnoreDeleted(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).qaRegulationVersionId(5201L).activeStatus("CLOSED").build());
    }

    private static MesPqcInspectionTaskDO signaturePqcTask(Long id, Long eventId) {
        return MesPqcInspectionTaskDO.builder().id(id).activeOrderId(8101L)
                .inspectionType("FINAL").qaItemCode("").inspectionItemPlanJson("{\"APPEARANCE\":1}")
                .plannedInspectionQuantity(1).inspectionRuleKey("FINAL").roundNo(1)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED).actualInspectionQuantity(1)
                .qaProcessId(5101L).regulationVersionId(5201L).submittedEventId(eventId).build();
    }

    @Test
    void activeOrderOperationsShouldAppearInFormalFactChain() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(operationAuditEventMapper.selectSuccessfulListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProEdhrOperationAuditEventDO.builder()
                        .id(8801L)
                        .objectType("ACTIVE_ORDER")
                        .objectId("8101")
                        .operationType("BATCH_REWORK")
                        .actionName("批次返工")
                        .actorUserId(3001L)
                        .actorUsername("生产组长甲")
                        .resultStatus("SUCCESS")
                        .afterSummaryHash("snapshot-1")
                        .metadataJson("{\"activeOrderId\":8101,\"signatureId\":7701}")
                        .occurredAt(LocalDateTime.of(2026, 9, 18, 22, 10))
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getFormalDetail(8101L);
        var getter = MesTeamLeaderActiveOrderDetail.class.getMethod("getOperationFacts");
        List<?> facts = (List<?>) getter.invoke(detail);

        assertEquals(1, facts.size());
        Object fact = facts.get(0);
        assertEquals("BATCH_REWORK", fact.getClass().getMethod("getOperationType").invoke(fact));
        assertEquals("ACTIVE_ORDER", fact.getClass().getMethod("getSourceType").invoke(fact));
        assertEquals("8101", fact.getClass().getMethod("getSourceId").invoke(fact));
        assertEquals(7701L, fact.getClass().getMethod("getSignatureId").invoke(fact));
    }

    @Test
    void marketReleaseOperationFactReadsSignatureIdFromReleaseTransaction() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(operationAuditEventMapper.selectSuccessfulListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProEdhrOperationAuditEventDO.builder()
                        .id(8802L).batchExecutionId(9001L)
                        .objectType("PRODUCTION_RELEASE_APPLICATION").objectId("7501")
                        .operationType("BATCH_RECORD_RELEASE_APPROVED").actionName("批记录上市放行")
                        .actorUserId(3001L).actorUsername("生产组长甲").resultStatus("SUCCESS")
                        .afterSummaryHash("snapshot-release")
                        .metadataJson("{\"activeOrderId\":8101,\"releaseTransactionId\":7601,\"signatureId\":1}")
                        .occurredAt(LocalDateTime.of(2026, 9, 18, 22, 30)).build()));
        when(releaseTransactionMapper.selectByBatchExecutionId(9001L)).thenReturn(
                MesProEdhrReleaseTransactionDO.builder().id(7601L)
                        .batchExecutionId(9001L).approvalSignatureId(7702L).build());

        var facts = service.getFormalDetail(8101L).getOperationFacts();

        assertEquals(1, facts.size());
        assertEquals(7702L, facts.get(0).getSignatureId());
    }

    @Test
    void marketReleaseOperationFactResolvesFormalTransactionForLegacyAuditMetadata() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(operationAuditEventMapper.selectSuccessfulListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProEdhrOperationAuditEventDO.builder()
                        .id(8803L)
                        .batchExecutionId(9001L)
                        .objectType("PRODUCTION_RELEASE_APPLICATION")
                        .objectId("7501")
                        .operationType("BATCH_RECORD_RELEASE_APPROVED")
                        .actionName("批记录上市放行")
                        .actorUserId(3001L)
                        .actorUsername("生产组长甲")
                        .resultStatus("SUCCESS")
                        .afterSummaryHash("snapshot-release")
                        .metadataJson("{\"activeOrderId\":8101,\"signatureId\":1}")
                        .occurredAt(LocalDateTime.of(2026, 9, 18, 22, 30))
                        .build()));
        when(releaseTransactionMapper.selectByBatchExecutionId(9001L)).thenReturn(
                MesProEdhrReleaseTransactionDO.builder().id(7601L)
                        .batchExecutionId(9001L).approvalSignatureId(7702L).build());

        var facts = service.getFormalDetail(8101L).getOperationFacts();

        assertEquals(1, facts.size());
        assertEquals(7702L, facts.get(0).getSignatureId());
    }

    @Test
    void marketReleaseOperationFactRejectsConflictingMetadataTransactionId() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(operationAuditEventMapper.selectSuccessfulListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProEdhrOperationAuditEventDO.builder()
                        .id(8804L)
                        .batchExecutionId(9001L)
                        .objectType("PRODUCTION_RELEASE_APPLICATION")
                        .objectId("7501")
                        .operationType("BATCH_RECORD_RELEASE_APPROVED")
                        .actionName("批记录上市放行")
                        .actorUserId(3001L)
                        .actorUsername("生产组长甲")
                        .resultStatus("SUCCESS")
                        .afterSummaryHash("snapshot-release")
                        .metadataJson("{\"activeOrderId\":8101,\"releaseTransactionId\":7602}")
                        .occurredAt(LocalDateTime.of(2026, 9, 18, 22, 30))
                        .build()));
        when(releaseTransactionMapper.selectByBatchExecutionId(9001L)).thenReturn(
                MesProEdhrReleaseTransactionDO.builder().id(7601L)
                        .batchExecutionId(9001L).approvalSignatureId(7702L).build());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getFormalDetail(8101L));

        assertEquals("ACTIVE_ORDER_MARKET_RELEASE_TRANSACTION_ID_MISMATCH", error.getMessage());
    }

    @Test
    void activeOrderMaintenanceOperationsShouldAppearInFormalFactChain() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("CLOSED").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(adminUserService.getUser(3001L)).thenReturn(new AdminUserDO().setId(3001L).setNickname("生产组长甲"));
        when(maintenanceAuditMapper.selectSuccessfulListByActiveOrderId(8101L)).thenReturn(List.of(
                MesProcessPoolTeamMaintenanceAuditDO.builder()
                        .id(9901L)
                        .operatorUserId(3001L)
                        .actionType("APPLY_ACTIVE_ORDER_VERSION_UPGRADE")
                        .targetType("ACTIVE_ORDER")
                        .targetId(8101L)
                        .resultStatus("SUCCESS")
                        .changeSummary("活跃订单版本升级审批通过后重启")
                        .auditTime(LocalDateTime.of(2026, 9, 18, 22, 20))
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getFormalDetail(8101L);
        var getter = MesTeamLeaderActiveOrderDetail.class.getMethod("getOperationFacts");
        List<?> facts = (List<?>) getter.invoke(detail);

        assertEquals(1, facts.size());
        Object fact = facts.get(0);
        assertEquals("APPLY_ACTIVE_ORDER_VERSION_UPGRADE",
                fact.getClass().getMethod("getOperationType").invoke(fact));
        assertEquals("ACTIVE_ORDER", fact.getClass().getMethod("getSourceType").invoke(fact));
        assertEquals("8101", fact.getClass().getMethod("getSourceId").invoke(fact));
        assertEquals(3001L, fact.getClass().getMethod("getActorUserId").invoke(fact));
    }

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock
    private MesProEdhrNonconformanceReviewMapper nonconformanceReviewMapper;
    @Mock
    private MesProEdhrOperationAuditEventMapper operationAuditEventMapper;
    @Mock
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper maintenanceAuditMapper;
    @Mock
    private MesProcessPoolActiveOrderDetailReadMapper detailReadMapper;
    @Mock
    private MesFrontlineProcessMaterialService processMaterialService;
    @Mock
    private MesPqcInspectionTaskMapper pqcTaskMapper;
    @Mock
    private MesPqcProcessInspectionAggregateDetailMapper pqcAggregateDetailMapper;
    @Mock
    private MesQaInspectionRegulationProcessMapper qaProcessMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListItemMapper replenishmentListItemMapper;
    @Mock
    private ErpKingdeeProductionReplenishmentListMapper replenishmentListMapper;
    @Mock
    private MesMdItemMapper itemMapper;
    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProProcessPoolEventRevisionMapper eventRevisionMapper;
    @Mock
    private ElectronicSignatureQueryService signatureQueryService;
    @Mock
    private AdminUserService adminUserService;
    @InjectMocks
    private MesTeamLeaderActiveOrderDetailServiceImpl service;

    @Mock private MesSubmissionSignatureIdentityReader submissionSignatureIdentityReader;
    @Mock private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper businessReviewMapper;

    @BeforeEach void verifiedPortFixture() { cn.iocoder.yudao.module.mes.service.pro.MesVerifiedConsumerFixture.wire(service, signatureQueryService, 1L); }
    @BeforeEach
    void setUp() {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "businessReviewMapper", businessReviewMapper);
        // These detail tests exercise the real business identity projection. Supply
        // its independently read event/review records as well as the existing row DTOs.
        lenient().when(eventMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long eventId = invocation.getArgument(0);
            var event = defaultPqcEvent(eventId);
            event.setTenantId(1L);
            event.setActualEmployeeId(3001L);
            event.setDeviceAccountId(3001L);
            event.setSignatureId(eventId + 10000L);
            event.setEventIdempotencyKey("fixture-original-" + eventId);
            var original = detailReadMapper.selectByActiveOrderId(8101L).stream()
                    .filter(row -> eventId.equals(row.getEventId())).findFirst();
            if (original.isPresent()) {
                var row = original.get();
                event.setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT);
                event.setRouteProcessId(row.getRouteProcessId());
                event.setProcessId(row.getProcessId());
                event.setRawPayload(row.getOriginalPayloadJson() == null
                        ? "{\"activeOrderId\":8101,\"signatureIdentityDomain\":\"SYSTEM_USER\"}"
                        : row.getOriginalPayloadJson());
            } else {
                event.setFeedbackSourceType("MES_PQC_INSPECTION_TASK");
                event.setFeedbackSourceId(4101L);
            }
            return event;
        });
        lenient().when(businessReviewMapper.selectListByEventId(anyLong())).thenAnswer(invocation -> {
            Long eventId = invocation.getArgument(0);
            var review = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO()
                    .setId(eventId + 30000L).setEventId(eventId).setReviewSignatureId(eventId + 20000L)
                    .setLeaderUserId(3001L).setLeaderType(eventId < 7100L ? "PRODUCTION" : "PQC")
                    .setReviewStatus("APPROVED");
            review.setTenantId(1L);
            return List.of(review);
        });
        lenient().when(submissionSignatureIdentityReader.read(anyLong(), anyLong(), anyLong(),
                org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            String name = id == 17001L ? "甲" : id == 17002L ? "乙" : "冻结签名者B";
            return new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignatureId(id)
                    .setSignerName(name).setRole(invocation.getArgument(3))
                    .setSignedAt(id == 17001L ? LocalDateTime.parse("2026-08-13T08:10:00") : LocalDateTime.parse("2026-08-13T15:20:00"));
        });
        lenient().when(pickListMapper.selectBatchIds(anyList())).thenAnswer(invocation ->
                ((List<Long>) invocation.getArgument(0)).stream()
                        .map(id -> cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO.builder()
                                .id(id).sourceBillNo("LL-" + id).documentStatus("已审核").build())
                        .toList());
        lenient().when(pickListItemMapper.selectListByPickListIds(anyList())).thenReturn(List.of());
        lenient().when(replenishmentListItemMapper.selectListByProductionOrderNo("881MO090889"))
                .thenReturn(List.of());
        lenient().when(eventMapper.selectBatchIds(anyList())).thenAnswer(invocation -> {
            List<Long> eventIds = invocation.getArgument(0);
            return eventIds.stream()
                    .map(MesTeamLeaderActiveOrderDetailServiceImplTest::defaultPqcEvent)
                    .toList();
        });
        lenient().when(eventRevisionMapper.selectListByEventId(anyLong())).thenReturn(List.of());
    }

    @Test
    void currentAndArchivedDetailsKeepActualPersonnelSeparateFromFrozenSignerDisplay() {
        var active = MesProcessPoolActiveOrderDO.builder().id(8101L).leaderUserId(3001L)
                .workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build();
        when(activeOrderMapper.selectById(8101L)).thenReturn(active);
        when(activeOrderMapper.selectByIdIgnoreDeleted(8101L)).thenReturn(active);
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L,5001L,6001L,"粗洗","100",7003L,"1","名单更新后的B",null,"2026-08-13T08:10:00")));
        for (var detail : List.of(service.getDetail(3001L,8101L), service.getArchivedFormalDetail(8101L))) {
            var submission = detail.getProcesses().get(0).getSubmissions().get(0);
            assertEquals("名单更新后的B",submission.getSubmitterName());
            assertEquals("冻结签名者B",submission.getSubmitterSignature().getSignerName());
        }
        org.mockito.Mockito.verify(submissionSignatureIdentityReader,org.mockito.Mockito.times(2))
                .read(17003L,7003L,8101L,"PRODUCTION_SUBMIT");
    }

    @Test
    void currentAndArchivedDetailsPreserveExplicitNonFormalSimulationDisplay() {
        var active = MesProcessPoolActiveOrderDO.builder().id(8101L).leaderUserId(3001L)
                .workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build();
        when(activeOrderMapper.selectById(8101L)).thenReturn(active);
        when(activeOrderMapper.selectByIdIgnoreDeleted(8101L)).thenReturn(active);
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L,5001L,6001L,"粗洗","100",7003L,"1","模拟组长",null,"2026-08-13T08:10:00")));
        when(submissionSignatureIdentityReader.read(17003L,7003L,8101L,"PRODUCTION_SUBMIT"))
                .thenReturn(new MesTeamLeaderActiveOrderDetail.SignatureDetail().setSignerName("模拟组长（模拟）")
                        .setRole("SIMULATION_SESSION").setSignedAt(LocalDateTime.parse("2026-08-13T08:10:00")));
        for (var detail : List.of(service.getDetail(3001L,8101L),service.getArchivedFormalDetail(8101L))) {
            var signature=detail.getProcesses().get(0).getSubmissions().get(0).getSubmitterSignature();
            assertEquals("模拟组长（模拟）",signature.getSignerName());
            assertEquals("SIMULATION_SESSION",signature.getRole());assertNull(signature.getSignatureId());
        }
    }

    @Test
    void shouldGroupMultipleEmployeesAndSubmissionsByFormalProcessSnapshot() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "30", "张三", "生产组长甲",
                        "2026-08-13T08:10:00"),
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7002L, "40", "李四", null,
                        "2026-08-13T09:20:00"),
                row(9102L, 5002L, 6002L, "精洗", "100.000000", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5002L, 6002L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(8101L, detail.getActiveOrderId());
        assertEquals("881MO090889", detail.getWorkOrderCode());
        assertEquals("球囊扩张压力泵工艺路线", detail.getRouteName());
        assertEquals(2, detail.getProcesses().size());
        MesTeamLeaderActiveOrderDetail.ProcessDetail roughWash = detail.getProcesses().get(0);
        assertEquals(new BigDecimal("100.000000"), roughWash.getRequiredQuantity());
        assertEquals(Boolean.TRUE, roughWash.getKeyFlag());
        assertEquals(new BigDecimal("70"), roughWash.getSubmittedQuantity());
        assertEquals(2, roughWash.getSubmissionCount());
        assertEquals("张三", roughWash.getSubmissions().get(0).getSubmitterName());
        assertEquals("生产组长甲", roughWash.getSubmissions().get(0).getReviewerName());
        assertEquals("李四", roughWash.getSubmissions().get(1).getSubmitterName());
        assertNull(roughWash.getSubmissions().get(1).getReviewerName());
        assertEquals(0, detail.getProcesses().get(1).getSubmissionCount());
        assertEquals(List.of(), detail.getProcesses().get(1).getSubmissions());
    }

    @Test
    void shouldExposeInputMaterialBatchesAndPqcSubmissionDetailsByProcess() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .routeVersionId(9301L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "30", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of(
                new MesFrontlineProcessMaterial(3101L, "MAT-A", "物料A", "S1",
                        MesFrontlineProcessMaterial.ROLE_INPUT, null, List.of("LOT-01", "LOT-02"),
                        new BigDecimal("10"), new BigDecimal("9"), new BigDecimal("9"),
                        List.of(2101L, 2102L), List.of("SIM-SOUT-001", "SIM-SOUT-002"),
                        List.of(2201L, 2202L), "hash-pick-list"),
                new MesFrontlineProcessMaterial(3102L, "OUT-A", "产出A", "S2",
                        MesFrontlineProcessMaterial.ROLE_OUTPUT, null, List.of(), null,
                        null, null, List.of(), List.of(), List.of(), null)));
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .inspectionType("FIRST")
                        .qaItemCode("").inspectionItemPlanJson("{\"WIDTH\":2}").plannedInspectionQuantity(2)
                        .inspectionRuleKey("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("12.3")
                        .itemResult("12.3")
                        .judgement("PASS")
                        .selectedEquipmentNumber("EQ-001")
                        .standardText("10-15")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(1, process.getInputMaterials().size());
        assertEquals("MAT-A", process.getInputMaterials().get(0).getMaterialCode());
        assertEquals(List.of("LOT-01", "LOT-02"), process.getInputMaterials().get(0).getBatchCodes());
        assertEquals(List.of(2101L, 2102L), process.getInputMaterials().get(0).getSourcePickListIds());
        assertEquals(List.of("SIM-SOUT-001", "SIM-SOUT-002"),
                process.getInputMaterials().get(0).getSourcePickListNos());
        assertEquals(1, detail.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail pqcSubmission = detail.getPqcSubmissions().get(0);
        assertEquals(4101L, pqcSubmission.getPqcTaskId());
        assertEquals(7101L, pqcSubmission.getSubmittedEventId());
        assertEquals(7001L, pqcSubmission.getProductionEventId());
        assertEquals(List.of(7001L), pqcSubmission.getProductionEventIds());
        assertEquals("PQC王五", pqcSubmission.getSubmitterName());
        assertEquals("PQC主管甲", pqcSubmission.getReviewerName());
        assertEquals(1, pqcSubmission.getProcessInspectionItems().size());
        assertEquals("WIDTH", pqcSubmission.getProcessInspectionItems().get(0).getItemCode());
        assertEquals("EQ-001", pqcSubmission.getProcessInspectionItems().get(0).getSelectedEquipmentNumber());
    }

    @Test
    void shouldSeparateOriginalSubmittedItemsFromCurrentProcessInspectionItems() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .routeVersionId(9301L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .inspectionType("FIRST")
                        .qaItemCode("")
                        .inspectionItemPlanJson("{\"WIDTH\":2}")
                        .plannedInspectionQuantity(2)
                        .inspectionRuleKey("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲")));
        when(eventMapper.selectBatchIds(List.of(7101L))).thenReturn(List.of(
                MesProProcessPoolEventDO.builder()
                        .id(7101L)
                        .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                        .rawPayload("""
                                {"pqcItemDetails":[{"itemCode":"WIDTH","itemName":"宽度","inspectionMethod":"卡尺","standardText":"10-15","sampleValues":["15.5"],"judgement":"FAIL","selectedEquipmentName":"游标卡尺","selectedEquipmentNumber":"EQ-NEW"}]}
                                """)
                        .build()));
        when(eventRevisionMapper.selectListByEventId(7101L)).thenReturn(List.of(
                MesProProcessPoolEventRevisionDO.builder()
                        .id(9001L)
                        .eventId(7101L)
                        .beforePayload("""
                                {"actualInspectionQuantity":13,"scrapQuantity":1,"pqcItemDetails":[{"itemCode":"WIDTH","itemName":"宽度","inspectionMethod":"卡尺","standardText":"10-15","sampleValues":["12.3"],"judgement":"PASS","selectedEquipmentName":"游标卡尺","selectedEquipmentNumber":"EQ-OLD"}]}
                                """)
                        .serverRevisionTime(LocalDateTime.parse("2026-08-13T10:00:00"))
                        .revisionStatus(MesProProcessPoolEventRevisionDO.STATUS_EFFECTIVE)
                        .build()));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("99.9")
                        .itemResult("99.9")
                        .judgement("FAIL")
                        .selectedEquipmentName("当前设备")
                        .selectedEquipmentNumber("EQ-CURRENT")
                        .standardText("10-15")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail submission =
                detail.getPqcSubmissions().get(0);
        assertEquals("12.3", submission.getSubmittedItems().get(0).getMeasuredValue());
        assertEquals("EQ-OLD", submission.getSubmittedItems().get(0).getSelectedEquipmentNumber());
        assertEquals("99.9", submission.getProcessInspectionItems().get(0).getMeasuredValue());
        assertEquals("EQ-CURRENT", submission.getProcessInspectionItems().get(0).getSelectedEquipmentNumber());
        assertEquals(13, submission.getClass().getMethod("getSubmittedInspectionQuantity").invoke(submission));
        assertEquals(1, submission.getClass().getMethod("getSubmittedScrapQuantity").invoke(submission));
        assertEquals(2, submission.getActualInspectionQuantity());
    }

    @Test
    void shouldJudgeEachDistinctOriginalNumericSampleUsingSnapshotLimits() {
        var items = parseSubmittedItems("""
                {"itemCode":"WIDTH","resultType":"NUMERIC","standardLowerLimit":10,
                 "standardUpperLimit":15,"standardPrecision":1,"sampleValues":["12.3","15.5","12.3"],
                 "judgement":"SUCCESS"}
                """);
        assertEquals(List.of("SUCCESS", "FAILURE", "SUCCESS"),
                items.stream().map(MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail::getJudgement).toList());
    }

    @Test
    void shouldJudgeEachDistinctOriginalBooleanSample() {
        var items = parseSubmittedItems("""
                {"itemCode":"APPEARANCE","resultType":"BOOLEAN",
                 "sampleValues":["合格","不合格"],"judgement":"SUCCESS"}
                """);
        assertEquals(List.of("SUCCESS", "FAILURE"),
                items.stream().map(MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail::getJudgement).toList());
    }

    @Test
    void shouldPreserveSnapshotJudgementForIdenticalValues() {
        var items = parseSubmittedItems("""
                {"itemCode":"APPEARANCE","sampleValues":["OK","OK"],"judgement":"PASS"}
                """);
        assertEquals(List.of("PASS", "PASS"),
                items.stream().map(MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail::getJudgement).toList());
    }

    @Test
    void shouldRejectDistinctNumericSamplesWithoutSnapshotLimits() {
        assertThrows(ServiceException.class, () -> parseSubmittedItems("""
                {"itemCode":"WIDTH","resultType":"NUMERIC",
                 "sampleValues":["12.3","15.5"],"judgement":"SUCCESS"}
                """));
    }

    @Test
    void shouldSerializePqcBusinessDateAsIsoStringEvenWithTimestampDatesEnabled() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .enable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var response = new cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo
                .MesTeamLeaderActiveOrderDetailRespVO.PqcSubmissionDetail()
                .setBusinessDate(LocalDate.of(2026, 9, 17));
        assertEquals("2026-09-17", mapper.readTree(mapper.writeValueAsString(response))
                .get("businessDate").asText());
    }

    private static List<MesTeamLeaderActiveOrderDetail.PqcSubmissionItemDetail> parseSubmittedItems(String json) {
        java.util.Map<?, ?> item = cn.iocoder.yudao.framework.common.util.json.JsonUtils
                .parseObject(json, java.util.Map.class);
        return org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                MesTeamLeaderActiveOrderDetailServiceImpl.class, "resolvePqcSubmittedItemDetails", List.of(item), 8101L);
    }

    @Test
    void shouldCollapseStage1PqcSubmissionCopiesAcrossProductionProcesses() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .routeVersionId(9301L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .simulated(Boolean.TRUE)
                .simulationStage("STAGE1")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", null, null, null, null, null),
                row(9102L, 5002L, 6002L, "精洗", "100.000000", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5002L, 6002L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .inspectionType("FIRST")
                        .qaItemCode("")
                        .inspectionItemPlanJson("{\"WIDTH\":2}")
                        .plannedInspectionQuantity(2)
                        .inspectionRuleKey("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .inspectionType("FIRST")
                        .qaItemCode("")
                        .inspectionItemPlanJson("{\"WIDTH\":2}")
                        .plannedInspectionQuantity(2)
                        .inspectionRuleKey("FIRST")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("FIRST")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(2)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7102L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("PQC-通用")
                        .processName("过程检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L, 7102L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7102L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("12.3")
                        .itemResult("12.3")
                        .judgement("PASS")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .sampleNo(1)
                        .itemCode("WIDTH")
                        .itemName("宽度")
                        .measuredValue("12.3")
                        .itemResult("12.3")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        int pqcSubmissionCount = detail.getPqcSubmissions().size();
        assertEquals(1, pqcSubmissionCount);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail submission =
                detail.getPqcSubmissions().get(0);
        assertEquals(4101L, submission.getPqcTaskId());
        assertEquals("FIRST", submission.getInspectionRuleKey());
        assertEquals("", submission.getQaItemCode());
        assertEquals("WIDTH", submission.getProcessInspectionItems().get(0).getItemCode());
        assertEquals(List.of(7101L), submission.getSubmittedEventIds());
        assertEquals(1, submission.getProcessInspectionItems().size());
    }

    @Test
    void shouldExposeMaterialDeviceMeteringValidityFromOriginalPayload() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        MesTeamLeaderActiveOrderDetailReadDO productionRow = row(9101L, 5001L, 6001L, "粗洗",
                "10.000000", 7001L, "10", "张三", "生产组长甲", "2026-08-13T08:10:00");
        productionRow.setOriginalPayloadJson("""
                {
                  "materialDetails": [
                    {
                      "materialId": 3102,
                      "materialCode": "OUT-A",
                      "materialName": "产出A",
                      "outputQuantity": 10,
                      "lossQuantity": 0,
                      "clearanceConfirmations": [
                        { "key": "workplace", "label": "清场", "confirmed": true },
                        { "key": "material", "label": "物料", "confirmed": true },
                        { "key": "cleaning", "label": "清洁", "confirmed": true }
                      ],
                      "selectedDevices": [
                        {
                          "deviceId": 980009,
                          "deviceCode": "B09393",
                          "deviceName": "超声波清洗机",
                          "inMeteringValidityPeriod": false
                        }
                      ],
                      "deviceParameterReadings": [
                        {
                          "deviceId": 980009,
                          "deviceCode": "B09393",
                          "deviceName": "超声波清洗机",
                          "parameterCode": "TEMP",
                          "parameterName": "清洗温度",
                          "unit": "℃",
                          "value": 45,
                          "textValue": "45",
                          "lowerLimit": 20,
                          "upperLimit": 30,
                          "parameterStatus": "ABOVE_UPPER"
                        }
                      ]
                    }
                  ]
                }
                """);
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(productionRow));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.SubmissionMaterialDetail material =
                detail.getProcesses().get(0).getSubmissions().get(0).getMaterials().get(0);
        assertEquals("OUT-A", material.getMaterialCode());
        assertEquals(1, material.getDevices().size());
        assertEquals("B09393", material.getDevices().get(0).getDeviceCode());
        assertEquals(Boolean.FALSE, material.getDevices().get(0).getInMeteringValidityPeriod());
        assertEquals(3, material.getClearanceConfirmations().size());
        assertEquals("workplace", material.getClearanceConfirmations().get(0).getKey());
        assertEquals(Boolean.TRUE, material.getClearanceConfirmations().get(0).getConfirmed());
        assertEquals(1, material.getDeviceParameters().size());
        assertEquals("TEMP", material.getDeviceParameters().get(0).getParameterCode());
        assertEquals(new BigDecimal("20"), material.getDeviceParameters().get(0).getLowerLimit());
        assertEquals(new BigDecimal("30"), material.getDeviceParameters().get(0).getUpperLimit());
        assertEquals("ABOVE_UPPER", material.getDeviceParameters().get(0).getParameterStatus());
    }

    @Test
    void shouldKeepFinalInspectionTasksSeparatedByOriginalEventAndFrozenItemPlan() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100.000000", 7001L, "100", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .inspectionType("FINAL")
                        .qaItemCode("").inspectionItemPlanJson("{\"APPEARANCE\":3}").plannedInspectionQuantity(3)
                        .inspectionRuleKey("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(8869L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .inspectionType("FINAL")
                        .qaItemCode("").inspectionItemPlanJson("{\"CLEAN\":3}").plannedInspectionQuantity(3)
                        .inspectionRuleKey("FINAL")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(3)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(8872L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("QA-粗洗")
                        .processName("粗洗检验")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(8869L, 8872L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(8869L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(8872L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC赵六")
                        .setReviewerName("PQC主管甲")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("CLEAN")
                        .itemName("清洁度")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(2, detail.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail appearance = detail.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail clean = detail.getPqcSubmissions().get(1);
        assertEquals("FINAL", appearance.getInspectionType());
        assertEquals("FINAL", appearance.getInspectionRuleKey());
        assertEquals("", appearance.getQaItemCode());
        assertEquals(List.of(8869L), appearance.getSubmittedEventIds());
        assertEquals(List.of(7001L), appearance.getProductionEventIds());
        assertEquals("PQC王五", appearance.getSubmitterName());
        assertEquals(1, appearance.getProcessInspectionItems().size());
        assertEquals("APPEARANCE", appearance.getProcessInspectionItems().get(0).getItemCode());
        assertEquals("FINAL", clean.getInspectionType());
        assertEquals("FINAL", clean.getInspectionRuleKey());
        assertEquals("", clean.getQaItemCode());
        assertEquals(List.of(8872L), clean.getSubmittedEventIds());
        assertEquals(List.of(7002L), clean.getProductionEventIds());
        assertEquals("PQC赵六", clean.getSubmitterName());
        assertEquals(1, clean.getProcessInspectionItems().size());
        assertEquals("CLEAN", clean.getProcessInspectionItems().get(0).getItemCode());
    }

    @Test
    void shouldKeepPatrolAmAndPmAsSeparatePqcSubmissionBlocks() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "清洗", "100.000000", 7001L, "100", "张三", "生产组长甲",
                        "2026-08-13T08:10:00")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .inspectionType("PATROL")
                        .qaItemCode("").inspectionItemPlanJson("{\"APPEARANCE\":1}").plannedInspectionQuantity(1)
                        .inspectionRuleKey("PATROL_AM")
                        .shiftCode("AM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(10287L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .inspectionType("PATROL")
                        .qaItemCode("").inspectionItemPlanJson("{\"APPEARANCE\":1}").plannedInspectionQuantity(1)
                        .inspectionRuleKey("PATROL_PM")
                        .shiftCode("PM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(10288L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("PQC-清洗")
                        .processName("清洗")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(10287L, 10288L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(10287L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员"),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(10288L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC管理员")
                        .setReviewerName("PQC管理员")));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("PASS")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(2, detail.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail am = detail.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail pm = detail.getPqcSubmissions().get(1);
        assertEquals("PATROL_AM", am.getInspectionRuleKey());
        assertEquals(List.of(10287L), am.getSubmittedEventIds());
        assertEquals(List.of(7001L), am.getProductionEventIds());
        assertEquals("PATROL_PM", pm.getInspectionRuleKey());
        assertEquals(List.of(10288L), pm.getSubmittedEventIds());
        assertEquals(List.of(7002L), pm.getProductionEventIds());
    }

    @Test
    void shouldKeepSamePqcScrapItemRowsSeparatedBySubmittedAndProductionEvent() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .qaRegulationVersionId(5201L)
                .activeStatus("ACTIVE")
                .build());
        MesTeamLeaderActiveOrderDetailReadDO firstProduction = row(9101L, 5001L, 6001L, "清洗",
                "100.000000", 7001L, "40", "甲", "生产组长甲", "2026-08-13T08:10:00")
                .setSubmitterSignatureId(17001L)
                .setSubmitterSignedAt(LocalDateTime.parse("2026-08-13T08:10:00"));
        MesTeamLeaderActiveOrderDetailReadDO secondProduction = row(9101L, 5001L, 6001L, "清洗",
                "100.000000", 7002L, "60", "乙", "生产组长甲", "2026-08-13T15:20:00")
                .setSubmitterSignatureId(17002L)
                .setSubmitterSignedAt(LocalDateTime.parse("2026-08-13T15:20:00"));
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(firstProduction, secondProduction));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcInspectionTaskDO.builder()
                        .id(4101L)
                        .activeOrderId(8101L)
                        .inspectionType("FINAL")
                        .qaItemCode("").inspectionItemPlanJson("{\"APPEARANCE\":1}").plannedInspectionQuantity(1)
                        .inspectionRuleKey("FINAL")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("AM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7101L)
                        .build(),
                MesPqcInspectionTaskDO.builder()
                        .id(4102L)
                        .activeOrderId(8101L)
                        .inspectionType("FINAL")
                        .qaItemCode("").inspectionItemPlanJson("{\"APPEARANCE\":1}").plannedInspectionQuantity(1)
                        .inspectionRuleKey("FINAL")
                        .businessDate(LocalDate.of(2026, 8, 13))
                        .shiftCode("PM")
                        .roundNo(1)
                        .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                        .actualInspectionQuantity(1)
                        .qaProcessId(5101L)
                        .regulationVersionId(5201L)
                        .submittedEventId(7102L)
                        .build()));
        when(qaProcessMapper.selectBatchIds(List.of(5101L))).thenReturn(List.of(
                MesQaInspectionRegulationProcessDO.builder()
                        .id(5101L)
                        .regulationVersionId(5201L)
                        .processCode("PQC-清洗")
                        .processName("清洗")
                        .sort(1)
                        .build()));
        when(detailReadMapper.selectEventPartiesByEventIds(List.of(7101L, 7102L))).thenReturn(List.of(
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7101L)
                        .setProductionEventId(7001L)
                        .setSubmitterName("PQC王五")
                        .setReviewerName("PQC主管甲")
                        .setScrapQuantity(2),
                new MesTeamLeaderActiveOrderEventPartyReadDO()
                        .setEventId(7102L)
                        .setProductionEventId(7002L)
                        .setSubmitterName("PQC赵六")
                        .setReviewerName("PQC主管甲")
                        .setScrapQuantity(3)));
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of(
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4201L)
                        .pqcTaskId(4101L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("FAIL")
                        .build(),
                MesPqcProcessInspectionAggregateDetailDO.builder()
                        .id(4202L)
                        .pqcTaskId(4102L)
                        .activeOrderId(8101L)
                        .routeProcessId(5001L)
                        .processId(6001L)
                        .sampleNo(1)
                        .itemCode("APPEARANCE")
                        .itemName("外观")
                        .judgement("FAIL")
                        .build()));

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(2, detail.getPqcSubmissions().size());
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail firstScrap = detail.getPqcSubmissions().get(0);
        MesTeamLeaderActiveOrderDetail.PqcSubmissionDetail secondScrap = detail.getPqcSubmissions().get(1);
        assertEquals(List.of(7101L), firstScrap.getSubmittedEventIds());
        assertEquals(List.of(7001L), firstScrap.getProductionEventIds());
        assertEquals(List.of(), firstScrap.getProductionSubmitterSignatures());
        var firstProductionSignature = process.getSubmissions().stream()
                .filter(submission -> submission.getEventId().equals(7001L)).findFirst().orElseThrow().getSubmitterSignature();
        assertEquals(17001L, firstProductionSignature.getSignatureId());
        assertEquals("甲", firstProductionSignature.getSignerName());
        assertEquals(LocalDateTime.parse("2026-08-13T08:10:00"),
                firstProductionSignature.getSignedAt());
        assertEquals(2, firstScrap.getScrapQuantity());
        assertEquals("PQC王五", firstScrap.getSubmitterName());
        assertEquals(List.of(7102L), secondScrap.getSubmittedEventIds());
        assertEquals(List.of(7002L), secondScrap.getProductionEventIds());
        assertEquals(List.of(), secondScrap.getProductionSubmitterSignatures());
        var secondProductionSignature = process.getSubmissions().stream()
                .filter(submission -> submission.getEventId().equals(7002L)).findFirst().orElseThrow().getSubmitterSignature();
        assertEquals(17002L, secondProductionSignature.getSignatureId());
        assertEquals("乙", secondProductionSignature.getSignerName());
        assertEquals(LocalDateTime.parse("2026-08-13T15:20:00"),
                secondProductionSignature.getSignedAt());
        assertEquals(3, secondScrap.getScrapQuantity());
        assertEquals("PQC赵六", secondScrap.getSubmitterName());
    }

    @Test
    void shouldMarkAllSubmissionsOfOverrunProcessAsQuantityConflict() throws Exception {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(9201L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7001L, "1000", "张三", "生产组长甲",
                        "2026-08-19T16:52:08"),
                row(9101L, 5001L, 6001L, "粗洗", "1500.000000", 7002L, "2000", "李四", "生产组长甲",
                        "2026-08-19T16:53:03")));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(pqcTaskMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());
        when(pqcAggregateDetailMapper.selectListByActiveOrderId(8101L)).thenReturn(List.of());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        MesTeamLeaderActiveOrderDetail.ProcessDetail process = detail.getProcesses().get(0);
        assertEquals(Boolean.TRUE, invokeBoolean(process, "getQuantityConflict"));
        assertEquals(0, new BigDecimal("1500").compareTo(invokeBigDecimal(process, "getOverageQuantity")));
        for (MesTeamLeaderActiveOrderDetail.SubmissionDetail submission : process.getSubmissions()) {
            assertEquals(Boolean.TRUE, invokeBoolean(submission, "getQuantityConflict"));
        }
    }

    @Test
    void shouldRejectActiveOrderOutsideCurrentLeaderOrRemovedOrder() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3002L)
                .activeStatus("ACTIVE")
                .build());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), error.getCode());
    }

    @Test
    void activeOrderDetailExposesCurrentStatusForReleaseApplication() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE")
                .udiControlDocumentNo("UDI-TEST-20260924-001/V1").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(releaseApplicationMapper.selectLatestByActiveOrderId(8101L)).thenReturn(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(7101L)
                        .activeOrderId(8101L)
                        .workOrderId(9001L)
                        .applicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                        .build());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING, detail.getActiveOrderStatus().getStatus());
        assertEquals("待上市放行", detail.getActiveOrderStatus().getStatusLabel());
        assertEquals("UDI-TEST-20260924-001/V1", detail.getUdiControlDocumentNo());
    }

    @Test
    void activeOrderDetailShowsVoidedWhenRejectedReleaseReviewClosedAsVoid() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L).leaderUserId(3001L).workOrderId(9001L).routeId(9201L).activeStatus("ACTIVE").build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of(
                row(9101L, 5001L, 6001L, "粗洗", "100", null, null, null, null, null)));
        when(processMaterialService.listFrozenMaterials(8101L, 9201L, 5001L, 6001L)).thenReturn(List.of());
        when(backfillMapper.selectByActiveOrderAndType(8101L, "BATCH_RECORD")).thenReturn(null);
        when(releaseApplicationMapper.selectLatestByActiveOrderId(8101L)).thenReturn(
                MesProcessPoolActiveOrderReleaseApplicationDO.builder()
                        .id(7101L)
                        .activeOrderId(8101L)
                        .workOrderId(9001L)
                        .applicationStatus(MesReleaseFlowStatus.PQC_RELEASE_REJECTED)
                        .build());
        when(nonconformanceReviewMapper.selectLatestBySource("PQC_RELEASE", 7101L)).thenReturn(
                MesProEdhrNonconformanceReviewDO.builder()
                        .id(7201L)
                        .sourceType("PQC_RELEASE")
                        .sourceId(7101L)
                        .activeOrderId(8101L)
                        .reviewStatus("closed")
                        .disposition("void")
                        .build());

        MesTeamLeaderActiveOrderDetail detail = service.getDetail(3001L, 8101L);

        assertEquals("VOIDED", detail.getActiveOrderStatus().getStatus());
        assertEquals("已作废", detail.getActiveOrderStatus().getStatusLabel());
    }

    @Test
    void shouldFailWhenFormalProcessSnapshotsAreMissing() {
        when(activeOrderMapper.selectById(8101L)).thenReturn(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .activeStatus("ACTIVE")
                .build());
        when(detailReadMapper.selectByActiveOrderId(8101L)).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class, () -> service.getDetail(3001L, 8101L));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(), error.getCode());
    }


    @Test
    void ordinaryOrderDoesNotReadAnyReworkSource() {
        var normal = MesProcessPoolActiveOrderDO.builder().id(420L).build();
        assertNull(org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "resolveReworkSource", normal));
        org.mockito.Mockito.verifyNoInteractions(activeOrderMapper, nonconformanceReviewMapper, signatureQueryService);
    }

    @Test
    void partialReworkReferenceIsAnErrorInsteadOfAnOrdinaryOrder() {
        var current = MesProcessPoolActiveOrderDO.builder().id(420L).reworkSourceActiveOrderId(419L).build();
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "resolveReworkSource", current));
        assertEquals("REWORK_DETAIL_FORMAL_LINK_REQUIRED", error.getMessage());
    }

    @Test
    void exactFormalSourceReviewAndQaEvidenceAreProjectedWithoutWorkOrderLookup() {
        var current = reworkFixture();
        MesTeamLeaderActiveOrderDetail.ReworkSourceDetail source =
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "resolveReworkSource", current);
        assertEquals(420L, source.getCurrentActiveOrderId()); assertEquals(419L, source.getSourceActiveOrderId());
        assertEquals(90L, source.getReviewId()); assertEquals(9908090345L, source.getQaUserId());
        assertEquals(29026L, source.getQaSignature().getSignatureId());
        assertEquals("VALID", source.getQaSignatureVerification().verificationStatus());
        assertNull(source.getQaSignature().getSignerName(), "A verified account signature without a name snapshot must retain its actual absence");
        org.mockito.Mockito.verify(activeOrderMapper).selectByIdIgnoreDeleted(419L);
        org.mockito.Mockito.verify(nonconformanceReviewMapper).selectById(90L);
    }

    @Test
    void sameWorkOrderCannotReplaceTheFormalSourceOrCrossTenantReview() {
        var current = reworkFixture();
        var wrong = MesProEdhrNonconformanceReviewDO.builder().id(90L).activeOrderId(418L).workOrderId(990274L)
                .reviewStatus("closed").disposition("rework").qaUserId(9908090345L).closedAt(LocalDateTime.now()).build();
        wrong.setTenantId(1L); when(nonconformanceReviewMapper.selectById(90L)).thenReturn(wrong);
        assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "resolveReworkSource", current));
        wrong.setActiveOrderId(419L).setTenantId(2L);
        assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "resolveReworkSource", current));
    }

    @Test
    void mutatedQaDecisionContentOrEvidenceHashCannotAppearVerified() {
        var current = reworkFixture();
        var review = nonconformanceReviewMapper.selectById(90L);
        review.setReviewOpinion("被修改的意见");
        IllegalStateException snapshotError = assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "resolveReworkSource", current));
        assertEquals("REWORK_DETAIL_QA_SNAPSHOT_MISMATCH", snapshotError.getMessage());
        review.setReviewOpinion("确认返工");
        when(signatureQueryService.verifyEvidence(29026L)).thenReturn(new ElectronicSignatureVerificationDTO(
                29026L, "VALID", "content", "content", "evidence", "different", "SHA256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "MISMATCH", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND", "CONSISTENT",
                List.of("CONSUMER_PORT_EVIDENCE_MISMATCH_FIXTURE"), LocalDateTime.now(), "V1"));
        IllegalStateException hashError = assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "resolveReworkSource", current));
        assertEquals("REWORK_DETAIL_QA_EVIDENCE_MISMATCH", hashError.getMessage());
    }


    @Test
    void softDeletedReworkedSourceUsesTheDedicatedHistoricalMapper() {
        reworkFixture();
        var source = activeOrderMapper.selectByIdIgnoreDeleted(419L);
        source.setRouteId(9201L).setVersion(0); source.setDeleted(true);
        when(detailReadMapper.selectArchivedReworkByActiveOrderId(419L,1L)).thenReturn(List.of(
                row(9101L,5001L,6001L,"清洗","100",null,null,null,null,null)
                        .setActiveOrderId(419L).setWorkOrderId(990274L)));
        when(processMaterialService.listArchivedFrozenMaterials(419L,9201L,5001L,6001L)).thenReturn(List.of());
        var detail = service.getArchivedFormalDetail(419L);
        assertEquals(419L,detail.getActiveOrderId()); assertEquals(1,detail.getProcesses().size());
        org.mockito.Mockito.verify(detailReadMapper).selectArchivedReworkByActiveOrderId(419L,1L);
        org.mockito.Mockito.verify(detailReadMapper,org.mockito.Mockito.never()).selectByActiveOrderId(419L);
        org.mockito.Mockito.verifyNoInteractions(signatureQueryService);
    }

    @Test
    void canonicalReviewAndAggregateCannotBeSubstitutedDespiteSelfConsistentHashes() {
        var current = reworkFixture(); var good = signatureQueryService.getById(29026L);
        for(String field : List.of("reviewSourceId","cellValuesHash")) {
            var wrong = JSON.parseObject(good.canonicalContentJson()); wrong.put(field,"wrong");
            when(signatureQueryService.getById(29026L)).thenReturn(copyReworkSignature(good,good.actorId(),wrong.toJSONString()));
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
            assertEquals("REWORK_DETAIL_QA_CANONICAL_MISMATCH",error.getMessage());
        }
    }

    @Test
    void verifiedSystemUserIdentityMustBelongToTheFormalQaSignerAndTenant() {
        var current = reworkFixture(); var good = signatureQueryService.getById(29026L);
        var content = JSON.parseObject(good.canonicalContentJson());
        content.put("signatureIdentity",new java.util.LinkedHashMap<>(java.util.Map.of("domain","SYSTEM_USER","tenantId",1L,"signerId",9908090345L,
                "operatorId",9908090345L,"displayName","QA测试人员")));
        when(signatureQueryService.getById(29026L)).thenReturn(copyReworkSignature(good,good.actorId(),content.toJSONString()));
        MesTeamLeaderActiveOrderDetail.ReworkSourceDetail valid =
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current);
        assertEquals(29026L,valid.getQaSignature().getSignatureId());
        content.getJSONObject("signatureIdentity").put("tenantId",2L);
        when(signatureQueryService.getById(29026L)).thenReturn(copyReworkSignature(good,good.actorId(),content.toJSONString()));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
        assertEquals("REWORK_DETAIL_QA_IDENTITY_MISMATCH",error.getMessage());
    }

    @Test
    void wrongQaActorAndCalculatedContentHashFailBeforeProjection() {
        var current = reworkFixture(); var good = signatureQueryService.getById(29026L);
        when(signatureQueryService.getById(29026L)).thenReturn(copyReworkSignature(good,9908090346L,good.canonicalContentJson()));
        IllegalStateException actorError = assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
        assertEquals("REWORK_DETAIL_QA_SIGNATURE_MISMATCH",actorError.getMessage());
        when(signatureQueryService.getById(29026L)).thenReturn(good);
        when(signatureQueryService.verifyEvidence(29026L)).thenReturn(new ElectronicSignatureVerificationDTO(
                29026L,"VALID","content","different","evidence","evidence","SHA256","v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "MISMATCH", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND", "CONSISTENT",
                List.of("CONSUMER_PORT_CONTENT_MISMATCH_FIXTURE"), LocalDateTime.now(), "V1"));
        IllegalStateException hashError = assertThrows(IllegalStateException.class,
                () -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
        assertEquals("REWORK_DETAIL_QA_EVIDENCE_MISMATCH",hashError.getMessage());
    }

    @Test
    void pendingVoidAndForeignSourceOrdersAreNeverPresentedAsReworkLineage() {
        var current = reworkFixture(); var review = nonconformanceReviewMapper.selectById(90L);
        review.setReviewStatus("pending_review");
        assertThrows(IllegalStateException.class,() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
        review.setReviewStatus("closed").setDisposition("void");
        assertThrows(IllegalStateException.class,() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
        review.setDisposition("rework"); var source = activeOrderMapper.selectByIdIgnoreDeleted(419L);
        source.setWorkOrderId(990275L);
        assertThrows(IllegalStateException.class,() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
        source.setWorkOrderId(990274L); source.setTenantId(2L);
        assertThrows(IllegalStateException.class,() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,"resolveReworkSource",current));
    }

    private ElectronicSignatureEvidenceDTO copyReworkSignature(ElectronicSignatureEvidenceDTO s,Long actor,String canonical) {
        return new ElectronicSignatureEvidenceDTO(s.id(),s.moduleCode(),s.actionCode(),s.subjectType(),s.subjectId(),s.subjectVersion(),
                actor,s.meaningCode(),s.meaningLabel(),s.reason(),s.signedAt(),s.timeEvidenceId(),s.authenticationMethod(),
                s.contentHash(),s.evidenceHash(),s.algorithm(),s.keyVersion(),s.policyVersion(),s.verificationStatus(),
                s.processInstanceId(),s.taskId(),s.nodeCode(),s.nodeOrder(),canonical,s.beforeContentJson(),s.afterContentJson(),
                s.fieldDiffJson(),JSON.parseObject(canonical).containsKey("signatureIdentity")
                        ? JSON.parseObject(canonical).getJSONObject("signatureIdentity").getString("displayName") : null,s.timeZone());
    }

    private MesProcessPoolActiveOrderDO reworkFixture() {
        var at = LocalDateTime.of(2026, 10, 5, 18, 0);
        var current = MesProcessPoolActiveOrderDO.builder().id(420L).workOrderId(990274L)
                .reworkSourceActiveOrderId(419L).reworkReviewId(90L).build(); current.setTenantId(1L);
        var source = MesProcessPoolActiveOrderDO.builder().id(419L).workOrderId(990274L)
                .activeStatus("REMOVED").businessStatus("REWORKED").build(); source.setTenantId(1L);
        var review = MesProEdhrNonconformanceReviewDO.builder().id(90L).reviewCode("NCR-90")
                .sourceType("ACTIVE_ORDER").sourceId(419L).activeOrderId(419L).workOrderId(990274L)
                .reviewStatus("closed").disposition("rework").qaUserId(9908090345L)
                .closedAt(at).nonconformanceReason("需要返工").reviewOpinion("确认返工").build(); review.setTenantId(1L);
        String aggregate = cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrQaDispositionAggregateHash.build(
                review, "rework", null, null, null, review.getReviewOpinion(), review.getQaUserId());
        review.setTraceSnapshotJson("{\"reviewId\":90,\"sourceType\":\"ACTIVE_ORDER\",\"sourceId\":419,"
                + "\"qaUserId\":9908090345,\"disposition\":\"rework\",\"qaSignatureSnapshotJson\":{"
                + "\"reviewId\":90,\"qaUserId\":9908090345,\"signatureId\":29026,"
                + "\"actionType\":\"QA_DISPOSITION\",\"disposition\":\"rework\",\"aggregateHash\":\"" + aggregate + "\"}}");
        String action = MesProBatchRecordExecutionSignatureService.ACTION_QA_DISPOSITION;
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, action,
                null, null, null, null, null, null, null, "EDHR_NONCONFORMANCE_REVIEW", 90L,
                "eDHR不合格评审处置", action, null, null, aggregate, null);
        when(activeOrderMapper.selectByIdIgnoreDeleted(419L)).thenReturn(source);
        lenient().when(nonconformanceReviewMapper.selectById(90L)).thenReturn(review);
        String canonical = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                new cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand(9908090345L,"MES",action,"MES_BATCH_RECORD",
                        subject,cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject),"确认返工")).canonicalContentJson();
        lenient().when(signatureQueryService.getById(29026L)).thenReturn(new ElectronicSignatureEvidenceDTO(
                29026L, "MES", action, "MES_BATCH_RECORD", subject, cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject),
                9908090345L, action, "QA处置", "确认返工", at, "time", "PASSWORD", "content", "evidence",
                "SHA256", "v1", "v1", "VALID", null, null, null, null, canonical, null, null, null, null));
        lenient().when(signatureQueryService.verifyEvidence(29026L)).thenReturn(new ElectronicSignatureVerificationDTO(
                29026L, "VALID", "content", "content", "evidence", "evidence", "SHA256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK", "NOT_BOUND", "CONSISTENT",
                List.of(), LocalDateTime.now(), "V1"));
        return current;
    }

    private static MesTeamLeaderActiveOrderDetailReadDO row(Long snapshotId,
                                                              Long routeProcessId,
                                                              Long processId,
                                                             String processName,
                                                             String requiredQuantity,
                                                             Long eventId,
                                                             String submittedQuantity,
                                                             String submitterName,
                                                             String reviewerName,
                                                             String submittedAt) {
        LocalDateTime parsedSubmittedAt = submittedAt == null ? null : LocalDateTime.parse(submittedAt);
        return new MesTeamLeaderActiveOrderDetailReadDO()
                .setSnapshotId(snapshotId)
                .setActiveOrderId(8101L)
                .setWorkOrderId(9001L)
                .setWorkOrderCode("881MO090889")
                .setRouteName("球囊扩张压力泵工艺路线")
                .setRouteProcessId(routeProcessId)
                .setProcessId(processId)
                .setProcessCode("P-" + processId)
                .setProcessName(processName)
                .setKeyFlag(true)
                .setRequiredQuantity(new BigDecimal(requiredQuantity))
                .setEventId(eventId)
                .setSubmittedQuantity(submittedQuantity == null ? null : new BigDecimal(submittedQuantity))
                .setSubmitterName(submitterName)
                .setReviewerName(reviewerName)
                .setSubmittedAt(parsedSubmittedAt)
                .setSubmitterSignatureId(eventId == null ? null : eventId + 10000)
                .setSubmitterSignedAt(parsedSubmittedAt);
    }

    private static MesProProcessPoolEventDO defaultPqcEvent(Long eventId) {
        return MesProProcessPoolEventDO.builder()
                .id(eventId)
                .eventType(MesProProcessPoolEventDO.EVENT_TYPE_PQC_INSPECTION)
                .rawPayload("""
                        {"actualInspectionQuantity":1,"scrapQuantity":0,"pqcItemDetails":[{"itemCode":"AUTO","itemName":"自动项目","sampleValues":["AUTO"],"judgement":"PASS"}]}
                        """)
                .build();
    }

    private static Boolean invokeBoolean(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (Boolean) method.invoke(target);
    }

    private static BigDecimal invokeBigDecimal(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (BigDecimal) method.invoke(target);
    }
}
