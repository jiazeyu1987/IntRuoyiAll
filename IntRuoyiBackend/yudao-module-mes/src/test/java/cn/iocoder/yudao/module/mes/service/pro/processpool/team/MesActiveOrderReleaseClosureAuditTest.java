package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.alibaba.fastjson.JSON;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Deliberately separate from Helmholtz's active-order maintenance tests. */
class MesActiveOrderReleaseClosureAuditTest {
    @Test
    void closureAuditUsesCanonicalPersistedBeforeAndAfterIncludingReleaseFields() {
        var f = new Fixture(false);
        f.service.closeForRelease(11L, 3, 81L, 20L, "parent-signature");
        assertEquals("CLOSED", f.read().getActiveStatus());
        var event = f.events.get(0);
        assertEquals("mes.active-order.close-by-release", event.getOperationId());
        var before = assertDoesNotThrow(() -> JSON.parseObject(event.getBeforeState().getCanonicalJson()));
        var after = assertDoesNotThrow(() -> JSON.parseObject(event.getAfterState().getCanonicalJson()),
                "afterState must be JSON from the persisted row, not comma-separated intended changes");
        assertEquals("ACTIVE", before.getString("activeStatus"));
        assertEquals(3, before.getIntValue("version"));
        assertNull(before.getLong("releaseDecisionId"));
        assertEquals("CLOSED", after.getString("activeStatus"));
        assertEquals("RELEASED", after.getString("businessStatus"));
        assertEquals(4, after.getIntValue("version"));
        assertEquals(81L, after.getLong("releaseDecisionId"));
        assertEquals(20L, after.getLong("releasedBy"));
        assertEquals(JSON.parseObject(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(f.read()))
                        .get("releasedAt"), after.get("releasedAt"),
                "must reread actual persisted timestamp instead of reconstructing it");
        assertEquals("parent-signature", event.getSignatureRecordId());
        assertEquals(1, f.jdbc.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class));
    }

    @Test
    void closureAuditFailureRollsBackActiveOrderVersionAndSpecializedAudit() {
        var f = new Fixture(true);
        var error = assertThrows(IllegalStateException.class,
                () -> f.service.closeForRelease(11L, 3, 81L, 20L, "parent-signature"));
        assertEquals("closure audit failed", error.getMessage());
        assertEquals("ACTIVE", f.read().getActiveStatus());
        assertEquals(3, f.read().getVersion());
        assertNull(f.read().getReleaseDecisionId());
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class));
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM specialized_audit", Integer.class));
    }

    @Test
    void staleClosureVersionDoesNotWriteOrAppend() {
        var f = new Fixture(false);
        assertThrows(RuntimeException.class, () -> f.service.closeForRelease(11L, 2, 81L, 20L, "signature"));
        assertEquals(3, f.read().getVersion());
        assertTrue(f.events.isEmpty());
        assertEquals(0, f.jdbc.queryForObject("SELECT COUNT(*) FROM specialized_audit", Integer.class));
    }

    private static final class Fixture {
        final JdbcTemplate jdbc;
        final MesTeamLeaderActiveOrderService service;
        final List<GxpAuditCommand> events = new ArrayList<>();

        Fixture(boolean failAudit) {
            var ds = new JdbcDataSource();
            ds.setURL("jdbc:h2:mem:release_closure_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
            jdbc = new JdbcTemplate(ds);
            jdbc.execute("CREATE TABLE mes_pro_process_pool_active_order (id BIGINT PRIMARY KEY, work_order_id BIGINT, active_status VARCHAR(32), business_status VARCHAR(32), version INT, release_decision_id BIGINT, released_by BIGINT, released_at TIMESTAMP)");
            jdbc.execute("CREATE TABLE audit_event (id INT PRIMARY KEY)");
            jdbc.execute("CREATE TABLE specialized_audit (id INT PRIMARY KEY)");
            jdbc.update("INSERT INTO mes_pro_process_pool_active_order (id, work_order_id, active_status, business_status, version) VALUES (11, 90, 'ACTIVE', 'COMPLETED', 3)");
            var mapper = mock(MesProcessPoolActiveOrderMapper.class);
            when(mapper.selectByIdForUpdate(11L)).thenAnswer(invocation -> read());
            when(mapper.selectById(11L)).thenAnswer(invocation -> read());
            when(mapper.closeForRelease(eq(11L), eq(3), eq(81L), eq(20L), any())).thenAnswer(invocation -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                // Fixed DB value intentionally differs from the caller's clock to detect fabricated after state.
                return jdbc.update("UPDATE mes_pro_process_pool_active_order SET active_status='CLOSED', business_status='RELEASED', version=4, release_decision_id=81, released_by=20, released_at=TIMESTAMP '2026-09-29 10:11:12' WHERE id=11 AND version=3");
            });
            var specialized = mock(MesProcessPoolTeamMaintenanceAuditMapper.class);
            when(specialized.insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO.class)))
                    .thenAnswer(invocation -> jdbc.update("INSERT INTO specialized_audit VALUES (1)"));
            var audit = mock(GxpAuditService.class);
            doAnswer(invocation -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                assertEquals("CLOSED", read().getActiveStatus());
                events.add(invocation.getArgument(0));
                jdbc.update("INSERT INTO audit_event VALUES (1)");
                if (failAudit) throw new IllegalStateException("closure audit failed");
                return null;
            }).when(audit).append(any());
            var target = mock(MesTeamLeaderActiveOrderServiceImpl.class, CALLS_REAL_METHODS);
            ReflectionTestUtils.setField(target, "activeOrderMapper", mapper);
            ReflectionTestUtils.setField(target, "auditMapper", specialized);
            ReflectionTestUtils.setField(target, "gxpAuditService", audit);
            var proxy = new ProxyFactory(target);
            proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds),
                    new AnnotationTransactionAttributeSource()));
            service = (MesTeamLeaderActiveOrderService) proxy.getProxy();
        }

        MesProcessPoolActiveOrderDO read() {
            return jdbc.queryForObject("SELECT * FROM mes_pro_process_pool_active_order WHERE id=11", (rs, index) ->
                    new MesProcessPoolActiveOrderDO().setId(rs.getLong("id")).setWorkOrderId(rs.getLong("work_order_id"))
                            .setActiveStatus(rs.getString("active_status")).setBusinessStatus(rs.getString("business_status"))
                            .setVersion(rs.getInt("version")).setReleaseDecisionId(rs.getObject("release_decision_id", Long.class))
                            .setReleasedBy(rs.getObject("released_by", Long.class))
                            .setReleasedAt(rs.getObject("released_at", LocalDateTime.class)));
        }
    }
}
