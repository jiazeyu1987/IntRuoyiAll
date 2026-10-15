package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** New-stage permission consumption: actual H2 rounds/attributes, immutable model and actual Flowable supervisor task. */
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w08_supervisor_read;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=5000")
class DccW08SupervisorReadDatabaseTest extends DccPreparedExecutionInheritanceDatabaseTest {
    private DccControlledFileQueryServiceImpl query;
    private DccControlledFileDetailAuthorizationGuard detail;
    private DccBoundWorkflowContractResolver bound;
    private AdminUserApi actors;
    private BpmTaskService tasks;
    private String publicId,bpm,taskId;
    private Long officeEventId;
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO issuedMetadata;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper officeEvents;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper officeTraces;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper officeLogs;

    private <T> T declaredExternalPorts(T service) {
        for(var field:service.getClass().getDeclaredFields())
            if(!Modifier.isStatic(field.getModifiers())&&field.isAnnotationPresent(Resource.class))
                ReflectionTestUtils.setField(service,field.getName(),mock(field.getType()));
        return service;
    }
    private void wireIfPresent(Object target,String name,Object value){
        if(Arrays.stream(target.getClass().getDeclaredFields()).anyMatch(field->field.getName().equals(name)))wire(target,name,value);
    }
    @BeforeEach void newBoundRound(TestInfo info) throws Exception {
        if(info.getTestMethod().orElseThrow().getDeclaringClass()!=DccW08SupervisorReadDatabaseTest.class)return;
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        publicId=deploy(Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml")));
        var prepared=contracts.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC,publicId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(publicId)),DccControlledFileProcessDefinitionKeys.UPLOAD);
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=? WHERE id=20",
                prepared.executionContract(),prepared.processDefinitionId(),prepared.definitionDigest());
        org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId("99");
        try{bpm=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(publicId).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"needTraining",false,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("DECLARED-OBLIGATION")))).start().getId();}
        finally{org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId(null);}
        tx().executeWithoutResult(status->snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,rounds.requireDraft("UPLOAD",20L),bpm,actual));
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_SUPERVISOR_REVIEW',process_instance_id=?,process_definition_key=?,submitted_time=? WHERE id=20",bpm,DccControlledFileProcessDefinitionKeys.UPLOAD,LocalDateTime.now());
        var task=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult();
        assertEquals("SUPERVISOR_REVIEW",task.getTaskDefinitionKey());taskId=task.getId();engine.getTaskService().setAssignee(taskId,"77");
        bound=new DccBoundWorkflowContractResolver();wire(bound,"definitions",definitions,"processes",processes,"rounds",rounds,"attributes",attributeRows);
        var actualTasks=new BpmTaskServiceImpl();wire(actualTasks,"taskService",engine.getTaskService());tasks=spy(actualTasks);
        actors=mock(AdminUserApi.class);
        when(actors.getUser(77L)).thenReturn(new AdminUserRespDTO().setId(77L).setTenantId(1L).setStatus(0));
        when(actors.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        query=declaredExternalPorts(new DccControlledFileQueryServiceImpl());detail=declaredExternalPorts(new DccControlledFileDetailAuthorizationGuard());
        wire(query,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"bpmTaskService",tasks,"adminUserApi",actors,
                "versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"detailAuthorizationGuard",detail,
                "downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
        wire(detail,"bpmTaskService",tasks);
        wireIfPresent(query,"boundWorkflowContractResolver",bound);wireIfPresent(detail,"boundWorkflowContractResolver",bound);wireIfPresent(detail,"adminUserApi",actors);
        wireIfPresent(detail,"projectAccessService",access);wireIfPresent(detail,"bpmProcessInstanceService",processes);
        var infra=(FileMapper)ReflectionTestUtils.getField(query,"fileMapper");
        when(infra.selectById(100L)).thenReturn(FileDO.builder().id(100L).configId(1L).name("bound-A1.pdf").type("application/pdf").path("actual-bound/A1.pdf").build());
        var preview=(DccControlledPreviewAccessService)ReflectionTestUtils.getField(query,"previewAccessService");
        var now=LocalDateTime.now();
        when(preview.prepareAccess(any())).thenReturn(new DccPreviewAccessResult(30L,"event",31L,"trace","declared-token","id","nonce",now,now.plusMinutes(15),"{}"));
        var business=(BusinessFileAccessService)ReflectionTestUtils.getField(query,"businessFileAccessService");
        when(business.assertAllowed(any())).thenAnswer(call->{var request=call.<BusinessFileAccessRequest>getArgument(0);
            assertEquals(100L,request.fileId());query.assertBusinessFileAccess(request.userId(),20L,request.operation(),audit());
            return Optional.of(new BusinessFileAccessReference("dcc","DCC_CONTROLLED_FILE",20L,"A/1",1L,null));});
        login(77L,1L);
    }
    @AfterEach void clearLogin(){SecurityContextHolder.clearContext();}
    private void login(Long actor,Long tenant){var principal=new LoginUser();principal.setId(actor);principal.setTenantId(tenant);principal.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of()));}
    private DccRequestAuditContext audit(){return new DccRequestAuditContext("127.0.0.1","task-owned supervisor permission fixture","w08-supervisor-read");}
    private DccControlledFileDO file(){return files.selectById(20L);}
    private boolean body(Long actor){return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query,"canReadBinary",actor,file(),DccAccessTypeEnum.PREVIEW,false));}
    private void noPreview(){assertFalse(body(77L));assertFalse(detail.isAllowed(77L,file(),false,null));}

    @Test void exactPublicActiveSupervisorCanReadThisBodyAndPreviewMetadataOnly(){
        var resolved=bound.resolve(file());assertTrue(resolved.active());assertEquals(1,resolved.attributeRound());
        assertTrue(body(77L));assertTrue(detail.isAllowed(77L,file(),false,null));
        var metadata=query.getPreviewMetadata(77L,20L,audit());assertEquals("PDF",metadata.getPreviewKind());assertEquals("declared-token",metadata.getViewerToken());
        assertEquals(publicId,resolved.processDefinitionId());assertEquals("A/1",file().getVersionNo());
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query,"canReadBinary",77L,file(),DccAccessTypeEnum.DOWNLOAD,false)));
        assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query,"canPrintControlledFile",77L,file())));
        verifyNoInteractions((DccProjectAccessService)ReflectionTestUtils.getField(query,"projectAccessService"));
        assertNull(masters.selectById(10L).getCurrentActiveControlledFileId());
    }
    @ParameterizedTest @ValueSource(strings={"other","requester","candidate","manager","anonymous","disabled","principalTenant","ignore"})
    void namesRolesOrCandidateMembershipCannotReplaceTheExactHumanSupervisorTask(String invalid){
        switch(invalid){
            case "other"->engine.getTaskService().setAssignee(taskId,"78");
            case "requester"->{login(99L,1L);assertFalse(body(99L));assertFalse(detail.isAllowed(99L,file(),true,null));return;}
            case "candidate"->{engine.getTaskService().setAssignee(taskId,null);engine.getTaskService().addCandidateUser(taskId,"77");}
            case "manager"->{engine.getTaskService().setAssignee(taskId,"78");assertFalse(detail.isAllowed(77L,file(),true,null));}
            case "anonymous"->SecurityContextHolder.clearContext();
            case "disabled"->actors.getUser(77L).setStatus(1);
            case "principalTenant"->login(77L,2L);
            case "ignore"->TenantContextHolder.setIgnore(true);
        }
        noPreview();
    }
    @ParameterizedTest @ValueSource(strings={"oldRound","wrongTenant","suspended","wrongTaskTenant","wrongTaskProcess","wrongTaskDefinition","matrixTask"})
    void aStatusStringCannotAuthorizeAnOldRoundOrAnotherTaskIdentity(String invalid){
        switch(invalid){
            case "oldRound"->jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='old-round' WHERE id=20");
            case "wrongTenant"->{var other=file();other.setTenantId(2L);assertThrows(IllegalArgumentException.class,()->ReflectionTestUtils.invokeMethod(query,"hasCurrentRunningApprovalTask",77L,other));return;}
            case "suspended"->engine.getRepositoryService().suspendProcessDefinitionById(publicId,true,null);
            case "matrixTask"->{engine.getTaskService().complete(taskId);var next=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult();engine.getTaskService().setAssignee(next.getId(),"77");}
            default->{var actual=engine.getTaskService().createTaskQuery().taskId(taskId).singleResult();var corrupted=mock(org.flowable.task.api.Task.class);
                when(corrupted.getTaskDefinitionKey()).thenReturn(actual.getTaskDefinitionKey());when(corrupted.getAssignee()).thenReturn(actual.getAssignee());
                when(corrupted.getTenantId()).thenReturn("wrongTaskTenant".equals(invalid)?"2":"1");
                when(corrupted.getProcessInstanceId()).thenReturn("wrongTaskProcess".equals(invalid)?"old-round":bpm);
                when(corrupted.getProcessDefinitionId()).thenReturn("wrongTaskDefinition".equals(invalid)?exactId:publicId);
                doReturn(List.of(corrupted)).when(tasks).getRunningTaskListByProcessInstanceId(bpm,null,null);}
        }
        if("oldRound".equals(invalid))assertThrows(IllegalStateException.class,()->body(77L));
        else noPreview();
    }
    @ParameterizedTest @ValueSource(strings={"missingPrepared","partialPrepared","wrongDigest","missingRound","wrongProject","unsubmitted","unknownGraph","legacyGraph"})
    void theRealBoundVerifierRejectsIncompleteOrUnknownFormalFacts(String invalid)throws Exception{
        switch(invalid){
            case "missingPrepared"->jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=NULL,prepared_process_definition_id=NULL,prepared_definition_digest=NULL WHERE id=20");
            case "partialPrepared"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=NULL WHERE id=20");
            case "wrongDigest"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=? WHERE id=20","f".repeat(64));
            case "missingRound"->jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id=20");
            case "wrongProject"->jdbc.update("UPDATE dcc_project_application_attributes SET project_code_id=6 WHERE application_id=20");
            case "unsubmitted"->jdbc.update("UPDATE dcc_project_application_attributes SET submitted=0 WHERE application_id=20");
            case "unknownGraph","legacyGraph"->{String definition="legacyGraph".equals(invalid)?exactId:deploy(legacyXml.replace("name=\"培训等待\"","name=\"unregistered graph\""));
                String another=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(definition).businessKey("20")
                        .variables(Map.of("controlledFileId",20L,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("OLD-OBLIGATION")))).start().getId();
                jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,prepared_workflow_contract=NULL,prepared_process_definition_id=NULL,prepared_definition_digest=NULL WHERE id=20",another);}
        }
        if("legacyGraph".equals(invalid))noPreview();
        else assertThrows(RuntimeException.class,()->body(77L));
    }
    @Test void aSupervisorBodyTaskDoesNotGrantAttachmentPreview(){
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getAttachmentPreviewMetadata(77L,20L,777L,audit()));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.readAttachmentPreviewFile(77L,20L,777L,"token","event","trace","id","nonce",audit()));
        verifyNoInteractions((DccControlledFileAttachmentService)ReflectionTestUtils.getField(query,"attachmentService"));
    }
    @Test void theRealCurrentCompilerKeepsBodyAndIndependentAttachmentQualification(){
        login(99L,1L);
        var scope=(DccControlledFileAssignmentScopeService)ReflectionTestUtils.getField(detail,"assignmentScopeService");
        when(scope.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        wire(detail,"permissionSupport",permissions);
        when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);when(access.listReadableProjectIds(99L)).thenReturn(List.of(5L));
        assertTrue(body(99L));assertTrue(detail.isAllowed(99L,file(),false,null));
        var attachmentRows=mock(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper.class);
        var attachment=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO.builder().id(777L).controlledFileId(20L)
                .storageFileId(200L).originalFileName("bound-attachment.pdf").contentType("application/pdf").fileSize(200L).build();
        attachment.setTenantId(1L);when(attachmentRows.selectById(777L)).thenReturn(attachment);
        var attachments=new DccControlledFileAttachmentServiceImpl();wire(attachments,"attachmentMapper",attachmentRows);wire(query,"attachmentService",attachments);
        var infra=(FileMapper)ReflectionTestUtils.getField(query,"fileMapper");
        when(infra.selectById(200L)).thenReturn(FileDO.builder().id(200L).configId(1L).name("bound-attachment.pdf").type("application/pdf").path("actual-bound/attachment.pdf").build());
        var business=(BusinessFileAccessService)ReflectionTestUtils.getField(query,"businessFileAccessService");
        doReturn(Optional.of(new BusinessFileAccessReference("dcc","DCC_CONTROLLED_FILE",20L,"A/1",1L,null)))
                .when(business).assertAllowed(argThat(request->request.fileId().equals(200L)&&request.userId().equals(99L)));
        assertEquals("PDF",query.getAttachmentPreviewMetadata(99L,20L,777L,audit()).getPreviewKind());
        attachment.setTenantId(2L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getAttachmentPreviewMetadata(99L,20L,777L,audit()));
        attachment.setTenantId(1L);attachment.setControlledFileId(21L);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getAttachmentPreviewMetadata(99L,20L,777L,audit()));
        attachment.setControlledFileId(20L);
        doThrow(new IllegalStateException("actual independent attachment business gate refused")).when(business)
                .assertAllowed(argThat(request->request.fileId().equals(200L)));
        assertThrows(RuntimeException.class,()->query.getAttachmentPreviewMetadata(99L,20L,777L,audit()));
    }
    @Test void aPersistedRequesterLabelCannotReplaceTheActualCompilerOrProjectQualification(){
        login(99L,1L);wire(detail,"permissionSupport",permissions);
        var scope=(DccControlledFileAssignmentScopeService)ReflectionTestUtils.getField(detail,"assignmentScopeService");
        when(scope.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(false);assertFalse(body(99L));
        when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);when(access.listReadableProjectIds(99L)).thenReturn(List.of(5L));
        jdbc.update("UPDATE dcc_controlled_file SET requester_id=77 WHERE id=20");login(77L,1L);engine.getTaskService().setAssignee(taskId,"78");
        when(scope.isWithinAssignedFileScope(77L,20L)).thenReturn(true);when(access.hasProjectEditorOrOwner(77L,5L)).thenReturn(true);when(access.listReadableProjectIds(77L)).thenReturn(List.of(5L));
        assertFalse(body(77L));assertFalse(detail.isAllowed(77L,file(),true,null));
    }
    @Test void actualViewerTokenIngressReadsOnlyTheCurrentSupervisorBodyAndStopsAfterTheTaskEnds() throws Exception {
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);var metadata=actualPdfMetadata(storage);
        assertArrayEquals(new byte[]{27,77,20},pdf(metadata).bytes());verify(storage,times(1)).getFileContent(1L,"actual-bound/A1.pdf");
        engine.getTaskService().complete(taskId);clearInvocations(storage);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->pdf(metadata));
        verifyNoInteractions(storage);
    }

    private String officeToken(cn.iocoder.yudao.module.infra.service.file.FileService storage) throws Exception {
        Long actor=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId();
        var properties=new DccOnlyOfficePreviewProperties();properties.setBaseUrl("https://office.local");
        properties.setPublicFileBaseUrl("https://files.local");properties.setJwtSecret("w08-supervisor-real-Office-HMAC-secret");
        var signed=new DccOnlyOfficePreviewTokenService();wire(signed,"properties",properties);
        wire(query,"onlyOfficePreviewProperties",properties,"onlyOfficePreviewTokenService",signed,"fileService",storage);
        var infra=(FileMapper)ReflectionTestUtils.getField(query,"fileMapper");
        when(infra.selectById(100L)).thenReturn(FileDO.builder().id(100L).configId(1L).name("bound-A1.docx")
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document").path("actual-bound/A1.docx").build());
        var actualAudit=new cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService();
        wire(actualAudit,"accessEventMapper",officeEvents,"watermarkTraceMapper",officeTraces,"accessLogMapper",officeLogs);
        var viewer=new cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService();wire(viewer,"hmacSecret","actual-ic27-metadata-Viewer-token-secret");
        var actualProducer=new DccControlledPreviewAccessService();wire(actualProducer,"accessAuditService",actualAudit,"viewerTokenService",viewer);
        wire(query,"previewAccessService",actualProducer);
        wire(query,"accessEventMapper",officeEvents,"watermarkTraceMapper",officeTraces,"accessLogMapper",officeLogs);
        var business=(BusinessFileAccessService)ReflectionTestUtils.getField(query,"businessFileAccessService");
        doAnswer(call->{var request=call.<BusinessFileAccessRequest>getArgument(0);
            assertEquals(actor,request.userId());query.assertBusinessFileAccess(request.userId(),20L,request.operation(),audit());
            if(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser()==null){
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getPreviewMetadata(actor,20L,audit()));
                assertNotNull(issuedMetadata);
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.readPreviewFile(actor,20L,
                        issuedMetadata.getViewerToken(),issuedMetadata.getAccessEventCode(),issuedMetadata.getWatermarkTraceCode(),
                        issuedMetadata.getViewerTokenId(),issuedMetadata.getViewerTokenNonce(),audit()));
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getAttachmentPreviewMetadata(actor,20L,777L,audit()));
                assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.assertBusinessFileAccess(actor,20L,BusinessFileAccessOperation.DOWNLOAD,audit()));
            }
            return Optional.of(new BusinessFileAccessReference("dcc","DCC_CONTROLLED_FILE",20L,"A/1",1L,null));}).when(business).assertAllowed(any());
        when(storage.getFileContent(1L,"actual-bound/A1.docx")).thenReturn(new byte[]{25,77,20});
        var metadata=tx().execute(status->query.getPreviewMetadata(actor,20L,audit()));assertEquals("OFFICE",metadata.getPreviewKind());
        issuedMetadata=metadata;
        assertNotNull(metadata.getOnlyofficeDocumentUrl());String token=metadata.getOnlyofficeDocumentUrl().split("token=",2)[1];
        var issued=signed.verifyControlledFile(token,20L);officeEventId=issued.getAccessEventId();
        var saved=officeEvents.selectById(officeEventId);assertNotNull(saved);assertEquals(bpm,saved.getProcessInstanceId(),"actual producer must persist the issued bound round");
        return token;
    }
    private void assertNoOfficeActor(){
        assertNull(cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser());
        var field=Arrays.stream(detail.getClass().getDeclaredFields()).filter(item->item.getName().equals("verifiedOfficeReadActor")).findFirst();
        if(field.isPresent())assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(detail,"verifiedOfficeReadActor")).get());
    }
    @Test void authenticatedMetadataAndAnonymousRealOfficeCallbackReadTheSameActiveSupervisorBodyWithoutLoginForgery() throws Exception {
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);String token=officeToken(storage);SecurityContextHolder.clearContext();
        assertArrayEquals(new byte[]{25,77,20},query.readOnlyOfficePreviewFile(20L,token,audit()).bytes());assertNoOfficeActor();
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_access_log WHERE controlled_file_id=20 AND user_id=77 AND action_type='OFFICE_READ' AND result='ALLOWED'",Integer.class));
        assertFalse(body(77L));assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->query.getPreviewMetadata(77L,20L,audit()));
        engine.getTaskService().complete(taskId);clearInvocations(storage);
        assertThrows(RuntimeException.class,()->query.readOnlyOfficePreviewFile(20L,token,audit()));verifyNoInteractions(storage);assertNoOfficeActor();
    }
    @ParameterizedTest @ValueSource(strings={"badToken","disabled","wrongFile","wrongRound","wrongTenant","storageFailure","businessFailure","eventFailure","conflictingPrincipal","reboundRound"})
    void signedOfficeSubjectCannotSurviveAnInvalidCallbackAndNeverLeaksItsReadScope(String invalid) throws Exception {
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);String issued=officeToken(storage);SecurityContextHolder.clearContext();
        Long requested=20L;String token=issued;
        switch(invalid){
            case "badToken"->token=issued.substring(0,issued.length()-1)+(issued.endsWith("A")?"B":"A");
            case "disabled"->actors.getUser(77L).setStatus(1);
            case "wrongFile"->requested=21L;
            case "wrongRound"->jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='old-round' WHERE id=20");
            case "reboundRound"->{String later=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(publicId).businessKey("20")
                    .variables(Map.of("controlledFileId",20L,"needTraining",false,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("NEW-ROUND")))).start().getId();
                var nextTask=engine.getTaskService().createTaskQuery().processInstanceId(later).singleResult();engine.getTaskService().setAssignee(nextTask.getId(),"77");
                jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,submitted_time=? WHERE id=20",later,LocalDateTime.now());
                jdbc.update("UPDATE dcc_application_round_link SET bpm_round=? WHERE application_id=20",later);}
            case "wrongTenant"->TenantContextHolder.setTenantId(2L);
            case "conflictingPrincipal"->login(99L,1L);
            case "storageFailure"->when(storage.getFileContent(1L,"actual-bound/A1.docx")).thenThrow(new IllegalStateException("actual callback storage failure"));
            case "businessFailure"->doThrow(new IllegalStateException("actual callback business denied")).when((BusinessFileAccessService)ReflectionTestUtils.getField(query,"businessFileAccessService")).assertAllowed(any());
            case "eventFailure"->jdbc.update("UPDATE dcc_controlled_file_access_event SET controlled_file_id=21 WHERE id=?",officeEventId);
        }
        Long target=requested;String attempted=token;
        assertThrows(RuntimeException.class,()->query.readOnlyOfficePreviewFile(target,attempted,audit()));
        if(!"storageFailure".equals(invalid))verifyNoInteractions(storage);
        if("conflictingPrincipal".equals(invalid)){assertEquals(99L,cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());SecurityContextHolder.clearContext();}
        assertNoOfficeActor();
    }
    @Test void theRealCompilerOfficeCallbackUsesVerifiedReadSubjectWithoutGrantingAttachmentsOrCreatingLogin() throws Exception {
        login(99L,1L);wire(detail,"permissionSupport",permissions);
        var scope=(DccControlledFileAssignmentScopeService)ReflectionTestUtils.getField(detail,"assignmentScopeService");when(scope.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        when(access.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);when(access.listReadableProjectIds(99L)).thenReturn(List.of(5L));
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);String token=officeToken(storage);SecurityContextHolder.clearContext();
        assertArrayEquals(new byte[]{25,77,20},query.readOnlyOfficePreviewFile(20L,token,audit()).bytes());assertNoOfficeActor();
        assertFalse(body(99L));verifyNoInteractions((DccDistributionReceiptService)ReflectionTestUtils.getField(query,"distributionReceiptService"));
    }

    @ParameterizedTest @ValueSource(strings={"sameSecond","equalEvent","earlierThanEvent","missingSubmissionTime"})
    void anOldOfficeEventCannotBecomeProofForAnotherExactRoundRegardlessOfTimestampOrdering(String timeCase) throws Exception {
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);
        String token=officeToken(storage);var oldEvent=officeEvents.selectById(officeEventId);String oldRound=file().getProcessInstanceId();
        var sourceAttributes=attributeRows.find("UPLOAD",20L,1);
        org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId("99");
        String another;
        try{another=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(publicId).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"needTraining",false,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("ANOTHER-EXACT-ROUND")))).start().getId();}
        finally{org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId(null);}
        var currentTask=engine.getTaskService().createTaskQuery().processInstanceId(another).singleResult();
        assertEquals("SUPERVISOR_REVIEW",currentTask.getTaskDefinitionKey());engine.getTaskService().setAssignee(currentTask.getId(),"77");
        Integer second=tx().execute(status->{
            int round=rounds.reserveDraft(5L,"UPLOAD",20L);assertEquals(2,round);
            var submitted=new cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO();
            org.springframework.beans.BeanUtils.copyProperties(sourceAttributes,submitted);
            submitted.setId(null);submitted.setCreateTime(null);submitted.setUpdateTime(null);submitted.setCreator(null);submitted.setUpdater(null);
            submitted.setApplicationRound(round);submitted.setSubmitted(true);
            assertEquals(1,attributeRows.insert(submitted));rounds.bindReservedDraft(5L,"UPLOAD",20L,round,another);return round;
        });
        LocalDateTime submittedAt=switch(timeCase){
            case "sameSecond"->oldEvent.getOccurredAt().withNano(0);
            case "equalEvent"->oldEvent.getOccurredAt();
            case "earlierThanEvent"->oldEvent.getOccurredAt().minusMinutes(1);
            case "missingSubmissionTime"->null;
            default->throw new AssertionError(timeCase);
        };
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,submitted_time=? WHERE id=20",another,submittedAt);
        var current=bound.resolve(file());assertTrue(current.active());assertTrue(current.trainingAfterDocumentControl());
        assertEquals(another,current.processInstanceId());assertEquals(second,current.attributeRound());assertNotEquals(oldRound,current.processInstanceId());
        assertEquals(oldRound,rounds.resolveBoundBpm("UPLOAD",20L,1));assertEquals(another,rounds.resolveBoundBpm("UPLOAD",20L,2));
        assertEquals(oldEvent,officeEvents.selectById(officeEventId),"issuance evidence must not be altered to fit the current round");
        SecurityContextHolder.clearContext();clearInvocations(storage);
        assertThrows(RuntimeException.class,()->query.readOnlyOfficePreviewFile(20L,token,audit()),
                "old signed event must not authorize a different exact bound BPM round despite favorable timestamps");
        verifyNoInteractions(storage);assertNoOfficeActor();
    }
    @Test void metadataPassesTheBoundRoundReturnedByTheRealAuthorizationToTheProducer(){
        var captured=(DccControlledPreviewAccessService)ReflectionTestUtils.getField(query,"previewAccessService");
        query.getPreviewMetadata(77L,20L,audit());
        verify(captured).prepareAccess(argThat(request->bpm.equals(request.processInstanceId())&&request.fileId().equals(20L)));
    }
    @ParameterizedTest @ValueSource(strings={"legacyNull","wrongSavedRound"})
    void eventWithoutTheIssuedExactRoundCannotBeRepairedDuringAnonymousCallback(String invalid)throws Exception{
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);String token=officeToken(storage);
        jdbc.update("UPDATE dcc_controlled_file_access_event SET process_instance_id=? WHERE id=?","legacyNull".equals(invalid)?null:"other-exact-round",officeEventId);
        var before=officeEvents.selectById(officeEventId);SecurityContextHolder.clearContext();clearInvocations(storage);
        assertThrows(RuntimeException.class,()->query.readOnlyOfficePreviewFile(20L,token,audit()));
        verifyNoInteractions(storage);assertEquals(before,officeEvents.selectById(officeEventId));assertNoOfficeActor();
    }
    @ParameterizedTest @ValueSource(strings={"missingSubmissionTime","submissionAfterEvent"})
    void sameIssuedRoundReadsWithExactEvidenceInsteadOfAnyTimestampIdentityGuess(String timeCase)throws Exception{
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);String token=officeToken(storage);
        var event=officeEvents.selectById(officeEventId);
        jdbc.update("UPDATE dcc_controlled_file SET submitted_time=? WHERE id=20","missingSubmissionTime".equals(timeCase)?null:event.getOccurredAt().plusMinutes(1));
        SecurityContextHolder.clearContext();assertArrayEquals(new byte[]{25,77,20},query.readOnlyOfficePreviewFile(20L,token,audit()).bytes());
        assertEquals(event,officeEvents.selectById(officeEventId));assertNoOfficeActor();
    }

    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO actualPdfMetadata(
            cn.iocoder.yudao.module.infra.service.file.FileService storage) throws Exception {
        return actualBodyMetadata(storage,"PDF");
    }
    private cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO actualBodyMetadata(
            cn.iocoder.yudao.module.infra.service.file.FileService storage,String expectedKind) throws Exception {
        var auditService=new cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService();
        wire(auditService,"accessEventMapper",officeEvents,"watermarkTraceMapper",officeTraces,"accessLogMapper",officeLogs);
        var viewer=new cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService();wire(viewer,"hmacSecret","actual-ic27-PDF-Viewer-token-secret");
        var producer=new DccControlledPreviewAccessService();wire(producer,"accessAuditService",auditService,"viewerTokenService",viewer);
        wire(query,"previewAccessService",producer,"viewerTokenService",viewer,"accessEventMapper",officeEvents,"watermarkTraceMapper",officeTraces,"accessLogMapper",officeLogs,"fileService",storage);
        when(storage.getFileContent(1L,"actual-bound/A1.pdf")).thenReturn(new byte[]{27,77,20});
        var metadata=tx().execute(status->query.getPreviewMetadata(77L,20L,audit()));assertEquals(expectedKind,metadata.getPreviewKind());
        var saved=officeEvents.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO>()
                .eq("access_event_code",metadata.getAccessEventCode()));assertNotNull(saved);assertEquals(bpm,saved.getProcessInstanceId());officeEventId=saved.getId();
        return metadata;
    }
    private DccControlledFileBinary pdf(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO metadata) {
        return query.readPreviewFile(77L,20L,metadata.getViewerToken(),metadata.getAccessEventCode(),metadata.getWatermarkTraceCode(),metadata.getViewerTokenId(),metadata.getViewerTokenNonce(),audit());
    }
    @Test void actualPdfMetadataAndCurrentHumanPrincipalReadTheExactIssuedRound() throws Exception {
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);var metadata=actualPdfMetadata(storage);
        assertArrayEquals(new byte[]{27,77,20},pdf(metadata).bytes());
        SecurityContextHolder.clearContext();clearInvocations(storage);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->pdf(metadata));verifyNoInteractions(storage);
    }
    @Test void anAuthenticatedOldPdfTokenCannotReadAnotherCompleteBoundRoundWithTheSameActorAndSameSecond() throws Exception {
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);var metadata=actualPdfMetadata(storage);
        var oldEvent=officeEvents.selectById(officeEventId);
        String another=bindAnotherPdfOrImageRound(oldEvent.getOccurredAt().withNano(0));
        var current=bound.resolve(file());assertEquals(another,current.processInstanceId());assertEquals(2,current.attributeRound());assertTrue(current.active());
        assertEquals(77L,cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());clearInvocations(storage);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->pdf(metadata));verifyNoInteractions(storage);
        assertEquals(oldEvent,officeEvents.selectById(officeEventId));
    }
    private String bindAnotherPdfOrImageRound(LocalDateTime submittedAt) {
        org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId("99");String another;
        try{another=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(publicId).businessKey("20")
                .variables(Map.of("controlledFileId",20L,"needTraining",false,"PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("PDF-ANOTHER-ROUND")))).start().getId();}
        finally{org.flowable.common.engine.impl.identity.Authentication.setAuthenticatedUserId(null);}
        var task=engine.getTaskService().createTaskQuery().processInstanceId(another).singleResult();engine.getTaskService().setAssignee(task.getId(),"77");
        tx().executeWithoutResult(status->{int round=rounds.reserveDraft(5L,"UPLOAD",20L);assertEquals(2,round);
            var previous=attributeRows.find("UPLOAD",20L,1);var submitted=new cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO();
            org.springframework.beans.BeanUtils.copyProperties(previous,submitted);submitted.setId(null);submitted.setCreateTime(null);submitted.setUpdateTime(null);
            submitted.setCreator(null);submitted.setUpdater(null);submitted.setApplicationRound(round);assertEquals(1,attributeRows.insert(submitted));rounds.bindReservedDraft(5L,"UPLOAD",20L,round,another);});
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=?,submitted_time=? WHERE id=20",another,submittedAt);
        return another;
    }
    @ParameterizedTest @ValueSource(strings={"legacyNull","wrongRound","wrongTenant"})
    void currentPrincipalCannotUsePdfEventWithoutItsExactSavedSourceTuple(String invalid)throws Exception{
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);var metadata=actualPdfMetadata(storage);
        if("wrongTenant".equals(invalid))jdbc.update("UPDATE dcc_controlled_file_access_event SET tenant_id=2 WHERE id=?",officeEventId);
        else jdbc.update("UPDATE dcc_controlled_file_access_event SET process_instance_id=? WHERE id=?","legacyNull".equals(invalid)?null:"other-round",officeEventId);
        var before=officeEvents.selectById(officeEventId);clearInvocations(storage);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->pdf(metadata));verifyNoInteractions(storage);
        assertEquals(before,officeEvents.selectById(officeEventId));
    }
    @ParameterizedTest @ValueSource(strings={"current","legacyNull","wrongRound","anotherBound"})
    void actualImageMetadataAndBytesUseTheSamePersistentRoundProofWithoutAnyNewPurpose(String scenario)throws Exception{
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='source.png' WHERE id=20");
        var infra=(FileMapper)ReflectionTestUtils.getField(query,"fileMapper");
        when(infra.selectById(100L)).thenReturn(FileDO.builder().id(100L).configId(1L).name("source.png").type("image/png").path("actual-bound/A1.png").build());
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);when(storage.getFileContent(1L,"actual-bound/A1.png")).thenReturn(new byte[]{27,77,21});
        var metadata=actualBodyMetadata(storage,"IMAGE");var event=officeEvents.selectById(officeEventId);
        assertEquals("CONTROLLED_PREVIEW",event.getPurpose());assertEquals(bpm,event.getProcessInstanceId());
        if("current".equals(scenario)){assertArrayEquals(new byte[]{27,77,21},pdf(metadata).bytes());return;}
        if("anotherBound".equals(scenario)){String another=bindAnotherPdfOrImageRound(event.getOccurredAt().withNano(0));
            assertEquals(another,bound.resolve(file()).processInstanceId());assertEquals(2,bound.resolve(file()).attributeRound());}
        else jdbc.update("UPDATE dcc_controlled_file_access_event SET process_instance_id=? WHERE id=?","legacyNull".equals(scenario)?null:"other-image-round",officeEventId);
        var before=officeEvents.selectById(officeEventId);clearInvocations(storage);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->pdf(metadata));verifyNoInteractions(storage);
        assertEquals(before,officeEvents.selectById(officeEventId));
    }
}
