package cn.iocoder.yudao.module.bpm.approval.service.signature;

/** Server-side signature eligibility, evaluated after identity and authority locks. */
public interface ApprovalSignatureAuthorizationService {
    void requireAuthorized(Long actorId);
}
