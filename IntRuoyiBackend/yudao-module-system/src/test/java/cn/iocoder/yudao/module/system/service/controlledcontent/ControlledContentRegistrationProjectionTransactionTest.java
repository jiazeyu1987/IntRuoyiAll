package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import jakarta.annotation.Resource;
import org.h2.api.Trigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.READY_TO_PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.SUPERSEDED;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import({ControlledContentRegistrationProjectionService.class, ControlledContentStateMachine.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts = "/sql/controlledcontent/create_registration_projection_tables.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/controlledcontent/drop_registration_projection_tables.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ControlledContentRegistrationProjectionTransactionTest extends BaseDbUnitTest {

    private static final ControlledContentKey KEY = ControlledContentKey.of(
            1L, DCC_REGISTRATION_CERTIFICATE, "REG-TX-1");

    @Resource
    private ControlledContentRegistrationProjectionService service;
    @Resource
    private ControlledContentVersionRefMapper versionRefMapper;
    @Resource
    private ControlledContentTransitionAuditMapper transitionAuditMapper;

    @Test
    void registerReadyCandidate_whenProjectionIsEmpty_commitsCandidateOnlyWithExactAudit() {
        ControlledContentVersionRefDO result = assertDoesNotThrow(() -> service.registerReadyCandidate(
                KEY, snapshot(null, null), snapshot(null, 200L),
                9001L, 200L, "V1", "READY_TO_PUBLISH", 501L, "FUTURE_INITIAL"));

        ControlledContentVersionRefDO persisted = versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L);
        assertEquals(READY_TO_PUBLISH.name(), persisted.getCanonicalStatus());
        assertNull(persisted.getSourceVersionRefId());
        assertNull(persisted.getSourceNativeVersionId());
        List<ControlledContentTransitionAuditDO> audits = selectTransitions();
        assertEquals(1, audits.size());
        assertEquals(result.getId(), audits.get(0).getVersionRefId());
        assertNull(audits.get(0).getFromStatus());
        assertEquals(READY_TO_PUBLISH.name(), audits.get(0).getToStatus());
        assertEquals("REGISTER_READY_CANDIDATE", audits.get(0).getAction());
    }

    @Test
    void registerReadyCandidate_whenEmptySnapshotHidesHistoricalRef_rejectsWithoutWrites() {
        seedHistoricalProjection();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.registerReadyCandidate(KEY, snapshot(null, null), snapshot(null, 200L),
                        9001L, 200L, "V1", "READY_TO_PUBLISH", 501L, "FUTURE_INITIAL"));

        assertTrue(exception.getMessage().contains("genuinely empty"));
        assertNull(versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L));
        assertEquals(1L, transitionAuditMapper.countTransitions(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1"));
    }

    @Test
    void publish_whenOnlyCandidateExists_commitsFirstActiveWithoutSupersedeAudit() {
        seedCandidateOnlyProjection();

        assertDoesNotThrow(() -> service.publish(KEY, snapshot(null, 200L), snapshot(200L, null),
                null, "ACTIVE", 501L, "FIRST_PUBLICATION"));

        ControlledContentVersionRefDO persisted = versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L);
        assertEquals(ACTIVE.name(), persisted.getCanonicalStatus());
        List<ControlledContentTransitionAuditDO> audits = selectTransitions();
        assertEquals(2, audits.size());
        assertEquals(List.of("REGISTER_READY_CANDIDATE", "PUBLISH"), audits.stream()
                .map(ControlledContentTransitionAuditDO::getAction).toList());
    }

    @Test
    void publish_whenCandidateSnapshotHidesHistoricalRef_rejectsWithoutWrites() {
        seedCandidateWithHistoricalProjection();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.publish(KEY, snapshot(null, 200L), snapshot(200L, null),
                        null, "ACTIVE", 501L, "FIRST_PUBLICATION"));

        assertTrue(exception.getMessage().contains("first publication"));
        assertEquals(READY_TO_PUBLISH.name(), versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L).getCanonicalStatus());
        assertEquals(2L, transitionAuditMapper.countTransitions(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1"));
    }

    @ParameterizedTest(name = "sourceVersionRefId={0}, sourceNativeVersionId={1}")
    @MethodSource("candidateOnlySourceFields")
    void publish_whenCandidateOnlyHasAnySourcePredecessor_rejectsWithoutWrites(
            Long sourceVersionRefId, Long sourceNativeVersionId) {
        ControlledContentVersionRefDO candidate = ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1);
        candidate.setSourceVersionRefId(sourceVersionRefId);
        candidate.setSourceNativeVersionId(sourceNativeVersionId);
        versionRefMapper.insert(candidate);
        transitionAuditMapper.insert(audit(22L, 12L, READY_TO_PUBLISH.name(),
                "REGISTER_READY_CANDIDATE", "SEED"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.publish(KEY, snapshot(null, 200L), snapshot(200L, null),
                        null, "ACTIVE", 501L, "FIRST_PUBLICATION"));

        assertTrue(exception.getMessage().contains("first publication"));
        assertEquals(READY_TO_PUBLISH.name(), versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L).getCanonicalStatus());
        assertEquals(1L, transitionAuditMapper.countTransitions(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1"));
    }

    @Test
    void publish_whenAllWritesSucceed_commitsCompleteSwitchAndTwoAudits() {
        seedReadyProjection();

        assertDoesNotThrow(() -> service.publish(KEY, snapshot(100L, 200L), snapshot(200L, null),
                "SUPERSEDED", "ACTIVE", 501L, "SUCCESS"));

        assertEquals("SUPERSEDED", versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 100L).getCanonicalStatus());
        assertEquals("ACTIVE", versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L).getCanonicalStatus());
        assertEquals(4L, transitionAuditMapper.countTransitions(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1"));
    }

    @Test
    void publish_whenSecondAuditWriteFails_rollsBackRefsAndEarlierAudit() {
        seedReadyProjection();

        assertThrows(DataIntegrityViolationException.class,
                () -> service.publish(KEY, snapshot(100L, 200L), snapshot(200L, null),
                        "SUPERSEDED", "ACTIVE", 501L, "FORCE_ROLLBACK"));

        assertEquals("ACTIVE", versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 100L).getCanonicalStatus());
        assertEquals("READY_TO_PUBLISH", versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L).getCanonicalStatus());
        assertEquals(2L, transitionAuditMapper.countTransitions(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1"));
    }

    @Test
    void publish_whenPostWriteProjectionDrifts_rollsBackRefsAndAudits() {
        seedReadyProjection();
        PostWriteProjectionDriftTrigger.enabled = true;

        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> service.publish(KEY, snapshot(100L, 200L), snapshot(200L, null),
                            "SUPERSEDED", "ACTIVE", 501L, "POST_WRITE_DRIFT"));
            assertTrue(exception.getMessage().contains("domainAfter"));
        } finally {
            PostWriteProjectionDriftTrigger.enabled = false;
        }

        assertEquals("ACTIVE", versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 100L).getCanonicalStatus());
        assertEquals("READY_TO_PUBLISH", versionRefMapper.selectByNativeVersion(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1", 200L).getCanonicalStatus());
        assertEquals(2L, transitionAuditMapper.countTransitions(
                1L, DCC_REGISTRATION_CERTIFICATE.name(), "REG-TX-1"));
    }

    public static final class PostWriteProjectionDriftTrigger implements Trigger {

        private static volatile boolean enabled;

        @Override
        public void init(Connection connection, String schemaName, String triggerName, String tableName,
                         boolean before, int type) {
            // No initialization is required for this deterministic test trigger.
        }

        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) throws SQLException {
            if (enabled && READY_TO_PUBLISH.name().equals(oldRow[7]) && ACTIVE.name().equals(newRow[7])) {
                newRow[5] = 201L;
            }
        }

        @Override
        public void close() {
        }

        @Override
        public void remove() {
        }
    }

    private void seedReadyProjection() {
        versionRefMapper.insert(ref(11L, 100L, ACTIVE.name(), 1, null));
        versionRefMapper.insert(ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1));
        transitionAuditMapper.insert(audit(21L, 11L, ACTIVE.name(), "REGISTER_ACTIVE", "SEED"));
        transitionAuditMapper.insert(audit(22L, 12L, READY_TO_PUBLISH.name(),
                "REGISTER_READY_CANDIDATE", "SEED"));
    }

    private void seedHistoricalProjection() {
        versionRefMapper.insert(ref(11L, 100L, SUPERSEDED.name(), null, null));
        transitionAuditMapper.insert(audit(21L, 11L, SUPERSEDED.name(), "SUPERSEDE_ACTIVE", "SEED"));
    }

    private void seedCandidateWithHistoricalProjection() {
        seedHistoricalProjection();
        versionRefMapper.insert(ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1));
        transitionAuditMapper.insert(audit(22L, 12L, READY_TO_PUBLISH.name(),
                "REGISTER_READY_CANDIDATE", "SEED"));
    }

    private void seedCandidateOnlyProjection() {
        versionRefMapper.insert(ref(12L, 200L, READY_TO_PUBLISH.name(), null, 1));
        transitionAuditMapper.insert(audit(22L, 12L, READY_TO_PUBLISH.name(),
                "REGISTER_READY_CANDIDATE", "SEED"));
    }

    private List<ControlledContentTransitionAuditDO> selectTransitions() {
        return transitionAuditMapper.selectList(new LambdaQueryWrapperX<ControlledContentTransitionAuditDO>()
                .eq(ControlledContentTransitionAuditDO::getTenantId, 1L)
                .eq(ControlledContentTransitionAuditDO::getContentType, DCC_REGISTRATION_CERTIFICATE.name())
                .eq(ControlledContentTransitionAuditDO::getContentKey, "REG-TX-1")
                .orderByAsc(ControlledContentTransitionAuditDO::getId));
    }

    private static Stream<Arguments> candidateOnlySourceFields() {
        return Stream.of(
                Arguments.of(11L, null),
                Arguments.of(null, 100L));
    }

    private ControlledContentProjectionSnapshot snapshot(Long activeNativeVersionId, Long candidateNativeVersionId) {
        return ControlledContentProjectionSnapshot.of(KEY, activeNativeVersionId, candidateNativeVersionId);
    }

    private ControlledContentVersionRefDO ref(Long id, Long nativeVersionId, String status,
                                              Integer activeFlag, Integer candidateFlag) {
        return ControlledContentVersionRefDO.builder()
                .id(id)
                .tenantId(1L)
                .contentType(DCC_REGISTRATION_CERTIFICATE.name())
                .contentKey("REG-TX-1")
                .nativeMasterId(9001L)
                .nativeVersionId(nativeVersionId)
                .versionNo("V" + nativeVersionId)
                .canonicalStatus(status)
                .domainStatus(status)
                .activeUniqueFlag(activeFlag)
                .openCandidateUniqueFlag(candidateFlag)
                .build();
    }

    private ControlledContentTransitionAuditDO audit(Long id, Long refId, String toStatus,
                                                     String action, String reason) {
        return ControlledContentTransitionAuditDO.builder()
                .id(id)
                .tenantId(1L)
                .versionRefId(refId)
                .contentType(DCC_REGISTRATION_CERTIFICATE.name())
                .contentKey("REG-TX-1")
                .toStatus(toStatus)
                .domainToStatus(toStatus)
                .action(action)
                .actorId(501L)
                .reason(reason)
                .build();
    }

}
