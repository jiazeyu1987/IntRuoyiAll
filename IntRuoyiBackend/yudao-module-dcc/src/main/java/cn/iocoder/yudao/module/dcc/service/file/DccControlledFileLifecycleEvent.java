package cn.iocoder.yudao.module.dcc.service.file;

import java.time.LocalDateTime;

/** Persisted transition identity. Required domain facts consume synchronously in the
 * lifecycle transaction; external notification delivery starts only after commit. */
public record DccControlledFileLifecycleEvent(String eventKey, String eventType, Long tenantId,
        Long masterId, Long controlledFileId, Long previousActiveFileId, String versionNo,
        String approvalProcessInstanceId, LocalDateTime occurredAt) {
}
