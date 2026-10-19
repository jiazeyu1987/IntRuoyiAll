package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.*;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesOrderQaStageReleaseCoverageTest {
    @Test
    void twentyOneStagesCoverSevenQaProcessesWithoutProductionSnapshots() {
        var service = service();
        List<String> missing = ReflectionTestUtils.invokeMethod(service, "missingExpectedPqcTaskIdentities", order(), tasks());
        assertEquals(List.of(), missing);
    }
    @Test
    void deletingAnEntireQaProcessCannotMakeRemainingTasksPass() {
        var tasks = tasks();
        tasks.removeIf(task -> Long.valueOf(7).equals(task.getQaProcessId()));
        List<String> missing = ReflectionTestUtils.invokeMethod(service(), "missingExpectedPqcTaskIdentities", order(), tasks);
        assertNotNull(missing);
        assertEquals(3, missing.size());
        assertTrue(missing.stream().allMatch(value -> value.contains("10|7|")));
    }
    private MesOrderReleaseCompletenessServiceImpl service() {
        var service = new MesOrderReleaseCompletenessServiceImpl();
        var versions = mock(MesQaInspectionRegulationVersionMapper.class);
        var processes = mock(MesQaInspectionRegulationProcessMapper.class);
        var items = mock(MesQaInspectionRegulationItemMapper.class);
        ReflectionTestUtils.setField(service, "regulationVersionMapper", versions);
        ReflectionTestUtils.setField(service, "qaProcessMapper", processes);
        ReflectionTestUtils.setField(service, "qaItemMapper", items);
        when(versions.selectById(10L)).thenReturn(MesQaInspectionRegulationVersionDO.builder().id(10L)
                .finalInspectionApplicable(false).finalInspectionNotApplicableReason("无末检")
                .inspectionTypeRulesJson("""
                    [{"key":"FIRST","inspectionType":"FIRST","required":true},
                     {"key":"PATROL_AM","inspectionType":"PATROL","required":true},
                     {"key":"PATROL_PM","inspectionType":"PATROL","required":true},
                     {"key":"FINAL","inspectionType":"FINAL","required":false}]
                    """).build());
        when(processes.selectListByVersionIds(List.of(10L))).thenReturn(java.util.stream.LongStream.rangeClosed(1, 7)
                .mapToObj(id -> MesQaInspectionRegulationProcessDO.builder().id(id).regulationVersionId(10L).build()).toList());
        List<MesQaInspectionRegulationItemDO> rows = new ArrayList<>();
        for (long id = 1; id <= 7; id++) for (String type : List.of("FIRST", "PATROL")) {
            rows.add(MesQaInspectionRegulationItemDO.builder().qaProcessId(id).regulationVersionId(10L)
                    .itemCode("A").inspectionType(type).build());
        }
        when(items.selectListByVersionId(10L)).thenReturn(rows);
        return service;
    }
    private MesProcessPoolActiveOrderDO order() {
        return MesProcessPoolActiveOrderDO.builder().id(1L).workOrderId(2L).qaRegulationVersionId(10L).build();
    }
    private ArrayList<MesPqcInspectionTaskDO> tasks() {
        var rows = new ArrayList<MesPqcInspectionTaskDO>();
        for (long id = 1; id <= 7; id++) for (String key : List.of("FIRST", "PATROL_AM", "PATROL_PM")) {
            rows.add(MesPqcInspectionTaskDO.builder().id((long) rows.size() + 1).activeOrderId(1L).workOrderId(2L)
                    .regulationVersionId(10L).qaProcessId(id).qaItemCode("").inspectionRuleKey(key)
                    .inspectionType(key.equals("FIRST") ? "FIRST" : "PATROL")
                    .shiftCode(key.equals("FIRST") ? "FIRST" : key.endsWith("AM") ? "AM" : "PM")
                    .roundNo(1).businessDate(LocalDate.of(2026,10,15)).plannedInspectionQuantity(2)
                    .inspectionItemPlanJson("{\"A\":2}").taskStatus("CONFIRMED").build());
        }
        return rows;
    }
}
