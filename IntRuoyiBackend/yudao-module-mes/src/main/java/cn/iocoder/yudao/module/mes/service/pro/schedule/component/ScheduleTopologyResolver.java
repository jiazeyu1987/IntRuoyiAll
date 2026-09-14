package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 排产路线拓扑解析器。
 */
@Component
public class ScheduleTopologyResolver {

    public List<MesProRouteProcessDO> orderRouteProcessesByDependency(
            MesProScheduleOrderDO scheduleOrder,
            Long workOrderId,
            List<MesProScheduleOrderProcessDO> snapshotProcesses,
            List<MesProRouteProcessDO> routeProcesses) {
        String topologyValidationError = validateRouteProcessTopologySnapshot(
                scheduleOrder, workOrderId, snapshotProcesses, routeProcesses);
        if (topologyValidationError != null) {
            throw new IllegalStateException(topologyValidationError);
        }
        Map<Long, MesProScheduleOrderProcessDO> snapshotMap =
                safeList(snapshotProcesses).stream()
                        .collect(Collectors.toMap(MesProScheduleOrderProcessDO::getRouteProcessId,
                                item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, MesProRouteProcessDO> routeProcessMap =
                buildRouteProcessMapByTopologyKey(snapshotProcesses, routeProcesses);
        Map<Long, Set<Long>> predecessorIdsByRouteProcessId = resolvePredecessors(snapshotProcesses);
        List<MesProScheduleOrderProcessDO> roots = snapshotMap.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getRootProcessFlag()))
                .filter(item -> predecessorIdsByRouteProcessId.getOrDefault(item.getRouteProcessId(), Set.of()).isEmpty())
                .sorted(Comparator.comparing(MesProScheduleOrderProcessDO::getSort,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (roots.isEmpty() || snapshotMap.size() != routeProcessMap.size()) {
            throw new IllegalStateException("排产工序拓扑快照无效，scheduleOrderId=" + scheduleOrder.getId());
        }
        Map<Long, List<MesProScheduleOrderProcessDO>> children = new LinkedHashMap<>();
        snapshotMap.values().forEach(process -> predecessorIdsByRouteProcessId
                .getOrDefault(process.getRouteProcessId(), Set.of())
                .forEach(predecessorId -> children.computeIfAbsent(predecessorId, ignored -> new ArrayList<>())
                        .add(process)));
        Map<Long, Integer> remainingIncoming = predecessorIdsByRouteProcessId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size(),
                        (left, right) -> left, LinkedHashMap::new));
        List<MesProRouteProcessDO> ordered = new ArrayList<>();
        Queue<Long> pending = new ArrayDeque<>();
        roots.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .forEach(pending::add);
        while (!pending.isEmpty()) {
            Long routeProcessId = pending.remove();
            MesProRouteProcessDO routeProcess = routeProcessMap.get(routeProcessId);
            if (routeProcess == null) {
                throw new IllegalStateException("排产拓扑引用不存在的路线工序，routeProcessId=" + routeProcessId);
            }
            ordered.add(routeProcess);
            children.getOrDefault(routeProcessId, Collections.emptyList()).stream()
                    .sorted(Comparator.comparing(MesProScheduleOrderProcessDO::getSort,
                            Comparator.nullsLast(Integer::compareTo)))
                    .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                    .forEach(childId -> {
                        int remaining = remainingIncoming.merge(childId, -1, Integer::sum);
                        if (remaining == 0) {
                            pending.add(childId);
                        }
                    });
        }
        if (ordered.size() != routeProcessMap.size()) {
            throw new IllegalStateException("排产工序拓扑快照存在断点或循环，scheduleOrderId=" + scheduleOrder.getId());
        }
        return ordered;
    }

