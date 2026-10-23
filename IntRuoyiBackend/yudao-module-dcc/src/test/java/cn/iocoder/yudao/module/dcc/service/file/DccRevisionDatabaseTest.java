package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckoutReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Modifier;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** H2 proves isolated transaction/locking behavior only, never MySQL character-index semantics. */
@Import({DccControlledFileRevisionServiceImpl.class, DccControlledFileNameClaimService.class,
        DccControlledFileVersionPolicy.class, DccControlledFileVersionPolicyProperties.class,DccObsoleteRetentionService.class,
        DccControlledFileAssignmentScopeService.class, DccWorkflowDatePolicy.class,
        DccApplicationExecutionContractService.class,DccLifecycleEffectiveContractResolver.class,DccExecutionDefinitionTransitions.class,
        DccBoundWorkflowContractResolver.class,DccObsoleteFormPreparationProof.class,DccObsoleteFormRuntimeStore.class,DccObsoleteFormRuntimeSnapshotMapper.class,DccObsoleteFormRuntimeObligationMapper.class,
        DccApprovalMatrixTokenService.class,DccApprovalMatrixTokenProperties.class,
        DccApplicationRoundService.class,DccWorkingApplicationDraftInitializer.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService.class,DccRevisionDatabaseTest.PreparedBeans.class})
