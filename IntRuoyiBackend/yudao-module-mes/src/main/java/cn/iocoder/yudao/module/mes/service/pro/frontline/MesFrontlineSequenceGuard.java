package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/** Called under the active-order row lock, before any formal submission writes. */
@Service
@RequiredArgsConstructor
public class MesFrontlineSequenceGuard {
    private final MesFrontlineSequenceSettingService settings;
    private final MesProRouteVersionMapper versions;
    private final MesFrontlineActiveOrderProcessService processes;
    private static final ErrorCode BLOCKED = new ErrorCode(1_070_999_001, "工序顺序限制：{}");

    public void requireAllowed(MesProcessPoolActiveOrderDO order, Long routeProcessId) {
        if (!settings.isEnabled()) return;
        var version = versions.selectById(order.getRouteVersionId());
        if (version == null || !Objects.equals(version.getRouteId(), order.getRouteId())) {
            throw exception(BLOCKED, "锁定工艺版本不存在或不匹配");
        }
        JSONObject snapshot;
        try {
            snapshot = JSON.parseObject(version.getRouteSnapshotJson());
        } catch (RuntimeException failure) {
            throw exception(BLOCKED, "锁定工艺快照格式无效");
        }
        JSONObject configs = snapshot == null ? null : snapshot.getJSONObject("configSnapshots");
        JSONObject graph = configs == null ? null : configs.getJSONObject("flowGraph");
        requirePredecessorsComplete(graph, routeProcessId,
                processes.listProcesses(order.getLeaderUserId(), order.getId()));
    }

    static void requirePredecessorsComplete(JSONObject graph, Long current,
                                            List<MesFrontlineActiveOrderProcess> progress) {
        if (graph == null || graph.getJSONArray("nodes") == null
                || graph.getJSONArray("edges") == null || graph.getJSONArray("boundaryEdges") == null) {
            throw exception(BLOCKED, "锁定工艺缺少完整流转关系，请完善工艺版本");
        }
        Map<Long, Set<Long>> predecessors = new LinkedHashMap<>();
        Map<Long, Set<Long>> successors = new LinkedHashMap<>();
        for (Object value : graph.getJSONArray("nodes")) {
            JSONObject node = (JSONObject) value;
            Long id = node.getLong("routeProcessId");
            if (id == null || predecessors.putIfAbsent(id, new LinkedHashSet<>()) != null) {
                throw exception(BLOCKED, "工艺节点身份缺失或重复");
            }
            successors.put(id, new LinkedHashSet<>());
        }
        if (!predecessors.containsKey(current)) throw exception(BLOCKED, "当前工序不属于锁定工艺");
        for (Object value : graph.getJSONArray("edges")) {
            JSONObject edge = (JSONObject) value;
            Long source = edge.getLong("sourceRouteProcessId");
            Long target = edge.getLong("targetRouteProcessId");
            if (!predecessors.containsKey(source) || !predecessors.containsKey(target)
                    || Objects.equals(source, target) || !"NORMAL".equals(edge.getString("relationType"))) {
                throw exception(BLOCKED, "工艺连线无效");
            }
            predecessors.get(target).add(source);
            successors.get(source).add(target);
        }
        Set<Long> starts = new HashSet<>();
        Set<Long> ends = new HashSet<>();
        for (Object value : graph.getJSONArray("boundaryEdges")) {
            JSONObject edge = (JSONObject) value;
            Long id = edge.getLong("routeProcessId");
            if (!predecessors.containsKey(id)) throw exception(BLOCKED, "工艺起止节点无效");
            switch (edge.getString("boundaryType")) {
                case "START" -> starts.add(id);
                case "END" -> ends.add(id);
                default -> throw exception(BLOCKED, "工艺起止关系无效");
            }
        }
        Set<Long> roots = new HashSet<>();
        Set<Long> leaves = new HashSet<>();
        Map<Long, Integer> degrees = new HashMap<>();
        predecessors.forEach((id, incoming) -> {
            degrees.put(id, incoming.size());
            if (incoming.isEmpty()) roots.add(id);
            if (successors.get(id).isEmpty()) leaves.add(id);
        });
        if (!starts.equals(roots) || !ends.equals(leaves)) throw exception(BLOCKED, "工艺起止连线不完整");
        Queue<Long> ready = new ArrayDeque<>(roots);
        int visited = 0;
        while (!ready.isEmpty()) {
            Long id = ready.remove();
            visited++;
            for (Long next : successors.get(id)) {
                if (degrees.compute(next, (key, degree) -> degree - 1) == 0) ready.add(next);
            }
        }
        if (visited != predecessors.size()) throw exception(BLOCKED, "工艺流转关系存在循环");
        Map<Long, MesFrontlineActiveOrderProcess> byId = new HashMap<>();
        progress.forEach(item -> byId.put(item.routeProcessId(), item));
        for (Long previous : predecessors.get(current)) {
            var item = byId.get(previous);
            if (item == null || item.targetQuantity() == null || item.targetQuantity().signum() <= 0
                    || item.submittedQuantity() == null) throw exception(BLOCKED, "前置工序生产数量数据无效");
            if (item.submittedQuantity().compareTo(item.targetQuantity()) < 0) {
                throw exception(BLOCKED, "前置工序“" + item.processName() + "”生产数量尚未达到100%（"
                        + item.submittedQuantity().stripTrailingZeros().toPlainString() + "/"
                        + item.targetQuantity().stripTrailingZeros().toPlainString() + "），不能提交当前工序");
            }
        }
    }
}
