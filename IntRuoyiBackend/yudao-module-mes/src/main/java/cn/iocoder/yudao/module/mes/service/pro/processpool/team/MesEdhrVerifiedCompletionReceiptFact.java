package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence;
import java.util.Objects;

/** Strictly persisted completion creation and its Flow6 projection. */
public record MesEdhrVerifiedCompletionReceiptFact(MesFlow6CompletionBackfillReceipt receipt,
                                                  MesEdhrSourceCreationEvidence creation,
                                                  String transactionId) {
    public MesEdhrVerifiedCompletionReceiptFact {
        Objects.requireNonNull(receipt, "receipt");
        Objects.requireNonNull(creation, "creation");
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Completion transactionId is required");
        }
    }
}
