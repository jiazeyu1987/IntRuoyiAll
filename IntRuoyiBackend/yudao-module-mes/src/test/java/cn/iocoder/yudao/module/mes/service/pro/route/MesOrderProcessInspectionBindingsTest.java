package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MesOrderProcessInspectionBindingsTest {
    @Test
    void fifteenCopiesOfTheSameInspectionFormBecomeOneOrderRecord() {
        List<MesProRouteFlowProcessBatchRecordDO> bindings = new ArrayList<>();
        for (long i = 15; i >= 1; i--) bindings.add(binding(i, 100 + i, "PROCESS_INSPECTION", 10L));
        bindings.add(binding(20L, 101L, "MAIN", 20L));
        var result = MesOrderProcessInspectionBindings.collapse(bindings);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(binding -> Long.valueOf(1).equals(binding.getId())));
        assertTrue(result.stream().anyMatch(binding -> "MAIN".equals(binding.getFormSlotType())));
    }

    @Test
    void differentFormalTemplatesAreNotSilentlyCollapsed() {
        assertEquals(2, MesOrderProcessInspectionBindings.collapse(List.of(
                binding(1L, 101L, "PROCESS_INSPECTION", 10L),
                binding(2L, 102L, "PROCESS_INSPECTION", 11L))).size());
    }

    private MesProRouteFlowProcessBatchRecordDO binding(Long id, Long process, String slot, Long template) {
        return MesProRouteFlowProcessBatchRecordDO.builder().id(id).routeId(40L).routeProcessId(process)
                .formSlotType(slot).formTemplateId(template).lastPublishedTemplateVersionId(1000L)
                .recordCategory("PROCESS_INSPECTION").validationProfile("PROCESS_INSPECTION")
                .ownerRoleKey("PQC").build();
    }
}
