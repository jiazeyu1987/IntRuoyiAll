package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE;

/**
 * 工艺路线候选版本配置快照服务实现。
 */
@Service
@Validated
public class MesProRouteCandidateConfigServiceImpl implements MesProRouteCandidateConfigService {

    private static final String SNAPSHOT_CONFIGS_KEY = "configSnapshots";

    @Resource
    private MesProRouteVersionMapper routeVersionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigSnapshot(Long candidateRouteVersionId, String configKey, Object configSnapshot) {
        MesProRouteVersionDO candidate = routeVersionMapper.selectById(candidateRouteVersionId);
        if (candidate == null) {
            throw exception(PRO_ROUTE_VERSION_NOT_EXISTS, candidateRouteVersionId);
        }
        if (Boolean.TRUE.equals(candidate.getActive())
                || !MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            throw exception(PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE,
                    candidate.getId(), candidate.getLifecycleStatus());
        }
        validateSourceActiveVersionStillCurrent(candidate);
        if (StrUtil.isBlank(configKey) || configSnapshot == null
                || StrUtil.isBlank(candidate.getRouteSnapshotJson())) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }

        JSONObject snapshot = JSON.parseObject(candidate.getRouteSnapshotJson());
        if (snapshot == null || snapshot.isEmpty()) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }
        Object jsonSnapshot = JSON.parse(JSON.toJSONString(configSnapshot));
        if (jsonSnapshot == null) {
            throw exception(PRO_ROUTE_VERSION_SNAPSHOT_INCOMPLETE, candidate.getId());
        }
        JSONObject configSnapshots = snapshot.getJSONObject(SNAPSHOT_CONFIGS_KEY);
        if (configSnapshots == null) {
            configSnapshots = new JSONObject(true);
            snapshot.put(SNAPSHOT_CONFIGS_KEY, configSnapshots);
        }
        configSnapshots.put(configKey, jsonSnapshot);

        MesProRouteVersionDO update = new MesProRouteVersionDO();
        update.setId(candidate.getId());
        update.setActive(Boolean.FALSE);
        update.setLifecycleStatus(candidate.getLifecycleStatus());
        update.setRouteSnapshotJson(snapshot.toJSONString());
        routeVersionMapper.updateById(update);
    }

    private void validateSourceActiveVersionStillCurrent(MesProRouteVersionDO candidate) {
        if (candidate.getSourceRouteVersionId() == null) {
            return;
        }
        MesProRouteVersionDO activeVersion = routeVersionMapper.selectActiveByRouteId(candidate.getRouteId());
        Long activeVersionId = activeVersion == null ? null : activeVersion.getId();
        if (!candidate.getSourceRouteVersionId().equals(activeVersionId)) {
            throw exception(PRO_ROUTE_VERSION_CONFLICT,
                    candidate.getRouteId(), candidate.getSourceRouteVersionId(), activeVersionId);
        }
    }
}
