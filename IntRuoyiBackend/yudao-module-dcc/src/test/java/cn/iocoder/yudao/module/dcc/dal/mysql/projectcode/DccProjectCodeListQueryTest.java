package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DccProjectCodeListQueryTest {
    @Test
    void countAndPageShareDerivedFiltersAndBoundValuesBeforeLimit() {
        var request = new DccProjectCodePageReqVO();
        request.setPageNo(3);
        request.setRouteConfigured(true);
        request.setMainBatchRecordConfigured(false);
        request.setQaRegulationConfigured(true);
        request.setRequireDccProductCode(true);
        request.setFileCountSort("desc");
        request.setListFilters("[{\"key\":\"associatedFileCount\",\"operator\":\"eq\",\"value\":12},"
                + "{\"key\":\"routeStatus\",\"operator\":\"contains\",\"value\":\"A%_\"},"
                + "{\"key\":\"qaRegulationStatus\",\"operator\":\"contains\",\"value\":\"已发布\"}]");
        var wrapper = DccProjectCodeListQuery.filter(request, List.of(41L, 43L), true, true);
        var parameters = new HashMap<String, Object>();
        parameters.put("tenantId", 9L);
        parameters.put("req", request);
        parameters.put("ew", wrapper);
        parameters.put("offset", 20L);
        var config = new Configuration();
        String count = bind(config, DccProjectCodeListQuery.count(), parameters);
        String page = bind(config, DccProjectCodeListQuery.page(), parameters);
        assertEquals(count.substring(count.indexOf(" FROM (")).trim(),
                page.substring(page.indexOf(" FROM ("), page.indexOf(" ORDER BY ", page.indexOf(") q WHERE"))).trim());
        assertTrue(count.contains("q.id IN"));
        assertTrue(count.contains("q.status = ?"));
        assertTrue(count.contains("p.tenant_id=?"));
        assertTrue(page.contains("q.associated_file_count DESC"));
        assertTrue(page.contains("LIMIT ? OFFSET ?"));
        assertFalse(count.contains("LIMIT ? OFFSET ?"));
        assertFalse(page.contains("A%_"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%a!%!_%"));
    }

    @Test
    void qaPermissionUsesSameDeniedDisplayAndExportRetainsFilterWithoutLimit() {
        var request = new DccProjectCodePageReqVO();
        request.setPageSize(-1);
        request.setListFilters("[{\"key\":\"qaRegulationStatus\",\"operator\":\"eq\",\"value\":\"无查询权限\"}]");
        var filter = DccProjectCodeListQuery.filter(request, List.of(), false, false);
        assertTrue(filter.getSqlSegment().contains("无查询权限"));
        assertFalse(filter.getSqlSegment().contains("q.qa_display"));
        assertTrue(filter.getSqlSegment().contains("1 = 0"));
        Map<String, Object> parameters = Map.of("tenantId", 9L, "req", request, "ew", filter, "offset", 0L);
        assertFalse(bind(new Configuration(), DccProjectCodeListQuery.page(), parameters).contains("LIMIT ? OFFSET ?"));
    }

    private static String bind(Configuration config, String script, Map<String, Object> parameters) {
        var bound = new XMLLanguageDriver().createSqlSource(config, script, Map.class).getBoundSql(parameters);
        bound.getParameterMappings().stream().filter(mapping -> mapping.getProperty().startsWith("ew."))
                .forEach(mapping -> assertNotNull(config.newMetaObject(parameters).getValue(mapping.getProperty())));
        return bound.getSql();
    }
}
