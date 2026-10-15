package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectApplicationAttributesDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectApplicationAttributesMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.junit.jupiter.api.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual core Mappers, Spring transactions, deployed V6 history, Bound and DCC HMAC verifier.
 * This tests consumption of a committed confirmation, not the public confirmation/authentication command.
 * Account/image-directory/storage and platform/audit/distribution are declared external ports in this class.
 * It never mocks VerifiedConfirmation, completion facts, source hashing or signature verification. */
class DccG02ControlledSignatureTimeTransactionTest {
    Fixture f;
    @BeforeEach void setup() throws Exception { f = new Fixture(); }
    @AfterEach void cleanup() { if (f != null) f.close(); }

    @Test void delayedControlUsesActualSignedTimeInsteadOfLaterProcessingTime() {
        f.confirmCommitted();
        var proof = f.confirmation.requireConfirmed(f.files.selectById(20L));
        assertEquals(f.signedAt, proof.signedAt());
        assertEquals(f.selected, proof.selectedEffectiveTime());
        assertNotEquals(f.signedAt, f.now.get());
        f.artifacts(); f.control();
        var saved = f.files.selectById(20L);
        assertEquals(f.signedAt, saved.getControlledTime());
        assertEquals(f.now.get(), saved.getPublishedTime());
        assertEquals(f.now.get(), f.jdbc.queryForObject(
                "SELECT occurred_at FROM dcc_workflow_lifecycle_event WHERE event_type='CONTROLLED'", LocalDateTime.class));
        assertEquals("CONTROLLED_PENDING_EFFECTIVE", saved.getStatus());
        assertNull(saved.getActivatedTime());
        assertEquals(10L, f.masters.selectById(3L).getCurrentActiveControlledFileId());
        assertEquals(20L, f.masters.selectById(3L).getLatestControlledFileId());
        assertEquals(0, f.events("ACTIVATED"));
        verify(f.batches, never()).acceptActivatedEvent(any());
    }

    @Test void sameDayFutureInstantDoesNotActivateUntilExactTimeAndNeverRewritesS() {
        f.confirmCommitted(); f.artifacts(); f.control();
        f.now.set(f.selected.minusNanos(1));
        assertTrue(f.lifecycle.dueVersions().isEmpty());
        assertEquals(Boolean.FALSE, f.tx.execute(s -> f.lifecycle.activateDue(20L)));
        f.now.set(f.selected);
        assertEquals(List.of(20L), f.lifecycle.dueVersions().stream().map(DccControlledFileDO::getId).toList());
        assertEquals(Boolean.TRUE, f.tx.execute(s -> f.lifecycle.activateDue(20L)));
        assertEquals(f.selected, f.files.selectById(20L).getActivatedTime());
        assertEquals(f.selected, f.files.selectById(20L).getEffectiveTime());
        assertEquals(f.signedAt, f.files.selectById(20L).getControlledTime());
        assertEquals(20L, f.masters.selectById(3L).getCurrentActiveControlledFileId());
        f.now.set(f.selected.plusHours(4));
        assertEquals(Boolean.FALSE, f.tx.execute(s -> f.lifecycle.activateDue(20L)));
        assertEquals(1, f.events("ACTIVATED"));
    }

    @Test void committedControlReplayKeepsOriginalSignedFactAndOutbox() {
        f.confirmCommitted(); f.artifacts(); f.control();
        var before = f.rows();
        f.now.set(f.selected.plusDays(1));
        f.control();
        assertEquals(before, f.rows());
        assertEquals(f.signedAt, f.files.selectById(20L).getControlledTime());
    }

    @Test void missingConfirmationIsRejectedBeforeAnyCoreMutation() {
        f.jdbc.update("UPDATE dcc_controlled_file SET status='READY_TO_PUBLISH',published_file_id=211,stamped_file_id=211 WHERE id=20");
        var before = f.rows();
        assertThrows(RuntimeException.class, f::control);
        assertEquals(before, f.rows());
    }

