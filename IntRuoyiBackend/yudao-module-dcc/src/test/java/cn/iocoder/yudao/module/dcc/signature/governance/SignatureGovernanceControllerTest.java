package cn.iocoder.yudao.module.dcc.signature.governance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.SignatureGovernanceController;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordRespVO;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvPackageStatus;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvReleaseGateResult;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvReleaseGateStatus;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvService;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvServiceImpl;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalAuthorizationOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalMetrics;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalModuleOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalRouteOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalService;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalSummary;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyOverview;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyService;
import cn.iocoder.yudao.module.dcc.signature.service.records.SignatureGovernanceRecordPdfArtifact;
import cn.iocoder.yudao.module.dcc.signature.service.records.SignatureGovernanceRecordService;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoveryRehearsalResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionPrecheckResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionReceiptResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionService;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBatchEvaluation;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBlockerCode;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewService;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

class SignatureGovernanceControllerTest extends BaseMockitoUnitTest {

    @Mock
    private SignatureGovernanceRetentionService retentionService;
    @Mock
    private SignatureGovernanceReviewService reviewService;
    @Mock
    private SignatureGovernanceCsvService csvService;
    @Mock
    private SignatureGovernancePolicyService policyService;
    @Mock
    private SignatureGovernancePortalService portalService;
    @Mock
    private SignatureGovernanceRecordService recordService;
    @InjectMocks
    private SignatureGovernanceController controller;

    @Test
    void retentionPrecheck_mapsRequestToServiceAndReturnsTypedBlockers() {
        when(retentionService.precheck(any())).thenReturn(SignatureGovernanceRetentionPrecheckResult.blocked(List.of(
                new SignatureGovernanceRetentionBlocker(
                        SignatureGovernanceRetentionBlockerCode.OBJECT_LOCK_MISSING,
                        "Object Lock is missing",
                        "Retention cannot be enabled"))));

        SignatureGovernanceController.RetentionPrecheckReqVO reqVO =
                new SignatureGovernanceController.RetentionPrecheckReqVO();
        reqVO.setEndpoint("https://minio.test.local");
        reqVO.setBucketName("dcc-signatures");
        reqVO.setObjectLockEnabled(false);
        reqVO.setVersioningEnabled(true);
        reqVO.setDefaultRetentionEnabled(true);
        reqVO.setRetentionMode("COMPLIANCE");
        reqVO.setPermissionsVerified(true);
        reqVO.setOwnerUserId(101L);
        reqVO.setSampleDccSignatureId(710088L);
        reqVO.setSampleEdhrArchiveId(810099L);

        CommonResult<SignatureGovernanceController.RetentionPrecheckRespVO> result =
                controller.precheckRetention(reqVO);

        assertEquals("BLOCKED", result.getData().getStatus());
        assertEquals("OBJECT_LOCK_MISSING", result.getData().getBlockers().get(0).getCode());
        assertNull(result.getData().getReceiptId());
        ArgumentCaptor<?> commandCaptor = ArgumentCaptor.forClass(Object.class);
        verify(retentionService).precheck((cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionPrecheckCommand) commandCaptor.capture());
        Object command = commandCaptor.getValue();
        assertNotNull(command);
    }

