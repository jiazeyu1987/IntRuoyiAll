package cn.iocoder.yudao.module.dcc.service.file;

import java.time.LocalDateTime;
import java.util.List;

/** First effective dispatch's exact roster. Persisted recipients survive retries; invalid accounts block sending. */
public record DccDistributionRosterSnapshot(Long tenantId, Long controlledFileId, String versionNo,
        String approvalProcessInstanceId, LocalDateTime snapshotAt, String policyVersion,
        List<Department> departments) {
    public record Department(Long departmentId, List<Long> userIds) {}
}
