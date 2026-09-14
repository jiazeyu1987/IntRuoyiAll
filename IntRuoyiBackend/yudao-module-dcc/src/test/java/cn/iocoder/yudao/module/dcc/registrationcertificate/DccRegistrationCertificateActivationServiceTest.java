package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.activation.DccRegistrationCertificateActivationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateBusinessEventNotifier;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRegistrationSyncService;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Import({
        DccRegistrationCertificateActivationService.class,
        DccRegistrationCertificateActivationServiceTest.DbTestConfiguration.class
})
class DccRegistrationCertificateActivationServiceTest extends BaseDbUnitTest {

    @Resource
    private DccRegistrationCertificateActivationService service;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;
    @Resource
    private DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    @Autowired
    private DccRegistrationCertificateFileMapper fileMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ControlledContentRegistrationProjectionService projectionService;
    @MockitoBean
    private DccRegistrationCertificateBusinessEventNotifier businessEventNotifier;
    @MockitoBean
    private DccProductCatalogRegistrationSyncService productCatalogRegistrationSyncService;

    @Test
    void duePendingCandidateSwitchesOnceToCurrentAndOld() {
        PendingFixture fixture = seedPendingCandidate(LocalDate.of(2026, 8, 17));

        DccRegistrationCertificateActivationResult result = activateOrFail(command(fixture, "activation-1"));

        assertEquals(fixture.certificateId(), result.certificateId());
        assertEquals(fixture.currentVersionId(), result.oldVersionId());
        assertEquals(fixture.pendingVersionId(), result.currentVersionId());
        assertEquals(fixture.pendingSnapshotId(), result.currentSnapshotId());
        assertTrue(result.activated());
        assertCurrentOldSwitch(fixture, fixture.pendingSnapshotId());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'ACTIVATION_APPLIED'", fixture.certificateId()));
        verify(projectionService).publish(any(), any(), any(), eq("OLD"), eq("CURRENT"), eq(99L), anyString());
        verify(businessEventNotifier).notifyRenewalCandidateActivated(
                eq(1L), eq(10L), eq(fixture.certificateId()), eq(fixture.pendingVersionId()),
                eq(99L), eq("activation-1"), eq("Product A"), eq("CERT-002"),
                eq(LocalDate.of(2026, 8, 17)), eq(LocalDate.of(2031, 9, 1)));
    }

    @Test
    void activationReplaysOrderedWaitingPeriodChangesIntoCandidateSnapshot() {
        PendingFixture fixture = seedPendingCandidate(LocalDate.of(2026, 8, 17));
        Long changedSnapshotId = seedChangedCurrentSnapshot(fixture.currentVersionId(), "Product B", 2);
        insertLifecycleEvent(fixture.certificateId(), fixture.currentVersionId(), fixture.currentVersionId(),
                fixture.currentSnapshotId(), changedSnapshotId, "change-1", "CHANGE_APPLIED", 2, 2);

        DccRegistrationCertificateActivationResult result = activateOrFail(command(fixture, "activation-replay"));

        DccRegistrationCertificateSnapshotDO replayed = snapshotMapper.selectById(result.currentSnapshotId());
        assertEquals("Product B", replayed.getProductName());
        assertEquals("Registrant", replayed.getRegistrantName());
        DccRegistrationCertificateVersionDO renewal = versionMapper.selectById(result.currentVersionId());
        assertEquals("CERT-002", renewal.getCertificateNo());
        assertEquals("III", renewal.getClassification());
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_activation_replay WHERE tenant_id = 1 AND certificate_id = ?", fixture.certificateId()));
        assertCurrentOldSwitch(fixture, result.currentSnapshotId());
    }

