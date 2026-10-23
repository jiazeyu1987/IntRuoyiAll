package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileCheckoutMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** W08 consumers of the actual W05 preparation port; isolated H2 and real immutable Flowable graphs. */
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:w08_prepared_creation;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=5000")
class DccW08PreparedCreationDatabaseTest extends DccPreparedExecutionInheritanceDatabaseTest {
    private static final String HASH = "a".repeat(64);
    private DccControlledFileSourceOwnershipService storage;
    private DccPublicUploadPlacementService placement;

    private <T> T isolatedPorts(T service) {
        for (var field : service.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    && (field.isAnnotationPresent(Resource.class) || field.isAnnotationPresent(Autowired.class)))
                ReflectionTestUtils.setField(service, field.getName(), mock(field.getType()));
        }
        return service;
    }

    private void portIfPresent(Object service, String name, Object value) {
        if (Arrays.stream(service.getClass().getDeclaredFields()).anyMatch(field -> field.getName().equals(name)))
            wire(service, name, value);
    }

    @Resource DccControlledFileCheckoutMapper checkoutMapper;
    @Resource cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper projects;

    private DccControlledFileQueryServiceImpl actualQuery() {
        if (!mockingDetails(contracts).isSpy()) contracts=spy(contracts);
        var query = isolatedPorts(new DccControlledFileQueryServiceImpl());
        storage = mock(DccControlledFileSourceOwnershipService.class);
        placement = mock(DccPublicUploadPlacementService.class);
        var scope = mock(DccControlledFileAssignmentScopeService.class);
        when(scope.isWithinAssignedFileScope(99L,20L)).thenReturn(true);
        when(scope.isWithinAssignedFileScope(eq(99L),argThat(id -> id != null && id > 20))).thenReturn(true);
        var names = mock(DccControlledFileNameClaimService.class);
        when(names.requireSourceName(any(DccControlledFileDO.class)))
                .thenAnswer(call -> call.<DccControlledFileDO>getArgument(0).getSourceOriginalFileName());
        when(storage.inspectSource(anyLong())).thenAnswer(call -> new DccControlledFilePreparedSource(call.getArgument(0),call.getArgument(0),HASH,false));
        when(storage.prepareSubmissionSource(anyLong(),eq(false)))
                .thenAnswer(call -> new DccControlledFilePreparedSource(call.<Long>getArgument(0)+1000,call.getArgument(0),HASH,true));
        when(storage.rollbackCleanup(any())).thenAnswer(call -> {
            DccControlledFilePreparedSource copy=call.getArgument(0);
            return (Runnable) () -> storage.cleanupPreparedSource(copy);
        });
        wire(query,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"checkoutMapper",checkoutMapper,
                "projectCodeMapper",projects,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),
                "sourceOwnershipService",storage,"publicUploadPlacementService",placement,
                "permissionSupport",permissions,"projectAccessService",access,"nameClaimService",names,
                "assignmentScopeService",scope,"workingApplicationDraftInitializer",initializer);
        portIfPresent(query,"applicationRoundService",rounds);
        portIfPresent(query,"executionContracts",contracts);
        portIfPresent(query,"attachmentService",a21EmptyAttachmentPort());
        a21WireCurrentReadProjection(query);
        return query;
    }

    private DccControlledFileRevisionServiceImpl actualRevision() {
        if (!mockingDetails(contracts).isSpy()) contracts=spy(contracts);
        var revision = isolatedPorts(new DccControlledFileRevisionServiceImpl());
        var scope = mock(DccControlledFileAssignmentScopeService.class);
        when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenReturn(true);
        var names = mock(DccControlledFileNameClaimService.class);
        when(names.requireSourceName(any(DccControlledFileDO.class)))
                .thenAnswer(call -> call.<DccControlledFileDO>getArgument(0).getSourceOriginalFileName());
        storage = mock(DccControlledFileSourceOwnershipService.class);
        placement = mock(DccPublicUploadPlacementService.class);
        when(storage.createVerifiedCopy(anyLong())).thenAnswer(call ->
                new DccControlledFilePreparedSource(call.<Long>getArgument(0)+2000,call.getArgument(0),HASH,true));
        when(storage.rollbackCleanup(any())).thenAnswer(call -> {
            DccControlledFilePreparedSource copy=call.getArgument(0);
            return (Runnable) () -> storage.cleanupPreparedSource(copy);
        });
        var dates = new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
        wire(revision,"fileMapper",files,"masterMapper",masters,"checkoutMapper",checkoutMapper,
                "versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"datePolicy",dates,
                "projectAccessService",access,"permissionSupport",permissions,"assignmentScopeService",scope,
                "sourceOwnershipService",storage,"publicUploadPlacementService",placement,"nameClaimService",names);
        portIfPresent(revision,"applicationRoundService",rounds);
        portIfPresent(revision,"executionContracts",contracts);
        portIfPresent(revision,"attachmentService",a21EmptyAttachmentPort());
        return revision;
    }

    private Long checkin(DccControlledFileQueryServiceImpl query,Long sourceId,String remark) {
        return checkin(query,sourceId,remark,false);
    }
    private Long checkin(DccControlledFileQueryServiceImpl query,Long sourceId,String remark,Boolean training) {
        var reason = new DccControlledFileCheckoutReqVO();reason.setReason("task-owned prepared source edit");
        tx().executeWithoutResult(status -> query.checkoutControlledFile(99L,sourceId,reason));
        var request = a21ExplicitCheckinRequest();request.setChangeDescription("task-owned prepared source checkin");
        request.setRemark(remark);
        var exactSource=files.selectById(sourceId);
        boolean newRevision=exactSource.getControlledTime()!=null && "NEW".equals(exactSource.getChangeType());
        request.setNeedTraining(training!=null?training:newRevision?false:Objects.requireNonNull(exactSource.getNeedTraining(),"saved fixture training choice"));
        return tx().execute(status -> query.checkinControlledFile(99L,sourceId,request).getId());
    }

    private DccControlledFileSubmitIterationReqVO request(String intent,String key,Long sourceId) {
        var selected=files.selectById(sourceId);
        var request = new DccControlledFileSubmitIterationReqVO();
        request.setRevisionChangeType(intent);request.setIdempotencyKey(key);
        request.setChangeDescription("task-owned exact prepared candidate");request.setNeedTraining(false);
        request.setEffectiveMode("PRESET_DATE");request.setEffectiveDate(LocalDate.of(2099,12,20));
        request.setProjectAttributes(new DccProjectAttributes(List.of("CE"),null,"Y","N","N",null));
        request.setSelectedSignoffDepartmentIds(List.of(101L));request.setSelectedDistributionDepartmentIds(List.of(201L));
        request.setExecutionContract(selected.getPreparedWorkflowContract());
        request.setExpectedProcessDefinitionId(selected.getPreparedProcessDefinitionId());
        request.setExpectedDefinitionDigest(selected.getPreparedDefinitionDigest());
        var manifest=a21EmptyAttachmentPort().readOrdinaryAttachmentManifest(99L,sourceId);
        request.setExpectedOrdinaryAttachmentIds(manifest.attachmentIds());
        request.setExpectedOrdinaryAttachmentManifestHash(manifest.payloadHash());
        return request;
    }

    private void assertPrepared(Long target,String id) {
        var file=files.selectById(target);
        assertEquals(DccApplicationExecutionContractService.LEGACY,file.getPreparedWorkflowContract());
        assertEquals(id,file.getPreparedProcessDefinitionId());
        assertEquals(DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(id)),file.getPreparedDefinitionDigest());
    }

    private Map<String,Object> originalBusinessFacts(Long id) {
        var facts=original(id);
        // Successful checkout/checkin legitimately updates these two generic mutation metadata fields.
        facts.remove("update_time");facts.remove("updater");
        return facts;
    }

    @Test void actualCheckinAndInitialFreezeCarryOnlyTheVerifiedExactRoot() {
        prepareRoot();var before=originalBusinessFacts(20L);var query=actualQuery();
        Long first=checkin(query,20L,"first task-owned edit");assertPrepared(first,exactId);
        Long second=checkin(query,first,"second task-owned edit");assertPrepared(second,exactId);
        assertEquals(first,files.selectById(second).getPredecessorControlledFileId());
        var revision=actualRevision();
        var candidate=tx().execute(status -> revision.createInitialCandidate(99L,second,request("INITIAL","real-initial",second)));
        assertNotNull(candidate);assertPrepared(candidate.getId(),exactId);
        assertEquals(exactId,candidate.getPreparedProcessDefinitionId());
        assertEquals(second,candidate.getSelectedIterationControlledFileId());
        assertEquals(second,candidate.getPredecessorControlledFileId());
        verify(contracts).inheritPrepared(99L,candidate.getId(),second,rounds.requireDraft("UPLOAD",second));
        assertNull(attributeRows.find("UPLOAD",candidate.getId(),1),"target attributes stay owned by later Workflow initialization");
        assertEquals(before,originalBusinessFacts(20L));verify(definitions,never()).getActiveProcessDefinition(anyString());
    }

    @Test void unknownRootCannotCheckinAndItsActualCheckoutRemainsActive() {
        var query=actualQuery();var reason=new DccControlledFileCheckoutReqVO();reason.setReason("unknown source negative");
        tx().executeWithoutResult(status -> query.checkoutControlledFile(99L,20L,reason));
        var input=a21ExplicitCheckinRequest();input.setChangeDescription("unknown source negative");input.setRemark("changed");input.setNeedTraining(false);
        var failure=assertThrows(IllegalStateException.class,()->tx().execute(status->query.checkinControlledFile(99L,20L,input)));
        assertEquals("EXECUTION_CONTRACT_AUTHORITATIVE_SOURCE_REQUIRED",failure.getMessage());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertEquals("ACTIVE",checkoutMapper.selectActiveByMasterIdForRead(1L,10L).getStatus());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
        verify(storage).cleanupPreparedSource(any());verify(definitions,never()).getActiveProcessDefinition(anyString());
    }

    @Test void lateOuterCheckinFailureRollsBackBodyPreparationAttributesAndCheckout() {
        prepareRoot();var query=actualQuery();var reason=new DccControlledFileCheckoutReqVO();reason.setReason("outer rollback");
        tx().executeWithoutResult(status->query.checkoutControlledFile(99L,20L,reason));var before=original(20L);
        var input=a21ExplicitCheckinRequest();input.setChangeDescription("outer rollback");input.setRemark("changed");input.setNeedTraining(false);
        var failure=assertThrows(IllegalStateException.class,()->tx().execute(status->{
            Long next=query.checkinControlledFile(99L,20L,input).getId();assertPrepared(next,exactId);
            throw new IllegalStateException("actual late outer failure");
        }));
        assertEquals("actual late outer failure",failure.getMessage());assertEquals(before,original(20L));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertNotNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));verify(storage).cleanupPreparedSource(any());
    }

    @Test void actualCandidateWithoutAuthoritativeSourceCannotFreezeAsSuccess() {
        copy(21L,20L);var revision=actualRevision();
        var failure=assertThrows(IllegalStateException.class,()->tx().execute(status -> revision.createInitialCandidate(99L,21L,request("INITIAL","unknown-candidate",21L))));
        assertEquals("EXECUTION_CONTRACT_AUTHORITATIVE_SOURCE_REQUIRED",failure.getMessage());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        verify(storage).cleanupPreparedSource(any());
    }

    @Test void actualCandidatePreparationAlsoRollsBackWhenOuterCallerFails() {
        prepareRoot();copy(21L,20L);inherit(21L);var before=original(21L);var revision=actualRevision();
        var failure=assertThrows(IllegalStateException.class,()->tx().execute(status->{
            var candidate=revision.createInitialCandidate(99L,21L,request("INITIAL","late-candidate",21L));
            assertPrepared(candidate.getId(),exactId);throw new IllegalStateException("late BPM failure after actual candidate");
        }));
        assertEquals("late BPM failure after actual candidate",failure.getMessage());assertEquals(before,original(21L));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));verify(storage).cleanupPreparedSource(any());
    }

    private String controlledUploadRoot() throws Exception {
        rejectedOriginalWithoutPreparedFields();
        jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP,activated_time=CURRENT_TIMESTAMP WHERE id=20");
        jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        var matcher=java.util.regex.Pattern.compile("SET @dcc_revision_bpmn = '([\\s\\S]*?)';")
                .matcher(Files.readString(root.resolve("IntRuoyiBackend/sql/mysql/20260930_dcc_a_workflow_bpmn_v4.sql")));
        assertTrue(matcher.find());return deploy(matcher.group(1));
    }

    @Test void actualCrossActionCheckinUsesOnlyTheRegisteredExactRevisionDefinition() throws Exception {
        String revisionId=controlledUploadRoot();
        transitions.setDefinitionTransitions(List.of(new DccExecutionDefinitionTransitions.Transition(exactId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exactId)),revisionId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revisionId)))));
        var before=originalBusinessFacts(20L);var query=actualQuery();Long body=checkin(query,20L,"controlled revision edit");
        assertPrepared(body,revisionId);assertEquals("REVISION",files.selectById(body).getChangeType());
        assertEquals(20L,files.selectById(body).getRevisionBaseActiveControlledFileId());
        assertNull(attributeRows.find("REVISION",body,1).getSourceApplicationId());assertEquals(before,originalBusinessFacts(20L));
        var candidate=tx().execute(status -> actualRevision().createRevision(99L,20L,body,request("PARTIAL","exact-revision",body)));
        assertNotNull(candidate);assertPrepared(candidate.getId(),revisionId);assertEquals(revisionId,candidate.getPreparedProcessDefinitionId());
        verify(definitions,never()).getActiveProcessDefinition(anyString());
    }

    @Test void actualCrossActionCheckinWithoutMappingCannotGuessTheRevisionDefinition() throws Exception {
        controlledUploadRoot();var query=actualQuery();
        var failure=assertThrows(IllegalStateException.class,()->checkin(query,20L,"unregistered transition"));
        assertEquals("EXECUTION_CONTRACT_EXACT_ACTION_TRANSITION_REQUIRED",failure.getMessage());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        assertNotNull(checkoutMapper.selectActiveByMasterIdForRead(1L,10L));verify(storage).cleanupPreparedSource(any());
        verify(definitions,never()).getActiveProcessDefinition(anyString());
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void aNewRevisionUsesTheConfirmedUncheckedTrainingDefaultAndKeepsOldSourceTrue(boolean explicitTrue) throws Exception {
        String revisionId=controlledUploadRoot();
        transitions.setDefinitionTransitions(List.of(new DccExecutionDefinitionTransitions.Transition(exactId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(exactId)),revisionId,
                DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(revisionId)))));
        jdbc.update("UPDATE dcc_controlled_file SET need_training=1 WHERE id=20");
        var query=actualQuery();Long body=checkin(query,20L,"new training choice",explicitTrue?true:null);
        var saved=files.selectById(body);assertEquals(explicitTrue,saved.getNeedTraining());assertTrue(files.selectById(20L).getNeedTraining());
        assertNull(saved.getEffectiveMode());assertNull(saved.getDraftApprovalMatrixJson());
        assertNull(saved.getSelectedSignoffDepartmentIdsJson());assertNull(saved.getDistributionDepartmentIdsJson());
        assertNull(saved.getTrainingRecordFileId());assertNull(saved.getControlledTime());assertNull(saved.getActivatedTime());assertNull(saved.getDistributedTime());
        assertNull(saved.getEffectiveTime());assertNull(saved.getEffectiveSignatureId());
    }

    @ParameterizedTest @ValueSource(strings={"missing","digest","predecessor","selected"})
    void actualCandidateReplayRejectsCorruptPreparedScopeWithoutRepairingIt(String invalid) {
        prepareRoot();copy(21L,20L);inherit(21L);var revision=actualRevision();
        var input=request("INITIAL","replay-scope",21L);
        var candidate=tx().execute(status->revision.createInitialCandidate(99L,21L,input));
        switch(invalid){
            case "missing"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=NULL WHERE id=?",candidate.getId());
            case "digest"->jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=? WHERE id=?","b".repeat(64),candidate.getId());
            case "predecessor"->jdbc.update("UPDATE dcc_controlled_file SET predecessor_controlled_file_id=20 WHERE id=?",candidate.getId());
            case "selected"->jdbc.update("UPDATE dcc_controlled_file SET selected_iteration_version_no='A/1-9' WHERE id=?",candidate.getId());
        }
        var before=original(candidate.getId());int count=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class);
        clearInvocations(contracts,storage,placement);
        assertThrows(RuntimeException.class,()->tx().execute(status->revision.createInitialCandidate(99L,21L,input)));
        assertEquals(before,original(candidate.getId()));assertEquals(count,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        verify(contracts,never()).inheritPrepared(any(),any(),any(),any());verifyNoInteractions(storage);
        verify(placement,never()).inherit(any(),any(),any(),any());
    }

    @ParameterizedTest @ValueSource(strings={"contract","id","digest"})
    void changedExpectedPreparedProofIsAReadonlyCandidateReplayConflict(String changed) {
        prepareRoot();copy(21L,20L);inherit(21L);var revision=actualRevision();
        var input=request("INITIAL","expected-replay",21L);var candidate=tx().execute(status->revision.createInitialCandidate(99L,21L,input));
        switch(changed){case "contract"->input.setExecutionContract(DccApplicationExecutionContractService.PUBLIC);
            case "id"->input.setExpectedProcessDefinitionId("changed-id");case "digest"->input.setExpectedDefinitionDigest("b".repeat(64));}
        var before=original(candidate.getId());clearInvocations(contracts,storage,placement);
        assertThrows(IllegalArgumentException.class,()->tx().execute(status->revision.createInitialCandidate(99L,21L,input)));
        assertEquals(before,original(candidate.getId()));verifyNoInteractions(storage);
        verify(placement,never()).inherit(any(),any(),any(),any());
        verify(contracts,never()).inheritPrepared(any(),any(),any(),any());
    }

    @Test void actualSubmittedCandidateReplayUsesTheReadonlyPortAndDoesNotCopyOrPrepareAgain() {
        prepareRoot();copy(21L,20L);inherit(21L);var revision=actualRevision();var input=request("INITIAL","submitted-replay",21L);
        var candidate=tx().execute(status->revision.createInitialCandidate(99L,21L,input));
        startThroughActualWorkflowAndOfficialBpm(candidate.getId(),exactId);
        String bpm=engine.getRuntimeService().createProcessInstanceQuery().processInstanceBusinessKey(candidate.getId().toString()).singleResult().getId();
        tx().executeWithoutResult(status->{initializer.initialize(99L,21L,candidate.getId(),null);
            snapshots.submitReservedDraft(99L,5L,"UPLOAD",candidate.getId(),rounds.requireDraft("UPLOAD",candidate.getId()),bpm,actual);});
        jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_MATRIX_REVIEW',process_instance_id=?,process_definition_key=? WHERE id=?",bpm,DccControlledFileProcessDefinitionKeys.UPLOAD,candidate.getId());
        var before=original(candidate.getId());clearInvocations(contracts,storage,placement);
        assertEquals(candidate.getId(),tx().execute(status->revision.createInitialCandidate(99L,21L,input)).getId());
        verify(contracts).verifyInheritedPrepared(99L,candidate.getId(),21L,rounds.requireDraft("UPLOAD",21L));
        verify(contracts,never()).inheritPrepared(any(),any(),any(),any());verifyNoInteractions(storage);
        verify(placement,never()).inherit(any(),any(),any(),any());
        assertEquals(before,original(candidate.getId()));
    }

    @Test void actualCheckinReplayRejectsMissingPreparationWithoutRefillingTheExistingBody() {
        prepareRoot();var query=actualQuery();Long body=checkin(query,20L,"checkin-replay");
        jdbc.update("UPDATE dcc_controlled_file SET prepared_definition_digest=NULL WHERE id=?",body);
        var input=a21ExplicitCheckinRequest();input.setChangeDescription("task-owned prepared source checkin");input.setRemark("checkin-replay");input.setNeedTraining(false);
        var before=original(body);clearInvocations(contracts,storage,placement);
        assertThrows(RuntimeException.class,()->tx().execute(status->query.checkinControlledFile(99L,20L,input)));
        assertEquals(before,original(body));verifyNoInteractions(storage);
        verify(contracts,never()).inheritPrepared(any(),any(),any(),any());
    }

    private String publicRoot() throws Exception {
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String definition=deploy(Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/upload.bpmn20.xml")));
        String digest=DccLifecycleEffectiveContractResolver.definitionDigest(engine.getRepositoryService().getBpmnModel(definition));
        var verified=contracts.requireOrdinaryPublic(DccApplicationExecutionContractService.PUBLIC,definition,digest,DccControlledFileProcessDefinitionKeys.UPLOAD);
        jdbc.update("UPDATE dcc_controlled_file SET prepared_workflow_contract=?,prepared_process_definition_id=?,prepared_definition_digest=? WHERE id=20",
                verified.executionContract(),verified.processDefinitionId(),verified.definitionDigest());
        return definition;
    }
    private DccControlledFileWorkflowServiceImpl applicationReader() {
        var workflow=isolatedPorts(new DccControlledFileWorkflowServiceImpl());
        wire(workflow,"controlledFileMapper",files,"controlledFileMasterMapper",masters,
                "projectApplicationSnapshots",snapshots,"projectAttributesService",attributes,
                "projectAccessService",access,"categoryPermissionSupport",permissions,"workingApplicationDraftInitializer",initializer,
                "versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"checkoutMapper",checkoutMapper,
                "projectCodeMapper",projects);
        var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(eq(99L),anyLong())).thenReturn(true);
        var departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);
        when(departments.getDeptList(eq(List.of(61L)))).thenReturn(List.of(new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(61L).setStatus(0).setName("declared signoff department")));
        when(departments.getDeptList(eq(List.of(62L)))).thenReturn(List.of(new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(62L).setStatus(0).setName("declared distribution department")));
        wire(workflow,"deptApi",departments);
        wire(workflow,"assignmentScopeService",scope);return workflow;
    }
    @Test void actualPublicTwoCheckinsRetainOriginalMatrixAndReopenSavedApplication() throws Exception {
        String definition=publicRoot(),original=canonicalMatrix(1);
        jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=?,effective_mode='AFTER_STANDARDIZATION_SIGNATURE',effective_date=NULL,selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[\"52\",\"53\"]',need_training=1 WHERE id=20",original);
        var query=actualQuery();Long first=checkin(query,20L,"first public edit",null),second=checkin(query,first,"second public edit",null);
        var saved=files.selectById(second);assertEquals(original,saved.getDraftApprovalMatrixJson());
        assertEquals("AFTER_STANDARDIZATION_SIGNATURE",saved.getEffectiveMode());assertNull(saved.getEffectiveDate());
        assertEquals("[\"51\"]",saved.getSelectedSignoffDepartmentIdsJson());assertEquals("[\"52\",\"53\"]",saved.getDistributionDepartmentIdsJson());assertTrue(saved.getNeedTraining());
        assertEquals(definition,saved.getPreparedProcessDefinitionId());assertNull(saved.getEffectiveTime());assertNull(saved.getEffectiveSignatureId());
        assertNull(saved.getControlledTime());assertNull(saved.getActivatedTime());assertNull(saved.getDistributedTime());
        var workflow=applicationReader();var reopened=tx().execute(status->workflow.readWorkingApplicationAttributes(99L,second));
        assertNotNull(reopened.draftApprovalMatrix());assertEquals(1,reopened.draftApprovalMatrix().getRouteVersionNo());
        assertEquals(List.of("51"),reopened.selectedSignoffDepartmentIds());assertEquals(List.of("52","53"),reopened.selectedDistributionDepartmentIds());
        assertTrue(reopened.needTraining());assertEquals("AFTER_STANDARDIZATION_SIGNATURE",reopened.effectiveMode());
        var input=new DccWorkingApplicationSaveReqVO();input.setActual(actual);input.setEffectiveMode("PRESET_DATE");input.setEffectiveDate(LocalDate.of(2099,12,20));
        input.setSelectedSignoffDepartmentIds(List.of(61L));input.setSelectedDistributionDepartmentIds(List.of(62L));input.setNeedTraining(false);input.setChangeDescription("explicit saved new choices");
        tx().executeWithoutResult(status->workflow.saveWorkingApplication(99L,second,input));
        var after=tx().execute(status->workflow.readWorkingApplicationAttributes(99L,second));
        assertFalse(after.needTraining());assertEquals(List.of("61"),after.selectedSignoffDepartmentIds());assertEquals(List.of("62"),after.selectedDistributionDepartmentIds());
        assertEquals(1,after.draftApprovalMatrix().getRouteVersionNo());assertEquals("explicit saved new choices",after.changeDescription());
        var revision=actualRevision();var candidate=tx().execute(status->revision.createInitialCandidate(99L,second,request("INITIAL","public-freeze",second)));
        assertEquals(original,candidate.getDraftApprovalMatrixJson());assertEquals("PRESET_DATE",candidate.getEffectiveMode());assertFalse(candidate.getNeedTraining());
        assertEquals("[\"101\"]",candidate.getSelectedSignoffDepartmentIdsJson());assertEquals("[\"201\"]",candidate.getDistributionDepartmentIdsJson());
        startThroughActualWorkflowAndOfficialBpm(candidate.getId(),definition);
        assertEquals(definition,engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceBusinessKey(candidate.getId().toString()).singleResult().getProcessDefinitionId());
        verify(definitions,never()).getActiveProcessDefinition(anyString());
    }

    @Test void explicitCheckinTrainingOverridesTheSavedChoiceWithoutChangingTheOriginal() throws Exception {
        publicRoot();String original=canonicalMatrix(1);
        jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=?,need_training=1 WHERE id=20",original);
        var query=actualQuery();Long body=checkin(query,20L,"training override",false);
        assertFalse(files.selectById(body).getNeedTraining());assertTrue(files.selectById(20L).getNeedTraining());
        assertEquals(original,files.selectById(body).getDraftApprovalMatrixJson());
    }

    @Test void actualRejectedPublicCheckinKeepsExecutedMatrixThroughTwoBodiesAndReopensItsOwnSavedFacts() throws Exception {
        String definition=rejectedPublicSource(),before=canonicalMatrix(1),executed=canonicalMatrix(2);
        jdbc.update("UPDATE dcc_controlled_file SET draft_approval_matrix_json=?,effective_mode='PRESET_DATE',effective_date='2099-12-20',selected_signoff_department_ids_json='[\"51\"]',distribution_department_ids_json='[\"52\"]',need_training=1 WHERE id=20",before);
        snapshotExecutedMatrix(executed);var sourceBefore=originalBusinessFacts(20L);var query=actualQuery();
        var reason=new DccControlledFileCheckoutReqVO();reason.setReason("actual rejected body correction");
        tx().executeWithoutResult(status->query.checkoutControlledFile(99L,20L,reason));
        var input=a21ExplicitCheckinRequest();input.setChangeDescription("actual rejected body correction");input.setNeedTraining(true);
        input.setUploadTicket("task-owned-rejected-ticket");input.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.checkinPrefix(20L),"actual-correction"));
        var tickets=mock(cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService.class);
        when(tickets.resolveForBinding(any())).thenReturn(new cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile(
                input.getUploadTicket(),10000L,"SOP.pdf","application/pdf",200L));
        when(storage.prepareSubmissionSource(10000L,false)).thenReturn(new DccControlledFilePreparedSource(11000L,10000L,"b".repeat(64),true));
        when(storage.inspectSource(11000L)).thenReturn(new DccControlledFilePreparedSource(11000L,10000L,"b".repeat(64),false));
        when(storage.prepareSubmissionSource(11000L,false)).thenReturn(new DccControlledFilePreparedSource(12000L,11000L,"b".repeat(64),true));
        wire(query,"uploadTicketService",tickets);
        DccRevisionAttachmentsDatabaseTest.a24BoundSourcePort(query,tickets,input,2L,10000L,"SOP.pdf","application/pdf",200L,"b".repeat(64));
        Long first=tx().execute(status->query.checkinControlledFile(99L,20L,input).getId());
        assertEquals(executed,files.selectById(first).getDraftApprovalMatrixJson());
        Long second=checkin(query,first,"second rejected correction",true);assertEquals(executed,files.selectById(second).getDraftApprovalMatrixJson());
        assertNotEquals(before,files.selectById(second).getDraftApprovalMatrixJson());assertEquals(sourceBefore,originalBusinessFacts(20L));
        var reopened=tx().execute(status->applicationReader().readWorkingApplicationAttributes(99L,second));
        assertEquals(2,reopened.draftApprovalMatrix().getRouteVersionNo());assertEquals(definition,files.selectById(second).getPreparedProcessDefinitionId());
        assertEquals(List.of("51"),reopened.selectedSignoffDepartmentIds());assertEquals(List.of("52"),reopened.selectedDistributionDepartmentIds());assertTrue(reopened.needTraining());
        assertEquals(executed,routeSnapshots.selectListByControlledFileId(20L).get(0).getSourceApprovalMatrixJson());
    }

    /** A21 fixture: no selected attachment is an explicit read/ready empty list. */
    private static DccControlledFileCheckinReqVO a21ExplicitCheckinRequest() {
        var input = new DccControlledFileCheckinReqVO();
        input.setNeedTraining(false);
        input.setAttachmentUploadTickets(java.util.List.of());
        return input;
    }

    /** Explicit no-ordinary-attachments port for this suite's existing body/round fixtures. */
    private static DccControlledFileAttachmentService a21EmptyAttachmentPort() {
        var port=mock(DccControlledFileAttachmentService.class);
        when(port.readOrdinaryAttachmentManifest(anyLong(),anyLong())).thenAnswer(call->{
            Long fileId=call.getArgument(1);var facts=java.util.List.<DccControlledFileAttachmentService.OrdinaryAttachmentFact>of();
            String canonical=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Arrays.asList(
                    "DCC_ORDINARY_ATTACHMENT_MANIFEST_V1",TenantContextHolder.getRequiredTenantId(),fileId,facts));
            return new DccControlledFileAttachmentService.OrdinaryAttachmentManifest(facts,cn.hutool.crypto.digest.DigestUtil.sha256Hex(canonical));
        });
        return port;
    }

    private static void a21WireCurrentReadProjection(DccControlledFileQueryServiceImpl query) {
        var guard=new DccControlledFileDetailAuthorizationGuard();
        for(var field:guard.getClass().getDeclaredFields()) if(field.getAnnotation(jakarta.annotation.Resource.class)!=null)
            ReflectionTestUtils.setField(guard,field.getName(),mock(field.getType()));
        ReflectionTestUtils.setField(guard,"assignmentScopeService",ReflectionTestUtils.getField(query,"assignmentScopeService"));
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        ReflectionTestUtils.setField(query,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
    }
}
