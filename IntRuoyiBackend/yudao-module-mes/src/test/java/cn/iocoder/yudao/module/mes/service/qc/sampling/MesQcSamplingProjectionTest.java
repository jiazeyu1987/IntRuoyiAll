package cn.iocoder.yudao.module.mes.service.qc.sampling;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcInspectionItemPlan;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MesQcSamplingProjectionTest {
    @Test
    void frontlineReceivesEachItemsFrozenQuantityAndSamplingEvidenceForDynamicRounds() {
        for (int round : List.of(1, 2, 7)) {
            String key = "PATROL";
            var evidence = Map.of("VISUAL", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "I"),
                    "STRENGTH", MesQcSamplingPlan.calculate(new BigDecimal("3000"), "S3"));
            var task = MesPqcInspectionTaskDO.builder().id(1L).inspectionType("PATROL")
                    .qaItemCode("").inspectionRuleKey(key).businessDate(LocalDate.of(2026, 11, 5))
                    .roundNo(round).plannedInspectionQuantity(50)
                    .inspectionItemPlanJson(MesPqcInspectionItemPlan.encode(Map.of("VISUAL", 50, "STRENGTH", 13)))
                    .inspectionSamplingPlanJson(JsonUtils.toJsonString(evidence)).build();
            var visual = item("VISUAL", "I");
            var strength = item("STRENGTH", "S3");
            var rule = new MesFrontlinePqcProcessRespVO.QaInspectionTypeRule();
            rule.setKey(key);
            MesFrontlinePqcProcessRespVO.PqcTaskOption result = ReflectionTestUtils.invokeMethod(
                    MesFrontlinePqcContextServiceImpl.class, "toPqcTaskOptionRespVO",
                    task, false, List.of(visual, strength), rule, Map.of());
            assertNotNull(result);
            assertEquals(50, result.getPlannedInspectionQuantity());
            assertEquals(key, result.getInspectionRuleKey());
            assertEquals(2, result.getInspectionItems().size());
            for (var projected : result.getInspectionItems()) {
                var expected = evidence.get(projected.getItemCode());
                assertEquals(expected.inspectionQuantity(), projected.getPlannedInspectionQuantity());
                assertEquals(expected, projected.getSamplingEvidence());
                assertEquals(expected.inspectionLevel(), projected.getInspectionLevel());
                assertEquals(new BigDecimal("0.4"), projected.getAql());
            }
            assertNull(visual.getSamplingEvidence());
            assertNull(strength.getSamplingEvidence());
        }
    }

    private static MesFrontlinePqcProcessRespVO.PqcInspectionItem item(String code, String level) {
        var item = new MesFrontlinePqcProcessRespVO.PqcInspectionItem();
        item.setItemCode(code);
        item.setApplicableInspectionTypes(List.of("PATROL"));
        item.setInspectionLevel(level);
        item.setAql(new BigDecimal("0.4"));
        return item;
    }
}
