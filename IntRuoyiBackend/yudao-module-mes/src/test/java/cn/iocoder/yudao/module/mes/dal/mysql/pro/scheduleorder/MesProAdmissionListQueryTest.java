package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.RowBounds;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MesProAdmissionListQueryTest {
    @Test
    void candidateProductsPreserveWorkOrderShortCircuitWithoutLoadingWorkOrders() {
        var request = new MesProScheduleOrderAdmissionDiffPageReqVO();
        request.setWorkOrderCode("MO-1");
        request.setProductSpecification("spec");
        var base = MesProAdmissionListQuery.baseFilter(request);
        String sql = bind(MesProAdmissionListQuery.products(), Map.of("tenantId", 1L, "base", base));
        assertTrue(sql.startsWith("SELECT DISTINCT q.product_id"));
        assertTrue(sql.contains("q.base_reason_code IS NULL"));
        assertTrue(sql.indexOf("x.temporary_frozen=1") < sql.indexOf("x.blocking_schedule_order_id IS NOT NULL"));
        assertTrue(sql.indexOf("x.blocking_schedule_order_id IS NOT NULL") < sql.indexOf("x.formal_erp_identity=0"));
        assertTrue(sql.contains("a.active_status IN ('ACTIVE','REMOVED')"));
        assertTrue(sql.contains("COALESCE(a.simulated,0)=0"));
        assertTrue(sql.contains("q.product_specification LIKE ?"));
        assertFalse(sql.contains("MO-1"));
    }

    @Test
    void summaryAndPageApplySameDisplayConditionsToBoundRealTimeOutcomes() {
        var request = new MesProScheduleOrderAdmissionDiffPageReqVO();
        request.setListFilters("[{\"key\":\"analysis\",\"operator\":\"eq\",\"value\":\"可加入\"},"
                + "{\"key\":\"admissionStatus\",\"operator\":\"contains\",\"value\":\"可入池\"},"
                + "{\"key\":\"message\",\"operator\":\"contains\",\"value\":\"缺路线\"}]");
        var parameters = new HashMap<String, Object>();
        parameters.put("tenantId", 1L);
        parameters.put("base", MesProAdmissionListQuery.baseFilter(request));
        parameters.put("ew", MesProAdmissionListQuery.filter(request));
        parameters.put("baseOutcomes", MesProAdmissionListQuery.BASE_OUTCOMES);
        parameters.put("routeOutcomes", List.of(new MesProAdmissionListQuery.Outcome(21L,"READY_TO_ADMIT","PASS","READY_TO_ADMIT","生产工单可加入排产工单池","排产员",true)));
        parameters.put("req", request);
        parameters.put("offset", 30L);
        String summary = bind(MesProAdmissionListQuery.summary(), parameters);
        String page = bind(MesProAdmissionListQuery.page(), parameters);
        assertEquals(summary.substring(summary.indexOf(" FROM (")).trim(),
                page.substring(page.indexOf(" FROM ("), page.lastIndexOf(" ORDER BY q.temporary_frozen")).trim());
        assertTrue(page.contains("UNION ALL"));
        assertTrue(page.contains("ro.product_id<=>b.product_id"));
        assertTrue(page.contains("LIMIT ? OFFSET ?"));
        assertFalse(summary.contains("LIMIT ? OFFSET ?"));
        request.setPageSize(-1);
        assertFalse(bind(MesProAdmissionListQuery.page(), parameters).contains("LIMIT ? OFFSET ?"));
    }

    private static String bind(String source, Map<String, Object> parameters) {
        var configuration = new Configuration();
        var sqlSource = new XMLLanguageDriver().createSqlSource(configuration, source, Map.class);
        var sql = sqlSource.getBoundSql(parameters);
        sql.getParameterMappings().forEach(parameter -> {
            if (parameter.getProperty().startsWith("__frch_")) {
                assertTrue(sql.hasAdditionalParameter(parameter.getProperty()));
            } else assertNotNull(configuration.newMetaObject(parameters).getValue(parameter.getProperty()));
        });
        try {
            var interceptor = new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override public Expression getTenantId() { return new LongValue(1); }
            });
            var statement = new MappedStatement.Builder(configuration, "admission.query", sqlSource, SqlCommandType.SELECT).build();
            // Exercise the actual MyBatis-Plus tenant rewrite, including derived UNION dimensions.
            var rewritten = new org.apache.ibatis.mapping.BoundSql(configuration, sql.getSql(), sql.getParameterMappings(), parameters);
            interceptor.beforeQuery(null, statement, parameters, RowBounds.DEFAULT, null, rewritten);
            assertNotNull(CCJSqlParserUtil.parse(rewritten.getSql()));
        } catch (Exception exception) { throw new AssertionError("Tenant SQL rewrite failed", exception); }
        return sql.getSql().trim();
    }
}