    public String validateRouteProcessTopologySnapshot(
            MesProScheduleOrderDO scheduleOrder,
            Long workOrderId,
            List<MesProScheduleOrderProcessDO> snapshotProcesses,
            List<MesProRouteProcessDO> routeProcesses) {
        if (scheduleOrder == null) {
            return "工单缺少排产快照，workOrderId=" + workOrderId;
        }
        Set<Long> routeProcessIds = safeList(routeProcesses).stream()
                .map(MesProRouteProcessDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> snapshotRouteProcessIds = safeList(snapshotProcesses).stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MesProRouteProcessDO> routeProcessMap =
                buildRouteProcessMapByTopologyKey(snapshotProcesses, routeProcesses);
        if (routeProcessIds.size() != safeList(routeProcesses).size()
                || snapshotRouteProcessIds.size() != safeList(snapshotProcesses).size()
                || routeProcessMap.size() != snapshotRouteProcessIds.size()) {
            return "排产工序拓扑快照无效，scheduleOrderId=" + scheduleOrder.getId();
        }
        Map<Long, Set<Long>> predecessorIdsByRouteProcessId;
        try {
            predecessorIdsByRouteProcessId = resolvePredecessors(snapshotProcesses);
        } catch (IllegalStateException ex) {
            return "排产工序拓扑快照无效，scheduleOrderId=" + scheduleOrder.getId();
        }
        List<MesProScheduleOrderProcessDO> roots = safeList(snapshotProcesses).stream()
                .filter(item -> Boolean.TRUE.equals(item.getRootProcessFlag()))
                .filter(item -> predecessorIdsByRouteProcessId.getOrDefault(item.getRouteProcessId(), Set.of()).isEmpty())
                .toList();
        boolean rootFlagsConsistent = safeList(snapshotProcesses).stream().allMatch(item ->
                Boolean.TRUE.equals(item.getRootProcessFlag())
                        == predecessorIdsByRouteProcessId.getOrDefault(item.getRouteProcessId(), Set.of()).isEmpty());
        if (roots.isEmpty() || !rootFlagsConsistent) {
            return "排产工序拓扑快照无效，scheduleOrderId=" + scheduleOrder.getId();
        }
        for (MesProScheduleOrderProcessDO process : safeList(snapshotProcesses)) {
            for (Long predecessorRouteProcessId : predecessorIdsByRouteProcessId
                    .getOrDefault(process.getRouteProcessId(), Set.of())) {
                if (!snapshotRouteProcessIds.contains(predecessorRouteProcessId)) {
                    return "排产工序直接前置快照不存在，scheduleOrderId="
                            + scheduleOrder.getId() + ", routeProcessId=" + process.getRouteProcessId();
                }
            }
        }
        Map<Long, List<Long>> children = new LinkedHashMap<>();
        predecessorIdsByRouteProcessId.forEach((routeProcessId, predecessorIds) -> predecessorIds.forEach(predecessorId ->
                children.computeIfAbsent(predecessorId, ignored -> new ArrayList<>()).add(routeProcessId)));
        Map<Long, Integer> remainingIncoming = predecessorIdsByRouteProcessId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size(),
                        (left, right) -> left, LinkedHashMap::new));
        Set<Long> visited = new LinkedHashSet<>();
        Queue<Long> pending = new ArrayDeque<>();
        roots.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .forEach(pending::add);
        while (!pending.isEmpty()) {
            Long routeProcessId = pending.remove();
            if (!visited.add(routeProcessId)) {
                continue;
            }
            for (Long childId : children.getOrDefault(routeProcessId, Collections.emptyList())) {
                int remaining = remainingIncoming.merge(childId, -1, Integer::sum);
                if (remaining == 0) {
                    pending.add(childId);
                }
            }
        }
        if (visited.size() != routeProcessMap.size()) {
            return "排产工序拓扑快照存在断点或循环，scheduleOrderId=" + scheduleOrder.getId();
        }
        return null;
    }

    private Map<Long, Set<Long>> resolvePredecessors(List<MesProScheduleOrderProcessDO> snapshotProcesses) {
        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO process : safeList(snapshotProcesses)) {
            if (process == null || process.getRouteProcessId() == null) {
                continue;
            }
            result.put(process.getRouteProcessId(), ScheduleTopologyPredecessors.resolve(process));
        }
        return result;
    }

    private Map<Long, MesProRouteProcessDO> buildRouteProcessMapByTopologyKey(
            List<MesProScheduleOrderProcessDO> snapshotProcesses,
            List<MesProRouteProcessDO> routeProcesses) {
        List<MesProScheduleOrderProcessDO> safeSnapshotProcesses = safeList(snapshotProcesses);
        List<MesProRouteProcessDO> safeRouteProcesses = safeList(routeProcesses);
        if (safeSnapshotProcesses.size() != safeRouteProcesses.size()) {
            return Collections.emptyMap();
        }
        Map<Long, MesProRouteProcessDO> routeProcessById = safeRouteProcesses.stream()
                .filter(routeProcess -> routeProcess.getId() != null)
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, item -> item,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Integer, List<MesProRouteProcessDO>> routeProcessesBySort = safeRouteProcesses.stream()
                .filter(routeProcess -> routeProcess.getSort() != null)
                .collect(Collectors.groupingBy(MesProRouteProcessDO::getSort,
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, MesProRouteProcessDO> routeProcessMap = new LinkedHashMap<>();
        Set<Long> mappedRuntimeRouteProcessIds = new LinkedHashSet<>();
        for (MesProScheduleOrderProcessDO snapshotProcess : safeSnapshotProcesses) {
            if (snapshotProcess.getRouteProcessId() == null) {
                return Collections.emptyMap();
            }
            MesProRouteProcessDO routeProcess = routeProcessById.get(snapshotProcess.getRouteProcessId());
            if (routeProcess == null && snapshotProcess.getSort() != null) {
                List<MesProRouteProcessDO> sortMatches =
                        routeProcessesBySort.getOrDefault(snapshotProcess.getSort(), Collections.emptyList());
                if (sortMatches.size() == 1) {
                    routeProcess = sortMatches.get(0);
                }
            }
            if (routeProcess == null || routeProcess.getId() == null
                    || !mappedRuntimeRouteProcessIds.add(routeProcess.getId())
                    || routeProcessMap.put(snapshotProcess.getRouteProcessId(), routeProcess) != null) {
                return Collections.emptyMap();
            }
        }
        return routeProcessMap;
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

}
