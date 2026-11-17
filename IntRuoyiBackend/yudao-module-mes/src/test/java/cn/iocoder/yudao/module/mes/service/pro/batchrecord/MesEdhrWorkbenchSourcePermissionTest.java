package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper;
import cn.iocoder.yudao.module.mes.workbench.MesEdhrWorkbenchRow;
import cn.iocoder.yudao.module.system.api.workbench.QueryContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesEdhrWorkbenchSourcePermissionTest {
    @Test void revokedPermissionKeepsTaskAndSqlStatusButBlocksItsAction() {
        var mapper = mock(MesWorkbenchTodoMapper.class);
        var permission = mock(MesDeviationTaskPermissionService.class);
        var source = new MesDeviationWorkbenchSource();
        ReflectionTestUtils.setField(source, "mapper", mapper);
        ReflectionTestUtils.setField(source, "taskPermissionService", permission);
        var query = new QueryContext();
        query.setTenantId(71L); query.setUserId(201L); query.setVisibility("visible"); query.freezeVisibility(1L);
        var row = new MesEdhrWorkbenchRow();
        row.setSourceId("EDHR_DEVIATION_TASK"); row.setBusinessId("13"); row.setTaskKey("EDHR_DEVIATION_TASK:13");
        row.setRowOrderKey("00000000000000000013"); row.setDomainTaskType("DEVIATION_SIGN");
        row.setSignatureCellKey("PREPARER"); row.setStatusLabel("待处理");
        when(mapper.chunkDeviation(query, null, 101)).thenReturn(List.of(row));
        when(permission.isDeviationTask("DEVIATION_SIGN")).thenReturn(true);
        when(permission.blockedReason(71L, 201L, "DEVIATION_SIGN", "PREPARER")).thenReturn("当前账号已无此偏差职责的办理权限");
        var rows = source.readChunk(query, null, 100).rows();
        assertEquals(1, rows.size());
        assertEquals("待处理", rows.get(0).getStatusLabel());
        assertEquals("当前账号已无此偏差职责的办理权限", rows.get(0).getAction().payload().get("blockedReason"));
        assertEquals("13", rows.get(0).getAction().payload().get("id"));
    }
}
