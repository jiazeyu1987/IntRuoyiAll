package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt;
import java.util.Objects;

public record MesEdhrVerifiedMaterialGateFact(MesReleaseMaterialGateReceipt ownerReceipt,
        MesEdhrSourceCreationEvidence provenance) {
    public MesEdhrVerifiedMaterialGateFact { Objects.requireNonNull(ownerReceipt); Objects.requireNonNull(provenance); }
}
