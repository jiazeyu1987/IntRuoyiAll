package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class DccControlledFileDetailAuthorizationGuardTest extends BaseMockitoUnitTest {
    @Mock private DccProjectReferenceReadAccess projectReferenceReadAccess;
    @Mock private DccArchiveAccessService archiveAccess;
    @Test void archivedHistoryIntentDoesNotGrantFormerApproverBodyRead() {
        g07Actor();var file=g07File("OBSOLETE");
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
            ()->guard.requireBodyRead(99L,file,g07Context(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,"round-exact","historic-task")));
    }
    @Test void archiveCannotBeReadThroughRequesterOrManagerShortcuts() {
        var file=activeFile();file.setStatus("OBSOLETE");file.setRequesterId(99L);
        assertFalse(guard.isAllowed(99L,file,true,null));
    }
    @Test void archiveDocumentControlReadsWithoutOriginalProjectAssignment() {
        var file=activeFile();file.setStatus("SUPERSEDED");
        when(archiveAccess.canRead(99L,file)).thenReturn(true);
        assertTrue(guard.isAllowed(99L,file,false,null));
        assertTrue(guard.canReadRetainedVersion(99L,file,null));
    }


    @Mock
    private DccControlledFileAssignmentScopeService assignmentScopeService;
    @Mock
    private DccDirectoryAccessPermissionService directoryAccessPermissionService;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccControlledFileViewMatrixAccessService viewMatrixAccessService;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Mock
    private BpmTaskService bpmTaskService;
    @Mock
    private DccOfflineTrainingRecordService offlineTraining;
    @Mock private cn.iocoder.yudao.module.system.api.user.AdminUserApi adminUserApi;
    @Mock private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccessService;
    @Mock private cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService bpmProcessInstanceService;
    @Mock private DccBoundWorkflowContractResolver boundWorkflowContractResolver;
    @Mock private DccApplicationRoundService applicationRoundService;
    @Mock private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper masterMapper;

    @Test
    void completedTrainingMetadataGrantStillRespectsHardFileScope() {
        var file = activeFile();
        when(assignmentScopeService.isWithinAssignedFileScope(99L,910L)).thenReturn(true);
        when(offlineTraining.canReadProgress(99L,file)).thenReturn(true);
        assertTrue(guard.isAllowed(99L,file,false,null));
        when(assignmentScopeService.isWithinAssignedFileScope(99L,910L)).thenReturn(false);
        assertFalse(guard.isAllowed(99L,file,false,null));
    }

    @InjectMocks
    private DccControlledFileDetailAuthorizationGuard guard;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"ACTIVE", "CONTROLLED_PENDING_EFFECTIVE"})
    void controlledFileWithoutAnyFileScopeIsDenied(String status) {
        TenantContextHolder.setTenantId(31L);
        DccControlledFileDO file = activeFile();
        file.setStatus(status);
        when(assignmentScopeService.isWithinAssignedFileScope(99L, 910L)).thenReturn(true);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(false);
        when(directoryAccessPermissionService.getAuthorizedDirectoryIds(eq(99L), any())).thenReturn(Set.of());
        if ("ACTIVE".equals(status)) {
            when(distributionRecipientMapper.countActiveElectronicRecipientAccess(31L, 910L, 99L)).thenReturn(0L);
        }

        assertFalse(guard.isAllowed(99L, file));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"ACTIVE", "CONTROLLED_PENDING_EFFECTIVE"})
    void controlledFileWithViewMatrixScopeIsAllowed(String status) {
        TenantContextHolder.setTenantId(31L);
        DccControlledFileDO file = activeFile();
        file.setStatus(status);
        when(assignmentScopeService.isWithinAssignedFileScope(99L, 910L)).thenReturn(true);
        when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(true);

        assertTrue(guard.isAllowed(99L, file));
    }

    @AfterEach
    void clearTenantContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    private void g07Actor() {
        TenantContextHolder.setTenantId(31L);
        var login = new cn.iocoder.yudao.framework.security.core.LoginUser();
        login.setId(99L); login.setTenantId(31L); login.setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login, null, java.util.List.of()));
        var user = new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO();
        user.setId(99L); user.setTenantId(31L); user.setStatus(0);
        org.mockito.Mockito.lenient().when(adminUserApi.getUser(99L)).thenReturn(user);
        org.mockito.Mockito.lenient().when(assignmentScopeService.isWithinAssignedFileScope(99L, 910L)).thenReturn(true);
        org.mockito.Mockito.lenient().when(projectAccessService.listReadableProjectIds(99L)).thenReturn(java.util.List.of(300L));
    }

    private DccControlledFileDO g07File(String status) {
        var file = activeFile(); file.setTenantId(31L); file.setStatus(status); file.setMasterId(700L);
        file.setDccProjectCodeId(300L); file.setVersionNo("A/1-2"); file.setRequesterId(99L);
        file.setSourceFileId(900L); file.setSourceSha256("sha-precise"); file.setFileTypeTaxonomyId(66L);
        file.setFileNumber("SOP-001"); file.setActivatedTime(java.time.LocalDateTime.of(2026,10,1,10,0));
        return file;
    }

    private DccControlledFileBodyReadContext g07Context(DccControlledFileBodyReadContext.Intent intent, String round, String task) {
        return new DccControlledFileBodyReadContext(intent, "910", "A/1-2", round == null ? null : "REVISION", round, task);
    }

    private void g07Master() {
        var master = cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder()
                .id(700L).tenantId(31L).dccProjectCodeId(300L).categoryId(10L).fileTypeTaxonomyLeafId(66L)
                .normalizedFileNumber("SOP-001").build();
        when(masterMapper.selectById(700L)).thenReturn(master);
    }

    private DccControlledFileDO pendingImport() {
        var file=g07File("CONTROLLED_PENDING_EFFECTIVE");
        file.setActivatedTime(null); file.setImportSource(DccControlledFileHistoricalImportService.SOURCE);
        file.setEffectiveMode(DccControlledFileHistoricalImportService.SOURCE); file.setImportedBy(99L);
        file.setImportedTime(java.time.LocalDateTime.of(2026,11,2,12,0));
        file.setControlledTime(java.time.LocalDateTime.of(2026,10,1,12,0));
        file.setEffectiveTime(java.time.LocalDateTime.of(2099,1,1,0,0));
        file.setReadOnlyFileId(901L); file.setPublishedFileId(901L);
        return file;
    }

    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO pendingImportMaster() {
        var master=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO.builder()
                .id(700L).tenantId(31L).dccProjectCodeId(300L).categoryId(10L).fileTypeTaxonomyLeafId(66L)
                .normalizedFileNumber("SOP-001").latestControlledFileId(910L).build();
        org.mockito.Mockito.lenient().when(masterMapper.selectById(700L)).thenReturn(master); return master;
    }

    @Test void futureImportCanBeInspectedWithViewGrantButNeverAsDailyBody() {
        g07Actor(); var file=pendingImport(); pendingImportMaster();
        when(permissionSupport.hasFilePermission(file,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.VIEW)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireBodyRead(99L,file,
                g07Context(DccControlledFileBodyReadContext.Intent.VERSION_HISTORY,null,null)));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L,file,DccControlledFileBodyReadContext.daily(910L)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"native","noView","wrongLatest","hasActive","assignment","project","tenant","masterIdentity"})
    void pendingImportHistoryRejectsOtherOriginsAndScopes(String reason) {
        g07Actor(); var file=pendingImport();
        var master=pendingImportMaster();
        org.mockito.Mockito.lenient().when(permissionSupport.hasFilePermission(file,99L,
                cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.VIEW)).thenReturn(!reason.equals("noView"));
        switch(reason) {
            case "native" -> file.setImportSource(null);
            case "wrongLatest" -> master.setLatestControlledFileId(911L);
            case "hasActive" -> master.setCurrentActiveControlledFileId(911L);
            case "assignment" -> when(assignmentScopeService.isWithinAssignedFileScope(99L,910L)).thenReturn(false);
            case "project" -> when(projectAccessService.listReadableProjectIds(99L)).thenReturn(java.util.List.of());
            case "tenant" -> TenantContextHolder.setTenantId(32L);
            case "masterIdentity" -> master.setFileTypeTaxonomyLeafId(67L);
        }
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L,file,g07Context(DccControlledFileBodyReadContext.Intent.VERSION_HISTORY,null,null)));
    }

    @Test void importMarkerWithoutStoredProvenanceIsAnExplicitDataError() {
        g07Actor(); var file=pendingImport(); file.setImportedBy(null);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> guard.requireBodyRead(99L,file,g07Context(DccControlledFileBodyReadContext.Intent.VERSION_HISTORY,null,null)));
    }

    private void g07Round(DccControlledFileDO file, boolean active) {
        file.setProcessInstanceId("round-exact");
        var history = org.mockito.Mockito.mock(org.flowable.engine.history.HistoricProcessInstance.class);
        when(bpmProcessInstanceService.getHistoricProcessInstance("round-exact")).thenReturn(history);
        when(history.getId()).thenReturn("round-exact"); when(history.getTenantId()).thenReturn("31");
        when(history.getProcessDefinitionId()).thenReturn("definition-exact"); when(history.getStartUserId()).thenReturn("99");
        org.mockito.Mockito.lenient().when(history.getBusinessKey()).thenReturn("910");
        if (active) {
            var runtime = org.mockito.Mockito.mock(org.flowable.engine.runtime.ProcessInstance.class);
            when(bpmProcessInstanceService.getProcessInstance("round-exact")).thenReturn(runtime);
            when(runtime.getId()).thenReturn("round-exact"); when(runtime.getTenantId()).thenReturn("31");
            when(runtime.getProcessDefinitionId()).thenReturn("definition-exact"); when(runtime.getBusinessKey()).thenReturn("910");
        }
        when(applicationRoundService.require("REVISION", 910L, "round-exact")).thenReturn(2);
        var flow = new DccBoundWorkflowContractResolver.RoundFlowContract(31L, 910L, "REVISION", 2, "round-exact",
                DccControlledFileProcessDefinitionKeys.REVISION, "definition-exact", "deployment-exact", "digest-exact",
                "PUBLIC_MATRIX_NATIVE_V1", DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL,
                false, null, null, DccBoundWorkflowContractResolver.RoundContractStatus.READY, null);
        when(boundWorkflowContractResolver.resolveRoundFlowContract(eq(file), any(DccApplicationRoundSummary.class))).thenReturn(flow);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"WORKING","READY_TO_PUBLISH","CONTROLLED_PENDING_EFFECTIVE","PENDING_MATRIX_REVIEW"})
    void g07_dailyDeniedEvenForRequesterWithAllBodyAndManagementGrants(String status) {
        g07Actor();
        var file = g07File(status);
        org.mockito.Mockito.lenient().when(directoryAccessPermissionService.hasDirectoryManagementPermission(99L)).thenReturn(true);
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> guard.requireBodyRead(99L, file, DccControlledFileBodyReadContext.daily(910L)),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
    }

    @Test void g07_activeDailyRequiresActualActivationAndCurrentTenantActor() {
        g07Actor(); var file = g07File("ACTIVE");
        when(permissionSupport.hasFilePermission(file, 99L, cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.VIEW)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireBodyRead(99L, file, DccControlledFileBodyReadContext.daily(910L)));
        file.setActivatedTime(null);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L, file, DccControlledFileBodyReadContext.daily(910L)));
        file.setActivatedTime(java.time.LocalDateTime.now()); file.setTenantId(32L);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L, file, DccControlledFileBodyReadContext.daily(910L)));
    }

    @Test void dailyProjectViewerWithoutTypeViewCannotReadBodyEvenAsRequester() {
        g07Actor(); var file = g07File("ACTIVE");
        cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException(
                () -> guard.requireBodyRead(99L, file, DccControlledFileBodyReadContext.daily(910L)),
                cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED);
        org.mockito.Mockito.verify(permissionSupport).hasFilePermission(file, 99L,
                cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.VIEW);
        org.mockito.Mockito.verify(viewMatrixAccessService).canAccessCurrentViewMatrix(99L, file);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void dailyBodyAcceptsActualFileViewOrExplicitLegacyViewMatrix(boolean fileView) {
        g07Actor(); var file = g07File("ACTIVE");
        when(permissionSupport.hasFilePermission(file, 99L,
                cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.VIEW)).thenReturn(fileView);
        if (!fileView) when(viewMatrixAccessService.canAccessCurrentViewMatrix(99L, file)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> guard.requireBodyRead(99L, file, DccControlledFileBodyReadContext.daily(910L)));
        if (fileView) org.mockito.Mockito.verifyNoInteractions(viewMatrixAccessService);
    }

    @Test void g07_unsubmittedAuthoringUsesFileEditAndFormalMasterScope() {
        g07Actor(); g07Master(); var file = g07File("WORKING"); file.setActivatedTime(null);
        when(permissionSupport.hasFilePermission(file,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(true);
        var context = g07Context(DccControlledFileBodyReadContext.Intent.AUTHORING,null,null);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireBodyRead(99L,file,context));
        file.setRequesterId(88L);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireBodyRead(99L,file,context));
        when(permissionSupport.hasFilePermission(file,99L,cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(false);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L,file,context));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"boundRound","controlled","outsideScope","wrongMaster","missingSource"})
    void unsubmittedAuthoringEditDoesNotBypassBodyIdentity(String badFact) {
        g07Actor(); if (!"outsideScope".equals(badFact)) g07Master(); var file=g07File("WORKING"); file.setActivatedTime(null); file.setRequesterId(88L);
        org.mockito.Mockito.lenient().when(permissionSupport.hasFilePermission(file,99L,
                cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(true);
        switch(badFact) {
            case "boundRound" -> file.setProcessInstanceId("formal-round");
            case "controlled" -> file.setControlledTime(java.time.LocalDateTime.now());
            case "outsideScope" -> when(assignmentScopeService.isWithinAssignedFileScope(99L,910L)).thenReturn(false);
            case "wrongMaster" -> file.setFileNumber("OTHER");
            case "missingSource" -> file.setSourceSha256(null);
        }
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L,file,g07Context(DccControlledFileBodyReadContext.Intent.AUTHORING,null,null)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"foreignTenant","otherRound","otherDefinition","otherUser","suspended","outsideScope"})
    void g07_approvalUsesExactServerTaskAndRejectsChangedFacts(String badFact) {
        g07Actor(); var file=g07File("PENDING_MATRIX_REVIEW"); g07Round(file,true);
        var task=org.mockito.Mockito.mock(org.flowable.task.api.Task.class);
        when(bpmTaskService.getTask("task-exact")).thenReturn(task);
        org.mockito.Mockito.lenient().when(task.getId()).thenReturn("task-exact");
        org.mockito.Mockito.lenient().when(task.getTenantId()).thenReturn("31");
        org.mockito.Mockito.lenient().when(task.getProcessInstanceId()).thenReturn("round-exact");
        org.mockito.Mockito.lenient().when(task.getProcessDefinitionId()).thenReturn("definition-exact");
        org.mockito.Mockito.lenient().when(task.getAssignee()).thenReturn("99");
        var context=g07Context(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,"round-exact","task-exact");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireBodyRead(99L,file,context));
        switch(badFact) {
            case "foreignTenant" -> when(task.getTenantId()).thenReturn("32");
            case "otherRound" -> when(task.getProcessInstanceId()).thenReturn("other-round");
            case "otherDefinition" -> when(task.getProcessDefinitionId()).thenReturn("other-definition");
            case "otherUser" -> when(task.getAssignee()).thenReturn("88");
            case "suspended" -> when(task.isSuspended()).thenReturn(true);
            case "outsideScope" -> when(assignmentScopeService.isWithinAssignedFileScope(99L,910L)).thenReturn(false);
        }
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L,file,context));
    }

    @Test void g07_historyRetainsSelectedRoundAndRequiresItsRealParticipant() {
        g07Actor(); var file=g07File("ACTIVE"); g07Round(file,false);
        file.setProcessInstanceId("new-round");
        var task=org.mockito.Mockito.mock(org.flowable.task.api.history.HistoricTaskInstance.class);
        when(bpmTaskService.getHistoricTask("historic-task")).thenReturn(task);
        when(task.getId()).thenReturn("historic-task"); when(task.getEndTime()).thenReturn(new java.util.Date());
        when(task.getTenantId()).thenReturn("31"); when(task.getProcessInstanceId()).thenReturn("round-exact");
        when(task.getProcessDefinitionId()).thenReturn("definition-exact"); when(task.getAssignee()).thenReturn("99");
        var context=g07Context(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,"round-exact","historic-task");
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireBodyRead(99L,file,context));
        when(task.getAssignee()).thenReturn("88");
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> guard.requireBodyRead(99L,file,context));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> guard.requireBodyRead(99L,file,
                g07Context(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,"round-exact",null)));
    }

    @Test void g07_trainingHasItsOwnBoundRoundGrantAndCannotBeUsedForDailyRead() {
        g07Actor();var file=g07File("PENDING_APPLICANT_TRAINING_RECORD");
        file.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION);g07Round(file,true);
        when(offlineTraining.canReadTrainingBody(99L,file)).thenReturn(true);
        var context=g07Context(DccControlledFileBodyReadContext.Intent.TRAINING_APPLICATION,"round-exact",null);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(()->guard.requireBodyRead(99L,file,context));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->guard.requireBodyRead(99L,file,DccControlledFileBodyReadContext.daily(910L)));
        when(offlineTraining.canReadTrainingBody(99L,file)).thenReturn(false);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                ()->guard.requireBodyRead(99L,file,context));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"crossFile","otherVersion","otherRound","otherUser","foreignTenant","endpointRound"})
    void applicationRoundQueryUsesTheExactTaskAuthorityAndRejectsForgedContexts(String badFact) {
        g07Actor(); var file=g07File("PENDING_SUPERVISOR_REVIEW"); g07Round(file,true);
        var task=org.mockito.Mockito.mock(org.flowable.task.api.Task.class);
        when(bpmTaskService.getTask("task-exact")).thenReturn(task);
        when(task.getId()).thenReturn("task-exact"); when(task.getTenantId()).thenReturn("31");
        when(task.getProcessInstanceId()).thenReturn("round-exact");
        when(task.getProcessDefinitionId()).thenReturn("definition-exact"); when(task.getAssignee()).thenReturn("99");
        var query=new DccControlledFileQueryServiceImpl();
        var files=org.mockito.Mockito.mock(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper.class);
        var evidence=org.mockito.Mockito.mock(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper.class);
        when(files.selectById(910L)).thenReturn(file);
        when(evidence.selectApplicationRounds(31L,300L,910L)).thenReturn(java.util.List.of());
        org.springframework.test.util.ReflectionTestUtils.setField(query,"controlledFileMapper",files);
        org.springframework.test.util.ReflectionTestUtils.setField(query,"applicationEvidenceMapper",evidence);
        org.springframework.test.util.ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        var context=g07Context(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,"round-exact","task-exact");
        org.junit.jupiter.api.Assertions.assertTrue(query.listApplicationRounds(99L,910L,context).isEmpty());
        org.springframework.test.util.ReflectionTestUtils.setField(query,"signatureMapper",
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper.class));
        org.junit.jupiter.api.Assertions.assertFalse(query.getApplicationEvidence(99L,910L,"REVISION","round-exact",context).recorded());
        org.mockito.Mockito.clearInvocations(evidence);
        var wrongContext=switch(badFact) {
            case "crossFile" -> new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                    "911","A/1-2","REVISION","round-exact","task-exact");
            case "otherVersion" -> new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                    "910","A/9","REVISION","round-exact","task-exact");
            case "otherRound" -> g07Context(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,"foreign-round","task-exact");
            default -> context;
        };
        if("otherUser".equals(badFact)) when(task.getAssignee()).thenReturn("88");
        if("foreignTenant".equals(badFact)) file.setTenantId(32L);
        if("endpointRound".equals(badFact)) {
            org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> query.getApplicationEvidence(99L,910L,"REVISION","foreign-round",wrongContext));
        } else {
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> query.listApplicationRounds(99L,910L,wrongContext));
        }
        org.mockito.Mockito.verifyNoInteractions(evidence);
    }

    private DccControlledFileDO activeFile() {
        return DccControlledFileDO.builder()
                .id(910L).categoryId(10L).directoryId(20L).requesterId(88L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus()).publishedFileId(911L).build();
    }
}
