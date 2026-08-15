package cn.iocoder.yudao.module.mes.service.pro.productionrelease.role;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MesProductionReleaseRequiredCandidateResolverImpl
        implements MesProductionReleaseRequiredCandidateResolver {

    private final RoleApi roleApi;
    private final PermissionApi permissionApi;
    private final AdminUserApi adminUserApi;

    public MesProductionReleaseRequiredCandidateResolverImpl(RoleApi roleApi, PermissionApi permissionApi,
            AdminUserApi adminUserApi) {
        this.roleApi = roleApi;
        this.permissionApi = permissionApi;
        this.adminUserApi = adminUserApi;
    }

    @Override
    public MesProductionReleaseRoleCandidates resolveRequiredCandidates(Long tenantId, String roleCode) {
        validateRoleCode(roleCode);
        Long contextTenantId = TenantContextHolder.getRequiredTenantId();
        if (tenantId == null || !tenantId.equals(contextTenantId)) {
            throw failure(tenantId, roleCode, MesProductionReleaseRoleResolutionReason.TENANT_CONTEXT_MISMATCH);
        }

        RoleRespDTO role = resolveRole(tenantId, roleCode);
        Set<Long> memberIds = permissionApi.getUserRoleIdListByRoleIds(Set.of(role.getId()));
        if (memberIds == null || memberIds.isEmpty()) {
            throw failure(tenantId, roleCode, MesProductionReleaseRoleResolutionReason.CANDIDATE_EMPTY);
        }
        if (memberIds.stream().anyMatch(Objects::isNull)) {
            throw failure(tenantId, roleCode, MesProductionReleaseRoleResolutionReason.ROLE_DATA_INCONSISTENT);
        }

        List<AdminUserRespDTO> users = adminUserApi.getUserList(memberIds);
        if (users == null) {
            throw failure(tenantId, roleCode,
                    MesProductionReleaseRoleResolutionReason.CANDIDATE_USER_NOT_IN_TENANT);
        }
        Map<Long, AdminUserRespDTO> usersById = indexUsers(tenantId, roleCode, users);
        if (!usersById.keySet().equals(memberIds)) {
            throw failure(tenantId, roleCode,
                    MesProductionReleaseRoleResolutionReason.CANDIDATE_USER_NOT_IN_TENANT);
        }
        if (usersById.values().stream()
                .anyMatch(user -> !CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus()))) {
            throw failure(tenantId, roleCode, MesProductionReleaseRoleResolutionReason.CANDIDATE_USER_DISABLED);
        }

        List<Long> candidateUserIds = memberIds.stream().sorted().toList();
        String canonicalSnapshot = tenantId + "|" + role.getId() + "|" + roleCode + "|"
                + candidateUserIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return new MesProductionReleaseRoleCandidates(role.getId(), roleCode, candidateUserIds,
                DigestUtil.sha256Hex(canonicalSnapshot));
    }

    private RoleRespDTO resolveRole(Long tenantId, String roleCode) {
        RoleRespDTO role;
        try {
            role = roleApi.getRoleByCode(roleCode);
        } catch (TooManyResultsException ex) {
            throw new MesProductionReleaseRoleResolutionException(tenantId, roleCode,
                    MesProductionReleaseRoleResolutionReason.ROLE_NOT_UNIQUE, ex);
        }
        if (role == null) {
            throw failure(tenantId, roleCode, MesProductionReleaseRoleResolutionReason.ROLE_NOT_FOUND);
        }
        if (role.getId() == null || !roleCode.equals(role.getCode())) {
            throw failure(tenantId, roleCode, MesProductionReleaseRoleResolutionReason.ROLE_DATA_INCONSISTENT);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())) {
            throw failure(tenantId, roleCode, MesProductionReleaseRoleResolutionReason.ROLE_DISABLED);
        }
        return role;
    }

    private Map<Long, AdminUserRespDTO> indexUsers(Long tenantId, String roleCode, List<AdminUserRespDTO> users) {
        Map<Long, AdminUserRespDTO> usersById = new HashMap<>();
        for (AdminUserRespDTO user : users) {
            if (user == null || user.getId() == null) {
                throw failure(tenantId, roleCode,
                        MesProductionReleaseRoleResolutionReason.CANDIDATE_USER_NOT_IN_TENANT);
            }
            AdminUserRespDTO existing = usersById.putIfAbsent(user.getId(), user);
            if (existing != null && !Objects.equals(existing.getStatus(), user.getStatus())) {
                throw failure(tenantId, roleCode,
                        MesProductionReleaseRoleResolutionReason.ROLE_DATA_INCONSISTENT);
            }
        }
        return usersById;
    }

    private static void validateRoleCode(String roleCode) {
        if (!MesProductionReleaseRoleCodes.isAllowed(roleCode)) {
            throw new IllegalArgumentException("Unsupported production release role code: " + roleCode);
        }
    }

    private static MesProductionReleaseRoleResolutionException failure(Long tenantId, String roleCode,
            MesProductionReleaseRoleResolutionReason reason) {
        return new MesProductionReleaseRoleResolutionException(tenantId, roleCode, reason);
    }
}
