package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchivePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignatureTimeReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordApprovalSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordApprovalSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_CHECKSUM_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_CHAIN_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_METADATA_INCOMPLETE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_RENDER_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_SOURCE_CHANGED_REGENERATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_TYPE_UNSUPPORTED;

@Service
@Slf4j
public class MesProBatchRecordExecutionArchiveServiceImpl implements MesProBatchRecordExecutionArchiveService {

    private static final String ARTIFACT_TYPE_PDF = "PDF";
    private static final String ARTIFACT_TYPE_EXCEL = "EXCEL";
    private static final String ARCHIVE_STATUS_GENERATING = "GENERATING";
    private static final String ARCHIVE_STATUS_SEALED = "SEALED";
    private static final String ARCHIVE_STATUS_FAILED = "FAILED";
    private static final int EXECUTION_STATUS_APPROVED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_APPROVED;
    private static final int EXECUTION_STATUS_FILL_COMPLETED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED;
    private static final String EVENT_GENERATE_START = "GENERATE_START";
    private static final String EVENT_GENERATE_SUCCESS = "GENERATE_SUCCESS";
    private static final String EVENT_GENERATE_FAILED = "GENERATE_FAILED";
    private static final String EVENT_ARCHIVE_SEAL = "ARCHIVE_SEAL";
    private static final String EVENT_DOWNLOAD_SUCCESS = "DOWNLOAD_SUCCESS";
    private static final String EVENT_DOWNLOAD_FAILED = "DOWNLOAD_FAILED";
    private static final String ARCHIVE_DIRECTORY = "mes/edhr/archive";
    private static final String METADATA_STORAGE_RETENTION = "storageRetention";
    private static final String STORAGE_LEGAL_HOLD_ON = "ON";
    private static final String DOMAIN_TRACE_STATUS_VERIFIED = "VERIFIED";
    private static final DateTimeFormatter ARCHIVE_CODE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private List<MesProBatchRecordExecutionArchiveRenderer> renderers;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProBatchRecordApprovalSnapshotMapper approvalSnapshotMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveEventMapper archiveEventMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private FileService fileService;
    @Resource
    private MesEdhrArchiveProtectedStorage protectedStorage;
    @Resource
    private MesProBatchRecordExecutionSignatureService signatureService;
    @Resource
    private MesProBatchRecordDomainTraceService domainTraceService;
    @Resource
    private MesProBatchRecordExecutionAttachmentService attachmentService;
    @Resource
    private MesProEdhrOperationAuditService operationAuditService;
    @Resource
    private MesProEdhrPermissionGateService permissionGateService;

    @Override
    public MesProBatchRecordExecutionArchiveRespVO generateExecutionArchive(
            MesProBatchRecordExecutionArchiveGenerateReqVO reqVO) {
        String artifactType = normalizeArtifactType(reqVO.getArtifactType());
        MesProBatchRecordExecutionDO execution = validateClosedExecution(reqVO.getExecutionId());
        requireExecutionArchiveAbility(execution, "ARCHIVE",
                "mes:pro-batch-record-execution-archive:create", "生成电子批记录归档");
        SourceData sourceData = buildSourceData(execution);
        Long actorId = SecurityFrameworkUtils.getLoginUserId();

        MesProBatchRecordExecutionArchiveDO sameSourceArchive = archiveMapper.selectSealedBySourceHashes(
                execution.getId(), artifactType, sourceData.executionSnapshotHash,
                sourceData.cellValuesHash, sourceData.signatureHash, sourceData.approvalSnapshotHash);
        if (sameSourceArchive != null && !Boolean.TRUE.equals(reqVO.getRegenerate())) {
            requireExistingArchiveStorageRetentionEvidence(sameSourceArchive, actorId);
            MesProBatchRecordExecutionArchiveRespVO result = toRespVO(sameSourceArchive, Boolean.FALSE);
            recordOperationAudit("ARCHIVE", "复用电子批记录归档", sameSourceArchive, "SUCCESS",
                    sameSourceArchive.getSha256(), sameSourceArchive.getSha256(), null);
            return result;
        }

        MesProBatchRecordExecutionArchiveDO latestArchive = archiveMapper.selectLatestByExecutionAndType(
                execution.getId(), artifactType);
        if (latestArchive != null && !Boolean.TRUE.equals(reqVO.getRegenerate())) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SOURCE_CHANGED_REGENERATE_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        Integer nextVersion = latestArchive == null ? 1 : latestArchive.getArchiveVersion() + 1;
        MesProBatchRecordExecutionArchiveDO archive = createGeneratingArchive(execution, artifactType, sourceData,
                actorId, now, nextVersion, reqVO.getComment());
        recordEvent(archive.getId(), execution.getId(), EVENT_GENERATE_START, actorId,
                "start archive generation: artifactType=" + artifactType + ", version=" + nextVersion, null);

        MesProBatchRecordExecutionArchiveRenderer renderer = requireRendererWithFailureEvent(archive, actorId, artifactType);
        MesProBatchRecordExecutionArchiveRenderResult renderResult = renderArchive(archive, renderer, sourceData, actorId, now);
        StorageRetentionEvidence retentionEvidence = persistArchiveFile(archive, actorId, renderResult);
        requireStorageRetentionEvidenceGate(archive, actorId, retentionEvidence);
        Long fileId = retentionEvidence.getFileId();
        Long sealSignatureId;
        try {
            sealSignatureId = recordSealSignature(archive, actorId, reqVO.getSealPassword(), reqVO.getComment(),
                    reqVO.getSignatureTime());
        } catch (RuntimeException ex) {
            deleteCreatedArchiveFile(fileId, ex);
            throw ex;
        }

        archive.setArchiveStatus(ARCHIVE_STATUS_SEALED)
                .setFileId(fileId)
                .setFileName(renderResult.getFileName())
                .setContentType(renderResult.getContentType())
                .setFileSize(renderResult.getFileSize())
                .setSha256(renderResult.getSha256())
                .setRenderSourceVersion(renderResult.getRenderSourceVersion())
                .setApprovalSnapshotId(sourceData.approvalSnapshotId)
                .setApprovalSnapshotHash(sourceData.approvalSnapshotHash)
                .setSealSignatureId(sealSignatureId)
                .setSealedBy(actorId)
                .setSealedAt(LocalDateTime.now())
                .setFailureReason(null);
        archiveMapper.updateById(archive);
        String storageRetentionMetadata = storageRetentionMetadataJson(retentionEvidence, renderResult.getSha256());
        if (StrUtil.isBlank(storageRetentionMetadata)) {
            markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }
        recordEvent(archive.getId(), execution.getId(), EVENT_GENERATE_SUCCESS, actorId,
                "archive generated: fileId=" + fileId + ", sha256=" + renderResult.getSha256(),
                storageRetentionMetadata);
        recordEvent(archive.getId(), execution.getId(), EVENT_ARCHIVE_SEAL, actorId,
                "archive sealed: sealSignatureId=" + sealSignatureId, null);
        MesProBatchRecordExecutionArchiveRespVO result = toRespVO(archive, Boolean.TRUE);
        recordOperationAudit("ARCHIVE", "生成电子批记录归档", archive, "SUCCESS",
                sourceData.executionSnapshotHash, renderResult.getSha256(), storageRetentionMetadata);
        return result;
    }

