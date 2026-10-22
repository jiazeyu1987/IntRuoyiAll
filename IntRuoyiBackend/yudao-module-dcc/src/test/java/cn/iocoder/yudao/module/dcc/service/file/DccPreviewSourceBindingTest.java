package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.file.access.DccBusinessFileAccessProvider;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenIssueCommand;
import cn.iocoder.yudao.module.dcc.service.token.DccPreviewSourceIdentity;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real public metadata -> signed token -> binary calls; persistence/BPM/storage are isolated ports. */
class DccPreviewSourceBindingTest {
    private static final Long TENANT = 31L, ACTOR = 99L, FILE = 910L, F1 = 100L, F2 = 200L;
    private static final String VERSION = "A/2", ROUND = "r3-round", TASK = "r3-task";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T03:00:00Z"), ZoneOffset.UTC);
    private static final DccRequestAuditContext AUDIT = new DccRequestAuditContext(
            "127.0.0.1", "R3 source-binding isolated test", "R3-source-binding");

    private final DccControlledFileQueryServiceImpl query = new DccControlledFileQueryServiceImpl();
    private final DccViewerTokenService tokens = new DccViewerTokenService();
    private final DccControlledPreviewAccessService previews = new DccControlledPreviewAccessService();
    private final DccControlledFileMapper files = mock(DccControlledFileMapper.class);
    private final FileMapper binaries = mock(FileMapper.class);
    private final FileService storage = mock(FileService.class);
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final DccControlledFileAssignmentScopeService assignments = mock(DccControlledFileAssignmentScopeService.class);
    private final DccDirectoryAccessPermissionService directories = mock(DccDirectoryAccessPermissionService.class);
    private final DccProjectAccessService projects = mock(DccProjectAccessService.class);
    private final BpmTaskService tasks = mock(BpmTaskService.class);
    private final BpmProcessInstanceService processes = mock(BpmProcessInstanceService.class);
    private final DccApplicationRoundService rounds = mock(DccApplicationRoundService.class);
    private final DccBoundWorkflowContractResolver contracts = mock(DccBoundWorkflowContractResolver.class);
    private final DccControlledFileAccessEventMapper events = mock(DccControlledFileAccessEventMapper.class);
    private final DccControlledFileWatermarkTraceMapper traces = mock(DccControlledFileWatermarkTraceMapper.class);
    private final DccControlledFileAccessLogMapper logs = mock(DccControlledFileAccessLogMapper.class);
    private final BusinessFileAccessService businessFiles = mock(BusinessFileAccessService.class);
    private final DccControlledPreviewWatermarkService watermarks = mock(DccControlledPreviewWatermarkService.class);
    private final Map<Long, FileDO> artifacts = new HashMap<>();
    private final AdminUserRespDTO actor = new AdminUserRespDTO();
    private final Task task = mock(Task.class);
    private DccControlledFileDO selected;
    private DccControlledFileBodyReadContext context;
    private DccControlledFileAccessEventDO savedEvent;
    private DccControlledFileWatermarkTraceDO savedTrace;
    private FileDO original;

