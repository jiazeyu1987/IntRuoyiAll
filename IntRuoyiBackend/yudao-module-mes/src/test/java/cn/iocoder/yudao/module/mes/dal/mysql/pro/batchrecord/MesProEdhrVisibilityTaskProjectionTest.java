package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionVisibilityService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MesProEdhrVisibilityTaskProjectionTest extends BaseDbUnitTest {
    @Resource private MesProEdhrBatchExecutionTaskMapper mapper;
    private final MesProEdhrBatchExecutionVisibilityService visibility =
            new MesProEdhrBatchExecutionVisibilityService();

    @Test void lightweightSqlPreservesCurrentProcessSelectionAndOmitsPayloads() {
        var controlled = insert(1, 1, 45, "MAIN", true);
        controlled.setRequiredPolicy("SKIPPABLE_CONTROLLED");
        mapper.updateById(controlled);
        insert(1, 2, 10, "MAIN", false);
        insert(1, 3, 10, "SECONDARY", true);
        var main = insert(1, 3, 0, "MAIN", true);
        insert(2, 1, 10, "MAIN", true);
        var deleted = insert(1, 4, 10, "MAIN", true);
        mapper.deleteById(deleted.getId());

        assertSameSelection(1, controlled.getId());
        controlled.setSkippedBy(71L).setSkippedAt(LocalDateTime.of(2026, 10, 27, 1, 0));
        mapper.updateById(controlled);
        assertSameSelection(1, main.getId());

        var rows = mapper.selectVisibilityTasksByBatchExecutionIds(List.of(1L));
        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(task -> task.getSpecialPayloadJson() == null
                && task.getFillableScopeJson() == null && task.getRequiredConditionJson() == null));
        assertEquals("R", visibility.resolveCurrentProcessTask(rows).getBatchRecordReportId());
        assertEquals(81L, visibility.resolveCurrentProcessTask(rows).getBatchRecordVersionId());
        assertTrue(mapper.selectVisibilityTasksByBatchExecutionIds(List.of()).isEmpty());
    }

    @Test void blankNodeAndAllApprovedSelectionKeepLegacyJavaSemantics() {
        var first = insert(3, 1, 40, "MAIN", true).setNodeType("\t");
        mapper.updateById(first);
        var last = insert(3, 2, 40, "MAIN", true).setNodeType("\u3000");
        mapper.updateById(last);
        assertSameSelection(3, last.getId());
        first.setStatus(0);
        mapper.updateById(first);
        assertSameSelection(3, first.getId());
    }

    private void assertSameSelection(long batchId, Long expected) {
        var full = visibility.resolveCurrentProcessTask(mapper.selectListByBatchExecutionId(batchId));
        var projected = visibility.resolveCurrentProcessTask(
                mapper.selectVisibilityTasksByBatchExecutionIds(List.of(batchId)));
        assertEquals(expected, full.getId());
        assertEquals(full.getId(), projected.getId());
        assertEquals(full.getBatchRecordDefinitionId(), projected.getBatchRecordDefinitionId());
        assertEquals(full.getBatchRecordVersionId(), projected.getBatchRecordVersionId());
    }

    private MesProEdhrBatchExecutionTaskDO insert(long batch, long process, int status,
                                                String slot, boolean required) {
        var task = new MesProEdhrBatchExecutionTaskDO().setBatchExecutionId(batch)
                .setRouteProcessId(process).setRouteProcessSort((int) process)
                .setBatchRecordSort("MAIN".equals(slot) ? 2 : 1).setNodeType("ROUTE_FORM")
                .setFormSlotType(slot).setStatus(status).setRequiredFlag(required)
                .setBatchRecordReportId("R").setBatchRecordDefinitionId(80L).setBatchRecordVersionId(81L)
                .setSpecialPayloadJson("large attachment and release payload")
                .setFillableScopeJson("large fillable scope").setRequiredConditionJson("large condition");
        mapper.insert(task);
        return task;
    }
}
