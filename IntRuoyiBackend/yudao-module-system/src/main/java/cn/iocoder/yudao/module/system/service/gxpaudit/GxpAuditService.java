package cn.iocoder.yudao.module.system.service.gxpaudit;

public interface GxpAuditService {

    void acquireLedgerLock();

    GxpAuditAppendResult append(GxpAuditCommand command);

}
