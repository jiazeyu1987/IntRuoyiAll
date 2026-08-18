package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ACTIVE_ORDER_PROCESS_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ACTIVE_ORDER_PROCESS_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_FRONTLINE_ROUTE_PROCESS_WORKSTATION_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;

@Service
public class MesFrontlineActiveOrderProcessServiceImpl implements MesFrontlineActiveOrderProcessService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final MesProcessPoolActiveOrderMapper activeOrderMapper;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper;
    private final MesProRouteVersionMapper routeVersionMapper;
    private final MesProProcessMapper processMapper;

    public MesFrontlineActiveOrderProcessServiceImpl(
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProcessPoolActiveOrderProcessSnapshotMapper processSnapshotMapper,
            MesProRouteVersionMapper routeVersionMapper,
            MesProProcessMapper processMapper) {
        this.activeOrderMapper = activeOrderMapper;
        this.processSnapshotMapper = processSnapshotMapper;
        this.routeVersionMapper = routeVersionMapper;
        this.processMapper = processMapper;
    }

    @Override
    public List<MesFrontlineActiveOrderProcess> listProcesses(Long leaderUserId, Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = requireActiveOrder(leaderUserId, activeOrderId);
        MesProRouteVersionDO routeVersion = requireLockedRouteVersion(activeOrder);
        JSONObject routeSnapshot = parseRouteSnapshot(activeOrder, routeVersion);
        Map<ProcessIdentity, JSONObject> nodes = parseProcessNodes(activeOrder, routeSnapshot);
        List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots =
                processSnapshotMapper.selectListByActiveOrderId(activeOrderId);
        if (processSnapshots == null || processSnapshots.isEmpty()) {
            throw snapshotInvalid(activeOrderId, "缺少逐工序目标快照");
        }
        if (processSnapshots.size() != nodes.size()) {
            throw snapshotInvalid(activeOrderId, "冻结流程工序与逐工序目标快照数量不一致");
        }
        String routeCode = normalize(routeSnapshot.getString("routeCode"));
        String routeName = normalize(routeSnapshot.getString("routeName"));
        return processSnapshots.stream()
                .map(snapshot -> toProcess(activeOrder, routeVersion, routeCode, routeName, nodes, snapshot))
                .sorted(Comparator
                        .comparing(MesFrontlineActiveOrderProcess::sort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesFrontlineActiveOrderProcess::routeProcessId))
                .toList();
    }

    @Override
    public MesFrontlineActiveOrderProcess requireProcess(Long leaderUserId, Long activeOrderId, Long routeId,
                                                         Long routeProcessId, Long processId) {
        return listProcesses(leaderUserId, activeOrderId).stream()
                .filter(process -> Objects.equals(process.routeId(), routeId)
                        && Objects.equals(process.routeProcessId(), routeProcessId)
                        && Objects.equals(process.processId(), processId))
                .findFirst()
                .orElseThrow(() -> exception(PRO_FRONTLINE_ACTIVE_ORDER_PROCESS_MISMATCH,
                        activeOrderId, routeProcessId, processId));
    }

    private MesProcessPoolActiveOrderDO requireActiveOrder(Long leaderUserId, Long activeOrderId) {
        MesProcessPoolActiveOrderDO activeOrder = activeOrderMapper.selectById(activeOrderId);
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)
                || !STATUS_ACTIVE.equals(activeOrder.getActiveStatus())
                || !STATUS_ACTIVE.equals(activeOrder.getBusinessStatus())) {
            throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS, activeOrderId);
        }
        if (activeOrder.getRouteId() == null || activeOrder.getRouteVersionId() == null) {
            throw snapshotInvalid(activeOrderId, "缺少锁定工艺路线或版本");
        }
        return activeOrder;
    }

    private MesProRouteVersionDO requireLockedRouteVersion(MesProcessPoolActiveOrderDO activeOrder) {
        MesProRouteVersionDO routeVersion = routeVersionMapper.selectById(activeOrder.getRouteVersionId());
        if (routeVersion == null || !Objects.equals(routeVersion.getRouteId(), activeOrder.getRouteId())) {
            throw snapshotInvalid(activeOrder.getId(), "锁定工艺版本不存在或路线身份不一致");
        }
        return routeVersion;
    }

    private JSONObject parseRouteSnapshot(MesProcessPoolActiveOrderDO activeOrder,
                                          MesProRouteVersionDO routeVersion) {
        if (routeVersion.getRouteSnapshotJson() == null || routeVersion.getRouteSnapshotJson().isBlank()) {
            throw snapshotInvalid(activeOrder.getId(), "锁定工艺版本缺少路线快照");
        }
        try {
            JSONObject snapshot = JSON.parseObject(routeVersion.getRouteSnapshotJson());
            if (snapshot == null || !Objects.equals(snapshot.getLong("routeId"), activeOrder.getRouteId())) {
                throw snapshotInvalid(activeOrder.getId(), "路线快照身份与活跃订单不一致");
            }
            return snapshot;
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw snapshotInvalid(activeOrder.getId(), "路线快照 JSON 无效");
        }
    }

    private Map<ProcessIdentity, JSONObject> parseProcessNodes(MesProcessPoolActiveOrderDO activeOrder,
                                                               JSONObject routeSnapshot) {
        JSONObject configSnapshots = routeSnapshot.getJSONObject("configSnapshots");
        JSONObject flowGraph = configSnapshots == null ? null : configSnapshots.getJSONObject("flowGraph");
        JSONArray nodes = flowGraph == null ? null : flowGraph.getJSONArray("nodes");
        if (nodes == null || nodes.isEmpty()) {
            throw snapshotInvalid(activeOrder.getId(), "路线快照缺少流程工序");
        }
        Map<ProcessIdentity, JSONObject> result = new LinkedHashMap<>();
        for (int index = 0; index < nodes.size(); index++) {
            JSONObject node = nodes.getJSONObject(index);
            Long routeProcessId = node == null ? null : node.getLong("routeProcessId");
            Long processId = node == null ? null : node.getLong("processId");
            if (routeProcessId == null || processId == null
                    || result.putIfAbsent(new ProcessIdentity(routeProcessId, processId), node) != null) {
                throw snapshotInvalid(activeOrder.getId(), "流程工序身份缺失或重复");
            }
        }
        return result;
    }

    private MesFrontlineActiveOrderProcess toProcess(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProRouteVersionDO routeVersion,
            String routeCode,
            String routeName,
            Map<ProcessIdentity, JSONObject> nodes,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        if (snapshot == null || !Objects.equals(snapshot.getActiveOrderId(), activeOrder.getId())
                || !Objects.equals(snapshot.getRouteId(), activeOrder.getRouteId())
                || !Objects.equals(snapshot.getRouteVersionId(), routeVersion.getId())
                || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                || snapshot.getProductionQuantityFactorSnapshot() == null
                || snapshot.getPlannedQuantitySnapshot() == null) {
            throw snapshotInvalid(activeOrder.getId(), "逐工序目标快照身份或数量字段不完整");
        }
        JSONObject node = nodes.get(new ProcessIdentity(snapshot.getRouteProcessId(), snapshot.getProcessId()));
        if (node == null) {
            throw snapshotInvalid(activeOrder.getId(), "逐工序目标快照不属于锁定工艺版本");
        }
        FrozenProcessLabel processLabel = resolveProcessLabel(activeOrder.getId(), snapshot.getProcessId(), node);
        String processCode = processLabel.processCode();
        String processName = processLabel.processName();
        if (processCode == null || processName == null) {
            throw snapshotInvalid(activeOrder.getId(), "流程工序缺少冻结编码或名称");
        }
        Long workstationId = node.getLong("routeProcessWorkstationId");
        if (workstationId == null || workstationId <= 0) {
            throw exception(PRO_FRONTLINE_ROUTE_PROCESS_WORKSTATION_REQUIRED,
                    activeOrder.getRouteId(), snapshot.getProcessId());
        }
        return new MesFrontlineActiveOrderProcess(activeOrder.getId(), activeOrder.getRouteId(), routeVersion.getId(),
                routeCode, routeName, snapshot.getRouteProcessId(), snapshot.getProcessId(), processCode, processName,
                node.getInteger("sort"), workstationId, normalize(node.getString("workstationCode")),
                normalize(node.getString("workstationName")), snapshot.getProductionQuantityFactorSnapshot(),
                snapshot.getPlannedQuantitySnapshot());
    }

    private static ServiceException snapshotInvalid(Long activeOrderId, String detail) {
        return exception(PRO_FRONTLINE_ACTIVE_ORDER_PROCESS_SNAPSHOT_INVALID, activeOrderId, detail);
    }

    private FrozenProcessLabel resolveProcessLabel(Long activeOrderId, Long processId, JSONObject node) {
        String processCode = normalize(node.getString("processCode"));
        String processName = normalize(node.getString("processName"));
        if (processCode != null && processName != null) {
            return new FrozenProcessLabel(processCode, processName);
        }
        MesProProcessDO process = processMapper.selectById(processId);
        if (process == null) {
            throw snapshotInvalid(activeOrderId, "冻结工序主数据不存在");
        }
        if (processCode == null) {
            processCode = normalize(process.getCode());
        }
        if (processName == null) {
            processName = normalize(process.getName());
        }
        return new FrozenProcessLabel(processCode, processName);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record ProcessIdentity(Long routeProcessId, Long processId) {
    }

    private record FrozenProcessLabel(String processCode, String processName) {
    }
}
