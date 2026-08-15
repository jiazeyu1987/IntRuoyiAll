package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentTransitionAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentTransitionAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.ControlledContentVersionRefMapper;
import jakarta.annotation.Resource;
import org.h2.api.Trigger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import java.sql.Connection;
import java.sql.SQLException;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus.READY_TO_PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
