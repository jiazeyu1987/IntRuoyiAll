package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureFailureAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignaturePolicyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureFailureAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignaturePolicyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_POLICY_MISSING;

@Service
@Validated
public class DccElectronicSignatureFailureAuditServiceImpl implements DccElectronicSignatureFailureAuditService {

    public static final String FAILURE_TYPE_PASSWORD_INVALID = "PASSWORD_INVALID";
    public static final String LOCK_REASON_PASSWORD_FAILURE_THRESHOLD = "PASSWORD_FAILURE_THRESHOLD";

    @Resource
    private DccElectronicSignatureFailureAuditMapper failureAuditMapper;
    @Resource
    private DccElectronicSignatureAuthorizationMapper authorizationMapper;
    @Resource
    private DccElectronicSignaturePolicyMapper policyMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean recordPasswordFailure(DccElectronicSignatureFailureAuditCommand command) {
        DccElectronicSignaturePolicyDO policy = policyMapper.selectEnabledPolicy();
        if (!isValidPolicy(policy)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_POLICY_MISSING);
        }
        DccElectronicSignatureAuthorizationDO authorization = authorizationMapper.selectByUserId(command.getTargetUserId());
        if (authorization == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED);
        }
        LocalDateTime failedAt = command.getFailedAt() != null ? command.getFailedAt() : LocalDateTime.now();
        if (failureAuditMapper.insert(DccElectronicSignatureFailureAuditDO.builder()
                .targetUserId(command.getTargetUserId())
                .controlledFileId(command.getControlledFileId())
                .revisionId(command.getRevisionId())
                .taskId(command.getTaskId())
                .actionType(command.getActionType())
                .meaningCode(command.getMeaningCode())
                .failureType(FAILURE_TYPE_PASSWORD_INVALID)
                .failureMessage(StrUtil.blankToDefault(command.getFailureMessage(), "password verification failed"))
                .failedAt(failedAt)
                .remoteIp(command.getRemoteIp())
                .userAgent(command.getUserAgent())
                .build()) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        return updateFailureLockState(authorization, policy, failedAt);
    }

    private boolean updateFailureLockState(DccElectronicSignatureAuthorizationDO authorization,
                                           DccElectronicSignaturePolicyDO policy,
                                           LocalDateTime failedAt) {
        int currentFailureCount = authorization.getFailureCount() == null ? 0 : authorization.getFailureCount();
        LocalDateTime windowStart = failedAt.minusMinutes(policy.getPasswordFailureWindowMinutes());
        int nextFailureCount = authorization.getLastFailureAt() != null
                && !authorization.getLastFailureAt().isBefore(windowStart)
                ? currentFailureCount + 1 : 1;
        DccElectronicSignatureAuthorizationDO update = DccElectronicSignatureAuthorizationDO.builder()
                .id(authorization.getId())
                .failureCount(nextFailureCount)
                .lastFailureAt(failedAt)
                .build();
        boolean locked = nextFailureCount >= policy.getPasswordFailureThreshold();
        if (locked) {
            update.setAuthorizationState(DccElectronicSignatureAuthorizationServiceImpl.STATE_LOCKED);
            update.setLockedUntil(failedAt.plusMinutes(policy.getLockMinutes()));
            update.setLockReason(LOCK_REASON_PASSWORD_FAILURE_THRESHOLD);
        }
        if (authorizationMapper.updateById(update) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        return locked;
    }

    private static boolean isValidPolicy(DccElectronicSignaturePolicyDO policy) {
        return policy != null
                && policy.getPasswordFailureWindowMinutes() != null && policy.getPasswordFailureWindowMinutes() > 0
                && policy.getPasswordFailureThreshold() != null && policy.getPasswordFailureThreshold() > 0
                && policy.getLockMinutes() != null && policy.getLockMinutes() > 0;
    }
}
