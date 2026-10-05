package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class DccWorkflowLifecycleTransactionTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileObsoleteAuditMapper auditMapper;
    @Resource private DccControlledFileNameClaimMapper claimMapper;
    private JdbcTemplate jdbc;
    private DccControlledFileLifecycleService service;
    private DccWorkflowDatePolicy dates;

    @BeforeEach
    void setupLifecycle() {
        jdbc = new JdbcTemplate(dataSource);
        assertTrue(jdbc.queryForObject("SELECT H2VERSION()", String.class) != null);
        dates = new DccWorkflowDatePolicy();
        dates.setZoneId("Asia/Singapore");
        dates.setReminderLeadDays(3);
        service = new DccControlledFileLifecycleService();
        ReflectionTestUtils.setField(service, "controlledFileMapper", fileMapper);
        ReflectionTestUtils.setField(service, "masterMapper", masterMapper);
        ReflectionTestUtils.setField(service, "obsoleteAuditMapper", auditMapper);
        ReflectionTestUtils.setField(service, "datePolicy", dates);
        ReflectionTestUtils.setField(service, "versionPolicy", DccControlledFileVersionPolicy.defaultPolicy());
        ReflectionTestUtils.setField(service, "jdbcTemplate", jdbc);
        ReflectionTestUtils.setField(service, "eventPublisher", (ApplicationEventPublisher) event -> {});
        // Ledger persistence/failure is covered by the real CC2 composition; this suite isolates lifecycle rules.
        ReflectionTestUtils.setField(service, "fileStateAudit", org.mockito.Mockito.mock(DccWorkflowFileStateAudit.class));
        ReflectionTestUtils.setField(service,"platformAdapter",org.mockito.Mockito.mock(DccControlledContentAdapter.class));
        var identities=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(identities,"reservationMapper",g25Reservations);
        ReflectionTestUtils.setField(identities,"masterMapper",masterMapper);
        ReflectionTestUtils.setField(identities,"claimMapper",claimMapper);
        var retention=new DccObsoleteRetentionService();
        ReflectionTestUtils.setField(retention,"identities",identities);
        ReflectionTestUtils.setField(service,"obsoleteRetentionService",retention);
    }

    @Test
    void futureControlChangesLatestOnlyAndPersistsSeparateControlledEvent() {
        seed(LocalDate.now().plusDays(10));
        control();
        assertEquals(1L, pointer("current_active_controlled_file_id"));
        assertEquals(2L, pointer("latest_controlled_file_id"));
        assertEquals("ACTIVE", state(1));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE", state(2));
        assertNull(jdbc.queryForObject("SELECT activated_time FROM dcc_controlled_file WHERE id=2", java.sql.Timestamp.class));
        assertEquals(1, events("CONTROLLED"));
        assertEquals(0, events("ACTIVATED"));
        assertEquals(Boolean.FALSE, new TransactionTemplate(transactionManager).execute(ignored -> service.activateDue(2L)));
    }

    @Test
    void effectiveTodayAtomicallyObsoletesOldAndKeepsPreassignedDate() {
        seed(LocalDate.now());
        control();
        assertEquals(2L, pointer("current_active_controlled_file_id"));
        assertEquals("OBSOLETE", state(1));
        assertEquals("ACTIVE", state(2));
        assertEquals(fileMapper.selectById(1L).getObsoletedTime().plusYears(20),
                jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
        assertEquals(LocalDate.now(), fileMapper.selectById(2L).getEffectiveDate());
        assertEquals(1, events("CONTROLLED"));
        assertEquals(1, events("ACTIVATED"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_obsolete_audit", Integer.class));
        assertEquals(Boolean.FALSE, new TransactionTemplate(transactionManager).execute(ignored -> service.activateDue(2L)));
    }

    @Test
    void concurrentDueActivationWritesOneSwitchOneAuditOneActivationEvent() throws Exception {
        seed(LocalDate.now().plusDays(1));
        control();
        // Test-owned H2 only: simulate an explicitly due test fixture without changing the system clock.
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2", LocalDate.now());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> activate = () -> {
            TenantContextHolder.setTenantId(1L);
            try {
                start.await();
                return new TransactionTemplate(transactionManager).execute(ignored -> service.activateDue(2L));
            } finally { TenantContextHolder.clear(); }
        };
        try {
            Future<Boolean> a = executor.submit(activate);
            Future<Boolean> b = executor.submit(activate);
            start.countDown();
            assertNotEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertEquals(1, events("ACTIVATED"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_obsolete_audit", Integer.class));
            assertEquals(2L, pointer("current_active_controlled_file_id"));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void failedControlRollsBackBothPointersArtifactFactsAndSuccessEvent() {
        seed(LocalDate.now());
        ReflectionTestUtils.setField(service, "eventPublisher", (ApplicationEventPublisher) event -> {
            throw new IllegalStateException("test-owned event persistence boundary failure");
        });
        assertThrows(IllegalStateException.class, this::control);
        assertEquals(1L, pointer("current_active_controlled_file_id"));
        assertEquals(1L, pointer("latest_controlled_file_id"));
        assertEquals("ACTIVE", state(1));
        assertNull(fileMapper.selectById(2L).getControlledTime());
        assertEquals(0, events("CONTROLLED"));
        assertEquals(0, events("ACTIVATED"));
    }

    @Test
    void failedActivationRollsBackOldObsoleteAuditNewStateAndCurrentPointer() {
        seed(LocalDate.now().plusDays(1));
        control();
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2", LocalDate.now());
        ReflectionTestUtils.setField(service, "eventPublisher", (ApplicationEventPublisher) event -> {
            throw new IllegalStateException("activation event failure");
        });
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager)
                .execute(ignored -> service.activateDue(2L)));
        assertEquals("ACTIVE", state(1));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE", state(2));
        assertEquals(1L, pointer("current_active_controlled_file_id"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_obsolete_audit", Integer.class));
        assertEquals(0, events("ACTIVATED"));
        assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim",java.time.LocalDateTime.class));
    }

    @Test
    void remindersAreDateOrderedAndExcludeDistributedVersions() {
        seed(LocalDate.now().plusDays(2));
        control();
        assertEquals(java.util.List.of(1L, 2L), service.pendingDistribution(true).stream().map(row -> row.getId()).toList());
        jdbc.update("UPDATE dcc_controlled_file SET distributed_time=CURRENT_TIMESTAMP WHERE id=1");
        assertEquals(java.util.List.of(2L), service.pendingDistribution(true).stream().map(row -> row.getId()).toList());
        dates.setReminderLeadDays(null);
        assertThrows(IllegalStateException.class, () -> service.pendingDistribution(true));
    }

    @Test
    void wrongTenantMissingConfigurationAndPastDateNeverSwitchVersions() {
        seed(LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class, this::control);
        dates.setZoneId(null);
        assertThrows(IllegalStateException.class, dates::now);
        TenantContextHolder.setTenantId(122L);
        assertThrows(IllegalArgumentException.class, () -> new TransactionTemplate(transactionManager)
                .execute(ignored -> service.activateDue(2L)));
        TenantContextHolder.setTenantId(1L);
        assertEquals(1L, pointer("current_active_controlled_file_id"));
    }

    @Test
    void obsoleteArchivalRequestRollsBackWithTheBusinessTransaction() {
        seed(LocalDate.now().plusDays(1));
        var requests=new DccWorkflowObsoleteArchiveRequestService(jdbc);
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactionManager).executeWithoutResult(ignored->{
            requests.request(1L,dates.now());throw new IllegalStateException("obsolete transaction failed");
        }));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_obsolete_archive",Integer.class));
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored->requests.request(1L,dates.now()));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM dcc_workflow_obsolete_archive WHERE controlled_file_id=1",String.class));
    }

    @Test
    void lifecycleEventKeyFitsTheDContractAndKeepsRoundAndEventTypeIdentity() {
        seed(LocalDate.now());
        String round="r".repeat(64);
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=? WHERE id=2",round);
        control();
        var keys=jdbc.queryForList("SELECT event_key FROM dcc_workflow_lifecycle_event ORDER BY id",String.class);
        assertEquals(2,keys.size());
        assertTrue(keys.stream().allMatch(key->key.length()<=128),"D contract accepts at most 128 characters");
        assertNotEquals(keys.get(0),keys.get(1));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event WHERE approval_process_instance_id=?",
                Integer.class,round));
    }
    @Test
    void committedControlReplayAfterTheEffectiveDateDoesNotRevalidateTheMutableDateOrCreateEvents() {
        seed(LocalDate.now().plusDays(1));
        control();
        jdbc.update("UPDATE dcc_controlled_file SET effective_date=? WHERE id=2",LocalDate.now().minusDays(1));
        assertDoesNotThrow(this::control);
        assertEquals(1,events("CONTROLLED"));
        assertEquals(0,events("ACTIVATED"));
        assertEquals(1L,pointer("current_active_controlled_file_id"));
    }
    @Test
    void futureControlRestoresTheControlledChainButDoesNotFabricateAnExecutingVersion() {
        seed(LocalDate.now().plusDays(10));
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=1");
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,status='OBSOLETE_CHAIN' WHERE id=10");
        control();
        assertEquals("ACTIVE_CHAIN",jdbc.queryForObject("SELECT status FROM dcc_controlled_file_master WHERE id=10",String.class));
        assertNull(pointer("current_active_controlled_file_id"));
        assertEquals(2L,pointer("latest_controlled_file_id"));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",state(2));
        assertEquals(0,events("ACTIVATED"));
    }

    @Test void futureControlClosesLowerWorkingIterationsOnlyAndPreservesTheirHistoricalSource() {
        seed(LocalDate.now().plusDays(10));working(3L,"A/1-1","WORKING");working(4L,"A/1-2","WORKING");
        working(5L,"B/1-1","WORKING");working(6L,"A/1-3","REJECTED");
        control();
        for(long id:new long[]{3,4}) {assertEquals("SUPERSEDED",state(id));assertEquals(2L,fileMapper.selectById(id).getSupersededByFileId());
            assertEquals(100L,fileMapper.selectById(id).getSourceFileId());assertNull(fileMapper.selectById(id).getControlledTime());}
        assertEquals("WORKING",state(5));assertNull(fileMapper.selectById(5L).getSupersededByFileId());
        assertEquals("REJECTED",state(6));assertEquals("ACTIVE",state(1));
        assertEquals(1L,pointer("current_active_controlled_file_id"));assertEquals(2L,pointer("latest_controlled_file_id"));
        control();assertEquals(1,events("CONTROLLED"));
    }
    @Test void failedControlledEventAlsoRollsBackLowerWorkingIterationClosure() {
        seed(LocalDate.now().plusDays(10));working(3L,"A/1-1","WORKING");
        ReflectionTestUtils.setField(service,"eventPublisher",(ApplicationEventPublisher)event->{throw new IllegalStateException("control event failed");});
        assertThrows(IllegalStateException.class,this::control);
        assertEquals("WORKING",state(3));assertNull(fileMapper.selectById(3L).getSupersededByFileId());
        assertNull(fileMapper.selectById(2L).getControlledTime());assertEquals(1L,pointer("latest_controlled_file_id"));
        assertEquals(0,events("CONTROLLED"));
    }
    @Test void anInvalidWorkingVersionCannotBeSilentlyLeftInTheControlledChain() {
        seed(LocalDate.now().plusDays(10));working(3L,"invalid-version","WORKING");
        assertThrows(IllegalStateException.class,this::control);
        assertEquals("WORKING",state(3));assertNull(fileMapper.selectById(2L).getControlledTime());
        assertEquals(1L,pointer("latest_controlled_file_id"));assertEquals(0,events("CONTROLLED"));
    }
    private void working(long id,String version,String status) {
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,
                  file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted)
                VALUES(?,10,10,20,100,100,'SOP.pdf','SOP','A-SOP',?,?,99,99,1,0)
                """,id,version,status);
    }

    private void seed(LocalDate date) {
        jdbc.update("""
                INSERT INTO dcc_controlled_file_master(id,category_id,file_name,file_number,status,tenant_id,
                  current_active_controlled_file_id,latest_controlled_file_id,deleted)
                VALUES(10,10,'SOP.pdf','A-SOP','ACTIVE_CHAIN',1,1,1,0)
                """);
        for (long id : new long[]{1,2}) {
            jdbc.update("""
                    INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,
                      file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,
                      published_file_id,stamped_file_id,effective_date,process_instance_id)
                    VALUES (?,10,10,20,100,100,'SOP.pdf','SOP','A-SOP',?,?,99,99,1,0,100,100,?,?)
                    """, id, id == 1 ? "A/1" : "B/1", id == 1 ? "ACTIVE" : "READY_TO_PUBLISH",
                    id == 1 ? date.minusDays(20) : date, "approval-" + id);
        }
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP,activated_time=CURRENT_TIMESTAMP WHERE id=1");
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name,source_original_file_name,normalized_file_number,deleted) VALUES(1,10,'SOP.pdf','SOP.pdf','A-SOP',0)");
    }

    private void control() {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            var master = masterMapper.selectByIdForUpdate(10L);
            var file = fileMapper.selectByIdAndTenantForUpdate(1L,2L);
            service.completeControl(file, master, 99L);
        });
    }
    private Long pointer(String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM dcc_controlled_file_master WHERE id=10", Long.class);
    }
    private String state(long id) { return jdbc.queryForObject("SELECT status FROM dcc_controlled_file WHERE id=?", String.class, id); }
    private int events(String type) { return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event WHERE event_type=?", Integer.class, type); }
}
