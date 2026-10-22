package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual public controller + Workflow + B mapping/placement + D snapshot service and H2 transaction. */
class DccLinkedUploadBindingOrderDatabaseTest extends DccWorkflowAttributesIntegrationTest {
    @Resource DccProjectFolderMapper folders;
    @Resource DccProjectFilePlacementMapper placements;
    @Resource DccProjectFolderStorageMappingMapper storageMappings;
    @Resource DccFileDirectoryMapper directories;
    @Resource DccControlledFileRelatedFileMapper relations;
    @Resource DccControlledFileRouteSnapshotMapper routeRows;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper taskRows;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper temporaryTickets;
    private DccUploadTicketService tickets;
    private BpmProcessInstanceApi bpm;
    private DccControlledFileRelatedFileServiceImpl related;
    private DccControlledFileController controller;
    private GxpAuditService ledger;
    private String sourceTicket;
    private org.flowable.engine.ProcessEngine engine;

    @BeforeEach void linkedPublicFixture() {
        tickets=configurePublicCreation();
        set(workflow,"directoryMapper",directories);
        var taxonomy=(cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService)
                ReflectionTestUtils.getField(workflow,"fileTypeTaxonomyAdminService");
        when(taxonomy.resolveActiveCategoryId(6L)).thenReturn(2L);
        set(workflow,"approvedProductIdentityMapper",mock(DccApprovedProductIdentityMapper.class));
        var physical=(cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper)ReflectionTestUtils.getField(workflow,"fileMapper");
        when(physical.selectById(100L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder()
                .id(100L).name("SOP.pdf").type("application/pdf").size(8L).build());
        var realTickets=spy(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketServiceImpl());
        set(realTickets,"temporaryFileMapper",temporaryTickets,"fileMapper",physical);
        sourceTicket=realTickets.createTicket(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreateCommand(
                99L,2L,creationRequest().getSessionId(),"SOURCE",100L,"SOP.pdf","application/pdf",8L,
                "realbody".getBytes(java.nio.charset.StandardCharsets.UTF_8),"actual-isolated-preview")).uploadTicket();
        tickets=realTickets;set(workflow,"uploadTicketService",tickets);
        jdbc.update("INSERT INTO dcc_file_directory(id,tenant_id,code,name,active,sort) VALUES(3,1,'BASE-3','Actual base',1,0)");
        jdbc.update("INSERT INTO dcc_file_category(id,tenant_id,code,name,source,lifecycle_stage,active,file_type_taxonomy_id) VALUES(2,1,'SOP','Actual category','LOCAL','CONTROLLED',1,6)");
        jdbc.update("INSERT INTO dcc_category_directory_binding(category_id,directory_id,active,tenant_id) VALUES(2,3,1,1)");
        var folder=new DccProjectFolderDO();folder.setId(500L);folder.setTenantId(1L);folder.setProjectCodeId(5L);
        folder.setParentId(0L);folder.setName("Selected folder");folder.setSortOrder(0);folder.setActive(true);folders.insert(folder);
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,file_name,file_number,status,dcc_project_code_id,latest_controlled_file_id) VALUES(30,1,2,'Target','TARGET','ACTIVE_CHAIN',6,202)");
        files.insert(DccControlledFileDO.builder().id(202L).tenantId(1L).masterId(30L).categoryId(2L).directoryId(3L)
                .sourceFileId(100L).originalFileId(100L).fileName("Target.pdf").title("Target").fileNumber("TARGET")
                .versionNo("A/1").status("ACTIVE").dccProjectCodeId(6L).requesterId(88L).submitterId(88L)
                .controlledTime(LocalDateTime.of(2026,10,5,10,0)).build());
        var resolver=new DccLatestControlledFileResolverImpl();set(resolver,"fileMapper",files,"masterMapper",masters,"jdbc",jdbc);
        ledger=mock(GxpAuditService.class);
        related=spy(new DccControlledFileRelatedFileServiceImpl());
        set(related,"relatedFileMapper",relations,"controlledFileMapper",files,"controlledFileMasterMapper",masters,
                "latestFileResolver",resolver,"relationStore",new DccRelationStore(jdbc,ledger),"relationAccessPolicy",mock(DccRelationAccessPolicy.class));
        var query=mock(DccControlledFileQueryService.class);when(query.canViewFileName(eq(99L),any())).thenReturn(true);
        set(workflow,"relatedFileService",related,"queryService",query,"routeSnapshotMapper",routeRows,"taskAssigneeSnapshotMapper",taskRows);
        var actor=new LoginUser();actor.setId(99L);actor.setTenantId(1L);actor.setUserType(2);
        actor.setInfo(Map.of("username","linked-upload-test",LoginUser.INFO_KEY_NICKNAME,"Actor99"));
        SecurityFrameworkUtils.setLoginUser(actor,new MockHttpServletRequest());
        var access=mock(DccProjectAccessService.class);
        var permission=(DccControlledFileCategoryPermissionSupport)ReflectionTestUtils.getField(workflow,"categoryPermissionSupport");
        var storage=new DccProjectFolderStorageService();set(storage,"mappings",storageMappings,"directories",directories,"taxonomy",taxonomy,"permissions",permission);
        var reason=mock(DccProjectConfigurationAuditService.class);
        var folderService=mock(DccFolderTemplateService.class);when(folderService.requireProjectFolder(5L,500L)).thenReturn(folder);
        var placement=new DccProjectFilePlacementService();set(placement,"projects",projects,"files",files,"placements",placements,
                "folders",folderService,"access",access,"jdbc",jdbc,"audit",reason,"ledger",ledger);
        var publicUpload=new DccPublicUploadPlacementService();set(publicUpload,"storage",storage,"projects",projects,"folders",folders,
                "files",files,"access",access,"audit",reason,"placements",placement,"placementRows",placements);
        set(workflow,"publicUploadPlacementService",publicUpload);
        controller=new DccControlledFileController();set(controller,"workflowService",workflow,"publicUploadPlacementService",publicUpload);
        var readiness=mock(DccControlledFileRouteReadinessService.class);
        var route=DccCategoryApprovalRouteDO.builder().id(11L).versionNo(1).build();
        var node=new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(1,"MATRIX_REVIEW","Review",1,
                "USER",99L,List.of(99L),"SEQUENTIAL",100,true,List.of(99L));
        when(readiness.evaluateDepartments(any(),any(),any(),any())).thenReturn(
                new DccControlledFileRouteReadinessService.RouteReadinessEvaluation(
                        new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(route,List.of(node)),
                        DccControlledFileRouteReadinessRespVO.builder().ready(true).build()));
        var people=mock(AdminUserApi.class);when(people.getUserList(any())).thenReturn(List.of(new AdminUserRespDTO().setId(99L).setNickname("Actor99")));
        var config=new org.flowable.spring.SpringProcessEngineConfiguration();config.setDataSource(dataSource);config.setTransactionManager(manager);
        config.setDatabaseType("mysql");config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);
        engine=config.buildProcessEngine();
        String xml="""
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" targetNamespace="g56-binding-order">
                  <process id="dcc-controlled-file-upload" name="Actual isolated upload" isExecutable="true">
                    <startEvent id="start"/><sequenceFlow id="toReview" sourceRef="start" targetRef="MATRIX_REVIEW"/>
                    <userTask id="MATRIX_REVIEW" name="Actual review"/><sequenceFlow id="toEnd" sourceRef="MATRIX_REVIEW" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """;
        engine.getRepositoryService().createDeployment().tenantId("1").addString("upload.bpmn20.xml",xml).deploy();
        bpm=mock(BpmProcessInstanceApi.class);when(bpm.createProcessInstance(any(),any())).thenAnswer(call->{
            var request=call.getArgument(1,cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO.class);
            var submitted=files.selectById(Long.valueOf(request.getBusinessKey()));assertEquals("PENDING_MATRIX_REVIEW",submitted.getStatus());
            assertEquals(1,relations.selectListByControlledFileId(submitted.getId()).size());
            return engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(request.getProcessDefinitionKey(),
                    request.getBusinessKey(),request.getVariables(),"1").getId();
        });
        set(workflow,"routeReadinessService",readiness,"approvalRouteAssigneeResolver",new DccControlledFileApprovalRouteAssigneeResolver(),
                "adminUserApi",people,"bpmProcessInstanceApi",bpm,"platformAdapter",mock(DccControlledContentAdapter.class));
    }

    @AfterEach void cleanOwnTables() {
        SecurityContextHolder.clearContext();
        if(engine!=null) {
            for(var deployment:engine.getRepositoryService().createDeploymentQuery().list())engine.getRepositoryService().deleteDeployment(deployment.getId(),true);
            engine.close();
        }
        for(String table:List.of("dcc_project_file_placement","dcc_project_folder_storage_mapping","dcc_controlled_file_related_file"))jdbc.update("DELETE FROM "+table);
    }

    private DccControlledFileSubmitReqVO linkedRequest() {
        var request=creationRequest();request.setOriginalUploadTicket(sourceTicket);request.setVersionNo("A/1");request.setNeedTraining(true);
        ReflectionTestUtils.setField(request,"directoryId",null);ReflectionTestUtils.setField(request,"directoryIdProvided",false);
        request.setProjectFolderId(500L);request.setProjectFolderChangeReason("Exact public upload location");
        request.setRelatedControlledFileIds(List.of(202L));return request;
    }
    private <T> T transaction(Supplier<T> action) { return new TransactionTemplate(manager).execute(s->action.get()); }
    private long submit() { return transaction(()->controller.submitControlledFile(linkedRequest()).getData()); }
    private static void set(Object target,Object... fields) { for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(target,(String)fields[i],fields[i+1]); }

    @Test void selectedRelatedSnapshotBindsDuringWorkingBeforePendingAndBpmAreCreated() {
        long id=submit();var saved=files.selectById(id);
        assertEquals("PENDING_MATRIX_REVIEW",saved.getStatus());assertNotNull(saved.getProcessInstanceId());
        assertNotNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(saved.getProcessInstanceId()).singleResult());
        assertTrue(saved.getNeedTraining());assertEquals(1,relations.selectListByControlledFileId(id).size());
        assertEquals(202L,relations.selectListByControlledFileId(id).get(0).getRelatedControlledFileId());
        assertEquals("A/1",relations.selectListByControlledFileId(id).get(0).getRelatedVersionNoSnapshot());
        assertNotNull(placements.findFile(id));verify(bpm,times(1)).createProcessInstance(any(),any());
    }

    @Test void exactReplayReturnsTheOriginalFileWithoutAnotherRelationTicketBindingOrBpm() {
        long id=submit();long replay=submit();assertEquals(id,replay);
        assertEquals(1,relations.selectListByControlledFileId(id).size());
        verify(related,times(1)).validateAndBindRelatedFiles(any(),any(),any());verify(tickets,times(1)).markBound(any());
        verify(bpm,times(1)).createProcessInstance(any(),any());assertEquals(1,engine.getRuntimeService().createProcessInstanceQuery().count());
    }

    @Test void lateTicketFailureRollsBackActualNewMasterNameSnapshotMappingRelationsAndTicket() {
        var before=counts();
        doAnswer(call->{call.callRealMethod();throw new IllegalStateException("ACTUAL_LATE_TICKET_BINDING_FAILURE");}).when(tickets).markBound(any());
        assertThrows(IllegalStateException.class,this::submit);
        assertEquals(before,counts());assertEquals("AVAILABLE",temporaryTickets.selectOne(
                cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getUploadTicket,sourceTicket).getStatus());
        assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());verifyNoInteractions(bpm);
    }

    @Test void placementAuditFailureAfterRealBpmCreationRollsBackTheEntirePublicSubmission() {
        var before=counts();doThrow(new IllegalStateException("ACTUAL_PLACEMENT_AUDIT_FAILURE")).when(ledger).append(any());
        assertThrows(IllegalStateException.class,this::submit);
        assertEquals(before,counts());assertEquals(0,engine.getRuntimeService().createProcessInstanceQuery().count());
        assertEquals("AVAILABLE",temporaryTickets.selectOne(
                cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO::getUploadTicket,sourceTicket).getStatus());
        verify(bpm,times(1)).createProcessInstance(any(),any());
    }

    @Test void existingWithoutApprovalCallerKeepsFinalizingAfterBindingItsOwnUnsubmittedSnapshot() {
        // This existing formal caller explicitly revises an actual same-tenant controlled source;
        // it is not evidence for the unrelated legacy NEW master default-tenant insertion path.
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP,activated_time=CURRENT_TIMESTAMP WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=20,latest_controlled_file_id=20 WHERE id=10");
        var request=linkedRequest();request.setDirectoryId(3L);request.setChangeType("REVISION");request.setFileNumber("N-1");
        request.setVersionNo("A/2");request.setRevisionSourceControlledFileId(20L);request.setRevisionSourceReason("Explicit controlled baseline revision");
        var sources=(DccControlledFileSourceOwnershipService)ReflectionTestUtils.getField(workflow,"sourceOwnershipService");
        when(sources.prepareSubmissionSource(100L,true)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"real-fixture-source-hash",false));
        var finalization=mock(DccControlledFileFinalizationService.class);set(workflow,"finalizationService",finalization);
        long id=transaction(()->workflow.submitControlledFileWithoutApproval(99L,request));
        assertEquals("FINALIZING",files.selectById(id).getStatus());assertNull(files.selectById(id).getProcessInstanceId());
        assertEquals(1,relations.selectListByControlledFileId(id).size());verify(finalization).activateWithoutApproval(id,true);
        verifyNoInteractions(bpm);
    }

    private Map<String,Integer> counts() {
        var result=new LinkedHashMap<String,Integer>();
        for(String table:List.of("dcc_controlled_file","dcc_controlled_file_master","dcc_controlled_file_name_claim",
                "dcc_source_name_reservation","dcc_application_round_link","dcc_project_application_attributes",
                "dcc_project_file_placement","dcc_project_folder_storage_mapping","dcc_controlled_file_related_file","dcc_file_directory"))
            result.put(table,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class));
        return result;
    }
}