    @Test
    void dccRetentionReceipt_mapsRequestToServiceAndReturnsRecordedReceipt() {
        when(retentionService.createDccEvidenceReceipt(any()))
                .thenReturn(SignatureGovernanceRetentionReceiptResult.recorded("DCC_SIGNATURE:710088:v-0001"));

        SignatureGovernanceController.RetentionReceiptReqVO reqVO =
                new SignatureGovernanceController.RetentionReceiptReqVO();
        reqVO.setSourceId(710088L);
        reqVO.setObjectKey("dcc/signature/710088.json");
        reqVO.setVersionId("v-0001");
        reqVO.setRetentionMode("COMPLIANCE");
        reqVO.setRetainUntil(java.time.Instant.parse("2036-05-28T00:00:00Z"));
        reqVO.setSha256("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
        reqVO.setEvidenceHash("dcc-evidence-hash");
        reqVO.setAuditEventId("audit-1001");

        CommonResult<SignatureGovernanceController.RetentionReceiptRespVO> result =
                controller.createDccEvidenceReceipt(reqVO);

        assertEquals("RECORDED", result.getData().getStatus());
        assertTrue(result.getData().getRecorded());
        assertEquals("DCC_SIGNATURE:710088:v-0001", result.getData().getReceiptId());
        verify(retentionService).createDccEvidenceReceipt(any());
    }

    @Test
    void edhrRetentionReceipt_mapsRequestToServiceAndReturnsTypedBlockers() {
        when(retentionService.createEdhrArchiveReceipt(any()))
                .thenReturn(SignatureGovernanceRetentionReceiptResult.blocked(List.of(
                        new SignatureGovernanceRetentionBlocker(
                                SignatureGovernanceRetentionBlockerCode.SIGNATURE_HASH_MISSING,
                                "signature hash is missing",
                                "recovery cannot prove archive signature"))));

        SignatureGovernanceController.RetentionReceiptReqVO reqVO =
                new SignatureGovernanceController.RetentionReceiptReqVO();
        reqVO.setSourceId(880077L);
        reqVO.setObjectKey("edhr/archive/880077.pdf");
        reqVO.setVersionId("v-0002");
        reqVO.setRetentionMode("COMPLIANCE");
        reqVO.setRetainUntil(java.time.Instant.parse("2036-05-28T00:00:00Z"));
        reqVO.setSha256("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
        reqVO.setArchiveSha256("edhr-archive-sha256");
        reqVO.setAuditEventId("audit-1002");

        CommonResult<SignatureGovernanceController.RetentionReceiptRespVO> result =
                controller.createEdhrArchiveReceipt(reqVO);

        assertEquals("BLOCKED", result.getData().getStatus());
        assertEquals(Boolean.FALSE, result.getData().getRecorded());
        assertEquals("SIGNATURE_HASH_MISSING", result.getData().getBlockers().get(0).getCode());
        verify(retentionService).createEdhrArchiveReceipt(any());
    }

    @Test
    void recoveryRehearsal_mapsRequestToServiceAndReturnsRecoveryGate() {
        when(retentionService.runRecoveryRehearsal(any()))
                .thenReturn(SignatureGovernanceRecoveryRehearsalResult.passed());

        SignatureGovernanceController.RecoveryRehearsalReqVO reqVO =
                new SignatureGovernanceController.RecoveryRehearsalReqVO();
        reqVO.setBackupId("backup-20260528-001");
        reqVO.setRecoveryRuntime("isolated-restore-runtime-01");
        reqVO.setOwnerReviewed(true);
        reqVO.setReportWritten(true);
        reqVO.setAuditWritten(true);
        SignatureGovernanceController.RecoverySampleReqVO sample =
                new SignatureGovernanceController.RecoverySampleReqVO();
        sample.setSampleType("DCC_SIGNATURE");
        sample.setObjectKey("dcc/signature/710088.json");
        sample.setVersionId("v-0001");
        sample.setExpectedSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        sample.setRestoredSha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        sample.setExpectedDomainHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        sample.setRestoredDomainHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        reqVO.setSamples(List.of(sample));

        CommonResult<SignatureGovernanceController.RecoveryRehearsalRespVO> result =
                controller.runRecoveryRehearsal(reqVO);

        assertEquals("PASSED", result.getData().getStatus());
        assertTrue(result.getData().getPassed());
        assertTrue(result.getData().getBlockers().isEmpty());
        verify(retentionService).runRecoveryRehearsal(any());
    }

    @Test
    void createPeriodicReviewBatch_returnsBlockedSnapshotPrerequisites() {
        when(reviewService.createBatch(any())).thenReturn(SignatureGovernanceReviewBatchEvaluation.blocked(List.of(
                SignatureGovernanceReviewBlocker.of(
                        SignatureGovernanceReviewBlockerCode.REVIEW_OWNER_MISSING,
                        "Review owner is missing",
                        "Batch cannot be created"))));

        SignatureGovernanceController.ReviewBatchCreateReqVO reqVO =
                new SignatureGovernanceController.ReviewBatchCreateReqVO();
        reqVO.setPeriodCode("2026-Q2");
        reqVO.setRuleVersion("review-rule-v1");
        reqVO.setScopeModules(List.of("DCC", "EDHR"));
        reqVO.setPermittedModules(List.of("DCC"));
        reqVO.setReviewSignatureStrategyConfigured(false);

        CommonResult<SignatureGovernanceController.ReviewBatchRespVO> result =
                controller.createReviewBatch(reqVO);

        assertEquals("BLOCKED", result.getData().getStatus());
        assertEquals("REVIEW_OWNER_MISSING", result.getData().getBlockers().get(0).getCode());
        verify(reviewService).createBatch(any());
    }

    @Test
    void evaluateCsvReleaseGate_keepsQaApprovalSeparateFromEngineeringVerification() {
        when(csvService.evaluateReleaseGate(any())).thenReturn(new SignatureGovernanceCsvReleaseGateResult(
                "release-20260528",
                SignatureGovernanceCsvReleaseGateStatus.BLOCKED,
                new cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvPackageResult(
                        "release-20260528",
                        SignatureGovernanceCsvPackageStatus.BLOCKED,
                        Set.of(),
                        List.of(),
                        true,
                        false,
                        List.of(SignatureGovernanceCsvBlocker.of(
                                SignatureGovernanceCsvBlockerCode.QA_APPROVAL_MISSING,
                                "QA approval is missing",
                                "Release gate remains blocked"))),
                true,
                false,
                List.of(SignatureGovernanceCsvBlocker.of(
                        SignatureGovernanceCsvBlockerCode.QA_APPROVAL_MISSING,
                        "QA approval is missing",
                        "Release gate remains blocked"))));

        SignatureGovernanceController.CsvReleaseGateReqVO reqVO =
                new SignatureGovernanceController.CsvReleaseGateReqVO();
        reqVO.setQualityOwner("qa-owner-1");
        reqVO.setEngineeringVerificationPassed(true);

        CommonResult<SignatureGovernanceController.CsvReleaseGateRespVO> result =
                controller.evaluateCsvReleaseGate("release-20260528", reqVO);

        assertEquals("BLOCKED", result.getData().getStatus());
        assertTrue(result.getData().getEngineeringVerificationPassed());
        assertEquals(Boolean.FALSE, result.getData().getQaApproved());
        assertEquals("QA_APPROVAL_MISSING", result.getData().getBlockers().get(0).getCode());
        verify(csvService).evaluateReleaseGate(any());
    }

    @Test
    void currentPolicy_usesPolicyServiceForEverySignatureModuleWithoutHardcodedDefaultCompliance() {
        when(policyService.describeModule(SignatureGovernanceModuleCode.DCC)).thenReturn(readyOverview(
                SignatureGovernanceModuleCode.DCC));
        when(policyService.describeModule(SignatureGovernanceModuleCode.EDHR)).thenReturn(blockedOverview(
                SignatureGovernanceModuleCode.EDHR, SignatureGovernancePolicyBlockerCode.POLICY_SOURCE_MISSING));
        when(policyService.describeModule(SignatureGovernanceModuleCode.SHOWROOM)).thenReturn(readyOverview(
                SignatureGovernanceModuleCode.SHOWROOM));
        when(policyService.describeModule(SignatureGovernanceModuleCode.INTAUTH)).thenReturn(blockedOverview(
                SignatureGovernanceModuleCode.INTAUTH,
                SignatureGovernancePolicyBlockerCode.AUTHORITY_SOURCE_UNCONFIRMED));

        CommonResult<SignatureGovernanceController.PolicyCurrentRespVO> result = controller.getCurrentPolicy();

        assertEquals("BLOCKED", result.getData().getStatus());
        assertFalse(result.getData().getReady());
        assertEquals(SignatureGovernanceModuleCode.DCC.name(), result.getData().getModules().get(0));
        assertEquals("POLICY_SOURCE_MISSING", result.getData().getBlockers().get(0).getCode());
        assertEquals(4, result.getData().getModuleStatuses().size());
        verify(policyService).describeModule(SignatureGovernanceModuleCode.DCC);
        verify(policyService).describeModule(SignatureGovernanceModuleCode.EDHR);
        verify(policyService).describeModule(SignatureGovernanceModuleCode.SHOWROOM);
        verify(policyService).describeModule(SignatureGovernanceModuleCode.INTAUTH);
    }

    @Test
    void currentPolicyMarksReadyWhenEveryModuleHasConfirmedPolicySourceAndAdapter() {
        Arrays.stream(SignatureGovernanceModuleCode.values()).forEach(moduleCode ->
                when(policyService.describeModule(moduleCode)).thenReturn(readyOverview(moduleCode)));

        CommonResult<SignatureGovernanceController.PolicyCurrentRespVO> result = controller.getCurrentPolicy();

        assertEquals("READY", result.getData().getStatus());
        assertTrue(result.getData().getReady());
        assertTrue(result.getData().getBlockers().isEmpty());
        assertEquals(4, result.getData().getModuleStatuses().size());
    }

    @Test
    void portalOverview_usesLoginUserAndReturnsUnifiedElectronicSignatureCards() {
        SignatureGovernancePortalOverview overview = new SignatureGovernancePortalOverview(
                "READY",
                true,
                SignatureGovernancePortalAuthorizationOverview.of("ENABLED", true, List.of()),
                new SignatureGovernancePortalSummary(2L, 2L, 0L, 12L, 21L),
                List.of(
                        new SignatureGovernancePortalModuleOverview(
                                SignatureGovernanceModuleCode.DCC,
                                "文件签名",
                                "受控文件签名、审批待办与授权管理",
                                "READY",
                                true,
                                SignatureGovernancePortalAuthorizationOverview.of("ENABLED", true, List.of()),
                                readyOverview(SignatureGovernanceModuleCode.DCC),
                                SignatureGovernancePortalMetrics.of(7L, 12L),
                                SignatureGovernancePortalRouteOverview.of("文件签名记录",
                                        "/signature-governance/file-signatures",
                                        "用户授权",
                                        "/signature-governance/authorizations"),
                                List.of()),
                        new SignatureGovernancePortalModuleOverview(
                                SignatureGovernanceModuleCode.EDHR,
                                "批记录签名",
                                "批记录签名、审核工作任务与授权管理",
                                "READY",
                                true,
                                SignatureGovernancePortalAuthorizationOverview.of("ENABLED", true, List.of()),
                                readyOverview(SignatureGovernanceModuleCode.EDHR),
                                SignatureGovernancePortalMetrics.of(5L, 9L),
                                SignatureGovernancePortalRouteOverview.of("批记录签名记录",
                                        "/signature-governance/batch-signatures",
                                        "工作任务",
                                        "/mes/pro/feedback/edhr-work-task"),
                                List.of())),
                List.of(SignatureGovernancePortalBlocker.of("SIGNATURE_AUTH_UNAUTHORIZED",
                        "当前用户未开通电子签名授权",
                        "所有需要电子签名的模块都会被阻断")));
        when(portalService.getOverview(101L)).thenReturn(overview);

        CommonResult<SignatureGovernanceController.PortalOverviewRespVO> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            result = controller.getPortalOverview();
        }

        assertEquals("READY", result.getData().getStatus());
        assertTrue(result.getData().getReady());
        assertEquals("ENABLED", result.getData().getAuthorization().getStatus());
        assertEquals(2L, result.getData().getSummary().getModuleTotal());
        assertEquals(12L, result.getData().getSummary().getPendingTotal());
        assertEquals("DCC", result.getData().getModules().get(0).getModuleCode());
        assertEquals("/signature-governance/file-signatures",
                result.getData().getModules().get(0).getRoutes().getPrimaryPath());
        assertEquals("/mes/pro/feedback/edhr-work-task",
                result.getData().getModules().get(1).getRoutes().getSecondaryPath());
        assertEquals("SIGNATURE_AUTH_UNAUTHORIZED", result.getData().getBlockers().get(0).getCode());
        verify(portalService).getOverview(101L);
    }

    @Test
    void signatureRecordsPage_usesUnifiedRecordServiceAndReturnsSourceLabels() {
        SignatureGovernanceRecordRespVO row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("BATCH_RECORD-88001");
        row.setSourceCode("BATCH_RECORD");
        row.setSourceLabel("批记录");
        row.setSourceTable("mes_pro_batch_record_execution_signature");
        row.setSourceRecordId(88001L);
        row.setBusinessRecordCode("BR-20260714-001");
        row.setBusinessRecordName("批记录执行");
        row.setSignerName("李四");
        row.setActorDeptNameSnapshot("生产部");
        row.setActorPostNamesSnapshot("班长");
        row.setActorRoleNamesSnapshot("eDHR审核人");
        row.setActionCode("APPROVE");
        row.setActionLabel("最终批准");
        row.setMeaningLabel("最终批准");
        row.setComment("通过");
        row.setSignedAt(LocalDateTime.of(2026, 7, 14, 9, 30));
        row.setEvidenceHash("batch-record-hash");
        row.setEvidenceStatus("VALID");
        when(recordService.getPage(any())).thenReturn(new PageResult<>(List.of(row), 1L));

        SignatureGovernanceRecordPageReqVO reqVO = new SignatureGovernanceRecordPageReqVO();
        reqVO.setSourceCodes(List.of("BATCH_RECORD"));
        reqVO.setKeyword("BR-20260714");
        reqVO.setSignerKeyword("李四");
        reqVO.setActionCode("APPROVE");

        CommonResult<PageResult<SignatureGovernanceRecordRespVO>> result = controller.getSignatureRecordPage(reqVO);

        assertEquals(1L, result.getData().getTotal());
        assertEquals("BATCH_RECORD-88001", result.getData().getList().get(0).getGlobalId());
        assertEquals("批记录", result.getData().getList().get(0).getSourceLabel());
        assertEquals("mes_pro_batch_record_execution_signature",
                result.getData().getList().get(0).getSourceTable());
        verify(recordService).getPage(reqVO);
    }

    @Test
    void mySignatureRecordsPage_forcesCurrentLoginUserScope() {
        SignatureGovernanceRecordRespVO row = new SignatureGovernanceRecordRespVO();
        row.setGlobalId("FILE-1001");
        row.setSourceCode("FILE");
        row.setSourceLabel("文件");
        row.setSourceTable("dcc_controlled_file_signature");
        row.setSourceRecordId(1001L);
        row.setBusinessRecordId(7001L);
        row.setBusinessRecordCode("DCC-001");
        row.setBusinessRecordName("我的签名文件");
        row.setSignerUserId(101L);
        row.setSignedAt(LocalDateTime.of(2026, 7, 15, 10, 30));
        when(recordService.getPage(any())).thenReturn(new PageResult<>(List.of(row), 1L));

        SignatureGovernanceRecordPageReqVO reqVO = new SignatureGovernanceRecordPageReqVO();
        reqVO.setSignerUserId(999L);
        reqVO.setKeyword("DCC-001");

        CommonResult<PageResult<SignatureGovernanceRecordRespVO>> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock =
                     mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            result = controller.getMySignatureRecordPage(reqVO);
        }

        assertEquals(1L, result.getData().getTotal());
        assertEquals("FILE-1001", result.getData().getList().get(0).getGlobalId());
        ArgumentCaptor<SignatureGovernanceRecordPageReqVO> reqCaptor =
                ArgumentCaptor.forClass(SignatureGovernanceRecordPageReqVO.class);
        verify(recordService).getPage(reqCaptor.capture());
        assertEquals(101L, reqCaptor.getValue().getSignerUserId());
        assertEquals("DCC-001", reqCaptor.getValue().getKeyword());
    }

