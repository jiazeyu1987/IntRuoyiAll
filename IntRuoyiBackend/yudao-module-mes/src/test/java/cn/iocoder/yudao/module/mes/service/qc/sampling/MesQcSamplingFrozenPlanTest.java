package cn.iocoder.yudao.module.mes.service.qc.sampling;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcInspectionItemPlan;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class MesQcSamplingFrozenPlanTest {
    @Test void stricterActualSamplingPreservesPerItemMinimaAndFrozenMatrix() {
        var task = new MesPqcInspectionTaskDO().setInspectionType("PATROL")
                .setPlannedInspectionQuantity(50)
                .setInspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("I", 50, "S3", 13)))
                .setInspectionSamplingPlanJson(JsonUtils.toJsonString(Map.of(
                        "I", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "I"),
                        "S3", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "S3"))));
        String frozen = task.getInspectionSamplingPlanJson();
        assertTrue(MesPqcInspectionItemPlan.matchesActualQuantities(task, Map.of("I", 60, "S3", 20), 60));
        assertTrue(MesPqcInspectionItemPlan.matchesActualQuantities(task, Map.of("I", 50, "S3", 13), 50));
        assertFalse(MesPqcInspectionItemPlan.matchesActualQuantities(task, Map.of("I", 49, "S3", 20), 49));
        assertFalse(MesPqcInspectionItemPlan.matchesActualQuantities(task, Map.of("I", 60, "S3", 12), 60));
        assertFalse(MesPqcInspectionItemPlan.matchesActualQuantities(task, Map.of("I", 60, "S3", 20), 50));
        assertFalse(MesPqcInspectionItemPlan.matchesActualQuantities(task, Map.of("I", 60), 60));
        assertTrue(MesPqcInspectionItemPlan.completeSamples(task, "S3",
                java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList()));
        var duplicate = new java.util.ArrayList<>(java.util.stream.IntStream.rangeClosed(1, 20).boxed().toList());
        duplicate.set(19, 19);
        assertFalse(MesPqcInspectionItemPlan.completeSamples(task, "S3", duplicate));
        duplicate.set(19, 21);
        assertFalse(MesPqcInspectionItemPlan.completeSamples(task, "S3", duplicate));
        assertFalse(MesPqcInspectionItemPlan.completeSamples(task, "I", java.util.List.of(1, 2)));
        assertEquals(Map.of("I", 50, "S3", 13), MesPqcInspectionItemPlan.require(task));
        assertEquals(frozen, task.getInspectionSamplingPlanJson());
    }
    @Test void rejectsChangedPlanOrMissingEvidence() {
        var task = new MesPqcInspectionTaskDO();
        task.setInspectionType("PATROL");
        task.setPlannedInspectionQuantity(50);
        task.setInspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("I", 50, "S3", 13)));
        var missing = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> MesPqcInspectionItemPlan.require(task));
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_SAMPLING_EVIDENCE_REQUIRED.getCode(), missing.getCode());
        assertTrue(missing.getMessage().contains("缺少冻结抽样证据"));
        task.setInspectionSamplingPlanJson(JsonUtils.toJsonString(Map.of(
                "I", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "I"),
                "S3", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "S3"))));
        assertEquals(Map.of("I", 50, "S3", 13), MesPqcInspectionItemPlan.require(task));
        task.setInspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("I", 12, "S3", 12)));
        assertThrows(IllegalStateException.class, () -> MesPqcInspectionItemPlan.require(task));
    }
}
