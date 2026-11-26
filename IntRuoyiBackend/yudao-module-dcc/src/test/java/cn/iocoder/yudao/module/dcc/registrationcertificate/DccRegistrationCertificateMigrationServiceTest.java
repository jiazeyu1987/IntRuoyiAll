package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.BatchCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.EntrustedEnterpriseCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.RowCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService.Result;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_TENANT_MISMATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({
        DccRegistrationCertificateMigrationService.class,
        DccRegistrationCertificateMigrationServiceTest.ClockConfiguration.class
})
class DccRegistrationCertificateMigrationServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateMigrationService service;
    @Resource
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private ControlledContentRegistrationProjectionService projectionService;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        if (jdbcTemplate != null) {
            jdbcTemplate.update("DELETE FROM mdm_enterprise WHERE enterprise_code LIKE 'IMPORT-%'");
        }
        TenantContextHolder.clear();
    }

    @Test
    void commitHistoricalBatch_persistsRestrictedArchiveRowsWithoutFilesAndReplaysBySourceRow() {
        ensureJdbcTemplate();
        BatchCommand command = batch(
                row(2, 10L, 20L, 30L, List.of("NO_ATTACHMENT_POLICY")),
                row(3, 11L, 21L, null, List.of("MISSING_PROJECT_CODE", "NO_ATTACHMENT_POLICY")));

        Result first = service.commitHistoricalBatch(command);

        assertEquals(2, first.committedCount());
        assertEquals(0, first.replayedCount());
        assertEquals(2, first.restrictedCount());
        assertEquals(2, count("dcc_registration_certificate"));
        assertEquals(2, count("dcc_registration_certificate_version"));
        assertEquals(2, count("dcc_registration_certificate_snapshot"));
        assertEquals(1, countWhere("dcc_registration_certificate", "project_code_id IS NULL"));
        assertEquals(0, count("dcc_registration_certificate_file"));
        assertEquals(2, countWhere("dcc_registration_certificate_audit",
                "event_type = 'HISTORICAL_IMPORT' AND result = 'SUCCESS'"));
        assertEquals(2, countWhere("dcc_registration_certificate_audit",
                "detail_json LIKE '%NO_ATTACHMENT_POLICY%'"));
        verify(projectionService, times(2)).registerActive(any(), any(), any(), any(), any(), any(), any(), any(), any());

        Result replay = service.commitHistoricalBatch(command);

        assertEquals(0, replay.committedCount());
        assertEquals(2, replay.replayedCount());
        assertEquals(2, replay.restrictedCount());
        assertEquals(2, count("dcc_registration_certificate"));
        assertEquals(0, count("dcc_registration_certificate_file"));
        verify(projectionService, times(2)).registerActive(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void commitHistoricalBatch_rollsBackWholeBatchWhenAnyRowIsInvalid() {
        ensureJdbcTemplate();
        BatchCommand command = batch(
                row(2, 10L, 20L, 30L, List.of("NO_ATTACHMENT_POLICY")),
                row(4, 12L, null, 31L, List.of("NO_ATTACHMENT_POLICY")));

        assertThrows(ServiceException.class, () -> service.commitHistoricalBatch(command));

        assertEquals(0, count("dcc_registration_certificate"));
        assertEquals(0, count("dcc_registration_certificate_version"));
        assertEquals(0, count("dcc_registration_certificate_snapshot"));
        assertEquals(0, count("dcc_registration_certificate_audit"));
        verify(projectionService, never()).registerActive(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void commitHistoricalBatch_sameSourceRowWithDifferentPayloadFailsWithoutIncrement() {
        ensureJdbcTemplate();
        BatchCommand command = batch(row(2, 10L, 20L, 30L, List.of("NO_ATTACHMENT_POLICY")));
        service.commitHistoricalBatch(command);

        BatchCommand changed = batch(row(2, 10L, 99L, 30L, List.of("NO_ATTACHMENT_POLICY")));

        assertServiceException(() -> service.commitHistoricalBatch(changed),
                REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        assertEquals(1, count("dcc_registration_certificate"));
        assertEquals(1, countWhere("dcc_registration_certificate_audit", "result = 'SUCCESS'"));
    }

    @Test
    void commitHistoricalBatch_futureEffectiveRowsRegisterReadyCandidateOnly() {
        ensureJdbcTemplate();

        Result result = service.commitHistoricalBatch(batch(futureRow(8)));

        assertEquals(1, result.committedCount());
        assertEquals(1, countWhere("dcc_registration_certificate",
                "status = 'PENDING_FIRST_EFFECTIVE' AND current_version_id IS NULL AND pending_version_id IS NOT NULL"));
        assertEquals(1, countWhere("dcc_registration_certificate_version",
                "status = 'PENDING_EFFECTIVE'"));
        verify(projectionService, never()).registerActive(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(projectionService, times(1)).registerReadyCandidate(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void commitHistoricalBatch_projectionFailureRollsBackFormalFacts() {
        ensureJdbcTemplate();
        doThrow(new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT))
                .when(projectionService)
                .registerActive(any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThrows(ServiceException.class, () -> service.commitHistoricalBatch(
                batch(row(9, 10L, 20L, 30L, List.of("NO_ATTACHMENT_POLICY")))));

        assertEquals(0, count("dcc_registration_certificate"));
        assertEquals(0, count("dcc_registration_certificate_version"));
        assertEquals(0, count("dcc_registration_certificate_snapshot"));
        assertEquals(0, count("dcc_registration_certificate_audit"));
    }

    @Test
    void commitHistoricalBatch_requiresCurrentTenantToMatchApprovedSubBatch() {
        ensureJdbcTemplate();
        TenantContextHolder.setTenantId(2L);

        assertServiceException(() -> service.commitHistoricalBatch(
                        batch(row(2, 10L, 20L, 30L, List.of("NO_ATTACHMENT_POLICY")))),
                REGISTRATION_CERTIFICATE_TENANT_MISMATCH);

        assertEquals(0, count("dcc_registration_certificate"));
        assertEquals(0, count("dcc_registration_certificate_version"));
        assertEquals(0, count("dcc_registration_certificate_audit"));
        verify(projectionService, never()).registerActive(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void commitHistoricalBatch_rejectsInvalidProjectCodeAndContradictoryProductionFactsWithoutIncrement() {
        ensureJdbcTemplate();

        assertThrows(ServiceException.class, () -> service.commitHistoricalBatch(
                batch(row(2, 10L, 20L, -1L, List.of("NO_ATTACHMENT_POLICY")))));
        assertEquals(0, count("dcc_registration_certificate"));
        assertEquals(0, count("dcc_registration_certificate_version"));
        assertEquals(0, count("dcc_registration_certificate_audit"));

        RowCommand contradictory = new RowCommand(3, 11L, 21L, null,
                "一次性使用造影导管", "上海瑛泰医疗器械股份有限公司",
                "沪械注准20262030001-3",
                LocalDate.of(2020, 1, 2), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1),
                "二类", "", "", "", "", "", "",
                false, true, List.of(new EntrustedEnterpriseCommand(100L, "山东瑛泰医疗器械有限公司")),
                List.of("NO_ATTACHMENT_POLICY"));

        assertThrows(ServiceException.class, () -> service.commitHistoricalBatch(batch(contradictory)));
        assertEquals(0, count("dcc_registration_certificate"));
        assertEquals(0, count("dcc_registration_certificate_version"));
        assertEquals(0, count("dcc_registration_certificate_audit"));
    }

    @Test
    void workbookKeepsFdaDatesEmptyAndSeparatesSheetsOnReplay() {
        ensureJdbcTemplate();
        var entries = new java.util.ArrayList<DccRegistrationCertificateMigrationService.WorkbookRow>();
        for (String type : List.of("FDA", "CE")) {
            var facts = new cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts();
            facts.setCertificateType(type);
            facts.setHistoricalSheet(type + "证书");
            facts.setHistoricalRow(4);
            var row = new RowCommand(4, 10L, null, null, "测试产品", null, null, null, null,
                    null, null, "II", null, null, null, null, null, null, null, null, List.of(),
                    List.of("NO_ATTACHMENT_POLICY", "HISTORICAL_WORKBOOK"));
            entries.add(new DccRegistrationCertificateMigrationService.WorkbookRow(row, facts));
        }
        var first = service.commitWorkbookRows(1L, 99L, "WORKBOOK", "test", entries);
        assertEquals(2, first.committedCount());
        assertEquals(2, countWhere("dcc_registration_certificate_version",
                "expiry_date IS NULL AND approval_date IS NULL AND status = 'CURRENT'"));
        assertEquals(2, service.commitWorkbookRows(1L, 99L, "WORKBOOK", "retry", entries).replayedCount());
        assertEquals(2, count("dcc_registration_certificate_audit"));
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "REGISTRATION_IMPORT_SAMPLE", matches = ".+")
    void importInitializationWorkbookThroughUploadService() throws Exception {
        ensureJdbcTemplate();
        var bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(System.getenv("REGISTRATION_IMPORT_SAMPLE")));
        var file = new org.springframework.mock.web.MockMultipartFile("file", "初始化.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        var entries = new cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.RegistrationCertificateWorkbook()
                .read(file.getInputStream());
        long id = 80000;
        for (String name : entries.stream().map(e -> e.company()).distinct().toList()) {
            jdbcTemplate.update("INSERT INTO mdm_enterprise (id,enterprise_code,name,type,status,tenant_id,deleted) VALUES (?,?,?,'OWNED_COMPANY','ENABLE',1,0)",
                    ++id, "REG-" + id, name);
        }
        var importer = new cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.RegistrationCertificateWorkbookImportService(
                jdbcTemplate, service);
        var first = importer.importFile(file, 99L);
        assertEquals(java.util.Map.of("国内注册证",119,"CE证书",43,"FDA证书",19), first.sheetCounts());
        assertEquals(181, first.inserted());
        assertEquals(181, importer.importFile(file,99L).existing());
        assertEquals(181, countWhere("dcc_registration_certificate_snapshot",
                "entrusted_production IS NULL AND self_production IS NULL"));
        var customerRow = entries.stream().filter(e -> e.sheet().equals("FDA证书") && e.row() == 12)
                .findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, customerRow.facts().getCustomerHeld());
        assertEquals(LocalDate.of(2026, 7, 7), customerRow.facts().getApprovalDate());
        assertEquals("上海瑛泰医疗器械股份有限公司", customerRow.company());
        assertEquals(0, count("dcc_registration_certificate_file"));
    }

    @Test
    void workbookImportsMultipleTenantCompaniesWithoutIndividualCompanyGrants() throws Exception {
        ensureJdbcTemplate();
        jdbcTemplate.update("INSERT INTO mdm_enterprise (id,enterprise_code,name,type,status,tenant_id,deleted) VALUES "
                + "(80001,'IMPORT-A','导入公司甲','OWNED_COMPANY','ENABLE',1,0),"
                + "(80002,'IMPORT-B','导入公司乙','OWNED_COMPANY','ENABLE',1,0)");
        var importer = new cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.RegistrationCertificateWorkbookImportService(
                jdbcTemplate, service);
        var file = workbookFile("导入公司甲", "导入公司乙");

        var result = importer.importFile(file, 99L);

        assertEquals(2, result.inserted());
        assertEquals(2, count("dcc_registration_certificate"));
        assertEquals(2, countWhere("dcc_registration_certificate_audit", "actor_id = 99 AND result = 'SUCCESS'"));
        assertEquals(2, importer.importFile(file, 99L).existing());
        assertEquals(2, count("dcc_registration_certificate"));
    }

    @Test
    void workbookRejectsMissingDisabledForeignTenantAndAmbiguousCompaniesBeforeWriting() throws Exception {
        ensureJdbcTemplate();
        jdbcTemplate.update("INSERT INTO mdm_enterprise (id,enterprise_code,name,type,status,tenant_id,deleted) VALUES "
                + "(80001,'IMPORT-A','导入公司甲','OWNED_COMPANY','ENABLE',1,0),"
                + "(80002,'IMPORT-B','停用公司','OWNED_COMPANY','DISABLE',1,0),"
                + "(80003,'IMPORT-C','其他租户公司','OWNED_COMPANY','ENABLE',2,0),"
                + "(80004,'IMPORT-D','重名公司','OWNED_COMPANY','ENABLE',1,0),"
                + "(80005,'IMPORT-E','重名公司','OWNED_COMPANY','ENABLE',1,0),"
                + "(80006,'IMPORT-F','供应商','SUPPLIER','ENABLE',1,0),"
                + "(80007,'IMPORT-G','已删除公司','OWNED_COMPANY','ENABLE',1,1)");
        var importer = new cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport.RegistrationCertificateWorkbookImportService(
                jdbcTemplate, service);
        for (String company : List.of("不存在公司", "停用公司", "其他租户公司", "重名公司", "供应商", "已删除公司")) {
            var file = workbookFile("导入公司甲", company);
            var failure = assertThrows(ServiceException.class, () -> importer.importFile(file, 99L));
            org.junit.jupiter.api.Assertions.assertTrue(failure.getMessage().contains("所属公司须唯一匹配已启用的自有企业"));
            assertEquals(0, count("dcc_registration_certificate"));
            assertEquals(0, count("dcc_registration_certificate_audit"));
        }
    }

    private org.springframework.mock.web.MockMultipartFile workbookFile(String... companies) throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var output = new java.io.ByteArrayOutputStream()) {
            var domestic = workbook.createSheet("国内注册证");
            workbookRow(domestic, 0, "公司名称", "项目代码", "产品名称", "注册证号", "首次获证日期", "生效日期", "有效期至", "类别", "备注");
            for (int i = 0; i < companies.length; i++) {
                workbookRow(domestic, i + 1, companies[i], "", "导入测试产品", "测试注册证" + i,
                        "2020-01-01", "2026-01-01", "2031-01-01", "二类", "");
            }
            var ce = workbook.createSheet("CE证书");
            workbookRow(ce, 0, "No", "公司名称 Company Name", "Product Name (English)", "Trademark",
                    "Product Name (Chinese)", "MDR Certificate", "", "", "", "", "", "MDD Certificate",
                    "", "", "", "", "", "", "CE Marker");
            workbookRow(ce, 1, "", "", "", "", "", "Class", "", "Certificate No", "Issue Date", "Expiry Date", "EMDN Code", "Class");
            workbookRow(ce, 2, "", "", "", "", "", "", "", "", "", "", "", "", "Certificate No",
                    "Initial Issue Date", "Expiry Date", "Extension Letter No", "Issue Date", "Expiry Date");
            workbookRow(workbook.createSheet("FDA证书"), 0, "K holder", "No.", "K number", "Listing number",
                    "Product name in EN", "Product name in CN", "Product code", "Class", "Regulation number",
                    "Cleared /exempt listing date", "File No.", "Listing status", "Remark");
            workbook.write(output);
            return new org.springframework.mock.web.MockMultipartFile("file", "注册证导入测试.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private void workbookRow(org.apache.poi.ss.usermodel.Sheet sheet, int index, String... values) {
        var row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
    }

    private BatchCommand batch(RowCommand... rows) {
        return new BatchCommand(1L, 99L,
                "D42162DC354E8976CED450FA8A2BB00A2AB6099EDDF19AB907FEC3366EF94FF4",
                "trace-t09-b", List.of(rows));
    }

    private RowCommand row(int sourceRow, Long ownerCompanyId, Long productMasterId, Long projectCodeId,
                           List<String> restrictedReasons) {
        return new RowCommand(sourceRow, ownerCompanyId, productMasterId, projectCodeId,
                "一次性使用造影导管", "上海瑛泰医疗器械股份有限公司",
                "沪械注准20262030001-" + sourceRow,
                LocalDate.of(2020, 1, 2), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1), LocalDate.of(2031, 1, 1),
                "二类", "", "", "", "", "", "",
                false, true, List.of(),
                restrictedReasons);
    }

    private RowCommand futureRow(int sourceRow) {
        return new RowCommand(sourceRow, 10L, 20L, null,
                "一次性使用造影导管", "上海瑛泰医疗器械股份有限公司",
                "沪械注准20262030001-" + sourceRow,
                LocalDate.of(2020, 1, 2), LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1), LocalDate.of(2031, 9, 1),
                "二类", "", "", "", "", "", "",
                false, true, List.of(),
                List.of("MISSING_PROJECT_CODE", "NO_ATTACHMENT_POLICY"));
    }

    private void ensureJdbcTemplate() {
        if (jdbcTemplate == null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private int countWhere(String tableName, String where) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + where, Integer.class);
    }

    @TestConfiguration
    static class ClockConfiguration {
        @Bean
        DccRegistrationCertificateBusinessClock registrationCertificateBusinessClock() {
            return new DccRegistrationCertificateBusinessClock(
                    Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"),
                            ZoneId.of("Asia/Shanghai")));
        }
    }
}
