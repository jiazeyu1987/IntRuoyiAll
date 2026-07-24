package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RouteSnapshotResolver {

    private final MesProRouteProcessService routeProcessService;
    private final MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;

    public RouteSnapshotResolver(MesProRouteProcessService routeProcessService,
                                 MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper) {
        this.routeProcessService = routeProcessService;
        this.routeProcessFlowEdgeMapper = routeProcessFlowEdgeMapper;
    }

    public ResolvedRoutePlan resolve(Long routeId,
                                     List<MesProRouteProcessDO> routeProcesses,
                                     List<MesProScheduleOrderProcessDO> snapshotProcesses) {
        Map<Long, Set<Long>> workstationProcessAliasesByRouteProcessId = new LinkedHashMap<>();
        List<MesProRouteProcessDO> resolvedRouteProcesses = mergeSnapshotRemainingRouteProcesses(
                routeId, safeList(routeProcesses), snapshotProcesses, workstationProcessAliasesByRouteProcessId);
        resolvedRouteProcesses = alignRouteProcessesWithTopologySnapshot(
                routeId, resolvedRouteProcesses, snapshotProcesses, workstationProcessAliasesByRouteProcessId);
        RouteTopologyRecovery topologyRecovery = recoverMissingTopologyFromRouteEdges(routeId, snapshotProcesses);
        return new ResolvedRoutePlan(resolvedRouteProcesses, topologyRecovery.scheduleOrderProcesses(),
                workstationProcessAliasesByRouteProcessId, topologyRecovery.validationError());
    }

    private RouteTopologyRecovery recoverMissingTopologyFromRouteEdges(
            Long routeId, List<MesProScheduleOrderProcessDO> snapshotProcesses) {
        List<MesProScheduleOrderProcessDO> safeSnapshotProcesses = safeList(snapshotProcesses);
        List<MesProScheduleOrderProcessDO> activeSnapshotProcesses =
                activeTopologyScheduleOrderProcesses(safeSnapshotProcesses);
        if (CollUtil.isEmpty(activeSnapshotProcesses)) {
            return new RouteTopologyRecovery(safeSnapshotProcesses, null);
        }
        if (hasRouteProcessTopologySnapshot(activeSnapshotProcesses)) {
            return hasInactiveTopologyPredecessor(activeSnapshotProcesses)
                    ? new RouteTopologyRecovery(safeSnapshotProcesses, routeTopologyValidationError(routeId))
                    : new RouteTopologyRecovery(safeSnapshotProcesses, null);
        }
        if (activeSnapshotProcesses.size() == 1) {
            MesProScheduleOrderProcessDO process = activeSnapshotProcesses.get(0);
            process.setPredecessorRouteProcessId(null);
            process.setRootProcessFlag(Boolean.TRUE);
            return new RouteTopologyRecovery(safeSnapshotProcesses, null);
        }
        Map<Long, Long> predecessorMap = buildRouteEdgePredecessorMap(routeId, activeSnapshotProcesses);
        if (predecessorMap == null) {
            return new RouteTopologyRecovery(safeSnapshotProcesses, routeTopologyValidationError(routeId));
        }
        activeSnapshotProcesses.forEach(process -> {
            Long predecessorRouteProcessId = predecessorMap.get(process.getRouteProcessId());
            process.setPredecessorRouteProcessId(predecessorRouteProcessId);
            process.setRootProcessFlag(predecessorRouteProcessId == null);
        });
        return new RouteTopologyRecovery(safeSnapshotProcesses, null);
    }

    private Map<Long, Long> buildRouteEdgePredecessorMap(
            Long routeId, List<MesProScheduleOrderProcessDO> activeSnapshotProcesses) {
        if (routeId == null || CollUtil.isEmpty(activeSnapshotProcesses)) {
            return null;
        }
        Set<Long> routeProcessIds = activeSnapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (routeProcessIds.size() != activeSnapshotProcesses.size()) {
            return null;
        }
        Map<Long, Set<Long>> outgoingMap = new LinkedHashMap<>();
        Map<Long, Set<Long>> incomingMap = new LinkedHashMap<>();
        routeProcessIds.forEach(id -> {
            outgoingMap.put(id, new LinkedHashSet<>());
            incomingMap.put(id, new LinkedHashSet<>());
        });
        Set<String> seenEdges = new LinkedHashSet<>();
        for (MesProRouteProcessFlowEdgeDO edge : safeList(routeProcessFlowEdgeMapper.selectListByRouteId(routeId))) {
            if (edge == null) {
                continue;
            }
            Long sourceRouteProcessId = edge.getSourceRouteProcessId();
            Long targetRouteProcessId = edge.getTargetRouteProcessId();
            if (!routeProcessIds.contains(sourceRouteProcessId) || !routeProcessIds.contains(targetRouteProcessId)) {
                continue;
            }
            if (Objects.equals(sourceRouteProcessId, targetRouteProcessId)
                    || !seenEdges.add(sourceRouteProcessId + "->" + targetRouteProcessId)) {
                return null;
            }
            outgoingMap.get(sourceRouteProcessId).add(targetRouteProcessId);
            incomingMap.get(targetRouteProcessId).add(sourceRouteProcessId);
        }
        if (routeProcessIds.stream().anyMatch(id -> incomingMap.get(id).isEmpty() && outgoingMap.get(id).isEmpty())
                || hasRouteProcessCycle(routeProcessIds, outgoingMap)) {
            return null;
        }
        List<Long> rootRouteProcessIds = routeProcessIds.stream()
                .filter(id -> incomingMap.get(id).isEmpty())
                .toList();
        if (rootRouteProcessIds.isEmpty()) {
            return null;
        }
        boolean hasMultiPredecessor = incomingMap.values().stream().anyMatch(predecessors -> predecessors.size() > 1);
        Set<Long> reachableRouteProcessIds = new LinkedHashSet<>();
        rootRouteProcessIds.forEach(rootRouteProcessId ->
                reachableRouteProcessIds.addAll(reachableRouteProcessIds(rootRouteProcessId, outgoingMap)));
        if (hasMultiPredecessor || reachableRouteProcessIds.size() != routeProcessIds.size()) {
            return null;
        }
        Map<Long, Long> predecessorMap = new LinkedHashMap<>();
        incomingMap.forEach((routeProcessId, predecessorIds) -> {
            if (predecessorIds.size() == 1) {
                predecessorMap.put(routeProcessId, predecessorIds.iterator().next());
            }
        });
        return predecessorMap;
    }

    private boolean hasRouteProcessCycle(Set<Long> routeProcessIds, Map<Long, Set<Long>> outgoingMap) {
        Set<Long> visiting = new LinkedHashSet<>();
        Set<Long> visited = new LinkedHashSet<>();
        for (Long routeProcessId : routeProcessIds) {
            if (hasRouteProcessCycle(routeProcessId, outgoingMap, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRouteProcessCycle(Long routeProcessId, Map<Long, Set<Long>> outgoingMap,
                                         Set<Long> visiting, Set<Long> visited) {
        if (visited.contains(routeProcessId)) {
            return false;
        }
        if (!visiting.add(routeProcessId)) {
            return true;
        }
        for (Long targetRouteProcessId : outgoingMap.getOrDefault(routeProcessId, Set.of())) {
            if (hasRouteProcessCycle(targetRouteProcessId, outgoingMap, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(routeProcessId);
        visited.add(routeProcessId);
        return false;
    }

    private Set<Long> reachableRouteProcessIds(Long rootRouteProcessId, Map<Long, Set<Long>> outgoingMap) {
        Set<Long> visited = new LinkedHashSet<>();
        Queue<Long> pending = new ArrayDeque<>();
        visited.add(rootRouteProcessId);
        pending.add(rootRouteProcessId);
        while (!pending.isEmpty()) {
            Long current = pending.remove();
            for (Long target : outgoingMap.getOrDefault(current, Set.of())) {
                if (visited.add(target)) {
                    pending.add(target);
                }
            }
        }
        return visited;
    }

    private String routeTopologyValidationError(Long routeId) {
        return "排产工序缺少有效路线流转关系，routeId=" + routeId;
    }

    private List<MesProRouteProcessDO> mergeSnapshotRemainingRouteProcesses(
            Long routeId,
            List<MesProRouteProcessDO> routeProcesses,
            List<MesProScheduleOrderProcessDO> snapshotProcesses,
            Map<Long, Set<Long>> workstationProcessAliasesByRouteProcessId) {
        if (CollUtil.isEmpty(snapshotProcesses)) {
            return routeProcesses;
        }
        Map<Long, MesProRouteProcessDO> routeProcessByProcessId = routeProcesses.stream()
                .filter(process -> process.getProcessId() != null)
                .collect(Collectors.toMap(MesProRouteProcessDO::getProcessId, process -> process, (left, right) -> left,
                        LinkedHashMap::new));
        Set<Long> routeProcessIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> routeSorts = routeProcesses.stream()
                .map(MesProRouteProcessDO::getSort)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesProRouteProcessDO> merged = new ArrayList<>(routeProcesses);
        for (MesProScheduleOrderProcessDO snapshotProcess : snapshotProcesses) {
            if (replaceRouteProcessWithSnapshotIfChanged(routeId, merged, snapshotProcess,
                    routeProcessByProcessId, routeProcessIds, routeSorts, workstationProcessAliasesByRouteProcessId)) {
                continue;
            }
            if (!shouldMergeSnapshotProcess(snapshotProcess, routeProcessByProcessId, routeProcessIds, routeSorts)) {
                continue;
            }
            MesProRouteProcessDO snapshotRouteProcess = buildRouteProcessFromSnapshot(routeId, snapshotProcess, null);
            merged.add(snapshotRouteProcess);
            if (snapshotProcess.getProcessId() != null) {
                routeProcessByProcessId.put(snapshotProcess.getProcessId(), snapshotRouteProcess);
            }
            if (snapshotProcess.getRouteProcessId() != null) {
                routeProcessIds.add(snapshotProcess.getRouteProcessId());
            }
            if (snapshotProcess.getSort() != null) {
                routeSorts.add(snapshotProcess.getSort());
            }
        }
        return sortRouteProcesses(merged);
    }

    private List<MesProRouteProcessDO> alignRouteProcessesWithTopologySnapshot(
            Long routeId,
            List<MesProRouteProcessDO> routeProcesses,
            List<MesProScheduleOrderProcessDO> snapshotProcesses,
            Map<Long, Set<Long>> workstationProcessAliasesByRouteProcessId) {
        List<MesProScheduleOrderProcessDO> activeSnapshotProcesses =
                activeTopologyScheduleOrderProcesses(snapshotProcesses);
        if (!hasRouteProcessTopologySnapshot(activeSnapshotProcesses)
                || hasInactiveTopologyPredecessor(activeSnapshotProcesses)) {
            return routeProcesses;
        }
        Set<Long> routeProcessIds = routeProcesses.stream()
                .map(MesProRouteProcessDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean alreadyAligned = activeSnapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .allMatch(routeProcessIds::contains);
        if (alreadyAligned) {
            return routeProcesses;
        }

        Map<Long, MesProRouteProcessDO> routeProcessById = routeProcesses.stream()
                .filter(routeProcess -> routeProcess.getId() != null)
                .collect(Collectors.toMap(MesProRouteProcessDO::getId, routeProcess -> routeProcess,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, MesProRouteProcessDO> alignedBySnapshotRouteProcessId = new LinkedHashMap<>();
        for (MesProScheduleOrderProcessDO snapshotProcess : activeSnapshotProcesses) {
            Long snapshotRouteProcessId = snapshotProcess.getRouteProcessId();
            if (snapshotRouteProcessId == null) {
                continue;
            }
            MesProRouteProcessDO publishedRouteProcess = shouldResolvePublishedSnapshotRouteProcess(snapshotProcess)
                    ? routeProcessService.resolveFrozenRouteProcess(
                    snapshotRouteProcessId, routeId, positiveIdOrNull(snapshotProcess.getProcessId()))
                    : routeProcessService.resolveCurrentRouteProcess(
                    snapshotRouteProcessId, routeId, positiveIdOrNull(snapshotProcess.getProcessId()));
            MesProRouteProcessDO liveRouteProcess = routeProcessById.getOrDefault(
                    publishedRouteProcess.getId(), publishedRouteProcess);
            MesProRouteProcessDO alignedRouteProcess =
                    buildRouteProcessFromCurrentSnapshot(routeId, snapshotProcess, liveRouteProcess);
            alignedBySnapshotRouteProcessId.put(snapshotRouteProcessId, alignedRouteProcess);
            recordSnapshotRouteProcessLiveWorkstationProcess(
                    workstationProcessAliasesByRouteProcessId, alignedRouteProcess, liveRouteProcess);
        }
        if (alignedBySnapshotRouteProcessId.size() != activeSnapshotProcesses.size()) {
            return routeProcesses;
        }
        return sortRouteProcesses(new ArrayList<>(alignedBySnapshotRouteProcessId.values()));
    }

    private boolean shouldResolvePublishedSnapshotRouteProcess(MesProScheduleOrderProcessDO snapshotProcess) {
        return snapshotProcess != null
                && (snapshotProcess.getRouteScheduleConfigId() != null
                || snapshotProcess.getCapacitySource() != null
                || snapshotProcess.getCapacityMode() != null);
    }

    private boolean replaceRouteProcessWithSnapshotIfChanged(
            Long routeId,
            List<MesProRouteProcessDO> merged,
            MesProScheduleOrderProcessDO snapshotProcess,
            Map<Long, MesProRouteProcessDO> routeProcessByProcessId,
            Set<Long> routeProcessIds,
            Set<Integer> routeSorts,
            Map<Long, Set<Long>> workstationProcessAliasesByRouteProcessId) {
        if (snapshotProcess == null
                || !Boolean.TRUE.equals(snapshotProcess.getEnabled())
                || !hasRemainingQuantity(snapshotProcess)
                || snapshotProcess.getProcessId() == null) {
            return false;
        }
        for (int i = 0; i < merged.size(); i++) {
            MesProRouteProcessDO routeProcess = merged.get(i);
            boolean sameRouteProcessId = snapshotProcess.getRouteProcessId() != null
                    && ObjUtil.equal(routeProcess.getId(), snapshotProcess.getRouteProcessId());
            if (!sameRouteProcessId) {
                continue;
            }
            if (ObjUtil.equal(routeProcess.getProcessId(), snapshotProcess.getProcessId())) {
                return true;
            }
            MesProRouteProcessDO snapshotRouteProcess = buildRouteProcessFromSnapshot(routeId, snapshotProcess, routeProcess);
            merged.set(i, snapshotRouteProcess);
            recordSnapshotRouteProcessLiveWorkstationProcess(
                    workstationProcessAliasesByRouteProcessId, snapshotRouteProcess, routeProcess);
            if (routeProcess.getProcessId() != null) {
                routeProcessByProcessId.remove(routeProcess.getProcessId());
            }
            routeProcessByProcessId.put(snapshotProcess.getProcessId(), snapshotRouteProcess);
            if (snapshotProcess.getRouteProcessId() != null) {
                routeProcessIds.add(snapshotProcess.getRouteProcessId());
            }
            if (snapshotProcess.getSort() != null) {
                routeSorts.add(snapshotProcess.getSort());
            }
            return true;
        }
        return false;
    }

    private void recordSnapshotRouteProcessLiveWorkstationProcess(
            Map<Long, Set<Long>> workstationProcessAliasesByRouteProcessId,
            MesProRouteProcessDO snapshotRouteProcess,
            MesProRouteProcessDO liveRouteProcess) {
        if (snapshotRouteProcess == null
                || liveRouteProcess == null
                || snapshotRouteProcess.getId() == null
                || snapshotRouteProcess.getWorkstationId() == null
                || snapshotRouteProcess.getProcessId() == null
                || liveRouteProcess.getProcessId() == null
                || ObjUtil.equal(snapshotRouteProcess.getProcessId(), liveRouteProcess.getProcessId())) {
            return;
        }
        workstationProcessAliasesByRouteProcessId
                .computeIfAbsent(snapshotRouteProcess.getId(), key -> new LinkedHashSet<>())
                .add(snapshotRouteProcess.getProcessId());
        workstationProcessAliasesByRouteProcessId
                .get(snapshotRouteProcess.getId())
                .add(liveRouteProcess.getProcessId());
    }

    private MesProRouteProcessDO buildRouteProcessFromSnapshot(Long routeId,
                                                               MesProScheduleOrderProcessDO snapshotProcess,
                                                               MesProRouteProcessDO liveRouteProcess) {
        return MesProRouteProcessDO.builder()
                .id(snapshotProcess.getRouteProcessId())
                .routeId(routeId)
                .processId(snapshotProcess.getProcessId())
                .workstationId(liveRouteProcess == null ? null : liveRouteProcess.getWorkstationId())
                .sort(snapshotProcess.getSort())
                .prepareTime(ObjUtil.defaultIfNull(liveRouteProcess == null ? null : liveRouteProcess.getPrepareTime(), 0))
                .waitTime(ObjUtil.defaultIfNull(liveRouteProcess == null ? null : liveRouteProcess.getWaitTime(), 0))
                .colorCode(liveRouteProcess == null ? null : liveRouteProcess.getColorCode())
                .keyFlag(Boolean.TRUE.equals(snapshotProcess.getKeyProcessFlag())
                        || (liveRouteProcess != null && Boolean.TRUE.equals(liveRouteProcess.getKeyFlag())))
                .checkFlag(liveRouteProcess == null ? null : liveRouteProcess.getCheckFlag())
                .batchRecordReportId(liveRouteProcess == null ? null : liveRouteProcess.getBatchRecordReportId())
                .build();
    }

    private MesProRouteProcessDO buildRouteProcessFromCurrentSnapshot(Long routeId,
                                                                      MesProScheduleOrderProcessDO snapshotProcess,
                                                                      MesProRouteProcessDO liveRouteProcess) {
        Long resolvedProcessId = positiveIdOrNull(snapshotProcess.getProcessId());
        if (resolvedProcessId == null && liveRouteProcess != null) {
            resolvedProcessId = liveRouteProcess.getProcessId();
        }
        return MesProRouteProcessDO.builder()
                .id(snapshotProcess.getRouteProcessId())
                .routeId(routeId)
                .processId(resolvedProcessId)
                .workstationId(liveRouteProcess == null ? null : liveRouteProcess.getWorkstationId())
                .sort(ObjUtil.defaultIfNull(snapshotProcess.getSort(),
                        liveRouteProcess == null ? null : liveRouteProcess.getSort()))
                .prepareTime(ObjUtil.defaultIfNull(liveRouteProcess == null ? null : liveRouteProcess.getPrepareTime(), 0))
                .waitTime(ObjUtil.defaultIfNull(liveRouteProcess == null ? null : liveRouteProcess.getWaitTime(), 0))
                .colorCode(liveRouteProcess == null ? null : liveRouteProcess.getColorCode())
                .keyFlag(Boolean.TRUE.equals(snapshotProcess.getKeyProcessFlag())
                        || (liveRouteProcess != null && Boolean.TRUE.equals(liveRouteProcess.getKeyFlag())))
                .checkFlag(liveRouteProcess == null ? null : liveRouteProcess.getCheckFlag())
                .batchRecordReportId(liveRouteProcess == null ? null : liveRouteProcess.getBatchRecordReportId())
                .build();
    }

    private boolean shouldMergeSnapshotProcess(MesProScheduleOrderProcessDO snapshotProcess,
                                               Map<Long, MesProRouteProcessDO> routeProcessByProcessId,
                                               Set<Long> routeProcessIds,
                                               Set<Integer> routeSorts) {
        if (snapshotProcess == null
                || !Boolean.TRUE.equals(snapshotProcess.getEnabled())
                || !hasRemainingQuantity(snapshotProcess)
                || snapshotProcess.getProcessId() == null) {
            return false;
        }
        if (routeProcessByProcessId.containsKey(snapshotProcess.getProcessId())) {
            return false;
        }
        if (snapshotProcess.getRouteProcessId() != null && routeProcessIds.contains(snapshotProcess.getRouteProcessId())) {
            return false;
        }
        return snapshotProcess.getSort() == null || !routeSorts.contains(snapshotProcess.getSort());
    }

    private boolean hasRouteProcessTopologySnapshot(Collection<MesProScheduleOrderProcessDO> snapshotProcesses) {
        return CollUtil.isNotEmpty(snapshotProcesses)
                && snapshotProcesses.stream().anyMatch(item -> item != null
                && (item.getPredecessorRouteProcessId() != null || item.getRootProcessFlag() != null));
    }

    private boolean hasInactiveTopologyPredecessor(Collection<MesProScheduleOrderProcessDO> snapshotProcesses) {
        if (CollUtil.isEmpty(snapshotProcesses)) {
            return false;
        }
        Set<Long> activeRouteProcessIds = snapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getRouteProcessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return snapshotProcesses.stream()
                .map(MesProScheduleOrderProcessDO::getPredecessorRouteProcessId)
                .filter(Objects::nonNull)
                .anyMatch(predecessorRouteProcessId -> !activeRouteProcessIds.contains(predecessorRouteProcessId));
    }

    private List<MesProScheduleOrderProcessDO> activeTopologyScheduleOrderProcesses(
            Collection<MesProScheduleOrderProcessDO> processes) {
        if (CollUtil.isEmpty(processes)) {
            return Collections.emptyList();
        }
        return processes.stream()
                .filter(this::isActiveTopologyScheduleOrderProcess)
                .toList();
    }

    private boolean isActiveTopologyScheduleOrderProcess(MesProScheduleOrderProcessDO process) {
        return process != null
                && !Boolean.FALSE.equals(process.getEnabled())
                && process.getRouteProcessId() != null;
    }

    private boolean hasRemainingQuantity(MesProScheduleOrderProcessDO process) {
        return process != null
                && process.getRemainingQuantity() != null
                && process.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    private Long positiveIdOrNull(Long id) {
        return id == null || id <= 0 ? null : id;
    }

    private List<MesProRouteProcessDO> sortRouteProcesses(List<MesProRouteProcessDO> routeProcesses) {
        return routeProcesses.stream()
                .sorted(Comparator.comparing(MesProRouteProcessDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MesProRouteProcessDO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    public record ResolvedRoutePlan(
            List<MesProRouteProcessDO> routeProcesses,
            List<MesProScheduleOrderProcessDO> scheduleOrderProcesses,
            Map<Long, Set<Long>> workstationProcessAliasesByRouteProcessId,
            String topologyValidationError) {
    }

    private record RouteTopologyRecovery(
            List<MesProScheduleOrderProcessDO> scheduleOrderProcesses,
            String validationError) {
    }

}
