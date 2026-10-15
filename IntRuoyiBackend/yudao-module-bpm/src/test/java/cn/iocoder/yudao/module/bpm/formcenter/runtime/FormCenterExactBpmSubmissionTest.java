package cn.iocoder.yudao.module.bpm.formcenter.runtime;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.formcenter.model.*;
import cn.iocoder.yudao.module.bpm.formcenter.service.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.flowable.engine.*;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.TestPropertySource;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual form mappers, Spring/H2 and engine. Policy and business verification are explicit external ports. */
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:form_exact_submit;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value", "spring.sql.init.encoding=UTF-8"})
class FormCenterExactBpmSubmissionTest extends BaseDbUnitTest {
    private static final String KEY="manager-exact-form-review";
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager transactionManager;
    @Resource FormActionInstanceMapper instances;
    @Resource FormActionSnapshotMapper snapshots;
    @Resource FormTaskPermissionMapper taskPermissions;
    FormCenterRuntimeServiceImpl service;
    ProcessEngine engine;
    BpmProcessInstanceApi api;
    String frozenId;
    String acceptedJson;
    boolean rejectPreparation, lateFailure, wrongKey;
    String invalidResult;
    final List<String> exactSteps=new ArrayList<>();
    String preflightJson;
    boolean requireBusinessLocksFirst, rejectPreflight;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        var engineConfig=new SpringProcessEngineConfiguration();
        engineConfig.setDataSource(dataSource); engineConfig.setTransactionManager(transactionManager);
        engineConfig.setDatabaseType("mysql");engineConfig.setDatabaseSchemaUpdate("true");engineConfig.setAsyncExecutorActivate(false);engineConfig.setDisableIdmEngine(true);
        engine=engineConfig.buildProcessEngine();
        frozenId=deploy("original");deploy("later");
        var context=new BusinessActionContextReqVO();context.setTenantId(1L);context.setDataDomain("DCC");
        context.setSystemCode("DCC");context.setObjectType("CONTROLLED_FILE");context.setObjectId("20");
        context.setObjectVersion("A/1");context.setActionCode("OBSOLETE");context.setObjectState("ACTIVE");
        instances.insert(FormActionInstanceDO.builder().id(301L).instanceCode("FORM-CODE-900").tenantId(1L).policyId(7L)
            .applicantUserId(99L).status("DRAFT").dataDomain("DCC").systemCode("DCC").objectType("CONTROLLED_FILE")
            .objectId("20").objectVersion("A/1").actionCode("OBSOLETE").objectState("ACTIVE").idempotencyKey("this-form")
            .businessContextJson(JsonUtils.toJsonString(context)).formDataJson("{\"prepared\":\"saved-D1\"}").build());
        acceptedJson=JsonUtils.toJsonString(Map.of("prepared","saved-D1","serverVerified",true));
        var policies=mock(BusinessApprovalPolicyMapper.class);
        when(policies.selectById(7L)).thenReturn(BusinessApprovalPolicyDO.builder().id(7L).tenantId(1L)
            .dataDomain("DCC").systemCode("DCC").objectType("CONTROLLED_FILE").actionCode("OBSOLETE").objectState("ACTIVE")
            .policyMode("BPM_REQUIRED").processDefinitionKey(KEY).effectExecutorCode("isolated-effect").status("PUBLISHED").build());
        api=mock(BpmProcessInstanceApi.class);
        when(api.createProcessInstance(anyLong(),any())).thenAnswer(call->{
            BpmProcessInstanceCreateReqDTO req=call.getArgument(1);
            assertEquals(acceptedJson,instances.selectById(301L).getFormDataJson(),"accepted data is stored before BPM start");
            assertEquals(frozenId,req.getProcessDefinitionId(),"exact frozen ID, never newer same-key");
            assertEquals("FORM_ACTION:FORM-CODE-900",req.getBusinessKey());
            assertEquals(301L,req.getVariables().get("formInstanceId"));
            assertEquals(Map.of("review",List.of(99L)),req.getStartUserSelectAssignees());
            assertEquals(Map.of("review",List.of(99L)),req.getApproveUserSelectAssignees());
            return engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(req.getProcessDefinitionId())
                .businessKey(req.getBusinessKey()).variables(req.getVariables()).start().getId();
        });
        doAnswer(call->{String id=call.getArgument(1);
            if(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(id).singleResult()!=null)
                engine.getRuntimeService().deleteProcessInstance(id,"late failure compensation");return null;
        }).when(api).cancelProcessInstance(anyLong(),anyString(),anyString());
        service=new FormCenterRuntimeServiceImpl();wire("actionInstanceMapper",instances);wire("actionSnapshotMapper",snapshots);
        wire("taskPermissionMapper",taskPermissions);wire("businessApprovalPolicyMapper",policies);wire("processInstanceApi",api);
        wire("flowableTaskService",engine.getTaskService());wire("lifecycleAdapters",List.of(new ExactAdapter()));
    }
    @AfterEach void cleanup(){if(engine!=null){engine.getRepositoryService().createDeploymentQuery().list()
        .forEach(row->engine.getRepositoryService().deleteDeployment(row.getId(),true));engine.close();}TenantContextHolder.clear();}
    void wire(String field,Object value){ReflectionTestUtils.setField(service,field,value);}
    String deploy(String name){String xml="<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"manager:test\"><process id=\""+KEY+"\" name=\""+name+"\" isExecutable=\"true\"><startEvent id=\"start\"/><sequenceFlow id=\"next\" sourceRef=\"start\" targetRef=\"review\"/><userTask id=\"review\" flowable:assignee=\"99\"/></process></definitions>";
        var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("exact.bpmn20.xml",xml).deploy();
        return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId();}
    FormInstanceSubmitReqVO request(){var r=new FormInstanceSubmitReqVO();r.setFormData(Map.of("prepared","submitted-request"));
        r.setStartUserSelectAssignees(Map.of("review",List.of(55L)));r.setApproveUserSelectAssignees(Map.of("review",List.of(56L)));return r;}
    FormInstanceRespVO submit(Long actor){return new TransactionTemplate(transactionManager).execute(s->service.submitInstance(301L,request(),actor));}
    void save(Long actor) {
        var req=new FormInstanceDraftReqVO();req.setFormData(Map.of("reason","saved actual reason","prepared","not prepared"));
        new TransactionTemplate(transactionManager).executeWithoutResult(s->service.saveDraft(301L,req,actor));
    }
    @Test void exactDraftSaveRejectsForeignAndMissingApplicantsWithoutWrites() {
        var before=instances.selectById(301L);
        for(Long actor:Arrays.asList(98L,null,0L)) {
            assertThrows(RuntimeException.class,()->save(actor));
            assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));
        }
        verify(api,never()).createProcessInstance(anyLong(),any());
    }
    @Test void exactDraftCanSaveFullInputsWithoutExecutionPreparationOrBpm() {
        rejectPreparation=true;save(99L);
        var after=instances.selectById(301L);
        assertEquals("DRAFT",after.getStatus());assertNull(after.getBpmProcessInstanceId());
        assertEquals("saved actual reason",JsonUtils.parseObject(after.getFormDataJson(),Map.class).get("reason"));
        assertEquals(1L,snapshots.selectCount(null));
        assertEquals(after.getFormDataJson(),snapshots.selectList(null).get(0).getFormDataJson());
        verify(api,never()).createProcessInstance(anyLong(),any());
    }
    @Test void exactDraftSaveRejectsAlreadyBoundBpmEvenWithDraftStatus() {
        var row=instances.selectById(301L);row.setBpmProcessInstanceId("bound-real-process");instances.updateById(row);
        var before=instances.selectById(301L);assertThrows(RuntimeException.class,()->save(99L));
        assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));
        verify(api,never()).createProcessInstance(anyLong(),any());
    }
    @Test void exactDraftLostCasCannotRecordSuccessfulSnapshot() {
        var mapper=spy(instances);
        doReturn(0).when(mapper).update((FormActionInstanceDO)isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        wire("actionInstanceMapper",mapper);var before=instances.selectById(301L);
        assertThrows(RuntimeException.class,()->save(99L));
        assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));
    }
    @Test void actualLateDraftSnapshotExceptionRollsBackTheSavedForm() {
        var mapper=spy(snapshots);
        doThrow(new IllegalStateException("late draft snapshot rejected")).when(mapper)
            .insert(any(cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionSnapshotDO.class));
        wire("actionSnapshotMapper",mapper);var before=instances.selectById(301L);
        assertThrows(RuntimeException.class,()->save(99L));assertEquals(before,instances.selectById(301L));
        assertEquals(0L,snapshots.selectCount(null));verify(api,never()).createProcessInstance(anyLong(),any());
    }
    @Test void nonMatchingExactAdapterKeepsOrdinaryDraftSaveContract() {
        var unrelated=new ExactAdapter(){@Override public boolean supports(FormActionInstance form){return false;}};
        wire("lifecycleAdapters",List.of(unrelated));save(98L);
        assertEquals("saved actual reason",JsonUtils.parseObject(instances.selectById(301L).getFormDataJson(),Map.class).get("reason"));
        assertEquals(1L,snapshots.selectCount(null));verify(api,never()).createProcessInstance(anyLong(),any());
    }
    @Test void exactBusinessLockPreparationPrecedesMutableFilePreflight() {
        requireBusinessLocksFirst=true;
        var result=submit(99L);assertEquals("IN_APPROVAL",result.getStatus());
        assertEquals(List.of("verify","preflight"),exactSteps);
        assertEquals(acceptedJson,preflightJson,"preflight checks server-verified canonical inputs");
        verify(api).createProcessInstance(anyLong(),any());
    }
    @Test void latePreflightRejectionCannotWriteVerifiedDataOrStartBpm() {
        rejectPreflight=true;var before=instances.selectById(301L);
        assertThrows(RuntimeException.class,()->submit(99L));
        assertEquals(List.of("verify","preflight"),exactSteps);
        assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));
        assertEquals(0L,taskPermissions.selectCount(null));verify(api,never()).createProcessInstance(anyLong(),any());
    }
    @Test void persistsVerifiedDataBeforeStartingTheFrozenDefinitionAndKeepsCodeSeparateFromId(){
        var result=submit(99L);assertEquals("IN_APPROVAL",result.getStatus());
        assertEquals(acceptedJson,instances.selectById(301L).getFormDataJson());
        var process=engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(result.getBpmProcessInstanceId()).singleResult();
        assertEquals(frozenId,process.getProcessDefinitionId());assertEquals(1L,snapshots.selectCount(null));
    }
    @Test void rejectedServerPreparationMakesZeroStartAndPreservesDraft(){rejectPreparation=true;var before=instances.selectById(301L);
        assertThrows(RuntimeException.class,()->submit(99L));verify(api,never()).createProcessInstance(anyLong(),any());
        assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));}
    @Test void wrongApplicantIsRejectedBeforeBusinessPreparationAndBpm(){var before=instances.selectById(301L);
        assertThrows(RuntimeException.class,()->submit(98L));verify(api,never()).createProcessInstance(anyLong(),any());assertEquals(before,instances.selectById(301L));}
    @Test void verifiedKeyCannotReplaceTheSavedPolicy(){wrongKey=true;var before=instances.selectById(301L);
        assertThrows(RuntimeException.class,()->submit(99L));verify(api,never()).createProcessInstance(anyLong(),any());assertEquals(before,instances.selectById(301L));}
    @Test void lateBindingFailureRollsBackPreparedDraftSnapshotPermissionsAndEngine(){lateFailure=true;var before=instances.selectById(301L);
        assertThrows(RuntimeException.class,()->submit(99L));verify(api).createProcessInstance(anyLong(),any());
        assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));assertEquals(0L,taskPermissions.selectCount(null));
        assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());}
    @Test void wrongTenantCannotReadOrReplaceTheSavedForm(){var before=instances.selectById(301L);TenantContextHolder.setTenantId(2L);
        assertThrows(RuntimeException.class,()->submit(99L));verify(api,never()).createProcessInstance(anyLong(),any());
        TenantContextHolder.setTenantId(1L);assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));}
    @Test void invalidDefinitionOrCanonicalDataIsRefusedBeforeWriting(){
        for(String bad:List.of("BLANK_ID","SPACED_ID","NULL_JSON","ARRAY_JSON")){
            invalidResult=bad;var before=instances.selectById(301L);assertThrows(RuntimeException.class,()->submit(99L));
            assertEquals(before,instances.selectById(301L));assertEquals(0L,snapshots.selectCount(null));
        }verify(api,never()).createProcessInstance(anyLong(),any());
    }
    @Test void existingNonExactAdapterKeepsItsOriginalKeyBasedSubmission(){
        wire("lifecycleAdapters",List.of(new FormControlledActionLifecycleAdapter(){
            public boolean supports(FormActionInstance form){return true;}
            public FormBusinessEffectPrecheck preflight(FormActionInstance form){return FormBusinessEffectPrecheck.pass();}
            public void onPendingApprovalStarted(FormActionInstance form){}
            public void onPendingApprovalClosed(FormActionInstance form,FormControlledActionApprovalOutcome outcome,String reason){}
        }));
        doAnswer(call->{BpmProcessInstanceCreateReqDTO req=call.getArgument(1);
            assertNull(req.getProcessDefinitionId());assertEquals(KEY,req.getProcessDefinitionKey());
            assertEquals("{\"prepared\":\"saved-D1\"}",instances.selectById(301L).getFormDataJson());
            return engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(KEY,req.getBusinessKey(),req.getVariables(),"1").getId();
        }).when(api).createProcessInstance(anyLong(),any());
        var result=submit(99L);assertEquals("IN_APPROVAL",result.getStatus());
        assertEquals(JsonUtils.toJsonString(request().getFormData()),instances.selectById(301L).getFormDataJson());
        assertNotEquals(frozenId,engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(result.getBpmProcessInstanceId()).singleResult().getProcessDefinitionId());
    }
    class ExactAdapter implements FormExactBpmSubmissionAdapter {
        public boolean supports(FormActionInstance instance){return true;}
        public FormBusinessEffectPrecheck preflight(FormActionInstance instance){
            exactSteps.add("preflight");
            preflightJson=JsonUtils.toJsonString(instance.getFormData());
            if(requireBusinessLocksFirst&&!exactSteps.contains("verify"))throw new IllegalStateException("Business locks must be acquired before mutable File preflight");
            return rejectPreflight?FormBusinessEffectPrecheck.fail("actual locked File rejected"):FormBusinessEffectPrecheck.pass();
        }
        public void onPendingApprovalStarted(FormActionInstance instance){if(lateFailure)throw new IllegalStateException("real downstream binding refused");}
        public void onPendingApprovalClosed(FormActionInstance instance,FormControlledActionApprovalOutcome outcome,String reason){}
        public VerifiedSubmission verifyExactSubmission(PersistedDraft saved,FormActionInstance requested,Long actor){
            exactSteps.add("verify");
            if(rejectPreparation)throw new IllegalArgumentException("persisted D1 and submitted D2 not approved");
            assertEquals(99L,actor);assertEquals(301L,saved.formInstanceId());assertEquals("FORM-CODE-900",saved.formInstanceCode());
            assertEquals("{\"prepared\":\"saved-D1\"}",saved.formDataJson());
            assertEquals("submitted-request",requested.getFormData().get("prepared"));
            if(invalidResult!=null)return new VerifiedSubmission(KEY,
                "BLANK_ID".equals(invalidResult)?" ":"SPACED_ID".equals(invalidResult)?" "+frozenId:frozenId,
                "NULL_JSON".equals(invalidResult)?"null":"ARRAY_JSON".equals(invalidResult)?"[]":acceptedJson,
                Map.of("review",List.of(99L)),Map.of("review",List.of(99L)));
            return new VerifiedSubmission(wrongKey?"foreign-key":KEY,frozenId,acceptedJson,
                Map.of("review",List.of(99L)),Map.of("review",List.of(99L)));
        }
    }
}
