package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_LOCKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED;

@Service
@Validated
public class DccElectronicSignatureAuthorizationServiceImpl implements DccElectronicSignatureAuthorizationService {

    public static final String STATE_UNAUTHORIZED = "UNAUTHORIZED";
    public static final String STATE_ENABLED = "ENABLED";
    public static final String STATE_DISABLED = "DISABLED";
    public static final String STATE_LOCKED = "LOCKED";

    @Resource
    private DccElectronicSignatureAuthorizationMapper authorizationMapper;
    @Resource
    private DccElectronicSignatureAuthorizationAuditService authorizationAuditService;

    @Override
    public boolean isElectronicSignatureEnabled(Long userId) {
        if (userId == null) {
            return false;
        }
        DccElectronicSignatureAuthorizationDO authorization = authorizationMapper.selectByUserId(userId);
        return isAuthorizationEnabledNow(authorization, LocalDateTime.now());
    }

    @Override
    public void validateElectronicSignatureEnabled(Long userId) {
        if (userId == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED);
        }
        DccElectronicSignatureAuthorizationDO authorization = authorizationMapper.selectByUserId(userId);
        validateAuthorizationEnabledNow(authorization, LocalDateTime.now());
    }

    @Override
    public Map<Long, Boolean> getAuthorizationMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Boolean> result = new LinkedHashMap<>();
        userIds.forEach(userId -> result.put(userId, Boolean.FALSE));
        Map<Long, DccElectronicSignatureAuthorizationDO> authorizationMap = CollectionUtils.convertMap(
                authorizationMapper.selectListByUserIds(userIds),
                DccElectronicSignatureAuthorizationDO::getUserId,
                item -> item);
        LocalDateTime now = LocalDateTime.now();
        result.replaceAll((userId, ignored) -> isAuthorizationEnabledNow(authorizationMap.get(userId), now));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuthorization(Long userId, boolean enabled) {
        throw exception(CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAuthorization(Long userId, boolean enabled, Long operatorId, String reason) {
        if (StrUtil.isBlank(reason)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_AUTH_REASON_REQUIRED);
        }
        DccElectronicSignatureAuthorizationDO existing = authorizationMapper.selectByUserId(userId);
        String afterState = enabled ? STATE_ENABLED : STATE_DISABLED;
        if (existing == null) {
            DccElectronicSignatureAuthorizationDO created = DccElectronicSignatureAuthorizationDO.builder()
                    .userId(userId)
                    .electronicSignatureEnabled(enabled)
                    .authorizationState(afterState)
                    .failureCount(0)
                    .build();
            if (authorizationMapper.insert(created) <= 0) {
                throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
            }
            authorizationAuditService.recordAuthorizationChange(DccElectronicSignatureAuthorizationAuditDO.builder()
                    .targetUserId(userId)
                    .operatorId(operatorId)
                    .beforeState(STATE_UNAUTHORIZED)
                    .beforeEnabled(Boolean.FALSE)
                    .afterState(afterState)
                    .afterEnabled(enabled)
                    .reason(StrUtil.trim(reason))
                    .operatedAt(LocalDateTime.now())
                    .build());
            return;
        }
        DccElectronicSignatureAuthorizationDO update = DccElectronicSignatureAuthorizationDO.builder()
                .id(existing.getId())
                .electronicSignatureEnabled(enabled)
                .authorizationState(afterState)
                .lockedUntil(null)
                .lockReason(null)
                .lastFailureAt(null)
                .failureCount(0)
                .build();
        if (authorizationMapper.updateById(update) <= 0) {
            throw exception(CONTROLLED_FILE_SIGNATURE_PERSIST_FAILED);
        }
        authorizationAuditService.recordAuthorizationChange(DccElectronicSignatureAuthorizationAuditDO.builder()
                .targetUserId(userId)
                .operatorId(operatorId)
                .beforeState(StrUtil.blankToDefault(existing.getAuthorizationState(), STATE_UNAUTHORIZED))
                .beforeEnabled(Boolean.TRUE.equals(existing.getElectronicSignatureEnabled()))
                .afterState(afterState)
                .afterEnabled(enabled)
                .reason(StrUtil.trim(reason))
                .operatedAt(LocalDateTime.now())
                .build());
    }

    private static boolean isAuthorizationEnabledNow(DccElectronicSignatureAuthorizationDO authorization,
                                                     LocalDateTime now) {
        if (authorization == null) {
            return false;
        }
        if (!Boolean.TRUE.equals(authorization.getElectronicSignatureEnabled())) {
            return false;
        }
        if (isActiveLock(authorization, now)) {
            return false;
        }
        return Objects.equals(STATE_ENABLED, authorization.getAuthorizationState())
                || isExpiredLock(authorization, now);
    }

    private static void validateAuthorizationEnabledNow(DccElectronicSignatureAuthorizationDO authorization,
                                                         LocalDateTime now) {
        if (authorization == null) {
            throw exception(CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED);
        }
        if (isAuthorizationDisabled(authorization)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_DISABLED);
        }
        if (isActiveLock(authorization, now)) {
            throw exception(CONTROLLED_FILE_SIGNATURE_LOCKED);
        }
        if (Objects.equals(STATE_ENABLED, authorization.getAuthorizationState())
                || isExpiredLock(authorization, now)) {
            return;
        }
        throw exception(CONTROLLED_FILE_SIGNATURE_NOT_AUTHORIZED);
    }

    private static boolean isAuthorizationDisabled(DccElectronicSignatureAuthorizationDO authorization) {
        return !Boolean.TRUE.equals(authorization.getElectronicSignatureEnabled())
                || Objects.equals(STATE_DISABLED, authorization.getAuthorizationState());
    }

    static boolean isActiveLock(DccElectronicSignatureAuthorizationDO authorization, LocalDateTime now) {
        if (authorization == null) {
            return false;
        }
        if (authorization.getLockedUntil() != null && authorization.getLockedUntil().isAfter(now)) {
            return true;
        }
        return Objects.equals(STATE_LOCKED, authorization.getAuthorizationState())
                && authorization.getLockedUntil() == null;
    }

    static boolean isExpiredLock(DccElectronicSignatureAuthorizationDO authorization, LocalDateTime now) {
        return authorization != null
                && Objects.equals(STATE_LOCKED, authorization.getAuthorizationState())
                && authorization.getLockedUntil() != null
                && !authorization.getLockedUntil().isAfter(now);
    }
}
