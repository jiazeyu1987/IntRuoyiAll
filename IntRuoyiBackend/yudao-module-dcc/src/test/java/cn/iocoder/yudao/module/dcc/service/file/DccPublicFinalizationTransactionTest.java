package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.BpmProcessInstanceEventListener;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.dcc.service.file.listener.DccControlledFileStatusListener;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.dal.mysql.controlledcontent.*;
import cn.iocoder.yudao.module.system.enums.controlledcontent.*;
import cn.iocoder.yudao.module.system.service.controlledcontent.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.flowable.engine.*;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.BeanUtils;
import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Spring/Flowable transaction, official End chain, HMAC verification, PDF stamp, binding, platform and GxP.
 * Frozen actor source and bound numeric-attribute identity are explicit W05 external ports; storage is a task-owned bytes port.
 * This proves downstream finalization, not production supervisor discovery or all Workflow commands. */
@Import({GxpAuditServiceImpl.class,cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl.class,
        cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl.class,cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccPublicFinalizationTransactionTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager transactionManager;
    @Resource(name="dccControlledFileMapper") DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccControlledFileSignatureMapper signatures;
    @Resource DccControlledFileRouteSnapshotMapper routes;
    @Resource DccControlledFileTaskAssigneeSnapshotMapper assignments;
    @Resource DccControlledFileAttachmentMapper attachments;
    @Resource DccControlledFileSignatureBindingMapper bindingRows;
    @Resource DccControlledFileObsoleteAuditMapper obsoleteRows;
    @Resource ControlledContentVersionRefMapper refs;
    @Resource ControlledContentTransitionAuditMapper transitions;
    @Resource FileMapper storageRows;
    @Resource GxpAuditServiceImpl ledger;
    @Resource DccDistributionBatchMapper batchRows;
    @Resource DccControlledFileDistributionMapper distributionRows;
    @Resource DccControlledFileDistributionRecipientMapper recipientRows;
    @Resource(name="dccControlledFileMessageJobMapper") DccControlledFileMessageJobMapper messageRows;
    @MockitoBean AdminUserApi accounts;
    @MockitoBean cn.iocoder.yudao.module.system.service.notify.NotifyTemplateService templates;
    @Resource cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl platformApi;
    @Resource cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl platformMessages;
    @Resource cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper notificationRows;
    @Resource DccDistributionDispatchAttemptMapper attemptRows;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper trainingTicketRows;
    private JdbcTemplate jdbc; private TransactionTemplate tx;
    private ProcessEngine engine;private GenericApplicationContext events;
    private DccControlledFileLifecycleService lifecycle;
    private DccControlledFileFinalizationServiceImpl finalization;
    private DccControlledContentAdapter adapter;
    private DccControlledFileSignatureBindingService bindings;
    private DccElectronicSignatureManagementServiceImpl verifier;
    private DccControlledFileSignatureEvidenceServiceImpl evidence;
    private DccBoundWorkflowContractResolver boundPort;
    private BpmTaskService taskReads;
    private BpmProcessInstanceServiceImpl processService;
    private FileService storage;
    private ControlledContentLifecycleCoreService core;
    private DccControlledFileDO file;
    private String processId;private org.flowable.engine.repository.ProcessDefinition definition;
    private final Map<String,byte[]> bytes=new HashMap<>();
    private final AtomicLong physicalIds=new AtomicLong(3000);
    private final LocalDateTime approvedAt=LocalDateTime.of(2026,10,12,1,0);
    private BpmProcessInstanceStatusEvent actualEnd;
    private boolean failControlled;
    private DccDistributionBatchService batches;
    private DccWorkflowDatePolicy dates;
    private LocalDateTime businessNow;
    private boolean usePresetDate;
    private boolean expireBeforeDocumentReview;
    private LocalDate submittedPresetDate;
    private boolean readyPlatformNotifications;
    private boolean actualTrainingTicketPath;
    private cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreated actualTrainingTicket;
    private cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketServiceImpl trainingTickets;
    private boolean corruptTrainingBytesAfterBinding;

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);tx=new TransactionTemplate(transactionManager);
        businessNow=LocalDateTime.now().withNano(0);
        // This isolated H2 fixture hosts Flowable's official H2 IDENTITY DDL; use its required explicit dialect.
        jdbc.execute("SET MODE LEGACY");jdbc.update("DELETE FROM infra_file WHERE path LIKE 'task-owned/%'");
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_controlled_file_attachment(id BIGINT AUTO_INCREMENT PRIMARY KEY,controlled_file_id BIGINT,storage_file_id BIGINT,original_file_name VARCHAR(255),content_type VARCHAR(128),file_size BIGINT,file_sha256 VARCHAR(64),sort_no INT,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(64),uploaded_nickname_snapshot VARCHAR(128),uploaded_at TIMESTAMP,tenant_id BIGINT,create_time TIMESTAMP,update_time TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),deleted BIT DEFAULT 0)");
        jdbc.update("DELETE FROM dcc_controlled_file_attachment");
        for(Long id:List.of(77L,88L,99L))when(accounts.getUser(id)).thenReturn(new AdminUserRespDTO().setId(id).setTenantId(1L).setStatus(0)
                .setUsername("account-"+id).setNickname("Account "+id));
        jdbc.execute("CREATE TABLE IF NOT EXISTS controlled_content_version_ref(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),native_master_id BIGINT,native_version_id BIGINT,version_no VARCHAR(64),canonical_status VARCHAR(64),domain_status VARCHAR(128),source_version_ref_id BIGINT,source_native_version_id BIGINT,successor_version_ref_id BIGINT,successor_native_version_id BIGINT,active_unique_flag INT,open_candidate_unique_flag INT,approval_process_instance_id VARCHAR(128),last_transition_time TIMESTAMP,creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS controlled_content_transition_audit(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,version_ref_id BIGINT,content_type VARCHAR(64),content_key VARCHAR(128),from_status VARCHAR(64),to_status VARCHAR(64),domain_from_status VARCHAR(128),domain_to_status VARCHAR(128),action VARCHAR(64),event_key VARCHAR(128),actor_id BIGINT,reason VARCHAR(1024),create_time TIMESTAMP)");
        for(String table:List.of("controlled_content_transition_audit","controlled_content_version_ref","dcc_controlled_file_signature_binding"))jdbc.update("DELETE FROM "+table);
        core=new ControlledContentLifecycleCoreService(refs,transitions,new ControlledContentStateMachine());adapter=new DccControlledContentAdapter();wire(adapter,"lifecycleCoreService",core);
        storage=mock(FileService.class);
        when(storage.getFile(anyLong())).thenAnswer(c->storageRows.selectById(c.getArgument(0)));
        when(storage.getFileContent(anyLong(),anyString())).thenAnswer(c->{var content=bytes.get(c.getArgument(1,String.class));
            if(content==null)throw new IllegalStateException("Task-owned physical bytes missing");return content;});
        when(storage.createFile(any(byte[].class),anyString(),anyString(),anyString())).thenAnswer(c->{long id=physicalIds.incrementAndGet();String path="task-owned/derived-"+id+".pdf";
            store(id,path,c.getArgument(0),c.getArgument(1));return "task-owned://"+path;});
        trainingTickets=new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketServiceImpl();
        wire(trainingTickets,"temporaryFileMapper",trainingTicketRows,"fileMapper",storageRows,"fileService",storage);
        byte[] source;try(var document=new org.apache.pdfbox.pdmodel.PDDocument();var output=new ByteArrayOutputStream()){
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());document.save(output);source=output.toByteArray();}
        store(111L,"task-owned/source.pdf",source,"SOP.pdf");
        bindings=new DccControlledFileSignatureBindingService();wire(bindings,"bindingMapper",bindingRows,"signatureMapper",signatures,"fileService",storage);
        var properties=new DccSignatureEvidenceProperties();properties.setHmacSecret("isolated-w06-current-contract-key");properties.setKeyVersion("IC24-ISOLATED");
        evidence=new DccControlledFileSignatureEvidenceServiceImpl();wire(evidence,"controlledFileMapper",files,"fileService",storage,"signatureEvidenceProperties",properties);
        verifier=new DccElectronicSignatureManagementServiceImpl();wire(verifier,"signatureMapper",signatures,"controlledFileMapper",files,"fileService",storage,
                "signatureEvidenceProperties",properties,"signatureBindingService",bindings);
        var imagePort=mock(DccElectronicSignatureImageService.class);when(imagePort.verifySignatureSnapshot(any(DccControlledFileSignatureDO.class)))
                .thenReturn(DccElectronicSignatureImageSnapshot.builder().content(new byte[]{1,2,3}).build());wire(verifier,"signatureImageService",imagePort);
        boundPort=mock(DccBoundWorkflowContractResolver.class);when(boundPort.resolve(any())).thenAnswer(c->bound(c.getArgument(0)));
        var effective=new DccLifecycleEffectiveContractResolver();wire(effective,"boundContracts",boundPort);
        var audit=new DccWorkflowFileStateAudit();wire(audit,"jdbcTemplate",jdbc,"auditService",ledger);
        dates=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return businessNow;}};dates.setZoneId("Asia/Singapore");
        lifecycle=new DccControlledFileLifecycleService();
        wire(lifecycle,"controlledFileMapper",files,"masterMapper",masters,"approvalSignatureMapper",signatures,
                "datePolicy",dates,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"jdbcTemplate",jdbc,
                "fileStateAudit",audit,"platformAdapter",adapter,"signatureManagementService",verifier,"effectiveContractResolver",effective,
                "obsoleteAuditMapper",obsoleteRows,"obsoleteRetentionService",mock(DccObsoleteRetentionService.class));
        batches=new DccDistributionBatchService();wire(batches,"controlledFileMapper",files,"batchMapper",batchRows,"distributionMapper",distributionRows,
                "recipientMapper",recipientRows,"messageJobMapper",messageRows,"jdbcTemplate",jdbc,"gxpAuditService",ledger,
                "transactionManager",transactionManager,"distributionService",mock(DccWorkflowDistributionService.class));
        wire(lifecycle,"distributionBatchService",batches);
        // Server roster policy is an explicit task-only unavailable source in this full finalization fixture.
        var rosterProviders=mock(org.springframework.beans.factory.ObjectProvider.class);
        when(rosterProviders.orderedStream()).thenAnswer(ignored->java.util.stream.Stream.of((DccDistributionRosterSource)request->
                new DccDistributionRosterSource.NotReady("ROSTER_POLICY_UNDECIDED","Full finalization fixture: formal policy undecided")));
        wire(batches,"rosterSources",rosterProviders);
        var migrationRoot=Path.of("").toAbsolutePath();while(migrationRoot!=null&&!Files.isRegularFile(migrationRoot.resolve("AGENTS.md")))migrationRoot=migrationRoot.getParent();assertNotNull(migrationRoot);
        String relationDdl=Files.readString(migrationRoot.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_d_relations.sql"))
                .replaceAll("(?m)^--.*$","").replace(" ENGINE=InnoDB","").replace(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin","")
                .replaceAll(",\\s*KEY \\w+ \\([^)]*\\)","");
        for(String ddl:relationDdl.split(";"))if(!ddl.isBlank())jdbc.execute(ddl);
        // Read-only use of the actual W04 migration projection helper; no W04 production/test edits.
        DccRelationStageH2Schema.create(jdbc);
        for(String table:List.of("dcc_relation_controlled_event","dcc_current_file_relation_set","dcc_current_file_relation",
                "dcc_relation_arrangement","dcc_relation_remediation_task","dcc_relation_notification_outbox","dcc_relation_stage_member","dcc_relation_stage_snapshot",
                "dcc_distribution_dispatch_attempt","dcc_distribution_batch"))jdbc.update("DELETE FROM "+table);
        var relationStore=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore(jdbc,ledger);
        var relationFiles=new cn.iocoder.yudao.module.dcc.service.file.relations.DccLatestControlledFileResolverImpl();wire(relationFiles,"fileMapper",files,"masterMapper",masters,"jdbc",jdbc);
        var scheduler=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationNotificationPostCommitScheduler(Runnable::run,relationStore,
                mock(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationNotificationDispatcher.class));
        var remediation=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService(relationStore,relationFiles,
                mock(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationAccessPolicy.class),scheduler);
        var relationConsumer=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationControlledEventConsumer(remediation);
        events=new GenericApplicationContext();events.refresh();wire(lifecycle,"eventPublisher",(org.springframework.context.ApplicationEventPublisher)e->{
            if(e instanceof DccControlledFileLifecycleEvent event)relationConsumer.onLifecycleEvent(event);
            if(failControlled&&e instanceof DccControlledFileLifecycleEvent event&&"CONTROLLED".equals(event.eventType()))throw new IllegalStateException("late required CONTROLLED consumer refusal");});
        processService=new BpmProcessInstanceServiceImpl();wire(processService,"processDefinitionService",mock(BpmProcessDefinitionService.class),
                "messageService",mock(BpmMessageService.class),"processInstanceEventPublisher",new BpmProcessInstanceEventPublisher(events));
        var officialListener=new BpmProcessInstanceEventListener();wire(officialListener,"processInstanceService",processService);
        var config=new SpringProcessEngineConfiguration();config.setDataSource(dataSource);config.setTransactionManager(transactionManager);
        config.setDatabaseSchemaUpdate("true");config.setAsyncExecutorActivate(false);config.setDisableIdmEngine(true);config.setEventListeners(List.of(officialListener));engine=config.buildProcessEngine();
        wire(processService,"runtimeService",engine.getRuntimeService(),"historyService",engine.getHistoryService());
        taskReads=mock(BpmTaskService.class);when(taskReads.getHistoricTask(anyString())).thenAnswer(c->engine.getHistoryService().createHistoricTaskInstanceQuery()
                .taskId(c.getArgument(0)).includeTaskLocalVariables().singleResult());
        when(taskReads.getTask(anyString())).thenAnswer(c->engine.getTaskService().createTaskQuery().taskId(c.getArgument(0)).singleResult());
        when(taskReads.getRunningTaskListByProcessInstanceId(anyString(),any(),any())).thenAnswer(c->engine.getTaskService().createTaskQuery().processInstanceId(c.getArgument(0)).list());
        wire(lifecycle,"bpmTaskService",taskReads);
        finalization=new DccControlledFileFinalizationServiceImpl();
        wire(finalization,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"transactionTemplate",tx,"platformAdapter",adapter,
                "routeSnapshotMapper",routes,"approvalSignatureMapper",signatures,"taskAssigneeSnapshotMapper",assignments,
                "signatureManagementService",verifier,"signatureBindingService",bindings,"lifecycleService",lifecycle,"bpmTaskService",taskReads,
                "fileMapper",storageRows,"fileService",storage,"pdfStampService",new DccPdfStampService(),"adminUserApi",accounts,
                "finalizationFailureService",new DccControlledFileFinalizationFailureService(transactionManager,files,adapter));
        injectIfPresent(finalization,"boundContracts",boundPort);injectIfPresent(finalization,"processes",processService);
        injectIfPresent(finalization,"runtime",engine.getRuntimeService());injectIfPresent(finalization,"history",engine.getHistoryService());injectIfPresent(finalization,"trainingAttachments",attachments);
        var categories=mock(DccFileCategoryMapper.class);when(categories.selectById(2L)).thenReturn(DccFileCategoryDO.builder().approvalSourceMode("PUBLIC_MATRIX").id(2L).trainingRequired(false).distributionRequired(false).build());wire(finalization,"categoryMapper",categories);
        var permissions=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);
        when(permissions.hasAnyRoles(88L,"doc_control")).thenReturn(true);when(permissions.hasAnyPermissions(88L,"dcc:controlled-file:approve")).thenReturn(true);
        var categoryPermissions=mock(DccControlledFileCategoryPermissionSupport.class);
        when(categoryPermissions.hasCategoryPermission(2L,88L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.APPROVE)).thenReturn(true);
        wire(finalization,"permissionApi",permissions,"permissionSupport",categoryPermissions);
        var dccListener=new DccControlledFileStatusListener();wire(dccListener,"finalizationService",finalization);
        events.addApplicationListener((org.springframework.context.ApplicationListener<BpmProcessInstanceStatusEvent>)event->{
            if(BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(event.getStatus())){
                var rejectedTask=engine.getHistoryService().createHistoricTaskInstanceQuery().processInstanceId(event.getId()).includeTaskLocalVariables().singleResult();
                System.out.println("OFFICIAL_REJECT_WINDOW actor="+event.getActorUserId()+" reason="+event.getReason()+" task="+rejectedTask+
                        " ended="+(rejectedTask!=null&&rejectedTask.getEndTime()!=null)+" locals="+(rejectedTask==null?null:rejectedTask.getTaskLocalVariables()));
            }
        });
        events.addApplicationListener(dccListener);
        events.addApplicationListener((org.springframework.context.ApplicationListener<BpmProcessInstanceStatusEvent>)event->actualEnd=event);
        for(String operation:List.of("dcc.controlled-file.standardization-effective","dcc.controlled-file.control","dcc.controlled-file.activate","dcc.controlled-file.auto-obsolete",
                "dcc.distribution.batch.accept","dcc.distribution.batch.fail","dcc.relation.controlled"))policy(operation);
        jdbc.update("UPDATE gxp_audit_policy_operation SET state_policy='ABSENT_TO_PRESENT' WHERE operation_id IN('dcc.relation.controlled','dcc.distribution.batch.accept')");
        login(88L);
    }
    @AfterEach void cleanup(){try{
        if(engine!=null){engine.getProcessEngineConfiguration().getEventDispatcher().setEnabled(false);
            engine.getRepositoryService().createDeploymentQuery().list().forEach(d->engine.getRepositoryService().deleteDeployment(d.getId(),true));engine.close();}
        if(events!=null)events.close();
    }finally{if(jdbc!=null){if(readyPlatformNotifications)jdbc.update("DELETE FROM system_notify_message WHERE template_id=330");
            jdbc.update("DELETE FROM infra_file WHERE path LIKE 'task-owned/%'");jdbc.execute("SET MODE MySQL");}
        SecurityContextHolder.clearContext();TenantContextHolder.clear();}}

    @Test void trainingRequesterBEndsRealReceiveAndCreatesActualStampedBoundControlledArtifact()throws Exception {
        prepare(true);saveTraining();var saved=files.selectById(20L);
        assertNotNull(saved.getControlledTime());assertEquals("ACTIVE",saved.getStatus());assertNotNull(saved.getPublishedFileId());
        assertEquals(approvedAt,saved.getApprovedTime());assertEquals(5,bindingRows.selectList().size());
        assertTrue(bindingRows.selectList().stream().allMatch(binding->Objects.equals(99L,binding.getBoundBy())));
        assertEquals(88L,actualEnd.getActorUserId());
        assertEquals(88L,actor("APPROVE"));assertEquals(99L,actor("START_FINALIZATION"));assertEquals(99L,actor("COMPLETE_CONTROL"));
        assertEquals(99L,jdbc.queryForObject("SELECT actor_id FROM gxp_audit_event WHERE operation_id='dcc.controlled-file.control'",Long.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_controlled_event WHERE controlled_file_id=20",Integer.class));
        assertEquals(1,batchRows.selectList().size());assertEquals("FAILED",batchRows.selectList().get(0).getStatus());
        assertEquals("ROSTER_POLICY_UNDECIDED",batchRows.selectList().get(0).getErrorCode());
        assertEquals(0,recipientRows.selectList().size());assertEquals(0,messageRows.selectList().size());
        var derived=storageRows.selectById(saved.getPublishedFileId());assertFalse(Arrays.equals(bytes.get("task-owned/source.pdf"),bytes.get(derived.getPath())));
        try(var pdf=org.apache.pdfbox.pdmodel.PDDocument.load(bytes.get(derived.getPath()))){assertEquals(1,pdf.getNumberOfPages());assertTrue(pdf.getPage(0).getResources().getXObjectNames().iterator().hasNext());}
        var before=jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id");
        finalization.handleProcessInstanceStatusChanged(actualEnd);assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));assertEquals(5,bindingRows.selectList().size());
    }
    @Test void noTrainingRealDocumentCompletionCreatesArtifactInsideOfficialPreflushWindow()throws Exception{
        prepare(false);var saved=files.selectById(20L);assertNotNull(saved.getControlledTime());assertEquals(approvedAt,saved.getApprovedTime());
        assertEquals(88L,actor("APPROVE"));assertEquals(88L,actor("START_FINALIZATION"));assertEquals(5,bindingRows.selectList().size());
    }
    @Test void lateControlledFailureRollsTrainingAttachmentFileBindingEngineAndNewAuditBack()throws Exception{
        prepare(true);var priorSignatures=jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id");
        var priorEvents=jdbc.queryForList("SELECT * FROM gxp_audit_event ORDER BY id");failControlled=true;
        assertThrows(RuntimeException.class,this::saveTraining);
        assertEquals(DccOfflineTrainingRecordService.STATUS,files.selectById(20L).getStatus());assertNull(files.selectById(20L).getTrainingRecordFileId());
        assertNull(files.selectById(20L).getControlledTime());assertEquals(0,bindingRows.selectList().size());assertEquals(0,attachments.selectList().size());
        assertEquals(0,batchRows.selectList().size());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_controlled_event",Integer.class));
        assertNotNull(engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").singleResult());
        assertEquals(priorSignatures,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));assertEquals(priorEvents,jdbc.queryForList("SELECT * FROM gxp_audit_event ORDER BY id"));
        failControlled=false;saveTraining();assertNotNull(files.selectById(20L).getControlledTime());
    }
    @Test void missingSupervisorHistoricSignatureRefusesBeforeAnyDerivedArtifact()throws Exception{
        prepare(true);jdbc.update("DELETE FROM dcc_controlled_file_signature WHERE meaning_code='SUPERVISOR_REVIEW_APPROVE'");
        assertThrows(RuntimeException.class,this::saveTraining);assertEquals(0,bindingRows.selectList().size());assertEquals(3000,physicalIds.get());
    }
    @Test void completedPublicReplayCannotAcceptADifferentApprovalActor()throws Exception{
        prepare(true);saveTraining();var before=jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id");
        actualEnd.setActorUserId(99L);
        assertThrows(RuntimeException.class,()->finalization.handleProcessInstanceStatusChanged(actualEnd));
        assertEquals(before,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));
    }
    @Test void lateNoTrainingControlFailureRestoresTheRealDocumentTaskAndCannotUseTechnicalRetry()throws Exception{
        failControlled=true;assertThrows(RuntimeException.class,()->prepare(false));
        assertEquals("PENDING_DOC_CONTROL_REVIEW",files.selectById(20L).getStatus());assertNull(files.selectById(20L).getControlledTime());
        var task=engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult();assertNotNull(task);assertEquals("DOC_CONTROL_REVIEW",task.getTaskDefinitionKey());
        assertEquals(0,signatures.selectList().stream().filter(s->"DOC_CONTROL_REVIEW_APPROVE".equals(s.getMeaningCode())).count());
        assertNull(files.selectById(20L).getEffectiveTime());assertEquals(0,bindingRows.selectList().size());
        assertThrows(RuntimeException.class,()->finalization.retryStamp(88L,20L));assertEquals("PENDING_DOC_CONTROL_REVIEW",files.selectById(20L).getStatus());
    }
    @Test void realSignedSupervisorRejectEndsThroughOfficialListenerWithoutLaterStageSignatures()throws Exception{
        prepare(true,true);assertEquals("REJECTED",files.selectById(20L).getStatus());assertNotNull(files.selectById(20L).getRejectedTime());
        assertEquals(1,signatures.selectList().size());assertEquals("SUPERVISOR_REVIEW_REJECT",signatures.selectList().get(0).getMeaningCode());
        assertEquals(0,bindingRows.selectList().size());assertNull(files.selectById(20L).getControlledTime());
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processId).singleResult());
        assertEquals(88L,actualEnd.getActorUserId());assertEquals(BpmProcessInstanceStatusEnum.REJECT.getStatus(),actualEnd.getStatus());
    }
    @Test void lateBatchAcceptAuditFailureRollsActualRelationAndArtifactAndEngineBack()throws Exception{
        prepare(true);jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.distribution.batch.accept'");
        assertThrows(RuntimeException.class,this::saveTraining);assertNull(files.selectById(20L).getControlledTime());
        assertEquals(0,bindingRows.selectList().size());assertEquals(0,batchRows.selectList().size());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_relation_controlled_event",Integer.class));
        assertNotNull(engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").singleResult());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"WRONG_TENANT","WRONG_BUSINESS","PARTIAL_ARTIFACT"})
    void completedPublicReplayRequiresTenantBusinessAndCompleteArtifact(String conflict)throws Exception{
        prepare(true);saveTraining();var before=jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id");
        switch(conflict){case "WRONG_TENANT"->TenantContextHolder.setTenantId(2L);
            case "WRONG_BUSINESS"->actualEnd.setBusinessKey("21");
            case "PARTIAL_ARTIFACT"->jdbc.update("UPDATE dcc_controlled_file SET stamped_file_id=NULL WHERE id=20");}
        if("WRONG_BUSINESS".equals(conflict))assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(finalization,"finalizeOrdinaryApproval",files.selectById(20L),actualEnd));
        else assertThrows(RuntimeException.class,()->finalization.handleProcessInstanceStatusChanged(actualEnd));
        assertEquals(before,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"WRONG_ACTOR","CHANGED_REASON","MISSING_SIGNATURE"})
    void rejectedPublicReplayRequiresOriginalSignedNodeEvidence(String conflict)throws Exception{
        prepare(true,true);var before=jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id");
        switch(conflict){case "WRONG_ACTOR"->actualEnd.setActorUserId(99L);
            case "CHANGED_REASON"->actualEnd.setReason("different reason");
            case "MISSING_SIGNATURE"->jdbc.update("DELETE FROM dcc_controlled_file_signature WHERE controlled_file_id=20");}
        assertThrows(RuntimeException.class,()->finalization.handleProcessInstanceStatusChanged(actualEnd));
        assertEquals(before,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));assertEquals("REJECTED",files.selectById(20L).getStatus());
    }
    @Test void technicalRetryCannotUseAFailureMarkerToBypassTheActualLiveTrainingReceive()throws Exception{
        prepare(true);var before=jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id");
        jdbc.update("UPDATE dcc_controlled_file SET status='FINALIZATION_FAILED' WHERE id=20");
        var refusal=assertThrows(IllegalStateException.class,()->finalization.retryStamp(88L,20L));
        assertTrue(refusal.getMessage().contains("仍活跃"));assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));
        assertNull(files.selectById(20L).getControlledTime());assertEquals(0,bindingRows.selectList().size());assertEquals(3000,physicalIds.get());
    }
    @Test void anUnregisteredBoundDefinitionRefusalDoesNotCreateARetryableSuccessProjection()throws Exception{
        prepare(true);var before=jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id");
        doThrow(new IllegalStateException("EXECUTION_CONTRACT_UNREGISTERED_DEFINITION")).when(boundPort).resolve(any());
        assertThrows(RuntimeException.class,this::saveTraining);assertEquals(DccOfflineTrainingRecordService.STATUS,files.selectById(20L).getStatus());
        assertEquals(before,jdbc.queryForList("SELECT * FROM controlled_content_transition_audit ORDER BY id"));assertEquals(0,bindingRows.selectList().size());
    }

    @Test void presetLegalAtSubmissionExpiresBeforeDocumentReviewAndActivatesAtActualCompletionTime()throws Exception{
        usePresetDate=true;expireBeforeDocumentReview=true;submittedPresetDate=businessNow.toLocalDate();enableReadyPlatformDistribution();
        prepare(false);assertPresetActivatedNow();assertEquals(2,notificationRows.selectList().size());
        var before=jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id");finalization.handleProcessInstanceStatusChanged(actualEnd);
        assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));assertEquals(2,notificationRows.selectList().size());
    }
    @Test void presetExpiresDuringRequiredTrainingAndWaitsForActualRequesterRecord()throws Exception{
        usePresetDate=true;submittedPresetDate=businessNow.toLocalDate();enableReadyPlatformDistribution();prepare(true);
        businessNow=businessNow.plusDays(2);var training=actualTrainingGate();var waiting=files.selectById(20L);
        assertEquals(DccOfflineTrainingRecordService.STATUS,waiting.getStatus());assertNull(waiting.getControlledTime());assertNull(waiting.getEffectiveTime());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertTrue(notificationRows.selectList().isEmpty());
        assertThrows(IllegalStateException.class,()->training.requireUpload(77L,waiting));
        assertNotNull(training.requireUpload(99L,waiting));
        saveTraining();assertPresetActivatedNow();assertEquals(2,notificationRows.selectList().size());
    }
    @Test void futurePresetAfterTrainingRetainsOldDailyVersionUntilTheActualDueDay()throws Exception{
        usePresetDate=true;submittedPresetDate=businessNow.toLocalDate().plusDays(4);enableReadyPlatformDistribution();prepare(true);
        businessNow=businessNow.plusDays(1);assertNotNull(actualTrainingGate().requireUpload(99L,files.selectById(20L)));saveTraining();
        var controlled=files.selectById(20L);assertEquals("CONTROLLED_PENDING_EFFECTIVE",controlled.getStatus());assertNotNull(controlled.getControlledTime());
        assertNull(controlled.getEffectiveTime());assertNull(controlled.getActivatedTime());assertEquals(submittedPresetDate,controlled.getEffectiveDate());
        assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertTrue(batchRows.selectList().isEmpty());assertTrue(notificationRows.selectList().isEmpty());
        businessNow=submittedPresetDate.atTime(12,0);assertEquals(true,tx.execute(s->lifecycle.activateDue(20L)));
        assertEquals(businessNow,files.selectById(20L).getEffectiveTime());assertEquals(businessNow,files.selectById(20L).getActivatedTime());assertEquals(2,notificationRows.selectList().size());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"STAMP","CONTROL_AUDIT","TRAINING_RECORD","CONTROLLED_CONSUMER"})
    void expiredPresetLateFailureKeepsOldVersionAndPreviousSignaturesUntilRealRetry(String failure)throws Exception{
        usePresetDate=true;submittedPresetDate=businessNow.toLocalDate();enableReadyPlatformDistribution();prepare(true);businessNow=businessNow.plusDays(2);
        var prior=jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id");
        switch(failure){case "STAMP"->wire(finalization,"pdfStampService",new DccPdfStampService(){@Override public byte[] stamp(byte[] source)throws java.io.IOException{throw new java.io.IOException("IC33 actual stamp failure");}});
            case "CONTROL_AUDIT"->jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.controlled-file.control'");
            case "TRAINING_RECORD"->jdbc.execute("ALTER TABLE dcc_controlled_file_attachment ADD CONSTRAINT ic33_record_fail CHECK(storage_file_id<>211)");
            case "CONTROLLED_CONSUMER"->failControlled=true;}
        try{assertThrows(RuntimeException.class,this::saveTraining);
            assertEquals(prior,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));assertNull(files.selectById(20L).getControlledTime());
            assertEquals(DccOfflineTrainingRecordService.STATUS,files.selectById(20L).getStatus());assertEquals("ACTIVE",files.selectById(10L).getStatus());
            assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());assertTrue(notificationRows.selectList().isEmpty());assertTrue(batchRows.selectList().isEmpty());
            assertTrue(attachments.selectList().isEmpty());assertTrue(bindingRows.selectList().isEmpty());
        }finally{switch(failure){case "STAMP"->wire(finalization,"pdfStampService",new DccPdfStampService());case "CONTROL_AUDIT"->policy("dcc.controlled-file.control");
            case "TRAINING_RECORD"->jdbc.execute("ALTER TABLE dcc_controlled_file_attachment DROP CONSTRAINT ic33_record_fail");case "CONTROLLED_CONSUMER"->failControlled=false;}}
        saveTraining();assertPresetActivatedNow();assertEquals(2,notificationRows.selectList().size());
        assertEquals(prior,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));
    }

    private void assertPresetActivatedNow(){var saved=files.selectById(20L);assertEquals("ACTIVE",saved.getStatus());assertEquals(submittedPresetDate,saved.getEffectiveDate());
        assertEquals(businessNow,saved.getControlledTime());assertEquals(businessNow,saved.getEffectiveTime());assertEquals(businessNow,saved.getActivatedTime());
        assertNull(saved.getEffectiveSignatureId());assertEquals(approvedAt,saved.getApprovedTime());assertEquals(20L,masters.selectById(3L).getCurrentActiveControlledFileId());
        assertEquals("OBSOLETE",files.selectById(10L).getStatus());assertEquals(5,bindingRows.selectList().size());assertEquals("SENT",batchRows.selectList().get(0).getStatus());}

    @Test void actualIssuedTrainingTicketMustCarryItsDigestThroughTheRealBinderAndFinalization()throws Exception{
        usePresetDate=true;submittedPresetDate=businessNow.toLocalDate();actualTrainingTicketPath=true;enableReadyPlatformDistribution();prepare(true);
        issueActualTrainingTicket();businessNow=businessNow.plusDays(2);saveTraining();assertPresetActivatedNow();
        var ticket=trainingTicketRows.selectList().get(0);var attachment=attachments.selectList().get(0);
        assertEquals(ticket.getFileSha256(),attachment.getFileSha256());assertEquals("BOUND",ticket.getStatus());assertEquals(2,notificationRows.selectList().size());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"PHYSICAL_BYTES","LATE_GXP"})
    void actualTicketBinderAndFinalizationFailureRollBackBindingWithoutLosingUploadedTicket(String failure)throws Exception{
        usePresetDate=true;submittedPresetDate=businessNow.toLocalDate();actualTrainingTicketPath=true;enableReadyPlatformDistribution();prepare(true);
        issueActualTrainingTicket();businessNow=businessNow.plusDays(2);
        var originalSignatures=jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id");
        var originalTicket=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_temporary_file WHERE upload_ticket=?",actualTrainingTicket.uploadTicket());
        var originalBytes=bytes.get("task-owned/training.pdf");
        if("PHYSICAL_BYTES".equals(failure))corruptTrainingBytesAfterBinding=true;
        else jdbc.update("DELETE FROM gxp_audit_policy_operation WHERE operation_id='dcc.controlled-file.control'");
        try{var refused=assertThrows(RuntimeException.class,this::saveTraining);
            if("PHYSICAL_BYTES".equals(failure))assertTrue(refused.getMessage().contains("摘要"));
            assertEquals(originalTicket,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_temporary_file WHERE upload_ticket=?",actualTrainingTicket.uploadTicket()));
            assertTrue(attachments.selectList().isEmpty());assertTrue(bindingRows.selectList().isEmpty());assertTrue(notificationRows.selectList().isEmpty());
            assertEquals(DccOfflineTrainingRecordService.STATUS,files.selectById(20L).getStatus());assertNull(files.selectById(20L).getTrainingRecordFileId());
            assertEquals(10L,masters.selectById(3L).getCurrentActiveControlledFileId());
            assertNotNull(engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").singleResult());
            assertEquals(originalSignatures,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));
        }finally{corruptTrainingBytesAfterBinding=false;bytes.put("task-owned/training.pdf",originalBytes);if("LATE_GXP".equals(failure))policy("dcc.controlled-file.control");}
        saveTraining();assertPresetActivatedNow();assertEquals("BOUND",trainingTicketRows.selectList().get(0).getStatus());assertEquals(2,notificationRows.selectList().size());
        assertEquals(originalSignatures,jdbc.queryForList("SELECT * FROM dcc_controlled_file_signature ORDER BY id"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"PURPOSE","EXPIRED","ACTOR","TENANT","MALFORMED_SHA"})
    void anInvalidActualTrainingTicketNeverBindsAnAttachmentOrAdvancesTheRound(String invalid)throws Exception{
        usePresetDate=true;submittedPresetDate=businessNow.toLocalDate();actualTrainingTicketPath=true;enableReadyPlatformDistribution();prepare(true);
        issueActualTrainingTicket();
        switch(invalid){case "PURPOSE"->jdbc.update("UPDATE dcc_controlled_file_temporary_file SET purpose='SOURCE' WHERE upload_ticket=?",actualTrainingTicket.uploadTicket());
            case "EXPIRED"->jdbc.update("UPDATE dcc_controlled_file_temporary_file SET expire_time=? WHERE upload_ticket=?",LocalDateTime.now().minusMinutes(1),actualTrainingTicket.uploadTicket());
            case "ACTOR"->jdbc.update("UPDATE dcc_controlled_file_temporary_file SET uploader_id=77 WHERE upload_ticket=?",actualTrainingTicket.uploadTicket());
            case "TENANT"->jdbc.update("UPDATE dcc_controlled_file_temporary_file SET tenant_id=2 WHERE upload_ticket=?",actualTrainingTicket.uploadTicket());
            case "MALFORMED_SHA"->jdbc.update("UPDATE dcc_controlled_file_temporary_file SET file_sha256='invalid-hash' WHERE upload_ticket=?",actualTrainingTicket.uploadTicket());}
        var original=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_temporary_file WHERE upload_ticket=?",actualTrainingTicket.uploadTicket());
        assertThrows(RuntimeException.class,this::saveTraining);
        assertEquals(original,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_temporary_file WHERE upload_ticket=?",actualTrainingTicket.uploadTicket()));
        assertTrue(attachments.selectList().isEmpty());assertTrue(notificationRows.selectList().isEmpty());assertNull(files.selectById(20L).getTrainingRecordFileId());
        assertEquals(DccOfflineTrainingRecordService.STATUS,files.selectById(20L).getStatus());
    }

    private void issueActualTrainingTicket(){
        login(99L);
        var content="actual offline training record".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        tx.executeWithoutResult(s->{store(211L,"task-owned/training.pdf",content,"training.pdf");
            actualTrainingTicket=trainingTickets.createTicket(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreateCommand(
                    99L,2L,"training:"+processId,"TRAINING_RECORD",211L,"training.pdf","application/pdf",(long)content.length,content,"ic33-training-upload"));});
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(content),trainingTicketRows.selectList().get(0).getFileSha256());
        assertEquals(99L,trainingTicketRows.selectList().get(0).getUploaderId());
    }

    private DccOfflineTrainingRecordService actualTrainingGate(){
        var gate=new DccOfflineTrainingRecordService();var projects=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
        var project=new cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO();project.setId(4L);project.setTenantId(1L);when(projects.selectById(4L)).thenReturn(project);
        var permissions=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permissions.hasAnyPermissions(99L,"dcc:controlled-file:query")).thenReturn(true);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);when(categories.hasCategoryPermission(2L,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.TRAINING_RECORD)).thenReturn(true);
        var fileScope=mock(DccControlledFileAssignmentScopeService.class);when(fileScope.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        var definitions=mock(BpmProcessDefinitionService.class);when(definitions.getProcessDefinition(anyString())).thenAnswer(c->engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(c.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(c->engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
        wire(gate,"files",files,"masters",masters,"projects",projects,"versions",DccControlledFileVersionPolicy.defaultPolicy(),"boundContracts",boundPort,
                "lifecycle",lifecycle,"processes",processService,"definitions",definitions,"runtime",engine.getRuntimeService(),"users",accounts,"permissions",permissions,
                "categories",categories,"fileScope",fileScope);return gate;
    }

    private void enableReadyPlatformDistribution()throws Exception{
        for(String ddl:new org.springframework.core.io.ClassPathResource("sql/dcc_d_platform_message_fixture.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!ddl.isBlank())jdbc.execute(ddl);
        readyPlatformNotifications=true;
        jdbc.update("DELETE FROM system_notify_message");
        for(String op:List.of("dcc.distribution.batch.prepare","dcc.distribution.attempt.start","dcc.distribution.attempt.finish"))policy(op);
        var template=cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO.builder().id(330L).code("dcc_distribution").type(2).nickname("DCC系统").status(0).content("{title} {version}").params(List.of("title","version")).build();
        when(templates.getNotifyTemplateByCodeFromCache("dcc_distribution")).thenReturn(template);when(templates.formatNotifyTemplateContent(anyString(),anyMap())).thenAnswer(c->new cn.iocoder.yudao.module.system.service.notify.NotifyTemplateServiceImpl().formatNotifyTemplateContent(c.getArgument(0),c.getArgument(1)));
        var attempt=new DccDistributionDispatchAttemptService();wire(attempt,"batchMapper",batchRows,"attemptMapper",attemptRows,"messageJobMapper",messageRows,"gxpAuditService",ledger);
        var delivery=new DccControlledFileMessageDeliveryService();wire(delivery,"messageJobMapper",messageRows,"notifyMessageSendApi",platformApi,"notifyMessageService",platformMessages,
                "controlledFileMapper",files,"distributionMapper",distributionRows,"distributionRecipientMapper",recipientRows,"transactionManager",transactionManager,"dispatchAttemptService",attempt);
        var dispatch=new DccWorkflowDistributionService();wire(dispatch,"controlledFileMapper",files,"distributionMapper",distributionRows,"recipientMapper",recipientRows,"messageJobMapper",messageRows,"messageDeliveryService",delivery);
        var providers=mock(org.springframework.beans.factory.ObjectProvider.class);when(providers.orderedStream()).thenAnswer(ignored->java.util.stream.Stream.of((DccDistributionRosterSource)request->
                new DccDistributionRosterSource.Ready(new DccDistributionRosterSnapshot(1L,20L,"B/1",processId,businessNow,"IC33-TASK-POLICY",List.of(new DccDistributionRosterSnapshot.Department(31L,List.of(99L,100L)))),"IC33_TASK_SOURCE","IC33_TASK_POLICY_PROOF")));
        wire(batches,"distributionService",dispatch,"messageDeliveryService",delivery,"rosterSources",providers);
    }

    private void prepare(boolean training)throws Exception {
        prepare(training,false);
    }
    private void prepare(boolean training,boolean rejectSupervisor)throws Exception {
        var root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();assertNotNull(root);
        var xml=Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml"));
        var deployment=engine.getRepositoryService().createDeployment().tenantId("1").addString("whole-public.bpmn20.xml",xml).deploy();definition=engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        masters.insert(DccControlledFileMasterDO.builder().id(3L).tenantId(1L).categoryId(2L).dccProjectCodeId(4L).fileName("SOP.pdf").fileNumber("SOP-01").status("ACTIVE_CHAIN").build());
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).categoryId(2L).directoryId(5L).dccProjectCodeId(4L)
                .fileName("SOP.pdf").title("SOP").fileNumber("SOP-01").versionNo("B/1").sourceFileId(111L).originalFileId(111L)
                .requesterId(99L).submitterId(99L).processType("CONTROLLED_FILE").changeType("NEW")
                .status("PENDING_DOC_CONTROL_REVIEW").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).needTraining(training)
                .effectiveMode(usePresetDate?DccWorkflowDatePolicy.PRESET_DATE:DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE)
                .effectiveDate(usePresetDate?submittedPresetDate:null).approvedTime(approvedAt)
                .distributionDepartmentIdsJson("[\"31\"]")
                .preparedWorkflowContract("PUBLIC_MATRIX_NATIVE_V1").preparedProcessDefinitionId(definition.getId())
                .preparedDefinitionDigest(DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(definition.getId()))).build();
        dates.requireEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate());
        if(usePresetDate){files.insert(DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(3L).categoryId(2L).directoryId(5L)
                .sourceFileId(111L).originalFileId(111L).fileName("SOP.pdf").fileNumber("SOP-01").title("SOP").versionNo("A/1").status("ACTIVE")
                .requesterId(99L).submitterId(99L).processInstanceId("prior-round").controlledTime(businessNow.minusDays(5)).activatedTime(businessNow.minusDays(5))
                .publishedFileId(111L).stampedFileId(111L).build());
            masters.updateById(DccControlledFileMasterDO.builder().id(3L).currentActiveControlledFileId(10L).latestControlledFileId(10L).build());}
        var variables=new HashMap<String,Object>();variables.put("controlledFileId",20L);variables.put("needTraining",training);
        variables.put("PROCESS_DCC_TASK_OBLIGATION_IDS",Map.of("MATRIX_REVIEW",List.of("department-31")));
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        processId=tx.execute(s->engine.getRuntimeService().startProcessInstanceById(definition.getId(),"20",variables).getId());file.setProcessInstanceId(processId);files.insert(file);
        tx.executeWithoutResult(s->{var key=ControlledContentKey.of(1L,ControlledContentType.DCC_CONTROLLED_FILE,"3");
            Long previous=null;if(usePresetDate){var old=core.registerActiveRef(key,3L,10L,"A/1","ACTIVE",99L,"actual prior controlled version");previous=old.getId();jdbc.update("UPDATE controlled_content_version_ref SET approval_process_instance_id='prior-round' WHERE native_version_id=10");}
            core.createCandidateRef(key,3L,20L,"B/1","WORKING",previous,usePresetDate?10L:null,99L,"actual isolated candidate");
            core.transitionVersionRef(key,20L,ControlledContentCanonicalStatus.IN_REVIEW,"PENDING_DOC_CONTROL_REVIEW",ControlledContentTransitionAction.SUBMIT,99L,"actual isolated submit",processId);});
        for(var stage:List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")){
            if(usePresetDate&&expireBeforeDocumentReview&&"DOC_CONTROL_REVIEW".equals(stage))businessNow=businessNow.plusDays(2);
            var task=engine.getTaskService().createTaskQuery().processInstanceId(processId).singleResult();assertEquals(stage,task.getTaskDefinitionKey());
            engine.getTaskService().setAssignee(task.getId(),"88");
            tx.executeWithoutResult(s->{
                if(rejectSupervisor){
                    files.updateById(DccControlledFileDO.builder().id(20L).status("PENDING_SUPERVISOR_REVIEW").build());
                    routes.insert(DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).routeVersionNo(1).stageCode("SUPERVISOR_REVIEW")
                            .stageNo(0).stageOrder(0).candidateSourceType("USER").candidateSourceId(88L).candidateSourceIds("88").resolvedUserIds("88")
                            .approveMethod("ANY").requireAllApprovals(false).build());
                    var rejected=sign(task.getId(),"SUPERVISOR_REVIEW_REJECT","REJECT","actual supervisor rejection");
                    engine.getTaskService().setVariableLocal(task.getId(),"dccVerifiedSignatureId",rejected.getId());
                    engine.getTaskService().setVariableLocal(task.getId(),"dccVerifiedSignatureActorId",88L);
                    engine.getTaskService().setVariableLocal(task.getId(),"dccVerifiedSignatureAction","REJECT");
                    var actions=new BpmTaskServiceImpl();
                    var model=mock(cn.iocoder.yudao.module.bpm.service.definition.BpmModelService.class);
                    when(model.getBpmnModelByDefinitionId(anyString())).thenAnswer(c->engine.getRepositoryService().getBpmnModel(c.getArgument(0)));
                    wire(actions,"taskService",engine.getTaskService(),"runtimeService",engine.getRuntimeService(),"modelService",model,"processInstanceService",processService);
                    actions.rejectTask(88L,new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO().setId(task.getId()).setReason("actual supervisor rejection"));
                    return;
                }
                if("MATRIX_REVIEW".equals(stage)){var assigned=sign(task.getId(),"MATRIX_REVIEW_ASSIGN","ASSIGN","real department assignment");
                    assignments.insert(DccControlledFileTaskAssigneeSnapshotDO.builder().controlledFileId(20L).tenantId(1L).processInstanceId(processId).stageCode(stage).stageNo(1)
                            .departmentId(31L).leaderUserId(88L).assigneeUserId(88L).obligationId("department-31").bpmTaskId(task.getId()).assignmentSignatureId(assigned.getId()).assignedTime(assigned.getSignedAt()).build());}
                String comment="actual approved "+stage;if("MATRIX_APPROVAL".equals(stage))comment+="；批准文件负责人事实：[77,\"account-77\",\"Account 77\"]";
                if("DOC_CONTROL_REVIEW".equals(stage))lifecycle.requireReviewEffectiveSelection(files.selectById(20L));
                var signed=sign(task.getId(),stage+"_APPROVE","APPROVE",comment);
                if("MATRIX_APPROVAL".equals(stage)){files.updateById(DccControlledFileDO.builder().id(20L).fileOwnerUserId(77L).fileOwnerUsernameSnapshot("account-77").fileOwnerNicknameSnapshot("Account 77")
                        .fileOwnerSignatureId(signed.getId()).fileOwnerApprovalTaskId(task.getId()).fileOwnerProcessInstanceId(processId).fileOwnerSelectedTime(signed.getSignedAt()).build());}
                int order=List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW").indexOf(stage);
                routes.insert(DccControlledFileRouteSnapshotDO.builder().controlledFileId(20L).routeVersionNo(1).stageName(stage).stageCode(stage).stageNo(order).stageOrder(order)
                        .candidateSourceType("MATRIX_REVIEW".equals(stage)?"DEPT":"USER").candidateSourceId("MATRIX_REVIEW".equals(stage)?31L:88L)
                        .candidateSourceIds("MATRIX_REVIEW".equals(stage)?"31":"88").resolvedUserIds("88")
                        .approveMethod("MATRIX_REVIEW".equals(stage)?"ALL":"ANY").requireAllApprovals("MATRIX_REVIEW".equals(stage)).build());
                if("DOC_CONTROL_REVIEW".equals(stage))lifecycle.recordStandardizationSignatureEffect(88L,20L,signed.getId());
                engine.getTaskService().setVariableLocal(task.getId(),BpmnVariableConstants.TASK_VARIABLE_STATUS,2);
                engine.getRuntimeService().setVariable(processId,BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID,88L);
                engine.getTaskService().complete(task.getId());
                if(training&&"DOC_CONTROL_REVIEW".equals(stage))files.updateById(DccControlledFileDO.builder().id(20L).status(DccOfflineTrainingRecordService.STATUS).build());
            });
            if(rejectSupervisor)break;
        }
    }
    private void saveTraining(){login(99L);tx.executeWithoutResult(s->{
        var current=files.selectByIdAndTenantForUpdate(1L,20L);assertEquals(DccOfflineTrainingRecordService.STATUS,current.getStatus());
        if(usePresetDate)actualTrainingGate().requireUpload(99L,current);
        var content="actual offline training record".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if(!actualTrainingTicketPath)store(211L,"task-owned/training.pdf",content,"training.pdf");
        if(actualTrainingTicketPath){
            // The real ticket already committed in its upload transaction; binding must persist its hash itself.
            var binder=new DccControlledFileAttachmentServiceImpl();wire(binder,"attachmentMapper",attachments,"controlledFileMapper",files,
                    "offlineTraining",actualTrainingGate(),"users",accounts,"fileMapper",storageRows);
            var resolved=trainingTickets.resolveTrainingForBinding(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand(
                    actualTrainingTicket.uploadTicket(),99L,2L,"training:"+processId,"TRAINING_RECORD"));
            var attachmentId=binder.bindTrainingRecord(99L,20L,processId,resolved);
            assertEquals(trainingTicketRows.selectList().get(0).getFileSha256(),attachments.selectById(attachmentId).getFileSha256(),"Real binder lost the actual ticket SHA before Finalization");
            trainingTickets.markBound(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand(
                    actualTrainingTicket.uploadTicket(),99L,2L,"training:"+processId,"TRAINING_RECORD",20L));
            if(corruptTrainingBytesAfterBinding){var damaged=bytes.get("task-owned/training.pdf").clone();damaged[0]=(byte)(damaged[0]^1);bytes.put("task-owned/training.pdf",damaged);}
        }else{
        var record=DccControlledFileAttachmentDO.builder().controlledFileId(20L).storageFileId(211L).attachmentPurpose("TRAINING_RECORD")
                .applicationProcessInstanceId(processId).uploadedBy(99L).uploadedAt(LocalDateTime.now()).uploadedUsernameSnapshot("account-99").uploadedNicknameSnapshot("Account 99")
                .originalFileName("training.pdf").contentType("application/pdf").fileSize((long)content.length).fileSha256(cn.hutool.crypto.digest.DigestUtil.sha256Hex(content)).sortNo(0).build();record.setTenantId(1L);attachments.insert(record);
        }
        files.updateById(DccControlledFileDO.builder().id(20L).trainingRecordFileId(211L).status("READY_TO_PUBLISH").build());
        var execution=engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId("TRAINING").singleResult();assertNotNull(execution);engine.getRuntimeService().trigger(execution.getId());
    });}
    private DccControlledFileSignatureDO sign(String task,String meaning,String action,String reason){
        var req=DccControlledFileSignatureEvidenceCreateReq.builder().tenantId(1L).controlledFileId(20L).processInstanceId(processId).taskId(task)
                .taskActionResult("APPROVE".equals(action)?"APPROVED":"REJECT".equals(action)?"REJECTED":"ASSIGNED").meaningCode(meaning).signerUserId(88L).signerUsername("account-88").signerNickname("Account 88")
                .signerPostNames("isolated formal post").signerRoleNames("isolated formal role").signaturePurpose("actual "+meaning)
                .authorizationBasis("isolated frozen task actor").authenticationMethod("PASSWORD").signedAt(LocalDateTime.now().withNano(0)).reasonText(reason)
                .controlledCopyHashStatus("NOT_APPLICABLE").signatureImageId(1L).signatureImageVersionNo(1).signatureImageFileId(9L)
                .signatureImageSha256("a".repeat(64)).signatureImageContentType("image/png").signatureImageFileSize(3L)
                .signatureImageStatusSnapshot("ACTIVE").signatureImageVerifiedStatus("VALID").build();
        var proof=evidence.createEvidence(req);var row=new DccControlledFileSignatureDO();BeanUtils.copyProperties(proof,row);
        row.setControlledFileId(20L);row.setTaskId(task);row.setActorId(88L);row.setActorUsernameSnapshot("account-88");row.setActorNicknameSnapshot("Account 88");
        row.setActorPostNamesSnapshot(req.getSignerPostNames());row.setActorRoleNamesSnapshot(req.getSignerRoleNames());row.setSignaturePurpose(req.getSignaturePurpose());
        row.setAuthorizationBasis(req.getAuthorizationBasis());row.setAuthenticationMethod("PASSWORD");row.setSignedAt(req.getSignedAt());row.setActionType(action);row.setMeaningCode(meaning);row.setComment(reason);row.setPasswordVerified(true);
        row.setSignatureMode("PASSWORD");
        signatures.insert(row);var verified=verifier.verifySignatureEvidence(row.getId());assertEquals("VALID",verified.getVerificationStatus(),verified.getVerificationReason());return row;
    }
    private DccBoundWorkflowContractResolver.BoundExecution bound(DccControlledFileDO actual){return new DccBoundWorkflowContractResolver.BoundExecution(1L,20L,processId,7,
            actual.getProcessDefinitionKey(),definition.getId(),definition.getDeploymentId(),file.getPreparedDefinitionDigest(),"PUBLIC_MATRIX_NATIVE_V1",
            DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processId).singleResult()!=null);}
    private void store(Long id,String path,byte[] content,String name){bytes.put(path,content);storageRows.insert(FileDO.builder().id(id).configId(7L).path(path).url("task-owned://"+path).name(name).type("application/pdf").size((long)content.length).build());}
    private void login(Long id){SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(id).setTenantId(1L).setUserType(UserTypeEnum.ADMIN.getValue())
            .setInfo(Map.of("username","account-"+id,LoginUser.INFO_KEY_NICKNAME,"Account "+id)),new org.springframework.mock.web.MockHttpServletRequest());}
    private Long actor(String action){return jdbc.queryForObject("SELECT actor_id FROM controlled_content_transition_audit WHERE action=?",Long.class,action);}
    private void policy(String op){jdbc.update("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES(1,'IC24-ISOLATED',?,'SERVICE_METHOD','DccWorkflowFileStateAudit','DCC','DCC_CONTROLLED_FILE','UPDATE','REQUIRED',?,'PRESENT_TO_PRESENT','GXP_CONTROLLED_DOCUMENT','IC24-W06','W06','GXP',TRUE)",op,op.endsWith("standardization-effective")?"REQUIRED":"NOT_REQUIRED");}
    private static void wire(Object target,Object...args){for(int i=0;i<args.length;i+=2)ReflectionTestUtils.setField(target,(String)args[i],args[i+1]);}
    private static void injectIfPresent(Object target,String name,Object value){if(org.springframework.util.ReflectionUtils.findField(target.getClass(),name)!=null)ReflectionTestUtils.setField(target,name,value);}
}
