package cn.iocoder.yudao.module.system.service.gxpaudit;

public interface GxpAuditService {

    void acquireLedgerLock();

    void assertLedgerLockHeld();

    Long reserveEventId(String operationId, String subjectId, String sourceLocator, String idempotencyKey);

    GxpAuditAppendResult append(GxpAuditCommand command);

}
