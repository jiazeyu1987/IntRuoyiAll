package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort {

    /**
     * Reads only an immutable Flow-4 receipt belonging to the supplied tenant.
     * Flow-6 must not infer a receipt or create one when this lookup fails.
     */
    MesFlow6CompletionBackfillReceipt getByReceiptId(Long receiptId, Long tenantId);
}