    @Test
    void activationIgnoresChangesAlreadyAppliedBeforeRenewalUpload() {
        PendingFixture fixture = seedPendingCandidate(LocalDate.of(2026, 8, 17));
        Long changedSnapshotId = seedChangedCurrentSnapshot(fixture.currentVersionId(), "Product Before Renewal", 2);
        assertEquals(1, certificateMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, fixture.certificateId())
                        .set(DccRegistrationCertificateDO::getCurrentSnapshotId, changedSnapshotId)));
        assertEquals(1, versionMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getId, fixture.pendingVersionId())
                        .set(DccRegistrationCertificateVersionDO::getBaseSnapshotId, changedSnapshotId)));
        assertEquals(1, jdbcTemplate.update("""
                UPDATE dcc_registration_certificate_lifecycle_event
                   SET event_sequence = 2,
                       source_snapshot_id = ?,
                       baseline_snapshot_revision = 2
                 WHERE tenant_id = 1 AND certificate_id = ? AND event_key = 'renewal-upload'
                """, changedSnapshotId, fixture.certificateId()));
        insertLifecycleEvent(fixture.certificateId(), fixture.currentVersionId(), fixture.currentVersionId(),
                fixture.currentSnapshotId(), changedSnapshotId, "change-before-renewal", "CHANGE_APPLIED", 1, 2);

        DccRegistrationCertificateActivationResult result = activateOrFail(command(fixture, "activation-after-change"));

        assertEquals(fixture.pendingSnapshotId(), result.currentSnapshotId());
        assertCurrentOldSwitch(fixture, fixture.pendingSnapshotId());
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_activation_replay WHERE tenant_id = 1 AND certificate_id = ?", fixture.certificateId()));
    }

    @Test
    void activationBlocksOutOfOrderReplayWithoutPartialSwitch() {
        PendingFixture fixture = seedPendingCandidate(LocalDate.of(2026, 8, 17));
        Long changedSnapshotId = seedChangedCurrentSnapshot(fixture.currentVersionId(), "Product B", 2);
        insertLifecycleEvent(fixture.certificateId(), fixture.currentVersionId(), fixture.currentVersionId(),
                fixture.currentSnapshotId(), changedSnapshotId, "change-gap", "CHANGE_APPLIED", 3, 2);

        ServiceException error = assertServiceException(() -> service.activateDueCandidate(command(fixture, "activation-gap")));

        assertEquals(REGISTRATION_CERTIFICATE_ACTIVATION_REPLAY_INCOMPLETE.getCode(), error.getCode());
        assertPendingStillOpen(fixture);
        assertEquals(0, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND event_type = 'ACTIVATION_APPLIED'"));
    }

    @Test
    void futureCandidateDoesNotActivateBeforeBusinessDate() {
        PendingFixture fixture = seedPendingCandidate(LocalDate.of(2026, 9, 1));

        ServiceException error = assertServiceException(() -> service.activateDueCandidate(command(fixture, "activation-future")));

        assertEquals(REGISTRATION_CERTIFICATE_ACTIVATION_BASE_CONFLICT.getCode(), error.getCode());
        assertPendingStillOpen(fixture);
    }

    @Test
    void concurrentActivationAttemptsDoNotCreateSecondCurrentOrSecondEvent() throws Exception {
        PendingFixture fixture = seedPendingCandidate(LocalDate.of(2026, 8, 17));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Object>> calls = List.of(
                    () -> outcome(() -> service.activateDueCandidate(command(fixture, "activation-race-1"))),
                    () -> outcome(() -> service.activateDueCandidate(command(fixture, "activation-race-2"))));
            List<Future<Object>> futures = pool.invokeAll(calls);
            int successes = 0;
            for (Future<Object> future : futures) {
                Object outcome = future.get();
                if (outcome instanceof DccRegistrationCertificateActivationResult) {
                    successes++;
                }
            }
            assertTrue(successes >= 1, "at least one concurrent activation should observe the activated state");
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_version WHERE tenant_id = 1 AND certificate_id = ? AND status = 'CURRENT'", fixture.certificateId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_version WHERE tenant_id = 1 AND certificate_id = ? AND status = 'OLD'", fixture.certificateId()));
        assertEquals(1, count("SELECT COUNT(*) FROM dcc_registration_certificate_lifecycle_event WHERE tenant_id = 1 AND certificate_id = ? AND event_type = 'ACTIVATION_APPLIED'", fixture.certificateId()));
    }

    private DccRegistrationCertificateActivationResult activateOrFail(DccRegistrationCertificateActivationCommand command) {
        try {
            return service.activateDueCandidate(command);
        } catch (RuntimeException exception) {
            fail("activation should switch the due candidate atomically, but failed with " + exception);
            throw exception;
        }
    }

    private static ServiceException assertServiceException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ServiceException exception) {
            return exception;
        } catch (RuntimeException exception) {
            fail("expected stable ServiceException, but got " + exception);
        }
        fail("expected stable ServiceException");
        return null;
    }

    private static Object outcome(Callable<DccRegistrationCertificateActivationResult> call) throws Exception {
        try {
            return call.call();
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private DccRegistrationCertificateActivationCommand command(PendingFixture fixture, String key) {
        return new DccRegistrationCertificateActivationCommand(
                1L, 99L, key, "trace-" + key, fixture.certificateId(), 2,
                fixture.currentVersionId(), fixture.pendingVersionId());
    }

    private PendingFixture seedPendingCandidate(LocalDate effectiveDate) {
        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(10L)
                .productMasterId(20L)
                .projectCodeId(40L)
                .firstObtainedDate(LocalDate.of(2021, 1, 1))
                .status("ACTIVE")
                .rowVersion(2)
                .build();
        certificate.setTenantId(1L);
        assertEquals(1, certificateMapper.insert(certificate));

        DccRegistrationCertificateVersionDO current = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo("CERT-001")
                .approvalDate(LocalDate.of(2021, 2, 1))
                .effectiveDate(LocalDate.of(2021, 3, 1))
                .expiryDate(LocalDate.of(2026, 8, 20))
                .classification("II")
                .categoryChanged(false)
                .status("CURRENT")
                .formalizedAt(LocalDateTime.of(2021, 3, 1, 9, 0))
                .formalizedBy(88L)
                .build();
        current.setTenantId(1L);
        assertEquals(1, versionMapper.insert(current));

        DccRegistrationCertificateSnapshotDO currentSnapshot = snapshot(current.getId(), "Product A", 1);
        assertEquals(1, snapshotMapper.insert(currentSnapshot));
        seedEntrusted(currentSnapshot.getId());

        DccRegistrationCertificateVersionDO pending = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(2)
                .versionType("RENEWAL_CERTIFICATE")
                .certificateNo("CERT-002")
                .approvalDate(LocalDate.of(2026, 8, 1))
                .effectiveDate(effectiveDate)
                .expiryDate(LocalDate.of(2031, 9, 1))
                .classification("III")
                .categoryChanged(true)
                .baseSnapshotId(currentSnapshot.getId())
                .status("PENDING_EFFECTIVE")
                .formalizedAt(LocalDateTime.of(2026, 8, 17, 9, 0))
                .formalizedBy(99L)
                .build();
        pending.setTenantId(1L);
        assertEquals(1, versionMapper.insert(pending));

        DccRegistrationCertificateSnapshotDO pendingSnapshot = snapshot(pending.getId(), "Product A", 1);
        assertEquals(1, snapshotMapper.insert(pendingSnapshot));
        seedEntrusted(pendingSnapshot.getId());

        DccRegistrationCertificateFileDO file = DccRegistrationCertificateFileDO.builder()
                .ownerType("VERSION")
                .ownerId(pending.getId())
                .fileKind("REGISTRATION_CERTIFICATE")
                .infraFileId(8001L)
                .originalName("renewal.pdf")
                .mimeType("application/pdf")
                .fileSize(256L)
                .sha256("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")
                .status("BOUND")
                .boundAt(LocalDateTime.of(2026, 8, 17, 9, 0))
                .boundBy(99L)
                .build();
        file.setTenantId(1L);
        assertEquals(1, fileMapper.insert(file));

        certificate.setCurrentVersionId(current.getId());
        certificate.setPendingVersionId(pending.getId());
        certificate.setCurrentSnapshotId(currentSnapshot.getId());
        assertEquals(1, certificateMapper.updateById(certificate));
        insertLifecycleEvent(certificate.getId(), current.getId(), pending.getId(),
                currentSnapshot.getId(), pendingSnapshot.getId(), "renewal-upload", "RENEWAL_UPLOADED", 1, 1);
        return new PendingFixture(certificate.getId(), current.getId(), currentSnapshot.getId(),
                pending.getId(), pendingSnapshot.getId());
    }

    private DccRegistrationCertificateSnapshotDO snapshot(Long versionId, String productName, int revision) {
        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(versionId)
                .revisionNo(revision)
                .productName(productName)
                .registrantName("Registrant")
                .modelSpecification("Model")
                .structureComposition("Structure")
                .intendedUse("Use")
                .technicalRequirements("Requirements")
                .residenceAddress("Residence")
                .productionAddress("Production")
                .entrustedProduction(true)
                .selfProduction(false)
                .entrustedEnterprisesJson("[{\"enterpriseId\":30,\"enterpriseName\":\"Factory A\"}]")
                .effectiveAt(LocalDateTime.of(2026, 8, 17, 0, 0))
                .build();
        snapshot.setTenantId(1L);
        return snapshot;
    }

    private void seedEntrusted(Long snapshotId) {
        DccRegistrationCertificateSnapshotEntrustedDO entrusted = DccRegistrationCertificateSnapshotEntrustedDO.builder()
                .snapshotId(snapshotId)
                .enterpriseId(30L)
                .enterpriseNameSnapshot("Factory A")
                .sortOrder(1)
                .build();
        entrusted.setTenantId(1L);
        assertEquals(1, entrustedMapper.insert(entrusted));
    }

    private Long seedChangedCurrentSnapshot(Long currentVersionId, String productName, int revision) {
        DccRegistrationCertificateSnapshotDO changed = snapshot(currentVersionId, productName, revision);
        assertEquals(1, snapshotMapper.insert(changed));
        seedEntrusted(changed.getId());
        return changed.getId();
    }

    private void insertLifecycleEvent(Long certificateId, Long sourceVersionId, Long targetVersionId,
                                      Long sourceSnapshotId, Long targetSnapshotId, String eventKey,
                                      String eventType, int sequence, Integer targetRevision) {
        jdbcTemplate.update("""
                INSERT INTO dcc_registration_certificate_lifecycle_event
                  (tenant_id, owner_company_id, certificate_id, source_version_id, target_version_id,
                   source_snapshot_id, target_snapshot_id, event_key, event_type, event_sequence,
                   baseline_row_version, baseline_snapshot_revision, actor_id, detail_json, occurred_at, creator)
                VALUES (1, 10, ?, ?, ?, ?, ?, ?, ?, ?, 2, ?, 99, '{}', ?, '99')
                """, certificateId, sourceVersionId, targetVersionId, sourceSnapshotId, targetSnapshotId,
                eventKey, eventType, sequence, targetRevision, LocalDateTime.of(2026, 8, 17, 9, sequence));
    }

    private void assertCurrentOldSwitch(PendingFixture fixture, Long expectedSnapshotId) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(fixture.certificateId());
        assertEquals("ACTIVE", certificate.getStatus());
        assertEquals(fixture.pendingVersionId(), certificate.getCurrentVersionId());
        assertEquals(null, certificate.getPendingVersionId());
        assertEquals(expectedSnapshotId, certificate.getCurrentSnapshotId());
        assertEquals("OLD", versionMapper.selectById(fixture.currentVersionId()).getStatus());
        assertEquals("CURRENT", versionMapper.selectById(fixture.pendingVersionId()).getStatus());
        assertEquals(LocalDate.of(2026, 8, 20), versionMapper.selectById(fixture.currentVersionId()).getExpiryDate());
    }

    private void assertPendingStillOpen(PendingFixture fixture) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(fixture.certificateId());
        assertEquals(fixture.currentVersionId(), certificate.getCurrentVersionId());
        assertEquals(fixture.pendingVersionId(), certificate.getPendingVersionId());
        assertEquals("CURRENT", versionMapper.selectById(fixture.currentVersionId()).getStatus());
        assertEquals("PENDING_EFFECTIVE", versionMapper.selectById(fixture.pendingVersionId()).getStatus());
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private record PendingFixture(Long certificateId, Long currentVersionId, Long currentSnapshotId,
                                  Long pendingVersionId, Long pendingSnapshotId) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DbTestConfiguration {

        @Bean
        DccRegistrationCertificateBusinessClock registrationCertificateBusinessClock() {
            return new DccRegistrationCertificateBusinessClock(
                    Clock.fixed(Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("Asia/Shanghai")));
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }
}
