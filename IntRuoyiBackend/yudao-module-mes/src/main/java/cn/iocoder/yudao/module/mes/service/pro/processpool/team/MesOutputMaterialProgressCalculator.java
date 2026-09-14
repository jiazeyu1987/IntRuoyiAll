package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

final class MesOutputMaterialProgressCalculator {

    private MesOutputMaterialProgressCalculator() {
    }

    static BigDecimal calculateConservativeProcessProgress(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            List<MesProProcessPoolEventDO> productionEvents,
            Collection<MesProcessPoolReportAllocationDO> currentAllocations) {
        List<Long> outputMaterialIds = parseRequiredOutputMaterialIds(activeOrder, snapshot);
        Map<Long, BigDecimal> quantitiesByMaterial = new LinkedHashMap<>();
        outputMaterialIds.forEach(materialId -> quantitiesByMaterial.put(materialId, zero(snapshot)));

        Map<Long, BigDecimal> currentAllocationQuantityByEventId =
                currentAllocationQuantityByEventId(activeOrder, snapshot, currentAllocations);
        if (currentAllocationQuantityByEventId.isEmpty()) {
            return zero(snapshot);
        }
        Set<Long> matchedEventIds = new LinkedHashSet<>();
        for (MesProProcessPoolEventDO event : productionEvents == null
                ? List.<MesProProcessPoolEventDO>of() : productionEvents) {
            BigDecimal allocatedQuantity = event == null || event.getId() == null
                    ? null : currentAllocationQuantityByEventId.get(event.getId());
            if (allocatedQuantity == null) {
                continue;
            }
            validateProductionEventIdentity(activeOrder, snapshot, event);
            matchedEventIds.add(event.getId());
            for (JSONObject detail : parseProductionMaterialDetails(activeOrder, event)) {
                Long materialId = parseMaterialId(activeOrder, detail.get("materialId"), "PRODUCTION_MATERIAL_ID");
                if (!quantitiesByMaterial.containsKey(materialId)) {
                    throw sourceMissing(activeOrder, "PRODUCTION_OUTPUT_MATERIAL_SCOPE");
                }
                BigDecimal outputQuantity = detail.getBigDecimal("outputQuantity");
                if (outputQuantity == null || outputQuantity.signum() < 0) {
                    throw sourceMissing(activeOrder, "PRODUCTION_OUTPUT_MATERIAL_QUANTITY");
                }
                quantitiesByMaterial.merge(materialId, outputQuantity.min(allocatedQuantity), BigDecimal::add);
            }
        }
        if (!matchedEventIds.containsAll(currentAllocationQuantityByEventId.keySet())) {
            throw sourceMissing(activeOrder, "PRODUCTION_EVENT_FOR_CURRENT_ALLOCATION");
        }
        return quantitiesByMaterial.values().stream()
                .min(BigDecimal::compareTo)
                .orElse(zero(snapshot))
                .setScale(scale(snapshot), RoundingMode.HALF_UP);
    }

