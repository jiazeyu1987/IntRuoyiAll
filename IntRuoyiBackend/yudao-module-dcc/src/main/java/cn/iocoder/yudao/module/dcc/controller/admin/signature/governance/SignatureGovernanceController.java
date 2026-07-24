package cn.iocoder.yudao.module.dcc.controller.admin.signature.governance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordRespVO;
import cn.iocoder.yudao.module.dcc.signature.core.SignatureGovernanceModuleCode;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvChangeControl;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvMaterial;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvMaterialStatus;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvMaterialType;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvPackageCommand;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvQaApproval;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvReleaseGateCommand;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvReleaseGateResult;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvService;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvTraceRelation;
import cn.iocoder.yudao.module.dcc.signature.service.csv.SignatureGovernanceCsvTrainingRecord;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalAuthorizationOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalMetrics;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalModuleOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalRouteOverview;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalService;
import cn.iocoder.yudao.module.dcc.signature.service.portal.SignatureGovernancePortalSummary;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyOverview;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyService;
import cn.iocoder.yudao.module.dcc.signature.service.records.SignatureGovernanceRecordPdfArtifact;
import cn.iocoder.yudao.module.dcc.signature.service.records.SignatureGovernanceRecordService;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoveryRehearsalCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoveryRehearsalResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoverySample;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRecoverySampleType;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionPrecheckCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionPrecheckResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionReceiptCommand;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionReceiptResult;
import cn.iocoder.yudao.module.dcc.signature.service.retention.SignatureGovernanceRetentionService;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBatchCommand;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewBatchEvaluation;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewFindingCode;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewService;
import cn.iocoder.yudao.module.dcc.signature.service.review.SignatureGovernanceReviewSourceProjection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 电子签名")
@RestController
@RequestMapping("/signature-governance")
@Validated
public class SignatureGovernanceController {

    @Resource
    private SignatureGovernanceRetentionService retentionService;
    @Resource
    private SignatureGovernanceReviewService reviewService;
    @Resource
    private SignatureGovernanceCsvService csvService;
    @Resource
    private SignatureGovernancePolicyService policyService;
    @Resource
    private SignatureGovernancePortalService portalService;
    @Resource
    private SignatureGovernanceRecordService signatureGovernanceRecordService;

    @PostMapping("/retention/precheck")
    @Operation(summary = "执行电子签名留存预检")
    @PreAuthorize("@ss.hasPermission('signature-governance:retention:query')")
    public CommonResult<RetentionPrecheckRespVO> precheckRetention(
            @Valid @RequestBody RetentionPrecheckReqVO reqVO) {
        return success(RetentionPrecheckRespVO.of(retentionService.precheck(reqVO.toCommand())));
    }

    @PostMapping("/retention/dcc-evidence-receipts")
    @Operation(summary = "记录 DCC 签名证据 WORM 留存回执")
    @PreAuthorize("@ss.hasPermission('signature-governance:retention:manage')")
    public CommonResult<RetentionReceiptRespVO> createDccEvidenceReceipt(
            @Valid @RequestBody RetentionReceiptReqVO reqVO) {
        return success(RetentionReceiptRespVO.of(retentionService.createDccEvidenceReceipt(
                reqVO.toCommand("DCC_SIGNATURE"))));
    }

    @PostMapping("/retention/edhr-archive-receipts")
    @Operation(summary = "记录 eDHR 归档 WORM 留存回执")
    @PreAuthorize("@ss.hasPermission('signature-governance:retention:manage')")
    public CommonResult<RetentionReceiptRespVO> createEdhrArchiveReceipt(
            @Valid @RequestBody RetentionReceiptReqVO reqVO) {
        return success(RetentionReceiptRespVO.of(retentionService.createEdhrArchiveReceipt(
                reqVO.toCommand("EDHR_ARCHIVE"))));
    }

