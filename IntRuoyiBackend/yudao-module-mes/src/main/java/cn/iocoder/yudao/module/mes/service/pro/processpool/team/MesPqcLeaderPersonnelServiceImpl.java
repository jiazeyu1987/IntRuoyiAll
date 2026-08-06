package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_NOT_EXISTS;

@Service
@Validated
public class MesPqcLeaderPersonnelServiceImpl implements MesPqcLeaderPersonnelService {

    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private final AdminUserApi adminUserApi;

    public MesPqcLeaderPersonnelServiceImpl(MesProcessPoolTeamLeaderScopeMapper scopeMapper,
                                             AdminUserApi adminUserApi) {
        this.scopeMapper = scopeMapper;
        this.adminUserApi = adminUserApi;
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
    public Long linkFormalInspector(MesPqcLeaderPersonnelLinkReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getSystemUserId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelLink");
        }
        adminUserApi.validateUser(reqBO.getSystemUserId());
        if (scopeMapper.selectPqcEmployeeScope(reqBO.getLeaderUserId(), reqBO.getSystemUserId()) != null) {
            throw exception(PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_DUPLICATE, reqBO.getSystemUserId());
        }
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

    private static AdminUserRespDTO requireUser(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        AdminUserRespDTO user = userMap.get(userId);
        if (user == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "pqcPersonnelUserId=" + userId);
        }
        return user;
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

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
