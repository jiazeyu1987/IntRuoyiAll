package cn.iocoder.yudao.module.dcc.service.file;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.*;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.system.api.dept.*;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.*;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Real route SQL/resolver/readiness -> HTTP -> formal A/B submit and actual Flowable. */
class DccG04RouteAndHistoryDatabaseTest extends DccWorkflowSelectedIterationDatabaseTest {
    @Resource DccCategoryApprovalRouteMapper routeRows;
    @Resource DccCategoryApprovalRouteNodeMapper nodeRows;
    @Resource DccControlledFileApplicationEvidenceMapper evidenceRows;
    DccControlledFileApprovalRouteAssigneeResolver resolver;DeptApi departments;
    DccControlledFileObsoleteServiceImpl obsolete;long formId=2000;
    DccElectronicSignatureAuthorizationService routeAuthorization;
    @BeforeEach void formalRoutes() throws Exception {
        resolver=new DccControlledFileApprovalRouteAssigneeResolver();defaults(resolver);
        departments=mock(DeptApi.class);
        when(departments.getDeptList(List.of(9007199254740993L))).thenReturn(List.of(new DeptRespDTO().setId(9007199254740993L).setLeaderUserId(99L).setName("替换会签部门")));
        when(departments.getDeptList(List.of(51L))).thenReturn(List.of(new DeptRespDTO().setId(51L).setLeaderUserId(99L).setName("有效默认部门")));
        var users=mock(AdminUserApi.class);when(users.getUserList(any())).thenAnswer(c->c.<List<Long>>getArgument(0).stream().map(id->new AdminUserRespDTO().setId(id).setStatus(0).setPostIds(Set.of(1L)).setNickname("真实测试账号")).toList());
        wire(resolver,"routeMapper",routeRows,"routeNodeMapper",nodeRows,"deptApi",departments,"adminUserApi",users);
        for(String type:List.of("NEW","REVISION","OBSOLETE")) {
            var route=DccCategoryApprovalRouteDO.builder().categoryId(2L).actionType(type).versionNo(1).active(true).build();routeRows.insert(route);
            var stages="OBSOLETE".equals(type)?List.of("MATRIX_REVIEW","MATRIX_APPROVAL"):List.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW");
            for(int index=0;index<stages.size();index++) nodeRows.insert(DccCategoryApprovalRouteNodeDO.builder().routeId(route.getId()).stageNo(index+1).stageOrder(index+1).sort(index+1).stageCode(stages.get(index)).stageName(stages.get(index))
                .candidateSourceType(index==0?"DEPT":"USER").candidateSourceId(index==0?51L:99L).candidateSourceIds(index==0?"51":"99").required(true)
                .approveMethod(index==0?"ALL":"ANY").approveRatio(index==0?100:null).requireAllApprovals(index==0).build());
        }
        var ready=new DccControlledFileRouteReadinessService();defaults(ready);wire(ready,"routeAssigneeResolver",resolver,"adminUserApi",users);
        var authorization=mock(DccElectronicSignatureAuthorizationService.class);when(authorization.getAuthorizationMap(any())).thenReturn(Map.of(99L,true));wire(ready,"signatureAuthorizationService",authorization);
        routeAuthorization=authorization;
        var images=mock(DccElectronicSignatureImageService.class);when(images.requireActiveSnapshot(99L)).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(1L).fileId(2L).sha256("test").imageStatus("ENABLED").verifiedStatus("VALID").build());wire(ready,"signatureImageService",images);
        var permission=mock(PermissionApi.class);when(permission.hasAnyPermissions(eq(99L),any(String[].class))).thenReturn(true);wire(ready,"permissionApi",permission);
        var categories=mock(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class);when(categories.selectById(2L)).thenReturn(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(2L).active(true).fileTypeTaxonomyId(6L).build());
        wire(workflow,"categoryMapper",categories,"routeReadinessService",ready,"approvalRouteAssigneeResolver",resolver,"deptApi",departments,"adminUserApi",users);
        obsolete=new DccControlledFileObsoleteServiceImpl();defaults(obsolete);wire(obsolete,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"permissionSupport",permissions,"approvalRouteAssigneeResolver",resolver,"taskSnapshotMapper",obligations,"applicationRoundService",rounds,"projectAttributesService",attributes);
        if(Arrays.stream(DccControlledFileObsoleteServiceImpl.class.getDeclaredFields()).anyMatch(f->f.getName().equals("routeReadinessService")))wire(obsolete,"routeReadinessService",ready);
        when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        var forms=mock(FormCenterRuntimeService.class);
        when(forms.createInstance(any(),eq(99L))).thenAnswer(c->{var r=new FormInstanceRespVO();r.setId(++formId);r.setInstanceCode("FORM-"+formId);r.setStatus("DRAFT");return r;});
        when(forms.submitInstance(anyLong(),any(),eq(99L))).thenAnswer(c->{long id=c.getArgument(0);var req=c.<FormInstanceSubmitReqVO>getArgument(1);var vars=new HashMap<String,Object>();vars.put("systemCode","DCC");vars.put("objectType","CONTROLLED_FILE");vars.put("actionCode","OBSOLETE");vars.put("objectId","20");vars.put("objectVersion",files.selectById(20L).getVersionNo());
            engine.getIdentityService().setAuthenticatedUserId("99");try {var process=engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(DccControlledFileProcessDefinitionKeys.OBSOLETE,"FORM_ACTION:FORM-"+id,vars,"1");var r=new FormInstanceRespVO();r.setId(id);r.setInstanceCode("FORM-"+id);r.setStatus("PENDING");r.setBpmProcessInstanceId(process.getId());return r;}finally{engine.getIdentityService().setAuthenticatedUserId(null);}});
        wire(obsolete,"formCenterRuntimeService",forms);
        var xml="""
          <?xml version="1.0" encoding="UTF-8"?>
          <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="test"><process id="dcc-controlled-file-obsolete" isExecutable="true"><startEvent id="start"/><sequenceFlow id="one" sourceRef="start" targetRef="MATRIX_REVIEW"/><userTask id="MATRIX_REVIEW" flowable:assignee="99"/><sequenceFlow id="two" sourceRef="MATRIX_REVIEW" targetRef="end"/><endEvent id="end"/></process></definitions>
          """;
        engine.getRepositoryService().createDeployment().tenantId("1").addString("obsolete-test.bpmn20.xml",xml).deploy();
        wire(query,"applicationEvidenceMapper",evidenceRows);
        var download=mock(cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService.class);
        when(download.decide(any())).thenReturn(cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyDecision.deny("测试无下载"));
        wire(query,"downloadPolicyService",download);
        var detailGuard=new DccControlledFileDetailAuthorizationGuard();defaults(detailGuard);wire(detailGuard,"assignmentScopeService",scope);wire(query,"detailAuthorizationGuard",detailGuard);

