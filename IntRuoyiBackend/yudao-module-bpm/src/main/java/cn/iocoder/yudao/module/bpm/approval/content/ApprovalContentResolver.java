package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalIdentity;

/** Domain-owned graph reading and authorization. Locked reads require the caller's ledger/identity/authority locks. */
public interface ApprovalContentResolver {
    ApprovalModuleCode moduleCode();
    PreparedApprovalContent prepare(Long actorId, String sourceTaskType, String sourceTaskId, String processInstanceId);
    PreparedApprovalContent lockAndAuthorize(Long actorId, ApprovalIdentity identity);
}
