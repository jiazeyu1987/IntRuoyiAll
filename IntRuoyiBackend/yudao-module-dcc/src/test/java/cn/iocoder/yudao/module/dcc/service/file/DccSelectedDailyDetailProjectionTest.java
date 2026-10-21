package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileVersionHistoryRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real reader -> Query -> history/action projection and real Guard. Only persistence/external ports are mocks. */
class DccSelectedDailyDetailProjectionTest {
    private static final Long ACTOR=99L, ACTIVE=9007199254740993L, WORKING=9007199254740995L, MASTER=700L;
    private final DccControlledFileQueryServiceImpl query=new DccControlledFileQueryServiceImpl();
    private final DccControlledFileDetailAuthorizationGuard guard=new DccControlledFileDetailAuthorizationGuard();
    private final DccSelectedDetailReadService reader=new DccSelectedDetailReadService();
    private DccControlledFileDO active,working;

    @BeforeEach void actualOwnerChain() throws Exception {
        TenantContextHolder.setTenantId(31L);
        var login=new LoginUser();login.setId(ACTOR);login.setTenantId(31L);login.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        initializePorts(query);initializePorts(guard);
        for(String name:List.of("assignmentScopeService","directoryAccessPermissionService","permissionSupport",
                "viewMatrixAccessService","distributionRecipientMapper","routeSnapshotMapper","bpmTaskService",
                "offlineTraining","adminUserApi","projectAccessService","bpmProcessInstanceService"))
            ReflectionTestUtils.setField(guard,name,ReflectionTestUtils.getField(query,name));
        ReflectionTestUtils.setField(guard,"masterMapper",ReflectionTestUtils.getField(query,"controlledFileMasterMapper"));
        ReflectionTestUtils.setField(guard,"applicationRoundService",ReflectionTestUtils.getField(query,"applicationRoundService"));
        ReflectionTestUtils.setField(guard,"boundWorkflowContractResolver",ReflectionTestUtils.getField(query,"roundFlowContracts"));
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        ReflectionTestUtils.setField(query,"downloadPolicyService",new DccDownloadPolicyService());
        ReflectionTestUtils.setField(reader,"files",ReflectionTestUtils.getField(query,"controlledFileMapper"));
        ReflectionTestUtils.setField(reader,"query",query);ReflectionTestUtils.setField(reader,"guard",guard);
        active=file(ACTIVE,"ACTIVE","A/1",501L);active.setPublishedFileId(501L);
        active.setControlledTime(LocalDateTime.of(2026,10,15,9,0));active.setActivatedTime(LocalDateTime.of(2026,10,15,9,1));
        working=file(WORKING,"WORKING","A/1-2",502L);
        DccControlledFileMapper files=port(query,"controlledFileMapper");
        when(files.selectById(ACTIVE)).thenReturn(active);when(files.selectById(WORKING)).thenReturn(working);
        when(files.selectListByMasterId(MASTER)).thenReturn(List.of(active,working));
        var master=DccControlledFileMasterDO.builder().id(MASTER).tenantId(31L).dccProjectCodeId(300L)
                .categoryId(10L).fileTypeTaxonomyLeafId(66L).normalizedFileNumber("F01-SOP")
                .currentActiveControlledFileId(ACTIVE).latestControlledFileId(ACTIVE).build();
        DccControlledFileMasterMapper masters=port(query,"controlledFileMasterMapper");when(masters.selectById(MASTER)).thenReturn(master);
        var project=DccProjectCodeDO.builder().id(300L).projectName("F01 project").projectCode("F01").status("ENABLE").build();
        project.setTenantId(31L);DccProjectCodeMapper projects=port(query,"projectCodeMapper");when(projects.selectById(300L)).thenReturn(project);
        FileMapper binaries=port(query,"fileMapper");
        for(Long id:List.of(501L,502L))when(binaries.selectById(id)).thenReturn(FileDO.builder().id(id).configId(1L)
                .name("exact-"+id+".pdf").type("application/pdf").path("f01/"+id+".pdf").build());
        DccControlledFileAssignmentScopeService assignments=port(query,"assignmentScopeService");
        when(assignments.isWithinAssignedFileScope(eq(ACTOR),anyLong())).thenReturn(true);
        DccProjectAccessService access=port(query,"projectAccessService");when(access.listReadableProjectIds(ACTOR)).thenReturn(List.of(300L));
        DccDirectoryAccessPermissionService directories=port(query,"directoryAccessPermissionService");
        when(directories.getAuthorizedDirectoryIds(ACTOR,DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(20L));
        when(directories.getAuthorizedDirectoryIds(ACTOR,DccAccessTypeEnum.QUERY)).thenReturn(Set.of(20L));
        var account=new AdminUserRespDTO();account.setId(ACTOR);account.setTenantId(31L);account.setStatus(0);
        AdminUserApi users=port(query,"adminUserApi");when(users.getUser(ACTOR)).thenReturn(account);
    }
    @AfterEach void clearSubject(){SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    private void initializePorts(Object target) throws Exception {
        for(var field:target.getClass().getDeclaredFields())
            if(!Modifier.isStatic(field.getModifiers())&&field.getAnnotation(Resource.class)!=null)
                ReflectionTestUtils.setField(target,field.getName(),mock(field.getType()));
    }
    @SuppressWarnings("unchecked") private <T>T port(Object owner,String name){return (T)ReflectionTestUtils.getField(owner,name);}
    private DccControlledFileDO file(Long id,String status,String version,Long source) {
        return DccControlledFileDO.builder().id(id).tenantId(31L).masterId(MASTER).dccProjectCodeId(300L)
                .categoryId(10L).fileTypeTaxonomyId(66L).directoryId(20L).requesterId(ACTOR).submitterId(ACTOR)
                .versionNo(version).status(status).sourceFileId(source).originalFileId(source).sourceSha256("f01-exact-source")
                .fileNumber("F01-SOP").fileName("F01.pdf").title("F01").changeType("NEW").build();
    }
    private DccControlledFileVersionHistoryRespVO workingOption(DccControlledFileRespVO response){
        return response.getVersionHistory().stream().filter(row->WORKING.equals(row.getId())).findFirst().orElseThrow();
    }
    @Test void explicitDailyDetailClosesSameMasterRequesterWorkingPreview() {
        var generic=query.getControlledFile(ACTOR,ACTIVE);
        var original=workingOption(generic);
        assertTrue(original.getCanPreview(),"fixture must prove the real requester WORKING preview grant");
        assertTrue(original.getActionProjection().getAllowedActions().contains("PREVIEW"));
        var selected=reader.read(ACTOR,ACTIVE,DccControlledFileBodyReadContext.daily(ACTIVE));
        assertTrue(selected.getCanPreview());assertEquals(ACTIVE,selected.getId());
        var history=workingOption(selected);
        assertFalse(history.getCanPreview(),"DAILY selected detail must not expose the WORKING preview entry");
        assertFalse(history.getActionProjection().getAllowedActions().contains("PREVIEW"));
        assertEquals(original.getActionProjection().getAllowedActions().stream().filter(action->!"PREVIEW".equals(action)).toList(),
                history.getActionProjection().getAllowedActions());
        assertEquals("A/1-2",history.getVersionNo());assertEquals(WORKING,history.getId());
        assertTrue(workingOption(query.getControlledFile(ACTOR,ACTIVE)).getCanPreview(),"generic authoring projection must remain unchanged");
    }
    @Test void dailyWorkingIsDeniedByRealGuardWithoutPollutingLaterGenericRead() {
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->reader.read(ACTOR,WORKING,DccControlledFileBodyReadContext.daily(WORKING)));
        assertTrue(workingOption(query.getControlledFile(ACTOR,ACTIVE)).getCanPreview());
    }
    @ParameterizedTest
    @EnumSource(value=DccControlledFileBodyReadContext.Intent.class,names={"AUTHORING","APPROVAL_TASK","APPROVAL_HISTORY","VERSION_HISTORY"})
    void legalNonDailyContextKeepsItsAuthorizedPreview(DccControlledFileBodyReadContext.Intent intent) {
        Long id=intent==DccControlledFileBodyReadContext.Intent.AUTHORING?WORKING:ACTIVE;
        DccControlledFileDO selected=id.equals(WORKING)?working:active;
        if(intent==DccControlledFileBodyReadContext.Intent.AUTHORING){
            DccProjectAccessService access=port(query,"projectAccessService");when(access.hasProjectEditorOrOwner(ACTOR,300L)).thenReturn(true);
            DccControlledFileCategoryPermissionSupport permissions=port(query,"permissionSupport");
            when(permissions.hasCategoryPermission(10L,ACTOR,DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(true);
            DccControlledFileAttachmentService attachments=port(query,"attachmentService");
            when(attachments.readOrdinaryAttachmentManifest(ACTOR,WORKING)).thenReturn(
                    new DccControlledFileAttachmentService.OrdinaryAttachmentManifest(List.of(),"a".repeat(64)));
        }
        boolean application=intent==DccControlledFileBodyReadContext.Intent.APPROVAL_TASK||intent==DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY;
        if(application)bindRound(intent==DccControlledFileBodyReadContext.Intent.APPROVAL_TASK);
        var context=new DccControlledFileBodyReadContext(intent,id.toString(),selected.getVersionNo(),
                application?"UPLOAD":null,application?"f01-round":null,application?"f01-task":null);
        var result=reader.read(ACTOR,id,context);
        assertTrue(result.getCanPreview());assertTrue(result.getActionProjection().getAllowedActions().contains("PREVIEW"));
        assertTrue(workingOption(result).getCanPreview(),"non-DAILY must not inherit daily option filtering");
    }
    private void bindRound(boolean activeTask){
        BpmProcessInstanceService processes=port(query,"bpmProcessInstanceService");
        var history=mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(processes.getHistoricProcessInstance("f01-round")).thenReturn(history);
        when(history.getId()).thenReturn("f01-round");when(history.getTenantId()).thenReturn("31");
        when(history.getStartUserId()).thenReturn("99");when(history.getProcessDefinitionId()).thenReturn("f01-definition");
        when(history.getBusinessKey()).thenReturn(ACTIVE.toString());
        DccApplicationRoundService rounds=port(query,"applicationRoundService");when(rounds.require("UPLOAD",ACTIVE,"f01-round")).thenReturn(1);
        DccBoundWorkflowContractResolver contracts=port(query,"roundFlowContracts");
        when(contracts.resolveRoundFlowContract(eq(active),any(DccApplicationRoundSummary.class))).thenReturn(
                new DccBoundWorkflowContractResolver.RoundFlowContract(31L,ACTIVE,"UPLOAD",1,"f01-round",
                        DccControlledFileProcessDefinitionKeys.UPLOAD,"f01-definition","f01-deployment","a".repeat(64),
                        "PUBLIC_MATRIX_NATIVE_V1",DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,
                        false,null,null,DccBoundWorkflowContractResolver.RoundContractStatus.READY,null));
        BpmTaskService tasks=port(query,"bpmTaskService");
        if(activeTask){
            var runtime=mock(org.flowable.engine.runtime.ProcessInstance.class);when(processes.getProcessInstance("f01-round")).thenReturn(runtime);
            when(runtime.getId()).thenReturn("f01-round");when(runtime.getTenantId()).thenReturn("31");
            when(runtime.getProcessDefinitionId()).thenReturn("f01-definition");when(runtime.getBusinessKey()).thenReturn(ACTIVE.toString());
            var task=mock(org.flowable.task.api.Task.class);when(tasks.getTask("f01-task")).thenReturn(task);
            when(task.getId()).thenReturn("f01-task");when(task.getTenantId()).thenReturn("31");when(task.getAssignee()).thenReturn("99");
            when(task.getProcessInstanceId()).thenReturn("f01-round");when(task.getProcessDefinitionId()).thenReturn("f01-definition");
        }else{
            var task=mock(org.flowable.task.api.history.HistoricTaskInstance.class);when(tasks.getHistoricTask("f01-task")).thenReturn(task);
            when(task.getId()).thenReturn("f01-task");when(task.getTenantId()).thenReturn("31");when(task.getAssignee()).thenReturn("99");
            when(task.getProcessInstanceId()).thenReturn("f01-round");when(task.getProcessDefinitionId()).thenReturn("f01-definition");
            when(task.getEndTime()).thenReturn(new java.util.Date());
        }
    }
}
