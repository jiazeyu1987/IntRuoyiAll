package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesActiveOrderSignatureEvidenceControllerTest {
    @Test void teamEntryRetainsTeamQueryPermission() throws Exception {
        var method = MesActiveOrderSignatureEvidenceController.class.getMethod("team", Long.class, Long.class);
        assertEquals("@ss.hasPermission('mes:pro-process-pool-team-leader:query')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals("/mes/pro/process-pool/team-leader/active-order/signature/get",
                method.getAnnotation(GetMapping.class).value()[0]);
    }

    @Test void pqcEntryRetainsPqcQueryPermission() throws Exception {
        var method = MesActiveOrderSignatureEvidenceController.class.getMethod("pqc", Long.class, Long.class);
        assertEquals("@ss.hasPermission('mes:pro-production-release:query')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals("/mes/pro/production-release/pqc/signature/get",
                method.getAnnotation(GetMapping.class).value()[0]);
    }

    @Test void historicBatchEntryRetainsBatchQueryPermission() throws Exception {
        var method = MesActiveOrderSignatureEvidenceController.class.getMethod("batch", Long.class, Long.class, Long.class);
        assertEquals("@ss.hasPermission('mes:pro-edhr-batch-execution:query')",
                method.getAnnotation(PreAuthorize.class).value());
        assertEquals("/mes/pro/edhr-batch-execution/signature/get", method.getAnnotation(GetMapping.class).value()[0]);
    }
}