    @PostMapping("/retention/recovery-rehearsals")
    @Operation(summary = "记录签名证据归档恢复演练结果")
    @PreAuthorize("@ss.hasPermission('signature-governance:retention:manage')")
    public CommonResult<RecoveryRehearsalRespVO> runRecoveryRehearsal(
            @Valid @RequestBody RecoveryRehearsalReqVO reqVO) {
        return success(RecoveryRehearsalRespVO.of(retentionService.runRecoveryRehearsal(reqVO.toCommand())));
    }

    @PostMapping("/periodic-review/batches")
    @Operation(summary = "创建电子签名周期审阅批次")
    @PreAuthorize("@ss.hasPermission('signature-governance:periodic-review:manage')")
    public CommonResult<ReviewBatchRespVO> createReviewBatch(
            @Valid @RequestBody ReviewBatchCreateReqVO reqVO) {
        return success(ReviewBatchRespVO.of(reviewService.createBatch(reqVO.toCommand())));
    }

    @PostMapping("/csv/packages/{releaseId}/release-gate")
    @Operation(summary = "评估电子签名 CSV 质量包发布门禁")
    @PreAuthorize("@ss.hasPermission('signature-governance:csv-package:manage')")
    public CommonResult<CsvReleaseGateRespVO> evaluateCsvReleaseGate(
            @PathVariable("releaseId") String releaseId,
            @Valid @RequestBody CsvReleaseGateReqVO reqVO) {
        return success(CsvReleaseGateRespVO.of(csvService.evaluateReleaseGate(reqVO.toCommand(releaseId))));
    }

    @GetMapping("/policies/current")
    @Operation(summary = "获取当前电子签名统一策略状态")
    @PreAuthorize("@ss.hasPermission('signature-governance:policy:query')")
    public CommonResult<PolicyCurrentRespVO> getCurrentPolicy() {
        List<SignatureGovernancePolicyOverview> overviews = Arrays.stream(SignatureGovernanceModuleCode.values())
                .map(policyService::describeModule)
                .toList();
        return success(PolicyCurrentRespVO.of(overviews));
    }

    @GetMapping("/portal/overview")
    @Operation(summary = "获取统一电子签名页签总览")
    @PreAuthorize("@ss.hasPermission('signature-governance:policy:query')")
    public CommonResult<PortalOverviewRespVO> getPortalOverview() {
        return success(PortalOverviewRespVO.of(portalService.getOverview(getLoginUserId())));
    }

    @GetMapping("/signature-records/page")
    @Operation(summary = "获取统一电子签名记录分页")
    @PreAuthorize("@ss.hasPermission('signature-governance:policy:query')")
    public CommonResult<PageResult<SignatureGovernanceRecordRespVO>> getSignatureRecordPage(
            @Valid SignatureGovernanceRecordPageReqVO reqVO) {
        return success(signatureGovernanceRecordService.getPage(reqVO));
    }

    @GetMapping("/my-signature-records/page")
    @Operation(summary = "获取当前登录人的电子签名记录分页")
    @PreAuthorize("@ss.hasPermission('signature-governance:policy:query')")
    public CommonResult<PageResult<SignatureGovernanceRecordRespVO>> getMySignatureRecordPage(
            @Valid SignatureGovernanceRecordPageReqVO reqVO) {
        reqVO.setSignerUserId(getLoginUserId());
        return success(signatureGovernanceRecordService.getPage(reqVO));
    }

