package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccRevisionServiceTest extends BaseMockitoUnitTest {
    @org.junit.jupiter.api.BeforeEach void g25DeclaredSourceProjectionPort() {
        org.mockito.Mockito.lenient().when(nameClaimService.requireSourceName(org.mockito.ArgumentMatchers.any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.class)))
            .thenAnswer(call -> call.<cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO>getArgument(0).getSourceOriginalFileName());
    }

    @InjectMocks private DccControlledFileRevisionServiceImpl service;
    @Mock private DccPublicUploadPlacementService publicUploadPlacementService;
    @Mock private DccControlledFileMapper fileMapper;
    @Mock private DccControlledFileMasterMapper masterMapper;
    @Mock private DccControlledFileCheckoutMapper checkoutMapper;
    @Mock private DccProjectAccessService projectAccessService;
    @Mock private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock private DccControlledFileSourceOwnershipService sourceOwnershipService;
    @Mock private DccControlledFileRelatedFileService relatedFileService;
    @Mock private DccControlledFileNameClaimService nameClaimService;
    @Mock private cn.iocoder.yudao.module.system.api.permission.PermissionApi scopePermissionApi;
    @Mock private cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper scopeAssignmentMapper;
    @Mock private DccControlledFileDistributionRecipientMapper scopeDistributionMapper;
    @Spy private DccControlledFileAssignmentScopeService assignmentScopeService = new DccControlledFileAssignmentScopeService();
    @Spy private DccControlledFileVersionPolicy versionPolicy = DccControlledFileVersionPolicy.defaultPolicy();
    @Spy private DccWorkflowDatePolicy datePolicy = new DccWorkflowDatePolicy();
    private DccControlledFileDO baseline, selected;
    private DccControlledFileSubmitIterationReqVO request;
    private final DccControlledFilePreparedSource frozen = new DccControlledFilePreparedSource(300L, 100L, "selected-hash", true);

    @BeforeEach void setup() {
        datePolicy.setZoneId("Asia/Singapore");
        TenantContextHolder.setTenantId(1L);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        org.springframework.test.util.ReflectionTestUtils.setField(assignmentScopeService, "permissionApi", scopePermissionApi);
        org.springframework.test.util.ReflectionTestUtils.setField(assignmentScopeService, "assignmentFileMapper", scopeAssignmentMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(assignmentScopeService, "distributionRecipientMapper", scopeDistributionMapper);
        baseline = file(20L, "A/9", "ACTIVE");
        selected = file(21L, "A/9-1", "WORKING");
        selected.setRevisionBaseActiveControlledFileId(20L);
        request = new DccControlledFileSubmitIterationReqVO();
        request.setIdempotencyKey("revision-key"); request.setRevisionChangeType("PARTIAL");
        request.setChangeDescription("selected earlier text"); request.setNeedTraining(false);
        request.setEffectiveDate(java.time.LocalDate.of(2099,12,20));
        request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(
                List.of("CE"), null, "Y", "N", "N", null));
        request.setSelectedSignoffDepartmentIds(List.of(101L, 102L));
        lenient().when(fileMapper.selectById(20L)).thenAnswer(i -> baseline);
        lenient().when(fileMapper.selectById(21L)).thenAnswer(i -> selected);
        lenient().when(masterMapper.selectByIdForUpdate(10L)).thenReturn(DccControlledFileMasterDO.builder().id(10L).tenantId(1L).latestControlledFileId(20L)
                .dccProjectCodeId(5L).fileTypeTaxonomyLeafId(6L).normalizedFileNumber("SOP-001").build());
        lenient().when(fileMapper.selectByIdAndTenantForUpdate(1L, 20L)).thenAnswer(i -> baseline);
        lenient().when(fileMapper.selectByIdAndTenantForUpdate(1L, 21L)).thenAnswer(i -> selected);
        lenient().when(fileMapper.selectListByMasterIdForUpdate(10L)).thenAnswer(i -> List.of(baseline, selected, file(22L, baseline.getVersionNo()+"-2", "WORKING")));
        lenient().when(permissionSupport.hasCategoryPermission(2L, 99L, DccFileCategoryPermissionActionEnum.UPLOAD)).thenReturn(true);
        lenient().when(sourceOwnershipService.createVerifiedCopy(100L)).thenReturn(frozen);
        lenient().when(fileMapper.insert(any(DccControlledFileDO.class))).thenAnswer(i -> { i.<DccControlledFileDO>getArgument(0).setId(30L); return 1; });
    }
    @AfterEach void clear() { TransactionSynchronizationManager.clear(); TenantContextHolder.clear(); }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"active", "latest", "foreignTenant", "foreignProject", "wrongRequester", "missingPredecessor", "cycle", "formalSelected", "sourceHash"})
    void initialCandidateRejectsInvalidChainOrBodyWithoutCreatingSuccess(String invalid) {
        initialWorkingChain("A/1");
        switch(invalid) {
            case "active" -> masterMapper.selectByIdForUpdate(10L).setCurrentActiveControlledFileId(20L);
            case "latest" -> masterMapper.selectByIdForUpdate(10L).setLatestControlledFileId(20L);
            case "foreignTenant" -> selected.setTenantId(2L);
            case "foreignProject" -> selected.setDccProjectCodeId(6L);
            case "wrongRequester" -> selected.setRequesterId(88L);
            case "missingPredecessor" -> selected.setPredecessorControlledFileId(19L);
            case "cycle" -> selected.setPredecessorControlledFileId(21L);
            case "formalSelected" -> selected.setVersionNo("A/1");
            case "sourceHash" -> selected.setSourceSha256("changed-hash");
        }
        assertThrows(RuntimeException.class,() -> service.createInitialCandidate(99L,21L,request));
        verify(fileMapper,never()).insert(any(DccControlledFileDO.class));
        if(!"sourceHash".equals(invalid)) { verify(nameClaimService,never()).claimExistingVersion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        verify(nameClaimService,never()).claimIdentity(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());verifyNoInteractions(sourceOwnershipService,relatedFileService);
        }
        else verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"date", "attributes", "departments", "body"})
    void initialCandidateRejectsChangedActualFactsOnSameKey(String changed) {
        initialWorkingChain("A/1");var result=service.createInitialCandidate(99L,21L,request);
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline,selected,result));
        switch(changed) {
            case "date" -> request.setEffectiveDate(request.getEffectiveDate().plusDays(1));
            case "attributes" -> request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("FDA"),null,"Y","N","N",null));
            case "departments" -> request.setSelectedSignoffDepartmentIds(List.of(101L,103L));
            case "body" -> selected.setSourceSha256("replaced-body");
        }
        assertThrows(IllegalArgumentException.class,() -> service.createInitialCandidate(99L,21L,request));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void initialCandidateRequiresRealEditorAndActualCheckoutRemainsAuthoritative() {
        initialWorkingChain("A/1");
        doThrow(new IllegalStateException("not EDIT")).when(projectAccessService).assertProjectEditorOrOwner(99L,5L);
        assertThrows(IllegalStateException.class,() -> service.createInitialCandidate(99L,21L,request));
        doNothing().when(projectAccessService).assertProjectEditorOrOwner(99L,5L);
        when(checkoutMapper.selectActiveByMasterId(1L,10L)).thenReturn(DccControlledFileCheckoutDO.builder().actorId(88L).build());
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> service.createInitialCandidate(99L,21L,request),cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ALREADY_CHECKED_OUT,88L);
        verify(nameClaimService,never()).claimExistingVersion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        verify(nameClaimService,never()).claimIdentity(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());verifyNoInteractions(sourceOwnershipService,relatedFileService);
    }
    private void initialWorkingChain(String initialVersion) {
        var master = masterMapper.selectByIdForUpdate(10L); master.setLatestControlledFileId(null);
        baseline.setVersionNo(initialVersion); baseline.setStatus("WORKING"); baseline.setChangeType("NEW");
        baseline.setControlledTime(null); baseline.setRevisionChangeType("INITIAL");
        selected.setVersionNo(initialVersion+"-1"); selected.setChangeType("NEW"); selected.setRevisionBaseActiveControlledFileId(null);
        selected.setPredecessorControlledFileId(20L);
        request.setRevisionChangeType("INITIAL");
        lenient().when(fileMapper.selectById(21L)).thenReturn(selected);
        lenient().when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline, selected));
    }
    @ParameterizedTest @CsvSource({"A/1", "B/1"})
    void initialCandidateFreezesSelectedBodyWithoutRevisionIncrementOrChangingOriginals(String initialVersion) {
        initialWorkingChain(initialVersion);
        selected.setProcessInstanceId("old-working-process"); selected.setProcessDefinitionKey("old-process-key");
        selected.setSubmittedTime(java.time.LocalDateTime.of(2026,9,30,12,0));
        selected.setControlledTime(selected.getSubmittedTime()); selected.setActivatedTime(selected.getSubmittedTime());
        selected.setDistributedTime(selected.getSubmittedTime()); selected.setDistributionPayloadHash("old-success");
        selected.setFileOwnerUserId(99L);selected.setFileOwnerUsernameSnapshot("old-owner");selected.setFileOwnerNicknameSnapshot("旧负责人");
        selected.setFileOwnerSignatureId(50L);selected.setFileOwnerApprovalTaskId("old-owner-task");selected.setFileOwnerProcessInstanceId("old-working-process");selected.setFileOwnerSelectedTime(selected.getSubmittedTime());
        var result = service.createInitialCandidate(99L, 21L, request);
        assertEquals(initialVersion, result.getVersionNo()); assertEquals("INITIAL", result.getRevisionChangeType());
        assertEquals("NEW", result.getChangeType()); assertEquals("WORKING", result.getStatus());
        assertEquals(21L, result.getSelectedIterationControlledFileId()); assertEquals(initialVersion+"-1",result.getSelectedIterationVersionNo());
        assertNull(result.getRevisionSourceControlledFileId()); assertNull(result.getRevisionSourceVersionNo());
        assertNull(result.getRevisionBaseActiveControlledFileId()); assertEquals(21L,result.getPredecessorControlledFileId());
        assertEquals(300L,result.getSourceFileId()); assertEquals(300L,result.getOriginalFileId());
        assertEquals(request.getEffectiveDate(),result.getEffectiveDate());
        assertNull(result.getProcessInstanceId()); assertNull(result.getProcessDefinitionKey());
        assertNull(result.getSubmittedTime()); assertNull(result.getControlledTime()); assertNull(result.getActivatedTime());
        assertNull(result.getDistributedTime()); assertNull(result.getDistributionPayloadHash());
        assertNull(result.getFileOwnerUserId());assertNull(result.getFileOwnerSignatureId());assertNull(result.getFileOwnerUsernameSnapshot());assertNull(result.getFileOwnerNicknameSnapshot());
        assertNull(result.getFileOwnerApprovalTaskId());assertNull(result.getFileOwnerProcessInstanceId());assertNull(result.getFileOwnerSelectedTime());
        assertEquals(50L,selected.getFileOwnerSignatureId());
        assertEquals(initialVersion,baseline.getVersionNo()); assertEquals(initialVersion+"-1",selected.getVersionNo());
        assertEquals(100L,selected.getSourceFileId());
        verify(relatedFileService).inheritRelatedFiles(21L,30L);
        verify(masterMapper,never()).updateById(any(DccControlledFileMasterDO.class));
    }
    @Test void initialCandidateFollowsRecordedPredecessorsAcrossTwoCheckins() {
        initialWorkingChain("A/1");
        var previous = file(22L,"A/1-1","WORKING"); previous.setChangeType("NEW");previous.setPredecessorControlledFileId(20L);
        selected.setVersionNo("A/1-2");selected.setPredecessorControlledFileId(22L);
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(selected,previous,baseline));
        assertEquals("A/1",service.createInitialCandidate(99L,21L,request).getVersionNo());
        assertEquals("A/1-2",selected.getVersionNo()); assertEquals("A/1-1",previous.getVersionNo());
    }
    @Test void initialCandidateHasOwnActualPayloadReplayAndDoesNotReallocateBody() {
        initialWorkingChain("A/1");var result = service.createInitialCandidate(99L,21L,request);
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline,selected,result));
        assertEquals(result.getId(),service.createInitialCandidate(99L,21L,request).getId());
        request.setEffectiveDate(request.getEffectiveDate().plusDays(1));
        assertThrows(IllegalArgumentException.class,() -> service.createInitialCandidate(99L,21L,request));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }

    @Test void formalRevisionUsesActualEffectiveDateAndRejectsChangedDateReplay() {
        selected.setEffectiveDate(java.time.LocalDate.of(2026, 10, 1));
        var created = service.createRevision(99L, 20L, 21L, request);
        assertEquals(java.time.LocalDate.of(2099, 12, 20), created.getEffectiveDate());
        assertEquals(java.time.LocalDate.of(2026, 10, 1), selected.getEffectiveDate());
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline, selected, created));
        request.setEffectiveDate(java.time.LocalDate.of(2099, 12, 21));
        assertThrows(IllegalArgumentException.class, () -> service.createRevision(99L, 20L, 21L, request));
        verify(sourceOwnershipService, times(1)).createVerifiedCopy(100L);
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"2000-01-01"})
    void missingOrPastApplicationDateRejectsBeforeReservationAndFrozenCopy(String date) {
        request.setEffectiveDate(date == null ? null : java.time.LocalDate.parse(date));
        assertThrows(IllegalArgumentException.class, () -> service.createRevision(99L, 20L, 21L, request));
        verify(nameClaimService,never()).claimExistingVersion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        verify(nameClaimService,never()).claimIdentity(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());verifyNoInteractions(sourceOwnershipService,relatedFileService);
        verify(fileMapper, never()).insert(any(DccControlledFileDO.class));
    }


    @ParameterizedTest @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(longs = {19L})
    void selectedWorkingBodyRequiresItsRecordedFormalSourceIdentity(Long recordedSource) {
        selected.setRevisionBaseActiveControlledFileId(recordedSource);
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> service.createRevision(99L, 20L, 21L, request),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CHECKIN_NOT_ALLOWED);
        verify(nameClaimService,never()).claimExistingVersion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        verify(nameClaimService,never()).claimIdentity(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());verifyNoInteractions(sourceOwnershipService,relatedFileService);
        verify(fileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @ParameterizedTest @CsvSource({"PARTIAL,20", "PARTIAL,21", "REPLACEMENT,20", "REPLACEMENT,21"})
    void formalRevisionCannotBypassAssignedScopeForBaselineOrSelectedBody(String intent, Long excludedId) {
        request.setRevisionChangeType(intent);
        lenient().when(scopePermissionApi.hasAnyPermissions(99L, "dcc:project-code-assignment:execute")).thenReturn(true);
        lenient().when(scopeAssignmentMapper.selectActiveControlledFileIdsByAssigneeUserId(eq(99L), any(java.time.LocalDateTime.class)))
                .thenReturn(List.of(excludedId == 20L ? 21L : 20L));
        lenient().when(scopeDistributionMapper.selectActiveElectronicControlledFileIdsByUserId(1L, 99L)).thenReturn(List.of());
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> service.createRevision(99L, 20L, 21L, request),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        verify(nameClaimService,never()).claimExistingVersion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        verify(nameClaimService,never()).claimIdentity(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());verifyNoInteractions(projectAccessService,permissionSupport,sourceOwnershipService,relatedFileService);
        verify(fileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings = {"attributes", "departments", "training"})
    void absentActualApplicationFactsCannotAllocateAFormalCandidate(String missing) {
        if ("attributes".equals(missing)) request.setProjectAttributes(null);
        else if ("departments".equals(missing)) request.setSelectedSignoffDepartmentIds(null);
        else request.setNeedTraining(null);
        assertThrows(IllegalArgumentException.class, () -> service.createRevision(99L, 20L, 21L, request));
        verify(nameClaimService,never()).claimExistingVersion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        verify(nameClaimService,never()).claimIdentity(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());verifyNoInteractions(sourceOwnershipService,relatedFileService);
        verify(fileMapper, never()).insert(any(DccControlledFileDO.class));
    }

    @ParameterizedTest @CsvSource({"PARTIAL,A/1,A/2", "PARTIAL,A/9,B/1", "REPLACEMENT,A/3,B/1"})
    void freezesSelectedBodyAndPersistsActualIntent(String type, String base, String target) {
        baseline.setVersionNo(base); selected.setVersionNo(base+"-1"); request.setRevisionChangeType(type);
        var result = service.createRevision(99L, 20L, 21L, request);
        assertEquals(target, result.getVersionNo());
        assertEquals(type, result.getRevisionChangeType());
        assertEquals(20L, result.getRevisionSourceControlledFileId()); assertEquals(base, result.getRevisionSourceVersionNo());
        assertEquals(21L, result.getSelectedIterationControlledFileId()); assertEquals(base+"-1", result.getSelectedIterationVersionNo());
        assertEquals(300L, result.getSourceFileId()); assertEquals("selected-hash", result.getSourceSha256());
        assertEquals("SOP.pdf", result.getSourceOriginalFileName());
        verify(sourceOwnershipService).createVerifiedCopy(100L);
        verify(relatedFileService).inheritRelatedFiles(21L, 30L);
        verify(masterMapper, never()).updateById(any(DccControlledFileMasterDO.class));
        assertEquals("ACTIVE", baseline.getStatus());
    }
    @Test void newRevisionDoesNotInheritSuccessfulLifecycleFacts() {
        selected.setControlledTime(java.time.LocalDateTime.of(2026,9,1,0,0));
        selected.setActivatedTime(selected.getControlledTime());
        selected.setDistributedTime(selected.getControlledTime());
        selected.setDistributionPayloadHash("old-confirmed-distribution");
        var result=service.createRevision(99L,20L,21L,request);
        assertNull(result.getControlledTime());assertNull(result.getActivatedTime());
        assertNull(result.getDistributedTime());assertNull(result.getDistributionPayloadHash());
        assertNotNull(selected.getControlledTime());
    }
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"attributes", "departments", "training"})
    void formalReplayRejectsChangedActualApplicationFacts(String changed) {
        request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(
                List.of("CE"),null,"Y","N","N",null));
        request.setSelectedSignoffDepartmentIds(List.of(101L,102L));
        var first=service.createRevision(99L,20L,21L,request);
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline,selected,first));
        if ("attributes".equals(changed)) request.setProjectAttributes(
                new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("FDA"),null,"Y","N","N",null));
        else if ("departments".equals(changed)) request.setSelectedSignoffDepartmentIds(List.of(101L,103L));
        else request.setNeedTraining(true);
        assertThrows(IllegalArgumentException.class,() -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void newFormalRevisionRequiresLatestControlledSourceWithoutExecutionFallback() {
        var master=masterMapper.selectByIdForUpdate(10L); master.setLatestControlledFileId(99L); master.setCurrentActiveControlledFileId(20L);
        assertThrows(RuntimeException.class,() -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
    }
    @Test void absentLatestPointerCannotUseCurrentExecutionAsFormalSource() {
        masterMapper.selectByIdForUpdate(10L).setCurrentActiveControlledFileId(20L);
        masterMapper.selectByIdForUpdate(10L).setLatestControlledFileId(null);
        assertThrows(RuntimeException.class,() -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
    }
    @Test void unchangedApplicationFactsReplayAfterLatestPointerAdvances() {
        request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(
                List.of("CE","FDA"),null,"Y","N","N",null));
        request.setSelectedSignoffDepartmentIds(List.of(101L,102L));
        var first=service.createRevision(99L,20L,21L,request);
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline,selected,first));
        masterMapper.selectByIdForUpdate(10L).setLatestControlledFileId(first.getId());
        request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(
                List.of("FDA","CE"),null,"Y","N","N",null));
        request.setSelectedSignoffDepartmentIds(List.of(102L,101L));
        assertEquals(first.getId(),service.createRevision(99L,20L,21L,request).getId());
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void malformedActualDepartmentCollectionFailsBeforeCopyingBody() {
        request.setSelectedSignoffDepartmentIds(List.of(101L,101L));
        assertThrows(IllegalArgumentException.class,() -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
    }
    @Test void sourceFailureDoesNotInsertOrReleaseCheckout() {
        when(sourceOwnershipService.createVerifiedCopy(100L)).thenThrow(new IllegalStateException("source offline"));
        assertEquals("source offline", assertThrows(IllegalStateException.class, () -> service.createRevision(99L,20L,21L,request)).getMessage());
        verify(fileMapper, never()).insert(any(DccControlledFileDO.class));
    }
    @Test void changedSelectedHashCleansCopyAndRejects() {
        selected.setSourceSha256("different");
        assertThrows(IllegalStateException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
        verify(fileMapper, never()).insert(any(DccControlledFileDO.class));
    }
    @Test void childFailureCleansFrozenCopyAndPropagates() {
        doThrow(new IllegalStateException("child failure")).when(relatedFileService).inheritRelatedFiles(21L,30L);
        assertThrows(IllegalStateException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService).cleanupPreparedSource(frozen);
    }
    @Test void wrongTenantRejectedBeforeSource() {
        baseline.setTenantId(2L);
        assertThrows(RuntimeException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void replacementRequiresFormalOwner() {
        request.setRevisionChangeType("REPLACEMENT");
        doThrow(new IllegalStateException("not OWNER")).when(projectAccessService).assertProjectOwner(99L,5L);
        assertThrows(IllegalStateException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void partialRequiresFormalEditor() {
        doThrow(new IllegalStateException("not EDIT")).when(projectAccessService).assertProjectEditorOrOwner(99L,5L);
        assertThrows(IllegalStateException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void checkoutAndInFlightApprovalBlockRevision() {
        selected.setCheckedOutBy(null);
        when(checkoutMapper.selectActiveByMasterId(1L,10L)).thenReturn(
                DccControlledFileCheckoutDO.builder().masterId(10L).baseIterationId(22L).actorId(88L).status("ACTIVE").build());
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> service.createRevision(99L,20L,21L,request),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ALREADY_CHECKED_OUT, 88L);
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
        verify(nameClaimService,never()).claimExistingVersion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
        verify(nameClaimService,never()).claimIdentity(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());verifyNoInteractions(relatedFileService);
        verify(fileMapper, never()).insert(any(DccControlledFileDO.class));
    }
    @Test void pendingApprovalCannotBeEdited() {
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline, selected, file(22L,"A/9-2","APPROVING")));
        assertThrows(RuntimeException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService, never()).createVerifiedCopy(anyLong());
    }
    @Test void newSubmissionInitialFactAndHistoricalProtection() {
        var fresh = file(null,"A/1","DRAFT"); fresh.setChangeType("NEW");
        service.recordInitialIntent(fresh); assertEquals("INITIAL", fresh.getRevisionChangeType());
        fresh.setId(1L); assertThrows(IllegalArgumentException.class, () -> service.recordInitialIntent(fresh));
    }
    @Test void ownerMayFormallyReplaceAnotherRequestersSelectedWorkingBody() {
        request.setRevisionChangeType("REPLACEMENT"); selected.setRequesterId(88L);
        var result = service.createRevision(99L,20L,21L,request);
        assertEquals(99L,result.getRequesterId()); assertEquals("REPLACEMENT",result.getRevisionChangeType());
        assertEquals(21L,result.getSelectedIterationControlledFileId());
        verify(projectAccessService).assertProjectOwner(99L,5L);
    }
    @Test void changedSignoffPayloadIsNotAnIdempotentReplay() {
        var first=service.createRevision(99L,20L,21L,request);
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline,selected,first));
        request.setSelectedSignoffUserIds(List.of(123L));
        assertThrows(IllegalArgumentException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,times(1)).createVerifiedCopy(100L);
    }
    @Test void formallyControlledFutureVersionCanBeTheExplicitRevisionSource() {
        baseline.setVersionNo("B/1"); baseline.setStatus("CONTROLLED_PENDING_EFFECTIVE"); selected.setVersionNo("B/1-1");
        var result=service.createRevision(99L,20L,21L,request);
        assertEquals("B/2",result.getVersionNo()); assertEquals(20L,result.getRevisionSourceControlledFileId());
        assertEquals("B/1",result.getRevisionSourceVersionNo());
        verify(masterMapper,never()).updateById(any(DccControlledFileMasterDO.class));
    }
    @Test void formalCreationReusesCompleteSourceAndNumberClaimInItsTransaction() {
        service.createRevision(99L,20L,21L,request);
        verify(nameClaimService).claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,21L);
    }
    @Test void reservationFailureCannotPrepareOrPersistFormalBody() {
        doThrow(new IllegalStateException("identity already reserved")).when(nameClaimService)
                .claimExistingVersion(1L,"SOP.pdf",5L,6L,"SOP-001",10L,21L);
        assertThrows(IllegalStateException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
        verify(fileMapper,never()).insert(any(DccControlledFileDO.class));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"project", "category", "taxonomy", "number", "sourceName"})
    void selectedBodyMustMatchTheControlledSourcesLogicalIdentity(String changed) {
        switch(changed) {
            case "project" -> selected.setDccProjectCodeId(55L);
            case "category" -> selected.setCategoryId(22L);
            case "taxonomy" -> selected.setFileTypeTaxonomyId(66L);
            case "number" -> selected.setFileNumber("OTHER-001");
            case "sourceName" -> selected.setSourceOriginalFileName("sop.pdf");
        }
        assertThrows(RuntimeException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
        verify(fileMapper,never()).insert(any(DccControlledFileDO.class));
    }
    @Test void sourceIdentityMustMatchTheLockedFormalMasterBeforeAuthorization() {
        baseline.setDccProjectCodeId(55L); selected.setDccProjectCodeId(55L);
        assertThrows(RuntimeException.class, () -> service.createRevision(99L,20L,21L,request));
        verify(projectAccessService,never()).assertProjectEditorOrOwner(anyLong(),anyLong());
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
    }
    @Test void failedPartialTargetDoesNotBlockDistinctReplacementNumber() {
        baseline.setVersionNo("A/1"); selected.setVersionNo("A/1-1"); request.setRevisionChangeType("REPLACEMENT");
        var failed=file(23L,"A/2","REJECTED"); failed.setRevisionChangeType("PARTIAL");
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline,selected,failed));
        assertEquals("B/1",service.createRevision(99L,20L,21L,request).getVersionNo());
    }
    @Test void failedTargetNumberIsNotReusedWithoutConfirmedPolicy() {
        baseline.setVersionNo("A/1"); selected.setVersionNo("A/1-1");
        var failed=file(23L,"A/2","REJECTED"); failed.setRevisionChangeType("PARTIAL");
        when(fileMapper.selectListByMasterIdForUpdate(10L)).thenReturn(List.of(baseline,selected,failed));
        assertThrows(IllegalArgumentException.class,() -> service.createRevision(99L,20L,21L,request));
        verify(sourceOwnershipService,never()).createVerifiedCopy(anyLong());
    }
    private DccControlledFileDO file(Long id,String version,String status) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L)
                .dccProjectCodeId(5L).fileTypeTaxonomyId(6L).fileNumber("SOP-001").fileName("template title")
                .sourceOriginalFileName("SOP.pdf").sourceFileId(100L).sourceSha256("selected-hash")
                .requesterId(99L).submitterId(99L).versionNo(version).status(status)
                .controlledTime("WORKING".equals(status) ? null : java.time.LocalDateTime.of(2026,10,1,12,0)).build();
    }
}