    @Test
    void signatureRecordPdfEndpoint_returnsAttachmentForAnyGlobalSignatureRecord() {
        when(recordService.exportRecordPdf("BPM-1784126214000")).thenReturn(new SignatureGovernanceRecordPdfArtifact(
                "electronic-signature-BPM-1784126214000.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        ResponseEntity<byte[]> response = controller.exportSignatureRecordPdf("BPM-1784126214000");

        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("electronic-signature-BPM-1784126214000.pdf"));
        assertEquals("%PDF-1.4", new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8));
        verify(recordService).exportRecordPdf("BPM-1784126214000");
    }

    @Test
    void signatureRecordsPageReqVo_usesProjectDateTimeFormatForSignedAtRange() throws Exception {
        DateTimeFormat dateTimeFormat = SignatureGovernanceRecordPageReqVO.class
                .getDeclaredField("signedAt")
                .getAnnotation(DateTimeFormat.class);

        assertNotNull(dateTimeFormat);
        assertEquals(FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, dateTimeFormat.pattern());
    }

    @Test
    void endpointsExposeGovernanceRoutesAndPermissions() {
        assertEndpoint(PostMapping.class, "/signature-governance/retention/precheck",
                "signature-governance:retention:query");
        assertEndpoint(PostMapping.class, "/signature-governance/retention/dcc-evidence-receipts",
                "signature-governance:retention:manage");
        assertEndpoint(PostMapping.class, "/signature-governance/retention/edhr-archive-receipts",
                "signature-governance:retention:manage");
        assertEndpoint(PostMapping.class, "/signature-governance/retention/recovery-rehearsals",
                "signature-governance:retention:manage");
        assertEndpoint(PostMapping.class, "/signature-governance/periodic-review/batches",
                "signature-governance:periodic-review:manage");
        assertEndpoint(PostMapping.class, "/signature-governance/csv/packages/{releaseId}/release-gate",
                "signature-governance:csv-package:manage");
        assertEndpoint(GetMapping.class, "/signature-governance/policies/current",
                "signature-governance:policy:query");
        assertEndpoint(GetMapping.class, "/signature-governance/portal/overview",
                "signature-governance:policy:query");
        assertEndpoint(GetMapping.class, "/signature-governance/signature-records/page",
                "signature-governance:policy:query");
        assertEndpoint(GetMapping.class, "/signature-governance/my-signature-records/page",
                "signature-governance:policy:query");
        assertEndpoint(GetMapping.class, "/signature-governance/signature-records/{globalId}/pdf",
                "signature-governance:policy:query");
    }

