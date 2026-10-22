package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.service.file.listener.DccControlledFileStatusListener;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.nio.file.*;
import java.util.regex.*;
import static cn.iocoder.yudao.module.dcc.service.file.DccG02ControlledSignatureTimeTransactionTest.Fixture.wire;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real official Flowable End publisher/listener, domain signatures, Finalization and lifecycle transaction.
 * The public G01 confirmation/authentication command is outside this consumption composition.
 * Platform/audit/directory/storage are explicit ports; engine completion and HMAC are never mocked. */
public class DccG02DelayedFinalizationTransactionTest {
    DccG02ControlledSignatureTimeTransactionTest.Fixture f;
    DccControlledFileFinalizationServiceImpl finalizer;
    GenericApplicationContext events;
    final AtomicReference<BpmProcessInstanceStatusEvent> actualEnd=new AtomicReference<>();
    BpmProcessInstanceServiceImpl processService;
    AdminUserApi accounts;
    @BeforeEach void setup() throws Exception {
        f=new DccG02ControlledSignatureTimeTransactionTest.Fixture();
        installFinalization();
    }
    void installFinalization() {
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(2), new org.springframework.mock.web.MockHttpServletRequest());
        events=new GenericApplicationContext();events.refresh();
        var process=new BpmProcessInstanceServiceImpl();processService=process;
        wire(process,"processDefinitionService",f.definitionReads,"messageService",mock(BpmMessageService.class),
                "processInstanceEventPublisher",new BpmProcessInstanceEventPublisher(events),"runtimeService",f.engine.getRuntimeService(),"historyService",f.engine.getHistoryService());
        var official=new BpmProcessInstanceEventListener();wire(official,"processInstanceService",process);
        f.engine.getRuntimeService().addEventListener(official);
        finalizer=new DccControlledFileFinalizationServiceImpl();
        wire(finalizer,"controlledFileMapper",f.files,"controlledFileMasterMapper",f.masters,"transactionTemplate",f.tx,"platformAdapter",f.platform,
                "routeSnapshotMapper",f.routes,"approvalSignatureMapper",f.signatures,"taskAssigneeSnapshotMapper",f.assignments,
                "signatureManagementService",f.verifier,"signatureBindingService",f.binding,"lifecycleService",f.lifecycle,"bpmTaskService",f.taskReads,
                "boundContracts",f.bound,"processes",process,"runtime",f.engine.getRuntimeService(),"history",f.engine.getHistoryService(),
                "fileMapper",f.physicalRows,"fileService",f.storage,"pdfStampService",new DccPdfStampService());
        wire(finalizer,"trainingAttachments",f.mapperSessions.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper.class));
        if(org.springframework.util.ReflectionUtils.findField(finalizer.getClass(),"confirmationEvidence")!=null)wire(finalizer,"confirmationEvidence",f.confirmation);
        var users=mock(AdminUserApi.class);accounts=users;var requester=new AdminUserRespDTO();requester.setId(99L);requester.setTenantId(1L);requester.setStatus(0);requester.setUsername("g02-compiler");requester.setNickname("G02 Compiler");
        when(users.getUser(99L)).thenReturn(requester);wire(finalizer,"adminUserApi",users);
        var categories=mock(DccFileCategoryMapper.class);when(categories.selectById(2L)).thenReturn(DccFileCategoryDO.builder().id(2L).trainingRequired(false).build());wire(finalizer,"categoryMapper",categories);
        var permissions=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);
        when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(permissions.hasAnyPermissions(99L,"dcc:controlled-file:approve")).thenReturn(true);
        var categoryPermissions=mock(DccControlledFileCategoryPermissionSupport.class);
        when(categoryPermissions.hasCategoryPermission(2L,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.APPROVE)).thenReturn(true);
        wire(finalizer,"permissionApi",permissions,"permissionSupport",categoryPermissions);
        when(f.storage.createFile(any(byte[].class),anyString(),anyString(),anyString())).thenAnswer(c->{
            long id=3000L+f.physicalRows.selectList().size();String path="g02/derived-"+id+".pdf";f.store(id,path,c.getArgument(0));return "g02-task://"+path;});
        var listener=new DccControlledFileStatusListener();wire(listener,"finalizationService",finalizer);
        events.addApplicationListener((org.springframework.context.ApplicationListener<BpmProcessInstanceStatusEvent>)actualEnd::set);
        events.addApplicationListener(listener);
    }
    @AfterEach void close(){try{if(events!=null)events.close();if(f!=null)f.close();}finally{SecurityContextHolder.clearContext();}}

    @Test void requesterConfirmationEndControlsWithRealSAndFutureEKeepsOldVersion() {
        f.tx.executeWithoutResult(s->{f.saveSignedProjection();f.approveEngineTask();});
        assertEquals(99L,actualEnd.get().getActorUserId());
        assertEquals(f.signedAt,f.files.selectById(20L).getControlledTime());
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",f.files.selectById(20L).getStatus());
        assertEquals(10L,f.masters.selectById(3L).getCurrentActiveControlledFileId());
        assertNull(f.engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(f.processId).singleResult());
        var original=f.rows();f.now.set(f.selected.plusDays(1));finalizer.handleProcessInstanceStatusChanged(actualEnd.get());assertEquals(original,f.rows());
    }

    @Test void realEndLateControlFailureRollsBackTaskSignatureProjectionAndCoreFacts() {
        var before=f.rows();
        f.jdbc.execute("ALTER TABLE dcc_workflow_lifecycle_event ADD CONSTRAINT g02_end_fail CHECK(event_type<>'CONTROLLED')");
        assertThrows(RuntimeException.class,()->f.tx.executeWithoutResult(s->{f.saveSignedProjection();f.approveEngineTask();}));
        assertNotNull(actualEnd.get(),"must fail after a real End, never before engine completion");
        assertEquals(before,f.rows());
        assertNotNull(f.engine.getTaskService().createTaskQuery().taskId(f.confirmTaskId).singleResult());
        assertNull(f.files.selectById(20L).getDelayedEffectiveSignatureId());
        assertEquals(0,f.bindingRows.selectList().size());
        f.jdbc.execute("ALTER TABLE dcc_workflow_lifecycle_event DROP CONSTRAINT g02_end_fail");
        var newS=f.signedAt.plusSeconds(2);f.signatureTime.set(newS);
        f.tx.executeWithoutResult(s->{f.saveSignedProjection();f.approveEngineTask();});
        assertEquals(newS,f.files.selectById(20L).getControlledTime());
        assertNotEquals(f.signedAt,f.files.selectById(20L).getDelayedEffectiveConfirmedAt());
        assertEquals(1,f.events("CONTROLLED"));
    }

    @Test void signedButUncompletedEngineTaskCannotControlOrUseTechnicalRetry() {
        f.tx.executeWithoutResult(s->f.saveSignedProjection());
        assertThrows(RuntimeException.class,()->f.confirmation.requireConfirmed(f.files.selectById(20L)));
        assertNotNull(f.engine.getTaskService().createTaskQuery().taskId(f.confirmTaskId).singleResult());
        assertNull(actualEnd.get());
        f.jdbc.update("UPDATE dcc_controlled_file SET status='FINALIZATION_FAILED' WHERE id=20");
        var before=f.rows();var denied=assertThrows(RuntimeException.class,()->finalizer.retryStamp(99L,20L));
        assertFalse(denied instanceof NullPointerException,"failure must come from the actual evidence/process gate");assertEquals(before,f.rows());
        assertEquals(0,f.bindingRows.selectList().size());
    }

    @Test void completedReplayCannotSubstituteDocumentControlForOriginalRequester() {
        f.tx.executeWithoutResult(s->{f.saveSignedProjection();f.approveEngineTask();});
        var before=f.rows();actualEnd.get().setActorUserId(88L);
        assertThrows(RuntimeException.class,()->finalizer.handleProcessInstanceStatusChanged(actualEnd.get()));
        assertEquals(before,f.rows());
    }

    @Test void publicCommandProducerBpmAndOfficialEndPreserveOneMicrosecondS() throws Exception {
        var chain=new PublicCommandChain();var response=chain.confirm();
        var u=chain.unifiedRows.selectList().get(0);var projection=f.signatures.selectById(response.getSignatureId());var file=f.files.selectById(20L);
        assertEquals("CONFIRMED",response.getStatus());assertEquals(0,u.getSignedAt().getNano()%1000);
        assertEquals(u.getSignedAt(),response.getSignedAt());assertEquals(u.getSignedAt(),projection.getSignedAt());
        assertEquals(u.getSignedAt(),file.getDelayedEffectiveConfirmedAt());assertEquals(u.getSignedAt(),file.getControlledTime());
        assertNotEquals(u.getSignedAt(),file.getPublishedTime());assertEquals(chain.request.getSelectedEffectiveTime(),file.getDelayedEffectiveTime());
        assertEquals("VALID",chain.unifiedVerifier.verifyEvidence(u.getId()).verificationStatus());
        assertEquals("VALID",f.verifier.verifySignatureEvidence(projection.getId()).getVerificationStatus());
        assertEquals(99L,actualEnd.get().getActorUserId());
        var done=f.engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(f.confirmTaskId).includeTaskLocalVariables().singleResult();
        assertNotNull(done.getEndTime());assertEquals(2,done.getTaskLocalVariables().get("TASK_STATUS"));
        assertEquals(1,chain.auditCount("signature.record.create"));assertEquals(1,chain.auditCount("dcc.controlled-file.delayed-effective-confirm"));assertEquals(1,chain.auditCount("dcc.controlled-file.control"));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",file.getStatus());assertEquals(10L,f.masters.selectById(3L).getCurrentActiveControlledFileId());
        var before=chain.rows();var replay=chain.confirm();assertEquals(response.getSignatureId(),replay.getSignatureId());assertEquals(before,chain.rows());
    }

    @Test void publicCommandLateRealGxpAppendFailureRollsBackUnifiedDccCasAndEngineThenRetries() throws Exception {
        var chain=new PublicCommandChain();var before=chain.rows();
        f.jdbc.execute("ALTER TABLE gxp_audit_event ADD CONSTRAINT g02_public_late CHECK(operation_id<>'dcc.controlled-file.control')");
        var failure=assertThrows(RuntimeException.class,chain::confirm);
        assertNotNull(actualEnd.get(),"must reach the real End before late audit failure");
        assertEquals(before,chain.rows());assertTrue(chain.unifiedRows.selectList().isEmpty());
        assertNotNull(f.engine.getTaskService().createTaskQuery().taskId(f.confirmTaskId).singleResult());
        assertNull(f.files.selectById(20L).getDelayedEffectiveSignatureId());
        f.jdbc.execute("ALTER TABLE gxp_audit_event DROP CONSTRAINT g02_public_late");
        var response=chain.confirm();assertEquals(response.getSignedAt(),f.files.selectById(20L).getControlledTime());
        assertEquals(1,chain.auditCount("signature.record.create"));assertEquals(1,f.events("CONTROLLED"));
        assertFalse(failure instanceof NullPointerException);
    }

    @Test void publicCommandRejectsStaleIdentityBeforeProducerAndTaskMutation() throws Exception {
        var chain=new PublicCommandChain();var before=chain.rows();
        var original=f.bound.resolve(f.files.selectById(20L));
        for(String conflict:List.of("ROUND","DEFINITION","DIGEST","PROCESS","TASK")) {
            chain.request.setAttributeRound(original.attributeRound());chain.request.setExpectedProcessDefinitionId(original.processDefinitionId());
            chain.request.setExpectedDefinitionDigest(original.definitionDigest());chain.request.setProcessInstanceId(f.processId);chain.request.setTaskId(f.confirmTaskId);
            switch(conflict) {
                case "ROUND"->chain.request.setAttributeRound(original.attributeRound()+1);
                case "DEFINITION"->chain.request.setExpectedProcessDefinitionId(original.processDefinitionId()+"-different");
                case "DIGEST"->chain.request.setExpectedDefinitionDigest("0".repeat(64));
                case "PROCESS"->chain.request.setProcessInstanceId(f.processId+"-different");
                case "TASK"->chain.request.setTaskId(f.confirmTaskId+"-different");
            }
            assertThrows(IllegalArgumentException.class,chain::confirm,conflict);
            assertEquals(before,chain.rows(),conflict);
            assertNotNull(f.engine.getTaskService().createTaskQuery().taskId(f.confirmTaskId).singleResult());
        }
        verify(accounts,never()).reauthenticateForSignature(anyLong(),anyString());assertNull(actualEnd.get());
    }

    @Test void publicCommandRejectsForeignCompilerAndInvalidTimeWithoutSigning() throws Exception {
        var chain=new PublicCommandChain();var before=chain.rows();
        assertThrows(IllegalArgumentException.class,()->f.tx.execute(s->chain.command.confirm(88L,20L,chain.request)));
        assertEquals(before,chain.rows());
        for(LocalDateTime invalid:Arrays.asList(null,chain.request.getSelectedEffectiveTime().plusNanos(1),
                ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDate().minusDays(1).atStartOfDay())) {
            chain.request.setSelectedEffectiveTime(invalid);
            assertThrows(IllegalArgumentException.class,chain::confirm);
            assertEquals(before,chain.rows());
        }
        verify(accounts,never()).reauthenticateForSignature(anyLong(),anyString());
        assertNull(actualEnd.get());assertNotNull(f.engine.getTaskService().createTaskQuery().taskId(f.confirmTaskId).singleResult());
    }

    @Test void publicCommandChangedSelectionReplayCannotResignOrMutateS() throws Exception {
        var chain=new PublicCommandChain();var saved=chain.confirm();var before=chain.rows();
        chain.request.setSelectedEffectiveTime(chain.request.getSelectedEffectiveTime().plusSeconds(1));
        assertThrows(IllegalArgumentException.class,chain::confirm);assertEquals(before,chain.rows());
        assertEquals(saved.getSignedAt(),f.files.selectById(20L).getControlledTime());
        verify(accounts,times(1)).reauthenticateForSignature(99L,"g02-isolated-credential");
    }

    @Test void publicTrainingCommandWaitsForReceiveAndConsumesExactOriginalUploadRecord() throws Exception {
        events.close();f.close();actualEnd.set(null);
        f=new DccG02ControlledSignatureTimeTransactionTest.Fixture(DccWorkflowDatePolicy.DELAYED,true);installFinalization();
        assertNull(f.engine.getTaskService().createTaskQuery().processInstanceId(f.processId).singleResult());
        assertNull(f.files.selectById(20L).getControlledTime());assertNull(actualEnd.get());
        f.commitTrainingRecord();assertNull(actualEnd.get());assertNull(f.files.selectById(20L).getControlledTime());
        var chain=new PublicCommandChain();var response=chain.confirm();
        assertEquals(response.getSignedAt(),f.files.selectById(20L).getControlledTime());assertEquals(99L,actualEnd.get().getActorUserId());
        var training=f.engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(f.processId).activityId("TRAINING").singleResult();
        assertNotNull(training.getEndTime());assertEquals(1,chain.auditCount("dcc.controlled-file.control"));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",f.files.selectById(20L).getStatus());
    }

    @Test void publicTrainingCommandRejectsForeignRoundRecordBeforeSigning() throws Exception {
        events.close();f.close();actualEnd.set(null);
        f=new DccG02ControlledSignatureTimeTransactionTest.Fixture(DccWorkflowDatePolicy.DELAYED,true);installFinalization();f.commitTrainingRecord();
        f.jdbc.update("UPDATE dcc_controlled_file_attachment SET application_process_instance_id='foreign-round' WHERE controlled_file_id=20");
        var chain=new PublicCommandChain();var before=chain.rows();assertThrows(IllegalArgumentException.class,chain::confirm);
        assertEquals(before,chain.rows());assertNull(actualEnd.get());verify(accounts,never()).reauthenticateForSignature(anyLong(),anyString());
        assertNotNull(f.engine.getTaskService().createTaskQuery().taskId(f.confirmTaskId).singleResult());
    }

    /** Only publicCommand* tests install this actual producer/command/ledger/BPM composition. */
    final class PublicCommandChain {
        final cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper unifiedRows;
        final cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl unifiedVerifier=new cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl();
        final DccDelayedEffectiveConfirmationService command=new DccDelayedEffectiveConfirmationService();
        final cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDelayedEffectiveConfirmReqVO request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDelayedEffectiveConfirmReqVO();
        final GxpSqlFailureCapture sqlFailure=new GxpSqlFailureCapture();
        PublicCommandChain() throws Exception {
            f.now.set(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime().plusMinutes(20));
            createOfficialSupportSchemas();
            f.table(cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO.class);
            f.jdbc.execute("ALTER TABLE system_electronic_signature ALTER COLUMN subject_id VARCHAR(2048)");
            f.jdbc.execute("ALTER TABLE system_electronic_signature ALTER COLUMN canonical_content_json VARCHAR(20000)");
            var config=f.mapperSessions.getConfiguration();
            com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.getGlobalConfig(config)
                    .setMetaObjectHandler(new cn.iocoder.yudao.framework.mybatis.core.handler.DefaultDBFieldHandler());
            config.addInterceptor(sqlFailure);
            for(var mapper:List.of(cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class,
                    cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper.class,
                    cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper.class,
                    cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper.class)) config.addMapper(mapper);
            unifiedRows=f.mapperSessions.getMapper(cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper.class);
            var ledger=new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl();
            wire(ledger,"auditEventMapper",f.mapperSessions.getMapper(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper.class),
                    "ledgerSequenceMapper",f.mapperSessions.getMapper(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper.class),
                    "policyOperationMapper",f.mapperSessions.getMapper(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper.class),"adminUserApi",accounts);
            for(String op:List.of("signature.record.create","dcc.controlled-file.delayed-effective-confirm","dcc.controlled-file.control","dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete")) {
                f.jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES(1,'G02-PUBLIC',?,'SERVICE_METHOD','G02 actual composition','DCC','DCC_CONTROLLED_FILE','UPDATE','REQUIRED',?,'PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','G02-public','G02','GXP',TRUE)",op,op.equals("signature.record.create")||op.endsWith("confirm")?"REQUIRED":"NOT_REQUIRED");
            }
            var audit=new DccWorkflowFileStateAudit();wire(audit,"jdbcTemplate",f.jdbc,"auditService",ledger);wire(f.lifecycle,"fileStateAudit",audit);
            var adapter=new DccControlledFileSignatureSubjectAdapter();wire(adapter,"signatureEvidenceService",f.signer);
            var unified=new cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl();
            wire(unified,"signatureRecordMapper",unifiedRows,"adminUserApi",accounts,"gxpAuditService",ledger,"subjectAdapters",List.of(adapter));
            wire(unifiedVerifier,"signatureRecordMapper",unifiedRows);
            var producer=new DccSignatureVerificationServiceImpl();
            var users=mock(cn.iocoder.yudao.module.system.service.user.AdminUserService.class);
            when(users.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO().setId(99L).setUsername("g02-compiler").setNickname("G02 Compiler").setDeptId(7L).setPostIds(Set.of(9L)));
            var depts=mock(cn.iocoder.yudao.module.system.service.dept.DeptService.class);when(depts.getDept(7L)).thenReturn(new cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO().setId(7L).setName("G02 Department"));
            var posts=mock(cn.iocoder.yudao.module.system.service.dept.PostService.class);when(posts.getPostList(Set.of(9L))).thenReturn(List.of(new cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO().setId(9L).setName("Existing compiler qualification")));
            var permissions=mock(cn.iocoder.yudao.module.system.service.permission.PermissionService.class);when(permissions.getUserRoleIdListByUserId(99L)).thenReturn(Set.of(10L));
            var roles=mock(cn.iocoder.yudao.module.system.service.permission.RoleService.class);when(roles.getRoleList(Set.of(10L))).thenReturn(List.of(new cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO().setId(10L).setName("Existing uploader")));
            var electronic=mock(DccElectronicSignatureAuthorizationService.class);
            var images=mock(DccElectronicSignatureImageService.class);
            var image=DccElectronicSignatureImageSnapshot.builder().imageId(1L).versionNo(1).fileId(9L).sha256("a".repeat(64)).contentType("image/png").fileSize(3L).imageStatus("ACTIVE").verifiedStatus("VALID").content(new byte[]{1,2,3}).build();
            when(images.requireActiveSnapshot(99L)).thenReturn(image);
            when(images.verifySignatureSnapshot(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO.class))).thenReturn(image);
            wire(f.verifier,"signatureImageService",images);
            wire(producer,"adminUserService",users,"deptService",depts,"postService",posts,"permissionService",permissions,"roleService",roles,
                    "electronicSignatureAuthorizationService",electronic,"signatureEvidenceService",f.signer,"signatureImageService",images,"signatureMapper",f.signatures,"electronicSignatureService",unified,"unifiedRecords",unifiedRows);
            var bpm=new cn.iocoder.yudao.module.bpm.service.task.BpmTaskServiceImpl();
            var models=mock(cn.iocoder.yudao.module.bpm.service.definition.BpmModelService.class);
            when(models.getBpmnModelByDefinitionId(anyString())).thenAnswer(c->f.engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
            wire(bpm,"taskService",f.engine.getTaskService(),"runtimeService",f.engine.getRuntimeService(),"historyService",f.engine.getHistoryService(),"processInstanceService",processService,"modelService",models);
            wire(f.lifecycle,"bpmTaskService",bpm);wire(f.confirmation,"tasks",bpm);wire(finalizer,"bpmTaskService",bpm);
            var dates=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toLocalDateTime();}};dates.setZoneId("Asia/Shanghai");
            wire(command,"fileMapper",f.files,"masterMapper",f.masters,"boundContracts",f.bound,"confirmationEvidence",f.confirmation,"signatureService",producer,"electronicAuthorization",electronic,
                    "signatureManagement",f.verifier,"signatures",f.signatures,"routes",f.routes,"assignments",f.assignments,"lifecycle",f.lifecycle,"datePolicy",dates,"audit",audit,
                    "accounts",accounts,"tasks",bpm,"engineTasks",f.engine.getTaskService(),"history",f.engine.getHistoryService());
            wire(command,"attachments",f.mapperSessions.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAttachmentMapper.class));
            var bound=f.bound.resolve(f.files.selectById(20L));
            request.setTaskId(f.confirmTaskId);request.setProcessInstanceId(f.processId);request.setAttributeRound(bound.attributeRound());request.setExpectedProcessDefinitionId(bound.processDefinitionId());request.setExpectedDefinitionDigest(bound.definitionDigest());
            request.setSelectedEffectiveTime(f.now.get().plusDays(1).withNano(0));request.setPassword("g02-isolated-credential");request.setReason("实际公开确认文件衔接关系");request.setIdempotencyKey("G02-PUBLIC-"+f.processId);
        }
        cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDelayedEffectiveConfirmRespVO confirm(){
            try{return f.tx.execute(s->command.confirm(99L,20L,request));}
            catch(RuntimeException failure){if(sqlFailure.actual!=null)System.err.println("G02_ACTUAL_GXP_SQL_FAILURE "+sqlFailure.actual);throw failure;}
        }
        int auditCount(String op){return f.jdbc.queryForObject("SELECT COUNT(*) FROM gxp_audit_event WHERE operation_id=?",Integer.class,op);}
        Map<String,List<Map<String,Object>>> rows(){var r=f.rows();for(String table:List.of("system_electronic_signature","gxp_audit_event","gxp_audit_ledger_sequence"))r.put(table,f.jdbc.queryForList("SELECT * FROM "+table));return r;}
        void createOfficialSupportSchemas() throws Exception {
            Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();assertNotNull(root);
            String gxp=Files.readString(root.resolve("IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/dcc_b_gxp_audit_tables.sql"));
            // Flowable uses its explicit H2 LEGACY dialect; table comments are metadata, not constraints.
            for(String sql:gxp.split(";"))if(!sql.isBlank())f.jdbc.execute(sql.replaceAll("\\) COMMENT '[^']*'", ")"));
            String officialGxp=Files.readString(root.resolve("IntRuoyiBackend/sql/mysql/20260908_gxp_audit_trail_core.sql"));
            assertTrue(officialGxp.contains("`subject_id` varchar(2048)"));assertTrue(officialGxp.contains("`idempotency_key` varchar(512)"));
            // The reused historical test resource has smaller capacities than the formal core DDL.
            // Match the current formal schema only in this task-owned database.
            f.jdbc.execute("ALTER TABLE gxp_audit_event ALTER COLUMN subject_id VARCHAR(2048)");
            f.jdbc.execute("ALTER TABLE gxp_audit_event ALTER COLUMN idempotency_key VARCHAR(512)");
            String schema=Files.readString(root.resolve("IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql"));
            for(String name:List.of("dcc_controlled_file_name_claim","dcc_legacy_source_name_scope","dcc_legacy_source_name_evidence","dcc_source_name_reservation","infra_file_config")) {
                var match=Pattern.compile("CREATE TABLE IF NOT EXISTS \\\"?"+name+"\\\"?\\s*\\(.*?\\);",Pattern.DOTALL).matcher(schema);
                assertTrue(match.find(),"official support table "+name);
                f.jdbc.execute(match.group().replaceAll("UNIQUE KEY (\\w+)","CONSTRAINT $1 UNIQUE"));
            }
            f.jdbc.execute("CREATE ALIAS JSON_EXTRACT FOR 'cn.iocoder.yudao.module.dcc.service.file.DccG02DelayedFinalizationTransactionTest.jsonExtract'");
            f.jdbc.execute("CREATE ALIAS JSON_UNQUOTE FOR 'cn.iocoder.yudao.module.dcc.service.file.DccG02DelayedFinalizationTransactionTest.jsonUnquote'");
            f.jdbc.execute("CREATE ALIAS HEX FOR 'cn.iocoder.yudao.module.dcc.service.file.DccG02DelayedFinalizationTransactionTest.hex'");
        }
    }
    public static String jsonExtract(String json,String path){if(json==null)return null;var node=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json).get(path.substring(2));return node==null?null:node.toString();}
    public static String jsonUnquote(String value){if(value==null)return null;return value.startsWith("\"")?cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(value,String.class):value;}
    public static String hex(byte[] value){return value==null?null:HexFormat.of().withUpperCase().formatHex(value);}
    @org.apache.ibatis.plugin.Intercepts(@org.apache.ibatis.plugin.Signature(type=org.apache.ibatis.executor.Executor.class,method="update",args={org.apache.ibatis.mapping.MappedStatement.class,Object.class}))
    public static final class GxpSqlFailureCapture implements org.apache.ibatis.plugin.Interceptor {
        String actual;
        @Override public Object intercept(org.apache.ibatis.plugin.Invocation call) throws Throwable {
            try{return call.proceed();}catch(Throwable failure){
                if(call.getArgs()[1] instanceof cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO event){
                    Throwable root=failure;while(root.getCause()!=null)root=root.getCause();
                    String code=root instanceof java.sql.SQLException sql?sql.getSQLState()+"/"+sql.getErrorCode():root.getClass().getSimpleName();
                    String detail=root.getMessage()==null?"":root.getMessage().split(":",2)[0];
                    actual="operation="+event.getOperationId()+", subjectLength="+event.getSubjectId().length()+", keyLength="+event.getIdempotencyKey().length()+", sql="+code+", detail="+detail;
                }
                throw failure;
            }
        }
    }
}
