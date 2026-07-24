package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProBatchRecordExecutionAttachmentServiceImpl.class, MesProEdhrGoldenFingerPermissionService.class})
class MesProBatchRecordExecutionAttachmentServiceTest extends BaseDbUnitTest {

    private static final Long TENANT_ID = 122L;
    private static final String FILE_SHA256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Resource
    private MesProBatchRecordExecutionAttachmentService attachmentService;
    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @MockitoBean
    private FileService fileService;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private RoleApi roleApi;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void bindAttachment_createsAppendOnlyLedgerWithHashChain() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        Long firstId = attachmentService.bindAttachment(baseCommand(execution, workTask));
        Long secondId = attachmentService.bindAttachment(baseCommand(execution, workTask)
                .setFileId(902L)
                .setStoragePath("edhr/501/evidence-2.png")
                .setFileName("evidence-2.png"));

        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionField(execution.getId(),
                        "sheet[0].rows[1].cells[2].visualEvidence", "visualEvidence");

        assertEquals(2, attachments.size());
        assertEquals(firstId, attachments.get(0).getId());
        assertEquals("ADD", attachments.get(0).getAttachmentAction());
        assertEquals(1, attachments.get(0).getVersionNo());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH,
                attachments.get(0).getPreviousAttachmentHash());
        assertNotNull(attachments.get(0).getAttachmentHash());
        assertEquals(64, attachments.get(0).getAttachmentHash().length());
        assertEquals(secondId, attachments.get(1).getId());
        assertEquals(2, attachments.get(1).getVersionNo());
        assertEquals(attachments.get(0).getAttachmentHash(), attachments.get(1).getPreviousAttachmentHash());
        assertEquals(99L, attachments.get(1).getOperatorId());
        assertEquals("aoteman", attachments.get(1).getOperatorName());
    }

    @Test
    void prepareUpload_returnsStructuredMetadataWithSha256() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        byte[] content = "edhr image bytes".getBytes(StandardCharsets.UTF_8);
        when(fileService.createFileAndReturnId(eq(content), eq("evidence.png"),
                eq("edhr/executions/" + execution.getId() + "/attachments"), eq("image/png")))
                .thenReturn(901L);
        when(fileService.getFile(901L)).thenReturn(FileDO.builder()
                .id(901L)
                .configId(28L)
                .name("evidence.png")
                .path("edhr/executions/" + execution.getId() + "/attachments/evidence.png")
                .url("http://127.0.0.1:9000/yudao/edhr/evidence.png")
                .type("image/png")
                .size((long) content.length)
                .build());

        MesProBatchRecordExecutionAttachmentPrepareUploadResult result =
                attachmentService.prepareUpload(new MesProBatchRecordExecutionAttachmentPrepareUploadCommand()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(workTask.getId())
                        .setOperatorId(99L)
                        .setFileName("evidence.png")
                        .setContentType("image/png")
                        .setContent(content));

        assertEquals(901L, result.getFileId());
        assertEquals(28L, result.getStorageConfigId());
        assertEquals("edhr/executions/" + execution.getId() + "/attachments/evidence.png",
                result.getStoragePath());
        assertEquals("http://127.0.0.1:9000/yudao/edhr/evidence.png", result.getFileUrl());
        assertEquals("evidence.png", result.getFileName());
        assertEquals("image/png", result.getContentType());
        assertEquals(content.length, result.getFileSize());
        assertEquals(MesProBatchRecordExecutionFieldAuditHasher.sha256("edhr image bytes"), result.getSha256());
        assertNotNull(result.getUploadToken());
        assertTrue(result.getUploadToken().contains(result.getSha256()));
    }

    @Test
    void prepareUpload_allowsEdhrBatchTaskDifferentFromProductionTask() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L, 1701L);
        byte[] content = "edhr batch attachment".getBytes(StandardCharsets.UTF_8);
        when(fileService.createFileAndReturnId(eq(content), eq("batch-evidence.txt"),
                eq("edhr/executions/" + execution.getId() + "/attachments"), eq("text/plain")))
                .thenReturn(902L);
        when(fileService.getFile(902L)).thenReturn(FileDO.builder()
                .id(902L)
                .configId(28L)
                .name("batch-evidence.txt")
                .path("edhr/executions/" + execution.getId() + "/attachments/batch-evidence.txt")
                .url("http://127.0.0.1:9000/yudao/edhr/batch-evidence.txt")
                .type("text/plain")
                .size((long) content.length)
                .build());

        MesProBatchRecordExecutionAttachmentPrepareUploadResult result =
                attachmentService.prepareUpload(new MesProBatchRecordExecutionAttachmentPrepareUploadCommand()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(workTask.getId())
                        .setOperatorId(99L)
                        .setFileName("batch-evidence.txt")
                        .setContentType("text/plain")
                        .setContent(content));

        assertEquals(902L, result.getFileId());
    }

    @Test
    void prepareUpload_allowsOverdueWorkTask() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        workTask.setStatus(MesProEdhrWorkTaskStatus.OVERDUE);
        workTaskMapper.updateById(workTask);
        byte[] content = "overdue edhr attachment".getBytes(StandardCharsets.UTF_8);
        when(fileService.createFileAndReturnId(eq(content), eq("overdue-evidence.txt"),
                eq("edhr/executions/" + execution.getId() + "/attachments"), eq("text/plain")))
                .thenReturn(903L);
        when(fileService.getFile(903L)).thenReturn(FileDO.builder()
                .id(903L)
                .configId(28L)
                .name("overdue-evidence.txt")
                .path("edhr/executions/" + execution.getId() + "/attachments/overdue-evidence.txt")
                .url("http://127.0.0.1:9000/yudao/edhr/overdue-evidence.txt")
                .type("text/plain")
                .size((long) content.length)
                .build());

        MesProBatchRecordExecutionAttachmentPrepareUploadResult result =
                attachmentService.prepareUpload(new MesProBatchRecordExecutionAttachmentPrepareUploadCommand()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(workTask.getId())
                        .setOperatorId(99L)
                        .setFileName("overdue-evidence.txt")
                        .setContentType("text/plain")
                        .setContent(content));

        assertEquals(903L, result.getFileId());
    }

    @Test
    void prepareUpload_allowsCandidatePoolFillMember() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L)
                .setCandidateSourceType("ROLE_GROUP")
                .setCandidateSourceId(7001L)
                .setCandidateUserSnapshot("99,100");
        workTaskMapper.updateById(workTask);
        byte[] content = "candidate edhr attachment".getBytes(StandardCharsets.UTF_8);
        when(fileService.createFileAndReturnId(eq(content), eq("candidate-evidence.txt"),
                eq("edhr/executions/" + execution.getId() + "/attachments"), eq("text/plain")))
                .thenReturn(904L);
        when(fileService.getFile(904L)).thenReturn(FileDO.builder()
                .id(904L)
                .configId(28L)
                .name("candidate-evidence.txt")
                .path("edhr/executions/" + execution.getId() + "/attachments/candidate-evidence.txt")
                .url("http://127.0.0.1:9000/yudao/edhr/candidate-evidence.txt")
                .type("text/plain")
                .size((long) content.length)
                .build());

        MesProBatchRecordExecutionAttachmentPrepareUploadResult result =
                attachmentService.prepareUpload(new MesProBatchRecordExecutionAttachmentPrepareUploadCommand()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(workTask.getId())
                        .setOperatorId(100L)
                        .setFileName("candidate-evidence.txt")
                        .setContentType("text/plain")
                        .setContent(content));

        assertEquals(904L, result.getFileId());
    }

    @Test
    void prepareUpload_rejectsEmptyContent() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.prepareUpload(new MesProBatchRecordExecutionAttachmentPrepareUploadCommand()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(workTask.getId())
                        .setOperatorId(99L)
                        .setFileName("empty.png")
                        .setContentType("image/png")
                        .setContent(new byte[0])));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID.getCode(), exception.getCode());
        verify(fileService, never()).createFileAndReturnId(eq(new byte[0]), eq("empty.png"),
                eq("edhr/executions/" + execution.getId() + "/attachments"), eq("image/png"));
    }

    @Test
    void prepareUpload_rejectsIncompleteFileServiceMetadata() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        byte[] content = "edhr image bytes".getBytes(StandardCharsets.UTF_8);
        when(fileService.createFileAndReturnId(eq(content), eq("evidence.png"),
                eq("edhr/executions/" + execution.getId() + "/attachments"), eq("image/png")))
                .thenReturn(901L);
        when(fileService.getFile(901L)).thenReturn(FileDO.builder()
                .id(901L)
                .configId(28L)
                .name("evidence.png")
                .type("image/png")
                .size((long) content.length)
                .build());

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.prepareUpload(new MesProBatchRecordExecutionAttachmentPrepareUploadCommand()
                        .setExecutionId(execution.getId())
                        .setWorkTaskId(workTask.getId())
                        .setOperatorId(99L)
                        .setFileName("evidence.png")
                        .setContentType("image/png")
                        .setContent(content)));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID.getCode(), exception.getCode());
    }

    @Test
    void bindAttachment_rejectsIncompleteFileMetadata() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.bindAttachment(baseCommand(execution, workTask).setSha256("")));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID.getCode(), exception.getCode());
        assertEquals(0, attachmentMapper.selectListByExecutionId(execution.getId()).size());
    }

    @Test
    void bindAttachment_rejectsSubmittedExecution() {
        MesProBatchRecordExecutionDO execution = insertExecution(1, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.bindAttachment(baseCommand(execution, workTask)));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID.getCode(), exception.getCode());
        assertEquals(0, attachmentMapper.selectListByExecutionId(execution.getId()).size());
    }

    @Test
    void bindAttachment_rejectsTaskAssigneeMismatch() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 100L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.bindAttachment(baseCommand(execution, workTask)));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_WORK_TASK_INVALID.getCode(), exception.getCode());
        assertEquals(0, attachmentMapper.selectListByExecutionId(execution.getId()).size());
    }

    @Test
    void bindAttachment_rejectsNonUploadFieldRule() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true, "input-text", "image/png", 4096L);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.bindAttachment(baseCommand(execution, workTask)));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID.getCode(), exception.getCode());
        assertEquals(0, attachmentMapper.selectListByExecutionId(execution.getId()).size());
    }

    @Test
    void bindAttachment_rejectsDisallowedContentType() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true, "upload-image", "image/png", 4096L);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.bindAttachment(baseCommand(execution, workTask)
                        .setContentType("application/pdf")));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION.getCode(), exception.getCode());
        assertEquals(0, attachmentMapper.selectListByExecutionId(execution.getId()).size());
    }

    @Test
    void bindAttachment_rejectsFileSizeOverFieldLimit() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true, "upload-image", "image/png", 1024L);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.bindAttachment(baseCommand(execution, workTask)
                        .setFileSize(2048L)));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION.getCode(), exception.getCode());
        assertEquals(0, attachmentMapper.selectListByExecutionId(execution.getId()).size());
    }

    @Test
    void verifyAttachmentChain_returnsValidForContinuousLedger() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        attachmentService.bindAttachment(baseCommand(execution, workTask));
        attachmentService.bindAttachment(baseCommand(execution, workTask)
                .setFileId(902L)
                .setStoragePath("edhr/501/evidence-2.png")
                .setFileName("evidence-2.png"));

        MesProBatchRecordExecutionAttachmentChainVerifyResult result =
                attachmentService.verifyAttachmentChain(execution.getId());

        assertTrue(result.isValid());
        assertEquals(2, result.getCheckedEventCount());
        assertEquals(0, result.getIssues().size());
        assertNotNull(result.getHeadHash());
        assertEquals(64, result.getHeadHash().length());
    }

    @Test
    void verifyAttachmentChain_reportsTamperedHash() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        attachmentService.bindAttachment(baseCommand(execution, workTask));
        MesProBatchRecordExecutionAttachmentDO attachment =
                attachmentMapper.selectListByExecutionId(execution.getId()).get(0);
        attachmentMapper.updateById(new MesProBatchRecordExecutionAttachmentDO()
                .setId(attachment.getId())
                .setAttachmentHash("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"));

        MesProBatchRecordExecutionAttachmentChainVerifyResult result =
                attachmentService.verifyAttachmentChain(execution.getId());

        assertFalse(result.isValid());
        assertEquals(1, result.getCheckedEventCount());
        assertEquals("HASH_MISMATCH", result.getIssues().get(0).getIssueCode());
        assertEquals(attachment.getId(), result.getIssues().get(0).getAttachmentId());
    }

    @Test
    void verifyAttachmentChain_reportsBrokenPreviousHash() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        attachmentService.bindAttachment(baseCommand(execution, workTask));
        attachmentService.bindAttachment(baseCommand(execution, workTask)
                .setFileId(902L)
                .setStoragePath("edhr/501/evidence-2.png")
                .setFileName("evidence-2.png"));
        MesProBatchRecordExecutionAttachmentDO second =
                attachmentMapper.selectListByExecutionId(execution.getId()).get(1);
        attachmentMapper.updateById(new MesProBatchRecordExecutionAttachmentDO()
                .setId(second.getId())
                .setPreviousAttachmentHash("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"));

        MesProBatchRecordExecutionAttachmentChainVerifyResult result =
                attachmentService.verifyAttachmentChain(execution.getId());

        assertFalse(result.isValid());
        assertEquals("PREVIOUS_HASH_MISMATCH", result.getIssues().get(0).getIssueCode());
        assertEquals(second.getId(), result.getIssues().get(0).getAttachmentId());
    }

    @Test
    void replaceAttachment_appendsReplaceEventAndKeepsHistory() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        Long firstId = attachmentService.bindAttachment(baseCommand(execution, workTask));

        Long replaceId = attachmentService.replaceAttachment(baseCommand(execution, workTask)
                .setFileId(903L)
                .setStoragePath("edhr/501/evidence-replaced.png")
                .setFileName("evidence-replaced.png")
                .setReasonText("现场图片替换"));

        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionField(execution.getId(),
                        "sheet[0].rows[1].cells[2].visualEvidence", "visualEvidence");
        assertEquals(2, attachments.size());
        assertEquals(firstId, attachments.get(0).getId());
        assertEquals("ADD", attachments.get(0).getAttachmentAction());
        assertEquals(replaceId, attachments.get(1).getId());
        assertEquals("REPLACE", attachments.get(1).getAttachmentAction());
        assertEquals(2, attachments.get(1).getVersionNo());
        assertEquals(attachments.get(0).getAttachmentHash(), attachments.get(1).getPreviousAttachmentHash());
        assertEquals("evidence-replaced.png", attachments.get(1).getFileName());
        assertTrue(attachmentService.verifyAttachmentChain(execution.getId()).isValid());
    }

    @Test
    void voidAttachment_appendsVoidEventAndKeepsHistory() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);
        Long firstId = attachmentService.bindAttachment(baseCommand(execution, workTask));

        Long voidId = attachmentService.voidAttachment(baseVoidCommand(execution, workTask));

        List<MesProBatchRecordExecutionAttachmentDO> attachments =
                attachmentMapper.selectListByExecutionField(execution.getId(),
                        "sheet[0].rows[1].cells[2].visualEvidence", "visualEvidence");
        assertEquals(2, attachments.size());
        assertEquals(firstId, attachments.get(0).getId());
        assertEquals("ADD", attachments.get(0).getAttachmentAction());
        assertEquals(voidId, attachments.get(1).getId());
        assertEquals("VOID", attachments.get(1).getAttachmentAction());
        assertEquals(2, attachments.get(1).getVersionNo());
        assertEquals(attachments.get(0).getAttachmentHash(), attachments.get(1).getPreviousAttachmentHash());
        assertEquals(attachments.get(0).getFileId(), attachments.get(1).getFileId());
        assertTrue(attachmentService.verifyAttachmentChain(execution.getId()).isValid());
    }

    @Test
    void replaceAttachment_rejectsMissingAttachmentGroup() {
        MesProBatchRecordExecutionDO execution = insertExecution(0, true);
        MesProEdhrWorkTaskDO workTask = insertWorkTask(execution, 99L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                attachmentService.replaceAttachment(baseCommand(execution, workTask)));

        assertEquals(MesProBatchRecordExecutionErrorCodeConstants
                .PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_GROUP_NOT_EXISTS.getCode(), exception.getCode());
        assertEquals(0, attachmentMapper.selectListByExecutionId(execution.getId()).size());
    }

    private MesProBatchRecordExecutionAttachmentBindCommand baseCommand(MesProBatchRecordExecutionDO execution,
                                                                       MesProEdhrWorkTaskDO workTask) {
        return new MesProBatchRecordExecutionAttachmentBindCommand()
                .setExecutionId(execution.getId())
                .setWorkTaskId(workTask.getId())
                .setAuditBatchId(7001L)
                .setSignatureId(8001L)
                .setRowIndex(1)
                .setColumnIndex(2)
                .setFieldKey("visualEvidence")
                .setFieldPath("sheet[0].rows[1].cells[2].visualEvidence")
                .setFieldLabel("现场图片")
                .setAttachmentType("IMAGE")
                .setAttachmentGroupKey("R1C2-IMG-1")
                .setFileId(901L)
                .setFileUrl("http://127.0.0.1:9000/yudao/edhr/501/evidence.png")
                .setStorageConfigId(28L)
                .setStoragePath("edhr/501/evidence.png")
                .setFileName("evidence.png")
                .setContentType("image/png")
                .setFileSize(2048L)
                .setSha256(FILE_SHA256)
                .setStorageRetentionJson("{\"fileId\":901,\"retention\":\"batch-record\"}")
                .setOperatorId(99L)
                .setOperatorName("aoteman")
                .setReasonCategory("OPERATOR_ENTRY")
                .setReasonText("现场上传");
    }

    private MesProBatchRecordExecutionAttachmentVoidCommand baseVoidCommand(MesProBatchRecordExecutionDO execution,
                                                                           MesProEdhrWorkTaskDO workTask) {
        return new MesProBatchRecordExecutionAttachmentVoidCommand()
                .setExecutionId(execution.getId())
                .setWorkTaskId(workTask.getId())
                .setFieldKey("visualEvidence")
                .setFieldPath("sheet[0].rows[1].cells[2].visualEvidence")
                .setAttachmentGroupKey("R1C2-IMG-1")
                .setOperatorId(99L)
                .setOperatorName("aoteman")
                .setReasonCategory("OPERATOR_ENTRY")
                .setReasonText("现场图片作废");
    }

    private MesProBatchRecordExecutionDO insertExecution(Integer status, Boolean activeRevisionFlag) {
        return insertExecution(status, activeRevisionFlag, "upload-image", "image/png", 4096L);
    }

    private MesProBatchRecordExecutionDO insertExecution(Integer status, Boolean activeRevisionFlag,
                                                        String componentFlag,
                                                        String allowedContentType,
                                                        Long maxFileSize) {
        MesProBatchRecordExecutionDO execution = MesProBatchRecordExecutionDO.builder()
                .executionCode("EDHR-ATT-501")
                .templateId(10L)
                .templateCode("TPL-ATT")
                .templateName("附件测试模板")
                .workOrderId(1001L)
                .workOrderCode("MO-ATT")
                .routeProcessId(2001L)
                .taskId(701L)
                .batchRecordReportId("RPT-ATT")
                .batchCode("BATCH-ATT")
                .status(status)
                .sheetLayoutJson("{}")
                .executionSnapshotJson(attachmentSnapshotJson(componentFlag, allowedContentType, maxFileSize))
                .cellValuesJson("[]")
                .cellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("[]"))
                .fieldAuditRevision(0L)
                .fieldAuditHeadHash(MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH)
                .revisionRootExecutionId(501L)
                .revisionNo(1)
                .activeRevisionFlag(activeRevisionFlag)
                .build();
        executionMapper.insert(execution);
        return execution;
    }

    private MesProEdhrWorkTaskDO insertWorkTask(MesProBatchRecordExecutionDO execution, Long assigneeUserId) {
        return insertWorkTask(execution, assigneeUserId, 701L);
    }

    private MesProEdhrWorkTaskDO insertWorkTask(MesProBatchRecordExecutionDO execution, Long assigneeUserId,
                                               Long batchTaskId) {
        MesProEdhrWorkTaskDO workTask = MesProEdhrWorkTaskDO.builder()
                .taskCode("WT-ATT-1")
                .taskType("FILL")
                .batchExecutionId(601L)
                .batchTaskId(batchTaskId)
                .executionId(execution.getId())
                .workOrderId(execution.getWorkOrderId())
                .workOrderCode(execution.getWorkOrderCode())
                .batchCode(execution.getBatchCode())
                .routeId(801L)
                .routeProcessId(execution.getRouteProcessId())
                .processId(901L)
                .processName("附件工序")
                .assigneeUserId(assigneeUserId)
                .status(MesProEdhrWorkTaskStatus.DOING)
                .actionUrl("/mes/pro/edhr/executions/" + execution.getId())
                .build();
        workTaskMapper.insert(workTask);
        return workTask;
    }

    private String attachmentSnapshotJson(String componentFlag, String allowedContentType, Long maxFileSize) {
        return """
                {"fields":[{"fieldPath":"sheet[0].rows[1].cells[2].visualEvidence","fieldKey":"visualEvidence","label":"现场图片","rowIndex":1,"columnIndex":2,"valueType":"STRING","component":"%s","required":false,"constraints":{"allowedContentTypes":["%s"],"maxFileSize":%d},"edhrCellRule":{"rowIndex":1,"columnIndex":2,"valueType":"STRING","componentFlag":"%s","required":false,"label":"现场图片","constraints":{"allowedContentTypes":["%s"],"maxFileSize":%d},"source":"MANUAL","confidence":1.0,"reviewed":true}}]}
                """.formatted(componentFlag, allowedContentType, maxFileSize, componentFlag, allowedContentType,
                maxFileSize);
    }
}