    @Test
    void controllerDependenciesAreRegisteredAsSpringServices() {
        assertTrue(SignatureGovernanceReviewServiceImpl.class.isAnnotationPresent(Service.class));
        assertTrue(SignatureGovernanceCsvServiceImpl.class.isAnnotationPresent(Service.class));
    }

    private static SignatureGovernancePolicyOverview readyOverview(SignatureGovernanceModuleCode moduleCode) {
        return new SignatureGovernancePolicyOverview(moduleCode, true, true, true,
                "policy-v1", moduleCode.name().toLowerCase() + "-policy-source",
                moduleCode.name().toLowerCase() + "-governance-adapter",
                moduleCode.name().toLowerCase() + "-adapter-v1",
                moduleCode.name().toLowerCase() + "-evidence-v1",
                List.of());
    }

    private static SignatureGovernancePolicyOverview blockedOverview(SignatureGovernanceModuleCode moduleCode,
            SignatureGovernancePolicyBlockerCode blockerCode) {
        return new SignatureGovernancePolicyOverview(moduleCode, true, false, true,
                "policy-v1", moduleCode.name().toLowerCase() + "-policy-source",
                moduleCode.name().toLowerCase() + "-governance-adapter",
                moduleCode.name().toLowerCase() + "-adapter-v1",
                moduleCode.name().toLowerCase() + "-evidence-v1",
                List.of(SignatureGovernancePolicyBlocker.of(blockerCode,
                        moduleCode + " policy is blocked",
                        "Production signing must remain blocked until the policy owner resolves this module")));
    }

