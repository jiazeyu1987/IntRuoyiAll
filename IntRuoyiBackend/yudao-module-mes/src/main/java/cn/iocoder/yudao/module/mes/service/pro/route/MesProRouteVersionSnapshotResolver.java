package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class MesProRouteVersionSnapshotResolver {

    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProRouteSnapshotCanonicalizer canonicalizer;

    public MesProRouteVersionSnapshotResolver(MesProRouteVersionMapper routeVersionMapper,
                                              MesProRouteSnapshotCanonicalizer canonicalizer) {
        this.routeVersionMapper = routeVersionMapper;
        this.canonicalizer = canonicalizer;
    }

    public ResolvedRouteProcessSnapshot resolve(Long routeVersionId, Long routeProcessId) {
        if (routeVersionId == null || routeProcessId == null) {
            throw new IllegalArgumentException("routeVersionId and routeProcessId are required");
        }
        ResolvedRouteVersionSnapshot resolvedVersion = resolveVersion(routeVersionId);
        JSONObject snapshot = JSON.parseObject(resolvedVersion.routeSnapshotJson());
        JSONArray nodes = snapshot.getJSONObject("configSnapshots")
                .getJSONObject("flowGraph").getJSONArray("nodes");
        List<JSONObject> matches = new ArrayList<>();
        for (Object value : nodes) {
            JSONObject node = (JSONObject) value;
            if (routeProcessId.equals(node.getLong("routeProcessId"))) {
                matches.add(node);
            }
        }
        if (matches.size() != 1) {
            throw new IllegalStateException("frozen route process must resolve exactly once: routeVersionId="
                    + routeVersionId + ", routeProcessId=" + routeProcessId + ", matches=" + matches.size());
        }
        JSONObject node = matches.get(0);
        return new ResolvedRouteProcessSnapshot(resolvedVersion.routeId(), resolvedVersion.routeVersionId(),
                routeProcessId, node.getLong("processId"), node.getLong("routeProcessWorkstationId"),
                node.getInteger("sort"), node.getString("processName"), node.toJSONString(),
                resolvedVersion.routeSnapshotSha256());
    }

    public ResolvedRouteVersionSnapshot resolveVersion(Long routeVersionId) {
        if (routeVersionId == null) {
            throw new IllegalArgumentException("routeVersionId is required");
        }
        if (!Integer.valueOf(1).equals(routeVersionMapper.selectSnapshotIdentityEnforcementReady())) {
            throw new IllegalStateException("route snapshot consumer enforcement is not ready");
        }
        MesProRouteVersionDO version = routeVersionMapper.selectById(routeVersionId);
        if (version == null) {
            throw new IllegalStateException("route version does not exist: " + routeVersionId);
        }
        if (!Set.of(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE,
                MesProRouteVersionLifecycleServiceImpl.STATUS_SUPERSEDED).contains(version.getLifecycleStatus())) {
            throw new IllegalStateException("route version is not published: " + routeVersionId);
        }
        if (MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE.equals(version.getLifecycleStatus())
                != Boolean.TRUE.equals(version.getActive())) {
            throw new IllegalStateException("route version publication state is inconsistent: " + routeVersionId);
        }
        if (!MesProRouteSnapshotCanonicalizer.FORMAT_VERSION.equals(version.getRouteSnapshotFormatVersion())
                || StrUtil.isBlank(version.getRouteSnapshotSha256())) {
            throw new IllegalStateException("route snapshot identity is not ready: " + routeVersionId);
        }
        String calculatedHash = canonicalizer.sha256(version.getRouteSnapshotJson());
        if (!calculatedHash.equals(version.getRouteSnapshotSha256())) {
            throw new IllegalStateException("route snapshot hash mismatch: " + routeVersionId);
        }
        MesProRouteSnapshotCanonicalizer.Validation validation =
                canonicalizer.validate(version.getRouteId(), version.getRouteSnapshotJson());
        if (!validation.ready()) {
            throw new IllegalStateException("route snapshot is invalid: " + validation.blockers());
        }
        return new ResolvedRouteVersionSnapshot(version.getRouteId(), version.getId(),
                version.getRouteSnapshotJson(), version.getRouteSnapshotSha256());
    }

    public record ResolvedRouteVersionSnapshot(Long routeId, Long routeVersionId,
                                               String routeSnapshotJson, String routeSnapshotSha256) {
    }

    public record ResolvedRouteProcessSnapshot(Long routeId, Long routeVersionId, Long routeProcessId,
                                               Long processId, Long workstationId, Integer sort,
                                               String processNameSnapshot, String configSnapshotJson,
                                               String routeSnapshotSha256) {
    }
}
