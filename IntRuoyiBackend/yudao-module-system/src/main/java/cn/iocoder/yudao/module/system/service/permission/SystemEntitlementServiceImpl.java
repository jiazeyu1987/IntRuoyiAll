package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementClaimDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementGrantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementPolicyDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementClaimMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementGrantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementPolicyMapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.service.permission.bo.SystemEntitlementSyncCommand;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemEntitlementServiceImpl implements SystemEntitlementService {

    private static final String SUBJECT_TYPE_USER = "USER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String SYNC_PASS = "PASS";

    @Resource
    private SystemEntitlementPolicyMapper policyMapper;
    @Resource
    private SystemEntitlementClaimMapper claimMapper;
    @Resource
    private SystemEntitlementGrantMapper grantMapper;
    @Resource
    private SystemEntitlementAuditEventMapper auditEventMapper;
    @Resource
    private MenuMapper menuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, allEntries = true)
    public void syncClaims(SystemEntitlementSyncCommand command) {
        validateCommand(command);
        SystemEntitlementPolicyDO policy = validatePolicy(command.getPolicyCode());
        Map<String, List<MenuDO>> menusByPermission = validatePolicyMenus(policy);

        List<SystemEntitlementClaimDO> sourceClaims = claimMapper.selectListBySource(command.getTenantId(),
                command.getSourceType(), command.getSourceKey(), command.getPolicyCode());
        List<SystemEntitlementClaimDO> activeSourceClaims = sourceClaims.stream()
                .filter(claim -> STATUS_ACTIVE.equals(claim.getStatus()))
                .collect(Collectors.toList());
        Map<Long, SystemEntitlementClaimDO> sourceClaimsByUserId = sourceClaims.stream()
                .collect(Collectors.toMap(SystemEntitlementClaimDO::getResolvedUserId, claim -> claim,
                        (first, second) -> first));
        Set<Long> targetUserIds = new LinkedHashSet<>(command.getResolvedUserIds());
        Set<Long> affectedUserIds = new LinkedHashSet<>();
        activeSourceClaims.forEach(claim -> affectedUserIds.add(claim.getResolvedUserId()));
        affectedUserIds.addAll(targetUserIds);

        LocalDateTime now = LocalDateTime.now();
        for (SystemEntitlementClaimDO claim : activeSourceClaims) {
            if (!targetUserIds.contains(claim.getResolvedUserId())) {
                claim.setStatus(STATUS_REVOKED)
                        .setRevokedAt(now)
                        .setLastSyncStatus(SYNC_PASS)
                        .setLastSyncMessage("source subject removed")
                        .setOperatorUserId(command.getOperatorUserId())
                        .setOperatorUsername(command.getOperatorUsername());
                claimMapper.updateById(claim);
                insertAudit(command, claim.getResolvedUserId(), "REVOKE", claim.getSourceDigest(),
                        command.getSourceDigest(), "source subject removed");
            }
        }

        Set<Long> activeUserIds = activeSourceClaims.stream()
                .filter(claim -> STATUS_ACTIVE.equals(claim.getStatus()))
                .map(SystemEntitlementClaimDO::getResolvedUserId)
                .collect(Collectors.toSet());
        for (Long userId : targetUserIds) {
            if (activeUserIds.contains(userId)) {
                SystemEntitlementClaimDO claim = activeSourceClaims.stream()
                        .filter(item -> Objects.equals(userId, item.getResolvedUserId()))
                        .findFirst()
                        .orElseThrow();
                claim.setSourceVersion(command.getSourceVersion())
                        .setSourceDigest(command.getSourceDigest())
                        .setLastSyncStatus(SYNC_PASS)
                        .setLastSyncMessage("source subject unchanged")
                        .setOperatorUserId(command.getOperatorUserId())
                        .setOperatorUsername(command.getOperatorUsername());
                claimMapper.updateById(claim);
                continue;
            }
            SystemEntitlementClaimDO revokedClaim = sourceClaimsByUserId.get(userId);
            if (revokedClaim != null) {
                if (!STATUS_REVOKED.equals(revokedClaim.getStatus())) {
                    throw new IllegalStateException("unsupported entitlement claim status: " + revokedClaim.getStatus());
                }
                String beforeDigest = revokedClaim.getSourceDigest();
                revokedClaim.setSourceVersion(command.getSourceVersion())
                        .setSourceDigest(command.getSourceDigest())
                        .setStatus(STATUS_ACTIVE)
                        .setEffectiveAt(now)
                        .setRevokedAt(null)
                        .setLastSyncStatus(SYNC_PASS)
                        .setLastSyncMessage("source subject reactivated")
                        .setOperatorUserId(command.getOperatorUserId())
                        .setOperatorUsername(command.getOperatorUsername());
                claimMapper.updateById(revokedClaim);
                insertAudit(command, userId, "GRANT", beforeDigest, command.getSourceDigest(),
                        "source subject reactivated");
                continue;
            }
            SystemEntitlementClaimDO newClaim = new SystemEntitlementClaimDO();
            newClaim.setTenantId(command.getTenantId());
            newClaim.setSourceType(command.getSourceType())
                    .setSourceKey(command.getSourceKey())
                    .setSourceVersion(command.getSourceVersion())
                    .setSourceDigest(command.getSourceDigest())
                    .setPolicyCode(command.getPolicyCode())
                    .setSubjectType(SUBJECT_TYPE_USER)
                    .setSubjectId(userId)
                    .setResolvedUserId(userId)
                    .setStatus(STATUS_ACTIVE)
                    .setEffectiveAt(now)
                    .setLastSyncStatus(SYNC_PASS)
                    .setLastSyncMessage("source subject active")
                    .setOperatorUserId(command.getOperatorUserId())
                    .setOperatorUsername(command.getOperatorUsername());
            claimMapper.insert(newClaim);
            insertAudit(command, userId, "GRANT", null, command.getSourceDigest(), "source subject active");
        }

        for (Long userId : affectedUserIds) {
            rebuildUserGrants(command.getTenantId(), userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, allEntries = true)
    public void revokeEntitlementSource(Long tenantId, String sourceType, String sourceKey, String policyCode,
                                        Long operatorUserId, String operatorUsername) {
        validateSourceIdentity(tenantId, sourceType, sourceKey, policyCode);
        validatePolicy(policyCode);

        List<SystemEntitlementClaimDO> activeSourceClaims = claimMapper.selectActiveListBySource(tenantId, sourceType,
                sourceKey, policyCode);
        if (CollUtil.isEmpty(activeSourceClaims)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> affectedUserIds = new LinkedHashSet<>();
        for (SystemEntitlementClaimDO claim : activeSourceClaims) {
            affectedUserIds.add(claim.getResolvedUserId());
            claim.setStatus(STATUS_REVOKED)
                    .setRevokedAt(now)
                    .setLastSyncStatus(SYNC_PASS)
                    .setLastSyncMessage("source explicitly revoked")
                    .setOperatorUserId(operatorUserId)
                    .setOperatorUsername(operatorUsername);
            claimMapper.updateById(claim);
            insertAudit(tenantId, sourceType, sourceKey, policyCode, claim.getResolvedUserId(), "REVOKE",
                    claim.getSourceDigest(), claim.getSourceDigest(), "source explicitly revoked", operatorUserId,
                    operatorUsername);
        }
        for (Long userId : affectedUserIds) {
            rebuildUserGrants(tenantId, userId);
        }
    }

    @Override
    public boolean hasAnyPermission(Long userId, String permissionCode) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || userId == null || StrUtil.isBlank(permissionCode)) {
            return false;
        }
        return CollUtil.isNotEmpty(grantMapper.selectActiveListByUserIdAndPermission(tenantId, userId, permissionCode));
    }

    @Override
    public Set<Long> getActiveMenuIdsByUserId(Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || userId == null) {
            return Set.of();
        }
        return grantMapper.selectActiveListByUserId(tenantId, userId).stream()
                .map(SystemEntitlementGrantDO::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void rebuildUserGrants(Long tenantId, Long userId) {
        grantMapper.revokeByTenantAndUser(tenantId, userId);
        List<SystemEntitlementClaimDO> claims = claimMapper.selectActiveListByUserId(tenantId, userId);
        if (CollUtil.isEmpty(claims)) {
            return;
        }
        Map<String, List<SystemEntitlementClaimDO>> claimsByPolicy = claims.stream()
                .collect(Collectors.groupingBy(SystemEntitlementClaimDO::getPolicyCode));
        for (Map.Entry<String, List<SystemEntitlementClaimDO>> entry : claimsByPolicy.entrySet()) {
            SystemEntitlementPolicyDO policy = validatePolicy(entry.getKey());
            Map<String, List<MenuDO>> menusByPermission = validatePolicyMenus(policy);
            Integer activeClaimCount = entry.getValue().size();
            for (Map.Entry<String, List<MenuDO>> menuEntry : menusByPermission.entrySet()) {
                for (MenuDO menu : menuEntry.getValue()) {
                    upsertGrant(tenantId, userId, entry.getKey(), menuEntry.getKey(), menu.getId(), activeClaimCount);
                }
            }
        }
    }

    private void upsertGrant(Long tenantId, Long userId, String policyCode, String permissionCode, Long menuId,
                             Integer activeClaimCount) {
        SystemEntitlementGrantDO grant = grantMapper.selectByIdentity(tenantId, userId, permissionCode, menuId,
                policyCode);
        if (grant == null) {
            grant = new SystemEntitlementGrantDO();
            grant.setTenantId(tenantId);
            grant.setSubjectType(SUBJECT_TYPE_USER)
                    .setSubjectId(userId)
                    .setResolvedUserId(userId)
                    .setPermissionCode(permissionCode)
                    .setMenuId(menuId)
                    .setPolicyCode(policyCode);
            grant.setActiveClaimCount(activeClaimCount)
                    .setStatus(STATUS_ACTIVE);
            grantMapper.insert(grant);
            return;
        }
        grant.setActiveClaimCount(activeClaimCount)
                .setStatus(STATUS_ACTIVE);
        grantMapper.updateById(grant);
    }

    private void validateCommand(SystemEntitlementSyncCommand command) {
        if (command == null || command.getTenantId() == null || StrUtil.isBlank(command.getSourceType())
                || StrUtil.isBlank(command.getSourceKey()) || StrUtil.isBlank(command.getPolicyCode())) {
            throw new IllegalArgumentException("entitlement sync command is incomplete");
        }
        if (CollUtil.isEmpty(command.getResolvedUserIds())) {
            throw new IllegalArgumentException("entitlement subjects are empty");
        }
    }

    private void validateSourceIdentity(Long tenantId, String sourceType, String sourceKey, String policyCode) {
        if (tenantId == null || StrUtil.isBlank(sourceType) || StrUtil.isBlank(sourceKey)
                || StrUtil.isBlank(policyCode)) {
            throw new IllegalArgumentException("entitlement source identity is incomplete");
        }
    }

    private SystemEntitlementPolicyDO validatePolicy(String policyCode) {
        SystemEntitlementPolicyDO policy = policyMapper.selectByPolicyCode(policyCode);
        if (policy == null || !CommonStatusEnum.ENABLE.getStatus().equals(policy.getStatus())) {
            throw new IllegalArgumentException("entitlement policy not exists or disabled: " + policyCode);
        }
        return policy;
    }

    private Map<String, List<MenuDO>> validatePolicyMenus(SystemEntitlementPolicyDO policy) {
        List<String> allowedPermissions = parseJsonArray(policy.getAllowedPermissionCodesJson());
        List<String> forbiddenPermissions = parseJsonArray(policy.getForbiddenPermissionCodesJson());
        if (CollUtil.isEmpty(allowedPermissions)) {
            throw new IllegalArgumentException("entitlement policy has no permissions: " + policy.getPolicyCode());
        }
        if (CollUtil.containsAny(allowedPermissions, forbiddenPermissions)) {
            throw new IllegalArgumentException("entitlement policy contains forbidden permission: " + policy.getPolicyCode());
        }
        Map<String, List<MenuDO>> result = allowedPermissions.stream()
                .collect(Collectors.toMap(permission -> permission, permission -> {
                    List<MenuDO> menus = menuMapper.selectListByPermission(permission).stream()
                            .filter(menu -> CommonStatusEnum.ENABLE.getStatus().equals(menu.getStatus()))
                            .collect(Collectors.toList());
                    if (CollUtil.isEmpty(menus)) {
                        throw new IllegalArgumentException("entitlement policy permission missing menu: " + permission);
                    }
                    return menus;
                }, (left, right) -> left));
        return result;
    }

    private static List<String> parseJsonArray(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        List<String> values = JsonUtils.parseArray(json, String.class);
        return values == null ? List.of() : new ArrayList<>(values);
    }

    private void insertAudit(SystemEntitlementSyncCommand command, Long userId, String eventType, String beforeDigest,
                             String afterDigest, String message) {
        insertAudit(command.getTenantId(), command.getSourceType(), command.getSourceKey(), command.getPolicyCode(),
                userId, eventType, beforeDigest, afterDigest, message, command.getOperatorUserId(),
                command.getOperatorUsername());
    }

    private void insertAudit(Long tenantId, String sourceType, String sourceKey, String policyCode, Long userId,
                             String eventType, String beforeDigest, String afterDigest, String message,
                             Long operatorUserId, String operatorUsername) {
        SystemEntitlementAuditEventDO auditEvent = new SystemEntitlementAuditEventDO();
        auditEvent.setTenantId(tenantId);
        auditEvent.setEventType(eventType)
                .setSourceType(sourceType)
                .setSourceKey(sourceKey)
                .setPolicyCode(policyCode)
                .setSubjectType(SUBJECT_TYPE_USER)
                .setSubjectId(userId)
                .setBeforeDigest(beforeDigest)
                .setAfterDigest(afterDigest)
                .setResultStatus(SYNC_PASS)
                .setMessage(message)
                .setOperatorUserId(operatorUserId)
                .setOperatorUsername(operatorUsername);
        auditEventMapper.insert(auditEvent);
    }

}
