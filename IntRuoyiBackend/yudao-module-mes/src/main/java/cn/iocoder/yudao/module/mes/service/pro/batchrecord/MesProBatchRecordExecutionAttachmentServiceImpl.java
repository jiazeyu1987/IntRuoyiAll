package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_GROUP_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_PERSIST_FAILED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_WORK_TASK_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;

@Service
public class MesProBatchRecordExecutionAttachmentServiceImpl implements MesProBatchRecordExecutionAttachmentService {

    private static final int EXECUTION_STATUS_DRAFT = 0;
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    private static final List<String> ATTACHMENT_COMPONENT_FLAGS = List.of("upload-file", "upload-image", "upload-images");

    @Resource
    private MesProBatchRecordExecutionMapper executionMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    @Resource
    private FileService fileService;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProBatchRecordExecutionAttachmentPrepareUploadResult prepareUpload(
            MesProBatchRecordExecutionAttachmentPrepareUploadCommand command) {
        requirePrepareUploadCommand(command);
        MesProBatchRecordExecutionDO execution = executionMapper.selectByIdForUpdate(command.getExecutionId());
        validateEditableWorkTask(execution, command.getWorkTaskId(), command.getOperatorId());
        String directory = "edhr/executions/" + execution.getId() + "/attachments";
        String sha256 = sha256(command.getContent());
        Long fileId = fileService.createFileAndReturnId(command.getContent(), StrUtil.trim(command.getFileName()),
                directory, StrUtil.trim(command.getContentType()));
        FileDO file = fileService.getFile(fileId);
        requireFileMetadata(file);
        String storageRetentionJson = "{\"fileId\":" + file.getId()
                + ",\"storageConfigId\":" + file.getConfigId()
                + ",\"storagePath\":\"" + file.getPath()
                + "\",\"sha256\":\"" + sha256 + "\"}";
        String storageRetentionHash = MesProBatchRecordExecutionFieldAuditHasher.sha256(
                "EDHR_ATTACHMENT_V1:RETENTION\n"
                        + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(storageRetentionJson));
        return MesProBatchRecordExecutionAttachmentPrepareUploadResult.builder()
                .uploadToken("EDHR_ATTACHMENT_UPLOAD:" + execution.getId() + ":" + file.getId() + ":" + sha256)
                .fileId(file.getId())
                .fileUrl(file.getUrl())
                .storageConfigId(file.getConfigId())
                .storagePath(file.getPath())
                .fileName(file.getName())
                .contentType(file.getType())
                .fileSize(file.getSize())
                .sha256(sha256)
                .storageRetentionJson(storageRetentionJson)
                .storageRetentionHash(storageRetentionHash)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bindAttachment(MesProBatchRecordExecutionAttachmentBindCommand command) {
        requireCommand(command);
        return appendFileAttachmentEvent(command, "ADD", false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long replaceAttachment(MesProBatchRecordExecutionAttachmentBindCommand command) {
        requireCommand(command);
        return appendFileAttachmentEvent(command, "REPLACE", true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long voidAttachment(MesProBatchRecordExecutionAttachmentVoidCommand command) {
        requireVoidCommand(command);
        MesProBatchRecordExecutionDO execution = executionMapper.selectByIdForUpdate(command.getExecutionId());
        MesProEdhrWorkTaskDO workTask = validateEditableWorkTask(execution, command.getWorkTaskId(),
                command.getOperatorId());
        MesProBatchRecordExecutionAttachmentDO latest = requireLatestAttachment(execution.getId(),
                command.getFieldPath(), command.getFieldKey(), command.getAttachmentGroupKey());
        MesProBatchRecordExecutionAttachmentDO attachment = MesProBatchRecordExecutionAttachmentDO.builder()
                .executionId(execution.getId())
                .batchExecutionId(workTask.getBatchExecutionId())
                .batchTaskId(workTask.getBatchTaskId())
                .workTaskId(workTask.getId())
                .rowIndex(latest.getRowIndex())
                .columnIndex(latest.getColumnIndex())
                .fieldKey(latest.getFieldKey())
                .fieldPath(latest.getFieldPath())
                .fieldLabel(latest.getFieldLabel())
                .attachmentType(latest.getAttachmentType())
                .attachmentGroupKey(latest.getAttachmentGroupKey())
                .attachmentAction("VOID")
                .versionNo(latest.getVersionNo() + 1)
                .fileId(latest.getFileId())
                .fileUrl(latest.getFileUrl())
                .storageConfigId(latest.getStorageConfigId())
                .storagePath(latest.getStoragePath())
                .fileName(latest.getFileName())
                .contentType(latest.getContentType())
                .fileSize(latest.getFileSize())
                .sha256(latest.getSha256())
                .storageRetentionJson(latest.getStorageRetentionJson())
                .storageRetentionHash(latest.getStorageRetentionHash())
                .auditBatchId(command.getAuditBatchId())
                .signatureId(command.getSignatureId())
                .previousAttachmentHash(latest.getAttachmentHash())
                .operatorId(command.getOperatorId())
                .operatorName(StrUtil.trim(command.getOperatorName()))
                .operatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .reasonCategory(StrUtil.trim(command.getReasonCategory()))
                .reasonText(StrUtil.trim(command.getReasonText()))
                .build();
        attachment.setAttachmentHash(hashAttachment(attachment));
        return insertAttachment(attachment);
    }

    private Long appendFileAttachmentEvent(MesProBatchRecordExecutionAttachmentBindCommand command,
                                           String action,
                                           boolean requireExistingGroup) {
        MesProBatchRecordExecutionDO execution = executionMapper.selectByIdForUpdate(command.getExecutionId());
        MesProEdhrWorkTaskDO workTask = validateEditableWorkTask(execution, command.getWorkTaskId(),
                command.getOperatorId());
        validateAttachmentFieldRule(execution, command);
        MesProBatchRecordExecutionAttachmentDO latest = attachmentMapper.selectLatestByExecutionFieldGroup(
                execution.getId(), command.getFieldPath(), command.getFieldKey(), command.getAttachmentGroupKey());
        if (requireExistingGroup && latest == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_GROUP_NOT_EXISTS);
        }
        if (StrUtil.isNotBlank(command.getExpectedPreviousAttachmentHash())
                && (latest == null || !Objects.equals(StrUtil.trim(command.getExpectedPreviousAttachmentHash()),
                latest.getAttachmentHash()))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_CHAIN_CONFLICT);
        }
        String previousHash = latest == null ? MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH
                : latest.getAttachmentHash();
        int versionNo = latest == null ? 1 : latest.getVersionNo() + 1;
        LocalDateTime operatedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String storageRetentionHash = MesProBatchRecordExecutionFieldAuditHasher.sha256(
                "EDHR_ATTACHMENT_V1:RETENTION\n"
                        + MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(command.getStorageRetentionJson()));

        MesProBatchRecordExecutionAttachmentDO attachment = MesProBatchRecordExecutionAttachmentDO.builder()
                .executionId(execution.getId())
                .batchExecutionId(workTask.getBatchExecutionId())
                .batchTaskId(workTask.getBatchTaskId())
                .workTaskId(workTask.getId())
                .rowIndex(command.getRowIndex())
                .columnIndex(command.getColumnIndex())
                .fieldKey(StrUtil.trim(command.getFieldKey()))
                .fieldPath(StrUtil.trim(command.getFieldPath()))
                .fieldLabel(StrUtil.trim(command.getFieldLabel()))
                .attachmentType(StrUtil.trim(command.getAttachmentType()))
                .attachmentGroupKey(StrUtil.trim(command.getAttachmentGroupKey()))
                .attachmentAction(action)
                .versionNo(versionNo)
                .fileId(command.getFileId())
                .fileUrl(StrUtil.trim(command.getFileUrl()))
                .storageConfigId(command.getStorageConfigId())
                .storagePath(StrUtil.trim(command.getStoragePath()))
                .fileName(StrUtil.trim(command.getFileName()))
                .contentType(StrUtil.trim(command.getContentType()))
                .fileSize(command.getFileSize())
                .sha256(StrUtil.trim(command.getSha256()))
                .storageRetentionJson(command.getStorageRetentionJson())
                .storageRetentionHash(storageRetentionHash)
                .auditBatchId(command.getAuditBatchId())
                .signatureId(command.getSignatureId())
                .previousAttachmentHash(previousHash)
                .operatorId(command.getOperatorId())
                .operatorName(StrUtil.trim(command.getOperatorName()))
                .operatedAt(operatedAt)
                .reasonCategory(StrUtil.trim(command.getReasonCategory()))
                .reasonText(StrUtil.trim(command.getReasonText()))
                .build();
        attachment.setAttachmentHash(hashAttachment(attachment));
        return insertAttachment(attachment);
    }

    private void validateAttachmentFieldRule(MesProBatchRecordExecutionDO execution,
                                             MesProBatchRecordExecutionAttachmentBindCommand command) {
        AttachmentSnapshotField field = resolveAttachmentSnapshotField(execution, command.getFieldPath(),
                command.getFieldKey(), command.getRowIndex(), command.getColumnIndex());
        if (!ATTACHMENT_COMPONENT_FLAGS.contains(field.component())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID,
                    field.label() + " 不是附件上传控件");
        }
        if ("IMAGE".equals(StrUtil.trim(command.getAttachmentType()))
                && !List.of("upload-image", "upload-images").contains(field.component())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_CELL_RULE_INVALID,
                    field.label() + " 不允许上传图片附件");
        }
        validateAllowedContentTypes(field, command.getContentType());
        validateMaxFileSize(field, command.getFileSize());
    }

    private AttachmentSnapshotField resolveAttachmentSnapshotField(MesProBatchRecordExecutionDO execution,
                                                                   String fieldPath,
                                                                   String fieldKey,
                                                                   Integer rowIndex,
                                                                   Integer columnIndex) {
        if (StrUtil.isBlank(execution.getExecutionSnapshotJson())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
        }
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(execution.getExecutionSnapshotJson());
            JsonNode fields = root.path("fields");
            if (!fields.isArray()) {
                throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
            }
            String expectedKey = attachmentFieldKey(fieldPath, fieldKey, rowIndex, columnIndex);
            for (JsonNode field : fields) {
                if (!Objects.equals(expectedKey, attachmentFieldKey(text(field, "fieldPath"),
                        text(field, "fieldKey"), integer(field, "rowIndex"), integer(field, "columnIndex")))) {
                    continue;
                }
                JsonNode rule = field.path("edhrCellRule");
                String component = StrUtil.blankToDefault(text(rule, "componentFlag"), text(field, "component"));
                JsonNode constraints = rule.path("constraints").isObject()
                        ? rule.path("constraints") : field.path("constraints");
                return new AttachmentSnapshotField(
                        StrUtil.blankToDefault(text(field, "label"), StrUtil.trim(fieldKey)),
                        StrUtil.trim(component),
                        constraints == null || constraints.isMissingNode()
                                ? JsonNodeFactory.instance.objectNode() : constraints);
            }
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
        } catch (JsonProcessingException ex) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_FIELD_NOT_DECLARED);
        }
    }

    private void validateAllowedContentTypes(AttachmentSnapshotField field, String contentType) {
        JsonNode allowed = field.constraints().get("allowedContentTypes");
        if (allowed == null || allowed.isNull()) {
            return;
        }
        if (!allowed.isArray()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 允许的附件类型配置无效");
        }
        for (JsonNode item : allowed) {
            if (item.isTextual() && Objects.equals(item.textValue(), StrUtil.trim(contentType))) {
                return;
            }
        }
        throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                field.label() + " 不允许上传 " + StrUtil.trim(contentType));
    }

    private void validateMaxFileSize(AttachmentSnapshotField field, Long fileSize) {
        JsonNode maxFileSize = field.constraints().get("maxFileSize");
        if (maxFileSize == null || maxFileSize.isNull()) {
            return;
        }
        if (!maxFileSize.canConvertToLong() || maxFileSize.asLong() < 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 附件大小上限配置无效");
        }
        if (fileSize > maxFileSize.asLong()) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_FIELD_AUDIT_VALUE_CONSTRAINT_VIOLATION,
                    field.label() + " 附件大小超过 " + maxFileSize.asLong());
        }
    }

    private MesProEdhrWorkTaskDO validateEditableWorkTask(MesProBatchRecordExecutionDO execution, Long workTaskId,
                                                         Long operatorId) {
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (!Integer.valueOf(EXECUTION_STATUS_DRAFT).equals(execution.getStatus())
                || !Boolean.TRUE.equals(execution.getActiveRevisionFlag())
                || execution.getClosedAt() != null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        boolean goldenFingerMode = goldenFingerPermissionService.hasGoldenFingerPermission(operatorId);
        if (workTask == null
                || !Objects.equals(workTask.getExecutionId(), execution.getId())
                || (!goldenFingerMode && !isAssignedOrCandidate(workTask, operatorId))
                || !List.of(MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE).contains(workTask.getStatus())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_WORK_TASK_INVALID);
        }
        return workTask;
    }

    private boolean isAssignedOrCandidate(MesProEdhrWorkTaskDO workTask, Long userId) {
        return MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, userId);
    }

    private MesProBatchRecordExecutionAttachmentDO requireLatestAttachment(Long executionId, String fieldPath,
                                                                          String fieldKey, String attachmentGroupKey) {
        MesProBatchRecordExecutionAttachmentDO latest = attachmentMapper.selectLatestByExecutionFieldGroup(
                executionId, fieldPath, fieldKey, attachmentGroupKey);
        if (latest == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_GROUP_NOT_EXISTS);
        }
        return latest;
    }

    private Long insertAttachment(MesProBatchRecordExecutionAttachmentDO attachment) {
        int inserted = attachmentMapper.insert(attachment);
        if (inserted != 1) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_PERSIST_FAILED);
        }
        return attachment.getId();
    }

    @Override
    public List<MesProBatchRecordExecutionAttachmentDO> listByExecution(Long executionId) {
        return attachmentMapper.selectListByExecutionId(executionId);
    }

    @Override
    public List<MesProBatchRecordExecutionAttachmentDO> listByField(Long executionId, String fieldPath, String fieldKey) {
        return attachmentMapper.selectListByExecutionField(executionId, fieldPath, fieldKey);
    }

    @Override
    public MesProBatchRecordExecutionAttachmentChainVerifyResult verifyAttachmentChain(Long executionId) {
        if (executionId == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        MesProBatchRecordExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper.selectListByExecutionId(executionId);
        List<MesProBatchRecordExecutionAttachmentChainVerifyResult.Issue> issues = new java.util.ArrayList<>();
        String globalHeadHash = MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH;
        var groups = attachments.stream()
                .collect(Collectors.groupingBy(this::attachmentChainGroupKey));
        for (List<MesProBatchRecordExecutionAttachmentDO> groupItems : groups.values()) {
            groupItems.sort(Comparator
                    .comparing(MesProBatchRecordExecutionAttachmentDO::getVersionNo,
                            Comparator.nullsFirst(Integer::compareTo))
                    .thenComparing(MesProBatchRecordExecutionAttachmentDO::getId,
                            Comparator.nullsFirst(Long::compareTo)));
            String expectedPreviousHash = MesProBatchRecordExecutionFieldAuditHasher.GENESIS_HEAD_HASH;
            int expectedVersion = 1;
            for (MesProBatchRecordExecutionAttachmentDO item : groupItems) {
                if (!Objects.equals(item.getVersionNo(), expectedVersion)) {
                    issues.add(issue(item, "VERSION_GAP",
                            "Attachment version is not continuous, expected " + expectedVersion));
                }
                if (!Objects.equals(item.getPreviousAttachmentHash(), expectedPreviousHash)) {
                    issues.add(issue(item, "PREVIOUS_HASH_MISMATCH",
                            "Attachment previous hash does not match previous ledger event"));
                }
                String actualHash = hashAttachment(item);
                if (!Objects.equals(item.getAttachmentHash(), actualHash)) {
                    issues.add(issue(item, "HASH_MISMATCH",
                            "Attachment hash does not match ledger content"));
                }
                expectedPreviousHash = item.getAttachmentHash();
                if (item.getAttachmentHash() != null) {
                    globalHeadHash = item.getAttachmentHash();
                }
                expectedVersion++;
            }
        }
        return MesProBatchRecordExecutionAttachmentChainVerifyResult.builder()
                .valid(issues.isEmpty())
                .checkedEventCount(attachments.size())
                .headHash(globalHeadHash)
                .issues(issues)
                .build();
    }

    private void requireCommand(MesProBatchRecordExecutionAttachmentBindCommand command) {
        if (command == null
                || command.getExecutionId() == null
                || command.getWorkTaskId() == null
                || command.getAuditBatchId() == null
                || command.getSignatureId() == null
                || command.getRowIndex() == null
                || command.getColumnIndex() == null
                || command.getFileId() == null
                || command.getStorageConfigId() == null
                || command.getFileSize() == null
                || command.getFileSize() <= 0
                || command.getOperatorId() == null
                || StrUtil.isBlank(command.getFieldKey())
                || StrUtil.isBlank(command.getFieldPath())
                || StrUtil.isBlank(command.getAttachmentType())
                || StrUtil.isBlank(command.getAttachmentGroupKey())
                || StrUtil.isBlank(command.getFileUrl())
                || StrUtil.isBlank(command.getStoragePath())
                || StrUtil.isBlank(command.getFileName())
                || StrUtil.isBlank(command.getContentType())
                || StrUtil.isBlank(command.getSha256())
                || !SHA256_PATTERN.matcher(StrUtil.trim(command.getSha256())).matches()
                || StrUtil.isBlank(command.getStorageRetentionJson())
                || StrUtil.isBlank(command.getOperatorName())
                || StrUtil.isBlank(command.getReasonCategory())
                || StrUtil.isBlank(command.getReasonText())
                || !List.of("FILE", "IMAGE").contains(StrUtil.trim(command.getAttachmentType()))) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID);
        }
        MesProBatchRecordExecutionFieldAuditHasher.canonicalizeJsonString(command.getStorageRetentionJson());
    }

    private void requirePrepareUploadCommand(MesProBatchRecordExecutionAttachmentPrepareUploadCommand command) {
        if (command == null
                || command.getExecutionId() == null
                || command.getWorkTaskId() == null
                || command.getOperatorId() == null
                || StrUtil.isBlank(command.getFileName())
                || StrUtil.isBlank(command.getContentType())
                || command.getContent() == null
                || command.getContent().length == 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID);
        }
    }

    private void requireFileMetadata(FileDO file) {
        if (file == null
                || file.getId() == null
                || file.getConfigId() == null
                || StrUtil.isBlank(file.getPath())
                || StrUtil.isBlank(file.getUrl())
                || StrUtil.isBlank(file.getName())
                || StrUtil.isBlank(file.getType())
                || file.getSize() == null
                || file.getSize() <= 0) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID);
        }
    }

    private void requireVoidCommand(MesProBatchRecordExecutionAttachmentVoidCommand command) {
        if (command == null
                || command.getExecutionId() == null
                || command.getWorkTaskId() == null
                || command.getOperatorId() == null
                || StrUtil.isBlank(command.getFieldKey())
                || StrUtil.isBlank(command.getFieldPath())
                || StrUtil.isBlank(command.getAttachmentGroupKey())
                || StrUtil.isBlank(command.getOperatorName())
                || StrUtil.isBlank(command.getReasonCategory())
                || StrUtil.isBlank(command.getReasonText())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_ATTACHMENT_FILE_METADATA_INVALID);
        }
    }

    private String hashAttachment(MesProBatchRecordExecutionAttachmentDO attachment) {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("executionId", attachment.getExecutionId());
        root.put("batchExecutionId", attachment.getBatchExecutionId());
        root.put("batchTaskId", attachment.getBatchTaskId());
        root.put("workTaskId", attachment.getWorkTaskId());
        root.put("rowIndex", attachment.getRowIndex());
        root.put("columnIndex", attachment.getColumnIndex());
        root.put("fieldKey", attachment.getFieldKey());
        root.put("fieldPath", attachment.getFieldPath());
        root.put("attachmentType", attachment.getAttachmentType());
        root.put("attachmentGroupKey", attachment.getAttachmentGroupKey());
        root.put("attachmentAction", attachment.getAttachmentAction());
        root.put("versionNo", attachment.getVersionNo());
        root.put("fileId", attachment.getFileId());
        root.put("fileUrl", attachment.getFileUrl());
        root.put("storageConfigId", attachment.getStorageConfigId());
        root.put("storagePath", attachment.getStoragePath());
        root.put("fileName", attachment.getFileName());
        root.put("contentType", attachment.getContentType());
        root.put("fileSize", attachment.getFileSize());
        root.put("sha256", attachment.getSha256());
        root.put("storageRetentionHash", attachment.getStorageRetentionHash());
        root.put("auditBatchId", attachment.getAuditBatchId());
        root.put("signatureId", attachment.getSignatureId());
        root.put("previousAttachmentHash", attachment.getPreviousAttachmentHash());
        root.put("operatorId", attachment.getOperatorId());
        root.put("operatorName", attachment.getOperatorName());
        root.put("operatedAt", attachment.getOperatedAt().toString());
        root.put("reasonCategory", attachment.getReasonCategory());
        root.put("reasonText", attachment.getReasonText());
        return MesProBatchRecordExecutionFieldAuditHasher.sha256(
                "EDHR_ATTACHMENT_V1:LEDGER\n" + MesProBatchRecordExecutionFieldAuditHasher.canonicalize(root));
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content);
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for eDHR attachment upload", e);
        }
    }

    private String attachmentChainGroupKey(MesProBatchRecordExecutionAttachmentDO attachment) {
        return attachment.getFieldPath() + "\n" + attachment.getFieldKey() + "\n" + attachment.getAttachmentGroupKey();
    }

    private static String attachmentFieldKey(String fieldPath, String fieldKey, Integer rowIndex, Integer columnIndex) {
        return StrUtil.nullToEmpty(StrUtil.trim(fieldPath)) + "|" + StrUtil.nullToEmpty(StrUtil.trim(fieldKey)) + "|"
                + rowIndex + "|" + columnIndex;
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode child = node == null ? null : node.get(fieldName);
        return child == null || child.isNull() ? null : child.asText();
    }

    private static Integer integer(JsonNode node, String fieldName) {
        JsonNode child = node == null ? null : node.get(fieldName);
        return child == null || !child.canConvertToInt() ? null : child.asInt();
    }

    private MesProBatchRecordExecutionAttachmentChainVerifyResult.Issue issue(
            MesProBatchRecordExecutionAttachmentDO attachment, String issueCode, String message) {
        return MesProBatchRecordExecutionAttachmentChainVerifyResult.Issue.builder()
                .attachmentId(attachment.getId())
                .fieldPath(attachment.getFieldPath())
                .fieldKey(attachment.getFieldKey())
                .attachmentGroupKey(attachment.getAttachmentGroupKey())
                .versionNo(attachment.getVersionNo())
                .issueCode(issueCode)
                .message(message)
                .build();
    }

    private record AttachmentSnapshotField(String label, String component, JsonNode constraints) {
    }
}
