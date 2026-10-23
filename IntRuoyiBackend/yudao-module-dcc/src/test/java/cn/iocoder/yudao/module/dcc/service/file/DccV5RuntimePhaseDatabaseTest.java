package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApiImpl;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual candidate whole graph + actual signatures/BPM, phase boundary and attachment transaction.
 * Supervisor directory is external isolated account port; lifecycle/artifact closure is not certified. */
class DccV5RuntimePhaseDatabaseTest extends DccRejectedRevisionRetryDatabaseTest {
    String bpm;
    BpmTaskService taskPort;
    DccOfflineTrainingRecordService training;
    DccApprovalFileOwnerSelectionService owners;
    final List<org.flowable.common.engine.api.delegate.event.FlowableEventListener> endListeners=new ArrayList<>();
    @AfterEach void removeTaskOwnedEndListeners(){for(var listener:endListeners)engine.getRuntimeService().removeEventListener(listener);endListeners.clear();}
    @BeforeEach void phaseDependencies(){
        jdbc.execute("CREATE TABLE IF NOT EXISTS w05_v5_finalization_boundary(file_id BIGINT,bpm VARCHAR(128),actor BIGINT)");jdbc.update("DELETE FROM w05_v5_finalization_boundary");
        // Private table from the actual attachment migration; shared H2 fixture is management-owned.
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file_attachment(id BIGINT AUTO_INCREMENT PRIMARY KEY,controlled_file_id BIGINT NOT NULL,storage_file_id BIGINT NOT NULL,original_file_name VARCHAR(512) NOT NULL,content_type VARCHAR(128) NOT NULL,file_size BIGINT NOT NULL,file_sha256 VARCHAR(64),sort_no INT NOT NULL DEFAULT 0,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(128),uploaded_nickname_snapshot VARCHAR(128),uploaded_at TIMESTAMP,tenant_id BIGINT NOT NULL,creator VARCHAR(64),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updater VARCHAR(64),update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted BIT DEFAULT 0)");
        jdbc.update("DELETE FROM dcc_controlled_file_attachment");
    }
    void startNative(String kind,boolean needsTraining)throws Exception{
        initial();wireActualSignedRejection();
        var context=(org.springframework.context.ConfigurableApplicationContext)cn.hutool.extra.spring.SpringUtil.getApplicationContext();
        var factory=context.getBeanFactory();if(factory.containsSingleton("w05ActualManagementService"))((org.springframework.beans.factory.support.DefaultListableBeanFactory)factory).destroySingleton("w05ActualManagementService");
        factory.registerSingleton("w05ActualManagementService",engine.getManagementService());
        var read=new BpmProcessInstanceServiceImpl();wire(read,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService());
        var definitions=mock(BpmProcessDefinitionService.class);
        when(definitions.getProcessDefinition(anyString())).thenAnswer(c->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(c.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(c->engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
        var leaf=new DccBoundWorkflowContractResolver();wire(leaf,"definitions",definitions,"processes",read,"rounds",rounds);
        if(Arrays.stream(leaf.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("attributes")))wire(leaf,"attributes",attributeRows);wire(executionContracts,"boundContracts",leaf);
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String id;
        if(kind.equals("v4"))id=legacyDefinitionIds.get(DccControlledFileProcessDefinitionKeys.UPLOAD);
        else{var deploy=engine.getRepositoryService().createDeployment().tenantId("1").addString("actual-v5.bpmn20.xml",Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml"))).deploy();id=engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deploy.getId()).singleResult().getId();}
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(id));
        var info=new cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO();when(definitions.getProcessDefinitionInfo(id)).thenReturn(info);when(definitions.canUserStartProcessDefinition(info,99L)).thenReturn(true);
        var starter=spy(read);wire(starter,"processDefinitionService",definitions,"adminUserApi",mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class));
        doReturn(new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO().setActivityNodes(new ArrayList<>())).when(starter).getApprovalDetail(eq(99L),any());
        var api=new BpmProcessInstanceApiImpl();wire(api,"processInstanceService",starter);
        String contract=kind.equals("v4")?DccApplicationExecutionContractService.LEGACY:DccApplicationExecutionContractService.PUBLIC;
        var selections=Map.of("SUPERVISOR_REVIEW",List.of(99L),"MATRIX_REVIEW",List.of(99L),"MATRIX_APPROVAL",List.of(99L),"DOC_CONTROL_REVIEW",List.of(99L));
        bpm=api.createProcessInstance(99L,new BpmProcessInstanceCreateReqDTO().setProcessDefinitionId(id).setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).setBusinessKey("20").setName("真实v5阶段验证")
                .setStartUserSelectAssignees(selections).setVariables(new HashMap<>(Map.of("controlledFileId",20L,"needTraining",needsTraining,
                        "PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("file-20:MATRIX_REVIEW:51")),cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,selections,
                        cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES,new HashMap<>(selections)))));
        tx().executeWithoutResult(s->snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,rounds.requireDraft("UPLOAD",20L),bpm,ce));
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=?,process_instance_id=?,process_definition_key=?,need_training=?,effective_mode='PRESET_DATE',effective_date='2099-12-20',file_owner_user_id=88,file_owner_signature_id=999,file_owner_approval_task_id='owner-task',file_owner_process_instance_id=?,file_owner_selected_time=CURRENT_TIMESTAMP,approved_time=CURRENT_TIMESTAMP WHERE id=20",contract,id,digest,bpm,DccControlledFileProcessDefinitionKeys.UPLOAD,needsTraining,bpm);
        for(int index=0;index<3;index++)routes.insert(DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).stageNo(index+1).stageOrder(index+1)
                .stageCode(List.of("MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW").get(index)).stageName("实际阶段").routeVersionNo(1).candidateSourceType(index==0?"DEPT":"USER")
                .candidateSourceId(index==0?51L:99L).candidateSourceIds(index==0?"51":"99").resolvedUserIds("99").approveMethod(index==0?"ALL":"ANY").approveRatio(index==0?100:null).requireAllApprovals(index==0).build());
        wire(workflow,"bpmProcessInstanceService",read);taskPort=(BpmTaskService)ReflectionTestUtils.getField(((org.springframework.aop.framework.Advised)workflow).getTargetSource().getTarget(),"bpmTaskService");
        training=new DccOfflineTrainingRecordService();
        var users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);when(users.getUser(anyLong())).thenAnswer(c->new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(c.getArgument(0)).setTenantId(1L).setStatus(0).setUsername("isolated-user").setNickname("明确职责账号"));
        var notification=mock(cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi.class);when(notification.sendSingleMessageIdempotentlyToAdmin(any())).thenReturn(1L);
        var permission=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permission.hasAnyPermissions(any(),any(String[].class))).thenReturn(true);
        wire(training,"files",files,"masters",masters,"projects",projects,"categories",permissions,"fileScope",scope,"versions",policy,"users",users,"permissions",permission,"processes",read,"definitions",definitions,"runtime",engine.getRuntimeService(),"messages",notification,"boundContracts",leaf);
        wire(workflow,"offlineTraining",training);
        wire(workflow,"runtimeService",engine.getRuntimeService());
        var signatureRows=sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper.class);
        owners=new DccApprovalFileOwnerSelectionService();wire(owners,"users",users,"files",files,"signatures",signatureRows);
        var management=new DccElectronicSignatureManagementServiceImpl();
        var props=new DccSignatureEvidenceProperties();props.setHmacSecret("r04-isolated-domain-signature-hmac-key");props.setKeyVersion("r04-private-v1");
        var images=mock(DccElectronicSignatureImageService.class);when(images.verifySignatureSnapshot(any(DccControlledFileSignatureDO.class))).thenAnswer(c->{var row=c.<DccControlledFileSignatureDO>getArgument(0);return DccElectronicSignatureImageSnapshot.builder()
                .imageId(row.getSignatureImageId()).versionNo(row.getSignatureImageVersionNo()).fileId(row.getSignatureImageFileId()).fileUrl(row.getSignatureImageFileUrl())
                .sha256(row.getSignatureImageSha256()).contentType(row.getSignatureImageContentType()).fileSize(row.getSignatureImageFileSize()).imageStatus(row.getSignatureImageStatusSnapshot()).verifiedStatus(row.getSignatureImageVerifiedStatus()).build();});
        wire(management,"signatureMapper",signatureRows,"controlledFileMapper",files,"signatureEvidenceProperties",props,"fileService",storage,"signatureImageService",images);
        var lifecycle=new DccControlledFileLifecycleService();wire(lifecycle,"approvalSignatureMapper",signatureRows,"signatureManagementService",management,"bpmTaskService",taskPort);
        wire(training,"lifecycle",lifecycle);
        var attachment=new DccControlledFileAttachmentServiceImpl();DccWorkflowSelectedIterationDatabaseTest.defaults(attachment);
        wire(attachment,"controlledFileMapper",files,"attachmentMapper",sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper.class),"fileMapper",infraFiles,"users",users,"offlineTraining",training);
        wire(workflow,"attachmentService",attachment);
    }
    void signedAdvance(String stage)throws Exception{
        var task=engine.getTaskService().createTaskQuery().processInstanceId(bpm).includeTaskLocalVariables().singleResult();assertEquals(stage,task.getTaskDefinitionKey());
        Long actor=stage.equals("DOC_CONTROL_REVIEW")?77L:99L;
        if(actor==77L){engine.getTaskService().setAssignee(task.getId(),"77");jdbc.update("UPDATE dcc_controlled_file_route_snapshot SET resolved_user_ids='77' WHERE controlled_file_id=20 AND stage_code='DOC_CONTROL_REVIEW'");
            var login=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(77L).setTenantId(1L).setUserType(2);org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null));
            Object target=((org.springframework.aop.framework.Advised)workflow).getTargetSource().getTarget();Object signer=ReflectionTestUtils.getField(target,"signatureVerificationService");
            var admin=(cn.iocoder.yudao.module.system.service.user.AdminUserService)ReflectionTestUtils.getField(signer,"adminUserService");var reviewer=new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO();reviewer.setId(77L);reviewer.setUsername("document-reviewer");reviewer.setNickname("实际文控签名人");reviewer.setDeptId(51L);reviewer.setPostIds(Set.of(10L));when(admin.getUser(77L)).thenReturn(reviewer);
            var permission=(cn.iocoder.yudao.module.system.service.permission.PermissionService)ReflectionTestUtils.getField(signer,"permissionService");when(permission.getUserRoleIdListByUserId(77L)).thenReturn(Set.of(10L));
            var image=(DccElectronicSignatureImageService)ReflectionTestUtils.getField(signer,"signatureImageService");var actualImage=image.requireActiveSnapshot(99L);when(image.requireActiveSnapshot(77L)).thenReturn(actualImage);}
        try{
        tx().executeWithoutResult(s->{try{Object target=((org.springframework.aop.framework.Advised)workflow).getTargetSource().getTarget();
            var signer=(DccSignatureVerificationService)ReflectionTestUtils.getField(target,"signatureVerificationService");var file=files.selectById(20L);
            var selection=stage.equals("MATRIX_APPROVAL")?owners.prepare(file,cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum.MATRIX_APPROVAL,88L):null;
            String reason=selection==null?"真实阶段签名":owners.signedReason("真实阶段签名",selection);
            var signed=signer.verifyPasswordAndCreateWorkflowSignature(actor,20L,task.getId(),bpm,stage,"APPROVE","isolated-credential",reason);
            if(selection!=null)owners.bind(file,task.getId(),99L,signed.getSignatureId(),selection,reason);
            var authorization=(DccWorkflowSignoffAssignmentService)ReflectionTestUtils.getField(target,"signoffAssignmentService");authorization.authorizeSignedAction(task.getId(),actor,"APPROVE",signed);
            taskPort.approveTask(actor,new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO().setId(task.getId()).setReason("真实阶段签名").setNextAssignees(new HashMap<>(Map.of("MATRIX_REVIEW",List.of(99L),"MATRIX_APPROVAL",List.of(99L),"DOC_CONTROL_REVIEW",List.of(99L)))));
        }catch(Exception e){throw new IllegalStateException(e);}});
        }finally{authenticateFixtureActor();}
    }
    void reachApproval()throws Exception{if(engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult().getTaskDefinitionKey().equals("SUPERVISOR_REVIEW"))signedAdvance("SUPERVISOR_REVIEW");signedAdvance("MATRIX_REVIEW");}
    Object sync(String completed)throws Exception{
        Object target=((org.springframework.aop.framework.Advised)workflow).getTargetSource().getTarget();
        return ReflectionTestUtils.invokeMethod(target,"syncStatusAfterApprove",files.selectById(20L),cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum.valueOf(completed),Set.of(),"actual-completed-task",completed);
    }
    @Test void v5ApprovalLeadsToDocumentReviewAndOnlyActualDocumentSignatureStartsTraining()throws Exception{
        startNative("v5",true);reachApproval();jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_APPROVAL' WHERE id=20");signedAdvance("MATRIX_APPROVAL");
        assertEquals("PENDING_DOC_CONTROL_REVIEW",tx().execute(s->{try{return sync("MATRIX_APPROVAL");}catch(Exception e){throw new IllegalStateException(e);}}));
        signedAdvance("DOC_CONTROL_REVIEW");assertEquals(1,engine.getRuntimeService().createExecutionQuery().processInstanceId(bpm).activityId("TRAINING").count());
        assertEquals("PENDING_APPLICANT_TRAINING_RECORD",tx().execute(s->{try{return sync("DOC_CONTROL_REVIEW");}catch(Exception e){throw new IllegalStateException(e);}}));
        assertEquals(4,count("dcc_controlled_file_signature"));assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM system_electronic_signature",Integer.class));
    }
    @Test void supervisorEnumStatusAndSignedWorkflowSubjectAreUniqueAndDoNotChangeLegacyOrders(){
        assertTrue(Arrays.stream(cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum.values()).anyMatch(stage->"SUPERVISOR_REVIEW".equals(stage.getCode())));
        assertTrue(Arrays.stream(cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum.values()).anyMatch(status->"PENDING_SUPERVISOR_REVIEW".equals(status.getStatus())));
        assertEquals(Integer.valueOf(0),ReflectionTestUtils.<Integer>invokeMethod(new DccControlledFileSignatureSubjectAdapter(),"nodeOrder","SUPERVISOR_REVIEW",true));
        assertEquals(Integer.valueOf(10),ReflectionTestUtils.<Integer>invokeMethod(new DccControlledFileSignatureSubjectAdapter(),"nodeOrder","MATRIX_REVIEW",true));
    }
    @Test void actualSupervisorTaskUsesItsUniqueFrozenActorAndVerifiedDefinition()throws Exception{
        startNative("v5",false);var task=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult();assertEquals("SUPERVISOR_REVIEW",task.getTaskDefinitionKey());
        routes.insert(DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).stageNo(0).stageOrder(0).stageCode("SUPERVISOR_REVIEW").stageName("主管")
                .candidateSourceType("USER").candidateSourceId(99L).candidateSourceIds("99").resolvedUserIds("99").approveMethod("ANY").requireAllApprovals(false).routeVersionNo(1).build());
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_SUPERVISOR_REVIEW' WHERE id=20");
        Object target=((org.springframework.aop.framework.Advised)workflow).getTargetSource().getTarget();
        assertNotNull(tx().execute(s->ReflectionTestUtils.invokeMethod(target,"validateTaskAction",99L,20L,task.getId(),DccControlledFileProcessDefinitionKeys.UPLOAD,"APPROVE")));
        var wrong=mock(org.flowable.task.api.Task.class);when(wrong.getId()).thenReturn(task.getId());when(wrong.getTenantId()).thenReturn("1");when(wrong.getProcessInstanceId()).thenReturn(bpm);
        when(wrong.getProcessDefinitionId()).thenReturn(legacyDefinitionIds.get(DccControlledFileProcessDefinitionKeys.UPLOAD));when(wrong.getTaskDefinitionKey()).thenReturn("SUPERVISOR_REVIEW");when(wrong.getAssignee()).thenReturn("99");
        var wrongPort=mock(BpmTaskService.class);when(wrongPort.validateTask(99L,task.getId())).thenReturn(wrong);wire(workflow,"bpmTaskService",wrongPort);
        assertThrows(RuntimeException.class,()->tx().execute(s->ReflectionTestUtils.invokeMethod(target,"validateTaskAction",99L,20L,task.getId(),DccControlledFileProcessDefinitionKeys.UPLOAD,"APPROVE")));
        assertEquals(0,count("dcc_controlled_file_signature"));assertEquals(1,engine.getTaskService().createTaskQuery().processInstanceId(bpm).count());
    }
    @Test void realSupervisorRejectionEndsOldRoundAndPreservesItsSignedReason(){
        assertDoesNotThrow(()->{
            startNative("v5",false);var task=engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult();
            routes.insert(DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).stageNo(0).stageOrder(0).stageCode("SUPERVISOR_REVIEW").stageName("主管")
                    .candidateSourceType("USER").candidateSourceId(99L).candidateSourceIds("99").resolvedUserIds("99").approveMethod("ANY").requireAllApprovals(false).routeVersionNo(1).build());
            jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_SUPERVISOR_REVIEW' WHERE id=20");
            var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRejectTaskReqVO();request.setTaskId(task.getId());request.setPassword("isolated-credential");request.setReason("主管真实拒绝原因");
            var response=workflow.rejectTask(99L,20L,request);assertEquals("REJECTED",response.getTaskActionResult());
            assertEquals("主管真实拒绝原因",files.selectById(20L).getRejectReason());assertEquals(0,engine.getTaskService().createTaskQuery().processInstanceId(bpm).count());
            assertNotNull(engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(bpm).finished().singleResult());
            var signature=sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper.class).selectById(response.getSignatureId());
            assertEquals("SUPERVISOR_REVIEW_REJECT",signature.getMeaningCode());assertEquals("VALID",signature.getEvidenceStatus());assertEquals(bpm,signature.getProcessInstanceId());
            assertEquals(1,count("dcc_controlled_file_signature"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_electronic_signature",Integer.class));
        });
    }
    void reachTraining(String kind)throws Exception{startNative(kind,true);reachApproval();signedAdvance("MATRIX_APPROVAL");if(!kind.equals("v4"))signedAdvance("DOC_CONTROL_REVIEW");jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_TRAINING_RECORD' WHERE id=20");}
    cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO uploadRequest()throws Exception{
        Long stored=storage.createFileAndReturnId(new byte[]{8,9,10},"真实培训记录.pdf","v5-training/record.pdf","application/pdf");
        var tickets=mock(DccUploadTicketService.class);when(tickets.resolveTrainingForBinding(any())).thenReturn(new cn.iocoder.yudao.module.dcc.service.upload.DccTrainingUploadTicketBoundFile(new DccUploadTicketBoundFile("real-training-ticket",stored,"真实培训记录.pdf","application/pdf",3L),cn.hutool.crypto.digest.DigestUtil.sha256Hex(new byte[]{8,9,10})));wire(workflow,"uploadTicketService",tickets);
        var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileTrainingRecordReqVO();request.setTrainingRecordUploadTicket("real-training-ticket");request.setSessionId("dcc-training:20:"+bpm.length()+":"+bpm+":session");return request;
    }
    @Test void v5TrainingBindingEndsWithoutAnotherDocumentTaskAndCallsFinalizationInSameTransaction()throws Exception{
        reachTraining("v5");var req=uploadRequest();installOfficialEndListener(new Boundary(false));workflow.uploadTrainingRecord(99L,20L,req);
        assertEquals("READY_TO_PUBLISH",files.selectById(20L).getStatus());assertEquals(1,count("dcc_controlled_file_attachment"));assertEquals(1,count("w05_v5_finalization_boundary"));
        assertEquals(77L,jdbc.queryForObject("SELECT actor FROM w05_v5_finalization_boundary",Long.class));assertEquals(99L,jdbc.queryForObject("SELECT uploaded_by FROM dcc_controlled_file_attachment",Long.class));assertEquals(0,engine.getTaskService().createTaskQuery().processInstanceId(bpm).count());
        assertNotNull(engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(bpm).finished().singleResult());assertNull(files.selectById(20L).getControlledTime());
    }
    @Test void lateFinalizationRefusalRollsBackAttachmentFileAndActualReceiveExecution()throws Exception{
        reachTraining("v5");var req=uploadRequest();var before=files.selectById(20L);installOfficialEndListener(new Boundary(true));
        assertThrows(RuntimeException.class,()->workflow.uploadTrainingRecord(99L,20L,req));assertEquals(before,files.selectById(20L));assertEquals(0,count("dcc_controlled_file_attachment"));assertEquals(0,count("w05_v5_finalization_boundary"));
        assertEquals(1,engine.getRuntimeService().createExecutionQuery().processInstanceId(bpm).activityId("TRAINING").count());
    }
    @Test void sameKeyExactV4StillReturnsToDocumentReviewAfterTraining()throws Exception{
        reachTraining("v4");wire(workflow,"finalizationService",new Boundary(true));workflow.uploadTrainingRecord(99L,20L,uploadRequest());
        assertEquals("PENDING_DOC_CONTROL_REVIEW",files.selectById(20L).getStatus());assertEquals("DOC_CONTROL_REVIEW",engine.getTaskService().createTaskQuery().processInstanceId(bpm).singleResult().getTaskDefinitionKey());assertEquals(0,count("w05_v5_finalization_boundary"));
    }
    class Boundary implements DccControlledFileFinalizationService{
        final boolean deny;Boundary(boolean deny){this.deny=deny;}
        public void handleProcessInstanceStatusChanged(cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent e){assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());assertEquals(bpm,e.getId());assertEquals("20",e.getBusinessKey());
            assertEquals(77L,e.getActorUserId());assertEquals(99L,cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId());
            jdbc.update("INSERT INTO w05_v5_finalization_boundary VALUES(?,?,?)",20L,e.getId(),e.getActorUserId());if(deny)throw new IllegalStateException("real finalization boundary refuses absent signed artifact closure");}
        public void retryStamp(Long actor,Long id){throw new UnsupportedOperationException();}public void precheckPublishControlledFile(Long actor,Long id){throw new UnsupportedOperationException();}public void applyApprovedPublishControlledFile(Long actor,Long id,String key){throw new UnsupportedOperationException();}public void releaseManualDistribution(Long actor,Long id){throw new UnsupportedOperationException();}public void activateWithoutApproval(Long id,boolean skip){throw new UnsupportedOperationException();}
        public DccControlledFileBinary readPreviewFile(Long actor,Long id){throw new UnsupportedOperationException();}public cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary readDownloadFile(Long actor,Long id,Boolean confirm,String key,DccRequestAuditContext context){throw new UnsupportedOperationException();}
    }
    void installOfficialEndListener(DccControlledFileFinalizationService boundary){
        var definitions=mock(BpmProcessDefinitionService.class);
        var process=new BpmProcessInstanceServiceImpl();wire(process,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService(),
                "processDefinitionService",definitions,"messageService",mock(cn.iocoder.yudao.module.bpm.service.message.BpmMessageService.class));
        var dcc=new cn.iocoder.yudao.module.dcc.service.file.listener.DccControlledFileStatusListener();wire(dcc,"finalizationService",boundary);
        var publisher=new cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher(event->{
            if(event instanceof cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent actual)dcc.onApplicationEvent(actual);else throw new AssertionError("unexpected event");});
        wire(process,"processInstanceEventPublisher",publisher);
        var listener=new cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener();wire(listener,"processInstanceService",process);
        engine.getRuntimeService().addEventListener(listener,org.flowable.common.engine.api.delegate.event.FlowableEngineEventType.PROCESS_COMPLETED);
        endListeners.add(listener);
    }
}
