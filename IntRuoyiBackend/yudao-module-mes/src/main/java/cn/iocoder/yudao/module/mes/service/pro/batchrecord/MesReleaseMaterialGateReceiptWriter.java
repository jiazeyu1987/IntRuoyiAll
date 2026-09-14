package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;

public interface MesReleaseMaterialGateReceiptWriter {

    MesReleaseMaterialGateReceipt persistReady(Long tenantId, Long batchExecutionId,
                                                String sourceSnapshotHash,
                                                MesProEdhrFourMaterialGateResult result,
                                                Long issuedBy);
}
