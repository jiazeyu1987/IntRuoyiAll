package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.ApprovalHistoryBackfillMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalProcessOwnershipMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.*;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Synthetic unit fixtures only: no real historical provenance, JDBC transaction or MySQL acceptance is claimed. */
class ApprovalHistoryBackfillServiceTest {
    private ApprovalHistoryBackfillMapper inventory;
    private BpmApprovalProcessOwnershipMapper owners;
    private PermissionApi permission;
    private HistoryService history;
    private ProcessEngineConfiguration engine;
    private SqlSessionFactory sessions;
    private ApprovalHistoryBackfillService service;
    private ObjectMapper originalJsonMapper;
    private final Map<String,BpmApprovalProcessOwnershipDO> persisted=new HashMap<>();
    private final List<Map<String,Object>> logs=new ArrayList<>();

    @BeforeEach void syntheticSetup() {
        originalJsonMapper=JsonUtils.getObjectMapper();
        persisted.clear();logs.clear();
        inventory=mock(ApprovalHistoryBackfillMapper.class);owners=mock(BpmApprovalProcessOwnershipMapper.class);
        permission=mock(PermissionApi.class);history=mock(HistoryService.class,RETURNS_DEEP_STUBS);
        engine=mock(ProcessEngineConfiguration.class);sessions=mock(SqlSessionFactory.class);
        var ds=mock(DataSource.class);var config=new Configuration();
        config.setEnvironment(new Environment("synthetic-unit",new JdbcTransactionFactory(),ds));
        when(sessions.getConfiguration()).thenReturn(config);
        when(engine.getDataSource()).thenReturn(ds);when(engine.getDatabaseType()).thenReturn("mysql");
        when(engine.getDatabaseTablePrefix()).thenReturn("");
        when(permission.hasAnyRolesOrSuperAdmin(7L,"super_admin")).thenReturn(true);
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        service=new ApprovalHistoryBackfillService(inventory,owners,permission,history,engine,sessions,true);
        when(inventory.lockTenant(1L)).thenReturn(1L);
        when(owners.selectExact(eq(1L),anyString())).thenAnswer(a->persisted.get(a.getArgument(1)));
        when(owners.insert(any())).thenAnswer(a->{BpmApprovalProcessOwnershipDO r=a.getArgument(0);r.setId(100L+persisted.size());persisted.put(r.getProcessInstanceId(),r);return 1;});
        when(inventory.batchLog(1L,"AHB:unit_batch")).thenAnswer(a->List.copyOf(logs));
        when(inventory.audit(eq(1L),eq("AHB:unit_batch"),eq(7L),anyString(),anyString())).thenAnswer(a->{
            logs.add(row("sub_type",a.getArgument(3),"extra",a.getArgument(4)));return 1;});
        engineIdentity("BUSINESS_APPROVAL:11","shared-key");
        when(inventory.business(1L,"1")).thenReturn(List.of(business()));
    }
    @AfterEach void clearSyntheticContext() {
        JsonUtils.init(originalJsonMapper);
        TenantContextHolder.clear();TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test void verifiedFormalRequestProducesMigrationReportAndExactIdempotentReplay() {
        var inspected=service.inspect(7L,"unit_batch");assertEquals("READY",inspected.status());
        var applied=service.execute(7L,"unit_batch",inspected.inspectionDigest());
        assertEquals("APPLIED",applied.status());assertEquals("HISTORY_BACKFILL",applied.source());
        assertEquals(inspected.inventoryDigest(),applied.inventoryDigest());
        var owner=persisted.get("p1");assertEquals("BUSINESS_APPROVAL_SUBMIT",owner.getWriterCode());
        assertEquals(applied.migratedAt(),owner.getCreatedAt());
        assertEquals("11",owner.getAuthorityId());assertEquals("v1",owner.getAuthorityVersion());
        assertEquals(2,logs.size());assertTrue(logs.get(0).get("extra").toString().contains("INSERTED_HISTORY"));
        var replay=service.execute(7L,"unit_batch",inspected.inspectionDigest());
        assertEquals(applied.migratedAt(),replay.migratedAt());
        verify(owners,times(1)).insert(any());verify(inventory,times(2)).audit(anyLong(),anyString(),anyLong(),anyString(),anyString());
    }
    @Test void completeTenantUnknownTaskBlocksEveryWriteAndReportsOriginalStartMissing() {
        when(inventory.runtimeTasks("1")).thenReturn(List.of(task("p1"),task("p2")));
        when(inventory.engines("1")).thenReturn(List.of(identity("p1","BUSINESS_APPROVAL:11","shared-key"),identity("p2","42","shared-key")));
        var report=service.inspect(7L,"unit_batch");assertEquals("BLOCKED",report.status());
        assertTrue(report.findings().stream().anyMatch(f->f.reason().contains("ORIGINAL_NATIVE_START_PROOF_REQUIRED")));
        assertThrows(IllegalStateException.class,()->service.execute(7L,"unit_batch",report.inspectionDigest()));
        verify(owners,never()).insert(any());verify(inventory,never()).audit(anyLong(),anyString(),anyLong(),anyString(),anyString());
    }
    @Test void currentDccPointerAndBusinessRequestAreUnresolvedRatherThanGuessedConflictOrEffect() {
        when(inventory.dccPointers(1L,"1")).thenReturn(List.of(row("id",42L,"tenant_id",1L,"process_instance_id","p1","version_no","A")));
        var report=service.inspect(7L,"unit_batch");assertEquals("BLOCKED",report.status());
        assertTrue(report.findings().get(0).reason().contains("CONTROLLED_FILE_ORIGINAL_SUBMISSION_PROOF_REQUIRED"));
    }
    @Test void conflictingFormalRequestsAndForeignTenantClaimsBlock() {
        var duplicate=business();duplicate.put("id",12L);
        // Wrong key is already an explicit formal-source mismatch, never silently deduplicated.
        when(inventory.business(1L,"1")).thenReturn(List.of(business(),duplicate));
        assertEquals("BLOCKED",service.inspect(7L,"unit_batch").status());
        var foreign=business();foreign.put("tenant_id",2L);
        when(inventory.business(1L,"1")).thenReturn(List.of(foreign));
        assertTrue(service.inspect(7L,"unit_batch").findings().get(0).reason().contains("CROSS_TENANT"));
    }
    @Test void staleInspectionAndWrongIdentityCannotWrite() {
        var initial=service.inspect(7L,"unit_batch");var changed=business();changed.put("request_status","APPROVED");
        when(inventory.business(1L,"1")).thenReturn(List.of(changed));
        assertThrows(IllegalStateException.class,()->service.execute(7L,"unit_batch",initial.inspectionDigest()));
        verify(owners,never()).insert(any());
        var wrong=identity("p1","BUSINESS_APPROVAL:11","shared-key");wrong.put("definition_tenant","2");
        when(inventory.engines("1")).thenReturn(List.of(wrong));
        assertEquals("BLOCKED",service.inspect(7L,"unit_batch").status());
    }
    @Test void exactHistoricalFormUploadEffectDoesNotClaimNativeDccAuthority() {
        formUploadEffect();
        var report=service.inspect(7L,"unit_batch");assertEquals("READY",report.status());
        assertEquals("FORM_ACTION_SUBMIT",report.findings().get(0).authority().writerCode());
        when(inventory.dccTransitions(1L)).thenReturn(List.of());
        assertEquals("BLOCKED",service.inspect(7L,"unit_batch").status());
    }
    @Test void genuineExistingDccStartStillConflictsWithFormEffect() {
        formUploadEffect();var old=existingDcc("FORM_ACTION:FA1");persisted.put("p1",old);
        var report=service.inspect(7L,"unit_batch");assertEquals("BLOCKED",report.status());
        assertTrue(report.findings().get(0).reason().contains("EXISTING_OWNER_FORMAL_CONFLICT"));
    }
    @Test void overwrittenEdhrExecutionWithoutOriginalExactSnapshotRemainsBlocked() {
        var request=business();request.put("data_domain","MES");request.put("system_code","MES");
        request.put("object_type","EDHR_BATCH_EXECUTION");request.put("action_code","SUBMIT_REVIEW");
        request.put("business_context_json",context(request));
        when(inventory.business(1L,"1")).thenReturn(List.of(request));
        assertTrue(service.inspect(7L,"unit_batch").findings().get(0).reason().contains("EDHR_EXACT_SUBMIT_OBJECT_EVIDENCE_REQUIRED"));
    }
    @Test void registrationCandidatesRequireExactEngineAndBindingSnapshots() {
        registration("[8,9]",List.of(8L,9L));var ready=service.inspect(7L,"unit_batch");
        assertEquals("READY",ready.status());assertEquals(List.of(8L,9L),ready.findings().get(0).candidates());
        registration("[8,8]",List.of(8L,9L));
        assertTrue(service.inspect(7L,"unit_batch").findings().get(0).reason().contains("DUPLICATE"));
        registration("[8,9]",List.of(8L,10L));
        assertTrue(service.inspect(7L,"unit_batch").findings().get(0).reason().contains("SELECTION_MISMATCH"));
        registration("[8.5]",List.of(8L));assertEquals("BLOCKED",service.inspect(7L,"unit_batch").status());
    }
    @Test void wrongReadbackCannotReportApplied() {
        var inspected=service.inspect(7L,"unit_batch");
        doAnswer(a->{BpmApprovalProcessOwnershipDO r=a.getArgument(0);r.setId(100L);r.setTenantId(2L);persisted.put("p1",r);return 1;})
                .when(owners).insert(any());
        assertThrows(IllegalStateException.class,()->service.execute(7L,"unit_batch",inspected.inspectionDigest()));
        assertTrue(logs.isEmpty());
    }
    @Test void changedTaskUniverseCannotReportApplied() {
        var initial=service.inspect(7L,"unit_batch");
        when(inventory.runtimeTasks("1")).thenReturn(List.of(task("p1")),List.of(task("p2")));
        assertThrows(IllegalStateException.class,()->service.execute(7L,"unit_batch",initial.inspectionDigest()));
        assertTrue(logs.stream().noneMatch(r->"SUMMARY".equals(r.get("sub_type"))));
    }
    @Test void springTransactionBoundaryRollsBackSyntheticStoreWhenAuditFails() {
        var inspected=service.inspect(7L,"unit_batch");
        when(inventory.audit(eq(1L),eq("AHB:unit_batch"),eq(7L),anyString(),anyString())).thenReturn(0);
        var transactionManager=new SyntheticTransactionManager();
        var factory=new ProxyFactory(service);factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(transactionManager,new AnnotationTransactionAttributeSource()));
        var transactional=(ApprovalHistoryBackfillService)factory.getProxy();
        TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThrows(IllegalStateException.class,()->transactional.execute(7L,"unit_batch",inspected.inspectionDigest()));
        assertTrue(transactionManager.rolledBack);assertTrue(persisted.isEmpty());assertTrue(logs.isEmpty());
        verify(owners).insert(any());
        // This proves Spring interceptor rollback against a synthetic unit resource, not JDBC/MySQL atomicity.
    }
    @Test void authorizationDefaultOffAndDifferentDatasourceFailFast() {
        var disabled=new ApprovalHistoryBackfillService(inventory,owners,permission,history,engine,sessions,false);
        assertThrows(IllegalStateException.class,()->disabled.inspect(7L,"unit_batch"));
        assertThrows(IllegalStateException.class,()->service.inspect(8L,"unit_batch"));
        when(engine.getDataSource()).thenReturn(mock(DataSource.class));
        assertThrows(IllegalStateException.class,()->service.inspect(7L,"unit_batch"));
        verify(owners,never()).insert(any());
    }
    @Test void executionDeclaresPhysicalSerializableRollbackAndNoStartupEntry() throws Exception {
        var tx=ApprovalHistoryBackfillService.class.getMethod("execute",Long.class,String.class,String.class).getAnnotation(Transactional.class);
        assertEquals(Isolation.SERIALIZABLE,tx.isolation());assertArrayEquals(new Class<?>[]{Exception.class},tx.rollbackFor());
        // Annotation checks describe the contract; this fixture does not prove actual MySQL rollback/isolation.
        for(var method:ApprovalHistoryBackfillService.class.getDeclaredMethods())
            assertFalse(method.isAnnotationPresent(org.springframework.scheduling.annotation.Scheduled.class));
    }
    @Test void exactSyntheticEdhrRequestExecutionAndSnapshotResolveAndExecute() {
        when(inventory.business(1L,"1")).thenReturn(List.of(edhrRequest()));
        var inspected=service.inspect(7L,"unit_batch");assertEquals("READY",inspected.status());
        var authority=inspected.findings().get(0).authority();
        assertEquals("NATIVE_EXTERNAL_EDHR",authority.projectionCode());
        assertEquals("EDHR_BUSINESS_APPROVAL_SUBMIT",authority.writerCode());
        assertEquals("BUSINESS_REQUEST",authority.authorityType());assertEquals("11",authority.authorityId());
        assertEquals("3",authority.authorityVersion());
        var applied=service.execute(7L,"unit_batch",inspected.inspectionDigest());assertEquals("APPLIED",applied.status());
        assertEquals("3",persisted.get("p1").getAuthorityVersion());
        assertEquals("EDHR_BUSINESS_APPROVAL_SUBMIT",persisted.get("p1").getWriterCode());
        assertEquals(applied.migratedAt(),persisted.get("p1").getCreatedAt());
    }
    @Test void syntheticEdhrWrongRevisionBlocksTenantBeforeWrites() {
        var request=edhrRequest();request.put("execution_revision",4);
        when(inventory.business(1L,"1")).thenReturn(List.of(request));
        var report=service.inspect(7L,"unit_batch");assertEquals("BLOCKED",report.status());
        assertTrue(report.findings().get(0).reason().contains("EDHR_EXACT_SUBMIT_OBJECT_EVIDENCE_REQUIRED"));
        assertThrows(IllegalStateException.class,()->service.execute(7L,"unit_batch",report.inspectionDigest()));
        verify(owners,never()).insert(any());verify(inventory,never()).audit(anyLong(),anyString(),anyLong(),anyString(),anyString());
    }
    @Test void syntheticEdhrWrongExecutionOrSnapshotProcessBlocksTenantBeforeWrites() {
        for(String field:List.of("execution_process","execution_snapshot_process")) {
            var request=edhrRequest();request.put(field,"older-process");
            when(inventory.business(1L,"1")).thenReturn(List.of(request));
            var report=service.inspect(7L,"unit_batch");assertEquals("BLOCKED",report.status());
            assertTrue(report.findings().get(0).reason().contains("EDHR_EXACT_SUBMIT_OBJECT_EVIDENCE_REQUIRED"));
            assertThrows(IllegalStateException.class,()->service.execute(7L,"unit_batch",report.inspectionDigest()));
        }
        verify(owners,never()).insert(any());verify(inventory,never()).audit(anyLong(),anyString(),anyLong(),anyString(),anyString());
    }
    @Test void registrationExecutePersistsExactCandidatesAndReplayDoesNotReinsert() {
        registration("[9,8]",List.of(8L,9L));
        var selectedUsers=new TreeSet<Long>();
        doAnswer(a->List.copyOf(selectedUsers)).when(owners).selectSelectedCandidates(1L,"p1");
        doAnswer(a->selectedUsers.add(a.getArgument(2))?1:0).when(owners).insertSelectedCandidate(eq(1L),eq("p1"),anyLong());
        var inspected=service.inspect(7L,"unit_batch");assertEquals("READY",inspected.status());
        var applied=service.execute(7L,"unit_batch",inspected.inspectionDigest());assertEquals("APPLIED",applied.status());
        assertEquals(List.of(8L,9L),List.copyOf(selectedUsers));
        assertEquals(List.of(8L,9L),applied.findings().get(0).candidates());
        assertEquals("REGISTRATION_CERTIFICATE_SUBMIT",persisted.get("p1").getWriterCode());
        assertEquals("51",persisted.get("p1").getAuthorityId());
        var replay=service.execute(7L,"unit_batch",inspected.inspectionDigest());assertEquals(applied.migratedAt(),replay.migratedAt());
        verify(owners,times(1)).insert(any());
        verify(owners,times(1)).insertSelectedCandidate(1L,"p1",8L);
        verify(owners,times(1)).insertSelectedCandidate(1L,"p1",9L);
    }