    @Test void tamperedOriginalSignedTimeFailsRealHmacAndCannotControl() {
        f.confirmCommitted(); f.artifacts();
        f.jdbc.update("UPDATE dcc_controlled_file_signature SET signed_at=? WHERE id=?", f.signedAt.plusSeconds(1), f.confirmSignatureId);
        f.jdbc.update("UPDATE dcc_controlled_file SET delayed_effective_confirmed_at=? WHERE id=20", f.signedAt.plusSeconds(1));
        var verify = f.verifier.verifySignatureEvidence(f.confirmSignatureId);
        assertEquals("INVALID", verify.getVerificationStatus());
        var before = f.rows(); assertThrows(RuntimeException.class, f::control); assertEquals(before, f.rows());
    }

    @Test void wrongAttributeRoundCannotBorrowTheCommittedSignature() {
        f.confirmCommitted(); f.artifacts();
        f.jdbc.update("UPDATE dcc_application_round_link SET attribute_round=3 WHERE application_id=20");
        f.jdbc.update("UPDATE dcc_project_application_attributes SET application_round=3 WHERE application_id=20");
        var before = f.rows(); assertThrows(RuntimeException.class, f::control); assertEquals(before, f.rows());
    }

    @Test void changedSavedSIsNotAcceptedAsACommittedControlReplay() {
        f.confirmCommitted();f.artifacts();f.control();
        f.jdbc.update("UPDATE dcc_controlled_file SET controlled_time=? WHERE id=20",f.signedAt.plusSeconds(1));
        var before=f.rows();assertThrows(RuntimeException.class,f::control);assertEquals(before,f.rows());
    }

    @Test void changedSelectedTimeFailsActualHmacWithoutRewritingTheOriginalSignature() {
        f.confirmCommitted();f.artifacts();
        f.jdbc.update("UPDATE dcc_controlled_file_signature SET selected_effective_time=? WHERE id=?",f.selected.plusHours(1),f.confirmSignatureId);
        f.jdbc.update("UPDATE dcc_controlled_file SET delayed_effective_time=? WHERE id=20",f.selected.plusHours(1));
        assertEquals("INVALID",f.verifier.verifySignatureEvidence(f.confirmSignatureId).getVerificationStatus());
        var before=f.rows();assertThrows(RuntimeException.class,f::control);assertEquals(before,f.rows());
    }

    @Test void lateActualOutboxInsertFailureRollsBackAndRetryConsumesCommittedS() {
        f.confirmCommitted(); f.artifacts();
        f.jdbc.execute("ALTER TABLE dcc_workflow_lifecycle_event ADD CONSTRAINT g02_control_fail CHECK(event_type<>'CONTROLLED')");
        var before = f.rows();
        assertThrows(RuntimeException.class, f::control);
        assertEquals(before, f.rows());
        assertEquals(f.signedAt, f.files.selectById(20L).getDelayedEffectiveConfirmedAt());
        assertEquals("ACTIVE", f.files.selectById(10L).getStatus());
        f.jdbc.execute("ALTER TABLE dcc_workflow_lifecycle_event DROP CONSTRAINT g02_control_fail");
        f.now.set(f.now.get().plusMinutes(1)); f.control();
        assertEquals(f.signedAt, f.files.selectById(20L).getControlledTime());
        assertEquals(1, f.events("CONTROLLED"));
    }

    @Test void immediateV6StillConsumesDocumentSignatureAndKeepsControlProcessingTime() throws Exception {
        f.close();f=null;f=new Fixture(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE);
        f.artifacts();f.control();
        var saved=f.files.selectById(20L);
        assertEquals(f.signedAt.minusMinutes(5),saved.getEffectiveTime());
        assertEquals(f.now.get(),saved.getControlledTime());
        assertEquals("ACTIVE",saved.getStatus());
        assertNull(saved.getDelayedEffectiveSignatureId());
    }

