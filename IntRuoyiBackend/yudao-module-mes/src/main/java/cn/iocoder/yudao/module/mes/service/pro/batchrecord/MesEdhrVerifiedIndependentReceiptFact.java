package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import java.util.Objects;

public record MesEdhrVerifiedIndependentReceiptFact(MesIndependentBatchPrerequisiteReceipt ownerReceipt,
        MesEdhrSourceCreationEvidence provenance) {
    public MesEdhrVerifiedIndependentReceiptFact { Objects.requireNonNull(ownerReceipt); Objects.requireNonNull(provenance); }
}
