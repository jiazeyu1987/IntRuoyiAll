package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual proxied initializer + B inheritance/Root round mapping, all on one isolated H2 transaction. */
@Import({DccWorkingApplicationDraftInitializer.class,DccApplicationRoundService.class,
        DccProjectApplicationSnapshotService.class,DccProjectAttributesService.class,DccWorkingApplicationDraftInitializerTest.Beans.class})
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_a_h08_initializer;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=10000")
class DccWorkingApplicationDraftInitializerTest extends BaseDbUnitTest {
    @TestConfiguration(proxyBeanMethods=false) static class Beans {
        @Bean JdbcTemplate jdbc(DataSource source){return new JdbcTemplate(source);}
    }
    @Resource DccWorkingApplicationDraftInitializer initializer;
    @Resource DccProjectApplicationSnapshotService bridge;
    @Resource DccProjectAttributesService attributes;
    @Resource DccApplicationRoundService rounds;
    @MockitoSpyBean DccControlledFileMapper files;
    @MockitoSpyBean DccControlledFileMasterMapper masters;
    @MockitoSpyBean DccProjectCodeMapper projects;
    @Resource JdbcTemplate jdbc;
    @Resource PlatformTransactionManager manager;
    @Resource org.mybatis.spring.SqlSessionTemplate sqlSessionTemplate;
    @MockitoBean DccProjectAccessService projectAccess;
    @MockitoBean DccControlledFileCategoryPermissionSupport categoryPermission;
    private final DccProjectAttributes nmpa=new DccProjectAttributes(List.of("NMPA"),null,"Y","N","N",null);
    private final DccProjectAttributes ce=new DccProjectAttributes(List.of("CE"),null,"N","Y","N",null);
    private final DccProjectAttributes fda=new DccProjectAttributes(List.of("FDA"),null,"N","N","N",null);

