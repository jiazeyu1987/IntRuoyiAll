package cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport;

import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportPageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DccHistoricalImportListFilterTest {

    @Test
    void historicalCountAndPageShareAllDerivedFiltersAndTenantScope() {
        var request = new DccRegistrationCertificateHistoricalImportPageReqVO();
        request.setListFilters("[{\"key\":\"restrictedReasons\",\"operator\":\"contains\",\"value\":\"资料不齐\"},"
                + "{\"key\":\"ownerCompanyCode\",\"operator\":\"eq\",\"value\":\"INT-001\"},"
                + "{\"key\":\"versionNo\",\"operator\":\"eq\",\"value\":\"-\"},"
                + "{\"key\":\"result\",\"operator\":\"eq\",\"value\":\"导入失败\"}]");
        var filter = DccRegistrationCertificateHistoricalImportServiceImpl.historicalListFilter(request);
        var parameters = new HashMap<String, Object>();
        parameters.put("tenantId", 1L);
        parameters.put("sourceHash", "ABC");
        parameters.put("ew", filter);
        parameters.put("limit", 10);
        parameters.put("offset", 20);
        int checked = 0;
        for (var method : DccRegistrationCertificateAuditMapper.class.getMethods()) {
            if (!method.getName().equals("countHistoricalImportPage")
                    && !method.getName().equals("selectHistoricalImportPage")) continue;
            var sql = String.join(" ", method.getAnnotation(Select.class).value());
            var config = new Configuration();
            var bound = new XMLLanguageDriver().createSqlSource(config, sql, Map.class).getBoundSql(parameters);
            assertTrue(bound.getSql().contains("a.tenant_id = ?"));
            assertTrue(bound.getSql().contains("LEFT JOIN dcc_registration_certificate_version v"));
            assertTrue(bound.getSql().contains("LEFT JOIN dcc_registration_certificate_snapshot s"));
            assertTrue(bound.getSql().contains("group_concat_max_len=4294967295"));
            assertTrue(bound.getSql().contains("JSON_TABLE"));
            assertFalse(bound.getSql().contains("资料不齐"));
            assertTrue(bound.getSql().contains("enterprise_code"));
            var mappings = bound.getParameterMappings().stream()
                    .filter(mapping -> mapping.getProperty().startsWith("ew.paramNameValuePairs.")).toList();
            assertEquals(4, mappings.size());
            mappings.forEach(mapping -> assertNotNull(config.newMetaObject(parameters).getValue(mapping.getProperty())));
            checked++;
        }
        assertEquals(2, checked);
    }
}
