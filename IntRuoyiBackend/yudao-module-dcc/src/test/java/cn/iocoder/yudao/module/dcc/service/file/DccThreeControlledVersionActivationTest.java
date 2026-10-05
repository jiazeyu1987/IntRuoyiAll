package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.service.file.listener.DccControlledFileActivationJob;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real lifecycle/claim/retention and immutable GxP ledger on isolated H2, with explicit business clock. */
@Import({DccWorkflowFileStateAudit.class,GxpAuditServiceImpl.class,DccThreeControlledVersionActivationTest.Beans.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccThreeControlledVersionActivationTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class Beans {@Bean JdbcTemplate jdbc(javax.sql.DataSource source){return new JdbcTemplate(source);}}
    @Resource JdbcTemplate jdbc;
    @Resource PlatformTransactionManager manager;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileObsoleteAuditMapper obsoleteAudits;
    @Resource DccControlledFileNameClaimMapper claims;
    @Resource DccWorkflowFileStateAudit stateAudit;
    @MockitoSpyBean GxpAuditServiceImpl ledger;
    @org.springframework.test.context.bean.override.mockito.MockitoBean cn.iocoder.yudao.module.system.api.user.AdminUserApi actualAccounts;
    @Resource GxpAuditEventMapper events;
    DccControlledFileLifecycleService lifecycle;DccControlledFileActivationJob job;FixedDates clock;
    final LocalDate day=LocalDate.of(2026,10,3);
    static class FixedDates extends DccWorkflowDatePolicy {LocalDateTime current;@Override public LocalDateTime now(){return current;}}
    static void wire(Object bean,Object...pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(bean,(String)pairs[i],pairs[i+1]);}
    @BeforeEach void fixture() throws Exception {
        org.mockito.Mockito.when(actualAccounts.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                .setId(99L).setTenantId(1L).setStatus(0).setUsername("actor99").setNickname("文控99"));
        TenantContextHolder.setTenantId(1L);try(var c=jdbc.getDataSource().getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        clock=new FixedDates();clock.current=day.atTime(9,0);clock.setZoneId("Asia/Shanghai");
        var identities=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(identities,"reservationMapper",g25Reservations);wire(identities,"masterMapper",masters,"claimMapper",claims,"fileMapper",files);
        var retention=new DccObsoleteRetentionService();wire(retention,"identities",identities);
        var service=new DccControlledFileLifecycleService();wire(service,"controlledFileMapper",files,"masterMapper",masters,"obsoleteAuditMapper",obsoleteAudits,
            "platformAdapter",org.mockito.Mockito.mock(DccControlledContentAdapter.class),
            "datePolicy",clock,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"jdbcTemplate",jdbc,"obsoleteRetentionService",retention,"fileStateAudit",stateAudit,
            "eventPublisher",(org.springframework.context.ApplicationEventPublisher)event->{});
        var factory=new ProxyFactory(service);factory.setProxyTargetClass(true);factory.addAdvice(new TransactionInterceptor(manager,new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));lifecycle=(DccControlledFileLifecycleService)factory.getProxy();job=new DccControlledFileActivationJob(lifecycle);
        for(String operation:List.of("control","activate","auto-obsolete"))jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES(1,'P16','dcc.controlled-file.' || ?,'SERVICE_METHOD','DCC.TEST','DCC','CONTROLLED_FILE','UPDATE','REQUIRED','NOT_REQUIRED','BEFORE_AFTER','GXP_CONTROLLED_DOCUMENT','P16','backend','GXP',TRUE)",operation);
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status,current_active_controlled_file_id,latest_controlled_file_id) VALUES(10,1,2,3,'SOP','N-1',5,6,'N-1','ACTIVE_CHAIN',1,1)");
        add(1,"A/1","ACTIVE",day.minusDays(10));jdbc.update("UPDATE dcc_controlled_file SET controlled_time=?,activated_time=? WHERE id=1",day.minusDays(10).atStartOfDay(),day.minusDays(10).atStartOfDay());
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name,source_original_file_name,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(1,10,'SOP.pdf','SOP.pdf',5,6,'N-1')");
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void add(long id,String version,String status,LocalDate date){files.insert(DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L).sourceFileId(100L+id).originalFileId(100L+id).fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf").fileNumber("N-1").versionNo(version).status(status).requesterId(99L).submitterId(99L).dccProjectCodeId(5L).fileTypeTaxonomyId(6L).publishedFileId(200L+id).stampedFileId(300L+id).processInstanceId("approval-"+id).effectiveDate(date).approvedTime(day.minusDays(2).atTime(8,0)).build());}
    void control(long id){var login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(2);login.setInfo(Map.of("username","control","nickname","正式文控"));SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));try{new TransactionTemplate(manager).executeWithoutResult(s->lifecycle.completeControl(files.selectByIdAndTenantForUpdate(1L,id),masters.selectByIdForUpdate(10L),99L));}finally{SecurityContextHolder.clearContext();}}
    void twoFuture(LocalDate second,LocalDate third){add(2,"A/2","READY_TO_PUBLISH",second);control(2);add(3,"A/3","READY_TO_PUBLISH",third);control(3);}
    Map<String,Object> facts(String sql){var result=new TreeMap<String,Object>(jdbc.queryForMap(sql));result.replaceAll((key,value)->value instanceof byte[] data?HexFormat.of().formatHex(data):value);return result;}
    String state(long id){return files.selectById(id).getStatus();}
    Long active(){return masters.selectById(10L).getCurrentActiveControlledFileId();}
    @Test void newerEarlierDateMustObsoleteTheLowerPendingVersionAndNeverPoisonLaterJob() {
        twoFuture(day.plusDays(20),day.plusDays(10));clock.current=day.plusDays(10).atTime(9,0);
        var original=files.selectById(2L);assertTrue(lifecycle.activateDue(3L));
        assertEquals("OBSOLETE",state(2));assertEquals(3L,files.selectById(2L).getSupersededByFileId());
        assertEquals(original.getEffectiveDate(),files.selectById(2L).getEffectiveDate());assertEquals(original.getControlledTime(),files.selectById(2L).getControlledTime());assertNull(files.selectById(2L).getActivatedTime());
        clock.current=day.plusDays(20).atTime(9,0);assertEquals("实际生效版本数：0",job.execute(null));assertEquals(3L,active());
        assertEquals(2,obsoleteAudits.selectList().size());
    }
    @Test void dueScanOfAStaleLowerPendingRowMustNotReverseCurrentExecutionOrThrow() {
        twoFuture(day.plusDays(20),day.plusDays(10));
        // Seed the exact pre-fix committed state, without pretending a new fixed activation created it.
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',activated_time=? WHERE id=3",day.plusDays(10).atTime(9,0));
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE',obsoleted_time=?,superseded_by_file_id=3 WHERE id=1",day.plusDays(10).atTime(9,0));
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=3 WHERE id=10");
        clock.current=day.plusDays(20).atTime(9,0);assertFalse(lifecycle.activateDue(2L));assertEquals("OBSOLETE",state(2));assertEquals(3L,active());
    }
    @ParameterizedTest @ValueSource(ints={-5,0,5})
    void threeControlledDatesPreserveHigherFutureAndEventuallyRetireEveryLowerControl(int order) {
        LocalDate second=day.plusDays(10),third=second.plusDays(order);twoFuture(second,third);
        var before2=files.selectById(2L);var before3=files.selectById(3L);
        clock.current=second.isBefore(third)?second.atTime(9,0):third.atTime(9,0);
        assertDoesNotThrow(()->job.execute(null));
        if(order>0){assertEquals(2L,active());assertEquals("CONTROLLED_PENDING_EFFECTIVE",state(3));assertNull(files.selectById(3L).getObsoletedTime());assertEquals(3L,masters.selectById(10L).getLatestControlledFileId());}
        else {assertEquals(3L,active());assertEquals("OBSOLETE",state(2));}
        clock.current=second.isAfter(third)?second.atTime(10,0):third.atTime(10,0);assertDoesNotThrow(()->job.execute(null));
        assertEquals(3L,active());assertEquals(3L,masters.selectById(10L).getLatestControlledFileId());assertEquals("OBSOLETE",state(1));assertEquals("OBSOLETE",state(2));assertEquals("ACTIVE",state(3));
        assertEquals(before2.getEffectiveDate(),files.selectById(2L).getEffectiveDate());assertEquals(before3.getEffectiveDate(),files.selectById(3L).getEffectiveDate());
        assertEquals(before2.getControlledTime(),files.selectById(2L).getControlledTime());assertEquals(before2.getApprovedTime(),files.selectById(2L).getApprovedTime());
        assertEquals(before2.getPublishedFileId(),files.selectById(2L).getPublishedFileId());assertEquals(before2.getStampedFileId(),files.selectById(2L).getStampedFileId());assertEquals(before2.getSourceFileId(),files.selectById(2L).getSourceFileId());
        assertEquals(files.selectById(2L).getObsoletedTime().plusYears(20),jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE master_id=10",LocalDateTime.class));
        assertEquals(2,obsoleteAudits.selectList().size());assertEquals(0,lifecycle.dueVersions().size());assertFalse(lifecycle.activateDue(2L));
    }
    @Test void higherControlledFutureAndFailedUncontrolledCandidatesRemainUntouched() {
        twoFuture(day.plusDays(10),day.plusDays(20));add(4,"A/4","REJECTED",day.plusDays(1));add(5,"A/5","READY_TO_PUBLISH",day.plusDays(1));
        var failed=facts("SELECT * FROM dcc_controlled_file WHERE id=4");var pending=facts("SELECT * FROM dcc_controlled_file WHERE id=5");
        clock.current=day.plusDays(10).atTime(9,0);assertTrue(lifecycle.activateDue(2L));
        assertEquals(2L,active());assertEquals(3L,masters.selectById(10L).getLatestControlledFileId());assertEquals("CONTROLLED_PENDING_EFFECTIVE",state(3));
        assertEquals(failed,facts("SELECT * FROM dcc_controlled_file WHERE id=4"));assertEquals(pending,facts("SELECT * FROM dcc_controlled_file WHERE id=5"));assertEquals(1,obsoleteAudits.selectList().size());
    }
    @Test void multiObsoleteWritesActualSystemLedgerForEachOriginalVersionAndPreservesSignatures() {
        twoFuture(day.plusDays(20),day.plusDays(10));
        jdbc.update("INSERT INTO dcc_controlled_file_signature(controlled_file_id,task_id,actor_id,action_type,signature_mode,comment,process_instance_id,version_no,source_file_id,tenant_id) VALUES(2,'approved-task',99,'APPROVE','FIXTURE','原签核意见','approval-2','A/2',102,1)");
        var signature=facts("SELECT * FROM dcc_controlled_file_signature WHERE controlled_file_id=2");
        clock.current=day.plusDays(10).atTime(9,0);assertTrue(lifecycle.activateDue(3L));
        var audits=events.selectList().stream().filter(e->"dcc.controlled-file.auto-obsolete".equals(e.getOperationId())).toList();assertEquals(2,audits.size());
        for(var audit:audits){assertEquals(-1L,audit.getActorId());assertEquals("SYSTEM_ACTOR",audit.getActorUsername());assertEquals("VOIDED",audit.getAfterState());assertTrue(audit.getAfterStateJson().contains("\"currentActiveControlledFileId\":3"));assertTrue(audit.getAfterStateJson().contains("\"latestControlledFileId\":3"));assertTrue(audit.getAfterStateJson().contains("\"supersededByFileId\":3"));}
        assertTrue(audits.stream().anyMatch(a->a.getBeforeStateJson().contains("CONTROLLED_PENDING_EFFECTIVE")));
        assertEquals(signature,facts("SELECT * FROM dcc_controlled_file_signature WHERE controlled_file_id=2"));
    }
    @Test void lateActivationLedgerFailureRollsBackAllObsoleteRowsPointersRetentionAndAudits() {
        twoFuture(day.plusDays(20),day.plusDays(10));var before1=facts("SELECT * FROM dcc_controlled_file WHERE id=1");var before2=facts("SELECT * FROM dcc_controlled_file WHERE id=2");var before3=facts("SELECT * FROM dcc_controlled_file WHERE id=3");var beforeMaster=facts("SELECT * FROM dcc_controlled_file_master WHERE id=10");var beforeClaim=facts("SELECT * FROM dcc_controlled_file_name_claim WHERE master_id=10");int eventCount=events.selectList().size();
        doAnswer(call->{GxpAuditCommand cmd=call.getArgument(0);if("dcc.controlled-file.activate".equals(cmd.getOperationId()))throw new IllegalStateException("late activation ledger failure");return call.callRealMethod();}).when(ledger).append(any());
        clock.current=day.plusDays(10).atTime(9,0);assertThrows(IllegalStateException.class,()->lifecycle.activateDue(3L));
        assertEquals(before1,facts("SELECT * FROM dcc_controlled_file WHERE id=1"));assertEquals(before2,facts("SELECT * FROM dcc_controlled_file WHERE id=2"));assertEquals(before3,facts("SELECT * FROM dcc_controlled_file WHERE id=3"));assertEquals(beforeMaster,facts("SELECT * FROM dcc_controlled_file_master WHERE id=10"));assertEquals(beforeClaim,facts("SELECT * FROM dcc_controlled_file_name_claim WHERE master_id=10"));
        assertEquals(eventCount,events.selectList().size());assertEquals(0,obsoleteAudits.selectList().size());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event WHERE event_type='ACTIVATED'",Integer.class));
    }
    @Test void concurrentJobAndDirectReplayCannotReverseTheHigherExecution() throws Exception {
        twoFuture(day.plusDays(20),day.plusDays(10));clock.current=day.plusDays(20).atTime(9,0);
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(3);
        try {
            var futures=new ArrayList<Future<Object>>();for(int command:new int[]{0,1,2})futures.add(pool.submit(()->{TenantContextHolder.setTenantId(1L);try{assertTrue(start.await(5,TimeUnit.SECONDS));return command==0?job.execute(null):lifecycle.activateDue(command==1?3L:2L);}finally{TenantContextHolder.clear();}}));
            start.countDown();for(var future:futures)assertNotNull(future.get(20,TimeUnit.SECONDS));
            assertEquals(3L,active());assertEquals("OBSOLETE",state(2));assertEquals(2,obsoleteAudits.selectList().size());
            assertEquals(2,events.selectList().stream().filter(e->"dcc.controlled-file.auto-obsolete".equals(e.getOperationId())).count());
            assertEquals(0,lifecycle.dueVersions().size());assertEquals("实际生效版本数：0",job.execute(null));
        } finally {start.countDown();pool.shutdownNow();}
    }
    @Test void immediatelyEffectiveThirdControlClosesOlderPendingWithinTheSameControlTransaction() {
        add(2,"A/2","READY_TO_PUBLISH",day.plusDays(20));control(2);add(3,"A/3","READY_TO_PUBLISH",day);
        control(3);assertEquals(3L,active());assertEquals(3L,masters.selectById(10L).getLatestControlledFileId());
        assertEquals("OBSOLETE",state(2));assertNull(files.selectById(2L).getActivatedTime());assertEquals(day.plusDays(20),files.selectById(2L).getEffectiveDate());
        assertEquals(2,obsoleteAudits.selectList().size());int ledgerCount=events.selectList().size();control(3);
        assertEquals(ledgerCount,events.selectList().size());assertEquals(2,obsoleteAudits.selectList().size());
        clock.current=day.plusDays(20).atTime(9,0);assertEquals("实际生效版本数：0",job.execute(null));
    }
}
