package cn.iocoder.yudao.module.mdm.service.companyscope;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmRoleCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.dataobject.companyscope.MdmUserCompanyScopeDO;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmRoleCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.dal.mysql.companyscope.MdmUserCompanyScopeMapper;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseStatusEnum;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import cn.iocoder.yudao.module.mdm.service.enterprise.MdmEnterpriseService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_CONFIG_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_FIELD_REQUIRED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_PERMISSION_MISSING;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_RECIPIENT_NOT_FOUND;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_ROLE_COMPANY_SCOPE_DISABLED;
import static cn.iocoder.yudao.module.mdm.service.companyscope.MdmCompanyScopeErrorCodes.MDM_ROLE_COMPANY_SCOPE_MISSING;

@Component
public class MdmCompanyScopeRecipientResolver {

    @Resource
    private MdmEnterpriseService enterpriseService;
    @Resource
    private MdmRoleCompanyScopeMapper roleScopeMapper;
    @Resource
    private MdmUserCompanyScopeMapper userScopeMapper;
    @Resource
    private RoleApi roleApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private AdminUserApi adminUserApi;

    public Set<Long> resolve(Long companyId, Collection<Long> roleIds, String permission) {
        Long requiredCompanyId = requirePositiveId(companyId, "companyId");
        List<Long> requiredRoleIds = validateRoleIds(roleIds);
        String requiredPermission = requirePermission(permission);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        validateRoleScopeRows(tenantId, requiredCompanyId, requiredRoleIds,
                roleScopeMapper.selectByTenantCompanyAndRoleIds(tenantId, requiredCompanyId, requiredRoleIds));
        validateEnabledOwnedCompanyForResolution(requiredCompanyId);
        validateSystemRoles(requiredRoleIds, roleApi.getRoleList(requiredRoleIds));
        LinkedHashSet<Long> authorizedRoleIds = new LinkedHashSet<>(requiredRoleIds);
        for (Long roleId : requiredRoleIds) {
            if (!permissionApi.hasAnyPermissionsInRoles(Set.of(roleId), requiredPermission)) {
                throw exception(MDM_COMPANY_SCOPE_PERMISSION_MISSING, roleId);
            }
        }

        Set<Long> roleUserIds = permissionApi.getUserRoleIdListByRoleIds(authorizedRoleIds);
        validateRawIdSet(roleUserIds);
        Set<Long> enabledCompanyUserIds = indexEnabledCompanyUsers(tenantId, requiredCompanyId,
                userScopeMapper.selectByTenantCompany(tenantId, requiredCompanyId));
        List<Long> candidateIds = roleUserIds.stream()
                .filter(enabledCompanyUserIds::contains)
                .sorted()
                .toList();
        if (candidateIds.isEmpty()) {
            throw exception(MDM_COMPANY_SCOPE_RECIPIENT_NOT_FOUND);
        }

        LinkedHashSet<Long> candidateSet = new LinkedHashSet<>(candidateIds);
        validateSystemUsers(candidateSet, adminUserApi.getUserList(candidateSet));
        return candidateSet;
    }

