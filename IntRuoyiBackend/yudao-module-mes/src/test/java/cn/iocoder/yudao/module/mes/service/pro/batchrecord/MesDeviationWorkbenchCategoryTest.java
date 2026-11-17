package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper;
import cn.iocoder.yudao.module.mes.workbench.*;
import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.workbench.index.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesDeviationWorkbenchCategoryTest {
    private QueryContext query() {
        var q = new QueryContext(); q.setTenantId(7L); q.setUserId(42L);
        q.setVisibility("visible"); q.freezeVisibility(1); return q;
    }
    private Configuration sql() throws Exception {
        var c = new Configuration();
        for (var p : List.of(Path.of("../yudao-module-system/src/main/resources/mapper/profileworkbench/WorkbenchQuerySql.xml"),
                Path.of("src/main/resources/mapper/profileworkbench/MesWorkbenchTodoMapper.xml"),
                Path.of("src/main/resources/mapper/profileworkbench/MesProjectionExportMapper.xml"))) {
            try (var in = Files.newInputStream(p)) { new XMLMapperBuilder(in,c,p.toString(),c.getSqlFragments()).parse(); }
        }
        return c;
    }
    @Test void countChunkAndIdentitySplitEveryDeviationDutyWithoutChangingOwnershipOrOpenSource() throws Exception {
        var c=sql(); var parameters=new HashMap<String,Object>(); parameters.put("q",query());
        parameters.put("businessId","13"); parameters.put("lockedBatchStatus",null);
        parameters.put("after",null); parameters.put("fetchLimit",101);
        var ns="cn.iocoder.yudao.module.mes.dal.mysql.pro.MesWorkbenchTodoMapper.";
        for(var suffix:List.of("countDeviation","chunkDeviation","identityDeviation")) {
            String s=c.getMappedStatement(ns+suffix).getBoundSql(parameters).getSql();
            assertTrue(s.contains("'EDHR_DEVIATION_TASK' AS source_id"));
            assertTrue(s.contains("'偏差' AS task_type"));
            assertTrue(s.contains("task_type IN ('DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY'"));
            assertTrue(s.contains("assignee_user_id = ?"));
            assertTrue(s.contains("dv.work_order_id = mes_pro_edhr_work_task.work_order_id"));
        }
        String ordinary=c.getMappedStatement(ns+"chunkEdhr").getBoundSql(parameters).getSql();
        assertTrue(ordinary.contains("task_type NOT IN ('DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY'"));
        assertFalse(ordinary.contains("'EDHR_DEVIATION_TASK' AS source_id"));
        assertFalse(ordinary.contains("${"));
    }
    @Test void persistedExportUsesSameSafeDutyDescriptionAndSeparateKeys() throws Exception {
        var c=sql(); var p=new HashMap<String,Object>(); p.put("q",query()); p.put("deviation",true);
        String s=c.getMappedStatement("cn.iocoder.yudao.module.mes.workbench.MesProjectionExportMapper.exportEdhr").getBoundSql(p).getSql();
        assertTrue(s.contains("'EDHR_DEVIATION_TASK:'"));
        assertTrue(s.contains("WHEN 'DEVIATION_VERIFY'"));
        assertTrue(s.contains("WHEN 'QUALITY_OWNER' THEN '完成质量负责人批准'"));
        assertTrue(s.contains("未关联批次（按生产订单办理）"));
        assertTrue(s.contains("task_type IN ('DEVIATION_RECEIVE','DEVIATION_CORRECT','DEVIATION_VERIFY'"));
    }
    @Test void deviationSourceKeepsIdentityLocksPermissionBlockAndItsOwnActionKind() {
        var source=new MesDeviationWorkbenchSource(); var mapper=mock(MesWorkbenchTodoMapper.class);
        var permissions=mock(MesDeviationTaskPermissionService.class); var q=query();
        ReflectionTestUtils.setField(source,"mapper",mapper);
        ReflectionTestUtils.setField(source,"taskPermissionService",permissions);
        var row=new MesEdhrWorkbenchRow(); row.setBusinessId("13"); row.setDomainTaskType("DEVIATION_SIGN"); row.setSignatureCellKey("PREPARER");
        when(mapper.lockEdhrIdentity(q,13)).thenReturn(null);
        when(mapper.identityDeviation(q,"13",null)).thenReturn(List.of(row));
        when(permissions.isDeviationTask("DEVIATION_SIGN")).thenReturn(true);
        when(permissions.blockedReason(7L,42L,"DEVIATION_SIGN","PREPARER")).thenReturn("当前账号已无此偏差职责的办理权限");
        var result=source.resolveVisibilityIdentity(q,"13");
        assertEquals("偏差",source.descriptor().taskType());
        assertEquals("mes.deviation.open",result.getAction().actionKind());
        assertTrue(result.getAction().payload().get("blockedReason").contains("无此偏差职责"));
        verify(mapper).lockEdhrIdentity(q,13);
        verify(mapper,never()).identityEdhr(any(),anyString(),any());
    }
    @Test void projectionExportsBothSourcesWithExactAudienceAndPolicies() {
        var mapper=mock(MesProjectionExportMapper.class); var authority=mock(WorkbenchAuthorityExport.class);
        var row=new MesEdhrWorkbenchRow(); row.setBusinessId("13"); row.setSourceId("EDHR_DEVIATION_TASK");
        row.setTaskKey("EDHR_DEVIATION_TASK:13"); row.setTaskType("偏差"); row.setSource("eDHR偏差任务");
        row.setDetail("请完成偏差整改"); row.setStatusLabel("待处理"); row.setRowOrderKey("00000000000000000013");
        row.setDomainTaskType("DEVIATION_CORRECT"); row.setOwnerUserId(42L);
        when(mapper.exportDeviation(any())).thenReturn(List.of(row));
        var exporter=new MesProjectionExport(mapper,authority,new MesEdhrWorkbenchSource(),new MesDeviationWorkbenchSource());
        var tasks=exporter.exportTasks(7,new ProjectionModel.Watermark(0,0));
        assertEquals(Set.of("EDHR_WORK_TASK","EDHR_DEVIATION_TASK"),exporter.sourceIds());
        assertEquals(1,tasks.size()); assertEquals("EDHR_DEVIATION_TASK:13",tasks.get(0).taskKey());
        assertEquals("mes.deviation.policy",tasks.get(0).audience().policyId());
        assertEquals(Set.of(42L),tasks.get(0).audience().members());
        assertEquals("mes.deviation.open",tasks.get(0).action().actionKind());
    }
}
