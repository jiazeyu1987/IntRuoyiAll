package cn.iocoder.yudao.module.system.service.gxpaudit;

public record GxpAuditAppendResult(Long eventId, Long ledgerSequence, String eventHash, boolean replayed, String auditStatus) {
    public GxpAuditAppendResult(Long eventId, Long ledgerSequence, String eventHash, boolean replayed) {
        this(eventId, ledgerSequence, eventHash, replayed, "RECORDED");
    }
    public static GxpAuditAppendResult developmentDisabled() {
        return new GxpAuditAppendResult(-com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(), null, GxpAuditDevelopmentMode.STATUS, false, GxpAuditDevelopmentMode.STATUS);
    }
}
