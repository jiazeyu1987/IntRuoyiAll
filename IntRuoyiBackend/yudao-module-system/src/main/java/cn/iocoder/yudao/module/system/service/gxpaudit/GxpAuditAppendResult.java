package cn.iocoder.yudao.module.system.service.gxpaudit;

public record GxpAuditAppendResult(Long eventId, Long ledgerSequence, String eventHash, boolean replayed) {
}