    @Override
    public PageResult<MesProBatchRecordExecutionArchiveRespVO> getExecutionArchivePage(
            MesProBatchRecordExecutionArchivePageReqVO pageReqVO) {
        LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO> wrapper =
                new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                        .eqIfPresent(MesProBatchRecordExecutionArchiveDO::getExecutionId, pageReqVO.getExecutionId())
                        .eqIfPresent(MesProBatchRecordExecutionArchiveDO::getArtifactType,
                                normalizeOptionalArtifactType(pageReqVO.getArtifactType()))
                        .eqIfPresent(MesProBatchRecordExecutionArchiveDO::getArchiveStatus, pageReqVO.getArchiveStatus())
                        .betweenIfPresent(MesProBatchRecordExecutionArchiveDO::getGeneratedAt,
                                pageReqVO.getGeneratedTimeStart(), pageReqVO.getGeneratedTimeEnd())
                        .orderByDesc(MesProBatchRecordExecutionArchiveDO::getArchiveVersion)
                        .orderByDesc(MesProBatchRecordExecutionArchiveDO::getId);
        List<Long> executionIds = selectExecutionIdsForPageFilter(pageReqVO);
        PageResult<MesProBatchRecordExecutionArchiveRespVO> result;
        if (executionIds != null && executionIds.isEmpty()) {
            result = new PageResult<>(Collections.emptyList(), 0L);
        } else {
            if (executionIds != null) {
                wrapper.in(MesProBatchRecordExecutionArchiveDO::getExecutionId, executionIds);
            }
            PageResult<MesProBatchRecordExecutionArchiveDO> pageResult = archiveMapper.selectPage(pageReqVO, wrapper);
            result = new PageResult<>(pageResult.getList().stream().map(archive -> toRespVO(archive, null)).toList(),
                    pageResult.getTotal());
        }
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + java.util.UUID.randomUUID())
                .setObjectType("EXECUTION_ARCHIVE_PAGE")
                .setObjectId("LIST")
                .setExecutionId(pageReqVO.getExecutionId())
                .setOperationType("QUERY")
                .setActionName("查询电子批记录归档列表")
                .setActorUserId(currentAuditUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode("mes:pro-batch-record-execution-archive:query")
                .setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS"));
        return result;
    }