    @BeforeEach void wireRealQueryPreviewAuditAndHmacToken() throws Exception {
        TenantContextHolder.setTenantId(TENANT);
        var login = new LoginUser(); login.setId(ACTOR); login.setTenantId(TENANT); login.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        actor.setId(ACTOR); actor.setTenantId(TENANT); actor.setStatus(0);
        when(users.getUser(ACTOR)).thenReturn(actor);
        when(assignments.isWithinAssignedFileScope(ACTOR, FILE)).thenReturn(true);
        when(projects.listReadableProjectIds(ACTOR)).thenReturn(List.of(300L));
        when(directories.getAuthorizedDirectoryIds(ACTOR, DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(20L));
        when(files.selectById(FILE)).thenAnswer(call -> selected);
        when(binaries.selectById(anyLong())).thenAnswer(call -> artifacts.get(call.getArgument(0)));
        when(watermarks.build(eq(ACTOR), eq("preview"), nullable(String.class))).thenReturn(
                DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());

        var guard = new DccControlledFileDetailAuthorizationGuard();
        ReflectionTestUtils.setField(guard, "assignmentScopeService", assignments);
        ReflectionTestUtils.setField(guard, "adminUserApi", users);
        ReflectionTestUtils.setField(guard, "projectAccessService", projects);
        ReflectionTestUtils.setField(guard, "bpmTaskService", tasks);
        ReflectionTestUtils.setField(guard, "bpmProcessInstanceService", processes);
        ReflectionTestUtils.setField(guard, "applicationRoundService", rounds);
        ReflectionTestUtils.setField(guard, "boundWorkflowContractResolver", contracts);
        ReflectionTestUtils.setField(query, "detailAuthorizationGuard", guard);
        ReflectionTestUtils.setField(query, "controlledFileMapper", files);
        ReflectionTestUtils.setField(query, "fileMapper", binaries);
        ReflectionTestUtils.setField(query, "fileService", storage);
        ReflectionTestUtils.setField(query, "assignmentScopeService", assignments);
        ReflectionTestUtils.setField(query, "directoryAccessPermissionService", directories);
        ReflectionTestUtils.setField(query, "projectAccessService", projects);
        ReflectionTestUtils.setField(query, "accessEventMapper", events);
        ReflectionTestUtils.setField(query, "watermarkTraceMapper", traces);
        ReflectionTestUtils.setField(query, "accessLogMapper", logs);
        ReflectionTestUtils.setField(query, "watermarkService", watermarks);

        // The same real token service issues metadata and verifies binary evidence; no mocked verify success.
        ReflectionTestUtils.setField(tokens, "hmacSecret", "r3-test-explicit-hmac-secret-at-least-32-characters");
        ReflectionTestUtils.setField(tokens, "clock", CLOCK);
        ReflectionTestUtils.setField(query, "viewerTokenService", tokens);
        var audit = new DccControlledFileAccessAuditService();
        ReflectionTestUtils.setField(audit, "accessEventMapper", events);
        ReflectionTestUtils.setField(audit, "watermarkTraceMapper", traces);
        ReflectionTestUtils.setField(audit, "accessLogMapper", logs);
        ReflectionTestUtils.setField(previews, "accessAuditService", audit);
        ReflectionTestUtils.setField(previews, "viewerTokenService", tokens);
        ReflectionTestUtils.setField(previews, "clock", CLOCK);
        ReflectionTestUtils.setField(query, "previewAccessService", previews);

        when(events.insert(any(DccControlledFileAccessEventDO.class))).thenAnswer(call -> {
            savedEvent = call.getArgument(0); savedEvent.setId(800L); return 1;
        });
        when(events.selectById(800L)).thenAnswer(call -> savedEvent);
        when(events.selectOne(any())).thenAnswer(call -> savedEvent);
        when(traces.insert(any(DccControlledFileWatermarkTraceDO.class))).thenAnswer(call -> {
            savedTrace = call.getArgument(0); savedTrace.setId(801L); return 1;
        });
        when(traces.selectOne(any())).thenAnswer(call -> savedTrace);
        when(logs.insert(any(DccControlledFileAccessLogDO.class))).thenReturn(1);
        var provider = new DccBusinessFileAccessProvider(query, files, null, null);
        var reference = new BusinessFileAccessReference("dcc", "DCC_CONTROLLED_FILE", FILE, VERSION, TENANT, null);
        when(businessFiles.assertAllowed(any(BusinessFileAccessRequest.class))).thenAnswer(call -> {
            provider.assertAllowed(call.getArgument(0), reference); return Optional.of(reference);
        });
        ReflectionTestUtils.setField(query, "businessFileAccessService", businessFiles);
    }

    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); TenantContextHolder.clear(); }

    static Stream<Arguments> previewCases() {
        return Stream.of(Arguments.of("DAILY", "pdf", "application/pdf"), Arguments.of("DAILY", "png", "image/png"),
                Arguments.of("APPROVAL_TASK", "pdf", "application/pdf"), Arguments.of("APPROVAL_TASK", "png", "image/png"));
    }

    static Stream<Arguments> descriptorCases() {
        return previewCases().flatMap(row -> {
            var values = row.get();
            return Stream.of("NAME", "TYPE", "SIZE", "NAME_NULL", "TYPE_NULL")
                    .map(field -> Arguments.of(values[0], values[1], values[2], field));
        });
    }

    static Stream<Arguments> legacyTokenExits() {
        return previewCases().flatMap(row -> {
            var values = row.get();
            return Stream.of("BINARY", "READING")
                    .map(exit -> Arguments.of(values[0], values[1], values[2], exit));
        });
    }

    @ParameterizedTest @MethodSource("previewCases")
    void unchangedSourceReadsOnlyTheMetadataSelectedFile(String intent, String extension, String contentType) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        assertArrayEquals(bytes("F1"), read(metadata).bytes());
        verify(storage).getFileContent(1L, "r3/F1." + extension);
        verify(storage, times(1)).getFileContent(anyLong(), anyString());
        verify(events, times(1)).insert(any(DccControlledFileAccessEventDO.class));
    }

    @ParameterizedTest @MethodSource("previewCases")
    void sameVersionRepointedArtifactCannotReadF2WithF1Metadata(String intent, String extension, String contentType) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        artifacts.put(F2, artifact(F2, 2L, "r3/F2." + extension, contentType));
        if ("DAILY".equals(intent)) selected.setPublishedFileId(F2); else selected.setSourceFileId(F2);
        when(storage.getFileContent(2L, "r3/F2." + extension)).thenReturn(bytes("F2"));
        assertSourceMismatchBeforeStorage(metadata);
    }

    @ParameterizedTest @MethodSource("previewCases")
    void sameFileIdChangedConfigCannotReadNewLocationWithOldMetadata(String intent, String extension, String contentType) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        original.setConfigId(2L);
        when(storage.getFileContent(2L, "r3/F1." + extension)).thenReturn(bytes("other-config"));
        assertSourceMismatchBeforeStorage(metadata);
    }

    @ParameterizedTest @MethodSource("previewCases")
    void sameFileIdChangedPathCannotReadNewLocationWithOldMetadata(String intent, String extension, String contentType) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        original.setPath("r3/replaced." + extension);
        when(storage.getFileContent(1L, "r3/replaced." + extension)).thenReturn(bytes("other-path"));
        assertSourceMismatchBeforeStorage(metadata);
    }

    @ParameterizedTest @MethodSource("descriptorCases")
    void sameFileIdChangedDescriptorCannotReuseOldMetadata(String intent, String extension, String contentType, String field) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        switch (field) {
            case "NAME" -> original.setName("renamed." + extension);
            case "TYPE" -> original.setType("application/octet-stream");
            case "SIZE" -> original.setSize(original.getSize() + 1L);
            case "NAME_NULL" -> original.setName(null);
            case "TYPE_NULL" -> original.setType(null);
            default -> fail("undeclared descriptor case");
        }
        assertSourceMismatchBeforeStorage(metadata);
    }

    @ParameterizedTest
    @CsvSource({"DAILY,DISABLED", "DAILY,OUTSIDE_SCOPE", "APPROVAL_TASK,DISABLED", "APPROVAL_TASK,OUTSIDE_SCOPE", "APPROVAL_TASK,REASSIGNED"})
    void currentPermissionRevocationStillDeniesBeforeAnyStorage(String intent, String revocation) throws Exception {
        var metadata = metadata(intent, "pdf", "application/pdf");
        switch (revocation) {
            case "DISABLED" -> actor.setStatus(1);
            case "OUTSIDE_SCOPE" -> when(assignments.isWithinAssignedFileScope(ACTOR, FILE)).thenReturn(false);
            case "REASSIGNED" -> when(task.getAssignee()).thenReturn("101");
            default -> fail("undeclared revocation case");
        }
        assertServiceException(() -> read(metadata), CONTROLLED_FILE_ACCESS_DENIED);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest @MethodSource("previewCases")
    void sourceBoundReadingUsesSameSourceAndDoesNotReadBytesOrIssueAnotherEvent(String intent, String extension, String contentType) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        assertDoesNotThrow(() -> readAccess(metadata));
        verifyNoInteractions(storage);
        verify(events, times(1)).insert(any(DccControlledFileAccessEventDO.class));
        verify(traces, times(1)).insert(any(DccControlledFileWatermarkTraceDO.class));
    }

    @ParameterizedTest @MethodSource("previewCases")
    void sourceBoundReadingRejectsSourceSwapBeforeAnyBytes(String intent, String extension, String contentType) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        artifacts.put(F2, artifact(F2, 2L, "r3/F2." + extension, contentType));
        if ("DAILY".equals(intent)) selected.setPublishedFileId(F2); else selected.setSourceFileId(F2);
        assertServiceException(() -> readAccess(metadata), CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest @MethodSource("previewCases")
    void nullableDescriptionsRemainValidAndAreNotFilledInTheSignedSource(String intent, String extension, String contentType) throws Exception {
        var metadata = metadata(intent, extension, contentType, true);
        assertNull(original.getName()); assertNull(original.getType());
        assertDoesNotThrow(() -> readAccess(metadata));
        assertArrayEquals(bytes("F1"), read(metadata).bytes());
        verify(storage, times(1)).getFileContent(1L, "r3/F1." + extension);
        assertNull(original.getName()); assertNull(original.getType());
    }

    @ParameterizedTest @MethodSource("legacyTokenExits")
    void validLegacyTokenWithoutSourceCannotDowngradeBinaryOrReading(String intent, String extension, String contentType, String exit) throws Exception {
        var metadata = metadata(intent, extension, contentType);
        var legacy = tokens.issue(new DccViewerTokenIssueCommand(TENANT, ACTOR, FILE, VERSION, savedEvent.getId(), context.purpose(), 900L));
        metadata.setViewerToken(legacy.token());
        metadata.setViewerTokenId(legacy.payload().getTokenId()); metadata.setViewerTokenNonce(legacy.payload().getNonce());
        assertServiceException(() -> {
            if ("BINARY".equals(exit)) read(metadata); else readAccess(metadata);
        }, CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @CsvSource({"DAILY,DISABLED", "DAILY,OUTSIDE_SCOPE", "APPROVAL_TASK,DISABLED", "APPROVAL_TASK,OUTSIDE_SCOPE", "APPROVAL_TASK,REASSIGNED"})
    void sourceBoundReadingStillRejectsRevokedPermission(String intent, String revocation) throws Exception {
        var metadata = metadata(intent, "pdf", "application/pdf");
        switch (revocation) {
            case "DISABLED" -> actor.setStatus(1);
            case "OUTSIDE_SCOPE" -> when(assignments.isWithinAssignedFileScope(ACTOR, FILE)).thenReturn(false);
            case "REASSIGNED" -> when(task.getAssignee()).thenReturn("101");
            default -> fail("undeclared revocation case");
        }
        assertServiceException(() -> readAccess(metadata), CONTROLLED_FILE_ACCESS_DENIED);
        verifyNoInteractions(storage);
    }

    @Test void sourceBoundSigningFailurePropagatesWithoutLegacyRetry() {
        var signer = spy(tokens);
        var failure = new IllegalStateException("source-bound signer unavailable");
        doThrow(failure).when(signer).issueSourceBound(any(DccViewerTokenIssueCommand.class), any(DccPreviewSourceIdentity.class));
        ReflectionTestUtils.setField(previews, "viewerTokenService", signer);
        ReflectionTestUtils.setField(query, "viewerTokenService", signer);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> metadata("DAILY", "pdf", "application/pdf")));
        verify(signer, never()).issue(any(DccViewerTokenIssueCommand.class));
        verifyNoInteractions(storage);
    }

    private void assertSourceMismatchBeforeStorage(DccControlledFilePreviewMetadataRespVO metadata) {
        assertServiceException(() -> read(metadata), CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        verifyNoInteractions(storage);
        assertEquals(VERSION, selected.getVersionNo(), "source change must not be mistaken for a version mismatch");
    }

    private DccControlledFilePreviewMetadataRespVO metadata(String intent, String extension, String contentType) throws Exception {
        return metadata(intent, extension, contentType, false);
    }

    private DccControlledFilePreviewMetadataRespVO metadata(String intent, String extension, String contentType, boolean nullableDescriptions) throws Exception {
        selected = DccControlledFileDO.builder().id(FILE).tenantId(TENANT).masterId(700L).dccProjectCodeId(300L)
                .categoryId(10L).directoryId(20L).requesterId(ACTOR).versionNo(VERSION).changeType("REVISION")
                .sourceFileId(F1).sourceSha256("r3-persisted-source-sha").originalFileId(F1).publishedFileId(F1)
                .fileNumber("SOP-R3").fileName("F1." + extension).status("ACTIVE")
                .controlledTime(LocalDateTime.of(2026, 10, 15, 1, 0)).activatedTime(LocalDateTime.of(2026, 10, 15, 2, 0)).build();
        context = DccControlledFileBodyReadContext.daily(FILE);
        if ("APPROVAL_TASK".equals(intent)) bindApprovalTask();
        original = artifact(F1, 1L, "r3/F1." + extension, contentType);
        if (nullableDescriptions) { original.setName(null); original.setType(null); }
        artifacts.put(F1, original);
        when(storage.getFileContent(1L, "r3/F1." + extension)).thenReturn(bytes("F1"));
        var metadata = query.getPreviewMetadata(ACTOR, FILE, context, AUDIT);
        assertEquals("pdf".equals(extension) ? "PDF" : "IMAGE", metadata.getPreviewKind());
        assertEquals("F1." + extension, metadata.getFileName());
        assertNotNull(metadata.getViewerToken()); assertNotNull(metadata.getViewerTokenNonce());
        assertNotNull(savedEvent); assertNotNull(savedTrace);
        verifyNoInteractions(storage); // Metadata does not read bytes; the later binary is the action under test.
        return metadata;
    }

    private void bindApprovalTask() {
        selected.setStatus("PENDING_DELAYED_EFFECTIVE_CONFIRM"); selected.setProcessInstanceId(ROUND);
        selected.setProcessDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION);
        context = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                FILE.toString(), VERSION, "REVISION", ROUND, TASK);
        var history = mock(HistoricProcessInstance.class);
        when(processes.getHistoricProcessInstance(ROUND)).thenReturn(history);
        when(history.getId()).thenReturn(ROUND); when(history.getTenantId()).thenReturn(TENANT.toString());
        when(history.getStartUserId()).thenReturn(ACTOR.toString()); when(history.getProcessDefinitionId()).thenReturn("r3-definition");
        when(history.getBusinessKey()).thenReturn(FILE.toString());
        var running = mock(ProcessInstance.class);
        when(processes.getProcessInstance(ROUND)).thenReturn(running);
        when(running.getId()).thenReturn(ROUND); when(running.getTenantId()).thenReturn(TENANT.toString());
        when(running.getBusinessKey()).thenReturn(FILE.toString()); when(running.getProcessDefinitionId()).thenReturn("r3-definition");
        when(rounds.require("REVISION", FILE, ROUND)).thenReturn(3);
        var flow = new DccBoundWorkflowContractResolver.RoundFlowContract(TENANT, FILE, "REVISION", 3, ROUND,
                DccControlledFileProcessDefinitionKeys.REVISION, "r3-definition", "r3-deployment", "r3-digest", "PUBLIC_MATRIX_NATIVE_V2",
                DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM, false, null, null,
                DccBoundWorkflowContractResolver.RoundContractStatus.READY, null);
        when(contracts.resolveRoundFlowContract(eq(selected), any(DccApplicationRoundSummary.class))).thenReturn(flow);
        when(tasks.getTask(TASK)).thenReturn(task);
        when(task.getId()).thenReturn(TASK); when(task.getTenantId()).thenReturn(TENANT.toString());
        when(task.getProcessInstanceId()).thenReturn(ROUND); when(task.getProcessDefinitionId()).thenReturn("r3-definition");
        when(task.getAssignee()).thenReturn(ACTOR.toString());
    }

    private DccControlledFileBinary read(DccControlledFilePreviewMetadataRespVO metadata) {
        return query.readPreviewFile(ACTOR, FILE, context, metadata.getViewerToken(), metadata.getAccessEventCode(),
                metadata.getWatermarkTraceCode(), metadata.getViewerTokenId(), metadata.getViewerTokenNonce(), AUDIT);
    }
    private void readAccess(DccControlledFilePreviewMetadataRespVO metadata) {
        query.assertPreviewReadingAccess(ACTOR, FILE, context, metadata.getViewerToken(), metadata.getAccessEventCode(),
                metadata.getWatermarkTraceCode(), metadata.getViewerTokenId(), metadata.getViewerTokenNonce(), AUDIT);
    }
    private FileDO artifact(Long id, Long configId, String path, String contentType) {
        return FileDO.builder().id(id).configId(configId).path(path).name(path.substring(path.lastIndexOf('/') + 1))
                .type(contentType).size(10L).build();
    }
    private byte[] bytes(String marker) { return ("r3-storage-" + marker).getBytes(StandardCharsets.UTF_8); }
}
