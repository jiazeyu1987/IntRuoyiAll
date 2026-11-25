package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateQueryMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageQuery;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DccRegistrationCertificateListFilterTest {

    @ParameterizedTest
    @CsvSource({"false,CE,ce", "false,FDA,fda", "false,国内,国内",
            "true,CE,ce", "true,FDA,fda", "true,国内,国内"})
    void certificateTypeFiltersDisplayLabelsInCountAndRows(boolean oldIndex, String label, String expected) {
        var query = DccRegistrationCertificatePageQuery.builder()
                .listFilters("[{\"key\":\"certificateType\",\"operator\":\"contains\",\"value\":\"" + label + "\"}]")
                .build();
        query.setListFilterWrapper(DccRegistrationCertificateQueryMapper.SqlProvider.listFilterWrapper(query, oldIndex));
        assertSql(oldIndex ? DccRegistrationCertificateQueryMapper.SqlProvider.countOldIndex()
                : DccRegistrationCertificateQueryMapper.SqlProvider.countPage(), query, false);
        assertSql(oldIndex ? DccRegistrationCertificateQueryMapper.SqlProvider.selectOldIndexPage()
                : DccRegistrationCertificateQueryMapper.SqlProvider.selectPage(), query, true);
        assertTrue(query.getListFilterWrapper().getSqlSegment().contains("$.certificateType"));
        assertTrue(query.getListFilterWrapper().getParamNameValuePairs().containsValue("%" + expected + "%"));
    }

    @Test
    void currentCountAndRowsBindSameFilterBeforePaginationAndKeepTenantScope() {
        var query = DccRegistrationCertificatePageQuery.builder().businessDate(LocalDate.of(2026, 11, 13))
                .listFilters("[{\"key\":\"status\",\"operator\":\"eq\",\"value\":\"当前有效\"},"
                        + "{\"key\":\"hasRegistrationFile\",\"operator\":\"eq\",\"value\":\"缺失\"}]")
                .build();
        query.setListFilterWrapper(DccRegistrationCertificateQueryMapper.SqlProvider.listFilterWrapper(query, false));
        assertSql(DccRegistrationCertificateQueryMapper.SqlProvider.countPage(), query, false);
        assertSql(DccRegistrationCertificateQueryMapper.SqlProvider.selectPage(), query, true);
    }

    @Test
    void oldIndexFiltersDisplayedExpiryStatusAndBindsThroughNestedWrapper() {
        var query = DccRegistrationCertificatePageQuery.builder()
                .listFilters("[{\"key\":\"status\",\"operator\":\"contains\",\"value\":\"原有效期至 2026\"}]")
                .build();
        query.setListFilterWrapper(DccRegistrationCertificateQueryMapper.SqlProvider.listFilterWrapper(query, true));
        assertSql(DccRegistrationCertificateQueryMapper.SqlProvider.countOldIndex(), query, false);
        assertSql(DccRegistrationCertificateQueryMapper.SqlProvider.selectOldIndexPage(), query, true);
        assertTrue(query.getListFilterWrapper().getSqlSegment().contains("已被替换，原有效期至"));
    }

    private static void assertSql(String script, DccRegistrationCertificatePageQuery query, boolean paged) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", 1L);
        parameters.put("companyIds", java.util.List.of());
        parameters.put("query", query);
        parameters.put("limit", 10);
        parameters.put("offset", 20);
        var config = new Configuration();
        var bound = new XMLLanguageDriver().createSqlSource(config, script, Map.class).getBoundSql(parameters);
        assertTrue(bound.getSql().contains("c.tenant_id = ?"));
        assertEquals(paged, bound.getSql().contains("LIMIT ? OFFSET ?"));
        var mappings = bound.getParameterMappings().stream()
                .filter(mapping -> mapping.getProperty().startsWith("query.listFilterWrapper.paramNameValuePairs."))
                .toList();
        assertFalse(mappings.isEmpty());
        for (var mapping : mappings) {
            assertNotNull(config.newMetaObject(parameters).getValue(mapping.getProperty()));
        }
        assertFalse(bound.getSql().contains("当前有效' OR"));
    }
}
