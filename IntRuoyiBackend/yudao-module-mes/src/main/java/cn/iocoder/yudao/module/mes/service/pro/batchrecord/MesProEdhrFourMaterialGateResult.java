package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionAttachmentDO;

import java.util.Collections;
import java.util.List;

public record MesProEdhrFourMaterialGateResult(String status,
                                                boolean ready,
                                                String manifestHash,
                                                List<MesProBatchRecordExecutionAttachmentDO> materials,
                                                String receiptId,
                                                String receiptHash,
                                                String materialVersionSetHash,
                                                Integer version) {
    public static final String STATUS_MATERIALS_PENDING = "MATERIALS_PENDING";
    public static final String STATUS_MATERIALS_READY = "MATERIALS_READY";
    public static final String STATUS_MATERIALS_RECHECK_REQUIRED = "MATERIALS_RECHECK_REQUIRED";

    public MesProEdhrFourMaterialGateResult {
        materials = materials == null ? Collections.emptyList() : List.copyOf(materials);
    }

    public MesProEdhrFourMaterialGateResult(String status, boolean ready, String manifestHash,
                                             List<MesProBatchRecordExecutionAttachmentDO> materials) {
        this(status, ready, manifestHash, materials, null, null, null, null);
    }
}
