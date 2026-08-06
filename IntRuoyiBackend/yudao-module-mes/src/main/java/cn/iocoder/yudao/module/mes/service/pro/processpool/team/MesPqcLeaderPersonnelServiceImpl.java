package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.api.permission.dto.RoleRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_OCCUPIED_BY_OTHER_LEADER;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_PERMISSION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_ROLE_REQUIRED;

@Service
@Validated
public class MesPqcLeaderPersonnelServiceImpl implements MesPqcLeaderPersonnelService {

    private static final String PQC_PERMISSION_ROLE_CODE = "pqc_permission";
    private static final String OCCUPIED_BY_OTHER_PQC_LEADER_REASON = "已被其他PQC组长选择";

    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private final AdminUserApi adminUserApi;
    private final PermissionApi permissionApi;
    private final RoleApi roleApi;

    public MesPqcLeaderPersonnelServiceImpl(MesProcessPoolTeamLeaderScopeMapper scopeMapper,
                                             AdminUserApi adminUserApi,
                                             PermissionApi permissionApi,
                                             RoleApi roleApi) {
        this.scopeMapper = scopeMapper;
        this.adminUserApi = adminUserApi;
        this.permissionApi = permissionApi;
        this.roleApi = roleApi;
    }

    @Override
    public List<MesPqcLeaderPersonnelBO> listPersonnel(Long leaderUserId, Boolean enabled) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelLeader");
        }
        List<MesProcessPoolTeamLeaderScopeDO> scopes = scopeMapper.selectPqcEmployeeScopes(leaderUserId, enabled);
        List<Long> employeeUserIds = scopes.stream()
                .map(MesProcessPoolTeamLeaderScopeDO::getEmployeeUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserList(employeeUserIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, Function.identity()));
        return scopes.stream()
                .map(scope -> toPersonnel(scope, requireUser(userMap, scope.getEmployeeUserId())))
                .toList();
    }

    @Override
    public List<MesTeamFormalUserCandidateBO> searchFormalInspectorCandidates(Long leaderUserId, String keyword) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelCandidateLeader");
        }
        String normalizedKeyword = normalizeText(keyword);
        Long pqcRoleId = requirePqcPermissionRoleId();
        Set<Long> userIds = permissionApi.getUserRoleIdListByRoleIds(Set.of(pqcRoleId));
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> occupiedByOtherPqcLeaderMap = scopeMapper
                .selectActiveScopesByLeaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .stream()
                .filter(scope -> MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType()))
                .filter(scope -> scope.getEmployeeUserId() != null && scope.getLeaderUserId() != null)
                .filter(scope -> !Objects.equals(scope.getLeaderUserId(), leaderUserId))
                .collect(Collectors.toMap(MesProcessPoolTeamLeaderScopeDO::getEmployeeUserId,
                        MesProcessPoolTeamLeaderScopeDO::getLeaderUserId, (first, ignored) -> first));
        return adminUserApi.getUserList(userIds).stream()
                .filter(Objects::nonNull)
                .filter(MesPqcLeaderPersonnelServiceImpl::isEnabledUser)
                .filter(user -> user.getId() != null)
                .filter(user -> matchesKeyword(user, normalizedKeyword))
                .sorted(Comparator.comparing(MesPqcLeaderPersonnelServiceImpl::resolveUserDisplayName,
                        Comparator.nullsLast(String::compareTo)).thenComparing(AdminUserRespDTO::getId))
                .map(user -> toFormalInspectorCandidate(user,
                        occupiedByOtherPqcLeaderMap.get(user.getId())))
                .toList();
    }

    @Override
    public Long linkFormalInspector(MesPqcLeaderPersonnelLinkReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getSystemUserId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelLink");
        }
        adminUserApi.validateUser(reqBO.getSystemUserId());
        assertPqcInspectorPermission(reqBO.getSystemUserId());
        if (scopeMapper.selectPqcEmployeeScope(reqBO.getLeaderUserId(), reqBO.getSystemUserId()) != null) {
            throw exception(PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_DUPLICATE, reqBO.getSystemUserId());
        }
        assertInspectorNotOccupiedByOtherPqcLeader(reqBO.getLeaderUserId(), reqBO.getSystemUserId());
        MesProcessPoolTeamLeaderScopeDO scope = MesProcessPoolTeamLeaderScopeDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC)
                .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                .employeeUserId(reqBO.getSystemUserId())
                .enabled(Boolean.TRUE)
                .build();
        scopeMapper.insert(scope);
        return scope.getId();
    }

    @Override
    public void updatePersonnelStatus(MesPqcLeaderPersonnelStatusUpdateReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getScopeId() == null
                || reqBO.getEnabled() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelStatus");
        }
        MesProcessPoolTeamLeaderScopeDO scope = scopeMapper.selectById(reqBO.getScopeId());
        if (!isCurrentPqcEmployeeScope(scope, reqBO.getLeaderUserId())) {
            throw exception(PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_NOT_EXISTS, reqBO.getScopeId());
        }
        scopeMapper.updateById(MesProcessPoolTeamLeaderScopeDO.builder()
                .id(scope.getId())
                .enabled(reqBO.getEnabled())
                .build());
    }

    private static MesPqcLeaderPersonnelBO toPersonnel(MesProcessPoolTeamLeaderScopeDO scope,
                                                        AdminUserRespDTO user) {
        return new MesPqcLeaderPersonnelBO()
                .setScopeId(scope.getId())
                .setSystemUserId(scope.getEmployeeUserId())
                .setDisplayName(resolveDisplayName(user))
                .setUsername(user.getUsername())
                .setEnabled(scope.getEnabled());
    }

    private static MesTeamFormalUserCandidateBO toFormalInspectorCandidate(AdminUserRespDTO user,
                                                                            Long occupiedLeaderUserId) {
        boolean occupiedByOtherPqcLeader = occupiedLeaderUserId != null;
        return new MesTeamFormalUserCandidateBO(user.getId(), resolveDisplayName(user))
                .setDisabled(occupiedByOtherPqcLeader)
                .setDisabledReason(occupiedByOtherPqcLeader ? OCCUPIED_BY_OTHER_PQC_LEADER_REASON : null)
                .setOccupiedByOtherPqcLeader(occupiedByOtherPqcLeader)
                .setOccupiedLeaderUserId(occupiedLeaderUserId);
    }

    private static AdminUserRespDTO requireUser(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        AdminUserRespDTO user = userMap.get(userId);
        if (user == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelUserId=" + userId);
        }
        return user;
    }

    private void assertPqcInspectorPermission(Long userId) {
        if (!hasPqcPersonnelPermission(userId)) {
            throw exception(PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_PERMISSION_REQUIRED, userId);
        }
    }

    private void assertInspectorNotOccupiedByOtherPqcLeader(Long leaderUserId, Long employeeUserId) {
        boolean occupiedByOtherPqcLeader = scopeMapper.selectActivePqcEmployeeScopesByEmployeeUserId(employeeUserId)
                .stream()
                .filter(Objects::nonNull)
                .anyMatch(scope -> !Objects.equals(scope.getLeaderUserId(), leaderUserId));
        if (occupiedByOtherPqcLeader) {
            throw exception(PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_OCCUPIED_BY_OTHER_LEADER, employeeUserId);
        }
    }

    private boolean hasPqcPersonnelPermission(Long userId) {
        return userId != null && permissionApi.hasAnyRoles(userId, PQC_PERMISSION_ROLE_CODE);
    }

    private Long requirePqcPermissionRoleId() {
        RoleRespDTO role = roleApi.getRoleByCode(PQC_PERMISSION_ROLE_CODE);
        if (role == null || role.getId() == null || !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())) {
            throw exception(PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_ROLE_REQUIRED, PQC_PERMISSION_ROLE_CODE);
        }
        return role.getId();
    }

    private static boolean isEnabledUser(AdminUserRespDTO user) {
        return CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus());
    }

    private static boolean matchesKeyword(AdminUserRespDTO user, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return true;
        }
        String displayName = resolveUserDisplayName(user);
        String username = normalizeText(user.getUsername());
        return (displayName != null && displayName.contains(normalizedKeyword))
                || (username != null && username.contains(normalizedKeyword));
    }

    private static boolean isPqcEmployeeScope(MesProcessPoolTeamLeaderScopeDO scope) {
        return scope != null
                && MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType());
    }

    private static boolean isCurrentPqcEmployeeScope(MesProcessPoolTeamLeaderScopeDO scope, Long leaderUserId) {
        return scope != null
                && Objects.equals(scope.getLeaderUserId(), leaderUserId)
                && MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PQC.equals(scope.getLeaderType())
                && MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE.equals(scope.getScopeType());
    }

    private static String resolveDisplayName(AdminUserRespDTO user) {
        String nickname = normalizeText(user.getNickname());
        String username = normalizeText(user.getUsername());
        if (nickname != null) {
            return nickname;
        }
        if (username != null) {
            return username;
        }
        throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelDisplayName");
    }

    private static String resolveUserDisplayName(AdminUserRespDTO user) {
        if (user == null) {
            return null;
        }
        String nickname = normalizeText(user.getNickname());
        return nickname != null ? nickname : normalizeText(user.getUsername());
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