    @Test void productSerializerLargeBusinessTenantAndApplicantResolveExactly() {
        useProductSerializer();
        for(long id:List.of(9007199254740991L,9007199254740992L,Long.MAX_VALUE)) {
            var request=serializedBusiness(id,id);
            scope(id,id,"BUSINESS_APPROVAL:11");
            when(inventory.business(id,Long.toString(id))).thenReturn(List.of(request));
            var context=JsonUtils.parseTree((String)request.get("business_context_json"));
            assertTrue(context.path("tenantId").isTextual());assertTrue(context.path("applicantUserId").isTextual());
            var report=service.inspect(7L,"unit_batch");assertEquals("READY",report.status());
            assertEquals(id,report.findings().get(0).authority().tenantId().longValue());
            assertEquals("BUSINESS_APPROVAL_SUBMIT",report.findings().get(0).authority().writerCode());
        }
    }
    @Test void productSerializerLargeBusinessApplicantAtOrdinaryTenantResolvesExactly() {
        useProductSerializer();var request=serializedBusiness(1L,Long.MAX_VALUE);
        scope(1L,Long.MAX_VALUE,"BUSINESS_APPROVAL:11");
        when(inventory.business(1L,"1")).thenReturn(List.of(request));
        assertTrue(JsonUtils.parseTree((String)request.get("business_context_json")).path("applicantUserId").isTextual());
        assertEquals("READY",service.inspect(7L,"unit_batch").status());
    }
    @Test void productSerializerLargeFormTenantAndSubmitSnapshotResolveExactly() {
        useProductSerializer();
        for(long tenant:List.of(9007199254740991L,9007199254740992L,Long.MAX_VALUE)) {
            var form=serializedForm(tenant);scope(tenant,7L,"FORM_ACTION:FA1");
            when(inventory.business(tenant,Long.toString(tenant))).thenReturn(List.of());
            when(inventory.forms(tenant,Long.toString(tenant))).thenReturn(List.of(form));
            assertTrue(JsonUtils.parseTree((String)form.get("submit_context_json")).path("tenantId").isTextual());
            var report=service.inspect(7L,"unit_batch");assertEquals("READY",report.status());
            assertEquals(tenant,report.findings().get(0).authority().tenantId().longValue());
            assertEquals("FORM_ACTION_SUBMIT",report.findings().get(0).authority().writerCode());
        }
    }
    @Test void businessContextLongRejectsOverflowNoncanonicalAndMissingWithoutWrites() {
        useProductSerializer();
        for(String field:List.of("tenantId","applicantUserId")) {
            String reason=field.equals("tenantId")?"SOURCE_CONTEXT_TENANT_MISMATCH":"BUSINESS_REQUEST_ORIGINAL_APPLICANT_MISMATCH";
            for(String raw:invalidJsonLongValues()) {
                var request=serializedBusiness(1L,7L);
                var context=(ObjectNode)JsonUtils.parseTree((String)request.get("business_context_json"));
                context.set(field,JsonUtils.parseTree(raw));request.put("business_context_json",JsonUtils.toJsonString(context));
                when(inventory.business(1L,"1")).thenReturn(List.of(request));assertBlockedBeforeWrites(reason);
            }
            var request=serializedBusiness(1L,7L);
            var context=(ObjectNode)JsonUtils.parseTree((String)request.get("business_context_json"));context.remove(field);
            request.put("business_context_json",JsonUtils.toJsonString(context));
            when(inventory.business(1L,"1")).thenReturn(List.of(request));assertBlockedBeforeWrites(reason);
        }
        verify(owners,never()).insert(any());verify(inventory,never()).audit(anyLong(),anyString(),anyLong(),anyString(),anyString());
    }
    @Test void formContextLongRejectsOverflowNoncanonicalAndMissingWithoutWrites() {
        useProductSerializer();scope(1L,7L,"FORM_ACTION:FA1");when(inventory.business(1L,"1")).thenReturn(List.of());
        for(String raw:invalidJsonLongValues()) {
            var form=serializedForm(1L);
            var context=(ObjectNode)JsonUtils.parseTree((String)form.get("business_context_json"));
            context.set("tenantId",JsonUtils.parseTree(raw));String json=JsonUtils.toJsonString(context);
            form.put("business_context_json",json);form.put("submit_context_json",json);
            when(inventory.forms(1L,"1")).thenReturn(List.of(form));assertBlockedBeforeWrites("SOURCE_CONTEXT_TENANT_MISMATCH");
        }
        var form=serializedForm(1L);
        var context=(ObjectNode)JsonUtils.parseTree((String)form.get("business_context_json"));context.remove("tenantId");
        String json=JsonUtils.toJsonString(context);form.put("business_context_json",json);form.put("submit_context_json",json);
        when(inventory.forms(1L,"1")).thenReturn(List.of(form));assertBlockedBeforeWrites("SOURCE_CONTEXT_TENANT_MISMATCH");
        verify(owners,never()).insert(any());verify(inventory,never()).audit(anyLong(),anyString(),anyLong(),anyString(),anyString());
    }
    @Test void largeLongParsingStillRequiresExactTenantApplicantAndEngineIdentity() {
        useProductSerializer();long id=Long.MAX_VALUE;scope(id,id,"BUSINESS_APPROVAL:11");
        for(String field:List.of("tenantId","applicantUserId")) {
            var request=serializedBusiness(id,id);
            var context=(ObjectNode)JsonUtils.parseTree((String)request.get("business_context_json"));
            context.put(field,Long.toString(id-1));request.put("business_context_json",JsonUtils.toJsonString(context));
            when(inventory.business(id,Long.toString(id))).thenReturn(List.of(request));
            var report=service.inspect(7L,"unit_batch");assertEquals("BLOCKED",report.status());
            assertTrue(report.findings().get(0).reason().contains(field.equals("tenantId")
                    ?"SOURCE_CONTEXT_TENANT_MISMATCH":"BUSINESS_REQUEST_ORIGINAL_APPLICANT_MISMATCH"));
        }
        when(inventory.business(id,Long.toString(id))).thenReturn(List.of(serializedBusiness(id,id)));
        scope(id,id-1,"BUSINESS_APPROVAL:11");
        assertTrue(service.inspect(7L,"unit_batch").findings().get(0).reason().contains("BUSINESS_REQUEST_ORIGINAL_APPLICANT_MISMATCH"));
        verify(owners,never()).insert(any());
    }

