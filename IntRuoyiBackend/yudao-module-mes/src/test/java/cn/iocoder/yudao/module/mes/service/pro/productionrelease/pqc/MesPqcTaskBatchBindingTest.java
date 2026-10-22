package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesPqcTaskBatchBindingTest {
    @Test void completingUnboundPqcTaskPersistsFormalBatchAndRejectsAnotherBatch() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),"pqc-task-binding"),MesProEdhrWorkTaskDO.class);
        var mapper=mock(MesProEdhrWorkTaskMapper.class,CALLS_REAL_METHODS);
        doReturn(1).when(mapper).update(any(MesProEdhrWorkTaskDO.class),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        var time=LocalDateTime.of(2026,10,12,1,40);
        assertEquals(1,mapper.completePqcApprovedTask(2784L,900000001255L,time));
        var row=ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        var wrapper=ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(mapper).update(row.capture(),wrapper.capture());
        assertEquals(900000001255L,row.getValue().getBatchExecutionId());
        assertEquals("DONE",row.getValue().getStatus()); assertEquals("APPROVE",row.getValue().getReason());
        assertEquals(time,row.getValue().getCompletedAt());
        String sql=wrapper.getValue().getSqlSegment();
        assertTrue(sql.contains("batch_execution_id IS NULL")&&sql.contains("batch_execution_id ="));
        assertTrue(sql.contains("task_type =")&&sql.contains("status IN"));
        assertThrows(IllegalArgumentException.class,()->mapper.completePqcApprovedTask(2784L,null,time));
    }
}
