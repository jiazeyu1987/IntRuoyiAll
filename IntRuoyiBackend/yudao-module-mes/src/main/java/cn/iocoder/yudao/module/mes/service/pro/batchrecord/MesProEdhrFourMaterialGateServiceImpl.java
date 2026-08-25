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

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;

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
        // Flow7 is the authoritative source owner and must be read before materials.
        MesProEdhrBatchTraceSourcePrecheckRespVO source;
        try {
            source = traceabilityService.resolveSourcePrecheck(
                    new MesProEdhrBatchTraceSourcePrecheckCommand().setBatchExecutionId(batchExecutionId));
        } catch (ServiceException ex) {
            throw exception(TRACE_CAPTURE_BLOCKED, TRACE_MAPPING_BLOCKED);
        }
        if (source == null || !Objects.equals(batchExecutionId, source.getBatchExecutionId())
                || source.getOriginLinkId() == null || blank(source.getTraceLinkHash())
                || blank(source.getSourceSnapshotHash())
                || blank(source.getRelationStatus()) || "NOT_APPLICABLE".equalsIgnoreCase(source.getRelationStatus())) {
            throw exception(TRACE_CAPTURE_BLOCKED, TRACE_MAPPING_BLOCKED);
        }

        Map<String, MesProEdhrBatchExecutionTaskDO> tasks = taskMapper.selectListByBatchExecutionId(batchExecutionId)
                .stream().filter(t -> REQUIRED_MATERIAL_TYPES.contains(t.getNodeType()))
                .collect(Collectors.toMap(MesProEdhrBatchExecutionTaskDO::getNodeType, Function.identity(),
                        (a, b) -> a.getId() >= b.getId() ? a : b));
        List<MesProBatchRecordExecutionAttachmentDO> attachments = attachmentMapper.selectListByBatchExecutionId(batchExecutionId);
        List<MesProBatchRecordExecutionAttachmentDO> valid = new ArrayList<>();
        boolean missing = false;
        boolean invalid = false;
        boolean sourceBindingChanged = false;
        for (String node : REQUIRED_MATERIAL_TYPES) {
            MesProEdhrBatchExecutionTaskDO task = tasks.get(node);
            if (task == null || !Objects.equals(task.getStatus(), MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED)) {
                missing = true;
                continue;
            }
            if (!Objects.equals(task.getRouteBindingSnapshotHash(), source.getSourceSnapshotHash())) {
                sourceBindingChanged = true;
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
        String manifest = sha256(valid.stream().sorted(Comparator.comparing(MesProBatchRecordExecutionAttachmentDO::getFieldKey))
                .map(a -> String.join("|", a.getFieldKey(), String.valueOf(a.getVersionNo()), String.valueOf(a.getFileId()),
                        a.getSha256(), a.getAttachmentHash(), source.getSourceSnapshotHash())).collect(Collectors.joining("\n")));
        if (sourceBindingChanged) {
            return new MesProEdhrFourMaterialGateResult(
                    MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_RECHECK_REQUIRED, false, manifest, valid);
        }
        MesProEdhrFourMaterialGateResult ready = new MesProEdhrFourMaterialGateResult(
                MesProEdhrFourMaterialGateResult.STATUS_MATERIALS_READY, true, manifest, valid);
        MesReleaseMaterialGateReceipt receipt = receiptWriter.persistReady(
                TenantContextHolder.getRequiredTenantId(), batchExecutionId,
                source.getSourceSnapshotHash(), ready, SecurityFrameworkUtils.getLoginUserId());
        return new MesProEdhrFourMaterialGateResult(ready.status(), ready.ready(), ready.manifestHash(),
                ready.materials(), receipt.getReceiptId(), receipt.getReceiptHash(),
                receipt.getMaterialVersionSetHash(), receipt.getVersion());
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

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("MATERIAL_MANIFEST_HASH_FAILED", e);
        }
    }
}