        var history=mock(cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService.class);when(history.getHistoricProcessInstance(anyString())).thenAnswer(c->engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(c.getArgument(0)).includeProcessVariables().singleResult());
        if(Arrays.stream(DccControlledFileQueryServiceImpl.class.getDeclaredFields()).anyMatch(f->f.getName().equals("applicationHistoryGuard"))) {
            var guard=Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccApplicationHistoryGuard");try{var instance=guard.getDeclaredConstructor().newInstance();wire(instance,"processes",history);wire(query,"applicationHistoryGuard",instance);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
    }
    @ParameterizedTest @ValueSource(strings={"NEW","REVISION","OBSOLETE"})
    void replacedDepartmentsSurviveRealPreviewAndFormalSubmission(String type) throws Exception {
        when(departments.getDeptList(List.of(51L))).thenReturn(List.of(new DeptRespDTO().setId(51L).setName("被移除的无负责人默认部门")));
        if(type.equals("NEW"))initial();else controlled("A/1");
        var controller=new DccControlledFileController();wire(controller,"workflowService",workflow,"obsoleteService",obsolete);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            var response=MockMvcBuilders.standaloneSetup(controller).build().perform(post("/dcc/controlled-files/route-preview").contentType("application/json").content("{\"categoryId\":\"2\",\"actionType\":\""+type+"\",\"selectedSignoffDepartmentIds\":[\"9007199254740993\"]}")).andReturn();
            var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(response.getResponse().getContentAsString(),com.fasterxml.jackson.databind.JsonNode.class);assertEquals(0,json.get("code").intValue());assertTrue(json.get("data").get("ready").booleanValue());
            assertEquals("9007199254740993",json.get("data").get("nodes").get(0).get("candidateSourceIds").get(0).textValue());
        }
        if(type.equals("OBSOLETE"))tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,obsoleteRequest("round-one")));
        else {long chosen=checkin(20L,"formal selection");var req=request(type.equals("NEW")?"INITIAL":"PARTIAL");req.setSelectedSignoffDepartmentIds(List.of(9007199254740993L));workflow.submitWorkingIteration(99L,chosen,req);}
        assertTrue(obligations.selectList().stream().allMatch(row->row.getDepartmentId().equals(9007199254740993L)));
        verify(departments,never()).getDeptList(List.of(51L));
        assertEquals(99L,resolver.resolveRouteForReadiness(2L,99L,type,List.of(9007199254740993L)).nodes().get(1).resolvedUserIds().get(0));
    }
    DccControlledFileObsoleteReqVO obsoleteRequest(String key){var req=new DccControlledFileObsoleteReqVO();req.setReason("本次独立作废申请");req.setIdempotencyKey(key);req.setSelectedSignoffDepartmentIds(List.of(9007199254740993L));req.setProjectAttributes(ce);return req;}
    @Test void failedObsoleteAndLaterRoundRemainBoundToTheirActualBpmAndOwnAttributes() {
        controlled("A/1");String first=tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,obsoleteRequest("first-failed"))).getBpmProcessInstanceId();
        engine.getRuntimeService().deleteProcessInstance(first,"本次作废审核失败");changeDefault();var next=obsoleteRequest("second-live");next.setProjectAttributes(fda);String second=tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,next)).getBpmProcessInstanceId();
        var rows=query.listApplicationRounds(99L,20L);assertEquals(Set.of(first,second),rows.stream().map(DccApplicationRoundSummary::getBpmRound).collect(java.util.stream.Collectors.toSet()));
        assertEquals(ce,query.getApplicationEvidence(99L,20L,"OBSOLETE",first).actualAttributes());assertEquals(nmpa,query.getApplicationEvidence(99L,20L,"OBSOLETE",first).defaultSource());
        assertEquals(fda,query.getApplicationEvidence(99L,20L,"OBSOLETE",second).actualAttributes());assertEquals(fda,query.getApplicationEvidence(99L,20L,"OBSOLETE",second).defaultSource());
        assertNotNull(engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(first).finished().singleResult());assertEquals("ACTIVE",files.selectById(20L).getStatus());
    }
    @Test void forgedPersistedBpmMappingCannotBorrowAnotherFileOrApplicationTypeHistory() {
        controlled("A/1");String real=tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,obsoleteRequest("actual"))).getBpmProcessInstanceId();
        jdbc.update("UPDATE dcc_application_round_link SET application_type='UPLOAD' WHERE application_id=20 AND bpm_round=?",real);
        assertThrows(IllegalStateException.class,()->query.listApplicationRounds(99L,20L));
        assertThrows(IllegalStateException.class,()->query.getApplicationEvidence(99L,20L,"UPLOAD",real));
    }
    @ParameterizedTest @ValueSource(strings={"FILE","VERSION","MISSING"})
    void mappedRoundRequiresTheExactRealBpmObjectAndVersion(String invalid) {
        controlled("A/1");String real=tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,obsoleteRequest("identity-"+invalid))).getBpmProcessInstanceId();
        if("FILE".equals(invalid))engine.getRuntimeService().setVariable(real,"objectId","999");
        if("VERSION".equals(invalid))engine.getRuntimeService().setVariable(real,"objectVersion","A/2");
        if("MISSING".equals(invalid))jdbc.update("UPDATE dcc_application_round_link SET bpm_round='unrecorded-bpm' WHERE bpm_round=?",real);
        assertThrows(IllegalStateException.class,()->query.listApplicationRounds(99L,20L));
        assertThrows(IllegalStateException.class,()->query.getApplicationEvidence(99L,20L,"OBSOLETE","MISSING".equals(invalid)?"unrecorded-bpm":real));
        assertEquals("ACTIVE",files.selectById(20L).getStatus());
    }
    @Test void obsoleteSubmissionCannotBypassTheSelectedParticipantsSignatureReadiness() {
        controlled("A/1");when(routeAuthorization.getAuthorizationMap(any())).thenReturn(Map.of(99L,false));
        int before=count("dcc_application_round_link");
        assertThrows(RuntimeException.class,()->tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,obsoleteRequest("unsigned"))));
        assertEquals(before,count("dcc_application_round_link"));assertEquals(0,count("dcc_controlled_file_task_assignee_snapshot"));assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
    }
}