class DccRevisionDatabaseTest extends BaseDbUnitTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class PreparedBeans {
        @org.springframework.context.annotation.Bean JdbcTemplate preparedJdbc(DataSource source){return new JdbcTemplate(source);}
    }
    @Resource private DccControlledFileRevisionService revisionService;
    @Resource private DccControlledFileNameClaimService claimService;
    @Resource private DccObsoleteRetentionService retentionService;
    @Resource private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Resource private DccWorkflowDatePolicy datePolicy;
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileCheckoutMapper checkoutMapper;
    @Resource private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Resource private DccControlledFileVersionPolicy policy;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DataSource dataSource;
    @MockBean private DccProjectAccessService projectAccessService;
    @MockBean private DccControlledFileCategoryPermissionSupport permissionSupport;
    @MockBean private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @MockBean private DccControlledFileAttachmentService attachmentService;
    @MockBean private DccControlledFileRelatedFileService relatedFileService;
    @MockBean private DccPublicUploadPlacementService publicUploadPlacementService;
    @MockBean private cn.iocoder.yudao.module.system.api.dept.DeptApi deptApi;
    @MockBean private cn.iocoder.yudao.module.system.api.permission.PermissionApi scopePermissionApi;
    @MockBean private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService bpmProcessInstanceService;
    @MockBean private cn.iocoder.yudao.module.bpm.service.task.BpmTaskService bpmTaskService;
    @MockBean private cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions;
    @MockBean private DccControlledFileApprovalRouteAssigneeResolver routeResolver;
    @MockBean private cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper forms;
    @Resource private DccApplicationExecutionContractService executionContracts;
    @Resource private DccApplicationRoundService applicationRoundService;
    @Resource private DccWorkingApplicationDraftInitializer initializer;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectApplicationSnapshotService snapshots;
    @Resource private DccExecutionDefinitionTransitions transitions;
    private static org.flowable.engine.ProcessEngine preparedEngine;
    private static java.util.Map<String,String> exactDefinitions;
    private final cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes fixtureAttributes=
            new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(java.util.List.of("CE"),null,"Y","N","N",null);
    private JdbcTemplate jdbc;
    private final LocalDateTime obsolete = LocalDateTime.of(2026,9,30,12,0);
    private final LocalDateTime deadline = obsolete.plusDays(17); // Explicit test fixture, no production default.
    private final DccControlledFilePreparedSource frozen = new DccControlledFilePreparedSource(300L,100L,"hash",true);

    @BeforeAll static void actualDefinitions() {
        preparedEngine=org.flowable.engine.ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:w08_revision_prepared_engine;DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate("true").setAsyncExecutorActivate(false).buildProcessEngine();
        exactDefinitions=new java.util.HashMap<>();
        DccW08PreparedUnitFixture.loadLegacyModels().forEach((key,model)->{
            var deployment=preparedEngine.getRepositoryService().createDeployment().tenantId("1")
                    .addBytes("actual.bpmn20.xml",new org.flowable.bpmn.converter.BpmnXMLConverter().convertToXML(model)).deploy();
            exactDefinitions.put(key,preparedEngine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult().getId());
        });
    }
    @AfterAll static void closeDefinitions(){if(preparedEngine!=null)preparedEngine.close();}

    @BeforeEach void fixtures() throws Exception {
        when(attachmentService.readOrdinaryAttachmentManifest(anyLong(),anyLong())).thenAnswer(call -> {
            Long selected=call.getArgument(1);
            var facts=java.util.List.<DccControlledFileAttachmentService.OrdinaryAttachmentFact>of();
            String canonical=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Arrays.asList("DCC_ORDINARY_ATTACHMENT_MANIFEST_V1",1L,selected,facts));
            return new DccControlledFileAttachmentService.OrdinaryAttachmentManifest(facts,cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical));
        });
        datePolicy.setZoneId("Asia/Singapore");
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
        TenantContextHolder.setTenantId(1L);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status,default_attributes_json,deleted) VALUES(5,1,'REVISION','REVISION','ENABLE',?,0)",
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(fixtureAttributes));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status,current_active_controlled_file_id,latest_controlled_file_id,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(10,1,2,3,'template','SOP-001','ACTIVE_CHAIN',20,20,5,6,'SOP-001')");
        fileMapper.insert(file(20L,"A/1","ACTIVE"));
        fileMapper.insert(file(21L,"A/1-1","WORKING"));
        when(permissionSupport.hasCategoryPermission(2L,99L,DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(true);
        when(sourceOwnershipService.createVerifiedCopy(100L)).thenReturn(frozen);
        when(sourceOwnershipService.rollbackCleanup(any())).thenAnswer(c->{var source=c.<DccControlledFilePreparedSource>getArgument(0);return (Runnable)()->sourceOwnershipService.cleanupPreparedSource(source);});
        when(definitions.getProcessDefinition(anyString())).thenAnswer(call->preparedEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(call.getArgument(0)).singleResult());
        when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(call->preparedEngine.getRepositoryService().getBpmnModel(call.getArgument(0)));
        when(bpmProcessInstanceService.getProcessInstance(anyString())).thenAnswer(call->preparedEngine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(call.getArgument(0)).includeProcessVariables().singleResult());
        when(bpmProcessInstanceService.getHistoricProcessInstance(anyString())).thenAnswer(call->preparedEngine.getHistoryService().createHistoricProcessInstanceQuery()
                .processInstanceId(call.getArgument(0)).includeProcessVariables().singleResult());
        String upload=exactDefinitions.get(DccControlledFileProcessDefinitionKeys.UPLOAD),revision=exactDefinitions.get(DccControlledFileProcessDefinitionKeys.REVISION);
        transitions.setDefinitionTransitions(java.util.List.of(new DccExecutionDefinitionTransitions.Transition(upload,
                DccLifecycleEffectiveContractResolver.definitionDigest(preparedEngine.getRepositoryService().getBpmnModel(upload)),revision,
                DccLifecycleEffectiveContractResolver.definitionDigest(preparedEngine.getRepositoryService().getBpmnModel(revision)))));
        tx().executeWithoutResult(status->{
            jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=NULL,current_active_controlled_file_id=NULL WHERE id=10");
            jdbc.update("UPDATE dcc_controlled_file SET change_type='NEW',status='WORKING',controlled_time=NULL WHERE id=20");
            initializer.initialize(99L,null,20L,fixtureAttributes);
            var bpm=preparedEngine.getRuntimeService().createProcessInstanceBuilder().processDefinitionId(upload).businessKey("20")
                    .variables(java.util.Map.of("controlledFileId",20L,"PROCESS_DCC_TASK_OBLIGATION_IDS",java.util.Map.of("MATRIX_REVIEW",java.util.List.of("W08-DECLARED-ROOT")))).start().getId();
            snapshots.submitReservedDraft(99L,5L,"UPLOAD",20L,applicationRoundService.requireDraft("UPLOAD",20L),bpm,fixtureAttributes);
            jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time='2026-10-01 12:00:00',process_instance_id=?,process_definition_key=? WHERE id=20",bpm,DccControlledFileProcessDefinitionKeys.UPLOAD);
            jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
            jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=20 WHERE id=21");
            initializer.initialize(99L,20L,21L,null);
            executionContracts.prepareLegacy(99L,fileMapper.selectById(21L),revision);
        });
    }

    @Test void outerSubmissionFailureRollsBackAllocatedRowAndCleansFrozenBody() {
        assertThrows(IllegalStateException.class, () -> tx().execute(status -> {
            revisionService.createRevision(99L,20L,21L,request("PARTIAL","outer-failure"));
            throw new IllegalStateException("approval start failed");
        }));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(20L, masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @Test void childFailureActuallyRollsBackRootInsert() {
        doThrow(new IllegalStateException("relationship failed")).when(relatedFileService).freezeCurrentRelationsForRevision(eq(99L),eq(20L),anyLong());
        assertThrows(RuntimeException.class, () -> revisionService.createRevision(99L,20L,21L,request("PARTIAL","child")));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @Test void formalFactsPersistAndReplayWithoutNewSource() {
        var result=revisionService.createRevision(99L,20L,21L,request("PARTIAL","replay"));
        var stored=fileMapper.selectById(result.getId());
        assertEquals("PARTIAL",stored.getRevisionChangeType()); assertEquals("A/1",stored.getRevisionSourceVersionNo());
        assertEquals("A/1-1",stored.getSelectedIterationVersionNo()); assertEquals(300L,stored.getSourceFileId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE master_id=10 AND source_original_file_name='SOP.pdf' AND normalized_file_number='SOP-001' AND deleted=0",Integer.class));
        assertEquals(result.getId(),revisionService.createRevision(99L,20L,21L,request("PARTIAL","replay")).getId());
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
        assertThrows(RuntimeException.class, () -> revisionService.createRevision(99L,20L,21L,request("REPLACEMENT","replay")));
    }
    private void newInitialChain() {
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,latest_controlled_file_id=NULL WHERE id=10");
        jdbc.update("UPDATE dcc_controlled_file SET status='WORKING',change_type='NEW',controlled_time=NULL WHERE master_id=10");
        jdbc.update("UPDATE dcc_controlled_file SET revision_change_type='INITIAL' WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=20,revision_base_active_controlled_file_id=NULL WHERE id=21");
        jdbc.update("UPDATE dcc_controlled_file SET previous_source_sha256='hash' WHERE id=21");
        tx().executeWithoutResult(status->{
            jdbc.update("DELETE FROM dcc_project_application_attributes WHERE application_id IN(20,21)");
            jdbc.update("DELETE FROM dcc_application_round_link WHERE application_id IN(20,21)");
            jdbc.update("UPDATE dcc_controlled_file SET process_instance_id=NULL,process_definition_key=NULL,prepared_workflow_contract=NULL,prepared_process_definition_id=NULL,prepared_definition_digest=NULL WHERE id IN(20,21)");
            initializer.initialize(99L,null,20L,fixtureAttributes);
            executionContracts.prepareLegacy(99L,fileMapper.selectById(20L),exactDefinitions.get(DccControlledFileProcessDefinitionKeys.UPLOAD));
            initializer.initialize(99L,20L,21L,null);
            executionContracts.inheritPrepared(99L,21L,20L,applicationRoundService.requireDraft("UPLOAD",20L));
        });
    }
    @Test void initialCandidatePersistsSelectedBodyAndDateWhileBothOriginalNamesRemainUnchanged() {
        newInitialChain();
        var created=revisionService.createInitialCandidate(99L,21L,request("INITIAL","first-initial"));
        assertEquals("A/1",created.getVersionNo()); assertEquals("INITIAL",created.getRevisionChangeType());
        assertEquals("A/1",fileMapper.selectById(20L).getVersionNo());
        assertEquals("A/1-1",fileMapper.selectById(21L).getVersionNo());
        assertEquals(100L,fileMapper.selectById(20L).getSourceFileId());
        assertEquals(100L,fileMapper.selectById(21L).getSourceFileId());
        assertEquals(300L,fileMapper.selectById(created.getId()).getSourceFileId());
        assertEquals(request("INITIAL","first-initial").getEffectiveDate(),fileMapper.selectById(created.getId()).getEffectiveDate());
        assertNull(masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        assertNull(masterMapper.selectById(10L).getLatestControlledFileId());
        assertEquals(created.getId(),revisionService.createInitialCandidate(99L,21L,request("INITIAL","first-initial")).getId());
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void initialCandidateConcurrentAllocationHasOneWinnerAndKeepsInitialRoleUnique() throws Exception {
        newInitialChain();
        assertEquals(1,race(i -> revisionService.createInitialCandidate(99L,21L,request("INITIAL","initial-"+i))));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE revision_change_type='INITIAL' AND selected_iteration_controlled_file_id IS NOT NULL",Integer.class));
        var duplicate=file(null,"A/1","WORKING");duplicate.setRevisionChangeType("INITIAL");duplicate.setSelectedIterationControlledFileId(21L);
        assertThrows(org.springframework.dao.DuplicateKeyException.class,() -> fileMapper.insert(duplicate));
    }
    @Test void initialCandidateOuterLateFailureRollsBackClaimCandidateAndCleansOnlyCopy() {
        newInitialChain();
        assertThrows(IllegalStateException.class,() -> tx().execute(status -> {
            revisionService.createInitialCandidate(99L,21L,request("INITIAL","late-failure"));
            throw new IllegalStateException("BPM creation failed after freeze");
        }));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        assertEquals(100L,fileMapper.selectById(21L).getSourceFileId());
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @Test void firstRealCheckinThenInitialSubmissionFreezesNewSelectedIterationWithoutBaseline() {
        newInitialChain();
        jdbc.update("DELETE FROM dcc_controlled_file WHERE id=21"); // This isolated scenario starts before the first checkin.
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("first new chain edit");
        tx().execute(status -> query.checkoutControlledFile(99L,20L,reason));
        when(sourceOwnershipService.prepareSubmissionSource(100L,false)).thenReturn(new DccControlledFilePreparedSource(301L,100L,"hash",true));
        var edit=a21ExplicitCheckinRequest();edit.setRemark("first edit");edit.setChangeDescription("new chain first checkin");
        var working=tx().execute(status -> query.checkinControlledFile(99L,20L,edit));
        assertEquals("A/1-1",working.getVersionNo());
        when(sourceOwnershipService.createVerifiedCopy(301L)).thenReturn(new DccControlledFilePreparedSource(302L,301L,"hash",true));
        var initialRequest=request("INITIAL","first-submission");
        initialRequest.setExpectedOrdinaryAttachmentManifestHash(a21EmptyManifestHash(working.getId()));
        var candidate=revisionService.createInitialCandidate(99L,working.getId(),initialRequest);
        assertEquals("A/1",candidate.getVersionNo());assertEquals(302L,candidate.getSourceFileId());
        assertNull(candidate.getRevisionSourceControlledFileId());assertNull(candidate.getRevisionBaseActiveControlledFileId());
        assertEquals("A/1-1",fileMapper.selectById(working.getId()).getVersionNo());
        assertEquals("A/1",fileMapper.selectById(20L).getVersionNo());
        assertNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
    }
    @Test void actualDatePersistsAndOwnRevisionHashRejectsChangedDateOrMissingZone() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_date='2026-10-01' WHERE id=21");
        var input=request("PARTIAL","date-replay");
        var created=revisionService.createRevision(99L,20L,21L,input);
        assertEquals(input.getEffectiveDate(),fileMapper.selectById(created.getId()).getEffectiveDate());
        assertEquals(java.time.LocalDate.of(2026,10,1),fileMapper.selectById(21L).getEffectiveDate());
        input.setEffectiveDate(input.getEffectiveDate().plusDays(1));
        assertThrows(IllegalArgumentException.class,() -> revisionService.createRevision(99L,20L,21L,input));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
        datePolicy.setZoneId(null);
        var exact = request("PARTIAL", "date-replay");
        assertEquals(created.getId(), revisionService.createRevision(99L,20L,21L,exact).getId());
        assertThrows(IllegalStateException.class, () -> revisionService.createRevision(99L,20L,21L,request("PARTIAL","missing-zone-new")));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void revisionPreparationMatchesRealAllocationAndShowsFrozenCandidateWithoutWriting() {
        var query = queryWithRealLifecycleMappers();
        when(projectAccessService.hasProjectEditorOrOwner(99L, 5L)).thenReturn(true);
        when(projectAccessService.hasProjectOwner(99L, 5L)).thenReturn(true);
        var options = tx().execute(status -> query.getRevisionOptions(99L, 20L));
        assertEquals("A/2", options.partialTarget().versionNo());
        assertEquals("B/1", options.replacementTarget().versionNo());
        assertEquals(21L, options.iterations().get(0).id());
        assertTrue(options.iterations().get(0).canPartial());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
        var created = revisionService.createRevision(99L, options.controlledBaselineId(), options.iterations().get(0).id(), request("PARTIAL", "options-submit"));
        assertEquals(options.partialTarget().versionNo(), created.getVersionNo());
        var pending = tx().execute(status -> query.getRevisionOptions(99L, 20L));
        assertEquals("FORMAL_REVISION_IN_PROGRESS", pending.lockedReason());
        assertFalse(pending.iterations().get(0).canPartial());
        assertFalse(pending.iterations().get(0).canReplacement());
        assertEquals("TARGET_ALREADY_ALLOCATED", pending.partialTarget().unavailableReason());
        assertEquals(1, pending.iterations().size());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        verify(sourceOwnershipService, times(1)).createVerifiedCopy(100L);
    }
    @Test void missingTrainingFactCannotCreateFormalCandidateOrReservation() {
        var input = request("PARTIAL", "training-choice"); input.setNeedTraining(null);
        assertThrows(IllegalArgumentException.class, () -> revisionService.createRevision(99L, 20L, 21L, input));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
        var created = revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "training-choice"));
        assertFalse(fileMapper.selectById(created.getId()).getNeedTraining());
        var changed = request("PARTIAL", "training-choice"); changed.setNeedTraining(true);
        assertThrows(IllegalArgumentException.class, () -> revisionService.createRevision(99L, 20L, 21L, changed));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        verify(sourceOwnershipService, times(1)).createVerifiedCopy(100L);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(longs = {19L})
    void selectedOriginMismatchRejectsBeforeClaimAndCandidateInTheRealDatabase(Long recordedSource) {
        jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=? WHERE id=21", recordedSource);
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "recorded-source")),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void concurrentFormalAllocationCreatesOneCandidate() throws Exception {
        int winners=race(i -> revisionService.createRevision(99L,20L,21L,request("PARTIAL","key-"+i)));
        assertEquals(1,winners);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/2'",Integer.class));
    }
    @Test void concurrentCheckoutUsesNativeMasterLockAndSingleActiveConstraint() throws Exception {
        int winners=race(i -> tx().execute(status -> {
            masterMapper.selectByIdForUpdate(10L);
            if(checkoutMapper.selectActiveByMasterId(1L,10L)!=null) throw new IllegalStateException("checked out");
            var lock=DccControlledFileCheckoutDO.builder().masterId(10L).baseIterationId(21L).actorId((long)i)
                    .reason("concurrent checkout").baseSourceSha256("hash").status("ACTIVE").build();
            lock.setTenantId(1L); checkoutMapper.insert(lock); return lock;
        }));
        assertEquals(1,winners);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE'",Integer.class));
    }
    @Test void concurrentWorkingAllocationLocksAndProducesUniqueHyphenNumbers() throws Exception {
        int winners=race(i -> tx().execute(status -> {
            masterMapper.selectByIdForUpdate(10L);
            var base=fileMapper.selectByIdAndTenantForUpdate(1L,21L);
            var version=policy.nextWorking(base,fileMapper.selectListByMasterIdForUpdate(10L));
            var next=file(null,version.display(),"WORKING"); fileMapper.insert(next); return next;
        }));
        assertEquals(2,winners);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/1-2'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE version_no='A/1-3'",Integer.class));
    }
    @Test void publicCheckoutAndFormalRevisionRaceShareTheSameMasterLock() throws Exception {
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO(); reason.setReason("same chain edit");
        int winners=race(actor -> actor==101
                ? tx().execute(status -> query.checkoutControlledFile(99L,21L,reason))
                : revisionService.createRevision(99L,20L,21L,request("PARTIAL","race")));
        assertEquals(1,winners);
        assertEquals(1,jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE') + (SELECT COUNT(*) FROM dcc_controlled_file WHERE revision_change_type='PARTIAL')",Integer.class));
    }
    @Test void formalConflictReadsLogicalActorAndDoesNotChangeOtherWorkingLock() {
        var later = file(22L, "A/1-2", "WORKING"); later.setPredecessorControlledFileId(21L);
        fileMapper.insert(later);
        var query = queryWithRealLifecycleMappers();
        var reason = new DccControlledFileCheckoutReqVO(); reason.setReason("edit another working iteration");
        tx().execute(status -> query.checkoutControlledFile(99L, 22L, reason));
        assertNull(fileMapper.selectById(21L).getCheckedOutBy());
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "locked-elsewhere")),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ALREADY_CHECKED_OUT, 99L);
        assertEquals(99L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getActorId());
        assertEquals(22L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getBaseIterationId());
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void publicCheckinChildFailureRollsBackVersionAndPreservesCheckoutLock() {
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO(); reason.setReason("edit selected body");
        tx().execute(status -> query.checkoutControlledFile(99L,21L,reason));
        var source=new DccControlledFilePreparedSource(301L,100L,"hash",true);
        when(sourceOwnershipService.prepareSubmissionSource(100L,false)).thenReturn(source);
        doThrow(new IllegalStateException("child checkin failure")).when(relatedFileService).inheritRelatedFiles(eq(21L),anyLong());
        var request=a21ExplicitCheckinRequest(); request.setChangeDescription("remark changed"); request.setRemark("new metadata");
        assertThrows(IllegalStateException.class, () -> tx().execute(status -> query.checkinControlledFile(99L,21L,request)));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(99L,fileMapper.selectById(21L).getCheckedOutBy());
        assertEquals("ACTIVE",checkoutMapper.selectActiveByMasterId(1L,10L).getStatus());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        verify(sourceOwnershipService).cleanupPreparedSource(source);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"new metadata",""})
    void publicCheckinSuccessCommitsTheClaimAndClearsOnlyItsLogicalLock(String remark) throws Exception {
        var query=queryWithRealLifecycleMappers();
        jdbc.update("UPDATE dcc_controlled_file SET remark='original remark' WHERE id=21");
        byte[] body="exact unchanged original body".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(body));
        jdbc.update("UPDATE dcc_controlled_file SET source_sha256=?,previous_source_sha256=? WHERE master_id=10",hash,hash);
        var storage=mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);
        var metadata=mock(cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper.class);
        when(metadata.selectById(100L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder()
                .id(100L).configId(28L).path("original").name("SOP.pdf").type("application/pdf").build());
        when(metadata.selectById(301L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder()
                .id(301L).configId(28L).path("isolated").name("SOP.pdf").type("application/pdf").build());
        when(storage.getFileContent(28L,"original")).thenReturn(body);
        when(storage.createFileAndReturnId(any(byte[].class),eq("SOP.pdf"),anyString(),eq("application/pdf"))).thenAnswer(call->{
            assertArrayEquals(body,call.getArgument(0));return 301L;
        });
        when(storage.getFileContent(28L,"isolated")).thenReturn(body.clone());
        var actualOwnership=new DccControlledFileSourceOwnershipService();
        ReflectionTestUtils.setField(actualOwnership,"controlledFileMapper",fileMapper);
        ReflectionTestUtils.setField(actualOwnership,"ownershipMapper",ownershipMapper);
        ReflectionTestUtils.setField(actualOwnership,"fileMapper",metadata);
        ReflectionTestUtils.setField(actualOwnership,"fileService",storage);
        ReflectionTestUtils.setField(query,"sourceOwnershipService",actualOwnership);
        ReflectionTestUtils.setField(query,"fileMapper",metadata);
        var reason=new DccControlledFileCheckoutReqVO(); reason.setReason("edit selected body");
        tx().execute(status -> query.checkoutControlledFile(99L,21L,reason));
        var request=a21ExplicitCheckinRequest(); request.setChangeDescription("remark changed"); request.setRemark(remark);
        var result=tx().execute(status -> query.checkinControlledFile(99L,21L,request));
        assertEquals("A/1-2",result.getVersionNo()); assertNull(result.getRevisionChangeType());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE source_original_file_name='SOP.pdf' AND normalized_file_number='SOP-001' AND deleted=0",Integer.class));
        assertNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
        assertNull(fileMapper.selectById(21L).getCheckedOutBy());
        assertEquals(20L,masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        var saved=fileMapper.selectById(result.getId());
        assertEquals(remark,saved.getRemark()); assertEquals(21L,saved.getPredecessorControlledFileId());
        assertEquals(hash,saved.getSourceSha256()); assertEquals(hash,saved.getPreviousSourceSha256());
        var ownership=ownershipMapper.selectByControlledFileId(1L,result.getId());
        assertEquals(100L,ownership.getOriginSourceFileId());assertEquals(301L,ownership.getSourceFileId());
        assertEquals(hash,ownership.getSourceSha256());assertEquals("CHECKIN",ownership.getOwnershipType());
        assertEquals(100L,fileMapper.selectById(21L).getSourceFileId());
        assertEquals("original remark",fileMapper.selectById(21L).getRemark());
        var evidence=checkoutMapper.selectLatestByBaseIterationId(1L,21L);
        assertEquals("CHECKED_IN",evidence.getStatus()); assertEquals(result.getId(),evidence.getCheckinIterationId());
        assertEquals(99L,evidence.getActorId()); assertNull(evidence.getCheckinUploadTicket());
        var audit=(DccControlledFileAccessAuditService)ReflectionTestUtils.getField(query,"lifecycleAuditService");
        verify(audit).recordLifecycleLog(argThat(command -> "CHECKIN".equals(command.actionType())
                && result.getId().equals(command.fileId()) && "SUCCESS".equals(command.result())
                && "remark changed".equals(command.reason())));
        assertEquals(result.getId(),tx().execute(status -> query.checkinControlledFile(99L,21L,request)).getId());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void otherUserIncludingProjectOwnerCannotTakeOverThePersistedCheckout(boolean owner) {
        var query=queryWithRealLifecycleMappers();
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("actor editing");
        tx().execute(status->query.checkoutControlledFile(99L,21L,reason));
        when(projectAccessService.hasProjectOwner(88L,5L)).thenReturn(owner);
        when(projectAccessService.hasProjectEditorOrOwner(88L,5L)).thenReturn(true);
        when(permissionSupport.hasCategoryPermission(2L,88L,DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(true);
        var before=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(jdbc.queryForList("SELECT * FROM dcc_controlled_file WHERE master_id=10 ORDER BY id"));
        var locks=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(jdbc.queryForList("SELECT * FROM dcc_controlled_file_checkout WHERE master_id=10 ORDER BY id"));
        var input=a21ExplicitCheckinRequest();input.setRemark("other edit");input.setChangeDescription("other checkin");
        var cancel=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCancelCheckoutReqVO();cancel.setReason("other cancel");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx().execute(s->query.checkinControlledFile(88L,21L,input)));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx().execute(s->query.cancelCheckoutControlledFile(88L,21L,cancel)));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx().execute(s->query.checkoutControlledFile(88L,21L,reason)));
        assertEquals(before,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(jdbc.queryForList("SELECT * FROM dcc_controlled_file WHERE master_id=10 ORDER BY id")));
        assertEquals(locks,cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(jdbc.queryForList("SELECT * FROM dcc_controlled_file_checkout WHERE master_id=10 ORDER BY id")));
        tx().execute(s->query.cancelCheckoutControlledFile(99L,21L,cancel));
        assertNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
        verify(sourceOwnershipService,never()).prepareSubmissionSource(anyLong(),anyBoolean());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"Z/1,REPLACEMENT,AA/1", "Z/9,PARTIAL,AA/1", "AZ/9,PARTIAL,BA/1", "ZZ/9,PARTIAL,AAA/1"})
    void actualRevisionCallerAllocatesConfirmedAlphabeticVersion(String baseline,String intent,String target) {
        String revision=baseline.split("/")[0]; int iteration=Integer.parseInt(baseline.split("/")[1]);
        jdbc.update("UPDATE dcc_controlled_file SET version_no=?,revision_code=?,iteration_no=? WHERE id=20",baseline,revision,iteration);
        jdbc.update("UPDATE dcc_controlled_file SET version_no=?,revision_code=?,iteration_no=? WHERE id=21",baseline+"-1",revision,iteration);
        when(projectAccessService.hasProjectEditorOrOwner(99L,5L)).thenReturn(true);
        when(projectAccessService.hasProjectOwner(99L,5L)).thenReturn(true);
        var input=request(intent,"alphabetic-revision");
        var result=revisionService.createRevision(99L,20L,21L,input);
        assertEquals(target,result.getVersionNo()); assertEquals(intent,result.getRevisionChangeType());
        assertEquals(20L,result.getRevisionSourceControlledFileId()); assertEquals(21L,result.getSelectedIterationControlledFileId());
        assertEquals(target.split("/")[0],fileMapper.selectById(result.getId()).getRevisionCode());
        assertEquals(20L,masterMapper.selectById(10L).getCurrentActiveControlledFileId());
        assertEquals(result.getId(),revisionService.createRevision(99L,20L,21L,input).getId());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"version", "missing", "obsolete", "sourceName", "missingOrigin"})
    void checkinOriginFailurePreservesRealCheckoutAndDoesNotReserveOrCopy(String invalid) {
        var query = queryWithRealLifecycleMappers();
        var checkout = new DccControlledFileCheckoutReqVO(); checkout.setReason("working revision edit");
        tx().execute(status -> query.checkoutControlledFile(99L, 21L, checkout));
        jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION' WHERE id=21");
        switch (invalid) {
            case "version" -> jdbc.update("UPDATE dcc_controlled_file SET version_no='A/2-1' WHERE id=21");
            case "missing" -> jdbc.update("UPDATE dcc_controlled_file SET deleted=1 WHERE id=20");
            case "obsolete" -> jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
            case "sourceName" -> jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name='other.pdf' WHERE id=20");
            case "missingOrigin" -> jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=NULL WHERE id=21");
        }
        var input = a21ExplicitCheckinRequest(); input.setChangeDescription("actual origin guard");
        input.setRemark("metadata change");
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> tx().execute(status -> query.checkinControlledFile(99L, 21L, input)),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        assertEquals(99L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getActorId());
        assertEquals(21L, checkoutMapper.selectActiveByMasterIdForRead(1L, 10L).getBaseIterationId());
        assertEquals(99L, fileMapper.selectById(21L).getCheckedOutBy());
        verify(sourceOwnershipService, never()).prepareSubmissionSource(anyLong(), anyBoolean());
    }
    @Test void concurrentSameNameHasOneOwner() throws Exception {
        additionalMaster(101L); additionalMaster(102L);
        assertEquals(1,race(i -> { claimService.claimIdentity(1L,"SOP.pdf",(long)i,6L,"SOP-001",(long)i); return true; }));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE deleted=0",Integer.class));
    }
    @Test void exactBinaryIdentityAndNumberGuardUseIndependentKeys() {
        for (long id=11;id<=15;id++) additionalMaster(id);
        jdbc.update("UPDATE dcc_controlled_file_master SET dcc_project_code_id=id-4 WHERE id BETWEEN 11 AND 13");
        claimService.claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,20L);
        claimService.claimIdentity(1L,"sop.pdf",7L,6L,"SOP-001",11L);
        claimService.claimIdentity(1L,"SOP.PDF",8L,6L,"SOP-001",12L);
        claimService.claimIdentity(1L,"SOP.docx",9L,6L,"SOP-001",13L);
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"SOP.pdf",99L,6L,"different",14L));
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"new.pdf",5L,6L,"SOP-001",15L));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
    }
    @Test void retentionRejectsNewChainAndAllowsSameChain() {
        additionalMaster(11L);
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline.minusSeconds(1)));
        claimService.claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,20L);
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-002",11L));
        assertThrows(RuntimeException.class, () -> claimService.claimIdentity(1L,"new.pdf",5L,6L,"SOP-001",11L));
    }
    @Test void retainedNumberBlocksOtherProjectAndTypeEvenWhenOriginalNameChanges() {
        additionalMaster(100L);
        reserve();
        claimService.retainObsoleteIdentity(1L,10L,LocalDateTime.now(),LocalDateTime.now().plusYears(20));
        assertThrows(RuntimeException.class,()->tx().execute(s->{claimService.claimIdentity(1L,"Different.pdf",100L,6L,"SOP-001",100L);return null;}));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
        claimService.claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,20L);
    }
    @Test void retainedNumberReadPreflightBlocksNewChainWithoutLockingOrWriting() {
        reserve();
        claimService.retainObsoleteIdentity(1L,10L,LocalDateTime.now(),LocalDateTime.now().plusYears(20));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->
            ReflectionTestUtils.invokeMethod(claimService,"requireNumberAvailableForNewFile","SOP-001"));
        ReflectionTestUtils.invokeMethod(claimService,"requireNumberAvailableForNewFile","NEW-001");
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
    }
    @Test void expiredRetentionCannotReleaseStillActiveOrWorkingChain() {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline));
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline));
    }
    @Test void expiredUnusedIdentityReleasesBothKeysWithoutDeletingHistory() {
        additionalMaster(11L);
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE'");
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,status='OBSOLETE_CHAIN' WHERE id=10");
        assertTrue(claimService.releaseExpiredIdentity(1L,10L,deadline));
        claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-001",11L);
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
    }
    @Test void stillAssignedExecutionPointerCannotBeReleasedEvenWithTerminalRows() {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE'");
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE deleted=0",Integer.class));
    }
    @Test void replayCannotShortenRetentionAndMissingDeadlineIsRejected() {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        claimService.retainObsoleteIdentity(1L,10L,obsolete,obsolete.plusDays(1));
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,deadline.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> claimService.retainObsoleteIdentity(1L,10L,obsolete,null));
        assertThrows(IllegalArgumentException.class, () -> claimService.retainObsoleteIdentity(1L,10L,obsolete,obsolete));
    }
    @Test void unresolvedHistoricalSourceIdentityBlocksNewClaims() {
        additionalMaster(11L);
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(tenant_id,master_id,normalized_name) VALUES(1,10,'legacy-template')");
        assertThrows(IllegalStateException.class, () -> claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-001",11L));
    }
    @Test void wrongTenantCannotReserveOrReleaseIdentity() {
        assertThrows(IllegalArgumentException.class, () -> claimService.claimIdentity(2L,"SOP.pdf",5L,6L,"SOP-001",10L));
        assertThrows(IllegalArgumentException.class, () -> claimService.releaseExpiredIdentity(2L,10L,deadline));
    }
    @Test void concurrentSameChainReuseAndExpiryReleaseKeepInUseIdentityReserved() throws Exception {
        reserve(); claimService.retainObsoleteIdentity(1L,10L,obsolete,deadline);
        jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE'");
        jdbc.update("UPDATE dcc_controlled_file_master SET current_active_controlled_file_id=NULL,status='OBSOLETE_CHAIN' WHERE id=10");
        assertEquals(2,race(actor -> actor==101 ? tx().execute(status -> {
            masterMapper.selectByIdForUpdate(10L);
            claimService.claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,20L);
            fileMapper.insert(file(null,"A/2","ACTIVE")); return true;
        }) : claimService.releaseExpiredIdentity(1L,10L,deadline)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE deleted=0",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file WHERE status='ACTIVE'",Integer.class));
    }
    @Test void confirmedTwentyYearServiceUsesRealClaimAndRollsBackWithCaller() {
        reserve();
        assertThrows(IllegalStateException.class,() -> tx().execute(status -> {
            retentionService.retain(1L,10L,obsolete);throw new IllegalStateException("obsolete approval failed");
        }));
        assertNull(jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE master_id=10",java.sql.Timestamp.class));
        tx().execute(status -> {retentionService.retain(1L,10L,obsolete);return null;});
        assertEquals(obsolete.plusYears(20),jdbc.queryForObject("SELECT retain_until FROM dcc_controlled_file_name_claim WHERE master_id=10",java.sql.Timestamp.class).toLocalDateTime());
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,obsolete.plusYears(20).minusSeconds(1)));
        assertFalse(claimService.releaseExpiredIdentity(1L,10L,obsolete.plusYears(20)));
    }
    @Test void actualScopeServiceStopsFormalAndCheckoutBeforeWritingInMemoryDatabase() {
        when(scopePermissionApi.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(true);
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> revisionService.createRevision(99L, 20L, 21L, request("PARTIAL", "out-of-scope")),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        var query = queryWithRealLifecycleMappers();
        var checkout = new DccControlledFileCheckoutReqVO(); checkout.setReason("outside assigned scope");
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> tx().execute(status -> query.checkoutControlledFile(99L, 20L, checkout)),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout", Integer.class));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
        verify(sourceOwnershipService, never()).prepareSubmissionSource(anyLong(), anyBoolean());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"CHECKOUT,project", "CHECKOUT,taxonomy", "CHECKOUT,number",
            "CHECKIN,project", "CHECKIN,taxonomy", "CHECKIN,number"})
    void realLifecycleRequiresFormalMasterIdentityAndOwnerCanStillCancel(String action, String changed) {
        var query = queryWithRealLifecycleMappers();
        var checkout = new DccControlledFileCheckoutReqVO(); checkout.setReason("edit working iteration");
        if ("CHECKIN".equals(action)) tx().execute(status -> query.checkoutControlledFile(99L, 21L, checkout));
        switch (changed) {
            case "project" -> jdbc.update("UPDATE dcc_controlled_file SET dcc_project_code_id=55 WHERE id=21");
            case "taxonomy" -> jdbc.update("UPDATE dcc_controlled_file SET file_type_taxonomy_id=66 WHERE id=21");
            case "number" -> jdbc.update("UPDATE dcc_controlled_file SET file_number='OTHER-001' WHERE id=21");
        }
        if ("CHECKOUT".equals(action)) {
            cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                    () -> tx().execute(status -> query.checkoutControlledFile(99L, 21L, checkout)),
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKOUT_NOT_ALLOWED);
        } else {
            var request = a21ExplicitCheckinRequest(); request.setChangeDescription("metadata edit");
            request.setRemark("changed metadata");
            cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                    () -> tx().execute(status -> query.checkinControlledFile(99L, 21L, request)),
                    cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        }
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim", Integer.class));
        assertEquals("CHECKIN".equals(action) ? 1 : 0,
                jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE'", Integer.class));
        verify(sourceOwnershipService, never()).prepareSubmissionSource(anyLong(), anyBoolean());
        if ("CHECKIN".equals(action)) {
            var cancel = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCancelCheckoutReqVO();
            cancel.setReason("abandon inconsistent working identity");
            tx().execute(status -> query.cancelCheckoutControlledFile(99L, 21L, cancel));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE status='ACTIVE'", Integer.class));
            assertNull(fileMapper.selectById(21L).getCheckedOutBy());
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file", Integer.class));
        }
    }

    @Test void legacyRevisionUsesVerifiedReadProjectionAndSuccessfulCandidateNeverBackfillsOldRows() {
        installLegacyScope();
        when(sourceOwnershipService.createVerifiedCopy(100L)).thenReturn(new DccControlledFilePreparedSource(300L,100L,"a".repeat(64),true));
        var candidate=revisionService.createRevision(99L,20L,21L,request("PARTIAL","legacy-body"));
        assertEquals("A/2",candidate.getVersionNo());assertEquals("SOP.pdf",candidate.getSourceOriginalFileName());
        assertNull(fileMapper.selectById(20L).getSourceOriginalFileName());assertNull(fileMapper.selectById(21L).getSourceOriginalFileName());
        assertNull(jdbc.queryForObject("SELECT source_original_file_name FROM dcc_controlled_file_name_claim WHERE master_id=10",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim",Integer.class));
    }
    @Test void legacyPublicCheckinAllocatesModernWorkingBodyAndNeverWritesOldSourceNames() {
        installLegacyScope();var query=queryWithRealLifecycleMappers();
        var historyBefore=legacyOriginalRow(20L);
        var sourceBusinessBefore=legacyOriginalRow(21L);sourceBusinessBefore.remove("update_time");sourceBusinessBefore.remove("updater");
        when(sourceOwnershipService.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"a".repeat(64),false));
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("edit verified historic body");tx().execute(s->query.checkoutControlledFile(99L,21L,reason));
        when(sourceOwnershipService.prepareSubmissionSource(100L,false)).thenReturn(new DccControlledFilePreparedSource(301L,100L,"a".repeat(64),true));
        var input=a21ExplicitCheckinRequest();input.setChangeDescription("verified selected version");input.setRemark("updated metadata");
        var result=tx().execute(s->query.checkinControlledFile(99L,21L,input));
        assertEquals("A/1-2",result.getVersionNo());assertEquals("SOP.pdf",fileMapper.selectById(result.getId()).getSourceOriginalFileName());
        assertNull(fileMapper.selectById(20L).getSourceOriginalFileName());assertNull(fileMapper.selectById(21L).getSourceOriginalFileName());
        assertNull(jdbc.queryForObject("SELECT source_original_file_name FROM dcc_controlled_file_name_claim WHERE master_id=10",String.class));
        assertNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
        assertEquals(historyBefore,legacyOriginalRow(20L));
        var sourceBusinessAfter=legacyOriginalRow(21L);sourceBusinessAfter.remove("update_time");sourceBusinessAfter.remove("updater");
        assertEquals(sourceBusinessBefore,sourceBusinessAfter);
    }
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"absent","wrong-hash"})
    void actualLegacyCheckinRejectsMissingOrConflictingVerificationEvidenceWithoutRepair(String invalid) {
        installLegacyScope();var query=queryWithRealLifecycleMappers();
        when(sourceOwnershipService.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"a".repeat(64),false));
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("legacy proof negative");
        tx().execute(status->query.checkoutControlledFile(99L,21L,reason));
        var before=legacyOriginalRow(21L);
        if("absent".equals(invalid))jdbc.update("DELETE FROM dcc_legacy_source_name_evidence WHERE controlled_file_id=21");
        else jdbc.update("UPDATE dcc_legacy_source_name_evidence SET actual_sha256=? WHERE controlled_file_id=21","c".repeat(64));
        var input=a21ExplicitCheckinRequest();input.setChangeDescription("legacy proof negative");input.setRemark("updated metadata");
        assertThrows(IllegalStateException.class,()->tx().execute(status->query.checkinControlledFile(99L,21L,input)));
        assertEquals(before,legacyOriginalRow(21L));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertNotNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));
        verify(sourceOwnershipService,never()).prepareSubmissionSource(anyLong(),anyBoolean());
    }
    @Test void actualLegacyProjectionAndPreparedBodyRollbackAfterLateRelationFailure() {
        installLegacyScope();var query=queryWithRealLifecycleMappers();
        when(sourceOwnershipService.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"a".repeat(64),false));
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("legacy projection late rollback");
        tx().execute(status->query.checkoutControlledFile(99L,21L,reason));var before=legacyOriginalRow(21L);
        var isolated=new DccControlledFilePreparedSource(301L,100L,"a".repeat(64),true);
        when(sourceOwnershipService.prepareSubmissionSource(100L,false)).thenReturn(isolated);
        doThrow(new IllegalStateException("actual late legacy relation failure")).when(relatedFileService).inheritRelatedFiles(eq(21L),anyLong());
        var input=a21ExplicitCheckinRequest();input.setChangeDescription("legacy projection late rollback");input.setRemark("updated metadata");
        var failure=assertThrows(IllegalStateException.class,()->tx().execute(status->query.checkinControlledFile(99L,21L,input)));
        assertEquals("actual late legacy relation failure",failure.getMessage());assertEquals(before,legacyOriginalRow(21L));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertNotNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));assertNull(fileMapper.selectById(20L).getSourceOriginalFileName());
        assertNull(jdbc.queryForObject("SELECT source_original_file_name FROM dcc_controlled_file_name_claim WHERE master_id=10",String.class));
        verify(sourceOwnershipService,times(1)).cleanupPreparedSource(isolated);
    }
    private java.util.Map<String,Object> legacyOriginalRow(Long id) {
        var row=jdbc.queryForMap("SELECT * FROM dcc_controlled_file WHERE id=?",id);
        row.replaceAll((key,value)->value instanceof byte[] bytes?java.util.HexFormat.of().formatHex(bytes):value);return row;
    }
    void installLegacyScope() {
        jdbc.update("UPDATE dcc_controlled_file SET source_original_file_name=NULL,source_sha256=? WHERE master_id=10","a".repeat(64));
        jdbc.update("INSERT INTO infra_file(id,config_id,name,path,size) VALUES(100,28,'SOP.pdf','sealed/body',4)");
        jdbc.update("INSERT INTO infra_file_config(id,storage,config) VALUES(28,20,?)","{\"endpoint\":\"http://127.0.0.1:9000\",\"bucket\":\"originals\",\"region\":\"us-east-1\",\"enablePathStyleAccess\":true}");
        jdbc.update("INSERT INTO dcc_controlled_file_name_claim(id,tenant_id,master_id,normalized_name) VALUES(7,1,10,'old-template')");
        jdbc.update("INSERT INTO dcc_legacy_source_name_scope(id,tenant_id,scope_id,manifest_sha256,facts_sha256,bytes_receipt_sha256,user_decision_sha256,scope_identity_sha256,status,claim_count,version_count,source_count,edge_count,name_count,verified_at,activated_at,actor_id,reason,request_id) VALUES(1,1,'legacy',?,?,?,?,?,'VERIFIED',1,2,1,1,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,99,'verified historic files','legacy')","b".repeat(64),"c".repeat(64),"d".repeat(64),"e".repeat(64),"f".repeat(64));
        for(long id:new long[]{20,21})jdbc.update("INSERT INTO dcc_legacy_source_name_evidence(tenant_id,verification_scope_id,legacy_claim_id,legacy_master_id,controlled_file_id,source_file_id,config_id,source_path,storage_type,storage_endpoint,storage_bucket,storage_region,storage_path_style,version_no,source_original_file_name,expected_sha256,actual_sha256,expected_size,actual_size,bytes_status,expected_version_count,claim_normalized_name,master_project_id,master_leaf_id,master_number,metadata_identity_sha256,preimage_sha256,proof_row_sha256) VALUES(1,1,7,10,?,100,28,'sealed/body',20,'http://127.0.0.1:9000','originals','us-east-1',1,?,'SOP.pdf',?,?,4,4,'MATCH',2,'old-template',5,6,'SOP-001',?,?,?)",id,id==20?"A/1":"A/1-1","a".repeat(64),"a".repeat(64),"b".repeat(64),"c".repeat(64),"d".repeat(64));
        jdbc.update("INSERT INTO dcc_source_name_reservation(tenant_id,source_original_file_name,reservation_kind,verification_scope_id,generation,active) VALUES(1,'SOP.pdf','LEGACY_GROUP',1,1,1)");
    }
    @AfterEach void cleanLegacyStorage() {
        jdbc.update("DELETE FROM dcc_controlled_file_source_ownership WHERE tenant_id=1 AND source_file_id=301 AND origin_source_file_id=100 AND ownership_type='CHECKIN'");
        jdbc.update("DELETE FROM infra_file WHERE id=100");jdbc.update("DELETE FROM infra_file_config WHERE id=28");
    }
    private void reserve() { claimService.claimIdentity(1L,"SOP.pdf",5L,6L,"SOP-001",10L); }
    private void additionalMaster(Long id) {
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,status,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number) VALUES(?,1,2,3,?,'SOP-001','ACTIVE_CHAIN',?,6,'SOP-001')",id,"master-"+id,id>=100?id:5L);
    }
    private DccControlledFileQueryServiceImpl queryWithRealLifecycleMappers() {
        var query=new DccControlledFileQueryServiceImpl();
        for (var field:DccControlledFileQueryServiceImpl.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && (field.getAnnotation(Resource.class)!=null
                    || field.getAnnotation(org.springframework.beans.factory.annotation.Autowired.class)!=null)) {
                ReflectionTestUtils.setField(query,field.getName(),mock(field.getType()));
            }
        }
        ReflectionTestUtils.setField(query,"controlledFileMapper",fileMapper);
        ReflectionTestUtils.setField(query,"controlledFileMasterMapper",masterMapper);
        var projects=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
        var project=cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO.builder()
                .id(5L).status("ENABLE").build();project.setTenantId(1L);
        when(projects.selectByIdForUpdate(5L)).thenReturn(project);
        ReflectionTestUtils.setField(query,"projectCodeMapper",projects);
        ReflectionTestUtils.setField(query,"checkoutMapper",checkoutMapper);
        ReflectionTestUtils.setField(query,"versionPolicy",policy);
        ReflectionTestUtils.setField(query,"sourceOwnershipService",sourceOwnershipService);
        ReflectionTestUtils.setField(query,"relatedFileService",relatedFileService);
        ReflectionTestUtils.setField(query,"permissionSupport",permissionSupport);
        ReflectionTestUtils.setField(query,"projectAccessService",projectAccessService);
        ReflectionTestUtils.setField(query,"nameClaimService",claimService);
        ReflectionTestUtils.setField(query,"assignmentScopeService",assignmentScopeService);
        ReflectionTestUtils.setField(query,"applicationRoundService",applicationRoundService);
        ReflectionTestUtils.setField(query,"executionContracts",executionContracts);
        ReflectionTestUtils.setField(query,"workingApplicationDraftInitializer",initializer);
        ReflectionTestUtils.setField(query,"attachmentService",attachmentService);
        var detailGuard=new DccControlledFileDetailAuthorizationGuard();
        for(var field:detailGuard.getClass().getDeclaredFields())if(field.getAnnotation(jakarta.annotation.Resource.class)!=null)
            ReflectionTestUtils.setField(detailGuard,field.getName(),mock(field.getType()));
        ReflectionTestUtils.setField(detailGuard,"assignmentScopeService",assignmentScopeService);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",detailGuard);
        ReflectionTestUtils.setField(query,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
        var directory=mock(DccDirectoryAccessPermissionService.class);
        when(directory.hasDirectoryManagementPermission(99L)).thenReturn(true);
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directory);
        when(sourceOwnershipService.inspectSource(100L)).thenReturn(new DccControlledFilePreparedSource(100L,100L,"hash",false));
        return query;
    }
    private TransactionTemplate tx() { return new TransactionTemplate(transactionManager); }
    @Test void newSignatureModeClearsPreviousFactsAndPersistsIndependentDistributionSnapshot() {
        jdbc.update("UPDATE dcc_controlled_file SET effective_mode='PRESET_DATE',effective_date='2026-10-01',effective_time='2026-10-01 12:00:00',effective_signature_id=777,effective_process_instance_id='old-round',distribution_department_ids_json='[\"101\"]' WHERE id=21");
        var input=request("PARTIAL","ic05-signature");input.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");input.setEffectiveDate(null);
        input.setSelectedDistributionDepartmentIds(java.util.List.of(9007199254740993L,201L));
        var saved=revisionService.createRevision(99L,20L,21L,input);
        var actual=fileMapper.selectById(saved.getId());
        assertEquals("AFTER_STANDARDIZATION_SIGNATURE",actual.getEffectiveMode());assertNull(actual.getEffectiveDate());
        assertNull(actual.getEffectiveTime());assertNull(actual.getEffectiveSignatureId());assertNull(actual.getEffectiveProcessInstanceId());
        assertEquals("[\"201\",\"9007199254740993\"]",actual.getDistributionDepartmentIdsJson());
        assertEquals("old-round",fileMapper.selectById(21L).getEffectiveProcessInstanceId());
        input.setSelectedDistributionDepartmentIds(java.util.List.of(201L,9007199254740993L));
        assertEquals(saved.getId(),revisionService.createRevision(99L,20L,21L,input).getId());
        input.setSelectedDistributionDepartmentIds(java.util.List.of(202L));
        assertThrows(IllegalArgumentException.class,()->revisionService.createRevision(99L,20L,21L,input));
    }

    @Test void invalidDistributionDepartmentFailsBeforeCandidateOrBodyCopy() {
        var input=request("PARTIAL","ic05-invalid-dept");
        doThrow(new IllegalArgumentException("department disabled")).when(deptApi).validateDeptList(input.getSelectedDistributionDepartmentIds());
        assertThrows(IllegalArgumentException.class,()->revisionService.createRevision(99L,20L,21L,input));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
    }

    private DccControlledFileSubmitIterationReqVO request(String intent,String key) {
        var request=new DccControlledFileSubmitIterationReqVO(); request.setRevisionChangeType(intent);
        request.setIdempotencyKey(key); request.setChangeDescription("selected body"); request.setNeedTraining(false);
        request.setEffectiveDate(java.time.LocalDate.of(2099,12,20));
        request.setEffectiveMode("PRESET_DATE");
        request.setSelectedDistributionDepartmentIds(java.util.List.of(201L, 202L));
        boolean initial="INITIAL".equals(intent);String preparedKey=initial?DccControlledFileProcessDefinitionKeys.UPLOAD:DccControlledFileProcessDefinitionKeys.REVISION;
        request.setExecutionContract(DccApplicationExecutionContractService.LEGACY);request.setExpectedProcessDefinitionId(exactDefinitions.get(preparedKey));
        request.setExpectedDefinitionDigest(DccLifecycleEffectiveContractResolver.definitionDigest(preparedEngine.getRepositoryService().getBpmnModel(exactDefinitions.get(preparedKey))));
        request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(
                java.util.List.of("CE"), null, "Y", "N", "N", null));
        request.setSelectedSignoffDepartmentIds(java.util.List.of(101L, 102L));
        request.setExpectedOrdinaryAttachmentIds(java.util.List.of());
        request.setExpectedOrdinaryAttachmentManifestHash(a21EmptyManifestHash(21L));
        return request;
    }
    private static String a21EmptyManifestHash(Long selected) {
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.Arrays.asList("DCC_ORDINARY_ATTACHMENT_MANIFEST_V1",1L,selected,java.util.List.of())));
    }
    private DccControlledFileDO file(Long id,String version,String status) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L)
                .dccProjectCodeId(5L).fileTypeTaxonomyId(6L).fileNumber("SOP-001").fileName("template")
                .sourceOriginalFileName("SOP.pdf").sourceFileId(100L).originalFileId(100L).title("SOP").sourceSha256("hash")
                .requesterId(99L).submitterId(99L).versionNo(version).status(status)
                .changeType(version.contains("-")?"REVISION":"NEW")
                .revisionBaseActiveControlledFileId(version.contains("-") ? 20L : null)
                .controlledTime("WORKING".equals(status) ? null : java.time.LocalDateTime.of(2026,10,1,12,0)).build();
    }
    private interface Operation { Object call(int actor) throws Exception; }
    private int race(Operation operation) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1); var winners=new AtomicInteger();
        try {
            var first=pool.submit(() -> run(start,winners,operation,101));
            var second=pool.submit(() -> run(start,winners,operation,102));
            start.countDown(); first.get(15,TimeUnit.SECONDS); second.get(15,TimeUnit.SECONDS);
            return winners.get();
        } finally { pool.shutdownNow(); }
    }
    private void run(CountDownLatch start,AtomicInteger winners,Operation operation,int actor) {
        TenantContextHolder.setTenantId(1L);
        try { start.await(); operation.call(actor); winners.incrementAndGet(); }
        catch (cn.iocoder.yudao.framework.common.exception.ServiceException | IllegalArgumentException | IllegalStateException conflict) {
            // Expected losing business request; database/environment failures are not accepted as conflicts.
        } catch (Exception failure) { throw new RuntimeException(failure); }
        finally { TenantContextHolder.clear(); }
    }

    /** A21 fixture: no selected attachment is an explicit read/ready empty list. */
    private static DccControlledFileCheckinReqVO a21ExplicitCheckinRequest() {
        var input = new DccControlledFileCheckinReqVO();
        input.setNeedTraining(false);
        input.setAttachmentUploadTickets(java.util.List.of());
        return input;
    }
}