    /** Private shared fixture lives in an authorized G02 test file; no global schema changes. */
    static final class Fixture implements AutoCloseable {
        final LocalDateTime signedAt = LocalDateTime.of(2026,10,15,10,0,0,123456000);
        final LocalDateTime selected = LocalDateTime.of(2026,10,15,10,30);
        final AtomicReference<LocalDateTime> signatureTime = new AtomicReference<>(signedAt);
        final AtomicReference<LocalDateTime> now = new AtomicReference<>(LocalDateTime.of(2026,10,15,10,20));
        final JdbcTemplate jdbc;
        final TransactionTemplate tx;
        final DataSourceTransactionManager transactionManager;
        final SqlSessionTemplate mapperSessions;
        final ProcessEngine engine;
        final DccControlledFileMapper files;
        final DccControlledFileMasterMapper masters;
        final DccControlledFileSignatureMapper signatures;
        final DccControlledFileRouteSnapshotMapper routes;
        final DccControlledFileTaskAssigneeSnapshotMapper assignments;
        final DccControlledFileSignatureBindingMapper bindingRows;
        final FileMapper physicalRows;
        final DccBoundWorkflowContractResolver bound = new DccBoundWorkflowContractResolver();
        final DccControlledFileLifecycleService lifecycle = new DccControlledFileLifecycleService();
        final DccDelayedEffectiveConfirmationEvidence confirmation = new DccDelayedEffectiveConfirmationEvidence();
        final DccControlledFileSignatureEvidenceServiceImpl signer = new DccControlledFileSignatureEvidenceServiceImpl();
        final DccElectronicSignatureManagementServiceImpl verifier = new DccElectronicSignatureManagementServiceImpl();
        final DccControlledFileSignatureBindingService binding = new DccControlledFileSignatureBindingService();
        final DccDistributionBatchService batches = mock(DccDistributionBatchService.class);
        final DccControlledContentAdapter platform = mock(DccControlledContentAdapter.class);
        final DccWorkflowFileStateAudit audit = mock(DccWorkflowFileStateAudit.class);
        final FileService storage = mock(FileService.class);
        final BpmTaskService taskReads = mock(BpmTaskService.class);
        final BpmProcessInstanceService processReads = mock(BpmProcessInstanceService.class);
        final BpmProcessDefinitionService definitionReads = mock(BpmProcessDefinitionService.class);
        final Map<String,byte[]> bytes = new HashMap<>();
        String processId;
        String confirmTaskId;
        Long confirmSignatureId;
        boolean failEnd;
        final String effectiveMode;
        final boolean needTraining;

