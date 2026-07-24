package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveDownloadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveGenerateReqVO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Comparator;
import java.util.Objects;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_CHECKSUM_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_METADATA_INCOMPLETE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_CHAIN_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_SOURCE_CHANGED_REGENERATE_REQUIRED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants.PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import(MesProBatchRecordExecutionArchiveServiceImpl.class)
class MesProBatchRecordExecutionArchiveServiceImplTest extends BaseDbUnitTest {

    private static final Long ACTOR_ID = 101L;
    private static final String ARTIFACT_TYPE = "PDF";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String RENDER_SOURCE_VERSION = "EDHR_ARCHIVE_V1";
    private static final byte[] ARCHIVE_BYTES = "sealed-pdf-content".getBytes(StandardCharsets.UTF_8);
    private static final String ARCHIVE_SHA256 = sha256(ARCHIVE_BYTES);
    private static final String SNAPSHOT_HASH = sha256("snapshot-v1");
    private static final String CELL_VALUES_HASH =
            MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("{\"source\":\"cell-values-v1\"}");
    private static final String SIGNATURE_HASH = sha256("legacy-existing-archive-signature-hash");
    private static final Long DOMAIN_TRACE_SNAPSHOT_ID = 7001L;
    private static final String DOMAIN_TRACE_HASH =
            "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";
    private static final String STORAGE_BUCKET = "edhr-lock-bucket";
    private static final String STORAGE_PATH = "mes/edhr/archive/edhr-execution-v1.pdf";
    private static final String STORAGE_RETENTION_MODE = "COMPLIANCE";
    private static final Instant STORAGE_RETAIN_UNTIL = Instant.parse("2036-05-28T00:00:00Z");
    private static final Instant STORAGE_VERIFIED_AT = Instant.parse("2026-05-28T02:30:00Z");
    private static final String STORAGE_LEGAL_HOLD_STATUS = "ON";
    private static final Long PROTECTED_FILE_CONFIG_ID = -1_040_750_314L;

    @Resource
    private MesProBatchRecordExecutionArchiveService archiveService;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProBatchRecordExecutionSignatureMapper signatureMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveMapper archiveMapper;
    @Resource
    private MesProBatchRecordExecutionArchiveEventMapper archiveEventMapper;
    @Resource
    private MesProBatchRecordApprovalSnapshotMapper approvalSnapshotMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;

    @MockitoBean
    private MesProBatchRecordExecutionArchiveRenderer renderer;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private MesEdhrArchiveProtectedStorage protectedStorage;
    @MockitoBean
    private MesProBatchRecordExecutionSignatureService signatureService;
    @MockitoBean
    private MesProBatchRecordDomainTraceService domainTraceService;
    @MockitoBean
    private MesProBatchRecordExecutionAttachmentService attachmentService;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private MesProEdhrPermissionGateService permissionGateService;

    @BeforeEach
    void setUp() {
        when(renderer.getArtifactType()).thenReturn(ARTIFACT_TYPE);
        when(attachmentService.verifyAttachmentChain(anyLong()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(0)
                        .headHash(null)
                        .build());
    }

    @Test
    @DisplayName("BDD: approved and closed execution with valid evidence creates SEALED archive")
    void generateExecutionArchive_approvedClosedExecution_createsSealedArchiveWithMetadataHashesSignatureAndEvents()
            throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        MesProBatchRecordApprovalSnapshotDO approvalSnapshot = insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(attachmentService.verifyAttachmentChain(execution.getId()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(0)
                        .headHash(null)
                        .build());
        String expectedSignatureHash = approvedSignatureHash(execution.getId());
        mockSuccessfulRenderAndSeal(execution.getId(), 8801L);
        StorageRetentionEvidence storageEvidence = storageEvidence(5001L);
        mockArchiveStorageRetention(storageEvidence);

        MesProBatchRecordExecutionArchiveRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = archiveService.generateExecutionArchive(generateReq(execution.getId(), false));
        }

        assertNotNull(response.getId());
        assertEquals(execution.getId(), response.getExecutionId());
        assertEquals(ARTIFACT_TYPE, response.getArtifactType());
        assertEquals("SEALED", response.getArchiveStatus());
        assertEquals(1, response.getArchiveVersion());
        assertEquals(5001L, response.getFileId());
        assertEquals("edhr-execution-v1.pdf", response.getFileName());
        assertEquals(CONTENT_TYPE, response.getContentType());
        assertEquals((long) ARCHIVE_BYTES.length, response.getFileSize());
        assertEquals(ARCHIVE_SHA256, response.getSha256());
        assertEquals(RENDER_SOURCE_VERSION, response.getRenderSourceVersion());
        assertEquals(SNAPSHOT_HASH, response.getExecutionSnapshotHash());
        assertEquals(CELL_VALUES_HASH, response.getCellValuesHash());
        assertEquals(execution.getFieldAuditRevision(), response.getFieldAuditRevision());
        assertEquals(execution.getFieldAuditHeadHash(), response.getFieldAuditHeadHash());
        assertEquals(expectedSignatureHash, response.getSignatureHash());
        assertEquals(approvalSnapshot.getId(), response.getApprovalSnapshotId());
        assertEquals(approvalSnapshot.getSnapshotHash(), response.getApprovalSnapshotHash());
        assertFalse(response.getSignatureHash().equals(sha256("submit-signature-v1\napprove-signature-v1")));
        assertEquals(8801L, response.getSealSignatureId());
        assertEquals(Boolean.TRUE, response.getCreated());

        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectById(response.getId());
        assertEquals("SEALED", archive.getArchiveStatus());
        assertEquals(5001L, archive.getFileId());
        assertEquals(8801L, archive.getSealSignatureId());
        assertEquals(execution.getFieldAuditRevision(), archive.getFieldAuditRevision());
        verify(permissionGateService).requireAbility(argThat(command ->
                "BATCH_RECORD_EXECUTION".equals(command.getObjectType())
                        && String.valueOf(execution.getId()).equals(command.getObjectId())
                        && "ARCHIVE".equals(command.getAbility())
                        && execution.getId().equals(command.getExecutionId())
                        && "mes:pro-batch-record-execution-archive:create"
                        .equals(command.getPermissionCode())));
        verify(operationAuditService).record(argThat(audit ->
                "ARCHIVE".equals(audit.getOperationType())
                        && "EXECUTION_ARCHIVE".equals(audit.getObjectType())
                        && String.valueOf(response.getId()).equals(audit.getObjectId())
                        && "SUCCESS".equals(audit.getResultStatus())
                        && "mes:pro-batch-record-execution-archive:create".equals(audit.getPermissionCode())));
        assertEquals(execution.getFieldAuditHeadHash(), archive.getFieldAuditHeadHash());
        assertEquals(approvalSnapshot.getId(), archive.getApprovalSnapshotId());
        assertEquals(approvalSnapshot.getSnapshotHash(), archive.getApprovalSnapshotHash());
        assertEventTypes(response.getId(), "GENERATE_SUCCESS", "ARCHIVE_SEAL");
        MesProBatchRecordExecutionArchiveEventDO generateSuccess =
                requireArchiveEvent(response.getId(), "GENERATE_SUCCESS");
        JSONObject storageRetention =
                JSON.parseObject(generateSuccess.getMetadataJson()).getJSONObject("storageRetention");
        assertEquals(5001L, storageRetention.getLong("fileId"));
        assertEquals(STORAGE_BUCKET, storageRetention.getString("bucket"));
        assertEquals(STORAGE_PATH, storageRetention.getString("path"));
        assertEquals(STORAGE_PATH, storageRetention.getString("key"));
        assertEquals(storageEvidence.getObjectVersionId(), storageRetention.getString("objectVersionId"));
        assertEquals(STORAGE_RETENTION_MODE, storageRetention.getString("retentionMode"));
        assertEquals(STORAGE_RETAIN_UNTIL.toString(), storageRetention.getString("retainUntil"));
        assertEquals(STORAGE_LEGAL_HOLD_STATUS, storageRetention.getString("legalHoldStatus"));
        assertEquals(STORAGE_VERIFIED_AT.toString(), storageRetention.getString("verifiedAt"));
        assertEquals(ARCHIVE_SHA256, storageRetention.getString("sha256"));
        assertEquals(Boolean.TRUE, storageRetention.getBoolean("objectLock"));
        assertEquals(Boolean.TRUE, storageRetention.getBoolean("legalHold"));
        assertFalse(generateSuccess.getMetadataJson().toLowerCase().contains("secret"));
        assertFalse(generateSuccess.getMetadataJson().toLowerCase().contains("presigned"));
        InOrder inOrder = inOrder(domainTraceService, renderer, fileService, signatureService);
        inOrder.verify(domainTraceService).verifyForArchive(execution.getId(), DOMAIN_TRACE_HASH);
        inOrder.verify(renderer).render(any());
        inOrder.verify(fileService).createFileWithStorageRetention(eq(PROTECTED_FILE_CONFIG_ID), eq(ARCHIVE_BYTES),
                eq("edhr-execution-v1.pdf"), eq("mes/edhr/archive"), eq(CONTENT_TYPE),
                argThat(this::matchesUploadStoragePolicy));
        inOrder.verify(signatureService).recordArchiveSealSignature(execution.getId(), "seal-secret", "seal note");
        verify(signatureService).recordArchiveSealSignature(execution.getId(), "seal-secret", "seal note");
        verify(signatureService).bindSignatureFieldAuditEvidence(8801L, execution.getId(),
                execution.getFieldAuditRevision(), execution.getFieldAuditHeadHash(), execution.getCellValuesHash());
    }

