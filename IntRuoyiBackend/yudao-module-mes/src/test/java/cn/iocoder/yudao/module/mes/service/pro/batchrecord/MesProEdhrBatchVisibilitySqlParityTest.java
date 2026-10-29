package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Both implementations read the same persisted fixture; the old canViewBatch is the independent oracle. */
class MesProEdhrBatchVisibilitySqlParityTest {
    public static void main(String[] args) throws Exception {
        new MesProEdhrBatchVisibilitySqlParityTest().matchesExistingSingleBatchVisibility();
        System.out.println("PASS production visibility SQL versus existing canViewBatch");
    }
    @Test void matchesExistingSingleBatchVisibility() throws Exception {
        TenantContextHolder.setTenantId(1L);
        try { new MesProEdhrBatchVisibilitySqlTest().compareWith(this::legacy); }
        finally { TenantContextHolder.clear(); }
    }
    private boolean legacy(Connection db, List<Long> matchingRules) throws Exception {
        var service = new MesProEdhrBatchExecutionVisibilityService();
        var permission = mock(PermissionApi.class);
        var origins = mock(MesProEdhrBatchExecutionOriginMapper.class);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var releases = mock(MesProEdhrReleaseTransactionMapper.class);
        var work = mock(MesProEdhrWorkTaskMapper.class);
        var reports = mock(MesProBatchRecordReportMapper.class);
        var rules = mock(MesProEdhrProcessFormPermissionRuleMapper.class);
        var resolver = mock(MesProEdhrCandidateResolver.class);
        ReflectionTestUtils.setField(service,"permissionApi",permission);
        ReflectionTestUtils.setField(service,"batchOriginMapper",origins);
        ReflectionTestUtils.setField(service,"activeOrderMapper",orders);
        ReflectionTestUtils.setField(service,"releaseApplicationMapper",applications);
        ReflectionTestUtils.setField(service,"releaseTransactionMapper",releases);
        ReflectionTestUtils.setField(service,"workTaskMapper",work);
        ReflectionTestUtils.setField(service,"reportMapper",reports);
        ReflectionTestUtils.setField(service,"processFormPermissionRuleMapper",rules);
        ReflectionTestUtils.setField(service,"candidateResolver",resolver);
        when(origins.selectListByBatchExecutionId(anyLong())).thenAnswer(call -> rows(db,
                "SELECT * FROM mes_pro_edhr_batch_execution_origin WHERE tenant_id=1 AND deleted=0 AND batch_execution_id="+call.getArgument(0)+" ORDER BY id",MesProEdhrBatchExecutionOriginDO.class));
        when(orders.selectById(anyLong())).thenAnswer(call -> first(rows(db,
                "SELECT * FROM mes_pro_process_pool_active_order WHERE tenant_id=1 AND deleted=0 AND id="+call.getArgument(0),MesProcessPoolActiveOrderDO.class)));
        when(applications.selectByBatchExecutionId(anyLong())).thenAnswer(call -> first(rows(db,
                "SELECT * FROM mes_pro_process_pool_active_order_release_application WHERE tenant_id=1 AND deleted=0 AND batch_execution_id="+call.getArgument(0),MesProcessPoolActiveOrderReleaseApplicationDO.class)));
        when(releases.selectByBatchExecutionId(anyLong())).thenAnswer(call -> first(rows(db,
                "SELECT * FROM mes_pro_edhr_release_transaction WHERE tenant_id=1 AND deleted=0 AND batch_execution_id="+call.getArgument(0)+" ORDER BY id DESC LIMIT 1",MesProEdhrReleaseTransactionDO.class)));
        when(work.selectTimelineListByBatchExecutionId(anyLong())).thenAnswer(call -> rows(db,
                "SELECT * FROM mes_pro_edhr_work_task WHERE tenant_id=1 AND deleted=0 AND batch_execution_id="+call.getArgument(0)+" ORDER BY create_time,id",MesProEdhrWorkTaskDO.class));
        when(work.selectById(anyLong())).thenAnswer(call -> first(rows(db,
                "SELECT * FROM mes_pro_edhr_work_task WHERE tenant_id=1 AND deleted=0 AND id="+call.getArgument(0),MesProEdhrWorkTaskDO.class)));
        when(reports.selectByReportId(anyString())).thenAnswer(call -> first(rows(db,
                "SELECT * FROM mes_pro_batch_record_report WHERE tenant_id=1 AND deleted=0 AND report_id='"+call.getArgument(0)+"'",MesProBatchRecordReportDO.class)));
        when(rules.selectEnabledFillRulesForRouteOrReport(anyLong(),anyString(),nullable(Long.class))).thenAnswer(call ->
                ruleRows(db,call.getArgument(0),call.getArgument(1),call.getArgument(2)));
        when(rules.selectEnabledFillRulesForRouteOrReport(anyLong(),anyString())).thenAnswer(call ->
                ruleRows(db,call.getArgument(0),call.getArgument(1),null));
        when(resolver.resolveProcessFormRule(any())).thenAnswer(call -> new MesProEdhrCandidateResolver.MesProEdhrCandidateContract(
                "USER",null,matchingRules.contains(call.<MesProEdhrProcessFormPermissionRuleDO>getArgument(0).getId()) ? "71" : "72"));
        var batch = rows(db,"SELECT * FROM mes_pro_edhr_batch_execution WHERE id=1",MesProEdhrBatchExecutionDO.class).get(0);
        var tasks = rows(db,"SELECT * FROM mes_pro_edhr_batch_execution_task WHERE tenant_id=1 AND deleted=0 AND batch_execution_id=1 ORDER BY route_process_sort,route_process_id,batch_record_sort,id",MesProEdhrBatchExecutionTaskDO.class);
        var handovers = rows(db,"SELECT * FROM mes_pro_edhr_market_release_handover WHERE tenant_id=1 AND deleted=0 AND batch_execution_id=1 ORDER BY id DESC",MesMarketReleaseHandoverMapper.Row.class);
        return service.canViewBatch(batch,tasks,71L,MesMarketReleaseAccess.resolve(handovers,71L));
    }
    private List<MesProEdhrProcessFormPermissionRuleDO> ruleRows(Connection db,Long route,String report,Long version) throws Exception {
        String base="SELECT * FROM mes_pro_edhr_process_form_permission_rule WHERE tenant_id=1 AND deleted=0 AND enabled=1 AND rule_type IN ('FILL','EQUIPMENT_FILL','QUALITY_FILL') AND batch_record_report_id='"+report+"' AND batch_record_version_id"+(version==null?" IS NULL":"="+version)+" AND route_process_id=";
        String order=" ORDER BY rule_type,scope_key,id DESC";
        var routeRules=rows(db,base+route+order,MesProEdhrProcessFormPermissionRuleDO.class);
        return routeRules.isEmpty()?rows(db,base+"0"+order,MesProEdhrProcessFormPermissionRuleDO.class):routeRules;
    }
    private static <T> T first(List<T> rows) { return rows.isEmpty()?null:rows.get(0); }
    private static <T> List<T> rows(Connection db,String sql,Class<T> type) throws Exception {
        var result = new ArrayList<T>(); var mapper = BeanPropertyRowMapper.newInstance(type);
        try(var statement=db.createStatement();var rows=statement.executeQuery(sql)) {
            while(rows.next()) result.add(mapper.mapRow(rows,result.size()));
        }
        return result;
    }
}
