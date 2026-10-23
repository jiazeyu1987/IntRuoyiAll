package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual public A submission -> C allocation/source cleanup -> B round/snapshot on real H2 transactions. */
class DccWorkflowRevisionIntegrationTest extends BaseDbUnitTest {
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccSourceNameReservationMapper g25Reservations;

    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager manager;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileNameClaimMapper claims;
    @Resource DccControlledFileCheckoutMapper checkouts;
    @Resource DccControlledFileRouteSnapshotMapper routes;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper obligations;
    @Resource DccProjectCodeMapper projects;
    @Resource DccProjectApplicationAttributesMapper attributesMapper;
    JdbcTemplate jdbc;DccControlledFileWorkflowServiceImpl workflow;
    DccControlledFileSourceOwnershipService sources;BpmProcessInstanceApi bpm;
    private final DccProjectAttributes original=new DccProjectAttributes(List.of("NMPA"),null,"Y","N","N",null);
    private final DccProjectAttributes actual=new DccProjectAttributes(List.of("CE"),null,"N","Y","N",null);

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,project_id BIGINT,application_type VARCHAR(16),application_id BIGINT,bpm_round VARCHAR(64),attribute_round INT,UNIQUE(tenant_id,application_type,application_id,bpm_round),UNIQUE(tenant_id,application_type,application_id,attribute_round))");
        jdbc.update("DELETE FROM dcc_application_round_link");
        var access=mock(DccProjectAccessService.class);var permission=mock(DccControlledFileCategoryPermissionSupport.class);
        when(permission.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        var attrs=new DccProjectAttributesService();wire(attrs,"projectCodeMapper",projects,"attributesMapper",attributesMapper,"accessService",access);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,status,default_attributes_json,deleted) VALUES(5,1,'revision-project','ENABLE',?,0)",attrs.encode(original));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status,current_active_controlled_file_id,latest_controlled_file_id,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,deleted) VALUES(10,1,2,3,'SOP','N-1','ACTIVE_CHAIN',20,21,5,6,'N-1',0)");
        for(var row:List.of(file(20L,"A/1","ACTIVE"),file(21L,"B/1","CONTROLLED_PENDING_EFFECTIVE"),file(22L,"B/1-1","WORKING")))files.insert(row);
        jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id IN(20,21)");
        var identity=new DccControlledFileNameClaimService();org.springframework.test.util.ReflectionTestUtils.setField(identity,"reservationMapper",g25Reservations);wire(identity,"masterMapper",masters,"claimMapper",claims,"fileMapper",files);
        sources=mock(DccControlledFileSourceOwnershipService.class);
        when(sources.rollbackCleanup(any())).thenAnswer(c->{var source=c.<DccControlledFilePreparedSource>getArgument(0);return (Runnable)()->sources.cleanupPreparedSource(source);});
        when(sources.createVerifiedCopy(100L)).thenReturn(new DccControlledFilePreparedSource(300L,100L,"frozen-hash",true));
        var revision=new DccControlledFileRevisionServiceImpl();
        wire(revision,"publicUploadPlacementService",mock(DccPublicUploadPlacementService.class));
        var scope=mock(DccControlledFileAssignmentScopeService.class);
        when(scope.isWithinAssignedFileScope(any(),any())).thenReturn(true);
        var revisionDates=new DccWorkflowDatePolicy();revisionDates.setZoneId("Asia/Singapore");
        wire(revision,"assignmentScopeService",scope,"datePolicy",revisionDates);
        wire(revision,"fileMapper",files,"masterMapper",masters,"checkoutMapper",checkouts,"projectAccessService",access,
                "permissionSupport",permission,"sourceOwnershipService",sources,"relatedFileService",mock(DccControlledFileRelatedFileService.class),
                "nameClaimService",identity,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
        var rounds=new DccApplicationRoundService();wire(rounds,"jdbc",jdbc,"projects",projects,"files",files);
        var bridge=new DccProjectApplicationSnapshotService();wire(bridge,"rounds",rounds,"attributes",attrs,"files",files,"masters",masters,"snapshots",attributesMapper);
        var initializer=new DccWorkingApplicationDraftInitializer();wire(initializer,"projectMapper",projects,"masterMapper",masters,"fileMapper",files,
                "rounds",rounds,"snapshots",bridge,"attributesMapper",attributesMapper,"projectAccess",access,"categoryPermission",permission);
        var resolver=new DccControlledFileApprovalRouteAssigneeResolver();
        var readiness=mock(DccControlledFileRouteReadinessService.class);
        var resolved=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(
                DccCategoryApprovalRouteDO.builder().id(30L).versionNo(1).actionType("REVISION").build(),
                List.of(node(1,"MATRIX_REVIEW","DEPT",51L,99L),node(2,"MATRIX_APPROVAL","USER",100L,100L),node(3,"DOC_CONTROL_REVIEW","USER",101L,101L)));
        when(readiness.evaluateDepartments(any(),any(),any(),eq("REVISION"))).thenReturn(
                new DccControlledFileRouteReadinessService.RouteReadinessEvaluation(resolved,DccControlledFileRouteReadinessRespVO.builder().ready(true).build()));
        var departments=mock(DeptApi.class);when(departments.getDeptList(List.of(51L))).thenReturn(List.of(new DeptRespDTO().setId(51L).setName("质量部").setLeaderUserId(99L)));
        var users=mock(AdminUserApi.class);when(users.getUserList(List.of(99L))).thenReturn(List.of(new AdminUserRespDTO().setId(99L).setNickname("负责人")));
        bpm=mock(BpmProcessInstanceApi.class);when(bpm.createProcessInstance(eq(99L),any())).thenReturn("formal-bpm-round");
        workflow=new DccControlledFileWorkflowServiceImpl();var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Singapore");
        wire(workflow,"publicUploadPlacementService",mock(DccPublicUploadPlacementService.class));
        wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"checkoutMapper",checkouts,
                "routeSnapshotMapper",routes,"taskAssigneeSnapshotMapper",obligations,"revisionService",revision,
                "applicationRoundService",rounds,"projectAttributesService",attrs,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                "projectApplicationSnapshots",bridge,
                "projectCodeMapper",projects,"workingApplicationDraftInitializer",initializer,
                "routeReadinessService",readiness,"approvalRouteAssigneeResolver",resolver,"workflowDatePolicy",dates,
                "deptApi",departments,"adminUserApi",users,"bpmProcessInstanceApi",bpm,"projectAccessService",access,
                "categoryPermissionSupport",permission,
                "platformAdapter",mock(DccControlledContentAdapter.class));
        new TransactionTemplate(manager).executeWithoutResult(s->workflow.prepareApplicationDraft(99L,files.selectById(22L),null));
    }
    @AfterEach void clear(){jdbc.update("DELETE FROM dcc_application_round_link");TenantContextHolder.clear();}
    @Test void publicSubmissionAllocatesOnlyTheFormalRevisionFromLatestFutureControlAndFreezesItsOwnAttributes() {
        long target=submit(request());
        var version=files.selectById(target);assertEquals("C/1",version.getVersionNo());
        assertEquals("REPLACEMENT",version.getRevisionChangeType());assertEquals(21L,version.getRevisionSourceControlledFileId());
        assertEquals(22L,version.getSelectedIterationControlledFileId());assertEquals("WORKING",files.selectById(22L).getStatus());
        assertEquals("PENDING_MATRIX_REVIEW",version.getStatus());assertEquals("formal-bpm-round",version.getProcessInstanceId());
        assertEquals(LocalDate.now().plusDays(20),version.getEffectiveDate());
        assertEquals(20L,masters.selectById(10L).getCurrentActiveControlledFileId());assertEquals(21L,masters.selectById(10L).getLatestControlledFileId());
        assertNull(version.getControlledTime());assertNull(version.getActivatedTime());
        var snapshot=attributesMapper.find("REVISION",target,1);
        assertTrue(snapshot.getSubmitted());assertNotEquals(snapshot.getDefaultSourceJson(),snapshot.getActualAttributesJson());
        assertEquals(1,obligations.selectListByControlledFileId(target).size());
    }
    @Test void bpmCreationFailureRollsBackFormalAllocationIdentitySnapshotsAndSourceOwnership() {
        when(bpm.createProcessInstance(any(),any())).thenThrow(new IllegalStateException("BPM failure"));
        assertThrows(IllegalStateException.class,()->submit(request()));
        assertEquals(3,files.selectListByMasterId(10L).size());assertEquals("WORKING",files.selectById(22L).getStatus());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_route_snapshot",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        verify(sources).cleanupPreparedSource(any());
    }
    @Test void replayReturnsTheSameCandidateAndChangedActualAttributesConflictBeforeAnotherBpm() {
        long target=submit(request());assertEquals(target,submit(request()));
        var changed=request();changed.setProjectAttributes(original);
        assertThrows(RuntimeException.class,()->submit(changed));
        verify(bpm,times(1)).createProcessInstance(any(),any());
        assertEquals(4,files.selectListByMasterId(10L).size());
    }
    @Test void h08RevisionCandidateFreezesSelectedDraftSourceAfterProjectDefaultsChange(){
        var attrs=new DccProjectAttributesService();wire(attrs,"projectCodeMapper",projects,"attributesMapper",attributesMapper,"accessService",mock(DccProjectAccessService.class));
        new TransactionTemplate(manager).executeWithoutResult(s->attrs.saveDraft(99L,5L,"REVISION",22L,1,actual));
        var fda=new DccProjectAttributes(List.of("FDA"),null,"N","N","N",null);
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attrs.encode(fda));
        long target=submit(request());var frozen=attributesMapper.find("REVISION",target,1);
        assertEquals(original,attrs.readValue(frozen.getDefaultSourceJson()));assertEquals(actual,attrs.readValue(frozen.getActualAttributesJson()));
        assertEquals(22L,frozen.getSourceApplicationId());assertEquals(1,frozen.getSourceApplicationRound());assertTrue(frozen.getSubmitted());
        assertEquals(20L,masters.selectById(10L).getCurrentActiveControlledFileId());assertEquals(21L,masters.selectById(10L).getLatestControlledFileId());
    }
    @Test void h02InitialCheckedInBodyCreatesOnlyTheFormalInitialCandidateWithThisApplicationDate() {
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=NULL,current_active_controlled_file_id=NULL WHERE id=10");
        jdbc.update("DELETE FROM dcc_controlled_file WHERE id=21");
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',controlled_time=NULL,version_no='A/1',revision_change_type='INITIAL' WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file SET version_no='A/1-1',predecessor_controlled_file_id=20,revision_base_active_controlled_file_id=NULL WHERE id=22");
        jdbc.update("UPDATE dcc_controlled_file SET change_type='NEW' WHERE id=22");
        jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id=22");
        jdbc.update("DELETE FROM dcc_project_application_attributes WHERE application_id=22");
        new TransactionTemplate(manager).executeWithoutResult(s->{workflow.prepareApplicationDraft(99L,files.selectById(20L),actual);
            workflow.prepareApplicationDraft(99L,files.selectById(22L),null);});
        var attrCodec=new DccProjectAttributesService();
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attrCodec.encode(new DccProjectAttributes(List.of("FDA"),null,"N","N","N",null)));
        var readiness=mock(DccControlledFileRouteReadinessService.class);
        var route=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(
            DccCategoryApprovalRouteDO.builder().id(31L).versionNo(1).actionType("NEW").build(),
            List.of(node(1,"MATRIX_REVIEW","DEPT",51L,99L),node(2,"MATRIX_APPROVAL","USER",100L,100L),node(3,"DOC_CONTROL_REVIEW","USER",101L,101L)));
        when(readiness.evaluateDepartments(any(),any(),any(),eq("NEW"))).thenReturn(new DccControlledFileRouteReadinessService.RouteReadinessEvaluation(route,DccControlledFileRouteReadinessRespVO.builder().ready(true).build()));
        wire(workflow,"routeReadinessService",readiness);
        var request=request();request.setRevisionChangeType("INITIAL");
        long target=assertDoesNotThrow(()->submit(request),"A must submit the real INITIAL candidate from the selected working body");var candidate=files.selectById(target);
        assertEquals("A/1",candidate.getVersionNo());assertEquals("INITIAL",candidate.getRevisionChangeType());
        assertEquals("NEW",candidate.getChangeType());assertEquals(22L,candidate.getSelectedIterationControlledFileId());
        assertEquals(request.getEffectiveDate(),candidate.getEffectiveDate());assertEquals("PENDING_MATRIX_REVIEW",candidate.getStatus());
        assertEquals("A/1-1",files.selectById(22L).getVersionNo());assertEquals("WORKING",files.selectById(22L).getStatus());
        assertNull(masters.selectById(10L).getLatestControlledFileId());assertNull(masters.selectById(10L).getCurrentActiveControlledFileId());
        var initialSnapshot=attributesMapper.find("UPLOAD",target,1);
        assertTrue(initialSnapshot.getSubmitted());assertEquals(original,attrCodec.readValue(initialSnapshot.getDefaultSourceJson()));
        assertEquals(actual,attrCodec.readValue(initialSnapshot.getActualAttributesJson()));assertEquals(22L,initialSnapshot.getSourceApplicationId());
        assertEquals(target,submit(request));
        var changed=request();changed.setRevisionChangeType("INITIAL");changed.setEffectiveDate(request.getEffectiveDate().plusDays(1));
        assertThrows(RuntimeException.class,()->submit(changed));verify(bpm,times(1)).createProcessInstance(any(),any());
    }
    @Test void h02RealFlowableBpmAndBothModuleFactsCommitOrRollBackTogether() {
        var config=new org.flowable.spring.SpringProcessEngineConfiguration();
        config.setDataSource(dataSource);config.setTransactionManager(manager);config.setDatabaseType("mysql");
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);
        var engine=config.buildProcessEngine();String deployment=null;
        try {
            deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("h02-revision.bpmn20.xml","""
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:flowable="http://flowable.org/bpmn" targetNamespace="dcc-h02-transaction">
                  <process id="dcc-controlled-file-revision" isExecutable="true"><startEvent id="start"/>
                    <sequenceFlow id="flow" sourceRef="start" targetRef="MATRIX_REVIEW"/>
                    <userTask id="MATRIX_REVIEW" name="Actual H02 transaction fixture" flowable:assignee="99"/>
                    <sequenceFlow id="finish" sourceRef="MATRIX_REVIEW" targetRef="end"/><endEvent id="end"/>
                  </process>
                </definitions>
                """).deploy().getId();
            when(bpm.createProcessInstance(any(),any())).thenAnswer(call->{
                cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO command=call.getArgument(1);
                return engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(command.getProcessDefinitionKey(),command.getBusinessKey(),command.getVariables(),"1").getId();});
            assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s->{submit(request());throw new IllegalStateException("late outer failure after real BPM and B freeze");}));
            assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());assertEquals(0,engine.getTaskService().createTaskQuery().count());
            assertEquals(3,files.selectListByMasterId(10L).size());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
            long candidateId=submit(request());var candidate=files.selectById(candidateId);
            assertNotNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(candidate.getProcessInstanceId()).singleResult());
            assertEquals(String.valueOf(candidateId),engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(candidate.getProcessInstanceId()).singleResult().getBusinessKey());
            assertEquals(1,engine.getTaskService().createTaskQuery().processInstanceId(candidate.getProcessInstanceId()).count());
            assertTrue(attributesMapper.find("REVISION",candidateId,1).getSubmitted());
            assertEquals(candidate.getProcessInstanceId(),jdbc.queryForObject("SELECT bpm_round FROM dcc_application_round_link WHERE application_id=?",String.class,candidateId));
        } finally {if(deployment!=null)engine.getRepositoryService().deleteDeployment(deployment,true);engine.close();}
    }
    private long submit(DccControlledFileSubmitIterationReqVO request){return new TransactionTemplate(manager).execute(s->workflow.submitWorkingIteration(99L,22L,request));}
    private DccControlledFileSubmitIterationReqVO request(){var req=new DccControlledFileSubmitIterationReqVO();req.setIdempotencyKey("formal-submit");req.setRevisionChangeType("REPLACEMENT");req.setChangeDescription("选定正文换版");req.setProjectAttributes(actual);req.setSelectedSignoffDepartmentIds(List.of(51L));req.setNeedTraining(false);req.setEffectiveDate(LocalDate.now().plusDays(20));return req;}
    private DccControlledFileDO file(long id,String version,String status){return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L)
            .dccProjectCodeId(5L).fileTypeTaxonomyId(6L).fileNumber("N-1").fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf")
            .sourceFileId(100L).originalFileId(100L).sourceSha256("frozen-hash").requesterId(99L).submitterId(99L).changeType(id==22L?"REVISION":"NEW").predecessorControlledFileId(id==22L?21L:null)
            .processType("CONTROLLED_FILE").revisionBaseActiveControlledFileId(id==22L?21L:null).versionNo(version).status(status).effectiveDate(LocalDate.now().plusDays(10)).build();}
    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node(int stage,String code,String source,long id,long actor){return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(stage,code,code,stage,source,id,List.of(id),"ANY",100,stage==1,List.of(actor));}
    private void wire(Object target,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(target,(String)pairs[i],pairs[i+1]);}
}