    private void assertEndpoint(Class<? extends Annotation> mappingAnnotationType, String fullPath,
                                String permission) {
        Method method = Arrays.stream(SignatureGovernanceController.class.getDeclaredMethods())
                .filter(candidate -> candidate.isAnnotationPresent(mappingAnnotationType))
                .filter(candidate -> hasFullMappingPath(candidate.getAnnotation(mappingAnnotationType), fullPath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing endpoint mapping: " + fullPath));
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains(permission));
    }

    private boolean hasFullMappingPath(Annotation methodMapping, String expectedFullPath) {
        return classPrefixes().flatMap(prefix -> annotationPaths(methodMapping)
                        .map(methodPath -> normalizePath(prefix + "/" + methodPath)))
                .anyMatch(expectedFullPath::equals);
    }

    private Stream<String> classPrefixes() {
        RequestMapping requestMapping = SignatureGovernanceController.class.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return Stream.of("");
        }
        return Stream.concat(Arrays.stream(requestMapping.value()), Arrays.stream(requestMapping.path())).distinct();
    }

    private Stream<String> annotationPaths(Annotation annotation) {
        try {
            String[] value = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
            String[] path = (String[]) annotation.annotationType().getMethod("path").invoke(annotation);
            return Stream.concat(Arrays.stream(value), Arrays.stream(path)).distinct();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot inspect endpoint mapping annotation", ex);
        }
    }

    private static String normalizePath(String value) {
        return value.replaceAll("/+", "/").replaceAll("/$", "");
    }
}
