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
