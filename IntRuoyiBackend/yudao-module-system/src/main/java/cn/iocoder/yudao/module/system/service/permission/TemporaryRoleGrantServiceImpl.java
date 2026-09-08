package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.temporaryrole.TemporaryRoleGrantRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.TemporaryRoleGrantAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.TemporaryRoleGrantMapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.service.permission.bo.TemporaryRoleGrantCreateCommand;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

@Service
public class TemporaryRoleGrantServiceImpl implements TemporaryRoleGrantService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    @Resource
    private TemporaryRoleGrantMapper temporaryRoleGrantMapper;
    @Resource
    private TemporaryRoleGrantAuditMapper temporaryRoleGrantAuditMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private MenuMapper menuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGrant(TemporaryRoleGrantCreateCommand command) {
        validateCreateCommand(command);
        roleService.validateRoleList(Set.of(command.getRoleId()));
        LocalDateTime now = LocalDateTime.now();
        if (CollUtil.isNotEmpty(temporaryRoleGrantMapper.selectOpenListByUserIdAndRoleId(command.getUserId(),
                command.getRoleId(), now))) {
            throw exception(TEMPORARY_ROLE_GRANT_ACTIVE_DUPLICATE);
        }
        TemporaryRoleGrantDO grant = new TemporaryRoleGrantDO();
        grant.setUserId(command.getUserId())
                .setRoleId(command.getRoleId())
                .setReason(command.getReason().trim())
                .setStatus(STATUS_PENDING)
                .setApplyTime(now)
                .setApplicantUserId(command.getApplicantUserId())
                .setApplicantUsername(command.getApplicantUsername())
                .setExpireTime(command.getExpireTime());
        temporaryRoleGrantMapper.insert(grant);
        insertAudit(grant, "APPLY", null, command.getApplicantUserId(), command.getApplicantUsername(),
                "temporary role grant applied");
        return grant.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, allEntries = true)
    public void approveGrant(Long id, Long approverUserId, String approverUsername) {
        TemporaryRoleGrantDO grant = validateGrant(id);
        if (!STATUS_PENDING.equals(grant.getStatus())) {
            throw exception(TEMPORARY_ROLE_GRANT_STATUS_INVALID, grant.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        if (!grant.getExpireTime().isAfter(now)) {
            throw exception(TEMPORARY_ROLE_GRANT_EXPIRE_TIME_INVALID);
        }
        grant.setStatus(STATUS_ACTIVE)
                .setApproveTime(now)
                .setApproverUserId(approverUserId)
                .setApproverUsername(approverUsername)
                .setEffectiveTime(now);
        temporaryRoleGrantMapper.updateById(grant);
        insertAudit(grant, "APPROVE", null, approverUserId, approverUsername, "temporary role grant approved");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, allEntries = true)
    public void revokeGrant(Long id, Long revokerUserId, String revokerUsername, String reason) {
        TemporaryRoleGrantDO grant = validateGrant(id);
        if (!STATUS_PENDING.equals(grant.getStatus()) && !STATUS_ACTIVE.equals(grant.getStatus())) {
            throw exception(TEMPORARY_ROLE_GRANT_STATUS_INVALID, grant.getStatus());
        }
        if (StrUtil.isBlank(reason)) {
            throw exception(TEMPORARY_ROLE_GRANT_REASON_REQUIRED);
        }
        grant.setStatus(STATUS_REVOKED)
                .setRevokeTime(LocalDateTime.now())
                .setRevokerUserId(revokerUserId)
                .setRevokerUsername(revokerUsername)
                .setRevokeReason(reason.trim());
        temporaryRoleGrantMapper.updateById(grant);
        insertAudit(grant, "REVOKE", null, revokerUserId, revokerUsername, reason.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, allEntries = true)
    public int expireOverdueGrants(LocalDateTime now, Long operatorUserId, String operatorUsername) {
        if (now == null) {
            throw exception(TEMPORARY_ROLE_GRANT_EXPIRE_TIME_INVALID);
        }
        List<TemporaryRoleGrantDO> grants = temporaryRoleGrantMapper.selectOverdueActiveList(now);
        for (TemporaryRoleGrantDO grant : grants) {
            grant.setStatus(STATUS_EXPIRED)
                    .setRevokeTime(now)
                    .setRevokerUserId(operatorUserId)
                    .setRevokerUsername(operatorUsername)
                    .setRevokeReason("expired");
            temporaryRoleGrantMapper.updateById(grant);
            insertAudit(grant, "EXPIRE", null, operatorUserId, operatorUsername, "temporary role grant expired");
        }
        return grants.size();
    }

    @Override
    public Set<Long> getActiveRoleIdsByUserId(Long userId, LocalDateTime now) {
        if (userId == null || now == null) {
            return Set.of();
        }
        return temporaryRoleGrantMapper.selectActiveListByUserId(userId, now).stream()
                .map(TemporaryRoleGrantDO::getRoleId)
                .collect(Collectors.toSet());
    }

    @Override
    public void recordPermissionUse(Long userId, String permissionCode) {
        if (userId == null || StrUtil.isBlank(permissionCode)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<TemporaryRoleGrantDO> grants = temporaryRoleGrantMapper.selectActiveListByUserId(userId, now);
        if (CollUtil.isEmpty(grants)) {
            return;
        }
        List<MenuDO> menus = menuMapper.selectListByPermission(permissionCode);
        if (CollUtil.isEmpty(menus)) {
            return;
        }
        Set<Long> menuIds = menus.stream()
                .filter(menu -> CommonStatusEnum.ENABLE.getStatus().equals(menu.getStatus()))
                .map(MenuDO::getId)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        Set<Long> roleIdsWithPermission = roleMenuMapper.selectListByMenuId(menuIds).stream()
                .map(item -> item.getRoleId())
                .collect(Collectors.toSet());
        for (TemporaryRoleGrantDO grant : grants) {
            if (roleIdsWithPermission.contains(grant.getRoleId())) {
                insertAudit(grant, "USE", permissionCode, userId, "permission-check",
                        "temporary role grant used");
            }
        }
    }

    @Override
    public PageResult<TemporaryRoleGrantRespVO> getGrantPage(TemporaryRoleGrantPageReqVO reqVO) {
        PageResult<TemporaryRoleGrantDO> pageResult = temporaryRoleGrantMapper.selectPage(reqVO);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), TemporaryRoleGrantRespVO.class),
                pageResult.getTotal());
    }

    @Override
    public List<TemporaryRoleGrantAuditDO> getAuditList(Long grantId) {
        validateGrant(grantId);
        return temporaryRoleGrantAuditMapper.selectListByGrantId(grantId);
    }

    private void validateCreateCommand(TemporaryRoleGrantCreateCommand command) {
        if (command == null || command.getUserId() == null || command.getRoleId() == null) {
            throw exception(TEMPORARY_ROLE_GRANT_NOT_EXISTS);
        }
        if (StrUtil.isBlank(command.getReason())) {
            throw exception(TEMPORARY_ROLE_GRANT_REASON_REQUIRED);
        }
        if (command.getExpireTime() == null || !command.getExpireTime().isAfter(LocalDateTime.now())) {
            throw exception(TEMPORARY_ROLE_GRANT_EXPIRE_TIME_INVALID);
        }
    }

    private TemporaryRoleGrantDO validateGrant(Long id) {
        TemporaryRoleGrantDO grant = temporaryRoleGrantMapper.selectById(id);
        if (grant == null) {
            throw exception(TEMPORARY_ROLE_GRANT_NOT_EXISTS);
        }
        return grant;
    }

    private void insertAudit(TemporaryRoleGrantDO grant, String eventType, String permissionCode, Long operatorUserId,
                             String operatorUsername, String message) {
        TemporaryRoleGrantAuditDO audit = new TemporaryRoleGrantAuditDO();
        audit.setTenantId(grant.getTenantId());
        audit.setGrantId(grant.getId())
                .setEventType(eventType)
                .setUserId(grant.getUserId())
                .setRoleId(grant.getRoleId())
                .setPermissionCode(permissionCode)
                .setOperatorUserId(operatorUserId)
                .setOperatorUsername(operatorUsername)
                .setMessage(message);
        temporaryRoleGrantAuditMapper.insert(audit);
    }

}