    private static Map<Long, BigDecimal> currentAllocationQuantityByEventId(
            MesProcessPoolActiveOrderDO activeOrder,
            MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
            Collection<MesProcessPoolReportAllocationDO> currentAllocations) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (MesProcessPoolReportAllocationDO allocation : currentAllocations == null
                ? List.<MesProcessPoolReportAllocationDO>of() : currentAllocations) {
            if (allocation == null) {
                throw sourceMissing(activeOrder, "REPORT_ALLOCATION");
            }
            if (!Objects.equals(id(activeOrder), allocation.getActiveOrderId())
                    || !Objects.equals(workOrderId(activeOrder), allocation.getWorkOrderId())
                    || !Objects.equals(snapshot.getRouteProcessId(), allocation.getRouteProcessId())
                    || !Objects.equals(snapshot.getProcessId(), allocation.getProcessId())) {
                continue;
            }
            if (allocation.getAllocatedQuantity() == null || allocation.getAllocatedQuantity().signum() < 0) {
                throw sourceMissing(activeOrder, "REPORT_ALLOCATION_QUANTITY");
            }
            if (allocation.getAllocatedQuantity().signum() == 0) {
                continue;
            }
            if (allocation.getEventId() == null || allocation.getEventId() <= 0) {
                throw sourceMissing(activeOrder, "REPORT_ALLOCATION_EVENT_ID");
            }
            result.merge(allocation.getEventId(), allocation.getAllocatedQuantity(), BigDecimal::add);
        }
        return result;
    }

    private static List<Long> parseRequiredOutputMaterialIds(MesProcessPoolActiveOrderDO activeOrder,
                                                             MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        String configJson = snapshot == null ? null : snapshot.getProductionConfigSnapshotJson();
        if (configJson == null || configJson.isBlank()) {
            throw sourceMissing(activeOrder, "PRODUCTION_OUTPUT_MATERIAL_IDS_REQUIRED");
        }
        JSONObject config;
        try {
            config = JSON.parseObject(configJson);
        } catch (RuntimeException ex) {
            throw sourceMissing(activeOrder, "PRODUCTION_CONFIG_SNAPSHOT_JSON");
        }
        JSONArray rawIds = config == null ? null : config.getJSONArray("outputMaterialIds");
        if (rawIds == null || rawIds.isEmpty()) {
            throw sourceMissing(activeOrder, "PRODUCTION_OUTPUT_MATERIAL_IDS_REQUIRED");
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

    private static void validateProductionEventIdentity(MesProcessPoolActiveOrderDO activeOrder,
                                                       MesProcessPoolActiveOrderProcessSnapshotDO snapshot,
                                                       MesProProcessPoolEventDO event) {
        if (!Objects.equals(workOrderId(activeOrder), event.getWorkOrderId())
                || !Objects.equals(routeId(activeOrder), event.getRouteId())
                || !Objects.equals(snapshot.getRouteProcessId(), event.getRouteProcessId())
                || !Objects.equals(snapshot.getProcessId(), event.getProcessId())
                || !MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT.equals(event.getEventType())) {
            throw sourceMissing(activeOrder, "PRODUCTION_EVENT_IDENTITY");
        }
    }

    private static List<JSONObject> parseProductionMaterialDetails(MesProcessPoolActiveOrderDO activeOrder,
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

    private static JSONObject toJsonObject(MesProcessPoolActiveOrderDO activeOrder, Object value) {
        if (value instanceof JSONObject object) {
            return object;
        }
        try {
            return value == null ? null : JSON.parseObject(JSON.toJSONString(value));
        } catch (RuntimeException ex) {
            throw sourceMissing(activeOrder, "PRODUCTION_MATERIAL_DETAIL_JSON");
        }
    }

    private static Long parseMaterialId(MesProcessPoolActiveOrderDO activeOrder, Object value, String field) {
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

    private static Long id(MesProcessPoolActiveOrderDO activeOrder) {
        return activeOrder == null ? null : activeOrder.getId();
    }

    private static Long workOrderId(MesProcessPoolActiveOrderDO activeOrder) {
        return activeOrder == null ? null : activeOrder.getWorkOrderId();
    }

    private static Long routeId(MesProcessPoolActiveOrderDO activeOrder) {
        return activeOrder == null ? null : activeOrder.getRouteId();
    }

    private static BigDecimal zero(MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        return BigDecimal.ZERO.setScale(scale(snapshot), RoundingMode.HALF_UP);
    }

    private static int scale(MesProcessPoolActiveOrderProcessSnapshotDO snapshot) {
        BigDecimal plannedQuantity = snapshot == null ? null : snapshot.getPlannedQuantitySnapshot();
        return plannedQuantity == null ? 6 : Math.max(6, plannedQuantity.scale());
    }

    private static RuntimeException sourceMissing(MesProcessPoolActiveOrderDO activeOrder, String field) {
        return exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING,
                activeOrder == null ? null : activeOrder.getId(), field);
    }
}
