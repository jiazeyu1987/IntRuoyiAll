package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;

/** A QA stage is one task. Each inspection item retains its own frozen sample quantity. */
public final class MesPqcInspectionItemPlan {
    private MesPqcInspectionItemPlan() { }

    public static Map<String, Integer> require(MesPqcInspectionTaskDO task) {
        if (task == null || task.getInspectionItemPlanJson() == null) {
            throw new IllegalStateException("PQC task has no frozen QA item sampling plan");
        }
        Map<String, Integer> parsed = JsonUtils.parseObject(task.getInspectionItemPlanJson(),
                new TypeReference<LinkedHashMap<String, Integer>>() { });
        validate(parsed);
        if (!Integer.valueOf(parsed.values().stream().mapToInt(Integer::intValue).max().orElseThrow())
                .equals(task.getPlannedInspectionQuantity())) {
            throw new IllegalStateException("PQC task sampling quantity does not match its frozen item plan");
        }
        return Map.copyOf(parsed);
    }

    public static String encode(Map<String, Integer> plan) {
        validate(plan);
        return JsonUtils.toJsonString(plan);
    }

    private static void validate(Map<String, Integer> plan) {
        if (plan == null || plan.isEmpty() || plan.entrySet().stream().anyMatch(entry ->
                entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() <= 0)) {
            throw new IllegalStateException("PQC item sampling plan must contain positive quantities for all items");
        }
    }
}