    @Override
    public MesProBatchRecordExecutionArchiveRespVO getLatestExecutionArchive(Long executionId, String artifactType) {
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectLatestByExecutionAndType(
                executionId, normalizeArtifactType(artifactType));
        if (archive == null) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_NOT_EXISTS);
        }
        MesProBatchRecordExecutionArchiveRespVO result = toRespVO(archive, Boolean.FALSE);
        recordOperationAudit("VIEW", "查看最新电子批记录归档", archive, "SUCCESS",
                archive.getExecutionSnapshotHash(), archive.getSha256(), null);
        return result;
    }

    @Override
    public MesProBatchRecordExecutionArchiveDownloadRespVO downloadExecutionArchive(Long id) {
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectById(id);
        if (archive == null || !ARCHIVE_STATUS_SEALED.equals(archive.getArchiveStatus())) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_NOT_EXISTS);
        }
        Long actorId = SecurityFrameworkUtils.getLoginUserId();
        requireArchiveObjectAbility(archive, "VIEW",
                "mes:pro-batch-record-execution-archive:download", "下载电子批记录归档");
        StorageRetentionMetadata metadata = requireDownloadStorageRetentionEvidence(archive, actorId);
        byte[] content;
        try {
            content = fileService.getFileContentWithStorageRetention(archive.getFileId(),
                    storageRetentionPolicy(metadata));
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            recordDownloadFailure(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }
        if (content == null || !Objects.equals(archive.getSha256(), sha256(content))) {
            recordDownloadFailure(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_CHECKSUM_MISMATCH);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_CHECKSUM_MISMATCH);
        }
        recordEvent(archive.getId(), archive.getExecutionId(), EVENT_DOWNLOAD_SUCCESS, actorId,
                "archive downloaded: fileSize=" + content.length, null);
        MesProBatchRecordExecutionArchiveDownloadRespVO respVO = new MesProBatchRecordExecutionArchiveDownloadRespVO();
        respVO.setFileName(archive.getFileName());
        respVO.setContentType(archive.getContentType());
        respVO.setFileSize(archive.getFileSize());
        respVO.setSha256(archive.getSha256());
        respVO.setApprovalSnapshotId(archive.getApprovalSnapshotId());
        respVO.setApprovalSnapshotHash(archive.getApprovalSnapshotHash());
        respVO.setContent(content);
        recordOperationAudit("DOWNLOAD", "下载电子批记录归档", archive, "SUCCESS",
                archive.getSha256(), archive.getSha256(), null);
        return respVO;
    }

    private MesProBatchRecordExecutionDO validateClosedExecution(Long executionId) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_EXISTS);
        }
        if (isFillCompletedOrdinaryExecution(execution)) {
            return execution;
        }
        if (!Objects.equals(execution.getStatus(), EXECUTION_STATUS_APPROVED) || execution.getApprovedAt() == null
                || execution.getClosedAt() == null || StrUtil.isBlank(execution.getProcessInstanceId())) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
        }
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(executionId);
        if (snapshot == null || !"APPROVED".equals(snapshot.getApprovalStatus())
                || StrUtil.isBlank(snapshot.getProcessInstanceId())
                || !Objects.equals(snapshot.getProcessInstanceId(), execution.getProcessInstanceId())
                || snapshot.getClosedAt() == null || snapshot.getApprovedAt() == null) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
        }
        return execution;
    }

    private boolean isFillCompletedOrdinaryExecution(MesProBatchRecordExecutionDO execution) {
        return Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED)
                && execution.getSubmittedAt() != null
                && execution.getClosedAt() != null;
    }

    private void requireExecutionArchiveAbility(MesProBatchRecordExecutionDO execution, String ability,
                                                String permissionCode, String actionName) {
        permissionGateService.requireAbility(new MesProEdhrPermissionGateCommand()
                .setScopeId(execution.getPermissionScopeId())
                .setObjectType("BATCH_RECORD_EXECUTION")
                .setObjectId(String.valueOf(execution.getId()))
                .setAbility(ability)
                .setExecutionId(execution.getId())
                .setRouteProcessId(execution.getRouteProcessId())
                .setReportId(execution.getBatchRecordReportId())
                .setRecordCategory(execution.getRecordCategory())
                .setPermissionCode(permissionCode)
                .setActionName(actionName));
    }

    private void requireArchiveObjectAbility(MesProBatchRecordExecutionArchiveDO archive, String ability,
                                             String permissionCode, String actionName) {
        permissionGateService.requireAbility(new MesProEdhrPermissionGateCommand()
                .setObjectType("EXECUTION_ARCHIVE")
                .setObjectId(String.valueOf(archive.getId()))
                .setAbility(ability)
                .setExecutionId(archive.getExecutionId())
                .setPermissionCode(permissionCode)
                .setActionName(actionName));
    }

    private SourceData buildSourceData(MesProBatchRecordExecutionDO execution) {
        if (StrUtil.isBlank(execution.getExecutionSnapshotJson()) || StrUtil.isBlank(execution.getCellValuesJson())) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_MISSING);
        }
        JSONObject executionSnapshot;
        Object cellValues;
        try {
            executionSnapshot = JSON.parseObject(execution.getExecutionSnapshotJson());
            cellValues = JSON.parse(execution.getCellValuesJson());
        } catch (Exception ex) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID);
        }
        if (executionSnapshot == null || executionSnapshot.isEmpty() || cellValues == null) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID);
        }
        List<MesProBatchRecordExecutionSignatureDO> signatures = signatureMapper.selectList(
                new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                        .eq(MesProBatchRecordExecutionSignatureDO::getExecutionId, execution.getId())
                        .orderByDesc(MesProBatchRecordExecutionSignatureDO::getId));
        if (CollUtil.isEmpty(signatures)) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID);
        }
        boolean fillCompletedOrdinary = isFillCompletedOrdinaryExecution(execution);
        boolean hasSubmitSignature = signatures.stream()
                .anyMatch(signature -> "SUBMIT".equals(signature.getActionType())
                        && (fillCompletedOrdinary
                        || Objects.equals(signature.getProcessInstanceId(), execution.getProcessInstanceId())));
        if (!hasSubmitSignature) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
        }
        MesProBatchRecordApprovalSnapshotDO approvalSnapshot = approvalSnapshotMapper.selectByExecutionId(execution.getId());
        if (fillCompletedOrdinary) {
            MesProBatchRecordExecutionAttachmentChainVerifyResult attachmentChain =
                    attachmentService.verifyAttachmentChain(execution.getId());
            if (attachmentChain == null || !attachmentChain.isValid()) {
                throw exception(PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_CHAIN_INVALID);
            }
            List<MesProBatchRecordExecutionAttachmentDO> attachments =
                    attachmentMapper.selectListByExecutionId(execution.getId());
            if (!MesProBatchRecordAttachmentRuleSupport.collectMissingRequiredAttachments(
                    execution.getExecutionSnapshotJson(), attachments).isEmpty()) {
                throw exception(PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_METADATA_INCOMPLETE);
            }
            validateActiveAttachmentArchiveMetadata(attachments);
            return new SourceData(execution, executionSnapshot, cellValues, signatures,
                    sourceHashFromJson(execution.getExecutionSnapshotJson()),
                    execution.getCellValuesHash(),
                    execution.getFieldAuditRevision(),
                    execution.getFieldAuditHeadHash(),
                    sourceHashFromSignatures(signatures),
                    attachments,
                    attachmentChain.getHeadHash(),
                    approvalSnapshot == null ? null : approvalSnapshot.getId(),
                    approvalSnapshot == null ? null : approvalSnapshot.getSnapshotHash());
        }
        boolean hasApproveSignature = signatures.stream()
                .anyMatch(signature -> "APPROVE".equals(signature.getActionType())
                        && Objects.equals(signature.getProcessInstanceId(), execution.getProcessInstanceId()));
        if (!hasApproveSignature) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
        }
        if (approvalSnapshot == null || approvalSnapshot.getId() == null
                || StrUtil.isBlank(approvalSnapshot.getSnapshotHash())
                || !"APPROVED".equals(approvalSnapshot.getApprovalStatus())
                || !Objects.equals(approvalSnapshot.getProcessInstanceId(), execution.getProcessInstanceId())) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
        }
        String domainTraceHash = requireLockedDomainTraceHash(approvalSnapshot);
        domainTraceService.verifyForArchive(execution.getId(), domainTraceHash);
        MesProBatchRecordExecutionAttachmentChainVerifyResult attachmentChain =
                attachmentService.verifyAttachmentChain(execution.getId());
        if (attachmentChain == null || !attachmentChain.isValid()) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_CHAIN_INVALID);
        }
        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionId(execution.getId());
        if (!MesProBatchRecordAttachmentRuleSupport.collectMissingRequiredAttachments(
                execution.getExecutionSnapshotJson(), attachments).isEmpty()) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_METADATA_INCOMPLETE);
        }
        validateActiveAttachmentArchiveMetadata(attachments);
        return new SourceData(execution, executionSnapshot, cellValues, signatures,
                sourceHashFromJson(execution.getExecutionSnapshotJson()),
                execution.getCellValuesHash(),
                execution.getFieldAuditRevision(),
                execution.getFieldAuditHeadHash(),
                sourceHashFromSignatures(signatures),
                attachments,
                attachmentChain.getHeadHash(),
                approvalSnapshot.getId(),
                approvalSnapshot.getSnapshotHash());
    }

    private void validateActiveAttachmentArchiveMetadata(List<MesProBatchRecordExecutionAttachmentDO> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }
        for (MesProBatchRecordExecutionAttachmentDO attachment : attachments) {
            if (!isActiveAttachmentEvent(attachment)) {
                continue;
            }
            if (attachment.getFileId() == null
                    || attachment.getStorageConfigId() == null
                    || StrUtil.isBlank(attachment.getStoragePath())
                    || StrUtil.isBlank(attachment.getFileName())
                    || StrUtil.isBlank(attachment.getContentType())
                    || attachment.getFileSize() == null
                    || attachment.getFileSize() <= 0
                    || StrUtil.isBlank(attachment.getSha256())
                    || StrUtil.isBlank(attachment.getStorageRetentionHash())
                    || StrUtil.isBlank(attachment.getAttachmentHash())
                    || attachment.getAuditBatchId() == null
                    || attachment.getSignatureId() == null) {
                throw exception(PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_METADATA_INCOMPLETE);
            }
        }
    }

    private boolean isActiveAttachmentEvent(MesProBatchRecordExecutionAttachmentDO attachment) {
        return attachment != null
                && ("ADD".equals(attachment.getAttachmentAction())
                || "REPLACE".equals(attachment.getAttachmentAction()));
    }

    private String requireLockedDomainTraceHash(MesProBatchRecordApprovalSnapshotDO snapshot) {
        JSONObject snapshotJson;
        try {
            snapshotJson = JSON.parseObject(snapshot.getSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID);
        }
        if (snapshotJson == null || snapshotJson.getLong("domainTraceSnapshotId") == null
                || StrUtil.isBlank(snapshotJson.getString("domainTraceHash"))
                || !DOMAIN_TRACE_STATUS_VERIFIED.equals(snapshotJson.getString("domainTraceStatus"))) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID);
        }
        return snapshotJson.getString("domainTraceHash");
    }

    private MesProBatchRecordExecutionArchiveDO createGeneratingArchive(MesProBatchRecordExecutionDO execution,
                                                                        String artifactType,
                                                                        SourceData sourceData,
                                                                        Long actorId,
                                                                        LocalDateTime generatedAt,
                                                                        Integer version,
                                                                        String comment) {
        MesProBatchRecordExecutionArchiveDO archive = MesProBatchRecordExecutionArchiveDO.builder()
                .executionId(execution.getId())
                .archiveCode(buildArchiveCode(execution.getId(), version, generatedAt))
                .archiveVersion(version)
                .artifactType(artifactType)
                .archiveStatus(ARCHIVE_STATUS_GENERATING)
                .executionSnapshotHash(sourceData.executionSnapshotHash)
                .cellValuesHash(sourceData.cellValuesHash)
                .fieldAuditRevision(sourceData.fieldAuditRevision)
                .fieldAuditHeadHash(sourceData.fieldAuditHeadHash)
                .signatureHash(sourceData.signatureHash)
                .approvalSnapshotId(sourceData.approvalSnapshotId)
                .approvalSnapshotHash(sourceData.approvalSnapshotHash)
                .generatedBy(actorId)
                .generatedAt(generatedAt)
                .remark(StrUtil.blankToDefault(StrUtil.trim(comment), null))
                .build();
        archiveMapper.insert(archive);
        return archive;
    }

    private MesProBatchRecordExecutionArchiveRenderer requireRendererWithFailureEvent(
            MesProBatchRecordExecutionArchiveDO archive, Long actorId, String artifactType) {
        try {
            return requireRenderer(artifactType);
        } catch (ServiceException ex) {
            markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE);
            throw ex;
        }
    }

    private MesProBatchRecordExecutionArchiveRenderResult renderArchive(
            MesProBatchRecordExecutionArchiveDO archive, MesProBatchRecordExecutionArchiveRenderer renderer,
            SourceData sourceData, Long actorId, LocalDateTime generatedAt) {
        try {
            MesProBatchRecordExecutionArchiveRenderResult renderResult = renderer.render(MesProBatchRecordExecutionArchiveRenderContext.builder()
                    .execution(sourceData.execution)
                    .executionSnapshot(sourceData.executionSnapshot)
                    .cellValues(sourceData.cellValues)
                    .signatures(sourceData.signatures)
                    .executionSnapshotHash(sourceData.executionSnapshotHash)
                    .cellValuesHash(sourceData.cellValuesHash)
                    .fieldAuditRevision(sourceData.fieldAuditRevision)
                    .fieldAuditHeadHash(sourceData.fieldAuditHeadHash)
                    .signatureHash(sourceData.signatureHash)
                    .attachments(sourceData.attachments)
                    .attachmentManifestHeadHash(sourceData.attachmentManifestHeadHash)
                    .approvalSnapshotId(sourceData.approvalSnapshotId)
                    .approvalSnapshotHash(sourceData.approvalSnapshotHash)
                    .generatedBy(actorId)
                    .generatedAt(generatedAt)
                    .build());
            validateRenderResult(renderResult);
            return renderResult;
        } catch (Exception ex) {
            markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_RENDER_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_RENDER_FAILED);
        }
    }

    private void validateRenderResult(MesProBatchRecordExecutionArchiveRenderResult renderResult) {
        if (renderResult == null || renderResult.getContent() == null || renderResult.getContent().length == 0
                || StrUtil.isBlank(renderResult.getFileName()) || StrUtil.isBlank(renderResult.getContentType())
                || renderResult.getFileSize() == null || renderResult.getFileSize() <= 0
                || StrUtil.isBlank(renderResult.getSha256()) || StrUtil.isBlank(renderResult.getRenderSourceVersion())
                || !Objects.equals(renderResult.getSha256(), sha256(renderResult.getContent()))
                || !Objects.equals(renderResult.getFileSize(), (long) renderResult.getContent().length)) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_RENDER_FAILED);
        }
    }

    private StorageRetentionEvidence persistArchiveFile(MesProBatchRecordExecutionArchiveDO archive, Long actorId,
                                                        MesProBatchRecordExecutionArchiveRenderResult renderResult) {
        try {
            StorageRetentionPolicy uploadPolicy = protectedStorage.requireUploadPolicy(renderResult.getSha256());
            return fileService.createFileWithStorageRetention(protectedStorage.getFileConfigId(),
                    renderResult.getContent(), renderResult.getFileName(), ARCHIVE_DIRECTORY,
                    renderResult.getContentType(), uploadPolicy);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[persistArchiveFile][archiveId({}) executionId({}) eDHR protected archive storage failed]",
                    archive.getId(), archive.getExecutionId(), ex);
            markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED);
        }
    }

    private void requireStorageRetentionEvidenceGate(MesProBatchRecordExecutionArchiveDO archive, Long actorId,
                                                     StorageRetentionEvidence evidence) {
        if (!isStorageRetentionEvidenceComplete(evidence)) {
            markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }
    }

    private Long recordSealSignature(MesProBatchRecordExecutionArchiveDO archive, Long actorId,
                                     String sealPassword, String comment,
                                     MesProBatchRecordExecutionSignatureTimeReqVO signatureTime) {
        try {
            MesProBatchRecordExecutionSignatureTimeCommand signatureTimeCommand =
                    buildSignatureTimeCommand(signatureTime);
            Long signatureId = signatureTimeCommand == null
                    ? signatureService.recordArchiveSealSignature(archive.getExecutionId(), sealPassword, comment)
                    : signatureService.recordArchiveSealSignature(archive.getExecutionId(), sealPassword, comment,
                            signatureTimeCommand);
            if (signatureId == null) {
                markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
                throw exception(PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
            }
            signatureService.bindSignatureFieldAuditEvidence(signatureId, archive.getExecutionId(),
                    archive.getFieldAuditRevision(), archive.getFieldAuditHeadHash(), archive.getCellValuesHash());
            return signatureId;
        } catch (ServiceException ex) {
            markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
        } catch (Exception ex) {
            markArchiveFailedAndRecordEvent(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
        }
    }

    private MesProBatchRecordExecutionSignatureTimeCommand buildSignatureTimeCommand(
            MesProBatchRecordExecutionSignatureTimeReqVO signatureTime) {
        if (signatureTime == null) {
            return null;
        }
        LocalDateTime selectedSignedAt = signatureTime.getSelectedSignedAt();
        String selectedTimeZone = signatureTime.getSelectedTimeZone();
        String selectedTimeReason = signatureTime.getSelectedTimeReason();
        if (selectedSignedAt == null && StrUtil.isBlank(selectedTimeZone) && StrUtil.isBlank(selectedTimeReason)) {
            return null;
        }
        if (selectedSignedAt == null || StrUtil.isBlank(selectedTimeZone) || StrUtil.isBlank(selectedTimeReason)) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
        }
        return new MesProBatchRecordExecutionSignatureTimeCommand()
                .setSelectedSignedAt(selectedSignedAt)
                .setSelectedTimeZone(StrUtil.trim(selectedTimeZone))
                .setSelectedTimeReason(StrUtil.trim(selectedTimeReason));
    }

    private void deleteCreatedArchiveFile(Long fileId, RuntimeException sealFailure) {
        try {
            fileService.deleteFile(fileId);
        } catch (Exception cleanupFailure) {
            cleanupFailure.addSuppressed(sealFailure);
            if (cleanupFailure instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cleanupFailure.getMessage(), cleanupFailure);
        }
    }

    private List<Long> selectExecutionIdsForPageFilter(MesProBatchRecordExecutionArchivePageReqVO pageReqVO) {
        if (pageReqVO.getWorkOrderId() == null && StrUtil.isBlank(pageReqVO.getWorkOrderCode())
                && StrUtil.isBlank(pageReqVO.getBatchCode())) {
            return null;
        }
        return executionMapper.selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionDO>()
                        .eqIfPresent(MesProBatchRecordExecutionDO::getWorkOrderId, pageReqVO.getWorkOrderId())
                        .eqIfPresent(MesProBatchRecordExecutionDO::getWorkOrderCode, pageReqVO.getWorkOrderCode())
                        .likeIfPresent(MesProBatchRecordExecutionDO::getBatchCode, pageReqVO.getBatchCode()))
                .stream()
                .map(MesProBatchRecordExecutionDO::getId)
                .toList();
    }

    private MesProBatchRecordExecutionArchiveRespVO toRespVO(MesProBatchRecordExecutionArchiveDO archive,
                                                             Boolean created) {
        MesProBatchRecordExecutionArchiveRespVO respVO = new MesProBatchRecordExecutionArchiveRespVO();
        respVO.setId(archive.getId());
        respVO.setExecutionId(archive.getExecutionId());
        respVO.setArchiveCode(archive.getArchiveCode());
        respVO.setArchiveVersion(archive.getArchiveVersion());
        respVO.setArtifactType(archive.getArtifactType());
        respVO.setArchiveStatus(archive.getArchiveStatus());
        respVO.setFileId(archive.getFileId());
        respVO.setFileName(archive.getFileName());
        respVO.setContentType(archive.getContentType());
        respVO.setFileSize(archive.getFileSize());
        respVO.setSha256(archive.getSha256());
        respVO.setRenderSourceVersion(archive.getRenderSourceVersion());
        respVO.setExecutionSnapshotHash(archive.getExecutionSnapshotHash());
        respVO.setCellValuesHash(archive.getCellValuesHash());
        respVO.setFieldAuditRevision(archive.getFieldAuditRevision());
        respVO.setFieldAuditHeadHash(archive.getFieldAuditHeadHash());
        respVO.setSignatureHash(archive.getSignatureHash());
        respVO.setApprovalSnapshotId(archive.getApprovalSnapshotId());
        respVO.setApprovalSnapshotHash(archive.getApprovalSnapshotHash());
        respVO.setGeneratedBy(archive.getGeneratedBy());
        respVO.setGeneratedAt(archive.getGeneratedAt());
        respVO.setSealedBy(archive.getSealedBy());
        respVO.setSealedAt(archive.getSealedAt());
        respVO.setSealSignatureId(archive.getSealSignatureId());
        respVO.setFailureReason(archive.getFailureReason());
        respVO.setRemark(archive.getRemark());
        populateAttachmentManifest(respVO, archive.getExecutionId());
        respVO.setCreated(created);
        return respVO;
    }

    private void populateAttachmentManifest(MesProBatchRecordExecutionArchiveRespVO respVO, Long executionId) {
        MesProBatchRecordExecutionAttachmentChainVerifyResult attachmentChain =
                attachmentService.verifyAttachmentChain(executionId);
        if (attachmentChain == null || !attachmentChain.isValid()) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_CHAIN_INVALID);
        }
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper.selectListByExecutionId(executionId);
        respVO.setAttachmentManifestCount(attachments.size());
        respVO.setAttachmentManifestHeadHash(attachmentChain.getHeadHash());
        respVO.setAttachmentManifest(attachments.stream()
                .map(this::toAttachmentManifestItem)
                .toList());
    }

    private MesProBatchRecordExecutionArchiveRespVO.AttachmentManifestItem toAttachmentManifestItem(
            MesProBatchRecordExecutionAttachmentDO attachment) {
        MesProBatchRecordExecutionArchiveRespVO.AttachmentManifestItem item =
                new MesProBatchRecordExecutionArchiveRespVO.AttachmentManifestItem();
        item.setId(attachment.getId());
        item.setFieldKey(attachment.getFieldKey());
        item.setAttachmentType(attachment.getAttachmentType());
        item.setAttachmentGroupKey(attachment.getAttachmentGroupKey());
        item.setFileName(attachment.getFileName());
        item.setContentType(attachment.getContentType());
        item.setFileSize(attachment.getFileSize());
        item.setSha256(attachment.getSha256());
        item.setAttachmentHash(attachment.getAttachmentHash());
        return item;
    }

    private StorageRetentionMetadata requireDownloadStorageRetentionEvidence(
            MesProBatchRecordExecutionArchiveDO archive, Long actorId) {
        try {
            return requireStoredArchiveStorageRetentionEvidence(archive);
        } catch (Exception ex) {
            recordDownloadFailure(archive, actorId, PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }
    }

    private void requireExistingArchiveStorageRetentionEvidence(MesProBatchRecordExecutionArchiveDO archive, Long actorId) {
        try {
            requireStoredArchiveStorageRetentionEvidence(archive);
        } catch (Exception ex) {
            recordEvent(archive.getId(), archive.getExecutionId(), EVENT_GENERATE_FAILED, actorId,
                    failureMessage(PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED), null);
            throw exception(PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }
    }

    private StorageRetentionMetadata requireStoredArchiveStorageRetentionEvidence(
            MesProBatchRecordExecutionArchiveDO archive) {
        StorageRetentionMetadata metadata = requireLatestStorageRetentionMetadata(archive);
        try {
            protectedStorage.requireClientRegistered();
            StorageRetentionEvidence evidence = fileService.requireStorageRetentionEvidence(archive.getFileId(),
                    storageRetentionPolicy(metadata));
            if (!matchesStorageRetentionMetadata(evidence, metadata)) {
                throw new IllegalStateException("archive storage retention evidence mismatch");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("archive storage retention evidence verification failed", ex);
        }
        return metadata;
    }

    private StorageRetentionMetadata requireLatestStorageRetentionMetadata(
            MesProBatchRecordExecutionArchiveDO archive) {
        for (MesProBatchRecordExecutionArchiveEventDO event : archiveEventMapper.selectListByArchiveId(archive.getId())) {
            if (StrUtil.isBlank(event.getMetadataJson())) {
                continue;
            }
            JSONObject metadataJson = JSON.parseObject(event.getMetadataJson());
            if (metadataJson == null || !metadataJson.containsKey(METADATA_STORAGE_RETENTION)) {
                continue;
            }
            JSONObject storageRetention = metadataJson.getJSONObject(METADATA_STORAGE_RETENTION);
            StorageRetentionMetadata metadata = toStorageRetentionMetadata(storageRetention);
            if (!Objects.equals(archive.getFileId(), metadata.fileId())) {
                throw new IllegalStateException("archive storage retention metadata fileId mismatch");
            }
            if (!Objects.equals(archive.getSha256(), metadata.sha256())) {
                throw new IllegalStateException("archive storage retention metadata sha256 mismatch");
            }
            return metadata;
        }
        throw new IllegalStateException("archive storage retention metadata missing");
    }

    private StorageRetentionMetadata toStorageRetentionMetadata(JSONObject storageRetention) {
        if (storageRetention == null) {
            throw new IllegalStateException("archive storage retention metadata missing");
        }
        Long fileId = storageRetention.getLong("fileId");
        String bucket = storageRetention.getString("bucket");
        String path = storageRetention.getString("path");
        String key = storageRetention.getString("key");
        String objectVersionId = storageRetention.getString("objectVersionId");
        String retentionMode = storageRetention.getString("retentionMode");
        Instant retainUntil = instantValue(storageRetention, "retainUntil");
        String legalHoldStatus = storageRetention.getString("legalHoldStatus");
        Instant verifiedAt = instantValue(storageRetention, "verifiedAt");
        String sha256 = storageRetention.getString("sha256");
        StorageRetentionMetadata metadata = new StorageRetentionMetadata(fileId, bucket, path, key, objectVersionId,
                retentionMode, retainUntil, legalHoldStatus, verifiedAt, sha256);
        if (!isStorageRetentionMetadataComplete(metadata)) {
            throw new IllegalStateException("archive storage retention metadata incomplete");
        }
        return metadata;
    }

    private Instant instantValue(JSONObject jsonObject, String fieldName) {
        String value = jsonObject.getString(fieldName);
        if (StrUtil.isBlank(value)) {
            throw new IllegalStateException("archive storage retention metadata " + fieldName + " missing");
        }
        return Instant.parse(value);
    }

    private StorageRetentionPolicy storageRetentionPolicy(StorageRetentionMetadata metadata) {
        return new StorageRetentionPolicy()
                .setObjectLockRequired(Boolean.TRUE)
                .setRetentionMode(metadata.retentionMode())
                .setRetainUntil(metadata.retainUntil())
                .setLegalHoldRequired(STORAGE_LEGAL_HOLD_ON.equalsIgnoreCase(metadata.legalHoldStatus()))
                .setObjectVersionId(metadata.objectVersionId())
                .setChecksumSha256(metadata.sha256());
    }

    private boolean matchesStorageRetentionMetadata(StorageRetentionEvidence evidence,
                                                   StorageRetentionMetadata metadata) {
        return isStorageRetentionEvidenceComplete(evidence)
                && Objects.equals(metadata.fileId(), evidence.getFileId())
                && Objects.equals(metadata.bucket(), evidence.getBucket())
                && Objects.equals(metadata.path(), evidence.getPath())
                && Objects.equals(metadata.key(), evidence.getKey())
                && Objects.equals(metadata.objectVersionId(), evidence.getObjectVersionId())
                && Objects.equals(metadata.retentionMode(), evidence.getRetentionMode())
                && Objects.equals(metadata.retainUntil(), evidence.getRetainUntil())
                && Objects.equals(metadata.legalHoldStatus(), evidence.getLegalHoldStatus())
                && Objects.equals(metadata.sha256(), evidence.getChecksumSha256());
    }

    private String storageRetentionMetadataJson(StorageRetentionEvidence evidence, String sha256) {
        JSONObject storageRetention = new JSONObject();
        storageRetention.put("objectLock", Boolean.TRUE);
        storageRetention.put("legalHold", STORAGE_LEGAL_HOLD_ON.equalsIgnoreCase(evidence.getLegalHoldStatus()));
        storageRetention.put("fileId", evidence.getFileId());
        storageRetention.put("bucket", evidence.getBucket());
        storageRetention.put("path", evidence.getPath());
        storageRetention.put("key", evidence.getKey());
        storageRetention.put("objectVersionId", evidence.getObjectVersionId());
        storageRetention.put("retentionMode", evidence.getRetentionMode());
        storageRetention.put("retainUntil", evidence.getRetainUntil().toString());
        storageRetention.put("legalHoldStatus", evidence.getLegalHoldStatus());
        storageRetention.put("verifiedAt", evidence.getVerifiedAt().toString());
        storageRetention.put("sha256", sha256);
        JSONObject metadata = new JSONObject();
        metadata.put(METADATA_STORAGE_RETENTION, storageRetention);
        return metadata.toJSONString();
    }

    private boolean isStorageRetentionEvidenceComplete(StorageRetentionEvidence evidence) {
        return evidence != null
                && evidence.getFileId() != null
                && StrUtil.isNotBlank(evidence.getBucket())
                && (StrUtil.isNotBlank(evidence.getPath()) || StrUtil.isNotBlank(evidence.getKey()))
                && StrUtil.isNotBlank(evidence.getObjectVersionId())
                && StrUtil.isNotBlank(evidence.getRetentionMode())
                && evidence.getRetainUntil() != null
                && StrUtil.isNotBlank(evidence.getLegalHoldStatus())
                && evidence.getVerifiedAt() != null;
    }

    private boolean isStorageRetentionMetadataComplete(StorageRetentionMetadata metadata) {
        return metadata.fileId() != null
                && StrUtil.isNotBlank(metadata.bucket())
                && (StrUtil.isNotBlank(metadata.path()) || StrUtil.isNotBlank(metadata.key()))
                && StrUtil.isNotBlank(metadata.objectVersionId())
                && StrUtil.isNotBlank(metadata.retentionMode())
                && metadata.retainUntil() != null
                && StrUtil.isNotBlank(metadata.legalHoldStatus())
                && metadata.verifiedAt() != null
                && StrUtil.isNotBlank(metadata.sha256());
    }

    private void recordDownloadFailure(MesProBatchRecordExecutionArchiveDO archive, Long actorId, ErrorCode errorCode) {
        recordEvent(archive.getId(), archive.getExecutionId(), EVENT_DOWNLOAD_FAILED, actorId,
                failureMessage(errorCode), null);
    }

    private void markArchiveFailedAndRecordEvent(MesProBatchRecordExecutionArchiveDO archive, Long actorId,
                                                 ErrorCode errorCode) {
        archive.setArchiveStatus(ARCHIVE_STATUS_FAILED)
                .setFailureReason(failureMessage(errorCode));
        archiveMapper.updateById(archive);
        recordEvent(archive.getId(), archive.getExecutionId(), EVENT_GENERATE_FAILED, actorId,
                failureMessage(errorCode), null);
    }

    private void recordEvent(Long archiveId, Long executionId, String eventType, Long actorId,
                             String message, String metadataJson) {
        archiveEventMapper.insert(MesProBatchRecordExecutionArchiveEventDO.builder()
                .archiveId(archiveId)
                .executionId(executionId)
                .eventType(eventType)
                .actorId(actorId)
                .eventTime(LocalDateTime.now())
                .message(message)
                .metadataJson(metadataJson)
                .build());
    }

    private String sourceHashFromJson(String json) {
        Object parsed = JSON.parse(json);
        if (parsed instanceof JSONObject jsonObject && jsonObject.containsKey("source")) {
            Object source = jsonObject.get("source");
            if (source != null) {
                return sha256(String.valueOf(source).getBytes(StandardCharsets.UTF_8));
            }
        }
        return sha256(json.getBytes(StandardCharsets.UTF_8));
    }

    private String sourceHashFromSignatures(List<MesProBatchRecordExecutionSignatureDO> signatures) {
        List<String> signatureProjections = signatures.stream()
                .filter(signature -> "SUBMIT".equals(signature.getActionType()) || "APPROVE".equals(signature.getActionType()))
                .sorted(Comparator.comparing(MesProBatchRecordExecutionSignatureDO::getSignedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesProBatchRecordExecutionSignatureDO::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::signatureProjection)
                .toList();
        return sha256(String.join("\n", signatureProjections).getBytes(StandardCharsets.UTF_8));
    }

    private String signatureProjection(MesProBatchRecordExecutionSignatureDO signature) {
        return "id=" + value(signature.getId())
                + "|executionId=" + value(signature.getExecutionId())
                + "|actionType=" + value(signature.getActionType())
                + "|actorId=" + value(signature.getActorId())
                + "|processInstanceId=" + value(signature.getProcessInstanceId())
                + "|bpmTaskId=" + value(signature.getBpmTaskId())
                + "|fieldAuditRevision=" + value(signature.getFieldAuditRevision())
                + "|fieldAuditHeadHash=" + value(signature.getFieldAuditHeadHash())
                + "|cellValuesHash=" + value(signature.getCellValuesHash())
                + "|signedAt=" + value(signature.getSignedAt())
                + "|selectedSignedAt=" + value(signature.getSelectedSignedAt())
                + "|signatureDisplayAt=" + value(signature.getSignatureDisplayAt())
                + "|signatureTimeMode=" + value(signature.getSignatureTimeMode())
                + "|selectedTimeZone=" + value(signature.getSelectedTimeZone())
                + "|selectedTimeReason=" + value(signature.getSelectedTimeReason())
                + "|selectedTimePolicyVersion=" + value(signature.getSelectedTimePolicyVersion())
                + "|selectedTimeAuditHash=" + value(signature.getSelectedTimeAuditHash())
                + "|reason=" + value(signature.getReason())
                + "|comment=" + value(signature.getComment());
    }

    private String value(Object value) {
        return Objects.toString(value, "");
    }

    private Long currentAuditUserId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }

    private void recordOperationAudit(String operationType, String actionName,
                                      MesProBatchRecordExecutionArchiveDO archive,
                                      String resultStatus, String beforeSummaryHash,
                                      String afterSummaryHash, String metadataJson) {
        operationAuditService.record(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR-AUD-" + java.util.UUID.randomUUID())
                .setObjectType("EXECUTION_ARCHIVE")
                .setObjectId(String.valueOf(archive.getId()))
                .setExecutionId(archive.getExecutionId())
                .setOperationType(operationType)
                .setActionName(actionName)
                .setActorUserId(currentAuditUserId())
                .setActorUsername(SecurityFrameworkUtils.getLoginUserNickname())
                .setPermissionCode(archivePermissionCode(operationType))
                .setPermissionDecision("ALLOW")
                .setResultStatus(resultStatus)
                .setBeforeSummaryHash(beforeSummaryHash)
                .setAfterSummaryHash(afterSummaryHash)
                .setMetadataJson(metadataJson));
    }

    private String archivePermissionCode(String operationType) {
        return switch (operationType) {
            case "ARCHIVE" -> "mes:pro-batch-record-execution-archive:create";
            case "DOWNLOAD" -> "mes:pro-batch-record-execution-archive:download";
            default -> "mes:pro-batch-record-execution-archive:query";
        };
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 calculation failed", ex);
        }
    }

    private String buildArchiveCode(Long executionId, Integer version, LocalDateTime generatedAt) {
        return "EDHRA-" + generatedAt.format(ARCHIVE_CODE_TIME_FORMATTER) + "-" + executionId + "-V" + version;
    }

    private String failureMessage(ErrorCode errorCode) {
        return "errorCode=" + errorCode.getCode() + ", message=" + errorCode.getMsg();
    }

    private MesProBatchRecordExecutionArchiveRenderer requireRenderer(String artifactType) {
        if (renderers == null || renderers.isEmpty()) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE);
        }
        return renderers.stream()
                .filter(renderer -> artifactType.equalsIgnoreCase(renderer.getArtifactType()))
                .findFirst()
                .orElseThrow(() -> exception(PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE));
    }

    private String normalizeArtifactType(String artifactType) {
        String normalized = StrUtil.trim(artifactType).toUpperCase(Locale.ROOT);
        if (!ARTIFACT_TYPE_PDF.equals(normalized) && !ARTIFACT_TYPE_EXCEL.equals(normalized)) {
            throw exception(PRO_BATCH_RECORD_ARCHIVE_TYPE_UNSUPPORTED);
        }
        return normalized;
    }

    private String normalizeOptionalArtifactType(String artifactType) {
        if (StrUtil.isBlank(artifactType)) {
            return null;
        }
        return normalizeArtifactType(artifactType);
    }

    private record StorageRetentionMetadata(Long fileId,
                                            String bucket,
                                            String path,
                                            String key,
                                            String objectVersionId,
                                            String retentionMode,
                                            Instant retainUntil,
                                            String legalHoldStatus,
                                            Instant verifiedAt,
                                            String sha256) {
    }

    private record SourceData(MesProBatchRecordExecutionDO execution,
                              JSONObject executionSnapshot,
                              Object cellValues,
                              List<MesProBatchRecordExecutionSignatureDO> signatures,
                              String executionSnapshotHash,
                              String cellValuesHash,
                              Long fieldAuditRevision,
                              String fieldAuditHeadHash,
                              String signatureHash,
                              List<MesProBatchRecordExecutionAttachmentDO> attachments,
                              String attachmentManifestHeadHash,
                              Long approvalSnapshotId,
                              String approvalSnapshotHash) {
    }
}
