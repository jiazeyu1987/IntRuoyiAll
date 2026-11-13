package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesSubmissionListFilters;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MesOperationalListFilterSqlTest {
    private static void assertTenantRewrite(String sql) throws Exception {
        var interceptor = new com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor(
                new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
                    @Override public net.sf.jsqlparser.expression.Expression getTenantId() {
                        return new net.sf.jsqlparser.expression.LongValue(1);
                    }
                    @Override public boolean ignoreTable(String tableName) {
                        return tableName.equals("list_filter_types");
                    }
                });
        String rewritten = interceptor.parserSingle(sql, null);
        assertTrue(rewritten.contains("tenant_id"));
        assertNotNull(CCJSqlParserUtil.parse(rewritten));
    }

    @Test
    void everyOperationalProjectionIsParseableByTheTenantInterceptor() throws Exception {
        Map<String, Class<?>> mappers = Map.ofEntries(
                Map.entry("mes_pro_work_order", cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper.class),
                Map.entry("mes_pro_schedule_order", cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper.class),
                Map.entry("mes_pro_process", cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper.class),
                Map.entry("mes_pro_route", cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper.class),
                Map.entry("mes_pro_feedback", cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper.class),
                Map.entry("mes_pro_feedback_import_record", cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackImportRecordMapper.class),
                Map.entry("mes_md_workstation", cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper.class),
                Map.entry("mes_md_workshop", cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkshopMapper.class),
                Map.entry("mes_qc_template", cn.iocoder.yudao.module.mes.dal.mysql.qc.template.MesQcTemplateMapper.class),
                Map.entry("mes_pro_mes_process_catalog", cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess.MesProMesProcessCatalogMapper.class),
                Map.entry("mes_dv_subject", cn.iocoder.yudao.module.mes.dal.mysql.dv.subject.MesDvSubjectMapper.class),
                Map.entry("mes_dv_check_plan", cn.iocoder.yudao.module.mes.dal.mysql.dv.checkplan.MesDvCheckPlanMapper.class),
                Map.entry("mes_dv_check_record", cn.iocoder.yudao.module.mes.dal.mysql.dv.checkrecord.MesDvCheckRecordMapper.class),
                Map.entry("mes_dv_mainten_record", cn.iocoder.yudao.module.mes.dal.mysql.dv.maintenrecord.MesDvMaintenRecordMapper.class),
                Map.entry("mes_dv_machinery", cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper.class),
                Map.entry("mes_dv_repair", cn.iocoder.yudao.module.mes.dal.mysql.dv.repair.MesDvRepairMapper.class),
                Map.entry("mes_qc_indicator", cn.iocoder.yudao.module.mes.dal.mysql.qc.indicator.MesQcIndicatorMapper.class));
        for (var field : MesSubmissionListFilters.FIELDS.entrySet()) {
            assertNotNull(CCJSqlParserUtil.parse("SELECT " + field.getValue().expression()
                    + " FROM mes_pro_process_pool_event pool_event"), "Timeline." + field.getKey());
        }
        for (var mapper : mappers.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, ListFilterQueryUtils.Field> fields = (Map<String, ListFilterQueryUtils.Field>) mapper.getValue().getField("LIST_FILTER_FIELDS").get(null);
            if (mapper.getKey().equals("mes_pro_schedule_order")) {
                var method = Class.forName("cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesScheduleOrderListFilters")
                        .getDeclaredMethod("augment", Map.class);
                method.setAccessible(true);
                fields = (Map<String, ListFilterQueryUtils.Field>) method.invoke(null, fields);
            }
            if (mapper.getKey().equals("mes_pro_process")) {
                var processMapper = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper.class,
                        org.mockito.Mockito.CALLS_REAL_METHODS);
                fields = processMapper.listFilterFields(new cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessPageReqVO());
                var scopedRequest = new cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessPageReqVO();
                scopedRequest.setRouteId(12L);
                for (var field : processMapper.listFilterFields(scopedRequest).entrySet()) {
                    assertNotNull(CCJSqlParserUtil.parse("SELECT " + field.getValue().expression() + " FROM mes_pro_process"), "ScopedProcess." + field.getKey());
                }
            }
            for (var field : fields.entrySet()) {
                String sql = "SELECT " + field.getValue().expression() + " FROM " + mapper.getKey();
                assertNotNull(CCJSqlParserUtil.parse(sql), mapper.getKey() + "." + field.getKey());
                assertTenantRewrite(sql);
            }
        }
    }

    @Test
    void qcTemplateCollectionOptionsRemainBoundAndTenantParseable() throws Exception {
        for (String operator : java.util.List.of("eq", "in")) {
            var request = new cn.iocoder.yudao.module.mes.controller.admin.qc.template.vo.MesQcTemplatePageReqVO();
            request.setListFilters("[{\"key\":\"types\",\"operator\":\"" + operator + "\",\"value\":"
                    + (operator.equals("in") ? "[\"IPQC\",\"FQC\"]" : "\"IPQC\"") + "}]");
            var filters = ListFilterQueryUtils.wrapper(request,
                    cn.iocoder.yudao.module.mes.dal.mysql.qc.template.MesQcTemplateMapper.LIST_FILTER_FIELDS);
            String sql = "SELECT * FROM mes_qc_template WHERE " + filters.getSqlSegment();
            // MyBatis placeholder paths are bound JDBC parameters in the executed statement.
            sql = sql.replaceAll("#\\{[^}]+}", "?");
            assertTrue(sql.contains("WITH RECURSIVE"));
            assertFalse(sql.contains("IPQC"));
            assertNotNull(CCJSqlParserUtil.parse(sql));
            assertTenantRewrite(sql);
        }
    }

    @Test
    void timelineFiltersApplyToCountAndTheDatabasePageBeforeLimit() throws Exception {
        Configuration configuration = new Configuration();
        Path path = Path.of("src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml");
        try (var input = Files.newInputStream(path)) {
            new XMLMapperBuilder(input, configuration, path.toString(), configuration.getSqlFragments()).parse();
        }
        for (String eventType : java.util.List.of("PRODUCTION_SUBMIT", "PQC_INSPECTION")) {
            ProcessPoolTimelinePageReqVO request = new ProcessPoolTimelinePageReqVO();
            request.setPageNo(2);
            request.setPageSize(20);
            request.setEventType(eventType);
            request.setListFilters("[{\"key\":\"completionQuantity\",\"operator\":\"contains\",\"value\":\"17 件%\"},{\"key\":\"inspectionTask\",\"operator\":\"contains\",\"value\":\"首检\"}]");
            var filters = ListFilterQueryUtils.wrapper(request, MesSubmissionListFilters.FIELDS);
            Map<String, Object> parameters = Map.of("reqVO", request, "ew", filters);
            for (String statement : java.util.List.of("selectTimelineCount", "selectTimelinePage")) {
                String sql = configuration.getMappedStatement("cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolTimelineReadMapper." + statement)
                        .getBoundSql(parameters).getSql();
                assertTrue(sql.contains("list_filter_payload.root_payload"));
                assertTrue(sql.contains("LATERAL"));
                assertFalse(sql.contains("17 件%"), "User values must remain bound parameters");
                assertFalse(sql.contains("GROUP_CONCAT"), "Group display must not be truncated at group_concat_max_len");
                assertNotNull(CCJSqlParserUtil.parse(sql));
                assertTenantRewrite(sql);
                if (statement.equals("selectTimelinePage")) {
                    assertTrue(sql.indexOf(" LIKE ") < sql.indexOf("LIMIT ?"), "Filtering must run before pagination");
                }
            }
        }
    }
}
