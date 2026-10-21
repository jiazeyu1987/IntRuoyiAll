package cn.iocoder.yudao.module.mes.service.pro.batchrecord;
public record MesEdhrVerifiedMaterialSelection(String sourceKind, Long releaseId, Long taskId,
        String precheckSnapshotHash, MesEdhrVerifiedMaterialGateFact materialFact,
        cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesEdhrVerifiedActiveOrderReleaseFact activeOrderFact) {
    public MesEdhrVerifiedMaterialSelection {
        if((materialFact==null)==(activeOrderFact==null)) throw new IllegalArgumentException("RELEASE_SELECTION_EXACT_FACT_BRANCH_REQUIRED");
    }
}
