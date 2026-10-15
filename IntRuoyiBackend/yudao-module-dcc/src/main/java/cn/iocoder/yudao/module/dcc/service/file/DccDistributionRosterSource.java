package cn.iocoder.yudao.module.dcc.service.file;

import java.time.LocalDateTime;
import java.util.List;

/** Server-only roster boundary. IC32 reads current department members on first effective dispatch; the batch freezes them. */
public interface DccDistributionRosterSource {
    Resolution resolve(Request request);

    record Request(Long tenantId, Long batchId, Long sourceEventId, String eventKey, Long masterId,
                   Long controlledFileId, String versionNo, String approvalProcessInstanceId,
                   LocalDateTime activatedAt, List<String> savedDepartmentIds) {}

    sealed interface Resolution permits Ready, NotReady {}

    /** Evidence identifies the real server-owned source and effective formal policy; it is never supplied by HTTP callers. */
    record Ready(DccDistributionRosterSnapshot snapshot, String sourceIdentity, String policyEvidence) implements Resolution {}

    /** Policy/source unready is an explicit domain outcome. Directory/technical failures must throw. */
    record NotReady(String errorCode, String reason) implements Resolution {}
}
