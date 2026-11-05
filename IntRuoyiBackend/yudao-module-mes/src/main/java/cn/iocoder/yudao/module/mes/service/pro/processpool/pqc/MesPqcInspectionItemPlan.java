package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

import cn.iocoder.yudao.module.mes.service.qc.sampling.MesQcSamplingPlan;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;

/** A QA stage is one task. Each inspection item retains its own frozen sample quantity. */
public final class MesPqcInspectionItemPlan {
    private MesPqcInspectionItemPlan() { }

    public static Map<String, MesQcSamplingPlan.Evidence> requireSampling(MesPqcInspectionTaskDO task) {
        if (!"PATROL".equals(task.getInspectionType())) return Map.of();
        if (task.getInspectionSamplingPlanJson() == null)
            throw new IllegalStateException("PQC task has no frozen matrix sampling evidence");
        Map<String, MesQcSamplingPlan.Evidence> evidence =
                JsonUtils.parseObject(task.getInspectionSamplingPlanJson(), new TypeReference<LinkedHashMap<String,
                        MesQcSamplingPlan.Evidence>>() { });
        Map<String, Integer> quantities = JsonUtils.parseObject(task.getInspectionItemPlanJson(),
                new TypeReference<LinkedHashMap<String, Integer>>() { });
        if (evidence == null || quantities == null || !evidence.keySet().equals(quantities.keySet()))
            throw new IllegalStateException("PQC matrix sampling evidence does not match item plan");
        for (var entry : evidence.entrySet()) {
            var rule = entry.getValue();
            if (rule == null) throw new IllegalStateException("PQC matrix sampling evidence is empty");
            var expected = MesQcSamplingPlan.calculate(
                    java.math.BigDecimal.valueOf(rule.totalQuantity()), rule.inspectionLevel());
            if (!expected.equals(rule) || !Integer.valueOf(rule.inspectionQuantity()).equals(quantities.get(entry.getKey())))
                throw new IllegalStateException("PQC matrix sampling evidence is inconsistent");
        }
        return evidence;
    }

    public static Map<String, Integer> require(MesPqcInspectionTaskDO task) {
        if (task == null || task.getInspectionItemPlanJson() == null) {
            throw new IllegalStateException("PQC task has no frozen QA item sampling plan");
        }
        Map<String, Integer> parsed = JsonUtils.parseObject(task.getInspectionItemPlanJson(),
                new TypeReference<LinkedHashMap<String, Integer>>() { });
        validate(parsed);
        requireSampling(task);
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

    /** Actual sampling may be stricter; the frozen QA plan remains the minimum for each item. */
    public static boolean matchesActualQuantities(MesPqcInspectionTaskDO task,
                                                  Map<String, Integer> actual, Integer taskQuantity) {
        Map<String, Integer> plan = require(task);
        return actual != null && plan.keySet().equals(actual.keySet())
                && plan.entrySet().stream().allMatch(entry -> actual.get(entry.getKey()) != null
                && actual.get(entry.getKey()) >= entry.getValue())
                && java.util.Objects.equals(taskQuantity, actual.values().stream()
                .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).max().orElse(0));
    }

    public static boolean completeSamples(MesPqcInspectionTaskDO task, String itemCode,
                                          java.util.List<Integer> sampleNumbers) {
        Integer minimum = require(task).get(itemCode);
        return minimum != null && sampleNumbers != null && sampleNumbers.size() >= minimum
                && sampleNumbers.stream().allMatch(java.util.Objects::nonNull)
                && sampleNumbers.stream().sorted().toList().equals(java.util.stream.IntStream
                .rangeClosed(1, sampleNumbers.size()).boxed().toList());
    }

    private static void validate(Map<String, Integer> plan) {
        if (plan == null || plan.isEmpty() || plan.entrySet().stream().anyMatch(entry ->
                entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() <= 0)) {
            throw new IllegalStateException("PQC item sampling plan must contain positive quantities for all items");
        }
    }
}
