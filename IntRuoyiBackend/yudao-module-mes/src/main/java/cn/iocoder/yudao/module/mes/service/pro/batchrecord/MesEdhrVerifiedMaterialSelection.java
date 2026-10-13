package cn.iocoder.yudao.module.mes.service.pro.batchrecord;
public record MesEdhrVerifiedMaterialSelection(String sourceKind, Long releaseId, Long taskId,
        String precheckSnapshotHash, MesEdhrVerifiedMaterialGateFact materialFact) {}
