package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.approval.DccRegistrationCertificateApprovalDetailController;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAccessRequestDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateApprovalDetailControllerTest {
    @Test
    void historicalNameDoesNotHideMissingFileSnapshots() {
        var approvals = mock(DccRegistrationCertificateApprovalService.class);
        var jdbc = mock(JdbcTemplate.class);
        var request = DccRegistrationCertificateAccessRequestDO.builder().certificateId(3L)
                .requestType("DOWNLOAD_FILE").ownerCompanyId(10L)
                .detailJson("{\"ownerCompanyName\":\"公司\"}").build();
        when(approvals.requireApprovalDetailRequest(eq(1L), nullable(Long.class), eq(2L), eq("process"))).thenReturn(request);
        when(jdbc.queryForList(anyString(), eq(String.class), eq(3L), eq(3L), eq(3L), eq(1L), eq(2L)))
                .thenReturn(java.util.Arrays.asList("旧证名称", null));
        TenantContextHolder.setTenantId(1L);
        try {
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> new DccRegistrationCertificateApprovalDetailController(approvals, jdbc).get(2L,"process"));
        } finally { TenantContextHolder.clear(); }
    }
    @Test
    void preservesSubmittedCompanySnapshot() {
        verifyCompany("{\"ownerCompanyName\":\"提交时公司\",\"certificateName\":\"产品名称\"}", "提交时公司", false);
    }

    @Test
    void resolvesHistoricalMissingCompanyUsingTenantAndOwner() {
        verifyCompany("{\"certificateName\":\"产品名称\"}", "实际所属公司", true);
    }

    @Test
    void resolvesEnterpriseInRequestTenantAndKeepsMissingNameVisible() {
        for (boolean missing : new boolean[]{false, true}) {
            var approvals = mock(DccRegistrationCertificateApprovalService.class);
            var jdbc = mock(JdbcTemplate.class);
            var request = DccRegistrationCertificateAccessRequestDO.builder().requestType("UPLOAD")
                    .detailJson("{\"draft\":{\"entrustedEnterpriseIds\":[17]}}").build();
            when(approvals.requireApprovalDetailRequest(eq(1L), nullable(Long.class), eq(2L), eq("process"))).thenReturn(request);
            when(jdbc.queryForList(contains("FROM mdm_enterprise WHERE tenant_id=?"), eq(String.class), eq(1L), eq(17)))
                    .thenReturn(missing ? List.of() : List.of("受托公司"));
            TenantContextHolder.setTenantId(1L);
            try {
                var result = new DccRegistrationCertificateApprovalDetailController(approvals,jdbc).get(2L,"process").getData();
                var draft = (Map<?, ?>)((Map<?, ?>)result.get("submission")).get("draft");
                if (missing) assertTrue(draft.get("entrustedEnterpriseDisplayError").toString().contains("无法完整核对"));
                else assertTrue(draft.get("entrustedEnterprisesJson").toString().contains("受托公司"));
                assertTrue(result.containsKey("files"));
                verify(jdbc).queryForList(contains("FROM mdm_enterprise WHERE tenant_id=?"),eq(String.class),eq(1L),eq(17));
            } finally { TenantContextHolder.clear(); }
        }
    }

    @Test
    void changeDatesUseSourceFormalFactsAndSubmittedApprovalSeparately() {
        var approvals = mock(DccRegistrationCertificateApprovalService.class);
        var jdbc = mock(JdbcTemplate.class);
        var request = DccRegistrationCertificateAccessRequestDO.builder().requestType("CHANGE")
                .detailJson("{\"operation\":\"CHANGE_CERTIFICATE\",\"approvalDate\":\"2026-11-20\"}").build();
        when(approvals.requireApprovalDetailRequest(eq(1L),nullable(Long.class),eq(2L),eq("process"))).thenReturn(request);
        when(jdbc.queryForList(contains("SELECT s.facts_json"),eq(String.class),eq(1L),eq(2L)))
                .thenReturn(List.of("{\"approvalDate\":\"2025-01-01\"}"));
        TenantContextHolder.setTenantId(1L);
        try {
            var result = new DccRegistrationCertificateApprovalDetailController(approvals,jdbc).get(2L,"process").getData();
            assertEquals("2025-01-01",result.get("originalApprovalDate"));
            assertEquals("2026-11-20",((Map<?,?>)result.get("submission")).get("approvalDate"));
            verify(jdbc).queryForList(contains("s.id=c.source_snapshot_id"),eq(String.class),eq(1L),eq(2L));
        } finally { TenantContextHolder.clear(); }
    }

    private void verifyCompany(String json, String expected, boolean historical) {
        var approvals = mock(DccRegistrationCertificateApprovalService.class);
        var jdbc = mock(JdbcTemplate.class);
        var request = DccRegistrationCertificateAccessRequestDO.builder()
                .requestType("DOWNLOAD_FILE").ownerCompanyId(10L).detailJson(json).build();
        when(approvals.requireApprovalDetailRequest(eq(1L), nullable(Long.class), eq(2L), eq("process")))
                .thenReturn(request);
        if (historical) {
            when(jdbc.queryForList(anyString(), eq(String.class), eq(1L), eq(10L)))
                    .thenReturn(List.of(expected));
        }
        TenantContextHolder.setTenantId(1L);
        try {
            var result = new DccRegistrationCertificateApprovalDetailController(approvals, jdbc).get(2L, "process");
            assertEquals(expected, ((Map<?, ?>) result.getData().get("submission")).get("ownerCompanyName"));
            verify(approvals).requireApprovalDetailRequest(eq(1L), nullable(Long.class), eq(2L), eq("process"));
            if (!historical) verify(jdbc, never()).queryForList(anyString(), eq(String.class), any(), any());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
