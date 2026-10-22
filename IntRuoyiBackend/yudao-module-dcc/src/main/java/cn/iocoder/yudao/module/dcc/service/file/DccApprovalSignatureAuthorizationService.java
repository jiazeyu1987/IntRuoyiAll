package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureAuthorizationService;
import org.springframework.stereotype.Service;

@Service
public class DccApprovalSignatureAuthorizationService implements ApprovalSignatureAuthorizationService {
    private final DccElectronicSignatureAuthorizationService authorization;

    public DccApprovalSignatureAuthorizationService(DccElectronicSignatureAuthorizationService authorization) {
        this.authorization = authorization;
    }

    @Override
    public void requireAuthorized(Long actorId) {
        authorization.validateElectronicSignatureEnabledLocked(actorId);
    }
}
