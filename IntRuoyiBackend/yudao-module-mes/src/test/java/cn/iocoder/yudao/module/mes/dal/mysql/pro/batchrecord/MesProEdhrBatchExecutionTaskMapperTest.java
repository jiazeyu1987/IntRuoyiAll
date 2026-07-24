package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MesProEdhrBatchExecutionTaskMapperTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchExecutionTaskMapper;

    @Test
    void selectByExecutionId_shouldReturnRouteFormRepresentativeWhenMultipleTasksShareExecution() {
        Long batchExecutionId = 2026072101L;
        Long executionId = 2026072102L;
        batchExecutionTaskMapper.insert(task(batchExecutionId, executionId, "STERILIZATION_REPORT",
                1001L, 1, 1, null));
        MesProEdhrBatchExecutionTaskDO expected = task(batchExecutionId, executionId, "ROUTE_FORM",
                1002L, 2, 1, "BR-001");
        batchExecutionTaskMapper.insert(expected);
        batchExecutionTaskMapper.insert(task(batchExecutionId, executionId, "ROUTE_FORM",
                1002L, 2, 2, "BR-002"));

        MesProEdhrBatchExecutionTaskDO selected = batchExecutionTaskMapper.selectByExecutionId(executionId);

        assertNotNull(selected);
        assertEquals(expected.getId(), selected.getId());
        assertEquals("ROUTE_FORM", selected.getNodeType());
    }

    private MesProEdhrBatchExecutionTaskDO task(Long batchExecutionId, Long executionId, String nodeType,
                                               Long routeProcessId, Integer routeProcessSort,
                                               Integer batchRecordSort, String batchRecordReportId) {
        return randomPojo(MesProEdhrBatchExecutionTaskDO.class, task -> {
            task.setBatchExecutionId(batchExecutionId);
            task.setExecutionId(executionId);
            task.setNodeType(nodeType);
            task.setRouteProcessId(routeProcessId);
            task.setRouteProcessSort(routeProcessSort);
            task.setBatchRecordSort(batchRecordSort);
            task.setBatchRecordReportId(batchRecordReportId);
            task.setStatus(1);
            task.setRequiredFlag(true);
        });
    }
}
