package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@Accessors(chain = true)
public class MesReleaseMaterialGateReceipt {

    public static final String STATUS_MATERIALS_READY = "MATERIALS_READY";
    public static final Set<String> REQUIRED_MATERIAL_TYPES = Set.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    private String receiptId;
    private Long batchExecutionId;
    private String gateStatus;
    private Set<String> materialTypeKeys;
    private String manifestHash;
    private String sourceSnapshotHash;
    private String materialVersionSetHash;
    private String receiptHash;
    private Long issuedBy;
    private String auditEventId;
    private Integer version;

    public boolean isCompleteFor(Long expectedBatchExecutionId) {
        return receiptId != null && !receiptId.isBlank()
                && expectedBatchExecutionId != null
                && expectedBatchExecutionId.equals(batchExecutionId)
                && STATUS_MATERIALS_READY.equals(gateStatus)
                && REQUIRED_MATERIAL_TYPES.equals(materialTypeKeys)
                && manifestHash != null && !manifestHash.isBlank()
                && sourceSnapshotHash != null && !sourceSnapshotHash.isBlank()
                && materialVersionSetHash != null && !materialVersionSetHash.isBlank()
                && receiptHash != null && !receiptHash.isBlank()
                && issuedBy != null
                && auditEventId != null && !auditEventId.isBlank()
                && version != null && version > 0;
    }
}
