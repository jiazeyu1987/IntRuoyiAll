package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/** Reads the dual completion gate from the locked production/PQC source tables. */
@Service
public class MesTeamLeaderActiveOrderCompletionProgressPortImpl
        implements MesTeamLeaderActiveOrderCompletionProgressPort {

    private final MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper;
    private final MesProcessPoolReportAllocationMapper allocationMapper;
    private final MesPqcInspectionTaskMapper pqcTaskMapper;
    private final MesProProcessPoolEventMapper eventMapper;

    public MesTeamLeaderActiveOrderCompletionProgressPortImpl(
            MesProcessPoolActiveOrderProcessSnapshotMapper snapshotMapper,
            MesProcessPoolReportAllocationMapper allocationMapper,
            MesPqcInspectionTaskMapper pqcTaskMapper,
            MesProProcessPoolEventMapper eventMapper) {
        this.snapshotMapper = snapshotMapper;
        this.allocationMapper = allocationMapper;
        this.pqcTaskMapper = pqcTaskMapper;
        this.eventMapper = eventMapper;
    }

    @Override
    public MesTeamLeaderActiveOrderCompletionProgress read(Long leaderUserId,
                                                            MesProcessPoolActiveOrderDO activeOrder) {
        if (activeOrder == null || !Objects.equals(activeOrder.getLeaderUserId(), leaderUserId)) {
            throw sourceMissing(activeOrder, "ACTIVE_ORDER_OWNER");
        }
        List<MesProcessPoolActiveOrderProcessSnapshotDO> snapshots =
                snapshotMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesProcessPoolReportAllocationDO> allocations =
                allocationMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesPqcInspectionTaskDO> tasks = pqcTaskMapper.selectListByActiveOrderIdForUpdate(activeOrder.getId());
        List<MesProProcessPoolEventDO> productionEvents =
                eventMapper.selectProductionSubmitsByWorkOrderAndRouteForUpdate(activeOrder.getWorkOrderId(),
                        activeOrder.getRouteId());
        if (snapshots == null || snapshots.isEmpty() || tasks == null || tasks.isEmpty()) {
            throw sourceMissing(activeOrder, "PRODUCTION_OR_PQC_SNAPSHOT");
        }

        Map<String, BigDecimal> allocatedByProcess = new HashMap<>();
        for (MesProcessPoolReportAllocationDO allocation : allocations == null ? List.<MesProcessPoolReportAllocationDO>of() : allocations) {
            if (allocation == null || !Objects.equals(activeOrder.getId(), allocation.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), allocation.getWorkOrderId())
                    || allocation.getRouteProcessId() == null || allocation.getProcessId() == null
                    || allocation.getAllocatedQuantity() == null
                    || allocation.getAllocatedQuantity().signum() < 0) {
                throw sourceMissing(activeOrder, "REPORT_ALLOCATION");
            }
            allocatedByProcess.merge(key(allocation.getRouteProcessId(), allocation.getProcessId()),
                    allocation.getAllocatedQuantity(), BigDecimal::add);
        }
        Map<String, MesProcessPoolActiveOrderProcessSnapshotDO> snapshotsByProcess = new HashMap<>();
        long productionComplete = 0;
        for (MesProcessPoolActiveOrderProcessSnapshotDO snapshot : snapshots) {
            if (snapshot == null || snapshot.getRouteProcessId() == null || snapshot.getProcessId() == null
                    || snapshot.getPlannedQuantitySnapshot() == null
                    || snapshot.getPlannedQuantitySnapshot().signum() <= 0) {
                throw sourceMissing(activeOrder, "PROCESS_TARGET");
            }
            if (snapshotsByProcess.put(key(snapshot.getRouteProcessId(), snapshot.getProcessId()), snapshot) != null) {
                throw sourceMissing(activeOrder, "PROCESS_SNAPSHOT_DUPLICATE");
            }
            BigDecimal allocated = allocatedByProcess.getOrDefault(
                    key(snapshot.getRouteProcessId(), snapshot.getProcessId()), BigDecimal.ZERO);
            BigDecimal conservativeProgress =
                    calculateConservativeProcessProgress(activeOrder, snapshot, productionEvents, allocated);
            if (conservativeProgress.compareTo(snapshot.getPlannedQuantitySnapshot()) >= 0) {
                productionComplete++;
            }
        }

        long inspectionComplete = tasks.stream().filter(Objects::nonNull).peek(task -> {
            if (!Objects.equals(activeOrder.getId(), task.getActiveOrderId())
                    || !Objects.equals(activeOrder.getWorkOrderId(), task.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), task.getRouteId())
                    || !Objects.equals(activeOrder.getRouteVersionId(), task.getRouteVersionId())
                    || task.getRouteProcessId() == null || task.getProcessId() == null || task.getId() == null
                    || !snapshotsByProcess.containsKey(key(task.getRouteProcessId(), task.getProcessId()))) {
                throw sourceMissing(activeOrder, "PQC_TASK");
            }
        }).filter(task -> MesPqcInspectionTaskDO.TASK_STATUS_CONFIRMED.equals(task.getTaskStatus())).count();
        return new MesTeamLeaderActiveOrderCompletionProgress()
                .setProductionProgressPercent(percent(productionComplete, snapshots.size()))
                .setInspectionProgressPercent(percent(inspectionComplete, tasks.size()));
    }

    private static BigDecimal percent(long completed, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateConservativeProcessProgress(MesProcessPoolActiveOrderDO activeOrder,
                                                            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                                            List<MesProProcessPoolEventDO> productionEvents,
                                                            BigDecimal allocationProgress) {
        List<Long> outputMaterialIds = parseOutputMaterialIds(activeOrder, snapshot);
        if (outputMaterialIds.isEmpty()) {
            return allocationProgress == null ? BigDecimal.ZERO : allocationProgress;
        }
        Map<Long, BigDecimal> quantitiesByMaterial = new LinkedHashMap<>();
        outputMaterialIds.forEach(materialId -> quantitiesByMaterial.put(materialId, BigDecimal.ZERO));
        for (MesProProcessPoolEventDO event : productionEvents == null ? List.<MesProProcessPoolEventDO>of() : productionEvents) {
            if (event == null) {
                throw sourceMissing(activeOrder, "PRODUCTION_EVENT");
            }
            if (!Objects.equals(snapshot.getRouteProcessId(), event.getRouteProcessId())
                    || !Objects.equals(snapshot.getProcessId(), event.getProcessId())) {
                continue;
            }
            if (!Objects.equals(activeOrder.getWorkOrderId(), event.getWorkOrderId())
                    || !Objects.equals(activeOrder.getRouteId(), event.getRouteId())
                    || !MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
                throw sourceMissing(activeOrder, "PRODUCTION_EVENT_IDENTITY");
            }
            for (JSONObject detail : parseProductionMaterialDetails(activeOrder, event)) {
                Long materialId = parseMaterialId(activeOrder, detail.get("materialId"), "PRODUCTION_MATERIAL_ID");
                if (!quantitiesByMaterial.containsKey(materialId)) {
                    throw sourceMissing(activeOrder, "PRODUCTION_OUTPUT_MATERIAL_SCOPE");
                }
                BigDecimal outputQuantity = detail.getBigDecimal("outputQuantity");
                if (outputQuantity == null || outputQuantity.signum() < 0) {
                    throw sourceMissing(activeOrder, "PRODUCTION_OUTPUT_MATERIAL_QUANTITY");
                }
                quantitiesByMaterial.merge(materialId, outputQuantity, BigDecimal::add);
            }
        }
        return quantitiesByMaterial.values().stream()
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private List<Long> parseOutputMaterialIds(MesProcessPoolActiveOrderDO activeOrder,
                                              MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        String configJson = snapshot.getProductionConfigSnapshotJson();
        if (configJson == null || configJson.isBlank()) {
            return List.of();
        }
        JSONObject config;
        try {
            config = JSON.parseObject(configJson);
        } catch (RuntimeException ex) {
            throw sourceMissing(activeOrder, "PRODUCTION_CONFIG_SNAPSHOT_JSON");
        }
        JSONArray rawIds = config == null ? null : config.getJSONArray("outputMaterialIds");
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Object rawId : rawIds) {
            Long materialId = parseMaterialId(activeOrder, rawId, "PRODUCTION_OUTPUT_MATERIAL_IDS");
            if (!normalized.add(materialId)) {
                throw sourceMissing(activeOrder, "PRODUCTION_OUTPUT_MATERIAL_DUPLICATE");
            }
        }
        return List.copyOf(normalized);
    }

    private List<JSONObject> parseProductionMaterialDetails(MesProcessPoolActiveOrderDO activeOrder,
                                                            MesProProcessPoolEventDO event) {
        String rawPayload = event.getRawPayload();
        if (rawPayload == null || rawPayload.isBlank()) {
            throw sourceMissing(activeOrder, "PRODUCTION_RAW_PAYLOAD");
        }
        JSONObject payload;
        try {
            payload = JSON.parseObject(rawPayload);
        } catch (RuntimeException ex) {
            throw sourceMissing(activeOrder, "PRODUCTION_RAW_PAYLOAD_JSON");
        }
        JSONArray rawDetails = payload == null ? null : payload.getJSONArray("materialDetails");
        if (rawDetails == null || rawDetails.isEmpty()) {
            throw sourceMissing(activeOrder, "PRODUCTION_MATERIAL_DETAILS");
        }
        Set<Long> eventMaterialIds = new LinkedHashSet<>();
        java.util.ArrayList<JSONObject> details = new java.util.ArrayList<>();
        for (Object rawDetail : rawDetails) {
            JSONObject detail = toJsonObject(activeOrder, rawDetail);
            Long materialId = parseMaterialId(activeOrder, detail.get("materialId"), "PRODUCTION_MATERIAL_ID");
            if (!eventMaterialIds.add(materialId)) {
                throw sourceMissing(activeOrder, "PRODUCTION_MATERIAL_DUPLICATE");
            }
            details.add(detail);
        }
        return List.copyOf(details);
    }

    private JSONObject toJsonObject(MesProcessPoolActiveOrderDO activeOrder, Object value) {
        if (value instanceof JSONObject object) {
            return object;
        }
        try {
            return value == null ? null : JSON.parseObject(JSON.toJSONString(value));
        } catch (RuntimeException ex) {
            throw sourceMissing(activeOrder, "PRODUCTION_MATERIAL_DETAIL_JSON");
        }
    }

    private Long parseMaterialId(MesProcessPoolActiveOrderDO activeOrder, Object value, String field) {
        Long materialId = null;
        if (value instanceof Number number) {
            materialId = number.longValue();
        } else if (value instanceof String text && !text.isBlank()) {
            try {
                materialId = Long.valueOf(text.trim());
            } catch (NumberFormatException ex) {
                throw sourceMissing(activeOrder, field);
            }
        }
        if (materialId == null || materialId <= 0) {
            throw sourceMissing(activeOrder, field);
        }
        return materialId;
    }

    private static String key(Long routeProcessId, Long processId) {
        return routeProcessId + ":" + processId;
    }

    private static RuntimeException sourceMissing(MesProcessPoolActiveOrderDO activeOrder, String field) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                activeOrder == null ? null : activeOrder.getId(), field);
    }
}
