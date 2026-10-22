package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import static org.junit.jupiter.api.Assertions.assertEquals;
class MesActiveOrderCorrectionEvidenceControllerTest {
    @ParameterizedTest @CsvSource({"team,mes:pro-process-pool-team-leader:query,/mes/pro/process-pool/team-leader/active-order/corrections", "pqc,mes:pro-production-release:query,/mes/pro/production-release/pqc/corrections", "batch,mes:pro-edhr-batch-execution:query,/mes/pro/edhr-batch-execution/corrections"})
    void correctionReadRetainsTheExactExistingDetailPermission(String method,String permission,String route)throws Exception {
        var endpoint="batch".equals(method)?MesActiveOrderCorrectionEvidenceController.class.getMethod(method,Long.class,Long.class)
                :MesActiveOrderCorrectionEvidenceController.class.getMethod(method,Long.class);
        assertEquals("@ss.hasPermission('"+permission+"')",endpoint.getAnnotation(PreAuthorize.class).value());
        assertEquals(route,endpoint.getAnnotation(GetMapping.class).value()[0]);
    }
}
