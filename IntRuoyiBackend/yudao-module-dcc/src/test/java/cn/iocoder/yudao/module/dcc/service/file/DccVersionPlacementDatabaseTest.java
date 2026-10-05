package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Query/revision/A/B and Flowable; selected body stays in the source's formally registered logical folder. */
@Import({DccPublicUploadPlacementService.class,DccProjectFilePlacementService.class,DccFolderTemplateService.class,
    DccProjectConfigurationAuditService.class,GxpAuditServiceImpl.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccVersionPlacementDatabaseTest extends DccWorkflowSelectedIterationDatabaseTest {
    @MockitoBean DccProjectFolderStorageService newUploadStorage;
    @MockitoBean cn.iocoder.yudao.module.system.api.user.AdminUserApi currentAuditAccounts;
    @Resource DccPublicUploadPlacementService locations;
    @Resource DccProjectFilePlacementService placements;
    @Resource DccProjectFilePlacementMapper placementRows;
    @Resource DccProjectFolderMapper folders;
    @Resource GxpAuditPolicyOperationMapper policies;
    @MockitoBean PermissionApi permissionApi;
    Long folderId;
    @BeforeEach void placementFixture(){
        org.mockito.Mockito.when(currentAuditAccounts.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                .setId(99L).setTenantId(1L).setStatus(0).setUsername("placement-test").setNickname("Placement test"));
        var login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(2);
        login.setInfo(Map.of("username","placement-test",LoginUser.INFO_KEY_NICKNAME,"Placement test"));SecurityFrameworkUtils.setLoginUser(login,new MockHttpServletRequest());
        jdbc.update("INSERT INTO dcc_file_directory(id,tenant_id,code,name,active,sort) VALUES(3,1,'NAS-3','Storage only',1,0)");
        var folder=new DccProjectFolderDO();folder.setTenantId(1L);folder.setProjectCodeId(5L);folder.setParentId(0L);folder.setName("Exact registered location");folder.setSortOrder(0);folder.setActive(true);folder.setSourceTemplateId(1L);folder.setSourceNodeKey("quality");folders.insert(folder);folderId=folder.getId();
        var p=new GxpAuditPolicyOperationDO();p.setTenantId(1L);p.setOperationId("dcc.project-file-placement.bind");p.setPolicyVersion("PLACEMENT-TEST");p.setSourceType("SERVICE_METHOD");p.setSourceLocator("ISOLATED_TEST");p.setDomain("DCC");p.setSubjectType("PLACEMENT");p.setActionType("CREATE");p.setReasonPolicy("REQUIRED");p.setSignaturePolicy("NOT_REQUIRED");p.setStatePolicy("BEFORE_AFTER");p.setRetentionClass("TEST");p.setTestIds("P03");p.setOwner("Root");p.setApplicability("GXP");p.setActive(true);policies.insert(p);
        for(Object service:List.of(query,workflow,revisions)) if(Arrays.stream(org.springframework.aop.support.AopUtils.getTargetClass(service).getDeclaredFields()).anyMatch(f->f.getName().equals("publicUploadPlacementService")))wire(service,"publicUploadPlacementService",locations);
    }
    @AfterEach void clearSecurity(){SecurityContextHolder.clearContext();}
    void register(){tx().execute(s->placements.bind(99L,5L,folderId,20L,3L,"Original selected project location"));}
    @Test void publicPlacementCheckinAndInitialCandidateInheritExactRegisteredFolder(){
        initial();register();long iteration=checkin(20L,"initial-location");
        assertEquals(folderId,placements.require(99L,5L,iteration).getProjectFolderId());
        long candidate=workflow.submitWorkingIteration(99L,iteration,request("INITIAL"));
        assertEquals(folderId,placements.require(99L,5L,candidate).getProjectFolderId());
        assertEquals(3L,placements.require(99L,5L,candidate).getStorageDirectoryId());
        assertEquals(3,placementRows.selectList().size());
    }
    @Test void publicPlacementRevisionCandidateInheritsSelectedIterationAndRollsBackOnLateFailure(){
        controlled("A/1");register();long iteration=checkin(20L,"revision-location");
        assertEquals(folderId,placements.require(99L,5L,iteration).getProjectFolderId());
        int bodiesBefore=bytes.size();
        assertThrows(IllegalStateException.class,()->tx().execute(s->{long candidate=workflow.submitWorkingIteration(99L,iteration,request("PARTIAL"));assertEquals(folderId,placements.require(99L,5L,candidate).getProjectFolderId());throw new IllegalStateException("later BPM caller failure");}));
        assertEquals(2,placementRows.selectList().size());assertEquals(bodiesBefore,bytes.size());
    }
    @Test void publicPlacementLegacyUnregisteredSourceStaysExplicitlyUnregistered(){
        initial();long iteration=checkin(20L,"legacy-location");assertNull(placementRows.findFile(iteration));
        long candidate=workflow.submitWorkingIteration(99L,iteration,request("INITIAL"));assertNull(placementRows.findFile(candidate));
    }
    @Test void publicPlacementRealFlowableTaskSuppliesTheExactAssignmentObligation(){
        initial();register();long iteration=checkin(20L,"assignment-context");
        long candidate=workflow.submitWorkingIteration(99L,iteration,request("INITIAL"));
        var task=engine.getTaskService().createTaskQuery().processInstanceId(files.selectById(candidate).getProcessInstanceId())
                .taskDefinitionKey("MATRIX_REVIEW").includeTaskLocalVariables().singleResult();assertNotNull(task);
        var bpm=org.mockito.Mockito.mock(cn.iocoder.yudao.module.bpm.service.task.BpmTaskService.class);
        org.mockito.Mockito.when(bpm.getTask(task.getId())).thenReturn(task);org.mockito.Mockito.when(bpm.validateTask(99L,task.getId())).thenReturn(task);
        var users=org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);
        var leader=new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(99L).setNickname("Leader").setStatus(0).setDeptId(51L).setPostIds(Set.of(1L));
        org.mockito.Mockito.when(users.getUser(99L)).thenReturn(leader);org.mockito.Mockito.when(users.getUserListByDeptIds(List.of(51L))).thenReturn(List.of(leader));
        var definitions=new cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionServiceImpl();wire(definitions,"repositoryService",engine.getRepositoryService());
        var assignment=new DccWorkflowSignoffAssignmentService();wire(assignment,"fileMapper",files,"snapshotMapper",obligations,"bpmTaskService",bpm,"adminUserApi",users,"definitions",definitions);
        var context=assignment.assignmentContext(99L,candidate,task.getId());
        assertEquals(51L,context.departmentId());assertEquals(task.getProcessInstanceId(),context.processInstanceId());
        assertEquals(engine.getRepositoryService().getProcessDefinition(task.getProcessDefinitionId()).getKey(),context.processDefinitionKey());
        org.mockito.Mockito.verifyNoInteractions(newUploadStorage);
        assertTrue(context.canAssign());assertFalse(context.assigned());assertEquals(1,context.assigneeOptions().size());
        assertEquals(task.getTaskLocalVariables().get(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID),context.obligationId());
    }
    @Test void publicPlacementProjectThenFolderLockPrecedesMasterAcrossTwoConnections() throws Exception {
        initial();register();checkout(20L);
        var held=new java.util.concurrent.CountDownLatch(1);var requested=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        org.mockito.Mockito.doAnswer(c->{if(Thread.currentThread().getName().equals("placement-checkin"))requested.countDown();return sql.selectOne(DccProjectCodeMapper.class.getName()+".selectByIdForUpdate",Map.of("id",c.getArgument(0)));}).when(projects).selectByIdForUpdate(5L);
        try{
            var owner=executor.submit(()->{cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);try{tx().executeWithoutResult(s->{locations.lockSourceLocation(99L,20L);held.countDown();await(release);masters.selectByIdForUpdate(10L);files.selectByIdAndTenantForUpdate(1L,20L);});}finally{cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}});
            assertTrue(held.await(5,java.util.concurrent.TimeUnit.SECONDS));
            var edit=executor.submit(()->{Thread.currentThread().setName("placement-checkin");cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);try{
                var login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(2);login.setInfo(Map.of("username","concurrent-test",LoginUser.INFO_KEY_NICKNAME,"Concurrent test"));SecurityFrameworkUtils.setLoginUser(login,new MockHttpServletRequest());
                return query.checkinControlledFile(99L,20L,edit("project-folder-lock")).getId();
            }finally{cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();SecurityContextHolder.clearContext();}});
            assertTrue(requested.await(5,java.util.concurrent.TimeUnit.SECONDS));release.countDown();owner.get(8,java.util.concurrent.TimeUnit.SECONDS);
            long id=edit.get(8,java.util.concurrent.TimeUnit.SECONDS);assertEquals(folderId,placements.require(99L,5L,id).getProjectFolderId());
        }finally{release.countDown();executor.shutdownNow();}
    }
}
