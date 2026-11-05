package cn.iocoder.yudao.module.mes.service.qc.sampling;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcInspectionItemPlan;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class MesQcSamplingFrozenPlanTest {
    @Test void rejectsChangedPlanOrMissingEvidence() {
        var task = new MesPqcInspectionTaskDO();
        task.setInspectionType("PATROL");
        task.setPlannedInspectionQuantity(50);
        task.setInspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("I", 50, "S3", 13)));
        assertThrows(IllegalStateException.class, () -> MesPqcInspectionItemPlan.require(task));
        task.setInspectionSamplingPlanJson(JsonUtils.toJsonString(Map.of(
                "I", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "I"),
                "S3", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "S3"))));
        assertEquals(Map.of("I", 50, "S3", 13), MesPqcInspectionItemPlan.require(task));
        task.setInspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("I", 12, "S3", 12)));
        assertThrows(IllegalStateException.class, () -> MesPqcInspectionItemPlan.require(task));
    }
}