    @BeforeEach void seed(){
        TenantContextHolder.setTenantId(1L);assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,project_id BIGINT,application_type VARCHAR(16),application_id BIGINT,bpm_round VARCHAR(64),attribute_round INT,UNIQUE(tenant_id,application_type,application_id,bpm_round),UNIQUE(tenant_id,application_type,application_id,attribute_round))");
        jdbc.update("DELETE FROM dcc_application_round_link");
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status,default_attributes_json,deleted) VALUES(5,1,'H08','H08','ENABLE',?,0)",attributes.encode(nmpa));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,status,deleted) VALUES(10,1,2,3,'SOP','N-1',5,'ACTIVE_CHAIN',0)");
        files.insert(working(20L,null,"A/1"));
        when(categoryPermission.hasCategoryPermission(any(),any(),any())).thenReturn(true);
    }
    @AfterEach void clear(){jdbc.update("DELETE FROM dcc_application_round_link");TenantContextHolder.clear();}
    private DccControlledFileDO working(Long id,Long predecessor,String version){return DccControlledFileDO.builder()
        .id(id).tenantId(1L).masterId(10L).dccProjectCodeId(5L).categoryId(2L).directoryId(3L).fileTypeTaxonomyId(6L)
        .fileName("SOP").title("SOP").fileNumber("N-1").sourceOriginalFileName("SOP.pdf").sourceSha256("verified-body")
        .sourceFileId(100L).originalFileId(100L).versionNo(version).requesterId(99L).submitterId(99L)
        .status("WORKING").changeType("NEW").processType("CONTROLLED_FILE").predecessorControlledFileId(predecessor).build();}
    private DccProjectApplicationAttributesDO init(Long from,Long target,DccProjectAttributes actual){return tx().execute(s->initializer.initialize(99L,from,target,actual));}
    private TransactionTemplate tx(){return new TransactionTemplate(manager);}
    private void prepareSource(){init(null,20L,ce);}
    private void target(){files.insert(working(30L,20L,"A/1-1"));}
    private int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}

    @Test void mandatoryProxyRejectsNoTransactionWithoutAnyRoundOrAttributeWrites(){
        assertThrows(IllegalTransactionStateException.class,()->initializer.initialize(99L,null,20L,ce));
        assertEquals(0,count("dcc_application_round_link"));assertEquals(0,count("dcc_project_application_attributes"));
    }
    @ParameterizedTest @ValueSource(strings={"CONTROLLED_CHAIN","WRONG_INTENT"})
    void absentSourceIsOnlyAllowedForAGenuineInitialNewApplication(String invalidInitial){
        if("CONTROLLED_CHAIN".equals(invalidInitial))jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=99 WHERE id=10");
        else jdbc.update("UPDATE dcc_controlled_file SET revision_change_type='PARTIAL' WHERE id=20");
        assertThrows(RuntimeException.class,()->init(null,20L,ce));
        assertEquals(0,count("dcc_application_round_link"));assertEquals(0,count("dcc_project_application_attributes"));
    }
    @Test void continuousDirectPredecessorsKeepSourceActualAndTheExactProvenance(){
        var root=init(null,20L,ce);jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(fda));
        target();var first=init(20L,30L,null);files.insert(working(40L,30L,"A/1-2"));var second=init(30L,40L,null);
        for(var saved:List.of(first,second)){assertEquals(nmpa,attributes.readValue(saved.getDefaultSourceJson()));assertEquals(ce,attributes.readValue(saved.getActualAttributesJson()));assertFalse(saved.getSubmitted());}
        assertEquals(root.getApplicationRound(),first.getSourceApplicationRound());assertEquals(20L,first.getSourceApplicationId());assertEquals(30L,second.getSourceApplicationId());
        assertEquals(3,count("dcc_application_round_link"));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE bpm_round IS NOT NULL",Integer.class));
    }
    @Test void replaysBeforeAndAfterBindingReadBackWithoutCoveringTheLaterManualEdit(){
        prepareSource();target();var first=init(20L,30L,ce);
        tx().executeWithoutResult(s->bridge.saveReservedDraft(99L,5L,"UPLOAD",30L,fda));
        assertEquals(first.getId(),init(20L,30L,ce).getId());assertEquals(fda,attributes.readValue(init(20L,30L,ce).getActualAttributesJson()));
        tx().executeWithoutResult(s->{bridge.submitReservedDraft(99L,5L,"UPLOAD",30L,first.getApplicationRound(),"actual-bpm",fda);
            jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_REVIEW',process_instance_id='actual-bpm',process_definition_key='dcc-controlled-file-upload' WHERE id=30");});
        var frozen=init(20L,30L,ce);assertTrue(frozen.getSubmitted());assertEquals(fda,attributes.readValue(frozen.getActualAttributesJson()));assertEquals(2,count("dcc_application_round_link"));
    }
    @Test void explicitActualChangeOnTheNewFilePreservesTheInheritedSourceAndTheOldFile(){
        prepareSource();target();var copied=init(20L,30L,fda);
        assertEquals(nmpa,attributes.readValue(copied.getDefaultSourceJson()));assertEquals(fda,attributes.readValue(copied.getActualAttributesJson()));
        assertEquals(ce,attributes.readValue(bridge.readReservedDraft(99L,5L,"UPLOAD",20L).getActualAttributesJson()));
        assertEquals(fda,attributes.readValue(init(20L,30L,ce).getActualAttributesJson()));
    }
    @Test void newApplicationFromTheOfficialControlledBaselineUsesCurrentDefaults(){
        var original=init(null,20L,ce);
        tx().executeWithoutResult(s->bridge.submitReservedDraft(99L,5L,"UPLOAD",20L,original.getApplicationRound(),"old-upload",ce));
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP,process_instance_id='old-upload',process_definition_key='dcc-controlled-file-upload' WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(fda));
        var revision=working(30L,20L,"A/1-1");revision.setChangeType("REVISION");revision.setRevisionBaseActiveControlledFileId(20L);files.insert(revision);
        var saved=init(20L,30L,null);assertEquals("REVISION",saved.getApplicationType());assertNull(saved.getSourceApplicationId());
        assertEquals(fda,attributes.readValue(saved.getDefaultSourceJson()));assertEquals(fda,attributes.readValue(saved.getActualAttributesJson()));
        assertEquals(nmpa,attributes.readValue(bridge.read(99L,5L,"UPLOAD",20L,"old-upload").getDefaultSourceJson()));
    }
    @Test void rejectedSubmittedSourceUsesItsActualBpmRoundAndKeepsOriginalSource(){
        var source=init(null,20L,ce);tx().executeWithoutResult(s->bridge.submitReservedDraft(99L,5L,"UPLOAD",20L,source.getApplicationRound(),"rejected-round",ce));
        jdbc.update("UPDATE dcc_controlled_file SET status='REJECTED',process_instance_id='rejected-round',process_definition_key='dcc-controlled-file-upload' WHERE id=20");
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(fda));target();
        var fork=init(20L,30L,null);assertEquals(nmpa,attributes.readValue(fork.getDefaultSourceJson()));assertEquals(ce,attributes.readValue(fork.getActualAttributesJson()));
        assertEquals(source.getApplicationRound(),fork.getSourceApplicationRound());assertEquals(20L,fork.getSourceApplicationId());
    }
    @ParameterizedTest @ValueSource(strings={"SOURCE_TENANT","TARGET_TENANT","PROJECT","MASTER","ACTION","SOURCE_APPLICANT","TARGET_APPLICANT",
            "DIRECT_PREDECESSOR","MISSING_SOURCE","MISSING_ATTRIBUTES","SOURCE_BODY","CANDIDATE_BODY","TARGET_CONTROLLED","SOURCE_BPM","OMITTED_SOURCE"})
    void wrongFactsAndMissingSavedSourceWriteNoTargetRoundOrAttributes(String change){
        prepareSource();target();
        switch(change){
            case "SOURCE_TENANT" -> jdbc.update("UPDATE dcc_controlled_file SET tenant_id=122 WHERE id=20");
            case "TARGET_TENANT" -> jdbc.update("UPDATE dcc_controlled_file SET tenant_id=122 WHERE id=30");
            case "PROJECT" -> jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=6 WHERE id=20");
            case "MASTER" -> jdbc.update("UPDATE dcc_controlled_file SET master_id=11 WHERE id=20");
            case "ACTION" -> jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION' WHERE id=30");
            case "SOURCE_APPLICANT" -> jdbc.update("UPDATE dcc_controlled_file SET requester_id=100 WHERE id=20");
            case "TARGET_APPLICANT" -> jdbc.update("UPDATE dcc_controlled_file SET requester_id=100 WHERE id=30");
            case "DIRECT_PREDECESSOR" -> jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=40 WHERE id=30");
            case "MISSING_SOURCE" -> jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id=20");
            case "MISSING_ATTRIBUTES" -> jdbc.update("DELETE FROM dcc_project_application_attributes WHERE application_id=20");
            case "SOURCE_BODY" -> jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='other.pdf' WHERE id=20");
            case "CANDIDATE_BODY" -> jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_controlled_file_id=20,selected_iteration_version_no='A/1',source_sha256='changed' WHERE id=30");
            case "TARGET_CONTROLLED" -> jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id=30");
            case "SOURCE_BPM" -> jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='unregistered' WHERE id=20");
            case "OMITTED_SOURCE" -> { }
            default -> throw new AssertionError(change);
        }
        int mappings=count("dcc_application_round_link"),records=count("dcc_project_application_attributes");
        assertThrows(RuntimeException.class,()->init("OMITTED_SOURCE".equals(change)?null:20L,30L,ce));
        assertEquals(mappings,count("dcc_application_round_link"));assertEquals(records,count("dcc_project_application_attributes"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes WHERE application_id=30",Integer.class));
    }
    @Test void outerLateFailureRollsBackTheInsertedFileAndItsOfficialRoundAndAttributes(){
        prepareSource();assertThrows(IllegalStateException.class,()->tx().executeWithoutResult(s->{target();initializer.initialize(99L,20L,30L,null);throw new IllegalStateException("late failure");}));
        assertNull(files.selectById(30L));assertEquals(1,count("dcc_application_round_link"));assertEquals(1,count("dcc_project_application_attributes"));
    }
    @Test void twoConcurrentInitializationsMaterializeOneRecordAndDoNotOverwriteTheWinner() throws Exception {
        prepareSource();target();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        Callable<Long> action=()->{TenantContextHolder.setTenantId(1L);try{start.await();return init(20L,30L,null).getId();}finally{TenantContextHolder.clear();}};
        try{var one=pool.submit(action);var two=pool.submit(action);start.countDown();assertEquals(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS));}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
        assertEquals(2,count("dcc_application_round_link"));assertEquals(2,count("dcc_project_application_attributes"));
    }
    @Test void waitingForBProjectLockDoesNotAcquireMasterAndCreateALockInversion() throws Exception {
        prepareSource();target();var projectLocked=new CountDownLatch(1);var initializerEntered=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        var initializerThread=new java.util.concurrent.atomic.AtomicReference<Thread>();
        var realProjects=sqlSessionTemplate.getMapper(DccProjectCodeMapper.class);
        doAnswer(call->{if(Thread.currentThread()==initializerThread.get())initializerEntered.countDown();return realProjects.selectByIdForUpdate(call.getArgument(0));})
                .when(projects).selectByIdForUpdate(5L);
        Callable<Void> existingB=()->{TenantContextHolder.setTenantId(1L);try{tx().executeWithoutResult(s->{projects.selectByIdForUpdate(5L);projectLocked.countDown();
            await(initializerEntered);masters.selectByIdForUpdate(10L);});return null;}finally{TenantContextHolder.clear();}};
        Callable<Long> initialize=()->{TenantContextHolder.setTenantId(1L);initializerThread.set(Thread.currentThread());try{await(projectLocked);return init(20L,30L,null).getId();}finally{TenantContextHolder.clear();}};
        try{var b=pool.submit(existingB);var a=pool.submit(initialize);b.get(15,TimeUnit.SECONDS);assertNotNull(a.get(15,TimeUnit.SECONDS));}
        finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @Test void actualMapperLockCallsAreProjectThenMasterThenBothFilesInAscendingOrder(){
        prepareSource();target();clearInvocations(files,masters,projects);
        init(20L,30L,null);
        var order=inOrder(files,masters,projects);
        order.verify(files).selectById(30L);order.verify(projects).selectByIdForUpdate(5L);
        order.verify(masters).selectByIdForUpdate(10L);order.verify(files).selectByIdAndTenantForUpdate(1L,20L);
        order.verify(files).selectByIdAndTenantForUpdate(1L,30L);
    }
    private void await(CountDownLatch latch){try{assertTrue(latch.await(5,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
}
