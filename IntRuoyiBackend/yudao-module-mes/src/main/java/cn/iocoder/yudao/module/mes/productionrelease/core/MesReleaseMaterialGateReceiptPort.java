package cn.iocoder.yudao.module.mes.productionrelease.core;

/**
 * Flow 8 owner boundary. Implementations must load a persisted, tenant-scoped
 * MATERIALS_READY receipt; Flow 10 never derives or accepts its payload from HTTP.
 */
public interface MesReleaseMaterialGateReceiptPort {

    MesReleaseMaterialGateReceipt getVerifiedByReceiptId(Long tenantId, Long batchExecutionId,
                                                          String receiptId, String sourceSnapshotHash);

    MesReleaseMaterialGateReceipt getLatestVerified(Long tenantId, Long batchExecutionId,
                                                    String sourceSnapshotHash);
}
