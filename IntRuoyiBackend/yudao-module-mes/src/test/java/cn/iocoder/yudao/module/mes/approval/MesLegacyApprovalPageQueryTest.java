package cn.iocoder.yudao.module.mes.approval;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesLegacyApprovalPageMapper;
import org.flowable.engine.*;
import org.flowable.task.api.history.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class MesLegacyApprovalPageQueryTest {
    @Test void doneStaticFiltersPrecedeExactCountAndSecondPageWithoutScanning() {
        TenantContextHolder.setTenantId(7L);
        try {
            var query=new MesLegacyApprovalPageQuery();var mapper=mock(MesLegacyApprovalPageMapper.class);
            var configuration=mock(ProcessEngineConfiguration.class);when(configuration.getDatabaseType()).thenReturn("mysql");
            var history=mock(HistoryService.class);var engineQuery=mock(HistoricTaskInstanceQuery.class,RETURNS_SELF);
            when(history.createHistoricTaskInstanceQuery()).thenReturn(engineQuery);
            var task=mock(HistoricTaskInstance.class);when(task.getId()).thenReturn("second");when(engineQuery.list()).thenReturn(List.of(task));
            ReflectionTestUtils.setField(query,"mapper",mapper);ReflectionTestUtils.setField(query,"configuration",configuration);ReflectionTestUtils.setField(query,"history",history);
            var counted=new ArrayList<String>();
            when(mapper.countDone(any())).thenAnswer(call->{var wrapper=(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>)call.getArgument(0);String sql=wrapper.getSqlSegment();
                if(sql.contains("s.id IS NULL"))return 0L;
                counted.add(sql);assertFalse(sql.contains("LIMIT"));assertTrue(sql.contains("x.submitted_at >="));assertTrue(sql.contains("x.submitted_at <="));
                assertTrue(sql.contains("x.submitted_by ="));assertTrue(sql.contains("LOCATE(LOWER("));assertTrue(wrapper.getParamNameValuePairs().containsValue("7"));
                assertTrue(wrapper.getParamNameValuePairs().containsValue("88"));assertTrue(wrapper.getParamNameValuePairs().containsValue("批_%"));return 2L;});
            when(mapper.done(any())).thenAnswer(call->{var wrapper=(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>)call.getArgument(0);
                assertTrue(wrapper.getSqlSegment().endsWith("LIMIT 1,1"));assertTrue(wrapper.getSqlSegment().contains(counted.get(0)));return List.of("second");});
            var req=new MesProBatchRecordExecutionApprovalPageReqVO();req.setPageNo(2);req.setPageSize(1);req.setExecutionCode("批_%");req.setWorkOrderCode("工单");req.setBatchCode("BATCH");req.setSubmittedBy(77L);
            req.setSubmittedAtStart(LocalDateTime.of(2026,1,1,0,0));req.setSubmittedAtEnd(LocalDateTime.of(2026,1,2,0,0));
            var page=query.done(88L,req);assertEquals(2L,page.getTotal());assertEquals(List.of(task),page.getList());assertEquals(1,counted.size());
            verify(mapper,times(1)).done(any());verify(engineQuery).taskTenantId("7");verify(engineQuery).taskIds(Set.of("second"));
        } finally {TenantContextHolder.clear();}
    }
    @Test void crossTenantSnapshotsAndExecutionReferencesNeverSatisfyPersistedSourceJoin() throws Exception {
        try (var db=java.sql.DriverManager.getConnection("jdbc:h2:mem:legacy-approval-tenant;MODE=MySQL;DB_CLOSE_DELAY=-1");
             var statement=db.createStatement()) {
            statement.execute("CREATE TABLE ACT_HI_TASKINST (ID_ VARCHAR PRIMARY KEY,PROC_DEF_ID_ VARCHAR,PROC_INST_ID_ VARCHAR,TENANT_ID_ VARCHAR)");
            statement.execute("CREATE TABLE ACT_RE_PROCDEF (ID_ VARCHAR PRIMARY KEY,TENANT_ID_ VARCHAR)");
            statement.execute("CREATE TABLE mes_pro_batch_record_approval_snapshot (id BIGINT PRIMARY KEY,process_instance_id VARCHAR,execution_id BIGINT,tenant_id BIGINT,deleted INT)");
            statement.execute("CREATE TABLE mes_pro_batch_record_execution (id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT)");
            statement.execute("INSERT INTO ACT_HI_TASKINST VALUES ('task','definition','process','7')");
            statement.execute("INSERT INTO ACT_RE_PROCDEF VALUES ('definition','7')");
            statement.execute("INSERT INTO mes_pro_batch_record_execution VALUES (100,7,0),(200,8,0)");
            // A foreign snapshot with the same process reference cannot authorize the source.
            statement.execute("INSERT INTO mes_pro_batch_record_approval_snapshot VALUES (1,'process',100,8,0)");
            String sql=MesLegacyApprovalPageMapper.class.getMethod("countDone",com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class)
                    .getAnnotation(org.apache.ibatis.annotations.Select.class).value()[0]
                    .replace(" AS UNSIGNED)"," AS BIGINT)") // H2 has no UNSIGNED type; fixture tenants are positive signed IDs.
                    .replace("${ew.customSqlSegment}","WHERE t.TENANT_ID_='7' AND d.TENANT_ID_='7' AND (s.id IS NULL OR x.id IS NULL)");
            try(var rs=statement.executeQuery(sql)){assertTrue(rs.next());assertEquals(1L,rs.getLong(1));}
            // A local snapshot pointing to another tenant's execution is also rejected.
            statement.execute("UPDATE mes_pro_batch_record_approval_snapshot SET tenant_id=7,execution_id=200");
            try(var rs=statement.executeQuery(sql)){assertTrue(rs.next());assertEquals(1L,rs.getLong(1));}
            statement.execute("UPDATE mes_pro_batch_record_approval_snapshot SET execution_id=100");
            try(var rs=statement.executeQuery(sql)){assertTrue(rs.next());assertEquals(0L,rs.getLong(1));}
        }
    }

}