        Fixture() throws Exception { this(DccWorkflowDatePolicy.DELAYED,false); }
        Fixture(String effectiveMode) throws Exception {this(effectiveMode,false);}
        Fixture(String effectiveMode,boolean needTraining) throws Exception {
            this.effectiveMode=effectiveMode;
            this.needTraining=needTraining;
            TenantContextHolder.setTenantId(1L);
            var ds = new DriverManagerDataSource("jdbc:h2:mem:g02_"+UUID.randomUUID()+";MODE=LEGACY;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", "sa", "");
            jdbc = new JdbcTemplate(ds);
            var manager = new DataSourceTransactionManager(ds);transactionManager=manager; tx = new TransactionTemplate(manager);
            for (var type : List.of(DccControlledFileDO.class,DccControlledFileMasterDO.class,DccControlledFileSignatureDO.class,
                    DccControlledFileObsoleteAuditDO.class,DccControlledFileRouteSnapshotDO.class,DccControlledFileTaskAssigneeSnapshotDO.class,
                    DccControlledFileSignatureBindingDO.class,DccControlledFileAttachmentDO.class,DccProjectApplicationAttributesDO.class,FileDO.class)) table(type);
            jdbc.execute("CREATE TABLE dcc_application_round_link(tenant_id BIGINT,project_id BIGINT,application_type VARCHAR(64),application_id BIGINT,bpm_round VARCHAR(128),attribute_round INT)");
            jdbc.execute("CREATE TABLE dcc_workflow_lifecycle_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,event_key VARCHAR(255),event_type VARCHAR(32),master_id BIGINT,controlled_file_id BIGINT,previous_active_file_id BIGINT,version_no VARCHAR(64),approval_process_instance_id VARCHAR(128),occurred_at TIMESTAMP(6),delivery_status VARCHAR(32),UNIQUE(tenant_id,event_key))");
            var factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(ds);
            var cfg = new MybatisConfiguration(); cfg.setMapUnderscoreToCamelCase(true); factory.setConfiguration(cfg);
            factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO)));
            var sessions = factory.getObject();
            for (var type : List.of(DccControlledFileMapper.class,DccControlledFileMasterMapper.class,DccControlledFileSignatureMapper.class,
                    DccControlledFileObsoleteAuditMapper.class,DccControlledFileRouteSnapshotMapper.class,DccControlledFileTaskAssigneeSnapshotMapper.class,
                    DccControlledFileSignatureBindingMapper.class,DccControlledFileAttachmentMapper.class,DccProjectApplicationAttributesMapper.class,FileMapper.class)) sessions.getConfiguration().addMapper(type);
            var m = new SqlSessionTemplate(sessions);mapperSessions=m;
            files=m.getMapper(DccControlledFileMapper.class); masters=m.getMapper(DccControlledFileMasterMapper.class);
            signatures=m.getMapper(DccControlledFileSignatureMapper.class); routes=m.getMapper(DccControlledFileRouteSnapshotMapper.class);
            assignments=m.getMapper(DccControlledFileTaskAssigneeSnapshotMapper.class); bindingRows=m.getMapper(DccControlledFileSignatureBindingMapper.class);
            physicalRows=m.getMapper(FileMapper.class);
            var engineCfg = new SpringProcessEngineConfiguration(); engineCfg.setDataSource(ds); engineCfg.setTransactionManager(manager);
            engineCfg.setDatabaseSchemaUpdate("true"); engineCfg.setAsyncExecutorActivate(false); engineCfg.setDisableIdmEngine(true);
            engine=engineCfg.buildProcessEngine();
            when(definitionReads.getProcessDefinition(anyString())).thenAnswer(c->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(c.getArgument(0)).singleResult());
            when(definitionReads.getProcessDefinitionBpmnModel(anyString())).thenAnswer(c->engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
            when(processReads.getProcessInstance(anyString())).thenAnswer(c->engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(c.getArgument(0)).includeProcessVariables().singleResult());
            when(processReads.getHistoricProcessInstance(anyString())).thenAnswer(c->engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(c.getArgument(0)).includeProcessVariables().singleResult());
            when(taskReads.getTask(anyString())).thenAnswer(c->engine.getTaskService().createTaskQuery().taskId(c.getArgument(0)).includeTaskLocalVariables().singleResult());
            when(taskReads.getHistoricTask(anyString())).thenAnswer(c->engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(c.getArgument(0)).includeTaskLocalVariables().singleResult());
            when(taskReads.getRunningTaskListByProcessInstanceId(anyString(),any(),any())).thenAnswer(c->engine.getTaskService().createTaskQuery().processInstanceId(c.getArgument(0)).includeTaskLocalVariables().list());
            var rounds = new DccApplicationRoundService(); wire(rounds,"jdbc",jdbc);
            wire(bound,"definitions",definitionReads,"processes",processReads,"rounds",rounds,"attributes",m.getMapper(DccProjectApplicationAttributesMapper.class),"routes",routes);
            var contracts = new DccLifecycleEffectiveContractResolver(); wire(contracts,"boundContracts",bound);
            var dates = new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return now.get();}}; dates.setZoneId("Asia/Shanghai");
            wire(lifecycle,"controlledFileMapper",files,"masterMapper",masters,"approvalSignatureMapper",signatures,"signatureManagementService",verifier,
                    "bpmTaskService",taskReads,"effectiveContractResolver",contracts,"datePolicy",dates,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                    "jdbcTemplate",jdbc,"obsoleteAuditMapper",m.getMapper(DccControlledFileObsoleteAuditMapper.class),"obsoleteRetentionService",mock(DccObsoleteRetentionService.class),
                    "platformAdapter",platform,"fileStateAudit",audit,"eventPublisher",(ApplicationEventPublisher)e->{},"distributionBatchService",batches);
            wire(confirmation,"signatureMapper",signatures,"signatureManagement",verifier,"boundContracts",bound,"tasks",taskReads);
            // Only inject production resources when they exist; missing integration stays a real test failure.
            if(org.springframework.util.ReflectionUtils.findField(lifecycle.getClass(),"confirmationEvidence")!=null) wire(lifecycle,"confirmationEvidence",confirmation);
            if(org.springframework.util.ReflectionUtils.findField(lifecycle.getClass(),"boundContracts")!=null) wire(lifecycle,"boundContracts",bound);
            when(batches.acceptActivatedEvent(any())).thenReturn(900L);
            when(batches.processAutomaticBatch(900L)).thenReturn(new DccDistributionBatchResult(900L,"COMPLETED",null,null));
            when(storage.getFile(anyLong())).thenAnswer(c->physicalRows.selectById(c.getArgument(0)));
            when(storage.getFileContent(anyLong(),anyString())).thenAnswer(c->{var data=bytes.get(c.getArgument(1));if(data==null)throw new IllegalStateException("G02 task-owned bytes missing");return data;});
            var properties = new DccSignatureEvidenceProperties(); properties.setKeyVersion("G02-ISOLATED"); properties.setHmacSecret("g02-isolated-hmac-key-for-real-verification");
            var images = mock(DccElectronicSignatureImageService.class);
            when(images.verifySignatureSnapshot(any(DccControlledFileSignatureDO.class))).thenAnswer(c->{var s=c.getArgument(0,DccControlledFileSignatureDO.class);
                return DccElectronicSignatureImageSnapshot.builder().imageId(s.getSignatureImageId()).versionNo(s.getSignatureImageVersionNo())
                        .fileId(s.getSignatureImageFileId()).sha256(s.getSignatureImageSha256()).content(new byte[]{1,2,3}).verifiedStatus("VALID").build();});
            wire(binding,"bindingMapper",bindingRows,"signatureMapper",signatures,"fileService",storage);
            wire(signer,"controlledFileMapper",files,"fileService",storage,"signatureEvidenceProperties",properties);
            wire(verifier,"signatureMapper",signatures,"controlledFileMapper",files,"fileService",storage,"signatureEvidenceProperties",properties,"signatureImageService",images,"signatureBindingService",binding);
            byte[] source;
            try(var pdf=new org.apache.pdfbox.pdmodel.PDDocument();var output=new java.io.ByteArrayOutputStream()) {
                pdf.addPage(new org.apache.pdfbox.pdmodel.PDPage());pdf.save(output);source=output.toByteArray();
            }
            store(111L,"g02/source.pdf",source);
            store(211L,"g02/controlled.pdf","controlled bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            masters.insert(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).currentActiveControlledFileId(10L).latestControlledFileId(10L).status("ACTIVE_CHAIN").build());
            files.insert(DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(3L).versionNo("A/1").status("ACTIVE").controlledTime(signedAt.minusDays(1)).activatedTime(signedAt.minusDays(1)).processInstanceId("previous-round").build());
            prepareEngine();
        }

        void prepareEngine() throws Exception {
            Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();assertNotNull(root);
            var deployed=engine.getRepositoryService().createDeployment().tenantId("1").addString("g02-upload.bpmn20.xml",Files.readString(root.resolve("IntRuoyiBackend/config/dcc-workflow-v6/upload.bpmn20.xml"))).deploy();
            var definition=engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployed.getId()).singleResult();
            String digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(definition.getId()));
            DccWorkflowDefinitionCatalog.require("PUBLIC_MATRIX_NATIVE_V2",definition.getKey(),digest);
            var vars=new HashMap<String,Object>();vars.put("controlledFileId",20L);vars.put("needTraining",needTraining);vars.put("effectiveMode",effectiveMode);vars.put("dccOriginalRequesterId",99L);
            vars.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,1);
            vars.put("PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("department-31")));
            vars.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,Map.of("DELAYED_EFFECTIVE_CONFIRM",List.of(99L)));
            processId=engine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(definition.getId()).businessKey("20").tenantId("1").variables(vars).start().getId();
            files.insert(DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).dccProjectCodeId(5L).categoryId(2L).versionNo("B/1").fileNumber("G02-B")
                    .requesterId(99L).needTraining(needTraining).sourceFileId(111L).processType("CONTROLLED_FILE").changeType("NEW").approvedTime(signedAt.minusHours(1))
                    .processInstanceId(processId).processDefinitionKey(definition.getKey()).effectiveMode(effectiveMode)
                    .preparedWorkflowContract("PUBLIC_MATRIX_NATIVE_V2").preparedProcessDefinitionId(definition.getId()).preparedDefinitionDigest(digest).status("PENDING_DOC_CONTROL_REVIEW").build());
            jdbc.update("INSERT INTO dcc_application_round_link VALUES(1,5,'UPLOAD',20,?,2)",processId);
            jdbc.update("INSERT INTO dcc_project_application_attributes(tenant_id,project_code_id,application_type,application_id,application_round,submitted,deleted) VALUES(1,5,'UPLOAD',20,2,1,0)");
            for (String stage:List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")) {
                var task=engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult();assertNotNull(task);assertEquals(stage,task.getTaskDefinitionKey());
                engine.getTaskService().setAssignee(task.getId(),"88");
                if("MATRIX_REVIEW".equals(stage)) {
                    var assigned=signStage(task.getId(),"MATRIX_REVIEW_ASSIGN","ASSIGN","部门负责人实际指派");
                    assignments.insert(DccControlledFileTaskAssigneeSnapshotDO.builder().controlledFileId(20L).processInstanceId(processId).stageCode(stage).stageNo(1)
                            .departmentId(31L).leaderUserId(88L).assigneeUserId(88L).obligationId("department-31").bpmTaskId(task.getId())
                            .assignmentSignatureId(assigned.getId()).assignedTime(assigned.getSignedAt()).build());
                }
                String reason="本轮实际批准 "+stage;
                if("MATRIX_APPROVAL".equals(stage))reason+="；批准文件负责人事实：[77,\"g02-owner\",\"G02 Owner\"]";
                var approved=signStage(task.getId(),stage+"_APPROVE","APPROVE",reason);
                if("MATRIX_APPROVAL".equals(stage))files.updateById(DccControlledFileDO.builder().id(20L).fileOwnerUserId(77L)
                        .fileOwnerUsernameSnapshot("g02-owner").fileOwnerNicknameSnapshot("G02 Owner").fileOwnerSignatureId(approved.getId())
                        .fileOwnerApprovalTaskId(task.getId()).fileOwnerProcessInstanceId(processId).fileOwnerSelectedTime(approved.getSignedAt()).build());
                int order=List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW").indexOf(stage);
                routes.insert(DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).routeVersionNo(1).stageCode(stage).stageName(stage).stageNo(order).stageOrder(order)
                        .candidateSourceType("MATRIX_REVIEW".equals(stage)?"DEPT":"USER").candidateSourceId("MATRIX_REVIEW".equals(stage)?31L:88L)
                        .candidateSourceIds("MATRIX_REVIEW".equals(stage)?"31":"88").resolvedUserIds("88").approveMethod("MATRIX_REVIEW".equals(stage)?"ALL":"ANY")
                        .requireAllApprovals("MATRIX_REVIEW".equals(stage)).build());
                if("DOC_CONTROL_REVIEW".equals(stage)&&DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(effectiveMode))
                    tx.executeWithoutResult(s->lifecycle.recordStandardizationSignatureEffect(88L,20L,approved.getId()));
                engine.getTaskService().setVariableLocal(task.getId(),BpmnVariableConstants.TASK_VARIABLE_STATUS,2);
                engine.getRuntimeService().setVariable(processId,BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID,88L);
                engine.getTaskService().complete(task.getId());
            }
            if(needTraining) {
                jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_APPLICANT_TRAINING_RECORD' WHERE id=20");
                assertNotNull(engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").singleResult());
                return;
            }
            if(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(effectiveMode)) return;
            jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_DELAYED_EFFECTIVE_CONFIRM' WHERE id=20");
            var confirm=engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult();assertNotNull(confirm);assertEquals(DccDelayedEffectiveConfirmationEvidence.STAGE,confirm.getTaskDefinitionKey());
            engine.getTaskService().setAssignee(confirm.getId(),"99");confirmTaskId=confirm.getId();
            assertEquals("PUBLIC_MATRIX_NATIVE_V2",bound.resolve(files.selectById(20L)).executionContract());
        }

        /** Committed training input fixture: actual record Mapper/bytes and engine ReceiveTask, not upload UI. */
        void commitTrainingRecord() {
            tx.executeWithoutResult(s->{
                byte[] content="G02 actual task-owned training record".getBytes(java.nio.charset.StandardCharsets.UTF_8);store(221L,"g02/training-record.pdf",content);
                var record=DccControlledFileAttachmentDO.builder().controlledFileId(20L).applicationProcessInstanceId(processId)
                        .attachmentPurpose("TRAINING_RECORD").storageFileId(221L).originalFileName("G02.pdf").contentType("application/pdf")
                        .fileSize((long)content.length).fileSha256(cn.hutool.crypto.digest.DigestUtil.sha256Hex(content)).uploadedBy(99L).uploadedAt(now.get()).sortNo(0).build();record.setTenantId(1L);
                mapperSessions.getMapper(DccControlledFileAttachmentMapper.class).insert(record);
                jdbc.update("UPDATE dcc_controlled_file SET training_record_file_id=221,status='PENDING_DELAYED_EFFECTIVE_CONFIRM' WHERE id=20");
                var waiting=engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").singleResult();assertNotNull(waiting);
                engine.getRuntimeService().trigger(waiting.getId());
                var task=engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult();assertNotNull(task);assertEquals(DccDelayedEffectiveConfirmationEvidence.STAGE,task.getTaskDefinitionKey());
                engine.getTaskService().setAssignee(task.getId(),"99");confirmTaskId=task.getId();
            });
        }

        DccControlledFileSignatureDO signStage(String taskId,String meaning,String action,String reason) {
            var req=DccControlledFileSignatureEvidenceCreateReq.builder().tenantId(1L).controlledFileId(20L).processInstanceId(processId).taskId(taskId)
                    .taskActionResult("ASSIGN".equals(action)?"ASSIGNED":"APPROVED").meaningCode(meaning).signerUserId(88L).signerUsername("g02-doc").signerNickname("G02 Document Control")
                    .signerPostNames("existing approval qualification").signerRoleNames("existing approval qualification").signaturePurpose("actual prior stage")
                    .authorizationBasis("actual frozen stage actor").authenticationMethod("PASSWORD").signedAt(signedAt.minusMinutes(5)).reasonText(reason)
                    .controlledCopyHashStatus("NOT_APPLICABLE").signatureImageId(1L).signatureImageVersionNo(1).signatureImageFileId(9L)
                    .signatureImageSha256("a".repeat(64)).signatureImageContentType("image/png").signatureImageFileSize(3L).signatureImageStatusSnapshot("ACTIVE").signatureImageVerifiedStatus("VALID").build();
            var proof=signer.createEvidence(req);var row=new DccControlledFileSignatureDO();BeanUtils.copyProperties(proof,row);
            row.setControlledFileId(20L);row.setActorId(88L);row.setTaskId(taskId);row.setActionType(action);row.setMeaningCode(meaning);
            row.setActorUsernameSnapshot(req.getSignerUsername());row.setActorNicknameSnapshot(req.getSignerNickname());row.setActorPostNamesSnapshot(req.getSignerPostNames());row.setActorRoleNamesSnapshot(req.getSignerRoleNames());
            row.setSignaturePurpose(req.getSignaturePurpose());row.setAuthorizationBasis(req.getAuthorizationBasis());row.setAuthenticationMethod("PASSWORD");row.setPasswordVerified(true);row.setSignedAt(req.getSignedAt());row.setComment(reason);
            signatures.insert(row);var checked=verifier.verifySignatureEvidence(row.getId());assertEquals("VALID",checked.getVerificationStatus(),checked.getVerificationReason());return row;
        }

        void saveSignedProjection() {
            LocalDateTime actualSignedAt=signatureTime.get();
            var req=DccControlledFileSignatureEvidenceCreateReq.builder().tenantId(1L).controlledFileId(20L).processInstanceId(processId).taskId(confirmTaskId)
                    .taskActionResult("APPROVED").meaningCode(DccDelayedEffectiveConfirmationEvidence.MEANING).signerUserId(99L).signerUsername("g02-requester").signerNickname("G02 Requester")
                    .signerPostNames("existing signer qualification").signerRoleNames("existing signer qualification").signaturePurpose("delayed effective confirmation")
                    .authorizationBasis("actual bound requester task").authenticationMethod("PASSWORD").signedAt(actualSignedAt).selectedEffectiveTime(selected).attributeRound(2)
                    .reasonText("文件衔接时间确认").controlledCopyHashStatus("NOT_APPLICABLE").signatureImageId(1L).signatureImageVersionNo(1).signatureImageFileId(9L)
                    .signatureImageSha256("a".repeat(64)).signatureImageContentType("image/png").signatureImageFileSize(3L).signatureImageStatusSnapshot("ACTIVE").signatureImageVerifiedStatus("VALID").build();
            var proof=signer.createEvidence(req);assertEquals(DccDelayedEffectiveConfirmationEvidence.PAYLOAD,proof.getEvidencePayloadVersion(),"G01 delayed canonical/HMAC must be real before G02 behavior RED");
            var row=new DccControlledFileSignatureDO();BeanUtils.copyProperties(proof,row);
            row.setControlledFileId(20L);row.setActorId(99L);row.setTaskId(confirmTaskId);row.setActionType("APPROVE");row.setMeaningCode(DccDelayedEffectiveConfirmationEvidence.MEANING);
            row.setActorUsernameSnapshot(req.getSignerUsername());row.setActorNicknameSnapshot(req.getSignerNickname());row.setActorPostNamesSnapshot(req.getSignerPostNames());row.setActorRoleNamesSnapshot(req.getSignerRoleNames());
            row.setSignaturePurpose(req.getSignaturePurpose());row.setAuthorizationBasis(req.getAuthorizationBasis());row.setAuthenticationMethod("PASSWORD");row.setPasswordVerified(true);
            row.setSignedAt(actualSignedAt);row.setSelectedEffectiveTime(selected);row.setAttributeRound(2);row.setComment(req.getReasonText());signatures.insert(row);confirmSignatureId=row.getId();
            var verified=verifier.verifySignatureEvidence(confirmSignatureId);assertEquals("VALID",verified.getVerificationStatus(),verified.getVerificationReason());
            files.updateById(DccControlledFileDO.builder().id(20L).delayedEffectiveTime(selected).delayedEffectiveSignatureId(confirmSignatureId).delayedEffectiveConfirmedAt(actualSignedAt).delayedEffectiveProcessInstanceId(processId).build());
        }
        void approveEngineTask() {
            engine.getTaskService().setVariablesLocal(confirmTaskId,Map.of(BpmnVariableConstants.TASK_VARIABLE_STATUS,2,"dccVerifiedSignatureId",confirmSignatureId,"dccVerifiedSignatureActorId",99L,"dccVerifiedSignatureAction","APPROVE"));
            engine.getRuntimeService().setVariable(processId,BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID,99L);
            engine.getTaskService().complete(confirmTaskId);
        }
        void confirmCommitted(){tx.executeWithoutResult(s->{saveSignedProjection();approveEngineTask();});}
        void artifacts(){jdbc.update("UPDATE dcc_controlled_file SET status='READY_TO_PUBLISH',published_file_id=211,stamped_file_id=211 WHERE id=20");binding.bindPublishedCopy(files.selectById(20L),211L,99L,"g02-committed-artifact");}
        void control(){tx.executeWithoutResult(s->{var master=masters.selectByIdForUpdate(3L);var file=files.selectByIdAndTenantForUpdate(1L,20L);lifecycle.completeControl(file,master,99L);});}
        int events(String type){return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event WHERE event_type=?",Integer.class,type);}
        Map<String,List<Map<String,Object>>> rows(){var result=new TreeMap<String,List<Map<String,Object>>>();for(String table:List.of("dcc_controlled_file","dcc_controlled_file_master","dcc_controlled_file_signature","dcc_workflow_lifecycle_event"))result.put(table,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"));return result;}
        void store(Long id,String path,byte[] content){bytes.put(path,content);physicalRows.insert(FileDO.builder().id(id).configId(7L).path(path).url("g02-task://"+path).name("G02.pdf").type("application/pdf").size((long)content.length).build());}
        void table(Class<?> type){var annotation=type.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);var columns=new LinkedHashMap<String,String>();
            for(var cls=type;cls!=Object.class;cls=cls.getSuperclass())for(var field:cls.getDeclaredFields()){if(Modifier.isStatic(field.getModifiers()))continue;
                String name=field.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase();var t=field.getType();
                String sql=t==Long.class?"BIGINT":t==Integer.class?"INT":t==Boolean.class?"TINYINT DEFAULT 0":t==LocalDateTime.class?"TIMESTAMP(6)":t==LocalDate.class?"DATE":"VARCHAR(8192)";
                columns.put(name,"id".equals(name)?"id BIGINT AUTO_INCREMENT PRIMARY KEY":name+" "+sql);}
            jdbc.execute("CREATE TABLE "+annotation.value()+"("+String.join(",",columns.values())+")");}
        static void wire(Object target,Object... args){for(int i=0;i<args.length;i+=2)ReflectionTestUtils.setField(target,(String)args[i],args[i+1]);}
        @Override public void close(){try{engine.close();}finally{jdbc.execute("SHUTDOWN");TenantContextHolder.clear();}}
    }
}