    private void useProductSerializer() {
        // Actual production module and JsonUtils writer; synthetic IDs do not claim database provenance.
        JsonUtils.init(originalJsonMapper.copy().registerModule(new YudaoJacksonAutoConfiguration().timestampSupportModuleBean()));
    }
    private void scope(Long tenant,Long applicant,String businessKey) {
        TenantContextHolder.setTenantId(tenant);String tenantText=tenant.toString();
        var process=identity("p1",businessKey,"shared-key");process.put("engine_tenant",tenantText);
        process.put("definition_tenant",tenantText);process.put("start_user",applicant.toString());
        var task=task("p1");task.put("TENANT_ID_",tenantText);
        when(inventory.engines(tenantText)).thenReturn(List.of(process));
        when(inventory.runtimeTasks(tenantText)).thenReturn(List.of(task));
    }
    private Map<String,Object> serializedBusiness(Long tenant,Long applicant) {
        var r=business();r.put("tenant_id",tenant);r.put("applicant_user_id",applicant);
        r.put("business_context_json",JsonUtils.toJsonString(BusinessApprovalContext.builder().tenantId(tenant)
                .dataDomain("DCC").systemCode("DCC").objectType("FORM_TEMPLATE").objectId("91").objectVersion("v1")
                .actionCode("UPGRADE").objectState("DRAFT").applicantUserId(applicant).build()));return r;
    }
    private Map<String,Object> serializedForm(Long tenant) {
        var r=business();r.put("tenant_id",tenant);r.put("id",21L);r.put("bpm_process_instance_id","p1");r.put("instance_code","FA1");
        String context=JsonUtils.toJsonString(BusinessActionContext.builder().tenantId(tenant).dataDomain("DCC")
                .systemCode("DCC").objectType("FORM_TEMPLATE").objectId("91").objectVersion("v1")
                .actionCode("UPGRADE").objectState("DRAFT").build());
        r.put("business_context_json",context);r.put("submit_context_json",context);r.put("submit_snapshot_id",31L);
        r.put("submit_form_json","{}");variable("formInstanceId",21L);variable("objectVersion","v1");variable("objectId","91");return r;
    }
    private void assertBlockedBeforeWrites(String reason) {
        var report=service.inspect(7L,"unit_batch");assertEquals("BLOCKED",report.status());
        assertTrue(report.findings().get(0).reason().contains(reason));
        assertThrows(IllegalStateException.class,()->service.execute(7L,"unit_batch",report.inspectionDigest()));
    }
    private static List<String> invalidJsonLongValues() {
        return List.of("9223372036854775808","-9223372036854775809","7.0","1e0","null","true","{}","[]",
                "\"9223372036854775808\"","\"-9223372036854775809\"","\"01\"","\" 1\"","\"1 \"",
                "\"+1\"","\"-0\"","\"1e0\"","\"1.0\"","\"\"");
    }

