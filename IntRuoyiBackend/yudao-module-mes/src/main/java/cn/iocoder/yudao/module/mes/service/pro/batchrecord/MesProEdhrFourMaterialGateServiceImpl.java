package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceSourcePrecheckRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityBlocker.TRACE_MAPPING_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityErrorCodeConstants.TRACE_CAPTURE_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;

@Service
public class MesProEdhrFourMaterialGateServiceImpl implements MesProEdhrFourMaterialGateService {

    private final MesProEdhrBatchExecutionTaskMapper taskMapper;
    private final MesProBatchRecordExecutionAttachmentMapper attachmentMapper;
    private final MesProEdhrBatchTraceabilityService traceabilityService;
    private final FileService fileService;
    private final MesReleaseMaterialGateReceiptWriter receiptWriter;
    public MesProEdhrFourMaterialGateServiceImpl(MesProEdhrBatchExecutionTaskMapper taskMapper,
                                                   MesProBatchRecordExecutionAttachmentMapper attachmentMapper,
                                                   MesProEdhrBatchTraceabilityService traceabilityService,
                                                   FileService fileService,
                                                   MesReleaseMaterialGateReceiptWriter receiptWriter) {
        this.taskMapper = taskMapper;
        this.attachmentMapper = attachmentMapper;
        this.traceabilityService = traceabilityService;
        this.fileService = fileService;
        this.receiptWriter = receiptWriter;
    }

    @Override
    public MesProEdhrFourMaterialGateResult evaluate(Long batchExecutionId) {
        if (batchExecutionId == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        MesProEdhrBatchTraceSourcePrecheckRespVO source = requireTraceSourceReady(batchExecutionId);
        List<MesProEdhrBatchExecutionTaskDO> materialTasks = taskMapper.selectListByBatchExecutionId(batchExecutionId)
                .stream().filter(t -> REQUIRED_MATERIAL_TYPES.contains(t.getNodeType()))
                .toList();
        requireUniqueMaterialTasks(materialTasks);
        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = materialTasks.stream()
                .collect(Collectors.toMap(MesProEdhrBatchExecutionTaskDO::getNodeType, Function.identity()));
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesProBatchRecordExecutionAttachmentDO> valid = new ArrayList<>();
        boolean missing = false;
        boolean invalid = false;
        boolean materialSourceChanged = false;
        for (String node : REQUIRED_MATERIAL_TYPES) {
            MesProEdhrBatchExecutionTaskDO task = tasks.get(node);
            if (task == null || !Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)) {
                missing = true;
                continue;
            }
            if (!Objects.equals(task.getMaterialSourceSnapshotHash(), source.getSourceSnapshotHash())) {
                materialSourceChanged = true;
            }
            MesProBatchRecordExecutionAttachmentDO latest = attachments.stream()
                    .filter(a -> Objects.equals(task.getId(), a.getBatchTaskId()))
                    .filter(a -> Objects.equals(node, a.getFieldKey()) || Objects.equals(node, a.getAttachmentGroupKey()))
                    .max(Comparator.comparing(MesProBatchRecordExecutionAttachmentDO::getVersionNo,
                            Comparator.nullsFirst(Integer::compareTo)).thenComparing(MesProBatchRecordExecutionAttachmentDO::getId,
                            Comparator.nullsFirst(Long::compareTo)))
                    .orElse(null);
            if (latest == null) {
                missing = true;
                continue;
            }
            if (!isCurrentValid(latest, task)) {
                invalid = true;
                continue;
            }
            valid.add(latest);
        }
        if (missing) return new MesProEdhrFourMaterialGateResult(
                MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_PENDING, false, null, valid);
        if (invalid || valid.size() != REQUIRED_MATERIAL_TYPES.size()) {
            return new MesProEdhrFourMaterialGateResult(
                    MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED, false, null, valid);
        }
        String manifest = materialManifest(valid, source.getSourceSnapshotHash());
        if (materialSourceChanged) {
            return new MesProEdhrFourMaterialGateResult(
                    MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED, false, manifest, valid);
        }
        return new MesProEdhrFourMaterialGateResult(MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, true,
                manifest, valid);
    }

