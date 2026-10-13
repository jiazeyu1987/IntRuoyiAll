package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalRequestDO;

/** Reads the existing domain relationship; it never creates an owner or grants permission. */
public interface EdhrEngineOwnershipReader {
    NativeApprovalOwnership read(BusinessApprovalRequestDO request, EngineApprovalIdentity identity, Long userId);

    EdhrDomainAssignmentProof readDomainAssignment(BusinessApprovalRequestDO request, EngineApprovalIdentity identity,
            Long workTaskId, Long authenticatedActorId);

    EdhrDomainAssignmentProof acquireDomainAssignment(BusinessApprovalRequestDO request, EngineApprovalIdentity identity,
            Long workTaskId, Long authenticatedActorId);
}
