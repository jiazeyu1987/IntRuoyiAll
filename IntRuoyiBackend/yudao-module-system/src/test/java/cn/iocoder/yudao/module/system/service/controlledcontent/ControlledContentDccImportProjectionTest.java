package cn.iocoder.yudao.module.system.service.controlledcontent;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.*;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.*;
import cn.iocoder.yudao.module.system.enums.controlledcontent.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** The import command must inspect genuine persisted DCC facts rather than trust a caller's projection. */
class ControlledContentDccImportProjectionTest {
    private final LocalDateTime now=LocalDateTime.of(2026,11,2,12,0);
    private JdbcTemplate jdbc;
    private ControlledContentLifecycleCoreService service;
    private ControlledContentVersionRefMapper refs;
    private ControlledContentTransitionAuditMapper audits;
    private ControlledContentKey key;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);key=ControlledContentKey.of(1L,ControlledContentType.DCC_CONTROLLED_FILE,"100");
        var ds=new DriverManagerDataSource("jdbc:h2:mem:import_projection_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE dcc_controlled_file_master(id BIGINT,tenant_id BIGINT,deleted INT,dcc_project_code_id BIGINT,category_id BIGINT,file_type_taxonomy_leaf_id BIGINT,current_active_controlled_file_id BIGINT)");
        jdbc.execute("""
          CREATE TABLE dcc_controlled_file(id BIGINT,master_id BIGINT,tenant_id BIGINT,deleted INT,version_no VARCHAR(32),status VARCHAR(40),
          imported_by BIGINT,imported_time TIMESTAMP,controlled_time TIMESTAMP,effective_time TIMESTAMP,activated_time TIMESTAMP,
          import_source VARCHAR(32),effective_mode VARCHAR(32),dcc_project_code_id BIGINT,category_id BIGINT,file_type_taxonomy_id BIGINT,
          published_file_id BIGINT,read_only_file_id BIGINT,process_instance_id VARCHAR(128),effective_signature_id BIGINT)
          """);
        refs=mock(ControlledContentVersionRefMapper.class);audits=mock(ControlledContentTransitionAuditMapper.class);
        service=new ControlledContentLifecycleCoreService(refs,audits,new ControlledContentStateMachine());
        ReflectionTestUtils.setField(service,"jdbcTemplate",jdbc);
        when(refs.selectChainForUpdate(1L,"DCC_CONTROLLED_FILE","100")).thenReturn(List.of());
        doAnswer(c->{((ControlledContentVersionRefDO)c.getArgument(0)).setId(900L);return 1;}).when(refs).insert(any(ControlledContentVersionRefDO.class));
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    private void source(boolean active) {
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES(100,1,0,10,20,30,?)",active?101L:null);
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(101,100,1,0,'B/2',?,88,?,?,?,?, 'CONTROLLED_IMPORT','CONTROLLED_IMPORT',10,20,30,500,500,NULL,NULL)",
                active?"ACTIVE":"CONTROLLED_PENDING_EFFECTIVE",now,now.minusYears(1),active?now.minusDays(1):now.plusDays(1),active?now:null);
    }
    @Test void pendingImportEstablishesFormalProjectionWithoutApprovalAndAuditsRealUploader() {
        source(false);var ref=service.projectDccControlledImport(key,100L,101L,88L,now);
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",ref.getCanonicalStatus());assertNull(ref.getApprovalProcessInstanceId());
        assertNull(ref.getActiveUniqueFlag());assertNull(ref.getOpenCandidateUniqueFlag());
        verify(audits).insert(argThat((ControlledContentTransitionAuditDO a)->"REGISTER_CONTROLLED_IMPORT".equals(a.getAction())
                && a.getActorId().equals(88L) && a.getCreateTime().equals(now) && a.getFromStatus()==null));
    }
    @Test void dueImportEstablishesOnlyItsActualActivePointer() {
        source(true);var ref=service.projectDccControlledImport(key,100L,101L,88L,now);
        assertEquals("ACTIVE",ref.getCanonicalStatus());assertEquals(1,ref.getActiveUniqueFlag());
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL");
        assertThrows(IllegalStateException.class,()->service.projectDccControlledImport(key,100L,101L,88L,now));
    }
    @Test void cannotRegisterInventedNativeFileOrWrongActorTenantAndType() {
        assertThrows(IllegalStateException.class,()->service.projectDccControlledImport(key,100L,101L,88L,now));
        source(false);
        assertThrows(IllegalArgumentException.class,()->service.projectDccControlledImport(key,100L,101L,99L,now));
        assertThrows(IllegalArgumentException.class,()->service.projectDccControlledImport(
                ControlledContentKey.of(2L,ControlledContentType.DCC_CONTROLLED_FILE,"100"),100L,101L,88L,now));
        assertThrows(IllegalArgumentException.class,()->service.projectDccControlledImport(
                ControlledContentKey.of(1L,ControlledContentType.MES_ROUTE,"100"),100L,101L,88L,now));
        verify(refs,never()).insert(any(ControlledContentVersionRefDO.class));
    }
    @Test void refusesInventedBpmOrInvalidOriginalDatesAndProjectTypeDrift() {
        source(false);jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='invented'");
        assertThrows(IllegalStateException.class,()->service.projectDccControlledImport(key,100L,101L,88L,now));
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=NULL,controlled_time=?",now.plusDays(2));
        assertThrows(IllegalStateException.class,()->service.projectDccControlledImport(key,100L,101L,88L,now));
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=?,category_id=21",now.minusYears(1));
        assertThrows(IllegalStateException.class,()->service.projectDccControlledImport(key,100L,101L,88L,now));
    }
    @Test void onlyJobMayAdvanceActualImportedPendingRefAndReplayDoesNotAddAudit() {
        source(true);
        var prior=ControlledContentVersionRefDO.builder().id(900L).tenantId(1L).nativeMasterId(100L).nativeVersionId(101L)
                .versionNo("B/2").canonicalStatus("CONTROLLED_PENDING_EFFECTIVE").domainStatus("CONTROLLED_PENDING_EFFECTIVE").build();
        when(refs.selectChainForUpdate(1L,"DCC_CONTROLLED_FILE","100")).thenReturn(List.of(prior));
        assertThrows(IllegalStateException.class,()->service.projectDccControlledImport(key,100L,101L,88L,now));
        var result=service.projectDccControlledImport(key,100L,101L,-1L,now);assertEquals("ACTIVE",result.getCanonicalStatus());
        service.projectDccControlledImport(key,100L,101L,-1L,now);
        verify(audits,times(1)).insert(any(ControlledContentTransitionAuditDO.class));
    }
}
