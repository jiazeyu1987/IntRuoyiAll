package cn.iocoder.yudao.module.bpm.approval.service;

/** Immutable identity data; not a content, lock, signature or submission authorization credential. */
public record ApprovalIdentity(Long tenantId, String ownerCode, String sourceTaskType,
        String sourceTaskId, String processInstanceId, String domainSubjectType, String domainSubjectId) {
}
