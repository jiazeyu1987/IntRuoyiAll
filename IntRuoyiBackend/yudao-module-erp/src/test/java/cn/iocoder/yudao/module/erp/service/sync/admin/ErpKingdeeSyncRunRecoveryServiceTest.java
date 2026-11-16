package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.quartz.JobStoreType;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ErpKingdeeSyncRunRecoveryServiceTest {
    private JdbcTemplate jdbc;
    private QuartzProperties properties;
    private ErpKingdeeSyncRunMapper mapper;
    private ErpKingdeeSyncRunRecoveryService service;
    private org.redisson.api.RLock executionLock;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE erp_kingdee_sync_run (id BIGINT, tenant_id BIGINT, status INT, deleted INT, "
                + "created_count INT, updated_count INT, skipped_count INT, failed_count INT, "
                + "ended_at TIMESTAMP, update_time TIMESTAMP, failure_message VARCHAR(1000))");
        jdbc.execute("CREATE TABLE QRTZ_FIRED_TRIGGERS (JOB_NAME VARCHAR(200), INSTANCE_NAME VARCHAR(200))");
        jdbc.update("INSERT INTO erp_kingdee_sync_run VALUES (7, 9, 10, 0, 200, 30, 70, 0, NULL, NULL, NULL)");
        properties = new QuartzProperties();
        properties.setJobStoreType(JobStoreType.JDBC);
        mapper = mock(ErpKingdeeSyncRunMapper.class);
        when(mapper.selectById(7L)).thenReturn(ErpKingdeeSyncRunDO.builder().id(7L)
                .syncType("PRODUCTION_MATERIAL_LIST").status(10).build());
        org.redisson.api.RedissonClient redisson = mock(org.redisson.api.RedissonClient.class);
        executionLock = mock(org.redisson.api.RLock.class);
        when(redisson.getLock(anyString())).thenReturn(executionLock);
        service = new ErpKingdeeSyncRunRecoveryService(mapper, jdbc, properties, redisson);
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void tearDown() { TenantContextHolder.clear(); }

    @Test
    void orphanIsFailedWithoutChangingAnyCommittedCount() {
        service.recover(7L);
        assertEquals(30, status());
        assertEquals(200, jdbc.queryForObject("SELECT created_count FROM erp_kingdee_sync_run", Integer.class));
        assertEquals(30, jdbc.queryForObject("SELECT updated_count FROM erp_kingdee_sync_run", Integer.class));
        assertEquals(70, jdbc.queryForObject("SELECT skipped_count FROM erp_kingdee_sync_run", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT failed_count FROM erp_kingdee_sync_run", Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT ended_at FROM erp_kingdee_sync_run", java.sql.Timestamp.class));
    }

    @Test
    void executingOnAnotherClusterNodeMustNotBeEnded() {
        jdbc.update("INSERT INTO QRTZ_FIRED_TRIGGERS VALUES (?, ?)", "kingdeeProductionMaterialListSyncJob", "another-node");
        assertThrows(IllegalStateException.class, () -> service.recover(7L));
        assertEquals(10, status());
    }

    @Test
    void unrelatedHandlerDoesNotBlockRecovery() {
        jdbc.update("INSERT INTO QRTZ_FIRED_TRIGGERS VALUES ('kingdeeStockSyncJob', 'another-node')");
        service.recover(7L);
        assertEquals(30, status());
    }

    @Test
    void tenantBoundaryIsEnforcedInStateChangeEvenIfLookupReturnsOtherTenant() {
        TenantContextHolder.setTenantId(10L);
        assertThrows(IllegalStateException.class, () -> service.recover(7L));
        assertEquals(10, status());
    }

    @Test
    void concurrentCompletionCannotBeOverwritten() {
        jdbc.update("UPDATE erp_kingdee_sync_run SET status = 20");
        assertThrows(IllegalStateException.class, () -> service.recover(7L));
        assertEquals(20, status());
    }

    @Test
    void missingRecordAndMissingTenantFailExplicitly() {
        assertThrows(IllegalStateException.class, () -> service.recover(8L));
        TenantContextHolder.clear();
        assertThrows(NullPointerException.class, () -> service.recover(7L));
        assertEquals(10, status());
    }

    @Test
    void memorySchedulerAndUnsafePrefixAreRejected() {
        properties.setJobStoreType(JobStoreType.MEMORY);
        assertThrows(IllegalStateException.class, () -> service.recover(7L));
        properties.setJobStoreType(JobStoreType.JDBC);
        properties.getProperties().put("org.quartz.jobStore.tablePrefix", "schema.QRTZ_");
        assertThrows(IllegalStateException.class, () -> service.recover(7L));
        assertEquals(10, status());
    }

    @Test
    void deletionWaitsForExecutingQuartzJobAndPreservesBusinessCounts() {
        jdbc.update("INSERT INTO QRTZ_FIRED_TRIGGERS VALUES ('kingdeeProductionMaterialListSyncJob', 'another-node')");
        assertFalse(service.stopAndDelete(7L));
        assertEquals(40, status());
        assertEquals(0, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_sync_run", Integer.class));
        jdbc.update("DELETE FROM QRTZ_FIRED_TRIGGERS");
        assertTrue(service.stopAndDelete(7L));
        assertEquals(1, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_sync_run", Integer.class));
        assertEquals(200, jdbc.queryForObject("SELECT created_count FROM erp_kingdee_sync_run", Integer.class));
    }

    @Test
    void directApiExecutionOnAnotherNodeAlsoBlocksDeletion() {
        when(executionLock.isLocked()).thenReturn(true);
        assertFalse(service.stopAndDelete(7L));
        assertEquals(40, status());
        assertEquals(0, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_sync_run", Integer.class));
        when(executionLock.isLocked()).thenReturn(false);
        assertTrue(service.stopAndDelete(7L));
    }

    @Test
    void deletionCannotChangeAnotherTenantAndDoesNotHideMissingTables() {
        TenantContextHolder.setTenantId(10L);
        assertFalse(service.stopAndDelete(7L));
        assertEquals(10, status());
        TenantContextHolder.setTenantId(9L);
        jdbc.execute("DROP TABLE QRTZ_FIRED_TRIGGERS");
        assertThrows(org.springframework.jdbc.BadSqlGrammarException.class, () -> service.stopAndDelete(7L));
        assertEquals(0, jdbc.queryForObject("SELECT deleted FROM erp_kingdee_sync_run", Integer.class));
    }

    private int status() {
        return jdbc.queryForObject("SELECT status FROM erp_kingdee_sync_run", Integer.class);
    }
}