    @GetMapping("/signature-records/{globalId}/pdf")
    @Operation(summary = "导出统一电子签名记录 PDF")
    @PreAuthorize("@ss.hasPermission('signature-governance:policy:query')")
    public ResponseEntity<byte[]> exportSignatureRecordPdf(@PathVariable("globalId") String globalId) {
        SignatureGovernanceRecordPdfArtifact artifact = signatureGovernanceRecordService.exportRecordPdf(globalId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(artifact.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(artifact.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(artifact.content());
    }

    @Data
    public static class RetentionPrecheckReqVO {

        @NotBlank
        private String endpoint;
        @NotBlank
        private String bucketName;
        private Boolean objectLockEnabled;
        private Boolean versioningEnabled;
        private Boolean defaultRetentionEnabled;
        private String retentionMode;
        private Boolean permissionsVerified;
        private Long ownerUserId;
        private Long sampleDccSignatureId;
        private Long sampleEdhrArchiveId;

        private SignatureGovernanceRetentionPrecheckCommand toCommand() {
            return new SignatureGovernanceRetentionPrecheckCommand(endpoint, bucketName,
                    Boolean.TRUE.equals(objectLockEnabled), Boolean.TRUE.equals(versioningEnabled),
                    Boolean.TRUE.equals(defaultRetentionEnabled), retentionMode,
                    Boolean.TRUE.equals(permissionsVerified), ownerUserId, sampleDccSignatureId,
                    sampleEdhrArchiveId);
        }
    }

    @Data
    public static class RetentionPrecheckRespVO {

        private String status;
        private Boolean ready;
        private String receiptId;
        private List<BlockerRespVO> blockers;

        private static RetentionPrecheckRespVO of(SignatureGovernanceRetentionPrecheckResult result) {
            RetentionPrecheckRespVO respVO = new RetentionPrecheckRespVO();
            respVO.setStatus(result.getStatus().name());
            respVO.setReady(result.isReady());
            respVO.setReceiptId(result.getReceiptId().orElse(null));
            respVO.setBlockers(result.getBlockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.getCode().name(), blocker.getMessage(),
                            blocker.getImpact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class RetentionReceiptReqVO {

        private Long sourceId;
        private String objectKey;
        private String versionId;
        private String retentionMode;
        private Instant retainUntil;
        private String sha256;
        private String evidenceHash;
        private String archiveSha256;
        private String signatureHash;
        private String auditEventId;

        private SignatureGovernanceRetentionReceiptCommand toCommand(String sourceType) {
            return new SignatureGovernanceRetentionReceiptCommand(sourceType, sourceId, objectKey, versionId,
                    retentionMode, retainUntil, sha256, evidenceHash, archiveSha256, signatureHash, auditEventId);
        }
    }

    @Data
    public static class RetentionReceiptRespVO {

        private String status;
        private Boolean recorded;
        private String receiptId;
        private List<BlockerRespVO> blockers;

        private static RetentionReceiptRespVO of(SignatureGovernanceRetentionReceiptResult result) {
            RetentionReceiptRespVO respVO = new RetentionReceiptRespVO();
            respVO.setStatus(result.getStatus().name());
            respVO.setRecorded(result.isRecorded());
            respVO.setReceiptId(result.getReceiptId().orElse(null));
            respVO.setBlockers(result.getBlockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.getCode().name(), blocker.getMessage(),
                            blocker.getImpact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class RecoveryRehearsalReqVO {

        private String backupId;
        private String recoveryRuntime;
        private Boolean ownerReviewed;
        private Boolean reportWritten;
        private Boolean auditWritten;
        private List<RecoverySampleReqVO> samples;

        private SignatureGovernanceRecoveryRehearsalCommand toCommand() {
            return new SignatureGovernanceRecoveryRehearsalCommand(backupId, recoveryRuntime,
                    Boolean.TRUE.equals(ownerReviewed), Boolean.TRUE.equals(reportWritten),
                    Boolean.TRUE.equals(auditWritten),
                    samples == null ? List.of() : samples.stream().map(RecoverySampleReqVO::toSample).toList());
        }
    }

    @Data
    public static class RecoverySampleReqVO {

        private String sampleType;
        private String objectKey;
        private String versionId;
        private String expectedSha256;
        private String restoredSha256;
        private String expectedDomainHash;
        private String restoredDomainHash;

        private SignatureGovernanceRecoverySample toSample() {
            return new SignatureGovernanceRecoverySample(SignatureGovernanceRecoverySampleType.valueOf(sampleType),
                    objectKey, versionId, expectedSha256, restoredSha256, expectedDomainHash, restoredDomainHash);
        }
    }

    @Data
    public static class RecoveryRehearsalRespVO {

        private String status;
        private Boolean passed;
        private List<BlockerRespVO> blockers;

        private static RecoveryRehearsalRespVO of(SignatureGovernanceRecoveryRehearsalResult result) {
            RecoveryRehearsalRespVO respVO = new RecoveryRehearsalRespVO();
            respVO.setStatus(result.getStatus().name());
            respVO.setPassed(result.isPassed());
            respVO.setBlockers(result.getBlockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.getCode().name(), blocker.getMessage(),
                            blocker.getImpact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class ReviewBatchCreateReqVO {

        private String reviewOwner;
        private String periodCode;
        private String ruleVersion;
        private LocalDate dueDate;
        private String reason;
        private List<String> scopeModules;
        private List<String> permittedModules;
        private List<ReviewProjectionReqVO> projections;
        private Boolean reviewSignatureStrategyConfigured;

        private SignatureGovernanceReviewBatchCommand toCommand() {
            return new SignatureGovernanceReviewBatchCommand(reviewOwner, periodCode, ruleVersion, dueDate,
                    reason, toModules(scopeModules), toModules(permittedModules),
                    projections == null ? List.of() : projections.stream().map(ReviewProjectionReqVO::toProjection)
                            .toList(),
                    Boolean.TRUE.equals(reviewSignatureStrategyConfigured));
        }
    }

    @Data
    public static class ReviewProjectionReqVO {

        private String moduleCode;
        private String sourceTable;
        private String sourceId;
        private String sourceHash;
        private String actionCode;
        private String meaningCode;
        private String findingCode;

        private SignatureGovernanceReviewSourceProjection toProjection() {
            return new SignatureGovernanceReviewSourceProjection(toModule(moduleCode), sourceTable, sourceId,
                    sourceHash, actionCode, meaningCode, SignatureGovernanceReviewFindingCode.valueOf(findingCode));
        }
    }

    @Data
    public static class ReviewBatchRespVO {

        private String status;
        private Boolean collectable;
        private String batchId;
        private String snapshotHash;
        private List<BlockerRespVO> blockers;
        private List<ReviewSnapshotItemRespVO> snapshotItems;

        private static ReviewBatchRespVO of(SignatureGovernanceReviewBatchEvaluation result) {
            ReviewBatchRespVO respVO = new ReviewBatchRespVO();
            respVO.setStatus(result.status().name());
            respVO.setCollectable(result.collectable());
            respVO.setBatchId(result.batchId());
            respVO.setSnapshotHash(result.snapshotHash());
            respVO.setBlockers(result.blockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.code().name(), blocker.message(), blocker.impact()))
                    .toList());
            respVO.setSnapshotItems(result.snapshotItems().stream()
                    .map(item -> new ReviewSnapshotItemRespVO(item.moduleCode().name(), item.sourceRef(),
                            item.findingCode().name()))
                    .toList());
            return respVO;
        }
    }

    public record ReviewSnapshotItemRespVO(String moduleCode, String sourceRef, String findingCode) {
    }

    @Data
    public static class CsvReleaseGateReqVO {

        private String qualityOwner;
        private List<CsvMaterialReqVO> materials;
        private List<CsvTraceRelationReqVO> traceRelations;
        private List<CsvTrainingRecordReqVO> trainingRecords;
        private List<CsvChangeControlReqVO> changeControls;
        private CsvQaApprovalReqVO qaApproval;
        private String recoveryEvidenceRef;
        private Boolean engineeringVerificationPassed;

        private SignatureGovernanceCsvReleaseGateCommand toCommand(String releaseId) {
            SignatureGovernanceCsvPackageCommand packageCommand = new SignatureGovernanceCsvPackageCommand(
                    releaseId,
                    qualityOwner,
                    materials == null ? List.of() : materials.stream().map(CsvMaterialReqVO::toMaterial).toList(),
                    traceRelations == null ? List.of()
                            : traceRelations.stream().map(CsvTraceRelationReqVO::toTraceRelation).toList(),
                    trainingRecords == null ? List.of()
                            : trainingRecords.stream().map(CsvTrainingRecordReqVO::toTrainingRecord).toList(),
                    changeControls == null ? List.of()
                            : changeControls.stream().map(CsvChangeControlReqVO::toChangeControl).toList(),
                    qaApproval == null ? null : qaApproval.toQaApproval(),
                    recoveryEvidenceRef,
                    Boolean.TRUE.equals(engineeringVerificationPassed));
            return new SignatureGovernanceCsvReleaseGateCommand(releaseId, packageCommand);
        }
    }

    @Data
    public static class CsvMaterialReqVO {

        private String type;
        private String documentId;
        private String version;
        private String status;
        private String owner;
        private List<String> reviewers;
        private List<String> approvers;
        private String sourceEvidence;
        private String changeControlId;
        private String signatureMeaning;

        private SignatureGovernanceCsvMaterial toMaterial() {
            return new SignatureGovernanceCsvMaterial(SignatureGovernanceCsvMaterialType.valueOf(type),
                    documentId, version, SignatureGovernanceCsvMaterialStatus.valueOf(status), owner,
                    reviewers == null ? List.of() : reviewers,
                    approvers == null ? List.of() : approvers,
                    sourceEvidence, changeControlId, signatureMeaning);
        }
    }

    @Data
    public static class CsvTraceRelationReqVO {

        private String requirementRef;
        private String designRef;
        private String testRef;
        private String evidenceRef;
        private String owner;
        private String status;
        private String blockerRef;
        private String qualityApprovalRef;

        private SignatureGovernanceCsvTraceRelation toTraceRelation() {
            return new SignatureGovernanceCsvTraceRelation(requirementRef, designRef, testRef, evidenceRef,
                    owner, SignatureGovernanceCsvMaterialStatus.valueOf(status), blockerRef, qualityApprovalRef);
        }
    }

    @Data
    public static class CsvTrainingRecordReqVO {

        private String trainingId;
        private String userId;
        private String sopDocumentId;
        private String evidenceRef;
        private Boolean effective;

        private SignatureGovernanceCsvTrainingRecord toTrainingRecord() {
            return new SignatureGovernanceCsvTrainingRecord(trainingId, userId, sopDocumentId, evidenceRef,
                    Boolean.TRUE.equals(effective));
        }
    }

    @Data
    public static class CsvChangeControlReqVO {

        private String changeControlId;
        private String status;
        private String evidenceRef;

        private SignatureGovernanceCsvChangeControl toChangeControl() {
            return new SignatureGovernanceCsvChangeControl(changeControlId,
                    SignatureGovernanceCsvMaterialStatus.valueOf(status), evidenceRef);
        }
    }

    @Data
    public static class CsvQaApprovalReqVO {

        private String approvalRef;
        private String approver;
        private String status;
        private String signatureEvidenceRef;

        private SignatureGovernanceCsvQaApproval toQaApproval() {
            return new SignatureGovernanceCsvQaApproval(approvalRef, approver,
                    SignatureGovernanceCsvMaterialStatus.valueOf(status), signatureEvidenceRef);
        }
    }

    @Data
    public static class CsvReleaseGateRespVO {

        private String status;
        private Boolean engineeringVerificationPassed;
        private Boolean qaApproved;
        private List<BlockerRespVO> blockers;

        private static CsvReleaseGateRespVO of(SignatureGovernanceCsvReleaseGateResult result) {
            CsvReleaseGateRespVO respVO = new CsvReleaseGateRespVO();
            respVO.setStatus(result.status().name());
            respVO.setEngineeringVerificationPassed(result.engineeringVerificationPassed());
            respVO.setQaApproved(result.qaApproved());
            respVO.setBlockers(result.blockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.code().name(), blocker.message(), blocker.impact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class PolicyCurrentRespVO {

        private String status;
        private Boolean ready;
        private List<String> modules;
        private List<PolicyModuleRespVO> moduleStatuses;
        private List<BlockerRespVO> blockers;

        private static PolicyCurrentRespVO of(List<SignatureGovernancePolicyOverview> overviews) {
            PolicyCurrentRespVO respVO = new PolicyCurrentRespVO();
            boolean ready = overviews.stream().allMatch(overview -> overview.blockers().isEmpty());
            respVO.setStatus(ready ? "READY" : "BLOCKED");
            respVO.setReady(ready);
            respVO.setModules(overviews.stream().map(overview -> overview.moduleCode().name()).toList());
            respVO.setModuleStatuses(overviews.stream().map(PolicyModuleRespVO::of).toList());
            respVO.setBlockers(overviews.stream()
                    .flatMap(overview -> overview.blockers().stream())
                    .map(blocker -> BlockerRespVO.of(blocker.code().name(), blocker.message(), blocker.impact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class PolicyModuleRespVO {

        private String moduleCode;
        private Boolean policySourcePresent;
        private Boolean authorityConfirmed;
        private Boolean adapterRegistered;
        private String policyVersion;
        private String policySourceCode;
        private String adapterCode;
        private String adapterVersion;
        private String evidenceSchemaVersion;
        private List<BlockerRespVO> blockers;

        private static PolicyModuleRespVO of(SignatureGovernancePolicyOverview overview) {
            PolicyModuleRespVO respVO = new PolicyModuleRespVO();
            respVO.setModuleCode(overview.moduleCode().name());
            respVO.setPolicySourcePresent(overview.policySourcePresent());
            respVO.setAuthorityConfirmed(overview.authorityConfirmed());
            respVO.setAdapterRegistered(overview.adapterRegistered());
            respVO.setPolicyVersion(overview.policyVersion());
            respVO.setPolicySourceCode(overview.policySourceCode());
            respVO.setAdapterCode(overview.adapterCode());
            respVO.setAdapterVersion(overview.adapterVersion());
            respVO.setEvidenceSchemaVersion(overview.evidenceSchemaVersion());
            respVO.setBlockers(overview.blockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.code().name(), blocker.message(), blocker.impact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class PortalOverviewRespVO {

        private String status;
        private Boolean ready;
        private PortalAuthorizationRespVO authorization;
        private PortalSummaryRespVO summary;
        private List<PortalModuleRespVO> modules;
        private List<BlockerRespVO> blockers;

        private static PortalOverviewRespVO of(SignatureGovernancePortalOverview overview) {
            PortalOverviewRespVO respVO = new PortalOverviewRespVO();
            respVO.setStatus(overview.status());
            respVO.setReady(overview.ready());
            respVO.setAuthorization(PortalAuthorizationRespVO.of(overview.authorization()));
            respVO.setSummary(PortalSummaryRespVO.of(overview.summary()));
            respVO.setModules(overview.modules().stream().map(PortalModuleRespVO::of).toList());
            respVO.setBlockers(overview.blockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.code(), blocker.message(), blocker.impact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class PortalAuthorizationRespVO {

        private String status;
        private Boolean enabled;
        private List<BlockerRespVO> blockers;

        private static PortalAuthorizationRespVO of(SignatureGovernancePortalAuthorizationOverview overview) {
            PortalAuthorizationRespVO respVO = new PortalAuthorizationRespVO();
            respVO.setStatus(overview.status());
            respVO.setEnabled(overview.enabled());
            respVO.setBlockers(overview.blockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.code(), blocker.message(), blocker.impact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class PortalSummaryRespVO {

        private Long moduleTotal;
        private Long readyModuleTotal;
        private Long blockedModuleTotal;
        private Long pendingTotal;
        private Long signatureTotal;

        private static PortalSummaryRespVO of(SignatureGovernancePortalSummary summary) {
            PortalSummaryRespVO respVO = new PortalSummaryRespVO();
            respVO.setModuleTotal(summary.moduleTotal());
            respVO.setReadyModuleTotal(summary.readyModuleTotal());
            respVO.setBlockedModuleTotal(summary.blockedModuleTotal());
            respVO.setPendingTotal(summary.pendingTotal());
            respVO.setSignatureTotal(summary.signatureTotal());
            return respVO;
        }
    }

    @Data
    public static class PortalModuleRespVO {

        private String moduleCode;
        private String moduleName;
        private String moduleDescription;
        private String status;
        private Boolean ready;
        private PortalAuthorizationRespVO authorization;
        private PolicyModuleRespVO policy;
        private PortalMetricsRespVO metrics;
        private PortalRouteRespVO routes;
        private List<BlockerRespVO> blockers;

        private static PortalModuleRespVO of(SignatureGovernancePortalModuleOverview overview) {
            PortalModuleRespVO respVO = new PortalModuleRespVO();
            respVO.setModuleCode(overview.moduleCode().name());
            respVO.setModuleName(overview.moduleName());
            respVO.setModuleDescription(overview.moduleDescription());
            respVO.setStatus(overview.status());
            respVO.setReady(overview.ready());
            respVO.setAuthorization(PortalAuthorizationRespVO.of(overview.authorization()));
            respVO.setPolicy(PolicyModuleRespVO.of(overview.policy()));
            respVO.setMetrics(PortalMetricsRespVO.of(overview.metrics()));
            respVO.setRoutes(PortalRouteRespVO.of(overview.routes()));
            respVO.setBlockers(overview.blockers().stream()
                    .map(blocker -> BlockerRespVO.of(blocker.code(), blocker.message(), blocker.impact()))
                    .toList());
            return respVO;
        }
    }

    @Data
    public static class PortalMetricsRespVO {

        private Long pendingCount;
        private Long signatureCount;

        private static PortalMetricsRespVO of(SignatureGovernancePortalMetrics metrics) {
            PortalMetricsRespVO respVO = new PortalMetricsRespVO();
            respVO.setPendingCount(metrics.pendingCount());
            respVO.setSignatureCount(metrics.signatureCount());
            return respVO;
        }
    }

    @Data
    public static class PortalRouteRespVO {

        private String primaryLabel;
        private String primaryPath;
        private String secondaryLabel;
        private String secondaryPath;

        private static PortalRouteRespVO of(SignatureGovernancePortalRouteOverview routes) {
            PortalRouteRespVO respVO = new PortalRouteRespVO();
            respVO.setPrimaryLabel(routes.primaryLabel());
            respVO.setPrimaryPath(routes.primaryPath());
            respVO.setSecondaryLabel(routes.secondaryLabel());
            respVO.setSecondaryPath(routes.secondaryPath());
            return respVO;
        }
    }

    @Data
    public static class BlockerRespVO {

        private String code;
        private String message;
        private String impact;

        private static BlockerRespVO of(String code, String message, String impact) {
            BlockerRespVO respVO = new BlockerRespVO();
            respVO.setCode(code);
            respVO.setMessage(message);
            respVO.setImpact(impact);
            return respVO;
        }
    }

    private static Set<SignatureGovernanceModuleCode> toModules(List<String> moduleCodes) {
        if (moduleCodes == null || moduleCodes.isEmpty()) {
            return Set.of();
        }
        return moduleCodes.stream().map(SignatureGovernanceController::toModule).collect(java.util.stream.Collectors.toSet());
    }

    private static SignatureGovernanceModuleCode toModule(String moduleCode) {
        return SignatureGovernanceModuleCode.valueOf(moduleCode);
    }
}
