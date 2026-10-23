package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccCategoryApprovalMatrixRespVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DccCategoryApprovalMatrixConfigurationContractTest {
    @Test void newlyExposedLongIdentitiesSerializeAsExactDecimalStrings() throws Exception {
        long exact = 9007199254740993L;
        var response = new DccCategoryApprovalMatrixRespVO().setFileTypeTaxonomyId(exact)
                .setRouteId(exact + 1).setDefaultSignoffDepartmentIds(List.of(exact));
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(response));
        assertTrue(json.get("fileTypeTaxonomyId").isTextual());
        assertEquals("9007199254740993", json.get("fileTypeTaxonomyId").asText());
        assertEquals("9007199254740994", json.get("routeId").asText());
        assertTrue(json.get("defaultSignoffDepartmentIds").get(0).isTextual());
        assertEquals("9007199254740993", json.get("defaultSignoffDepartmentIds").get(0).asText());
    }
}
