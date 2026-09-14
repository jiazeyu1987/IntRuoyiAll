package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrMaterialGateReceiptDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrMaterialGateReceiptMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceiptPort;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class MesReleaseMaterialGateReceiptPortImpl
        implements MesReleaseMaterialGateReceiptPort, MesReleaseMaterialGateReceiptWriter {

    private final MesProEdhrMaterialGateReceiptMapper mapper;

    public MesReleaseMaterialGateReceiptPortImpl(MesProEdhrMaterialGateReceiptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesReleaseMaterialGateReceipt persistReady(Long tenantId, Long batchExecutionId,
                                                       String sourceSnapshotHash,
                                                       MesProEdhrFourMaterialGateResult result,
                                                       Long issuedBy) {
        requirePersistInput(tenantId, batchExecutionId, sourceSnapshotHash, result, issuedBy);
        String materialTypesJson = JSON.toJSONString(new TreeSet<>(MesReleaseMaterialGateReceipt.REQUIRED_MATERIAL_TYPES));
        String versionSetHash = materialVersionSetHash(result.materials());
        MesProEdhrMaterialGateReceiptDO current = mapper.selectLatestByBatchExecutionId(tenantId, batchExecutionId);
        if (current != null && Objects.equals(current.getSourceSnapshotHash(), sourceSnapshotHash)
                && Objects.equals(current.getManifestHash(), result.manifestHash())
                && Objects.equals(current.getMaterialVersionSetHash(), versionSetHash)
                && MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY.equals(current.getGateStatus())) {
            if (!Objects.equals(current.getReceiptHash(), receiptHash(current))) {
                throw new IllegalStateException("MATERIAL_GATE_RECEIPT_HASH_FAILED");
            }
            return toCore(current, materialTypesJson);
        }
        int version = current == null || current.getVersion() == null ? 1 : current.getVersion() + 1;
        String receiptId = "MATERIALS-" + batchExecutionId + "-" + version;
        String auditEventId = "FLOW8-MATERIALS-" + batchExecutionId + "-" + version;
        MesProEdhrMaterialGateReceiptDO row = new MesProEdhrMaterialGateReceiptDO()
                .setReceiptId(receiptId)
                .setTenantId(tenantId)
                .setBatchExecutionId(batchExecutionId)
                .setGateStatus(MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY)
                .setMaterialTypeKeysJson(materialTypesJson)
                .setManifestHash(result.manifestHash())
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setMaterialVersionSetHash(versionSetHash)
                .setIssuedBy(issuedBy)
                .setAuditEventId(auditEventId)
                .setVersion(version);
        row.setReceiptHash(receiptHash(row));
        if (mapper.insert(row) != 1 || row.getId() == null) {
            throw new IllegalStateException("MATERIAL_GATE_RECEIPT_PERSIST_FAILED");
        }
        return toCore(row, materialTypesJson);
    }

    @Override
    public MesReleaseMaterialGateReceipt getVerifiedByReceiptId(Long tenantId, Long batchExecutionId,
                                                                  String receiptId, String sourceSnapshotHash) {
        if (tenantId == null || batchExecutionId == null || StrUtil.isBlank(receiptId)
                || StrUtil.isBlank(sourceSnapshotHash)) {
            return null;
        }
        MesProEdhrMaterialGateReceiptDO row = mapper.selectByReceiptId(tenantId, batchExecutionId, receiptId);
        return verifyRow(row, tenantId, batchExecutionId, sourceSnapshotHash);
    }

    @Override
    public MesReleaseMaterialGateReceipt getLatestVerified(Long tenantId, Long batchExecutionId,
                                                           String sourceSnapshotHash) {
        if (tenantId == null || batchExecutionId == null || StrUtil.isBlank(sourceSnapshotHash)) {
            return null;
        }
        return verifyRow(mapper.selectLatestByBatchExecutionId(tenantId, batchExecutionId),
                tenantId, batchExecutionId, sourceSnapshotHash);
    }

    private MesReleaseMaterialGateReceipt verifyRow(MesProEdhrMaterialGateReceiptDO row,
                                                    Long tenantId, Long batchExecutionId,
                                                    String sourceSnapshotHash) {
        if (row == null || !Objects.equals(row.getTenantId(), tenantId)
                || !Objects.equals(row.getBatchExecutionId(), batchExecutionId)
                || !Objects.equals(row.getSourceSnapshotHash(), sourceSnapshotHash)
                || !MesReleaseMaterialGateReceipt.STATUS_MATERIALS_READY.equals(row.getGateStatus())
                || !Objects.equals(row.getReceiptHash(), receiptHash(row))) {
            return null;
        }
        MesReleaseMaterialGateReceipt receipt = toCore(row, row.getMaterialTypeKeysJson());
        return receipt.isCompleteFor(batchExecutionId) ? receipt : null;
    }

    private void requirePersistInput(Long tenantId, Long batchExecutionId, String sourceSnapshotHash,
                                     MesProEdhrFourMaterialGateResult result, Long issuedBy) {
        if (tenantId == null || tenantId <= 0 || batchExecutionId == null || batchExecutionId <= 0
                || StrUtil.isBlank(sourceSnapshotHash) || issuedBy == null || issuedBy <= 0
                || result == null || !result.ready() || StrUtil.isBlank(result.manifestHash())
                || result.materials().size() != MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES.size()
                || !materialTypesMatchRequired(result.materials())) {
            throw new IllegalArgumentException("MATERIAL_GATE_RECEIPT_INPUT_INCOMPLETE");
        }
    }

    private boolean materialTypesMatchRequired(List<MesProBatchRecordExecutionAttachmentDO> materials) {
        if (materials == null) {
            return false;
        }
        Set<String> actualTypes = materials.stream()
                .map(MesProBatchRecordExecutionAttachmentDO::getFieldKey)
                .collect(Collectors.toSet());
        return actualTypes.size() == MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES.size()
                && actualTypes.containsAll(MesProEdhrFourMaterialGateService.REQUIRED_MATERIAL_TYPES);
    }

    private MesReleaseMaterialGateReceipt toCore(MesProEdhrMaterialGateReceiptDO row, String materialTypesJson) {
        Set<String> types = new TreeSet<>(JSON.parseArray(materialTypesJson, String.class));
        return new MesReleaseMaterialGateReceipt()
                .setReceiptId(row.getReceiptId()).setBatchExecutionId(row.getBatchExecutionId())
                .setGateStatus(row.getGateStatus()).setMaterialTypeKeys(types)
                .setManifestHash(row.getManifestHash()).setSourceSnapshotHash(row.getSourceSnapshotHash())
                .setMaterialVersionSetHash(row.getMaterialVersionSetHash()).setReceiptHash(row.getReceiptHash())
                .setIssuedBy(row.getIssuedBy()).setAuditEventId(row.getAuditEventId())
                .setVersion(row.getVersion());
    }

    private String receiptHash(MesProEdhrMaterialGateReceiptDO row) {
        return sha256(String.join("|", String.valueOf(row.getTenantId()),
                String.valueOf(row.getBatchExecutionId()), String.valueOf(row.getReceiptId()),
                String.valueOf(row.getGateStatus()), String.valueOf(row.getMaterialTypeKeysJson()),
                String.valueOf(row.getManifestHash()), String.valueOf(row.getSourceSnapshotHash()),
                String.valueOf(row.getMaterialVersionSetHash()), String.valueOf(row.getIssuedBy()),
                String.valueOf(row.getAuditEventId()), String.valueOf(row.getVersion())));
    }

    private String materialVersionSetHash(List<MesProBatchRecordExecutionAttachmentDO> materials) {
        String canonical = materials.stream().sorted((left, right) -> {
                    int field = String.valueOf(left.getFieldKey()).compareTo(String.valueOf(right.getFieldKey()));
                    return field != 0 ? field : Long.compare(value(left.getFileId()), value(right.getFileId()));
                }).map(item -> String.join("|", String.valueOf(item.getFieldKey()),
                        String.valueOf(item.getVersionNo()), String.valueOf(item.getFileId()),
                        String.valueOf(item.getSha256()), String.valueOf(item.getAttachmentHash())))
                .collect(Collectors.joining("\n"));
        return sha256(canonical);
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("MATERIAL_GATE_RECEIPT_HASH_FAILED", ex);
        }
    }
}
