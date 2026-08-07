package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteFlowConfigServiceImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED;

@Service
public class MesRouteStartProductionLeaderAuthorizationServiceImpl
        implements MesRouteStartProductionLeaderAuthorizationService {

    private static final String CANDIDATE_SOURCE_TYPE_USER = "USER";
    private static final String CANDIDATE_SOURCE_TYPE_USERS = "USERS";
    private static final String CANDIDATE_SOURCE_TYPE_ROLE = "ROLE";

    private final MesProRouteMapper routeMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final PermissionApi permissionApi;

    public MesRouteStartProductionLeaderAuthorizationServiceImpl(
            MesProRouteMapper routeMapper,
            MesProRouteProcessMapper routeProcessMapper,
            MesProRouteVersionMapper routeVersionMapper,
            PermissionApi permissionApi) {
        this.routeMapper = routeMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.permissionApi = permissionApi;
    }

    @Override
    public List<MesProRouteDO> listResponsibleRoutes(Long leaderUserId) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "leaderUserId");
        }
        Set<Long> routeIds = resolveConfiguredRouteIds(leaderUserId, listActiveRouteVersions());
        if (routeIds.isEmpty()) {
            return List.of();
        }
        List<MesProRouteDO> routes = routeMapper.selectBatchIds(routeIds);
        Set<Long> loadedRouteIds = routes.stream()
                .filter(Objects::nonNull)
                .map(MesProRouteDO::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean invalidRouteSummary = routes.stream().anyMatch(route -> route == null
                || route.getId() == null
                || route.getName() == null
                || route.getName().isBlank());
        if (!loadedRouteIds.equals(routeIds) || invalidRouteSummary) {
            Set<Long> missingRouteIds = new LinkedHashSet<>(routeIds);
            missingRouteIds.removeAll(loadedRouteIds);
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                    "responsibleRoutes missingRouteIds=" + missingRouteIds);
        }
        return routes.stream()
                .sorted(Comparator.comparing(MesProRouteDO::getId))
                .toList();
    }

    @Override
    public List<MesProRouteProcessDO> listAuthorizedRouteProcesses(Long leaderUserId) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "leaderUserId");
        }
        List<MesProRouteVersionDO> activeVersions = listActiveRouteVersions();
        Set<Long> routeIds = resolveAuthorizedRouteIds(leaderUserId, activeVersions);
        if (routeIds.isEmpty()) {
            return List.of();
        }
        return routeProcessMapper.selectListByRouteIds(routeIds).stream()
                .filter(process -> process != null && process.getId() != null)
                .sorted(Comparator
                        .comparing(MesProRouteProcessDO::getRouteId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesProRouteProcessDO::getId))
                .toList();
    }

    @Override
    public void assertCanMaintainRouteProcess(Long leaderUserId, Long routeProcessId) {
        if (leaderUserId == null || routeProcessId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "routeProcessLossReason");
        }
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(routeProcessId);
        if (routeProcess == null || routeProcess.getRouteId() == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "routeProcessId=" + routeProcessId);
        }
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(routeProcess.getRouteId());
        if (!resolveAuthorizedRouteIds(leaderUserId, activeVersion == null ? List.of() : List.of(activeVersion)).contains(routeProcess.getRouteId())) {
            throw exception(PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED, "路线开始工序");
        }
    }

    private List<MesProRouteVersionDO> listActiveRouteVersions() {
        return routeVersionMapper.selectList(
                new LambdaQueryWrapperX<MesProRouteVersionDO>()
                        .eq(MesProRouteVersionDO::getActive, Boolean.TRUE)
                        .eq(MesProRouteVersionDO::getLifecycleStatus, MesProRouteVersionMapper.STATUS_ACTIVE));
    }

    private Set<Long> resolveAuthorizedRouteIds(Long leaderUserId, List<MesProRouteVersionDO> routeVersions) {
        if (CollUtil.isEmpty(routeVersions)) {
            return Set.of();
        }
        List<MesProRouteVersionDO> activeRouteVersions = routeVersions.stream()
                .filter(this::isActiveRouteVersion)
                .toList();
        if (CollUtil.isEmpty(activeRouteVersions)) {
            return Set.of();
        }
        return resolveConfiguredRouteIds(leaderUserId, activeRouteVersions);
    }

    private Set<Long> resolveConfiguredRouteIds(Long leaderUserId,
                                                List<MesProRouteVersionDO> routeVersions) {
        if (CollUtil.isEmpty(routeVersions)) {
            return Set.of();
        }
        Set<Long> userRoleIds = null;
        Set<Long> authorizedRouteIds = new LinkedHashSet<>();
        for (MesProRouteVersionDO routeVersion : routeVersions) {
            if (!isActiveRouteVersion(routeVersion)) {
                continue;
            }
            for (RouteStartProductionLeaderSnapshot snapshot
                    : parseRouteStartProductionLeaderSnapshots(routeVersion)) {
                if (CANDIDATE_SOURCE_TYPE_USERS.equals(snapshot.candidateSourceType())
                        && snapshot.candidateSourceIds().contains(leaderUserId)) {
                    authorizedRouteIds.add(snapshot.routeId());
                    continue;
                }
                if (CANDIDATE_SOURCE_TYPE_ROLE.equals(snapshot.candidateSourceType())) {
                    if (userRoleIds == null) {
                        Set<Long> roleIds = permissionApi.getUserRoleIdListByUserId(leaderUserId);
                        userRoleIds = roleIds == null ? Set.of() : new LinkedHashSet<>(roleIds);
                    }
                    if (snapshot.candidateSourceIds().stream().anyMatch(userRoleIds::contains)) {
                        authorizedRouteIds.add(snapshot.routeId());
                    }
                }
            }
        }
        return authorizedRouteIds;
    }

    private boolean isActiveRouteVersion(MesProRouteVersionDO routeVersion) {
        return routeVersion != null
                && routeVersion.getRouteId() != null
                && Boolean.TRUE.equals(routeVersion.getActive())
                && MesProRouteVersionMapper.STATUS_ACTIVE.equals(routeVersion.getLifecycleStatus());
    }

    private List<RouteStartProductionLeaderSnapshot> parseRouteStartProductionLeaderSnapshots(
            MesProRouteVersionDO routeVersion) {
        Object snapshot = resolveRouteVersionConfigSnapshot(routeVersion,
                MesProRouteFlowConfigServiceImpl.ROUTE_START_PRODUCTION_LEADERS_KEY);
        if (snapshot == null) {
            return List.of();
        }
        if (!(snapshot instanceof JSONArray items)) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                    "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
        }
        List<RouteStartProductionLeaderSnapshot> result = new ArrayList<>();
        for (Object value : items) {
            JSONObject item = toJsonObject(routeVersion, value);
            Long productionLineId = item.getLong("productionLineId");
            String candidateSourceType = normalizeCandidateSourceType(item.getString("candidateSourceType"));
            List<Long> candidateSourceIds = parseCandidateSourceIds(item.get("candidateSourceIds"));
            if (productionLineId == null
                    || !Objects.equals(productionLineId, routeVersion.getRouteId())
                    || candidateSourceIds.isEmpty()) {
                throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                        "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
            }
            result.add(new RouteStartProductionLeaderSnapshot(routeVersion.getRouteId(), productionLineId,
                    candidateSourceType, candidateSourceIds));
        }
        return result;
    }

    private Object resolveRouteVersionConfigSnapshot(MesProRouteVersionDO routeVersion, String configKey) {
        if (routeVersion == null || routeVersion.getRouteSnapshotJson() == null) {
            return null;
        }
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                    "routeSnapshotJson routeVersionId=" + routeVersion.getId());
        }
        JSONObject configSnapshots = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
        return configSnapshots == null ? null : configSnapshots.get(configKey);
    }

    private JSONObject toJsonObject(MesProRouteVersionDO routeVersion, Object value) {
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(value));
            if (jsonObject != null) {
                return jsonObject;
            }
        } catch (RuntimeException ex) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                    "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
        }
        throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                "routeStartProductionLeaders routeVersionId=" + routeVersion.getId());
    }

    private String normalizeCandidateSourceType(String candidateSourceType) {
        if (CANDIDATE_SOURCE_TYPE_USER.equals(candidateSourceType)
                || CANDIDATE_SOURCE_TYPE_USERS.equals(candidateSourceType)) {
            return CANDIDATE_SOURCE_TYPE_USERS;
        }
        if (CANDIDATE_SOURCE_TYPE_ROLE.equals(candidateSourceType)) {
            return CANDIDATE_SOURCE_TYPE_ROLE;
        }
        throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED,
                "routeStartProductionLeaders candidateSourceType=" + candidateSourceType);
    }

    private List<Long> parseCandidateSourceIds(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }
        if (rawValue instanceof JSONArray array) {
            return array.stream()
                    .map(value -> value == null ? null : Long.valueOf(String.valueOf(value)))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        if (rawValue instanceof List<?> list) {
            return list.stream()
                    .map(value -> value == null ? null : Long.valueOf(String.valueOf(value)))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        String text = String.valueOf(rawValue).trim();
        if (text.isEmpty()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .distinct()
                .toList();
    }

    private record RouteStartProductionLeaderSnapshot(Long routeId,
                                                       Long productionLineId,
                                                       String candidateSourceType,
                                                       List<Long> candidateSourceIds) {
    }

}
