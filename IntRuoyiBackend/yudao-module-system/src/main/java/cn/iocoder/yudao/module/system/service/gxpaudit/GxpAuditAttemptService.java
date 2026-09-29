package cn.iocoder.yudao.module.system.service.gxpaudit;

public interface GxpAuditAttemptService {

    GxpAuditAppendResult record(GxpAuditAttemptCommand command);
}