    private void validateEnabledOwnedCompanyForResolution(Long companyId) {
        try {
            enterpriseService.getEnabledEnterprises(List.of(companyId),
                    Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType()));
        } catch (ServiceException enterpriseFailure) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
    }

    private void validateRoleScopeRows(Long tenantId, Long companyId, List<Long> roleIds,
                                       List<MdmRoleCompanyScopeDO> rows) {
        if (rows == null) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        Set<Long> requestedRoleIds = new LinkedHashSet<>(roleIds);
        Map<Long, MdmRoleCompanyScopeDO> byRoleId = new LinkedHashMap<>();
        for (MdmRoleCompanyScopeDO row : rows) {
            if (!isValidRoleScopeRow(row, tenantId, companyId, requestedRoleIds)
                    || byRoleId.putIfAbsent(row.getRoleId(), row) != null) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
        }
        for (Long roleId : roleIds) {
            MdmRoleCompanyScopeDO row = byRoleId.get(roleId);
            if (row == null || Boolean.TRUE.equals(row.getDeleted())) {
                throw exception(MDM_ROLE_COMPANY_SCOPE_MISSING, roleId);
            }
            if (!MdmEnterpriseStatusEnum.ENABLE.getStatus().equals(row.getStatus())) {
                throw exception(MDM_ROLE_COMPANY_SCOPE_DISABLED, roleId);
            }
        }
    }

    private boolean isValidRoleScopeRow(MdmRoleCompanyScopeDO row, Long tenantId, Long companyId,
                                        Set<Long> requestedRoleIds) {
        return row != null && row.getId() != null && row.getId() > 0
                && Objects.equals(tenantId, row.getTenantId())
                && row.getRoleId() != null && requestedRoleIds.contains(row.getRoleId())
                && Objects.equals(companyId, row.getCompanyId())
                && row.getDeleted() != null
                && row.getRevision() != null && row.getRevision() > 0
                && MdmEnterpriseStatusEnum.isValid(row.getStatus());
    }

    private void validateSystemRoles(List<Long> requestedRoleIds, List<RoleRespDTO> roles) {
        if (roles == null) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID, "result");
        }
        Set<Long> requested = new LinkedHashSet<>(requestedRoleIds);
        Set<Long> returned = new LinkedHashSet<>();
        for (RoleRespDTO role : roles) {
            if (role == null || role.getId() == null || !requested.contains(role.getId())
                    || !returned.add(role.getId()) || StrUtil.isBlank(role.getCode()) || StrUtil.isBlank(role.getName())
                    || !CommonStatusEnum.isEnable(role.getStatus())) {
                throw exception(MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID,
                        role == null ? "null" : role.getId());
            }
        }
        if (!returned.equals(requested)) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_ROLE_INVALID, "missing");
        }
    }

    private Set<Long> indexEnabledCompanyUsers(Long tenantId, Long companyId, List<MdmUserCompanyScopeDO> rows) {
        if (rows == null) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        Map<Long, MdmUserCompanyScopeDO> byUserId = new LinkedHashMap<>();
        for (MdmUserCompanyScopeDO row : rows) {
            if (!isValidUserScopeRow(row, tenantId, companyId)
                    || byUserId.putIfAbsent(row.getUserId(), row) != null) {
                throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
            }
        }
        LinkedHashSet<Long> enabledUserIds = new LinkedHashSet<>();
        byUserId.values().stream()
                .filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
                .filter(row -> MdmEnterpriseStatusEnum.ENABLE.getStatus().equals(row.getStatus()))
                .map(MdmUserCompanyScopeDO::getUserId)
                .sorted()
                .forEach(enabledUserIds::add);
        return enabledUserIds;
    }

    private boolean isValidUserScopeRow(MdmUserCompanyScopeDO row, Long tenantId, Long companyId) {
        return row != null && row.getId() != null && row.getId() > 0
                && Objects.equals(tenantId, row.getTenantId())
                && row.getUserId() != null && row.getUserId() > 0
                && Objects.equals(companyId, row.getCompanyId())
                && row.getDeleted() != null
                && row.getRevision() != null && row.getRevision() > 0
                && MdmEnterpriseStatusEnum.isValid(row.getStatus());
    }

    private void validateSystemUsers(Set<Long> candidateIds, List<AdminUserRespDTO> users) {
        if (users == null) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID, "result");
        }
        Set<Long> returned = new LinkedHashSet<>();
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null || !candidateIds.contains(user.getId())
                    || !returned.add(user.getId()) || StrUtil.isBlank(user.getUsername())
                    || !CommonStatusEnum.isEnable(user.getStatus())) {
                throw exception(MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID,
                        user == null ? "null" : user.getId());
            }
        }
        if (!returned.equals(candidateIds)) {
            throw exception(MDM_COMPANY_SCOPE_SYSTEM_USER_INVALID, "missing");
        }
    }

    private void validateRawIdSet(Set<Long> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
    }

    private List<Long> validateRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "roleIds");
        }
        List<Long> result = new ArrayList<>(roleIds);
        if (result.stream().anyMatch(id -> id == null || id <= 0)) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "roleIds");
        }
        if (new LinkedHashSet<>(result).size() != result.size()) {
            throw exception(MDM_COMPANY_SCOPE_CONFIG_INVALID);
        }
        return result;
    }

    private Long requirePositiveId(Long value, String field) {
        if (value == null || value <= 0) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, field);
        }
        return value;
    }

    private String requirePermission(String permission) {
        String normalized = StrUtil.trimToNull(permission);
        if (normalized == null) {
            throw exception(MDM_COMPANY_SCOPE_FIELD_REQUIRED, "permission");
        }
        return normalized;
    }

}