    @Test
    void generateExecutionArchive_withSelectedSignatureTimePassesSealSignatureTimeCommand() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(attachmentService.verifyAttachmentChain(execution.getId()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(0)
                        .headHash(null)
                        .build());
        when(renderer.render(any())).thenReturn(renderResult());
        when(signatureService.recordArchiveSealSignature(eq(execution.getId()), eq("seal-secret"), eq("seal note"),
                any(MesProBatchRecordExecutionSignatureTimeCommand.class))).thenReturn(8803L);
        mockArchiveStorageRetention(storageEvidence(5003L));
        LocalDateTime selectedSignedAt = LocalDateTime.of(2026, 6, 15, 13, 40);

        MesProBatchRecordExecutionArchiveGenerateReqVO reqVO = generateReq(execution.getId(), false);
        reqVO.setSignatureTime(new MesProBatchRecordExecutionSignatureTimeReqVO()
                .setSelectedSignedAt(selectedSignedAt)
                .setSelectedTimeZone("Asia/Shanghai")
                .setSelectedTimeReason("封存按线下复核完成时间显示"));
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            archiveService.generateExecutionArchive(reqVO);
        }

        ArgumentCaptor<MesProBatchRecordExecutionSignatureTimeCommand> timeCaptor =
                ArgumentCaptor.forClass(MesProBatchRecordExecutionSignatureTimeCommand.class);
        verify(signatureService).recordArchiveSealSignature(eq(execution.getId()), eq("seal-secret"),
                eq("seal note"), timeCaptor.capture());
        assertEquals(selectedSignedAt, timeCaptor.getValue().getSelectedSignedAt());
        assertEquals("Asia/Shanghai", timeCaptor.getValue().getSelectedTimeZone());
        assertEquals("封存按线下复核完成时间显示", timeCaptor.getValue().getSelectedTimeReason());
    }

    @Test
    @DisplayName("BDD: archive response includes attachment manifest for active attachment ledger")
    void generateExecutionArchive_includesAttachmentManifest() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        MesProBatchRecordExecutionAttachmentDO attachment = insertAttachment(execution);
        when(attachmentService.verifyAttachmentChain(execution.getId()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(1)
                        .headHash(attachment.getAttachmentHash())
                        .build());
        mockSuccessfulRenderAndSeal(execution.getId(), 8801L);
        mockArchiveStorageRetention(storageEvidence(5001L));

        MesProBatchRecordExecutionArchiveRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = archiveService.generateExecutionArchive(generateReq(execution.getId(), false));
        }

        assertEquals(1, response.getAttachmentManifestCount());
        assertEquals(attachment.getAttachmentHash(), response.getAttachmentManifestHeadHash());
        assertNotNull(response.getAttachmentManifest());
        assertEquals(1, response.getAttachmentManifest().size());
        MesProBatchRecordExecutionArchiveRespVO.AttachmentManifestItem item =
                response.getAttachmentManifest().get(0);
        assertEquals(attachment.getId(), item.getId());
        assertEquals("visualEvidence", item.getFieldKey());
        assertEquals("IMAGE", item.getAttachmentType());
        assertEquals("R1C2-IMG-1", item.getAttachmentGroupKey());
        assertEquals("evidence.png", item.getFileName());
        assertEquals("image/png", item.getContentType());
        assertEquals(2048L, item.getFileSize());
        assertEquals(attachment.getSha256(), item.getSha256());
        assertEquals(attachment.getAttachmentHash(), item.getAttachmentHash());
    }

    @Test
    @DisplayName("BDD: archive rejects invalid attachment ledger before renderer and file side effects")
    void generateExecutionArchive_invalidAttachmentChain_rejectsBeforeRenderer() {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(attachmentService.verifyAttachmentChain(execution.getId()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(false)
                        .checkedEventCount(1)
                        .issues(List.of(MesProBatchRecordExecutionAttachmentChainVerifyResult.Issue.builder()
                                .issueCode("ATTACHMENT_HASH_MISMATCH")
                                .message("Attachment hash does not match ledger content")
                                .build()))
                        .build());

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_CHAIN_INVALID);
        }

        assertEquals(0L, countArchives(execution.getId()));
        verify(renderer, never()).render(any());
        verifyNoInteractions(fileService);
        verifyNoInteractions(signatureService);
    }

    @Test
    @DisplayName("BDD: valid attachment chain but incomplete ACTIVE metadata is rejected before archive side effects")
    void generateExecutionArchive_activeAttachmentMissingMetadata_rejectsBeforeRenderer() {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        MesProBatchRecordExecutionAttachmentDO attachment = insertAttachment(execution);
        attachmentMapper.updateById(new MesProBatchRecordExecutionAttachmentDO()
                .setId(attachment.getId())
                .setFileSize(0L));
        when(attachmentService.verifyAttachmentChain(execution.getId()))
                .thenReturn(MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                        .valid(true)
                        .checkedEventCount(1)
                        .headHash(attachment.getAttachmentHash())
                        .build());

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_ATTACHMENT_METADATA_INCOMPLETE);
        }

        assertEquals(0L, countArchives(execution.getId()));
        verify(renderer, never()).render(any());
        verifyNoInteractions(fileService);
        verifyNoInteractions(signatureService);
    }

    @Test
    @DisplayName("BDD: non-closed execution generation is rejected and creates no archive, file or ARCHIVE_SEAL signature")
    void generateExecutionArchive_nonClosedExecution_rejectsAndDoesNotCreateArchiveFileOrSealSignature() {
        for (int status : List.of(0, 1, 2)) {
            MesProBatchRecordExecutionDO execution = insertExecution(status, "snapshot-v1", "cell-values-v1");

            try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
                assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                        PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
            }

            assertEquals(0L, countArchives(execution.getId()));
        }

        assertEquals(0L, archiveEventMapper.selectCount());
        verifyNoInteractions(fileService);
        verifyNoInteractions(signatureService);
    }

    @Test
    @DisplayName("BDD: approved execution without closing evidence is rejected before archive side effects")
    void generateExecutionArchive_approvedButClosingEvidenceMissing_rejectsAndDoesNotCreateArchiveOrFile() {
        MesProBatchRecordExecutionDO execution = insertExecution(3, "snapshot-v1", "cell-values-v1");
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
        }

        assertEquals(0L, countArchives(execution.getId()));
        assertEquals(0L, archiveEventMapper.selectCount());
        verifyNoInteractions(fileService);
        verifyNoInteractions(signatureService);
    }

    @Test
    @DisplayName("BDD: same execution, artifact and source hashes with regenerate=false returns existing SEALED archive")
    void generateExecutionArchive_sameSourceAndRegenerateFalse_returnsExistingArchiveWithoutRendererOrFileCall()
            throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        MesProBatchRecordApprovalSnapshotDO approvalSnapshot = insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        MesProBatchRecordExecutionArchiveDO existing = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, approvedSignatureHash(execution.getId()));
        StorageRetentionEvidence storageEvidence = storageEvidence(existing.getFileId());
        insertStorageRetentionMetadataEvent(existing, storageEvidence);
        when(fileService.requireStorageRetentionEvidence(eq(existing.getFileId()), any(StorageRetentionPolicy.class)))
                .thenReturn(storageEvidence);

        MesProBatchRecordExecutionArchiveRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = archiveService.generateExecutionArchive(generateReq(execution.getId(), false));
        }

        assertEquals(existing.getId(), response.getId());
        assertEquals(1, response.getArchiveVersion());
        assertEquals(approvalSnapshot.getId(), response.getApprovalSnapshotId());
        assertEquals(approvalSnapshot.getSnapshotHash(), response.getApprovalSnapshotHash());
        assertEquals(Boolean.FALSE, response.getCreated());
        assertEquals(1L, countArchives(execution.getId()));
        verify(fileService).requireStorageRetentionEvidence(eq(existing.getFileId()),
                argThat(policy -> matchesStoragePolicy(policy, storageEvidence)));
        verify(renderer, never()).render(any());
        verify(fileService, never()).createFileWithStorageRetention(any(), any(), any(), any(), any());
        verify(signatureService, never()).recordArchiveSealSignature(any(), any(), any());
    }

    @Test
    @DisplayName("BDD: same-source existing SEALED archive without storage retention metadata is rejected")
    void generateExecutionArchive_sameSourceWithoutStorageRetentionMetadata_rejectsExistingArchive() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        MesProBatchRecordExecutionArchiveDO existing = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, approvedSignatureHash(execution.getId()));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }

        assertEquals(1L, countArchives(execution.getId()));
        assertEventTypes(existing.getId(), "GENERATE_FAILED");
        verify(fileService, never()).requireStorageRetentionEvidence(anyLong(), any(StorageRetentionPolicy.class));
        verify(renderer, never()).render(any());
        verify(fileService, never()).createFileWithStorageRetention(any(), any(), any(), any(), any());
        verify(signatureService, never()).recordArchiveSealSignature(any(), any(), any());
    }

    @Test
    @DisplayName("BDD: approved snapshot without locked DomainTrace hash is rejected before archive side effects")
    void generateExecutionArchive_missingDomainTraceHashInApprovalSnapshot_rejectsBeforeArchiveSideEffects() {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        removeDomainTraceHashFromApprovalSnapshot(execution.getId());
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID);
        }

        assertEquals(0L, countArchives(execution.getId()));
        assertEquals(0L, archiveEventMapper.selectCount());
        verify(domainTraceService, never()).verifyForArchive(anyLong(), any());
        verify(renderer, never()).render(any());
        verify(fileService, never()).createFileWithStorageRetention(any(), any(), any(), any(), any());
        verify(signatureService, never()).recordArchiveSealSignature(any(), any(), any());
    }

    @Test
    @DisplayName("BDD: changed DomainTrace between approval and archive blocks generation before renderer or file writes")
    void generateExecutionArchive_changedDomainTraceRejectsBeforeArchiveSideEffects() {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        doThrow(new IllegalStateException("domain trace changed"))
                .when(domainTraceService).verifyForArchive(execution.getId(), DOMAIN_TRACE_HASH);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertThrows(RuntimeException.class,
                    () -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)));
        }

        assertEquals(0L, countArchives(execution.getId()));
        assertEquals(0L, archiveEventMapper.selectCount());
        verify(renderer, never()).render(any());
        verify(fileService, never()).createFileWithStorageRetention(any(), any(), any(), any(), any());
        verify(signatureService, never()).recordArchiveSealSignature(any(), any(), any());
    }

    @Test
    @DisplayName("BDD: existing SEALED archive with changed source hashes and regenerate=false requires explicit regenerate")
    void generateExecutionArchive_changedSourceAndRegenerateFalse_rejectsRegenerateRequired() {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v2", "cell-values-v2");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v2");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v2");
        insertSealedArchive(execution.getId(), 1, SNAPSHOT_HASH, CELL_VALUES_HASH, SIGNATURE_HASH);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_SOURCE_CHANGED_REGENERATE_REQUIRED);
        }

        assertEquals(1L, countArchives(execution.getId()));
        verify(renderer, never()).render(any());
        verify(fileService, never()).createFileWithStorageRetention(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("BDD: regenerate=true creates next version without overwriting existing SEALED version")
    void generateExecutionArchive_regenerateTrue_createsNextVersionAndKeepsExistingArchive() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v2", "cell-values-v2");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v2");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v2");
        MesProBatchRecordExecutionArchiveDO existing = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, SIGNATURE_HASH);
        mockSuccessfulRenderAndSeal(execution.getId(), 8802L);
        mockArchiveStorageRetention(storageEvidence(5002L));

        MesProBatchRecordExecutionArchiveRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = archiveService.generateExecutionArchive(generateReq(execution.getId(), true));
        }

        assertFalse(existing.getId().equals(response.getId()));
        assertEquals(2, response.getArchiveVersion());
        assertEquals(Boolean.TRUE, response.getCreated());
        assertEquals(2L, countArchives(execution.getId()));
        assertEquals("SEALED", archiveMapper.selectById(existing.getId()).getArchiveStatus());
    }

    @Test
    @DisplayName("BDD: SEALED archive download reads the same protected object version, verifies SHA-256 and records DOWNLOAD_SUCCESS")
    void downloadExecutionArchive_sealedArchiveWithMatchingChecksum_returnsBytesAndRecordsSuccess() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, SIGNATURE_HASH);
        StorageRetentionEvidence storageEvidence = storageEvidence(archive.getFileId());
        insertStorageRetentionMetadataEvent(archive, storageEvidence);
        when(fileService.requireStorageRetentionEvidence(eq(archive.getFileId()), any(StorageRetentionPolicy.class)))
                .thenReturn(storageEvidence);
        when(fileService.getFileContentWithStorageRetention(eq(archive.getFileId()), any(StorageRetentionPolicy.class)))
                .thenReturn(ARCHIVE_BYTES);

        MesProBatchRecordExecutionArchiveDownloadRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = archiveService.downloadExecutionArchive(archive.getId());
        }

        assertEquals(archive.getFileName(), response.getFileName());
        assertEquals(archive.getContentType(), response.getContentType());
        assertEquals(archive.getFileSize(), response.getFileSize());
        assertEquals(archive.getSha256(), response.getSha256());
        assertEquals(archive.getApprovalSnapshotId(), response.getApprovalSnapshotId());
        assertEquals(archive.getApprovalSnapshotHash(), response.getApprovalSnapshotHash());
        assertArrayEquals(ARCHIVE_BYTES, response.getContent());
        assertEventTypes(archive.getId(), "DOWNLOAD_SUCCESS");
        verify(permissionGateService).requireAbility(argThat(command ->
                "EXECUTION_ARCHIVE".equals(command.getObjectType())
                        && String.valueOf(archive.getId()).equals(command.getObjectId())
                        && "VIEW".equals(command.getAbility())
                        && archive.getExecutionId().equals(command.getExecutionId())
                        && "mes:pro-batch-record-execution-archive:download"
                        .equals(command.getPermissionCode())));
        InOrder inOrder = inOrder(fileService);
        inOrder.verify(fileService).requireStorageRetentionEvidence(eq(archive.getFileId()),
                any(StorageRetentionPolicy.class));
        inOrder.verify(fileService).getFileContentWithStorageRetention(eq(archive.getFileId()),
                any(StorageRetentionPolicy.class));
        verify(fileService).requireStorageRetentionEvidence(eq(archive.getFileId()),
                argThat(policy -> matchesStoragePolicy(policy, storageEvidence)));
        verify(fileService).getFileContentWithStorageRetention(eq(archive.getFileId()),
                argThat(policy -> matchesStoragePolicy(policy, storageEvidence)));
        verify(fileService, never()).getFileContent(anyLong(), any());
    }

    @Test
    @DisplayName("BDD: SEALED archive download without storage retention metadata is rejected before content is returned")
    void downloadExecutionArchive_missingStorageRetentionMetadata_rejectsAndRecordsFailure() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, SIGNATURE_HASH);

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.downloadExecutionArchive(archive.getId()),
                    PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }

        assertEventTypes(archive.getId(), "DOWNLOAD_FAILED");
        verify(fileService, never()).requireStorageRetentionEvidence(anyLong(), any(StorageRetentionPolicy.class));
        verify(fileService, never()).getFileContentWithStorageRetention(anyLong(), any(StorageRetentionPolicy.class));
        verify(fileService, never()).getFileContent(anyLong(), any());
    }

    @Test
    @DisplayName("BDD: SEALED archive download re-reads storage retention evidence and rejects verifier failure")
    void downloadExecutionArchive_storageRetentionVerifierFailure_rejectsAndRecordsFailure() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, SIGNATURE_HASH);
        insertStorageRetentionMetadataEvent(archive, storageEvidence(archive.getFileId()));
        when(fileService.requireStorageRetentionEvidence(eq(archive.getFileId()), any(StorageRetentionPolicy.class)))
                .thenThrow(new IllegalStateException("retention evidence lost"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.downloadExecutionArchive(archive.getId()),
                    PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }

        assertEventTypes(archive.getId(), "DOWNLOAD_FAILED");
        verify(fileService).requireStorageRetentionEvidence(eq(archive.getFileId()),
                any(StorageRetentionPolicy.class));
        verify(fileService, never()).getFileContentWithStorageRetention(anyLong(), any(StorageRetentionPolicy.class));
        verify(fileService, never()).getFileContent(anyLong(), any());
    }

    @Test
    @DisplayName("BDD: SEALED archive download rejects version-bound content read failure before checksum")
    void downloadExecutionArchive_versionBoundContentReadFailure_rejectsAndRecordsFailure() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, SIGNATURE_HASH);
        StorageRetentionEvidence storageEvidence = storageEvidence(archive.getFileId());
        insertStorageRetentionMetadataEvent(archive, storageEvidence);
        when(fileService.requireStorageRetentionEvidence(eq(archive.getFileId()), any(StorageRetentionPolicy.class)))
                .thenReturn(storageEvidence);
        when(fileService.getFileContentWithStorageRetention(eq(archive.getFileId()), any(StorageRetentionPolicy.class)))
                .thenThrow(new IllegalStateException("object version mismatch"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.downloadExecutionArchive(archive.getId()),
                    PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }

        assertEventTypes(archive.getId(), "DOWNLOAD_FAILED");
        verify(fileService).getFileContentWithStorageRetention(eq(archive.getFileId()),
                argThat(policy -> matchesStoragePolicy(policy, storageEvidence)));
        verify(fileService, never()).getFileContent(anyLong(), any());
    }

    @Test
    @DisplayName("BDD: download rejects checksum mismatch and records DOWNLOAD_FAILED")
    void downloadExecutionArchive_checksumMismatch_rejectsAndRecordsFailure() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        MesProBatchRecordExecutionArchiveDO archive = insertSealedArchive(execution.getId(), 1,
                SNAPSHOT_HASH, CELL_VALUES_HASH, SIGNATURE_HASH);
        StorageRetentionEvidence storageEvidence = storageEvidence(archive.getFileId());
        insertStorageRetentionMetadataEvent(archive, storageEvidence);
        when(fileService.requireStorageRetentionEvidence(eq(archive.getFileId()), any(StorageRetentionPolicy.class)))
                .thenReturn(storageEvidence);
        when(fileService.getFileContentWithStorageRetention(eq(archive.getFileId()), any(StorageRetentionPolicy.class)))
                .thenReturn("tampered".getBytes(StandardCharsets.UTF_8));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.downloadExecutionArchive(archive.getId()),
                    PRO_BATCH_RECORD_ARCHIVE_CHECKSUM_MISMATCH);
        }

        assertEventTypes(archive.getId(), "DOWNLOAD_FAILED");
        verify(fileService).getFileContentWithStorageRetention(eq(archive.getFileId()),
                argThat(policy -> matchesStoragePolicy(policy, storageEvidence)));
        verify(fileService, never()).getFileContent(anyLong(), any());
    }

    @Test
    @DisplayName("BDD: renderer unavailable fails fast and records failure event without placeholder success")
    void generateExecutionArchive_rendererUnavailable_failsFastAndRecordsFailureEvent() {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(renderer.getArtifactType()).thenReturn("EXCEL");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE);
        }

        assertEquals(0L, countSealedArchives(execution.getId()));
        assertFailureEvent(execution.getId(), PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE);
        verify(fileService, never()).createFileWithStorageRetention(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("BDD: generation rejects missing storage retention evidence before SEALED or GENERATE_SUCCESS")
    void generateExecutionArchive_missingStorageRetentionEvidence_failsFastBeforeSealedOrSuccessEvent()
            throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(renderer.render(any())).thenReturn(renderResult());
        StorageRetentionPolicy uploadPolicy = mockArchiveStorageUploadPolicy();
        when(fileService.createFileWithStorageRetention(eq(PROTECTED_FILE_CONFIG_ID), eq(ARCHIVE_BYTES),
                eq("edhr-execution-v1.pdf"), eq("mes/edhr/archive"), eq(CONTENT_TYPE), same(uploadPolicy)))
                .thenReturn(storageEvidence(5005L).setObjectVersionId(null));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        }

        assertEquals(0L, countSealedArchives(execution.getId()));
        assertFailureEvent(execution.getId(), PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED);
        assertNoEventType(execution.getId(), "GENERATE_SUCCESS");
        verify(signatureService, never()).recordArchiveSealSignature(any(), any(), any());
        verify(fileService, never()).createFileAndReturnId(any(byte[].class), any(), any(), any());
    }

    @Test
    @DisplayName("BDD: FileService persistence failure fails fast and records failure event without placeholder success")
    void generateExecutionArchive_fileServiceFailure_failsFastAndRecordsFailureEvent() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(renderer.render(any())).thenReturn(renderResult());
        StorageRetentionPolicy uploadPolicy = mockArchiveStorageUploadPolicy();
        when(fileService.createFileWithStorageRetention(eq(PROTECTED_FILE_CONFIG_ID), eq(ARCHIVE_BYTES),
                eq("edhr-execution-v1.pdf"), eq("mes/edhr/archive"), eq(CONTENT_TYPE), same(uploadPolicy)))
                .thenThrow(new IllegalStateException("storage down"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED);
        }

        assertEquals(0L, countSealedArchives(execution.getId()));
        assertFailureEvent(execution.getId(), PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED);
        verify(signatureService, never()).recordArchiveSealSignature(any(), any(), any());
    }

    @Test
    @DisplayName("BDD: seal signature failure fails fast and records failure event without placeholder success")
    void generateExecutionArchive_signatureFailure_failsFastAndRecordsFailureEvent() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(renderer.render(any())).thenReturn(renderResult());
        mockArchiveStorageRetention(storageEvidence(5003L));
        when(signatureService.recordArchiveSealSignature(execution.getId(), "seal-secret", "seal note"))
                .thenThrow(new IllegalArgumentException("bad signature"));

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
        }

        assertEquals(0L, countSealedArchives(execution.getId()));
        assertFailureEvent(execution.getId(), PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
        verify(fileService).deleteFile(5003L);
    }

    @Test
    @DisplayName("BDD: seal signature failure cleanup failure is not swallowed")
    void generateExecutionArchive_signatureFailureAndFileCleanupFailure_failsFastWithCleanupError() throws Exception {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");
        when(renderer.render(any())).thenReturn(renderResult());
        mockArchiveStorageRetention(storageEvidence(5004L));
        when(signatureService.recordArchiveSealSignature(execution.getId(), "seal-secret", "seal note"))
                .thenThrow(new IllegalArgumentException("bad signature"));
        doThrow(new IllegalStateException("delete file failed")).when(fileService).deleteFile(5004L);

        RuntimeException thrown;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            thrown = assertThrows(RuntimeException.class,
                    () -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)));
        }

        assertTrue(thrown.getMessage().contains("delete file failed"));
        assertEquals(0L, countSealedArchives(execution.getId()));
        assertFailureEvent(execution.getId(), PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED);
        verify(fileService).deleteFile(5004L);
    }

    @Test
    @DisplayName("BDD: archive requires SUBMIT and APPROVE signatures to bind the same process instance")
    void generateExecutionArchive_submitSignatureDifferentProcess_rejectsBeforeArchiveSideEffects() {
        MesProBatchRecordExecutionDO execution = insertApprovedClosedExecution("snapshot-v1", "cell-values-v1");
        insertApprovalSnapshot(execution);
        MesProBatchRecordExecutionSignatureDO submitSignature =
                insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        signatureMapper.updateById(new MesProBatchRecordExecutionSignatureDO()
                .setId(submitSignature.getId())
                .setProcessInstanceId("edhr-pi-other"));
        insertSignature(execution.getId(), "APPROVE", "approve-signature-v1");

        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            assertServiceException(() -> archiveService.generateExecutionArchive(generateReq(execution.getId(), false)),
                    PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_CLOSED);
        }

        assertEquals(0L, countArchives(execution.getId()));
        verify(renderer, never()).render(any());
        verify(fileService, never()).createFileWithStorageRetention(any(), any(), any(), any(), any());
        verify(signatureService, never()).recordArchiveSealSignature(any(), any(), any());
    }

    @Test
    @DisplayName("BDD: fill-completed ordinary execution archives with SUBMIT evidence and no process approval")
    void generateExecutionArchive_fillCompletedOrdinaryExecutionUsesSubmitEvidenceWithoutApprove() throws Exception {
        MesProBatchRecordExecutionDO execution = insertFillCompletedOrdinaryExecution("snapshot-v1", "cell-values-v1");
        insertSignature(execution.getId(), "SUBMIT", "submit-signature-v1");
        when(renderer.render(any())).thenReturn(renderResult());
        when(signatureService.recordArchiveSealSignature(execution.getId(), "seal-secret", "seal note"))
                .thenReturn(8805L);
        mockArchiveStorageRetention(storageEvidence(5005L));

        MesProBatchRecordExecutionArchiveRespVO response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockLoginUser()) {
            response = archiveService.generateExecutionArchive(generateReq(execution.getId(), false));
        }

        assertNotNull(response.getId());
        MesProBatchRecordExecutionArchiveDO archive = archiveMapper.selectById(response.getId());
        assertEquals(approvedSignatureHash(execution.getId()), archive.getSignatureHash());
        assertEquals(0L, approvalSnapshotMapper.selectCount());
        verify(renderer).render(any());
        verify(fileService).createFileWithStorageRetention(eq(PROTECTED_FILE_CONFIG_ID), eq(ARCHIVE_BYTES),
                eq("edhr-execution-v1.pdf"), eq("mes/edhr/archive"), eq(CONTENT_TYPE), any());
        verify(signatureService).recordArchiveSealSignature(execution.getId(), "seal-secret", "seal note");
    }

    private void mockSuccessfulRenderAndSeal(Long executionId, Long sealSignatureId) throws Exception {
        when(renderer.render(any())).thenReturn(renderResult());
        when(signatureService.recordArchiveSealSignature(executionId, "seal-secret", "seal note"))
                .thenReturn(sealSignatureId);
    }

    private void mockArchiveStorageRetention(StorageRetentionEvidence storageEvidence) {
        StorageRetentionPolicy uploadPolicy = mockArchiveStorageUploadPolicy();
        when(fileService.createFileWithStorageRetention(eq(PROTECTED_FILE_CONFIG_ID), eq(ARCHIVE_BYTES),
                eq("edhr-execution-v1.pdf"), eq("mes/edhr/archive"), eq(CONTENT_TYPE), same(uploadPolicy)))
                .thenReturn(storageEvidence);
    }

    private StorageRetentionPolicy mockArchiveStorageUploadPolicy() {
        StorageRetentionPolicy uploadPolicy = uploadStoragePolicy();
        when(protectedStorage.getFileConfigId()).thenReturn(PROTECTED_FILE_CONFIG_ID);
        when(protectedStorage.requireUploadPolicy(eq(ARCHIVE_SHA256))).thenReturn(uploadPolicy);
        return uploadPolicy;
    }

    private StorageRetentionPolicy uploadStoragePolicy() {
        return new StorageRetentionPolicy()
                .setObjectLockRequired(Boolean.TRUE)
                .setRetentionMode(STORAGE_RETENTION_MODE)
                .setRetentionDays(3650)
                .setLegalHoldRequired(Boolean.TRUE)
                .setChecksumSha256(ARCHIVE_SHA256);
    }

    private StorageRetentionEvidence storageEvidence(Long fileId) {
        return new StorageRetentionEvidence()
                .setFileId(fileId)
                .setBucket(STORAGE_BUCKET)
                .setPath(STORAGE_PATH)
                .setKey(STORAGE_PATH)
                .setObjectVersionId("object-version-" + fileId)
                .setRetentionMode(STORAGE_RETENTION_MODE)
                .setRetainUntil(STORAGE_RETAIN_UNTIL)
                .setLegalHoldStatus(STORAGE_LEGAL_HOLD_STATUS)
                .setVerifiedAt(STORAGE_VERIFIED_AT)
                .setChecksumSha256(ARCHIVE_SHA256);
    }

    private void insertStorageRetentionMetadataEvent(MesProBatchRecordExecutionArchiveDO archive,
                                                     StorageRetentionEvidence storageEvidence) {
        JSONObject storageRetention = new JSONObject();
        storageRetention.put("objectLock", true);
        storageRetention.put("legalHold", true);
        storageRetention.put("fileId", storageEvidence.getFileId());
        storageRetention.put("bucket", storageEvidence.getBucket());
        storageRetention.put("path", storageEvidence.getPath());
        storageRetention.put("key", storageEvidence.getKey());
        storageRetention.put("objectVersionId", storageEvidence.getObjectVersionId());
        storageRetention.put("retentionMode", storageEvidence.getRetentionMode());
        storageRetention.put("retainUntil", storageEvidence.getRetainUntil().toString());
        storageRetention.put("legalHoldStatus", storageEvidence.getLegalHoldStatus());
        storageRetention.put("verifiedAt", storageEvidence.getVerifiedAt().toString());
        storageRetention.put("sha256", storageEvidence.getChecksumSha256());
        JSONObject metadata = new JSONObject();
        metadata.put("storageRetention", storageRetention);
        archiveEventMapper.insert(MesProBatchRecordExecutionArchiveEventDO.builder()
                .archiveId(archive.getId())
                .executionId(archive.getExecutionId())
                .eventType("GENERATE_SUCCESS")
                .actorId(ACTOR_ID)
                .eventTime(LocalDateTime.now())
                .message("archive generated with storage retention evidence")
                .metadataJson(metadata.toJSONString())
                .build());
    }

    private MesProBatchRecordExecutionArchiveRenderResult renderResult() {
        return MesProBatchRecordExecutionArchiveRenderResult.builder()
                .fileName("edhr-execution-v1.pdf")
                .contentType(CONTENT_TYPE)
                .fileSize((long) ARCHIVE_BYTES.length)
                .sha256(ARCHIVE_SHA256)
                .renderSourceVersion(RENDER_SOURCE_VERSION)
                .content(ARCHIVE_BYTES)
                .build();
    }

    private MesProBatchRecordExecutionArchiveGenerateReqVO generateReq(Long executionId, boolean regenerate) {
        MesProBatchRecordExecutionArchiveGenerateReqVO reqVO = new MesProBatchRecordExecutionArchiveGenerateReqVO();
        reqVO.setExecutionId(executionId);
        reqVO.setArtifactType(ARTIFACT_TYPE);
        reqVO.setSealPassword("seal-secret");
        reqVO.setComment("seal note");
        reqVO.setRegenerate(regenerate);
        return reqVO;
    }

    private MesProBatchRecordExecutionDO insertExecution(Integer status, String snapshotSource, String cellValuesSource) {
        String cellValuesJson = "{\"source\":\"" + cellValuesSource + "\"}";
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("EXE-" + System.nanoTime())
                .templateId(10L)
                .templateCode("TPL-EDHR")
                .templateName("EDHR")
                .workOrderId(20L)
                .workOrderCode("MO-20260524")
                .batchCode("BATCH-20260524")
                .status(status)
                .sheetLayoutJson("{\"sheet\":\"main\"}")
                .metaJson("{\"version\":\"v1\"}")
                .executionSnapshotJson("{\"source\":\"" + snapshotSource + "\"}")
                .cellValuesJson(cellValuesJson)
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(cellValuesJson))
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .remark("execution remark")
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private MesProBatchRecordExecutionDO insertApprovedClosedExecution(String snapshotSource, String cellValuesSource) {
        MesProBatchRecordExecutionDO execution = insertExecution(3, snapshotSource, cellValuesSource);
        setIfPresent(execution, "processDefinitionKey", "mes-edhr-approval-v1");
        setIfPresent(execution, "processInstanceId", "edhr-pi-" + execution.getId());
        setIfPresent(execution, "submittedBy", ACTOR_ID);
        setIfPresent(execution, "submittedAt", LocalDateTime.now().minusHours(2));
        setIfPresent(execution, "approvedBy", ACTOR_ID);
        setIfPresent(execution, "approvedAt", LocalDateTime.now().minusHours(1));
        setIfPresent(execution, "closedAt", LocalDateTime.now().minusHours(1));
        executionMapper.updateById(execution);
        return execution;
    }

    private MesProBatchRecordExecutionDO insertFillCompletedOrdinaryExecution(String snapshotSource, String cellValuesSource) {
        MesProBatchRecordExecutionDO execution = insertExecution(
                MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED, snapshotSource, cellValuesSource);
        setIfPresent(execution, "submittedBy", ACTOR_ID);
        setIfPresent(execution, "submittedAt", LocalDateTime.now().minusHours(2));
        setIfPresent(execution, "closedAt", LocalDateTime.now().minusHours(1));
        executionMapper.updateById(execution);
        return execution;
    }

    private MesProBatchRecordExecutionSignatureDO insertSignature(Long executionId, String actionType, String signatureSource) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        MesProBatchRecordExecutionSignatureDO signature = MesProBatchRecordExecutionSignatureDO.builder()
                .executionId(executionId)
                .actorId(ACTOR_ID)
                .actionType(actionType)
                .signatureMode("PASSWORD")
                .passwordVerified(Boolean.TRUE)
                .comment(signatureSource)
                .signedAt(LocalDateTime.now())
                .fieldAuditRevision(execution.getFieldAuditRevision())
                .fieldAuditHeadHash(execution.getFieldAuditHeadHash())
                .cellValuesHash(execution.getCellValuesHash())
                .build();
        setIfPresent(signature, "processInstanceId", "edhr-pi-" + executionId);
        if ("APPROVE".equals(actionType)) {
            setIfPresent(signature, "bpmTaskId", "edhr-task-" + executionId);
            setIfPresent(signature, "approvalResult", "APPROVE");
        }
        signatureMapper.insert(signature);
        return signature;
    }

    private MesProBatchRecordApprovalSnapshotDO insertApprovalSnapshot(MesProBatchRecordExecutionDO execution) {
        MesProBatchRecordApprovalSnapshotDO snapshot = MesProBatchRecordApprovalSnapshotDO.builder()
                .executionId(execution.getId())
                .processDefinitionKey("mes-edhr-approval-v1")
                .processDefinitionId("edhr-def-v1")
                .processInstanceId("edhr-pi-" + execution.getId())
                .approvalStatus("APPROVED")
                .snapshotJson("{\"executionId\":" + execution.getId()
                        + ",\"domainTraceSnapshotId\":" + DOMAIN_TRACE_SNAPSHOT_ID
                        + ",\"domainTraceHash\":\"" + DOMAIN_TRACE_HASH + "\""
                        + ",\"domainTraceStatus\":\"VERIFIED\"}")
                .snapshotHash(SNAPSHOT_HASH)
                .submitSignatureId(1L)
                .approveSignatureId(2L)
                .submittedBy(ACTOR_ID)
                .submittedAt(LocalDateTime.now().minusHours(2))
                .approvedBy(ACTOR_ID)
                .approvedAt(LocalDateTime.now().minusHours(1))
                .closedAt(LocalDateTime.now().minusHours(1))
                .build();
        approvalSnapshotMapper.insert(snapshot);
        return snapshot;
    }

    private MesProBatchRecordExecutionAttachmentDO insertAttachment(MesProBatchRecordExecutionDO execution) {
        MesProBatchRecordExecutionAttachmentDO attachment = MesProBatchRecordExecutionAttachmentDO.builder()
                .executionId(execution.getId())
                .batchExecutionId(41L)
                .batchTaskId(42L)
                .workTaskId(31L)
                .rowIndex(1)
                .columnIndex(2)
                .fieldKey("visualEvidence")
                .fieldPath("sheet.main.rows[1].cells[2]")
                .fieldLabel("现场图片")
                .attachmentType("IMAGE")
                .attachmentGroupKey("R1C2-IMG-1")
                .attachmentAction("ADD")
                .versionNo(1)
                .fileId(6001L)
                .fileUrl("http://127.0.0.1:9000/yudao/edhr/evidence.png")
                .storageConfigId(28L)
                .storagePath("edhr/evidence.png")
                .fileName("evidence.png")
                .contentType("image/png")
                .fileSize(2048L)
                .sha256("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .storageRetentionHash("retention-hash")
                .auditBatchId(7001L)
                .signatureId(8001L)
                .previousAttachmentHash(null)
                .attachmentHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
                .operatorId(ACTOR_ID)
                .operatorName("QA")
                .operatedAt(LocalDateTime.now().minusMinutes(30))
                .reasonCategory("CORRECTION")
                .reasonText("operator correction")
                .build();
        attachmentMapper.insert(attachment);
        return attachment;
    }

    private void removeDomainTraceHashFromApprovalSnapshot(Long executionId) {
        MesProBatchRecordApprovalSnapshotDO snapshot = approvalSnapshotMapper.selectByExecutionId(executionId);
        JSONObject snapshotJson = JSON.parseObject(snapshot.getSnapshotJson());
        snapshotJson.remove("domainTraceHash");
        approvalSnapshotMapper.updateById(new MesProBatchRecordApprovalSnapshotDO()
                .setId(snapshot.getId())
                .setSnapshotJson(snapshotJson.toJSONString()));
    }

    private MesProBatchRecordExecutionArchiveDO insertSealedArchive(Long executionId, Integer version,
                                                                    String snapshotHash, String cellValuesHash,
                                                                    String signatureHash) {
        MesProBatchRecordApprovalSnapshotDO approvalSnapshot = approvalSnapshotMapper.selectByExecutionId(executionId);
        MesProBatchRecordExecutionArchiveDO archive = MesProBatchRecordExecutionArchiveDO.builder()
                .executionId(executionId)
                .archiveCode("EDHRA-" + System.nanoTime())
                .archiveVersion(version)
                .artifactType(ARTIFACT_TYPE)
                .archiveStatus("SEALED")
                .fileId(5000L + version)
                .fileName("edhr-execution-v" + version + ".pdf")
                .contentType(CONTENT_TYPE)
                .fileSize((long) ARCHIVE_BYTES.length)
                .sha256(ARCHIVE_SHA256)
                .renderSourceVersion(RENDER_SOURCE_VERSION)
                .executionSnapshotHash(snapshotHash)
                .cellValuesHash(cellValuesHash)
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .signatureHash(signatureHash)
                .approvalSnapshotId(approvalSnapshot == null ? 9900L + version : approvalSnapshot.getId())
                .approvalSnapshotHash(approvalSnapshot == null ? SNAPSHOT_HASH : approvalSnapshot.getSnapshotHash())
                .sealSignatureId(8800L + version)
                .generatedBy(ACTOR_ID)
                .generatedAt(LocalDateTime.now().minusMinutes(10))
                .sealedBy(ACTOR_ID)
                .sealedAt(LocalDateTime.now().minusMinutes(9))
                .remark("existing archive")
                .build();
        archiveMapper.insert(archive);
        return archive;
    }

    private void assertEventTypes(Long archiveId, String... expectedTypes) {
        List<String> actualTypes = archiveEventMapper.selectListByArchiveId(archiveId).stream()
                .map(MesProBatchRecordExecutionArchiveEventDO::getEventType)
                .toList();
        for (String expectedType : expectedTypes) {
            assertTrue(actualTypes.contains(expectedType), "Expected archive event type: " + expectedType);
        }
    }

    private MesProBatchRecordExecutionArchiveEventDO requireArchiveEvent(Long archiveId, String eventType) {
        return archiveEventMapper.selectListByArchiveId(archiveId).stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected archive event type: " + eventType));
    }

    private void assertNoEventType(Long executionId, String eventType) {
        Long count = archiveEventMapper.selectCount(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveEventDO>()
                .eq(MesProBatchRecordExecutionArchiveEventDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionArchiveEventDO::getEventType, eventType));
        assertEquals(0L, count, "Expected no archive event type: " + eventType);
    }

    private boolean matchesStoragePolicy(StorageRetentionPolicy policy, StorageRetentionEvidence storageEvidence) {
        return policy != null
                && Boolean.TRUE.equals(policy.getObjectLockRequired())
                && STORAGE_RETENTION_MODE.equals(policy.getRetentionMode())
                && STORAGE_RETAIN_UNTIL.equals(policy.getRetainUntil())
                && Boolean.TRUE.equals(policy.getLegalHoldRequired())
                && storageEvidence.getObjectVersionId().equals(policy.getObjectVersionId())
                && ARCHIVE_SHA256.equals(policy.getChecksumSha256());
    }

    private boolean matchesUploadStoragePolicy(StorageRetentionPolicy policy) {
        return policy != null
                && Boolean.TRUE.equals(policy.getObjectLockRequired())
                && STORAGE_RETENTION_MODE.equals(policy.getRetentionMode())
                && Integer.valueOf(3650).equals(policy.getRetentionDays())
                && Boolean.TRUE.equals(policy.getLegalHoldRequired())
                && policy.getObjectVersionId() == null
                && ARCHIVE_SHA256.equals(policy.getChecksumSha256());
    }

    private Long countArchives(Long executionId) {
        return archiveMapper.selectCount(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                .eq(MesProBatchRecordExecutionArchiveDO::getExecutionId, executionId));
    }

    private Long countSealedArchives(Long executionId) {
        return archiveMapper.selectCount(new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveDO>()
                .eq(MesProBatchRecordExecutionArchiveDO::getExecutionId, executionId)
                .eq(MesProBatchRecordExecutionArchiveDO::getArchiveStatus, "SEALED"));
    }

    private void assertFailureEvent(Long executionId, ErrorCode expectedCode) {
        List<MesProBatchRecordExecutionArchiveEventDO> events = archiveEventMapper.selectList(
                new LambdaQueryWrapperX<MesProBatchRecordExecutionArchiveEventDO>()
                        .eq(MesProBatchRecordExecutionArchiveEventDO::getExecutionId, executionId)
                        .eq(MesProBatchRecordExecutionArchiveEventDO::getEventType, "GENERATE_FAILED"));
        assertFalse(events.isEmpty(), "Expected GENERATE_FAILED event for execution " + executionId);
        assertTrue(events.stream().anyMatch(event -> event.getMessage() != null
                        && event.getMessage().contains(String.valueOf(expectedCode.getCode()))),
                "Expected failure event message to include error code " + expectedCode.getCode());
    }

    private String approvedSignatureHash(Long executionId) {
        return sha256(signatureMapper.selectListByExecutionId(executionId).stream()
                .filter(signature -> "SUBMIT".equals(signature.getActionType())
                        || "APPROVE".equals(signature.getActionType()))
                .sorted(Comparator.comparing(MesProBatchRecordExecutionSignatureDO::getSignedAt,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MesProBatchRecordExecutionSignatureDO::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::signatureProjection)
                .toList()
                .stream()
                .reduce((left, right) -> left + "\n" + right)
                .orElse(""));
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

    private void setIfPresent(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException ignored) {
            // RED stage can run before the new persistence fields exist.
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Failed to set field " + fieldName, ex);
        }
    }

    private MockedStatic<SecurityFrameworkUtils> mockLoginUser() {
        MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(ACTOR_ID);
        return security;
    }

    private static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 calculation failed", ex);
        }
    }
}
