package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportPageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.DccRegistrationCertificateHistoricalImportRow;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.DccRegistrationCertificateHistoricalImportServiceImpl;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DccRegistrationCertificateHistoricalImportServiceTest {

    @Mock
    private DccRegistrationCertificateAuditMapper auditMapper;
    @Mock
    private MdmEnterpriseApi enterpriseApi;

    private DccRegistrationCertificateHistoricalImportServiceImpl service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        service = new DccRegistrationCertificateHistoricalImportServiceImpl(auditMapper, enterpriseApi);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getPageReadsHistoricalImportDetailAndKeepsRestrictedReasons() {
        when(auditMapper.countHistoricalImportPage(1L, "ABC")).thenReturn(1L);
        when(auditMapper.selectHistoricalImportPage(1L, "ABC", 10, 0))
                .thenReturn(List.of(audit("ABC", 2, 301L)));
        when(enterpriseApi.getEnabledEnterprises(eq(List.of(301L)), eq(List.of("OWNED_COMPANY"))))
                .thenReturn(List.of(MdmEnterpriseRespDTO.builder()
                        .id(301L)
                        .tenantId(1L)
                        .enterpriseCode("INT-001")
                        .name("甲公司")
                        .type("OWNED_COMPANY")
                        .status("ENABLE")
                        .revision(1)
                        .build()));

        DccRegistrationCertificateHistoricalImportPageReqVO req =
                new DccRegistrationCertificateHistoricalImportPageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);
        req.setSourceHash(" abc ");

        var result = service.getHistoricalImportPage(req);

        assertEquals(1L, result.getTotal());
        assertEquals("ABC", result.getList().get(0).getSourceHash());
        assertEquals(2, result.getList().get(0).getSourceRow());
        assertEquals("甲公司", result.getList().get(0).getOwnerCompanyName());
        assertEquals(List.of("MISSING_PROJECT_CODE"),
                result.getList().get(0).getRestrictedReasons());
    }

    private DccRegistrationCertificateHistoricalImportRow audit(String sourceHash, int sourceRow, Long companyId) {
        DccRegistrationCertificateHistoricalImportRow row = new DccRegistrationCertificateHistoricalImportRow();
        row.setId(900L);
        row.setOwnerCompanyId(companyId);
        row.setCertificateId(901L);
        row.setCertificateRecordId(901L);
        row.setCertificateOwnerCompanyId(companyId);
        row.setVersionId(902L);
        row.setVersionRecordId(902L);
        row.setSnapshotId(903L);
        row.setSnapshotRecordId(903L);
        row.setActorId(1L);
        row.setResult("SUCCESS");
        row.setResultCode("OK");
        row.setRequestTraceId("trace-1");
        row.setDetailJson("""
                {"sourceHash":"%s","sourceRow":%d,"payloadHash":"payload",
                 "outcomeCertificateId":901,"outcomeVersionId":902,
                 "outcomeSnapshotId":903,"restrictedReasons":["MISSING_PROJECT_CODE"]}
                """.formatted(sourceHash.toUpperCase(), sourceRow));
        row.setOccurredAt(LocalDateTime.of(2026, 8, 24, 17, 0));
        row.setCertificateNo("REG-001");
        row.setVersionNo(1);
        row.setProductName("产品A");
        return row;
    }
}