    private void requireUniqueMaterialTasks(List<MesProEdhrBatchExecutionTaskDO> materialTasks) {
        List<String> duplicateNodeTypes = materialTasks.stream()
                .collect(Collectors.groupingBy(MesProEdhrBatchExecutionTaskDO::getNodeType, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!duplicateNodeTypes.isEmpty()) {
            throw exception(MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_FOUR_MATERIAL_GATE_BLOCKED,
                    "duplicate required material tasks: " + String.join(",", duplicateNodeTypes));
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public MesProEdhrFourMaterialGateResult requireMaterialsReady(Long batchExecutionId) {
        MesProEdhrFourMaterialGateResult result = evaluate(batchExecutionId);
        if (!result.ready()) {
            throw exception(MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_FOUR_MATERIAL_GATE_BLOCKED,
                    result.status());
        }
        if (receiptWriter == null) {
            throw new IllegalStateException("MATERIAL_GATE_RECEIPT_WRITER_NOT_WIRED");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long issuedBy = SecurityFrameworkUtils.getLoginUserId();
        if (issuedBy == null) {
            throw new IllegalStateException("MATERIAL_GATE_RECEIPT_ISSUER_REQUIRED");
        }
        MesProEdhrBatchTraceSourcePrecheckRespVO source = requireTraceSourceReady(batchExecutionId);
        if (!Objects.equals(result.manifestHash(), materialManifest(result.materials(), source.getSourceSnapshotHash()))) {
            throw exception(MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_RELEASE_FOUR_MATERIAL_GATE_BLOCKED,
                    MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED);
        }
        receiptWriter.persistReady(tenantId, batchExecutionId, source.getSourceSnapshotHash(), result, issuedBy);
        return result;
    }

    private MesProEdhrBatchTraceSourcePrecheckRespVO requireTraceSourceReady(Long batchExecutionId) {
        MesProEdhrBatchTraceSourcePrecheckRespVO source;
        try {
            source = traceabilityService.resolveSourcePrecheck(
                    new MesProEdhrBatchTraceSourcePrecheckCommand().setBatchExecutionId(batchExecutionId));
        } catch (ServiceException ex) {
            throw exception(TRACE_CAPTURE_BLOCKED, TRACE_MAPPING_BLOCKED);
        }
        if (source == null || !Objects.equals(batchExecutionId, source.getBatchExecutionId())
                || source.getOriginLinkId() == null || blank(source.getTraceLinkHash())
                || blank(source.getSourceSnapshotHash()) || blank(source.getRelationStatus())
                || !("CAPTURED".equalsIgnoreCase(source.getRelationStatus())
                || "READY".equalsIgnoreCase(source.getRelationStatus()))) {
            throw exception(TRACE_CAPTURE_BLOCKED, TRACE_MAPPING_BLOCKED);
        }
        return source;
    }

    private boolean isCurrentValid(MesProBatchRecordExecutionAttachmentDO a,
                                   MesProEdhrBatchExecutionTaskDO task) {
        if (!"ADD".equals(a.getAttachmentAction()) || a.getVersionNo() == null || a.getVersionNo() <= 0
                || a.getFileId() == null || a.getFileSize() == null || a.getFileSize() <= 0
                || !Objects.equals(a.getBatchExecutionId(), task.getBatchExecutionId())
                || blank(a.getFieldPath()) || blank(a.getAttachmentGroupKey()) || blank(a.getAttachmentType())
                || blank(a.getFileName()) || blank(a.getContentType()) || blank(a.getFileUrl())
                || blank(a.getStoragePath()) || blank(a.getSha256()) || !a.getSha256().matches("(?i)[0-9a-f]{64}")
                || blank(a.getAttachmentHash()) || blank(a.getStorageRetentionJson())
                || blank(a.getStorageRetentionHash()) || a.getOperatedAt() == null) {
            return false;
        }
        if (!Objects.equals(a.getStorageRetentionHash(),
                MesProEdhrSpecialNodeAttachmentHasher.retentionHash(a.getStorageRetentionJson()))
                || !Objects.equals(a.getAttachmentHash(),
                MesProEdhrSpecialNodeAttachmentHasher.attachmentHash(a))) {
            return false;
        }
        FileDO file = fileService.getFile(a.getFileId());
        return file != null && Objects.equals(file.getId(), a.getFileId()) && Objects.equals(file.getConfigId(), a.getStorageConfigId())
                && Objects.equals(file.getName(), a.getFileName()) && Objects.equals(file.getPath(), a.getStoragePath())
                && Objects.equals(file.getUrl(), a.getFileUrl()) && Objects.equals(file.getType(), a.getContentType())
                && Objects.equals(file.getSize(), a.getFileSize());
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static String materialManifest(List<MesProBatchRecordExecutionAttachmentDO> materials,
                                           String sourceSnapshotHash) {
        return sha256(materials.stream().sorted(Comparator.comparing(MesProBatchRecordExecutionAttachmentDO::getFieldKey))
                .map(a -> String.join("|", a.getFieldKey(), String.valueOf(a.getVersionNo()), String.valueOf(a.getFileId()),
                        a.getSha256(), a.getAttachmentHash(), sourceSnapshotHash)).collect(Collectors.joining("\n")));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("MATERIAL_MANIFEST_HASH_FAILED", e);
        }
    }
}