    private void engineIdentity(String key,String definition) {
        when(inventory.engines("1")).thenReturn(List.of(identity("p1",key,definition)));
        when(inventory.runtimeTasks("1")).thenReturn(List.of(task("p1")));
    }
    private Map<String,Object> business() {
        var r=row("id",11L,"tenant_id",1L,"process_instance_id","p1","process_definition_key","shared-key",
                "data_domain","DCC","system_code","DCC","object_type","FORM_TEMPLATE","object_id","91",
                "object_version","v1","action_code","UPGRADE","object_state","DRAFT","applicant_user_id",7L);
        r.put("business_context_json",context(r));return r;
    }
    private String context(Map<String,Object> r) {
        return JsonUtils.toJsonString(row("tenantId",1L,"dataDomain",r.get("data_domain"),"systemCode",r.get("system_code"),
                "objectType",r.get("object_type"),"objectId",r.get("object_id"),"objectVersion",r.get("object_version"),
                "actionCode",r.get("action_code"),"objectState",r.get("object_state"),"applicantUserId",7L));
    }
    private Map<String,Object> edhrRequest() {
        var request=business();request.put("data_domain","MES");request.put("system_code","MES");
        request.put("object_type","EDHR_BATCH_EXECUTION");request.put("action_code","SUBMIT_REVIEW");
        request.put("object_version","3");request.put("business_context_json",context(request));
        request.put("execution_id",91L);request.put("execution_tenant",1L);request.put("execution_revision",3);
        request.put("execution_process","p1");request.put("execution_definition_key","shared-key");
        request.put("execution_snapshot_id",71L);request.put("execution_snapshot_process","p1");
        request.put("execution_snapshot_definition","def1");request.put("execution_snapshot_key","shared-key");
        return request;
    }
    private void formUploadEffect() {
        engineIdentity("FORM_ACTION:FA1","shared-key");when(inventory.business(1L,"1")).thenReturn(List.of());
        var f=business();f.put("id",21L);f.put("bpm_process_instance_id","p1");f.put("instance_code","FA1");
        f.put("object_type","CONTROLLED_FILE");f.put("action_code","UPLOAD");f.put("idempotency_key","effect21");
        f.put("business_context_json",context(f));f.put("submit_context_json",context(f));
        f.put("submit_snapshot_id",31L);f.put("submit_form_json","{\"versionNo\":\"A\"}");
        when(inventory.forms(1L,"1")).thenReturn(List.of(f));
        variable("formInstanceId",21L);variable("objectVersion","v1");variable("objectId","91");
        when(inventory.dccPointers(1L,"1")).thenReturn(List.of(row("tenant_id",1L,"id",42L,"process_instance_id","p1","version_no","A")));
        when(inventory.effects(1L)).thenReturn(List.of(row("bpm_process_instance_id","p1","instance_id",21L,"status","APPLIED",
                "result_ref","42","idempotency_key","effect21","execution_code","EFFECT-effect21")));
        when(inventory.dccTransitions(1L)).thenReturn(List.of(row("native_version_id",42L,"version_no","A",
                "approval_process_instance_id","p1","action","APPROVE","event_key","effect21:approve")));
    }
    private void registration(String selectedJson,List<Long> engineCandidates) {
        engineIdentity("DCC_REG_CERT_ACCESS:51","dcc-registration-certificate-access");
        when(inventory.business(1L,"1")).thenReturn(List.of());
        when(inventory.registrations(1L,"1")).thenReturn(List.of(row("tenant_id",1L,"id",61L,"request_id",51L,
                "request_tenant",1L,"request_process_id","p1","bpm_process_instance_id","p1","request_key","rk51",
                "business_key","DCC_REG_CERT_ACCESS:51","detail_json","{\"candidateUserIds\":"+selectedJson+"}")));
        variable("PROCESS_START_USER_SELECT_ASSIGNEES",Map.of("REG_CERT_ACCESS_APPROVAL",engineCandidates));
    }
    private void variable(String name,Object value) {
        var variable=mock(HistoricVariableInstance.class);when(variable.getValue()).thenReturn(value);
        when(history.createHistoricVariableInstanceQuery().processInstanceId("p1").variableName(name).list()).thenReturn(List.of(variable));
    }
    private BpmApprovalProcessOwnershipDO existingDcc(String key) {
        var r=new BpmApprovalProcessOwnershipDO();r.setId(1L);r.setTenantId(1L);r.setProcessInstanceId("p1");
        r.setProcessDefinitionId("def1");r.setProcessDefinitionKey("shared-key");r.setBusinessKey(key);
        r.setProjectionCode("DCC_TASK");r.setAuthorityType("CONTROLLED_FILE");r.setAuthorityId("42");r.setAuthorityVersion("A");
        r.setWriterCode("DCC_CONTROLLED_FILE_SUBMIT");r.setCreatedAt(LocalDateTime.of(2020,1,1,0,0));
        r.setEvidenceDigest(ApprovalProcessOwnershipService.digest(r));return r;
    }
    private static Map<String,Object> identity(String process,String key,String definition) {
        return row("process_id",process,"definition_id","def1","definition_key",definition,"business_key",key,
                "engine_tenant","1","definition_tenant","1","start_user","7","scope","HISTORY");
    }
    private static Map<String,Object> task(String process) {return row("ID_","task-"+process,"PROC_INST_ID_",process,"PROC_DEF_ID_","def1","TENANT_ID_","1");}
    private static Map<String,Object> row(Object... pairs) {var values=new TreeMap<String,Object>();for(int i=0;i<pairs.length;i+=2)values.put((String)pairs[i],pairs[i+1]);return values;}
    private final class SyntheticTransactionManager extends AbstractPlatformTransactionManager {
        private boolean rolledBack;
        private Map<String,BpmApprovalProcessOwnershipDO> originalOwners;
        private List<Map<String,Object>> originalLogs;
        @Override protected Object doGetTransaction() {return new Object();}
        @Override protected void doBegin(Object transaction,TransactionDefinition definition) {
            originalOwners=new HashMap<>(persisted);originalLogs=new ArrayList<>(logs);
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            rolledBack=true;persisted.clear();persisted.putAll(originalOwners);logs.clear();logs.addAll(originalLogs);
        }
    }
}
