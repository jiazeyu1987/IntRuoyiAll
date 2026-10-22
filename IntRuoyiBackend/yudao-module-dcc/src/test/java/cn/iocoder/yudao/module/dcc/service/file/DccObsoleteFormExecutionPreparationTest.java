package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.formcenter.model.*;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormExactBpmSubmissionAdapter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.nio.file.*;
import jakarta.annotation.Resource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual DCC raw configuration/definition/HMAC in private H2 and real engine; organizational
 * read, enabled directories and action permission remain explicit external ports, never registered production providers. */
class DccObsoleteFormExecutionPreparationTest extends DccNewActionBaselinePreparationDatabaseTest {
    @Resource org.mybatis.spring.SqlSessionTemplate sql;
    DccObsoleteFormExecutionPreparationService source;
    DccDirectSupervisorSource organization;
    cn.iocoder.yudao.module.system.api.dept.DeptApi departments;
    String obsoleteId;
    String draftJson;
    FormActionInstance request;
    @BeforeEach void clearTaskOwnedFormBeforeSource(){clearTaskOwnedForm(jdbc);}
    @AfterEach void clearTaskOwnedFormAfterSource(){clearTaskOwnedForm(jdbc);}
    /** These two fixtures create form501 themselves; the DCC shared cleaner does not own this BPM table.
     * Remove only their exact private row, including its intentional FORGED-code mutation. */
    static void clearTaskOwnedForm(org.springframework.jdbc.core.JdbcTemplate database){
        boolean exists=database.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)connection->{
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"),"task-owned form cleanup must use isolated H2");
            try(var tables=connection.getMetaData().getTables(null,"PUBLIC","bpm_form_action_instance",new String[]{"TABLE"})){return tables.next();}
        });
        if(!exists)return; // The private case may not have created its table yet; no SQL error is suppressed.
        int removed=database.update("DELETE FROM bpm_form_action_instance WHERE id=501 AND tenant_id=1 AND applicant_user_id=99 "
                +"AND system_code='DCC' AND object_type='CONTROLLED_FILE' AND object_id='20' AND object_version='A/1' AND action_code='OBSOLETE' "
                +"AND status='COMPLETED' AND instance_code IN ('ACTUAL-FORM-CODE','FORGED','ACTUAL-CODE') AND deleted=0");
        assertTrue(removed==0||removed==1,"only the single task-owned form may be removed");
    }
    @BeforeEach void sourceFixture(org.junit.jupiter.api.TestInfo info)throws Exception{
        if(info.getTestMethod().orElseThrow().getDeclaringClass()!=DccObsoleteFormExecutionPreparationTest.class)return;
        authenticate(99L);
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
        source=new DccObsoleteFormExecutionPreparationService();
        var config=mock(DccApprovalMatrixTokenProperties.class);when(config.requireSigningKey()).thenReturn("ic26-source-private-preparation-hmac-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var provider=mock(org.springframework.beans.factory.ObjectProvider.class);organization=mock(DccDirectSupervisorSource.class);
        when(provider.getIfAvailable()).thenReturn(organization);
        when(organization.require(any())).thenAnswer(c->{var input=c.<DccDirectSupervisorSource.Context>getArgument(0);return new DccDirectSupervisorSource.Evidence(input.tenantId(),input.requesterId(),99L,"ISOLATED_AUTHORITATIVE_PORT","test-source-identity","source-v1");});
        var users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);when(users.getUser(anyLong())).thenAnswer(c->new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(c.getArgument(0)).setTenantId(1L).setStatus(0).setUsername("formal-user").setNickname("真实候选").setPostIds(Set.of(10L)));
        when(users.getUserList(any())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(users::getUser).toList());
        departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);when(departments.getDeptList(any())).thenReturn(List.of(new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(51L).setStatus(0).setName("正式会签部门").setLeaderUserId(99L)));
        var routeResolver=new DccControlledFileApprovalRouteAssigneeResolver();wire(routeResolver,"deptApi",departments,"adminUserApi",users);
        var readonly=new DccControlledFileRouteReadinessService();
        var auth=mock(DccElectronicSignatureAuthorizationService.class);when(auth.getAuthorizationMap(any())).thenAnswer(c->{var result=new HashMap<Long,Boolean>();for(Long id:c.<Collection<Long>>getArgument(0))result.put(id,true);return result;});
        var signatureImages=mock(DccElectronicSignatureImageService.class);when(signatureImages.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(10L).fileId(10L).verifiedStatus("VALID").build());
        var actionPermissions=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(actionPermissions.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);
        wire(readonly,"adminUserApi",users,"permissionApi",actionPermissions,"signatureAuthorizationService",auth,"signatureImageService",signatureImages);
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        obsoleteId=deploy(Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/obsolete.bpmn20.xml")));
        when(definitions.getActiveProcessDefinition(DccControlledFileProcessDefinitionKeys.OBSOLETE)).thenAnswer(c->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(obsoleteId).singleResult());
        wire(contracts,"definitions",definitions);
        for(var name:List.of("files","masters","projects","projectAccess","categoryPermissions","sources","tokens","executionContracts","routeResolver","readiness","supervisors","users","signing"))
            if(Arrays.stream(source.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals(name)))wire(source,name,switch(name){
                case "files"->files;case "masters"->masters;case "projects"->projects;case "projectAccess"->access;case "categoryPermissions"->permissions;case "sources"->matrixSources;
                case "tokens"->tokens;case "executionContracts"->contracts;case "routeResolver"->routeResolver;case "readiness"->readonly;case "supervisors"->provider;case "users"->users;case "signing"->config;default->throw new AssertionError(name);});
        var proof=new DccObsoleteFormPreparationProof();wire(proof,"signing",config);wire(source,"proof",proof);
        var context=BusinessActionContext.builder().tenantId(1L).dataDomain("DCC").systemCode("DCC").objectType("CONTROLLED_FILE").objectId("20").objectVersion("A/1").actionCode("OBSOLETE").objectState("ACTIVE").build();
        var policy=FormActionPolicy.builder().policyId(10L).tenantId(1L).dataDomain("DCC").systemCode("DCC").objectType("CONTROLLED_FILE").actionCode("OBSOLETE").objectState("ACTIVE")
                .policyType(FormPolicyType.NONE).approvalMode(FormApprovalMode.BPM_REQUIRED).bpmProcessKey(DccControlledFileProcessDefinitionKeys.OBSOLETE).effectExecutorCode("DCC_OBSOLETE").status(FormActionPolicy.STATUS_PUBLISHED).build();
        request=new FormActionInstance("ACTUAL-FORM-CODE",FormActionResolution.from(policy),context,99L,"actual-obsolete-form-key");
        request.setFormData(Map.of("controlledFileId","20","fileCode","PREPARED-1","fileVersion","A/1","reason","本次正式作废原因","dccSignoffDepartmentIds",List.of("51")));
        draftJson=JsonUtils.toJsonString(request.getFormData());
    }
    void authenticate(Long actor){var login=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(actor).setTenantId(1L).setUserType(2);org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null));}
    @AfterEach void clearSourceAuthentication(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    FormExactBpmSubmissionAdapter.PersistedDraft persisted(){return new FormExactBpmSubmissionAdapter.PersistedDraft(1L,501L,"ACTUAL-FORM-CODE",99L,"DRAFT",null,draftJson);}
    DccApprovalMatrixBaselineService.Expectations expected(){var baseline=matrixSources.read(2L);String key="DCC_OBSOLETE:501:ACTUAL-FORM-CODE";
        String token=tokens.issueBaseline(new DccApprovalMatrixTokenService.Context(1L,99L,5L,2L,6L,"OBSOLETE",20L,key),baseline.canonical());
        return new DccApprovalMatrixBaselineService.Expectations(baseline.baseline().getRouteId(),baseline.baseline().getRouteVersionNo(),baseline.baseline().getMatrixDigest(),token,null,false,key);}
    String prepare(){return tx().execute(s->source.prepareForDraft(persisted(),request,99L,expected()));}
    @Test void serverPreparationReturnsCanonicalIndependentFormAndFrozenServerCandidates(){
        var before=original(20L);String saved=prepare();draftJson=saved;request.setFormData(JsonUtils.parseObject(saved,Map.class));
        var result=tx().execute(s->source.verifyExactSubmission(persisted(),request,99L));
        assertEquals(obsoleteId,result.processDefinitionId());assertEquals(DccControlledFileProcessDefinitionKeys.OBSOLETE,result.processDefinitionKey());assertEquals(saved,result.formDataJson());
        assertEquals(List.of(99L),result.startUserSelectAssignees().get("SUPERVISOR_REVIEW"));assertEquals(List.of(99L),result.startUserSelectAssignees().get("MATRIX_REVIEW"));assertEquals(List.of(99L),result.approveUserSelectAssignees().get("MATRIX_APPROVAL"));
        assertFalse(result.startUserSelectAssignees().containsKey("DOC_CONTROL_REVIEW"));assertFalse(result.approveUserSelectAssignees().containsKey("TRAINING"));assertEquals(before,original(20L));assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @ParameterizedTest @ValueSource(strings={"ACTOR","TENANT","FORM_CODE","OBJECT_VERSION","PREPARATION_ID","RAW","RUNTIME","SUPERVISOR","TOKEN","MISSING","NO_PROVIDER","INACTIVE_SOURCE","PERMISSION","AUTHENTICATED_ACTOR","MASTER_PROJECT","MASTER_TAXONOMY"})
    void malformedOrUnavailablePreparationCannotBeTrusted(String violation){
        String saved=prepare();draftJson=saved;request.setFormData(JsonUtils.parseObject(saved,Map.class));
        var persisted=persisted();Long actor=99L;
        switch(violation){
            case "ACTOR"->actor=88L;case "TENANT"->persisted=new FormExactBpmSubmissionAdapter.PersistedDraft(2L,501L,"ACTUAL-FORM-CODE",99L,"DRAFT",null,saved);
            case "FORM_CODE"->persisted=new FormExactBpmSubmissionAdapter.PersistedDraft(1L,501L,"DIFFERENT",99L,"DRAFT",null,saved);
            case "INACTIVE_SOURCE"->jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
            case "PERMISSION"->when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(false);
            case "AUTHENTICATED_ACTOR"->authenticate(88L);case "MASTER_PROJECT"->jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=6 WHERE id=10");case "MASTER_TAXONOMY"->jdbc.update("UPDATE dcc_controlled_file_master SET file_type_taxonomy_leaf_id=7 WHERE id=10");
            case "NO_PROVIDER"->{var provider=(org.springframework.beans.factory.ObjectProvider<DccDirectSupervisorSource>)org.springframework.test.util.ReflectionTestUtils.getField(source,"supervisors");when(provider.getIfAvailable()).thenReturn(null);}
            default->{var data=JsonUtils.parseObject(saved,Map.class);var preparation=(Map<String,Object>)data.get("dccExecutionPreparation");
                switch(violation){case "OBJECT_VERSION"->preparation.put("controlledFileVersionNo","A/2");case "PREPARATION_ID"->preparation.put("preparedProcessDefinitionId",exactId);case "RAW"->preparation.put("sourceApprovalMatrixJson","{}");case "RUNTIME"->preparation.put("runtimeApprovalNodesJson","{}");case "SUPERVISOR"->preparation.put("supervisorProvenance",Map.of("supervisorUserId","88"));case "TOKEN"->preparation.put("authorizationToken","invalid");case "MISSING"->data.remove("dccExecutionPreparation");}
                request.setFormData(data);}
        }
        var finalPersisted=persisted;var finalActor=actor;var before=original(20L);assertThrows(RuntimeException.class,()->tx().execute(s->source.verifyExactSubmission(finalPersisted,request,finalActor)));assertEquals(before,original(20L));assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @Test void absentProductionSupervisorProviderProducesExplicitNotReadyBeforeAnyPreparation(){
        var provider=(org.springframework.beans.factory.ObjectProvider<DccDirectSupervisorSource>)org.springframework.test.util.ReflectionTestUtils.getField(source,"supervisors");if(provider!=null)when(provider.getIfAvailable()).thenReturn(null);
        var before=original(20L);var error=assertThrows(IllegalStateException.class,this::prepare);assertTrue(error.getMessage().contains("SOURCE")||error.getMessage().contains("SUPERVISOR"));assertEquals(before,original(20L));
    }
    @Test void changedDepartmentLeaderCannotReplaceFrozenServerMapOrCauseCurrentConfigLookup(){
        String saved=prepare();draftJson=saved;request.setFormData(JsonUtils.parseObject(saved,Map.class));
        when(departments.getDeptList(any())).thenReturn(List.of(new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(51L).setStatus(0).setName("同部门新负责人").setLeaderUserId(88L)));
        var result=tx().execute(s->source.verifyExactSubmission(persisted(),request,99L));assertEquals(List.of(99L),result.startUserSelectAssignees().get("MATRIX_REVIEW"));
    }
    @Test void crossProjectMasterIsRejectedBeforeSupervisorReadOrPreparationHmac(){
        jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=6,latest_controlled_file_id=20 WHERE id=10");var before=original(20L);
        assertThrows(RuntimeException.class,this::prepare);verify(organization,never()).require(any());assertEquals(before,original(20L));assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @Test void adapterRejectsUnboundHistoricalV1AsNewSubmissionAndRetainsPreflightContract(){
        String saved=prepare();draftJson=saved;request.setFormData(JsonUtils.parseObject(saved,Map.class));
        var obsolete=mock(DccControlledFileObsoleteService.class);var adapter=new DccControlledFileObsoleteFormEffectExecutor(obsolete);wire(adapter,"executionPreparation",source);
        assertTrue(adapter.preflight(request).isPassed());
        var rejected=assertThrows(IllegalArgumentException.class,()->tx().execute(s->adapter.verifyExactSubmission(persisted(),request,99L)));assertEquals("DCC_OBSOLETE_RESERVED_ATTRIBUTE_PROOF_REQUIRED",rejected.getMessage());verify(obsolete).precheckObsoleteControlledFile(eq(99L),eq(20L),any());assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
    @Test void recordedObsoleteRoundReadsOnlyItsActualFormServerProofAndIndependentFormIdentity(){
        String saved=prepare();
        jdbc.execute("CREATE TABLE IF NOT EXISTS bpm_form_action_instance(id BIGINT PRIMARY KEY,instance_code VARCHAR(128),tenant_id BIGINT,policy_id BIGINT,applicant_user_id BIGINT,status VARCHAR(64),data_domain VARCHAR(64),system_code VARCHAR(64),object_type VARCHAR(64),object_id VARCHAR(128),object_version VARCHAR(128),action_code VARCHAR(64),object_state VARCHAR(64),idempotency_key VARCHAR(128),business_context_json CLOB,form_data_json CLOB,bpm_process_instance_id VARCHAR(128),creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");
        engine.getIdentityService().setAuthenticatedUserId("99");String bpm;
        try{bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(obsoleteId).businessKey("FORM_ACTION:ACTUAL-FORM-CODE")
                .variables(Map.of("formInstanceId",501L,"controlledFileId",20L,"systemCode","DCC","objectType","CONTROLLED_FILE","actionCode","OBSOLETE","objectId","20","objectVersion","A/1","tenantId",1L)).start().getId();}
        finally{engine.getIdentityService().setAuthenticatedUserId(null);}
        tx().executeWithoutResult(s->{rounds.bind(5L,"OBSOLETE",20L,bpm);attributes.beginDraft(99L,5L,"OBSOLETE",20L,1);attributes.saveDraft(99L,5L,"OBSOLETE",20L,1,actual);attributes.freeze(99L,5L,"OBSOLETE",20L,1);});
        engine.getRuntimeService().deleteProcessInstance(bpm,"completed form identity fixture, not actual obsolete effect");
        jdbc.update("INSERT INTO bpm_form_action_instance(id,instance_code,tenant_id,applicant_user_id,status,system_code,object_type,object_id,object_version,action_code,form_data_json,bpm_process_instance_id) VALUES(501,'ACTUAL-FORM-CODE',1,99,'COMPLETED','DCC','CONTROLLED_FILE','20','A/1','OBSOLETE',?,?)",saved,bpm);
        if(!sql.getConfiguration().hasMapper(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class))sql.getConfiguration().addMapper(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class);
        var resolver=new DccBoundWorkflowContractResolver();wire(resolver,"definitions",definitions,"processes",processes,"rounds",rounds,"attributes",attributeRows,"forms",sql.getMapper(cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper.class),"formPreparationProof",org.springframework.test.util.ReflectionTestUtils.getField(source,"proof"));
        var summary=new DccApplicationRoundSummary();summary.setControlledFileId(20L);summary.setApplicationType("OBSOLETE");summary.setBpmRound(bpm);summary.setAttributeRound(1);
        var before=original(20L);var flow=resolver.resolveRoundFlowContract(files.selectById(20L),summary);
        assertEquals("READY",flow.status().toString());assertEquals(DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_OBSOLETE_APPROVAL_END,flow.flowRule());assertNull(flow.needTraining());assertEquals(501L,flow.formInstanceId());assertEquals("ACTUAL-FORM-CODE",flow.formInstanceCode());assertEquals(before,original(20L));
        jdbc.update("UPDATE bpm_form_action_instance SET instance_code='FORGED' WHERE id=501");assertThrows(RuntimeException.class,()->resolver.resolveRoundFlowContract(files.selectById(20L),summary));assertEquals(before,original(20L));
    }
}
