package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.file.access.DccBusinessFileAccessProvider;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccControlledPreviewAccessService;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessRequest;
import cn.iocoder.yudao.module.dcc.service.preview.DccPreviewAccessResult;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccPreviewSourceIdentity;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessReference;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessOperation;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** New source guards added after M04. Real Query + body guard + signed Office token; no business writes. */
@MockitoSettings(strictness = Strictness.LENIENT)
class DccControlledFileBodyReadCurrentSourceTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileMapper files;
    @Mock private FileMapper binaries;
    @Mock private FileService storage;
    @Mock private AdminUserApi users;
    @Mock private DccControlledFileAssignmentScopeService assignments;
    @Mock private DccDirectoryAccessPermissionService directories;
    @Mock private DccProjectAccessService projectAccess;
    @Mock private BpmTaskService tasks;
    @Mock private BpmProcessInstanceService processes;
    @Mock private DccApplicationRoundService rounds;
    @Mock private DccBoundWorkflowContractResolver contracts;
    @Mock private DccControlledPreviewAccessService previews;
    @Mock private DccControlledPreviewWatermarkService watermarks;
    @Mock private DccControlledFileAccessEventMapper events;
    @Mock private DccControlledFileWatermarkTraceMapper traces;
    @Mock private DccControlledFileAccessLogMapper logs;
    @Mock private DccViewerTokenService viewerTokens;
    @Mock private BusinessFileAccessService businessFiles;
    @InjectMocks private DccControlledFileQueryServiceImpl query;

    private DccControlledFileDO selected;
    private Task exactTask;
    private DccControlledFileBodyReadContext context;
    private DccOnlyOfficePreviewTokenService officeTokens;
    private DccBusinessFileAccessProvider provider;
    private final BusinessFileAccessReference reference = new BusinessFileAccessReference("dcc", "DCC_CONTROLLED_FILE", 910L, "A/2", 31L, null);
    private final DccRequestAuditContext audit = new DccRequestAuditContext("127.0.0.1", "G07 current-source isolated test", "g07-source-proof");

    @BeforeEach void currentDelayedConfirmationScenario() {
        TenantContextHolder.setTenantId(31L);
        var login = new LoginUser(); login.setId(99L); login.setTenantId(31L); login.setUserType(2);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
        var user = new AdminUserRespDTO(); user.setId(99L); user.setTenantId(31L); user.setStatus(0);
        when(users.getUser(99L)).thenReturn(user);
        when(assignments.isWithinAssignedFileScope(99L,910L)).thenReturn(true);

        var guard = new DccControlledFileDetailAuthorizationGuard();
        ReflectionTestUtils.setField(guard,"assignmentScopeService",assignments);
        ReflectionTestUtils.setField(guard,"adminUserApi",users);
        ReflectionTestUtils.setField(guard,"bpmTaskService",tasks);
        ReflectionTestUtils.setField(guard,"bpmProcessInstanceService",processes);
        ReflectionTestUtils.setField(guard,"applicationRoundService",rounds);
        ReflectionTestUtils.setField(guard,"boundWorkflowContractResolver",contracts);
        ReflectionTestUtils.setField(guard,"projectAccessService",projectAccess);
        ReflectionTestUtils.setField(query,"detailAuthorizationGuard",guard);
        ReflectionTestUtils.setField(query,"directoryAccessPermissionService",directories);
        ReflectionTestUtils.setField(query,"assignmentScopeService",assignments);
        ReflectionTestUtils.setField(query,"projectAccessService",projectAccess);
        when(projectAccess.listReadableProjectIds(99L)).thenReturn(List.of(300L));
        when(directories.getAuthorizedDirectoryIds(99L,cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum.PREVIEW)).thenReturn(Set.of(20L));
        provider = new DccBusinessFileAccessProvider(query, files, null, null);

        selected = DccControlledFileDO.builder().id(910L).tenantId(31L).masterId(700L).dccProjectCodeId(300L)
                .categoryId(10L).directoryId(20L).requesterId(99L).changeType("REVISION").versionNo("A/2")
                .status("PENDING_DELAYED_EFFECTIVE_CONFIRM").effectiveMode("DELAYED")
                .processInstanceId("round-delayed").processDefinitionKey(DccControlledFileProcessDefinitionKeys.REVISION)
                .sourceFileId(100L).sourceSha256("approved-exact-source-sha").originalFileId(200L)
                .stampedFileId(300L).publishedFileId(400L).fileNumber("SOP-SOURCE").fileName("selected.pdf").build();
        when(files.selectById(910L)).thenReturn(selected);
        context = new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,
                "910","A/2","REVISION","round-delayed","task-delayed");
        var historic = mock(HistoricProcessInstance.class);
        when(processes.getHistoricProcessInstance("round-delayed")).thenReturn(historic);
        when(historic.getId()).thenReturn("round-delayed"); when(historic.getTenantId()).thenReturn("31");
        when(historic.getStartUserId()).thenReturn("99"); when(historic.getProcessDefinitionId()).thenReturn("definition-v6");
        when(historic.getBusinessKey()).thenReturn("910");
        var runtime = mock(ProcessInstance.class);
        when(processes.getProcessInstance("round-delayed")).thenReturn(runtime);
        when(runtime.getId()).thenReturn("round-delayed"); when(runtime.getTenantId()).thenReturn("31");
        when(runtime.getBusinessKey()).thenReturn("910"); when(runtime.getProcessDefinitionId()).thenReturn("definition-v6");
        when(rounds.require("REVISION",910L,"round-delayed")).thenReturn(3);
        var flow = new DccBoundWorkflowContractResolver.RoundFlowContract(31L,910L,"REVISION",3,"round-delayed",
                DccControlledFileProcessDefinitionKeys.REVISION,"definition-v6","deployment-v6","digest-v6","PUBLIC_MATRIX_NATIVE_V2",
                DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM,false,null,null,
                DccBoundWorkflowContractResolver.RoundContractStatus.READY,null);
        when(contracts.resolveRoundFlowContract(eq(selected),any(DccApplicationRoundSummary.class))).thenReturn(flow);
        exactTask = mock(Task.class);
        when(tasks.getTask("task-delayed")).thenReturn(exactTask);
        when(exactTask.getId()).thenReturn("task-delayed"); when(exactTask.getTenantId()).thenReturn("31");
        when(exactTask.getProcessInstanceId()).thenReturn("round-delayed"); when(exactTask.getProcessDefinitionId()).thenReturn("definition-v6");
        when(exactTask.getTaskDefinitionKey()).thenReturn("DELAYED_EFFECTIVE_CONFIRM"); when(exactTask.getAssignee()).thenReturn("99");

        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.pdf","application/pdf",utf8Size("approved-source")));
        when(businessFiles.assertAllowed(any(BusinessFileAccessRequest.class))).thenAnswer(call -> {
            var request = call.getArgument(0, BusinessFileAccessRequest.class);
            provider.assertAllowed(request,reference);
            return Optional.of(reference);
        });
        when(watermarks.build(eq(99L),eq("preview"),anyString())).thenReturn(DccControlledPreviewWatermarkRespVO.builder().purpose("preview").build());
        var now = LocalDateTime.of(2026,10,15,2,30,10);
        when(previews.prepareAccess(any(DccPreviewAccessRequest.class))).thenReturn(new DccPreviewAccessResult(
                800L,"event-source",801L,"trace-source","viewer","viewer-id","viewer-nonce",now,now.plusMinutes(15),"{}"));
        when(previews.prepareSourceBoundAccess(any(DccPreviewAccessRequest.class), any(DccPreviewSourceIdentity.class))).thenReturn(new DccPreviewAccessResult(
                800L,"event-source",801L,"trace-source","viewer","viewer-id","viewer-nonce",now,now.plusMinutes(15),"{}"));
        var event = DccControlledFileAccessEventDO.builder().id(800L).tenantId(31L).controlledFileId(910L).fileVersionNo("A/2")
                .userId(99L).accessEventCode("event-source").accessType("PREVIEW").purpose(context.purpose())
                .result("SUCCESS").processInstanceId("round-delayed").build();
        when(events.selectOne(any())).thenReturn(event); when(events.selectById(800L)).thenReturn(event);
        var trace = DccControlledFileWatermarkTraceDO.builder().id(801L).traceCode("trace-source").accessEventId(800L)
                .accessEventCode("event-source").controlledFileId(910L).fileVersionNo("A/2").userId(99L).build();
        when(traces.selectOne(any())).thenReturn(trace);
        when(logs.insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO.class))).thenReturn(1);

        var properties = new DccOnlyOfficePreviewProperties(); properties.setBaseUrl("http://office.invalid");
        properties.setPublicFileBaseUrl("http://preview.invalid"); properties.setJwtSecret("g07-task-owned-token-secret");
        officeTokens = new DccOnlyOfficePreviewTokenService();
        ReflectionTestUtils.setField(officeTokens,"properties",properties);
        ReflectionTestUtils.setField(query,"onlyOfficePreviewTokenService",officeTokens);
        ReflectionTestUtils.setField(query,"onlyOfficePreviewProperties",properties);
    }
    @AfterEach void clearSubject() { SecurityContextHolder.clearContext(); TenantContextHolder.clear(); }
    private FileDO binary(Long id,String name,String type,long size) { return FileDO.builder().id(id).configId(1L).name(name).type(type).path("g07-source/"+name).size(size).build(); }
    private long utf8Size(String content) { return content.getBytes(StandardCharsets.UTF_8).length; }
    private DccPreviewSourceIdentity approvedSource() { return DccPreviewSourceIdentity.fromFile(binary(100L,"exact-source.pdf","application/pdf",utf8Size("approved-source"))); }
    private DccControlledFileBinary readBinary() { return query.readPreviewFile(99L,910L,context,"viewer","event-source","trace-source","viewer-id","viewer-nonce",audit); }

    @Test void readingLifecycleRechecksExactTaskWithoutReadingBytesOrIssuingNewEvent() throws Exception {
        assertReadingAccess();
        verify(viewerTokens).verifySourceBound("viewer",new DccViewerTokenExpectedContext(31L,99L,910L,"A/2",800L,
                context.purpose(),900L,"viewer-nonce","viewer-id"),approvedSource());
        verify(storage,never()).getFileContent(any(),any());
        verify(previews,never()).prepareAccess(any());
        verify(previews,never()).prepareSourceBoundAccess(any(),any());
        verify(logs,never()).insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO.class));
    }
    @Test void readingLifecycleRejectsReassignedTaskBeforeTokenOrBodyRead() throws Exception {
        when(exactTask.getAssignee()).thenReturn("101");
        assertServiceException(this::assertReadingAccess,CONTROLLED_FILE_ACCESS_DENIED);
        verify(viewerTokens,never()).verify(any(),any()); verify(viewerTokens,never()).verifySourceBound(any(),any(),any()); verify(storage,never()).getFileContent(any(),any());
    }
    @Test void readingLifecycleRejectsDisabledAccountBeforeTokenOrBodyRead() throws Exception {
        users.getUser(99L).setStatus(1);
        assertServiceException(this::assertReadingAccess,CONTROLLED_FILE_ACCESS_DENIED);
        verify(viewerTokens,never()).verify(any(),any()); verify(viewerTokens,never()).verifySourceBound(any(),any(),any()); verify(storage,never()).getFileContent(any(),any());
    }
    @Test void readingLifecycleRejectsWrongEventRoundBeforeTokenOrBodyRead() throws Exception {
        events.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>()).setProcessInstanceId("round-other");
        assertServiceException(this::assertReadingAccess,CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        verify(viewerTokens,never()).verify(any(),any()); verify(viewerTokens,never()).verifySourceBound(any(),any(),any()); verify(storage,never()).getFileContent(any(),any());
    }
    private void assertReadingAccess() {
        query.assertPreviewReadingAccess(99L,910L,context,"viewer","event-source","trace-source","viewer-id","viewer-nonce",audit);
    }

    @Test void delayedTaskReadsOnlyTheExactApprovedSourceWithTheSameMetadataAndBinaryProof() throws Exception {
        var metadata = query.getPreviewMetadata(99L,910L,context,audit);
        assertEquals("exact-source.pdf",metadata.getFileName());
        assertEquals("2026-10-15T02:30:10Z",metadata.getPreviewRegisteredAt());
        when(storage.getFileContent(1L,"g07-source/exact-source.pdf")).thenReturn("approved-source".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("approved-source".getBytes(StandardCharsets.UTF_8),readBinary().bytes());
        verify(previews).prepareSourceBoundAccess(argThat(request -> context.purpose().equals(request.purpose())
                && "round-delayed".equals(request.processInstanceId()) && "A/2".equals(request.versionId())),eq(approvedSource()));
        verify(viewerTokens).verifySourceBound(eq("viewer"),argThat((DccViewerTokenExpectedContext expected) ->
                context.purpose().equals(expected.purpose()) && expected.fileId().equals(910L) && "A/2".equals(expected.versionId())),eq(approvedSource()));
        verify(binaries,never()).selectById(200L); verify(binaries,never()).selectById(300L); verify(binaries,never()).selectById(400L);
    }

    @Test void dailyAndReassignedTaskCannotReachDelayedMetadataTokenOrStorage() throws Exception {
        assertServiceException(() -> query.getPreviewMetadata(99L,910L,audit),CONTROLLED_FILE_ACCESS_DENIED);
        when(exactTask.getAssignee()).thenReturn("88");
        assertServiceException(() -> query.getPreviewMetadata(99L,910L,context,audit),CONTROLLED_FILE_ACCESS_DENIED);
        verifyNoInteractions(previews,businessFiles,storage);
    }

    @Test void missingDelayedSourceFailsWithoutBorrowingAnyOtherArtifact() {
        selected.setSourceFileId(null);
        var error=assertThrows(IllegalStateException.class,() -> query.getPreviewMetadata(99L,910L,context,audit));
        assertTrue(error.getMessage().contains("exact saved source"));
        verifyNoInteractions(previews,businessFiles,storage,binaries);
    }

    @Test void currentDrawingUsesItsOwnCompanionPdfForDelayedTask() throws Exception {
        selected.setDrawingPdfFileId(101L);
        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.dwg","application/acad",utf8Size("declared-drawing")));
        when(binaries.selectById(101L)).thenReturn(binary(101L,"exact-companion.pdf","application/pdf",utf8Size("current-companion")));
        var metadata=query.getPreviewMetadata(99L,910L,context,audit);
        assertEquals("PDF",metadata.getPreviewKind());assertEquals("exact-companion.pdf",metadata.getFileName());
        when(storage.getFileContent(1L,"g07-source/exact-companion.pdf")).thenReturn("current-companion".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("current-companion".getBytes(StandardCharsets.UTF_8),readBinary().bytes());
        verify(storage,never()).getFileContent(1L,"g07-source/exact-source.dwg");
    }

    @Test void delayedOfficeMetadataMustUseTheSelectedSourceTypeAndIssueItsRealSignedOfficeUrl() {
        when(binaries.selectById(100L)).thenReturn(binary(100L,"approved-confirmation.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",utf8Size("declared-office-preview")));
        var metadata=query.getPreviewMetadata(99L,910L,context,audit);
        assertEquals("OFFICE",metadata.getPreviewKind());
        assertEquals("approved-confirmation.docx",metadata.getFileName());
        assertNotNull(metadata.getOnlyofficeDocumentUrl());
        var token=metadata.getOnlyofficeDocumentUrl().split("token=",2)[1];
        var payload=officeTokens.verifyControlledFile(token,910L);
        assertEquals(100L,payload.getInfraFileId());
        assertEquals(context.wire(),payload.getBodyReadContext());
        assertEquals(context.purpose(),payload.getPurpose());
        verifyNoInteractions(storage);
    }

    @Test void actualSignedOfficeCallbackReadsOriginalSourceThenRejectsChangedSourceBeforeAuthorizationOrStorage() throws Exception {
        selected.setStatus("PENDING_MATRIX_REVIEW");
        when(exactTask.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",utf8Size("signed-source")));
        var metadata=query.getPreviewMetadata(99L,910L,context,audit);
        assertEquals("OFFICE",metadata.getPreviewKind());
        assertNotNull(metadata.getOnlyofficeDocumentUrl());
        String token=metadata.getOnlyofficeDocumentUrl().split("token=",2)[1];
        var payload=officeTokens.verifyControlledFile(token,910L);
        assertEquals(100L,payload.getInfraFileId());assertEquals(context.wire(),payload.getBodyReadContext());
        when(storage.getFileContent(1L,"g07-source/exact-source.docx")).thenReturn("signed-source".getBytes(StandardCharsets.UTF_8));
        SecurityContextHolder.clearContext();
        var result=query.readOnlyOfficePreviewFile(910L,token,audit);
        assertArrayEquals("signed-source".getBytes(StandardCharsets.UTF_8),result.bytes());
        assertNull(SecurityFrameworkUtils.getLoginUser());
        clearInvocations(storage,businessFiles);
        selected.setSourceFileId(102L);
        when(binaries.selectById(102L)).thenReturn(binary(102L,"changed.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",utf8Size("changed-source")));
        assertServiceException(() -> query.readOnlyOfficePreviewFile(910L,token,audit),CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        verifyNoInteractions(storage,businessFiles);assertNull(SecurityFrameworkUtils.getLoginUser());
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"bodyReadContext")).get());
    }

    @Test void officeCallbackCannotBorrowAnotherRoundEventEvenWhenItsSignedContextAndSourceRemainUnchanged() throws Exception {
        selected.setStatus("PENDING_MATRIX_REVIEW");
        when(exactTask.getTaskDefinitionKey()).thenReturn("MATRIX_REVIEW");
        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",utf8Size("signed-source")));
        var metadata=query.getPreviewMetadata(99L,910L,context,audit);
        assertEquals("OFFICE",metadata.getPreviewKind());
        assertNotNull(metadata.getOnlyofficeDocumentUrl());
        String token=metadata.getOnlyofficeDocumentUrl().split("token=",2)[1];
        SecurityContextHolder.clearContext();
        var event=events.selectById(800L);
        event.setProcessInstanceId("other-round");
        clearInvocations(storage,businessFiles);
        assertServiceException(() -> query.readOnlyOfficePreviewFile(910L,token,audit),CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        verifyNoInteractions(storage,businessFiles);
        event.setProcessInstanceId("round-delayed");
        event.setPurpose(DccControlledFileBodyReadContext.daily(910L).purpose());
        assertServiceException(() -> query.readOnlyOfficePreviewFile(910L,token,audit),CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        verifyNoInteractions(storage,businessFiles);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"bodyReadContext")).get());
        assertNull(SecurityFrameworkUtils.getLoginUser());
    }

    private BusinessFileAccessRequest directPreviewRequest() {
        return new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW,selected.getPublishedFileId(),31L,99L,
                null,"g07-infra-default-daily",reference,"127.0.0.1","G07 actual provider direct read");
    }

    @Test void realInfraProviderDefaultsToDailyAndCannotBorrowAnExistingApprovalTaskGrant() {
        assertServiceException(() -> provider.assertAllowed(directPreviewRequest(),reference),CONTROLLED_FILE_ACCESS_DENIED);
        verifyNoInteractions(previews,businessFiles,storage);
    }

    @ParameterizedTest
    @CsvSource({"metadata,disabled", "metadata,ignored", "metadata,reassigned",
            "binary,disabled", "binary,ignored", "binary,reassigned",
            "infra,disabled", "infra,ignored", "infra,outsideScope"})
    void currentExitRechecksIdentityBeforeIssuingTokenOrReadingAnyBytes(String exit, String failure) throws Exception {
        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.pdf","application/pdf",utf8Size("allowed-source")));
        when(storage.getFileContent(1L,"g07-source/exact-source.pdf")).thenReturn("allowed-source".getBytes(StandardCharsets.UTF_8));
        if ("infra".equals(exit)) {
            selected.setStatus("ACTIVE");selected.setActivatedTime(LocalDateTime.of(2026,10,15,0,0));
            provider.assertAllowed(directPreviewRequest(),reference);
        } else if ("metadata".equals(exit)) {
            assertNotNull(query.getPreviewMetadata(99L,910L,context,audit).getViewerToken());
        } else {
            assertArrayEquals("allowed-source".getBytes(StandardCharsets.UTF_8),readBinary().bytes());
        }
        clearInvocations(previews,businessFiles,storage);
        switch (failure) {
            case "disabled" -> users.getUser(99L).setStatus(1);
            case "ignored" -> TenantContextHolder.setIgnore(true);
            case "reassigned" -> when(exactTask.getAssignee()).thenReturn("88");
            case "outsideScope" -> when(assignments.isWithinAssignedFileScope(99L,910L)).thenReturn(false);
            default -> fail("unknown explicit scenario");
        }
        assertServiceException(() -> {
            switch (exit) {
                case "metadata" -> query.getPreviewMetadata(99L,910L,context,audit);
                case "binary" -> readBinary();
                case "infra" -> provider.assertAllowed(directPreviewRequest(),reference);
                default -> fail("unknown exit");
            }
        },CONTROLLED_FILE_ACCESS_DENIED);
        verifyNoInteractions(previews,businessFiles,storage);
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"bodyReadContext")).get());
    }

    @ParameterizedTest
    @ValueSource(strings={"disabled", "reassigned", "foreignTenant"})
    void signedOfficeCallbackRechecksCurrentAccountTaskAndTenantBeforeAnyBytes(String failure) throws Exception {
        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",utf8Size("signed-source")));
        var metadata=query.getPreviewMetadata(99L,910L,context,audit);
        String token=metadata.getOnlyofficeDocumentUrl().split("token=",2)[1];
        SecurityContextHolder.clearContext();
        switch (failure) {
            case "disabled" -> users.getUser(99L).setStatus(1);
            case "reassigned" -> when(exactTask.getAssignee()).thenReturn("88");
            case "foreignTenant" -> TenantContextHolder.setTenantId(32L);
            default -> fail("unknown callback scenario");
        }
        clearInvocations(previews,businessFiles,storage);
        if ("foreignTenant".equals(failure)) {
            var denied=assertThrows(IllegalStateException.class,() -> query.readOnlyOfficePreviewFile(910L,token,audit));
            assertEquals("OnlyOffice preview token tenant context is invalid",denied.getMessage());
        } else {
            assertServiceException(() -> query.readOnlyOfficePreviewFile(910L,token,audit),CONTROLLED_FILE_ACCESS_DENIED);
        }
        verifyNoInteractions(previews,storage);
        assertNull(SecurityFrameworkUtils.getLoginUser());
        assertNull(((ThreadLocal<?>)ReflectionTestUtils.getField(query,"bodyReadContext")).get());
    }

    @Test void officeTenantIgnoredCallbackUsesOnlyTheVerifiedSignedTenantAndRestoresItsOuterContext() throws Exception {
        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",utf8Size("signed-tenant-body")));
        var metadata=query.getPreviewMetadata(99L,910L,context,audit);
        String token=metadata.getOnlyofficeDocumentUrl().split("token=",2)[1];
        when(storage.getFileContent(1L,"g07-source/exact-source.docx")).thenReturn("signed-tenant-body".getBytes(StandardCharsets.UTF_8));
        SecurityContextHolder.clearContext();TenantContextHolder.setTenantId(32L);TenantContextHolder.setIgnore(true);
        assertArrayEquals("signed-tenant-body".getBytes(StandardCharsets.UTF_8),query.readOnlyOfficePreviewFile(910L,token,audit).bytes());
        assertEquals(32L,TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());
        users.getUser(99L).setStatus(1);clearInvocations(storage);
        assertServiceException(() -> query.readOnlyOfficePreviewFile(910L,token,audit),CONTROLLED_FILE_ACCESS_DENIED);
        verifyNoInteractions(storage);assertEquals(32L,TenantContextHolder.getTenantId());assertTrue(TenantContextHolder.isIgnore());
        assertNull(SecurityFrameworkUtils.getLoginUser());
    }

    @Test void historicalTaskReadsItsExactRetainedBodyInsteadOfCurrentSourceAndRechecksTheActualHistoricActor() throws Exception {
        selected.setStatus("SUPERSEDED");selected.setRequesterId(88L);selected.setProcessInstanceId("new-current-round");
        when(processes.getProcessInstance("round-delayed")).thenReturn(null);
        var historicTask=mock(HistoricTaskInstance.class);
        when(tasks.getHistoricTask("task-delayed")).thenReturn(historicTask);
        when(historicTask.getId()).thenReturn("task-delayed");when(historicTask.getTenantId()).thenReturn("31");
        when(historicTask.getProcessInstanceId()).thenReturn("round-delayed");when(historicTask.getProcessDefinitionId()).thenReturn("definition-v6");
        when(historicTask.getAssignee()).thenReturn("99");when(historicTask.getEndTime()).thenReturn(new java.util.Date(1L));
        context=new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_HISTORY,
                "910","A/2","REVISION","round-delayed","task-delayed");
        events.selectById(800L).setPurpose(context.purpose());
        when(binaries.selectById(400L)).thenReturn(binary(400L,"retained-A2.pdf","application/pdf",utf8Size("retained-selected-version")));
        var metadata=query.getPreviewMetadata(99L,910L,context,audit);
        assertEquals("retained-A2.pdf",metadata.getFileName());
        when(storage.getFileContent(1L,"g07-source/retained-A2.pdf")).thenReturn("retained-selected-version".getBytes(StandardCharsets.UTF_8));
        assertArrayEquals("retained-selected-version".getBytes(StandardCharsets.UTF_8),readBinary().bytes());
        verify(storage,never()).getFileContent(1L,"g07-source/exact-source.pdf");
        clearInvocations(storage,previews);
        when(historicTask.getAssignee()).thenReturn("88");
        assertServiceException(this::readBinary,CONTROLLED_FILE_ACCESS_DENIED);
        verifyNoInteractions(storage,previews);
        assertEquals("new-current-round",selected.getProcessInstanceId());
    }

    @Test void signedOfficeTokenCannotReadAnotherSelectedFileEvenForTheSameAuthenticatedActor() {
        when(binaries.selectById(100L)).thenReturn(binary(100L,"exact-source.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",utf8Size("signed-source")));
        String token=query.getPreviewMetadata(99L,910L,context,audit).getOnlyofficeDocumentUrl().split("token=",2)[1];
        SecurityContextHolder.clearContext();clearInvocations(storage,businessFiles,files);
        var denied=assertThrows(IllegalArgumentException.class,() -> query.readOnlyOfficePreviewFile(911L,token,audit));
        assertEquals("BODY_READ_CONTEXT_INVALID",denied.getMessage());
        verifyNoInteractions(storage,businessFiles,files);
        assertNull(SecurityFrameworkUtils.getLoginUser());
    }
}
