package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccControlledImportRelationsDatabaseTest {
    JdbcTemplate jdbc; TransactionTemplate tx; DccRelationStore store; GxpAuditService audit;
    LocalDateTime now=LocalDateTime.of(2026,11,7,1,0);
    DccControlledFileDO source;
    @BeforeEach void setup() {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MYSQL;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE dcc_controlled_file_master(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT,latest_controlled_file_id BIGINT,current_active_controlled_file_id BIGINT,dcc_project_code_id BIGINT,category_id BIGINT,file_type_taxonomy_leaf_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE dcc_controlled_file(id BIGINT PRIMARY KEY,tenant_id BIGINT,master_id BIGINT,deleted INT,status VARCHAR(32),
                  dcc_project_code_id BIGINT,category_id BIGINT,file_type_taxonomy_id BIGINT,source_file_id BIGINT,source_sha256 VARCHAR(64),
                  import_source VARCHAR(32),effective_mode VARCHAR(32),imported_by BIGINT,imported_time TIMESTAMP,controlled_time TIMESTAMP,
                  effective_time TIMESTAMP,activated_time TIMESTAMP,published_time TIMESTAMP,read_only_file_id BIGINT,
                  change_type VARCHAR(32),revision_change_type VARCHAR(32),process_instance_id VARCHAR(128),predecessor_controlled_file_id BIGINT,
                  effective_signature_id BIGINT,prepared_workflow_contract VARCHAR(64),prepared_process_definition_id VARCHAR(128),prepared_definition_digest VARCHAR(64))
                """);
        jdbc.execute("CREATE TABLE dcc_controlled_file_source_ownership(tenant_id BIGINT,controlled_file_id BIGINT,source_file_id BIGINT,source_sha256 VARCHAR(64),ownership_type VARCHAR(32),claimed_by BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_related_file(tenant_id BIGINT,controlled_file_id BIGINT,related_master_id BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE dcc_current_file_relation(tenant_id BIGINT,source_master_id BIGINT,related_master_id BIGINT)");
        jdbc.execute("CREATE TABLE dcc_current_file_relation_set(tenant_id BIGINT,source_master_id BIGINT,controlled_file_id BIGINT,row_version BIGINT DEFAULT 0,PRIMARY KEY(tenant_id,source_master_id))");
        jdbc.execute("CREATE TABLE relation_audit(id BIGINT AUTO_INCREMENT PRIMARY KEY,operation VARCHAR(128))");
        audit=mock(GxpAuditService.class);
        doAnswer(call->{jdbc.update("INSERT INTO relation_audit(operation) VALUES('controlled-import')");return null;}).when(audit).append(any());
        store=new DccRelationStore(jdbc,audit);TenantContextHolder.setTenantId(1L);
        source=DccControlledFileDO.builder().id(100L).tenantId(1L).masterId(10L).status("ACTIVE")
                .importSource("CONTROLLED_IMPORT").effectiveMode("CONTROLLED_IMPORT").importedBy(7L)
                .importedTime(now).controlledTime(now).effectiveTime(now).activatedTime(now)
                .changeType("NEW").revisionChangeType("INITIAL").build();
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    void seed() {
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES(10,1,0,100,100,9,8,6)");
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,tenant_id,master_id,deleted,status,dcc_project_code_id,category_id,file_type_taxonomy_id,
                  source_file_id,source_sha256,import_source,effective_mode,imported_by,imported_time,controlled_time,effective_time,
                  activated_time,published_time,read_only_file_id,change_type,revision_change_type)
                VALUES(100,1,10,0,'ACTIVE',9,8,6,500,'digest','CONTROLLED_IMPORT','CONTROLLED_IMPORT',7,?,?,?,?,?,500,'NEW','INITIAL')
                """,now,now,now,now,now);
        jdbc.update("INSERT INTO dcc_controlled_file_source_ownership VALUES(1,100,500,'digest','CONTROLLED_IMPORT',7,0)");
    }
    void initialize(){tx.executeWithoutResult(s->store.initializeControlledImportRelations(source,7L));}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    @Test void originalImportTransactionCommitsAnExplicitEmptySetAndAudit() {
        tx.executeWithoutResult(s->{seed();store.initializeControlledImportRelations(source,7L);});
        assertEquals(100L,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=1 AND source_master_id=10",Long.class));
        assertEquals(0L,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set",Long.class));
        assertEquals(0,count("dcc_current_file_relation"));assertEquals(1,count("relation_audit"));
    }
    @Test void failedImportRollsBackFileMasterSetAndAuditTogether() {
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{
            seed();store.initializeControlledImportRelations(source,7L);throw new IllegalStateException("IMPORT_TX_FAILED");
        }));
        for(String table:new String[]{"dcc_controlled_file","dcc_controlled_file_master","dcc_controlled_file_source_ownership","dcc_current_file_relation_set","relation_audit"})assertEquals(0,count(table));
    }
    @Test void auditFailureCannotLeaveAnUntracedCurrentSet() {
        seed();doThrow(new IllegalStateException("AUDIT_FAILED")).when(audit).append(any());
        assertThrows(IllegalStateException.class,this::initialize);assertEquals(0,count("dcc_current_file_relation_set"));
    }
    @Test void existingCurrentSetCannotBeReinitializedOrRegressed() {
        seed();jdbc.update("INSERT INTO dcc_current_file_relation_set VALUES(1,10,200,3)");
        assertThrows(DccRelationFailure.class,this::initialize);
        assertEquals(200L,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set",Long.class));
        assertEquals(3L,jdbc.queryForObject("SELECT row_version FROM dcc_current_file_relation_set",Long.class));verifyNoInteractions(audit);
    }
    @ParameterizedTest @ValueSource(strings={
        "UPDATE dcc_controlled_file SET tenant_id=2", "UPDATE dcc_controlled_file_master SET tenant_id=2",
        "UPDATE dcc_controlled_file_master SET latest_controlled_file_id=101", "UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=101",
        "UPDATE dcc_controlled_file_master SET category_id=99", "UPDATE dcc_controlled_file SET status='WORKING'",
        "UPDATE dcc_controlled_file SET process_instance_id='invented'", "UPDATE dcc_controlled_file SET effective_signature_id=33",
        "UPDATE dcc_controlled_file SET prepared_workflow_contract='PUBLIC_MATRIX_NATIVE_V2'", "UPDATE dcc_controlled_file SET effective_time=NULL",
        "UPDATE dcc_controlled_file_source_ownership SET tenant_id=2", "UPDATE dcc_controlled_file_source_ownership SET controlled_file_id=101",
        "UPDATE dcc_controlled_file_source_ownership SET ownership_type='UPLOAD'", "UPDATE dcc_controlled_file_source_ownership SET source_sha256='wrong'",
        "UPDATE dcc_controlled_file_source_ownership SET claimed_by=8", "DELETE FROM dcc_controlled_file_source_ownership",
        "INSERT INTO dcc_controlled_file_related_file VALUES(1,100,20,0)",
        "INSERT INTO dcc_current_file_relation VALUES(1,10,20)"
    }) void invalidPersistedSourceOrNonemptySnapshotIsRejected(String mutation) {
        seed();jdbc.update(mutation);assertThrows(DccRelationFailure.class,this::initialize);
        assertEquals(0,count("dcc_current_file_relation_set"));verifyNoInteractions(audit);
    }
    @Test void actorAndTenantCannotBeTakenFromAnImportFlagAlone() {
        seed();assertThrows(DccRelationFailure.class,()->tx.executeWithoutResult(s->store.initializeControlledImportRelations(source,8L)));
        TenantContextHolder.setTenantId(2L);assertThrows(DccRelationFailure.class,this::initialize);
        assertEquals(0,count("dcc_current_file_relation_set"));verifyNoInteractions(audit);
    }
    @Test void anotherTenantsRelationRowsCannotChangeThisImportsInitialSet() {
        seed();jdbc.update("INSERT INTO dcc_controlled_file_related_file VALUES(2,100,20,0)");
        jdbc.update("INSERT INTO dcc_current_file_relation VALUES(2,10,20)");
        jdbc.update("INSERT INTO dcc_current_file_relation_set VALUES(2,10,200,3)");
        initialize();
        assertEquals(100L,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=1",Long.class));
        assertEquals(200L,jdbc.queryForObject("SELECT controlled_file_id FROM dcc_current_file_relation_set WHERE tenant_id=2",Long.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation WHERE tenant_id=1",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_current_file_relation WHERE tenant_id=2",Integer.class));
    }
    @Test void initializerRequiresTheOriginalTransaction() {
        seed();assertThrows(DccRelationFailure.class,()->store.initializeControlledImportRelations(source,7L));
        assertEquals(0,count("dcc_current_file_relation_set"));verifyNoInteractions(audit);
    }
    @Test void packagedPolicyRegistersImportSetCreationWithoutInventingAControlledEvent() {
        var bundle=new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditPolicyBundleLoader().load();
        var operations=bundle.policyNode().path("operations");
        var matches=new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        operations.forEach(row->{if("dcc.relation.controlled-import".equals(row.path("operationId").asText()))matches.add(row);});
        assertEquals(1,matches.size());var operation=matches.get(0);
        assertEquals("DCC_RELATION_SET",operation.path("subjectType").asText());
        assertEquals("NONE",operation.path("signaturePolicy").asText());
        assertEquals("ABSENT_TO_PRESENT",operation.path("statePolicy").asText());
        assertEquals("cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore#initializeControlledImportRelations",
                operation.path("sourceLocators").get(0).asText());
    }
}
