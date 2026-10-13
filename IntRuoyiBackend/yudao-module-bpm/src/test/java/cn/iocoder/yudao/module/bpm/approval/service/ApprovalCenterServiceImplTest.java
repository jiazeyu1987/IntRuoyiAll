package cn.iocoder.yudao.module.bpm.approval.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordResult;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordService;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureRecordServiceImpl;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureImageSnapshot;
import cn.iocoder.yudao.module.bpm.approval.service.signature.ApprovalSignatureImageSnapshotProvider;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalCenterReviewPreflight;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProvider;
import cn.iocoder.yudao.module.bpm.approval.service.provider.ApprovalTaskProviderRegistry;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.inOrder;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.APPROVAL_VIEW_TYPE_UNSUPPORTED;

@ExtendWith(MockitoExtension.class)
class ApprovalCenterServiceImplTest {

    @Mock
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ApprovalSignatureRecordService signatureRecordService;

    @Test
    void getTaskPageFailsFastWhenRequestedModuleIsNotRegistered() {
        ApprovalCenterService service = new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of()),
                permissionApi, adminUserApi, signatureRecordService);
        ApprovalTaskQuery query = new ApprovalTaskQuery()
                .setViewType(ApprovalTaskViewType.TODO)
                .setModuleCode(ApprovalModuleCode.DCC)
                .setPageNo(1)
                .setPageSize(10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getTaskPage(100L, query));

        assertTrue(ex.getMessage().contains("APPROVAL_ADAPTER_NOT_REGISTERED"));
        assertTrue(ex.getMessage().contains("DCC"));
    }

    @Test
    void getTaskPageReturnsBusinessErrorWhenModuleDoesNotSupportViewType() {
        ApprovalTaskProvider provider = provider(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("showroom-old", LocalDateTime.parse("2026-06-23T09:00:00"))),
                loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi, signatureRecordService);
        ApprovalTaskQuery query = new ApprovalTaskQuery()
                .setViewType(ApprovalTaskViewType.CC)
                .setModuleCode(ApprovalModuleCode.SHOWROOM)
                .setPageNo(1)
                .setPageSize(10);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getTaskPage(100L, query));

        assertEquals(APPROVAL_VIEW_TYPE_UNSUPPORTED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("SHOWROOM"));
        assertTrue(ex.getMessage().contains("CC"));
    }

    @Test
    void getTaskPageReturnsBusinessErrorWhenNoProviderSupportsViewType() {
        ApprovalTaskProvider provider = provider(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("showroom-old", LocalDateTime.parse("2026-06-23T09:00:00"))),
                loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi, signatureRecordService);
        ApprovalTaskQuery query = new ApprovalTaskQuery()
                .setViewType(ApprovalTaskViewType.CC)
                .setPageNo(1)
                .setPageSize(10);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.getTaskPage(100L, query));

        assertEquals(APPROVAL_VIEW_TYPE_UNSUPPORTED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("全部可见模块"));
        assertTrue(ex.getMessage().contains("CC"));
    }

    @Test
    void getTaskPageAggregatesProvidersAndSortsByTaskCreatedAtDesc() {
        ApprovalTaskProvider showroom = provider(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("showroom-new", LocalDateTime.parse("2026-06-23T11:00:00"))),
                loginUserId -> true);
        ApprovalTaskProvider edhr = provider(ApprovalModuleCode.EDHR, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("edhr-old", LocalDateTime.parse("2026-06-23T10:00:00"))),
                loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(showroom, edhr)), permissionApi, adminUserApi,
                signatureRecordService);
        ApprovalTaskQuery query = new ApprovalTaskQuery()
                .setViewType(ApprovalTaskViewType.TODO)
                .setPageNo(1)
                .setPageSize(10);

        PageResult<ApprovalTaskSummary> page = service.getTaskPage(100L, query);

        assertEquals(2L, page.getTotal());
        assertEquals("showroom-new", page.getList().get(0).getId());
        assertEquals("edhr-old", page.getList().get(1).getId());
    }

    @Test
    void getTaskPageKeepsGlobalTotalStableAcrossPagesWhenProvidersHaveDifferentTotals() {
        List<ApprovalTaskSummary> edhrRows = rows(ApprovalModuleCode.EDHR, "edhr", 20,
                LocalDateTime.parse("2026-07-20T12:00:00"));
        List<ApprovalTaskSummary> feedbackRows = rows(ApprovalModuleCode.MES_FEEDBACK, "feedback", 24,
                LocalDateTime.parse("2026-07-20T11:00:00"));
        List<ApprovalTaskQueryContext> capturedContexts = new ArrayList<>();
        ApprovalTaskProvider edhr = pagedProvider(ApprovalModuleCode.EDHR, edhrRows, capturedContexts::add);
        ApprovalTaskProvider feedback = pagedProvider(ApprovalModuleCode.MES_FEEDBACK, feedbackRows,
                capturedContexts::add);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(edhr, feedback)), permissionApi, adminUserApi,
                signatureRecordService);

        PageResult<ApprovalTaskSummary> page = service.getTaskPage(100L, new ApprovalTaskQuery()
                .setViewType(ApprovalTaskViewType.TODO)
                .setPageNo(2)
                .setPageSize(20));

        assertEquals(44L, page.getTotal());
        assertEquals(20, page.getList().size());
        assertTrue(page.getList().stream().allMatch(row -> row.getId().startsWith("feedback-")));
        assertEquals(2, capturedContexts.size());
        assertTrue(capturedContexts.stream().allMatch(context -> context.getPageNo() == 1));
        assertTrue(capturedContexts.stream().allMatch(context -> context.getPageSize() == 200));
    }

    @Test
    void getTaskPageFailsFastWhenProviderReportsTodoTotalWithoutFirstPageRows() {
        ApprovalTaskProvider inconsistentProvider = inconsistentTotalProvider(ApprovalModuleCode.BPM, 128L);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(inconsistentProvider)), permissionApi, adminUserApi,
                signatureRecordService);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.getTaskPage(100L, new ApprovalTaskQuery()
                        .setViewType(ApprovalTaskViewType.TODO)
                        .setPageNo(1)
                        .setPageSize(10)));

        assertEquals("APPROVAL_QUERY_WINDOW_INCOMPLETE",ex.getMessage());
    }

    @Test
    void getTaskPageEnrichesAssigneeUserNameForVisibleRows() {
        ApprovalTaskSummary row = summary("bpm-todo-row", LocalDateTime.parse("2026-07-18T19:17:59"))
                .setAssigneeUserId(910272L);
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(910272L);
        user.setUsername("aoteman");
        user.setNickname("??1");
        when(adminUserApi.getUserMap(Set.of(910272L))).thenReturn(Map.of(910272L, user));
        ApprovalTaskProvider provider = provider(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.TODO),
                List.of(row), loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        PageResult<ApprovalTaskSummary> page = service.getTaskPage(100L, new ApprovalTaskQuery()
                .setModuleCode(ApprovalModuleCode.BPM)
                .setViewType(ApprovalTaskViewType.TODO)
                .setPageNo(1)
                .setPageSize(10));

        assertEquals(1L, page.getTotal());
        assertEquals("aoteman", page.getList().get(0).getAssigneeUserName());
        verify(adminUserApi).getUserMap(Set.of(910272L));
    }

    @Test
    void getTaskPageEnrichesInitiatorUserNameForVisibleRows() {
        ApprovalTaskSummary row = summary("bpm-initiated-row", LocalDateTime.parse("2026-07-18T19:35:23"))
                .setInitiatorUserId(151L);
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(151L);
        user.setUsername("wangxin");
        user.setNickname("王鑫");
        when(adminUserApi.getUserMap(Set.of(151L))).thenReturn(Map.of(151L, user));
        ApprovalTaskProvider provider = provider(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.TODO),
                List.of(row), loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        PageResult<ApprovalTaskSummary> page = service.getTaskPage(100L, new ApprovalTaskQuery()
                .setModuleCode(ApprovalModuleCode.BPM)
                .setViewType(ApprovalTaskViewType.TODO)
                .setPageNo(1)
                .setPageSize(10));

        assertEquals(1L, page.getTotal());
        assertEquals("王鑫(wangxin)", page.getList().get(0).getInitiatorUserName());
        verify(adminUserApi).getUserMap(Set.of(151L));
    }

    @Test
    void getTaskPageKeepsTaskWhenAssigneeUserSnapshotIsMissing() {
        ApprovalTaskSummary row = summary("orphan-assignee-row", LocalDateTime.parse("2026-07-18T20:41:58"))
                .setAssigneeUserId(113L);
        when(adminUserApi.getUserMap(Set.of(113L))).thenReturn(Map.of());
        ApprovalTaskProvider provider = provider(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.TODO),
                List.of(row), loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        PageResult<ApprovalTaskSummary> page = service.getTaskPage(100L, new ApprovalTaskQuery()
                .setModuleCode(ApprovalModuleCode.BPM)
                .setViewType(ApprovalTaskViewType.TODO)
                .setPageNo(1)
                .setPageSize(10));

        assertEquals(1L, page.getTotal());
        assertEquals(113L, page.getList().get(0).getAssigneeUserId());
        assertEquals("用户不存在(113)", page.getList().get(0).getAssigneeUserName());
        verify(adminUserApi).getUserMap(Set.of(113L));
    }

    @Test
    void listProvidersHidesSrmModuleWhenUserLacksSrmAdminRole() {
        ApprovalTaskProvider showroom = provider(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("showroom-new", LocalDateTime.parse("2026-06-23T11:00:00"))),
                loginUserId -> true);
        ApprovalTaskProvider srm = provider(ApprovalModuleCode.SRM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("srm-new", LocalDateTime.parse("2026-06-23T12:00:00"))),
                loginUserId -> false);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(showroom, srm)), permissionApi, adminUserApi,
                signatureRecordService);

        List<ApprovalProviderDescriptor> descriptors = service.listProviders(100L);

        assertEquals(1, descriptors.size());
        assertEquals(ApprovalModuleCode.SHOWROOM, descriptors.get(0).getModuleCode());
    }

    @Test
    void listProvidersKeepsSrmModuleWhenUserHasSrmAdminRole() {
        ApprovalTaskProvider showroom = provider(ApprovalModuleCode.SHOWROOM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("showroom-new", LocalDateTime.parse("2026-06-23T11:00:00"))),
                loginUserId -> true);
        ApprovalTaskProvider srm = provider(ApprovalModuleCode.SRM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("srm-new", LocalDateTime.parse("2026-06-23T12:00:00"))),
                loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(showroom, srm)), permissionApi, adminUserApi,
                signatureRecordService);

        List<ApprovalProviderDescriptor> descriptors = service.listProviders(100L);

        assertEquals(2, descriptors.size());
        assertTrue(descriptors.stream().anyMatch(item -> item.getModuleCode() == ApprovalModuleCode.SRM));
    }

    @Test
    void getTaskPageMarksGlobalViewWhenLoginUserHasApprovalAdminRole() {
        when(permissionApi.hasAnyRoles(100L, RoleCodeEnum.APPROVAL_ADMIN.getCode())).thenReturn(true);
        ApprovalTaskProvider provider = provider(ApprovalModuleCode.BPM, Set.of(ApprovalTaskViewType.TODO),
                List.of(summary("global-row", LocalDateTime.parse("2026-06-23T11:00:00"))), loginUserId -> true);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        PageResult<ApprovalTaskSummary> page = service.getTaskPage(100L, new ApprovalTaskQuery()
                .setModuleCode(ApprovalModuleCode.BPM)
                .setViewType(ApprovalTaskViewType.TODO)
                .setPageNo(1)
                .setPageSize(10));

        assertEquals(1L, page.getTotal());
        verify(permissionApi).hasAnyRoles(100L, RoleCodeEnum.APPROVAL_ADMIN.getCode());
        verify(permissionApi, never()).hasAnyRolesOrSuperAdmin(100L, RoleCodeEnum.APPROVAL_ADMIN.getCode());
    }

    @Test
    void getTaskPageKeepsGlobalViewFalseWhenUserLacksApprovalAdminRole() {
        when(permissionApi.hasAnyRoles(100L, RoleCodeEnum.APPROVAL_ADMIN.getCode())).thenReturn(false);
        AtomicReference<ApprovalTaskQueryContext> capturedContext = new AtomicReference<>();
        ApprovalTaskProvider provider = pagedProvider(ApprovalModuleCode.BPM,
                List.of(summary("self-related-row", LocalDateTime.parse("2026-07-21T09:00:00"))),
                capturedContext::set);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        PageResult<ApprovalTaskSummary> page = service.getTaskPage(100L, new ApprovalTaskQuery()
                .setModuleCode(ApprovalModuleCode.BPM)
                .setViewType(ApprovalTaskViewType.TODO)
                .setPageNo(1)
                .setPageSize(10));

        assertEquals(1L, page.getTotal());
        assertFalse(capturedContext.get().isGlobalView());
        verify(permissionApi).hasAnyRoles(100L, RoleCodeEnum.APPROVAL_ADMIN.getCode());
        verify(permissionApi, never()).hasAnyRolesOrSuperAdmin(100L, RoleCodeEnum.APPROVAL_ADMIN.getCode());
    }

    @ParameterizedTest
    @MethodSource("blankReviewReasons")
    void reviewTaskRequiresReasonBeforePreflightAndSignatureOrProviderDispatch(
            ApprovalTaskReviewResult result, String reason) {
        AtomicBoolean dispatched = new AtomicBoolean(false);
        AtomicBoolean visibilityChecked = new AtomicBoolean(false);
        ApprovalTaskProvider provider = reviewProvider(ApprovalModuleCode.EDHR, loginUserId -> {
            visibilityChecked.set(true);
            return true;
        }, context -> dispatched.set(true));
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);
        ApprovalTaskReviewCommand command = new ApprovalTaskReviewCommand()
                .setModuleCode(ApprovalModuleCode.EDHR)
                .setSourceTaskType("EDHR_WORK_TASK")
                .setSourceTaskId("9001")
                .setBusinessKey("9001")
                .setResult(result)
                .setReason(reason)
                .setSignaturePassword("secret");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reviewTask(100L, command));

        assertEquals("APPROVAL_REVIEW_REASON_REQUIRED", ex.getMessage());
        assertFalse(dispatched.get());
        assertFalse(visibilityChecked.get());
        verifyNoInteractions(permissionApi, signatureRecordService);
    }

    @Test
    void reviewTaskRequiresSignaturePasswordBeforeProviderDispatch() {
        AtomicBoolean dispatched = new AtomicBoolean(false);
        ApprovalTaskProvider provider = reviewProvider(ApprovalModuleCode.MES_FEEDBACK, loginUserId -> true,
                context -> dispatched.set(true));
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        ApprovalTaskReviewCommand command = new ApprovalTaskReviewCommand()
                .setModuleCode(ApprovalModuleCode.MES_FEEDBACK)
                .setSourceTaskType("MES_PRO_FEEDBACK")
                .setSourceTaskId("9001")
                .setResult(ApprovalTaskReviewResult.APPROVE)
                .setReason("reviewed production evidence");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.reviewTask(100L, command));

        assertEquals("APPROVAL_SIGNATURE_PASSWORD_REQUIRED", ex.getMessage());
        assertFalse(dispatched.get());
        verifyNoInteractions(signatureRecordService);
    }

    @Test
    void reviewTaskPersistsSignatureRecordBeforeDispatchingToVisibleProviderWithGlobalFlag() {
        when(permissionApi.hasAnyRoles(100L, RoleCodeEnum.APPROVAL_ADMIN.getCode())).thenReturn(true);
        AtomicReference<ApprovalTaskReviewContext> captured = new AtomicReference<>();
        List<String> events = new ArrayList<>();
        doAnswer(invocation -> {
            events.add("signature");
            return ApprovalSignatureRecordResult.builder()
                    .signatureImageFileUrl("http://127.0.0.1:9000/yudao/signature/user-100.png")
                    .subjectId("subject-9001")
                    .evidenceHash("evidence-9001")
                    .build();
        }).when(signatureRecordService).recordReviewSignature(any());
        ApprovalTaskProvider provider = reviewProvider(ApprovalModuleCode.MES_FEEDBACK, loginUserId -> true,
                context -> {
                    events.add("provider");
                    captured.set(context);
                });
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        service.reviewTask(100L, new ApprovalTaskReviewCommand()
                .setModuleCode(ApprovalModuleCode.MES_FEEDBACK)
                .setSourceTaskType("MES_PRO_FEEDBACK")
                .setSourceTaskId("9001")
                .setBusinessKey("9001")
                .setResult(ApprovalTaskReviewResult.APPROVE)
                .setReason("reviewed production evidence")
                .setSignaturePassword("secret"));

        assertEquals(100L, captured.get().getLoginUserId());
        assertEquals(ApprovalModuleCode.MES_FEEDBACK, captured.get().getModuleCode());
        assertEquals("MES_PRO_FEEDBACK", captured.get().getSourceTaskType());
        assertEquals("9001", captured.get().getSourceTaskId());
        assertEquals(ApprovalTaskReviewResult.APPROVE, captured.get().getResult());
        assertEquals("secret", captured.get().getSignaturePassword());
        assertTrue(captured.get().isGlobalView());
        verify(signatureRecordService).recordReviewSignature(argThat(context ->
                context.getLoginUserId().equals(100L)
                        && context.getModuleCode() == ApprovalModuleCode.MES_FEEDBACK
                        && "MES_PRO_FEEDBACK".equals(context.getSourceTaskType())
                        && "9001".equals(context.getSourceTaskId())
                        && context.getResult() == ApprovalTaskReviewResult.APPROVE
                        && "secret".equals(context.getSignaturePassword())
                        && context.isGlobalView()));
        assertEquals(List.of("signature", "provider"), events);
    }

    @Test
    void reviewTaskPropagatesSignatureImageSnapshotToProviderContext() {
        String signatureImageFileUrl = "http://127.0.0.1:9000/yudao/signature/user-100.png";
        String signatureSubjectId = "subject-task-approve-101";
        String signatureEvidenceHash = "evidence-task-approve-101";
        when(signatureRecordService.recordReviewSignature(any())).thenReturn(
                ApprovalSignatureRecordResult.builder()
                        .signatureImageFileUrl(signatureImageFileUrl)
                        .subjectId(signatureSubjectId)
                        .evidenceHash(signatureEvidenceHash)
                        .build());
        AtomicReference<ApprovalTaskReviewContext> captured = new AtomicReference<>();
        ApprovalTaskProvider provider = reviewProvider(ApprovalModuleCode.BPM, loginUserId -> true, captured::set);
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        service.reviewTask(100L, new ApprovalTaskReviewCommand()
                .setModuleCode(ApprovalModuleCode.BPM)
                .setSourceTaskType("BPM_TASK_TODO")
                .setSourceTaskId("task-approve-101")
                .setBusinessKey("pi-approve-101")
                .setProcessInstanceId("pi-approve-101")
                .setResult(ApprovalTaskReviewResult.APPROVE)
                .setReason("reviewed production evidence")
                .setSignaturePassword("secret"));

        assertEquals(signatureImageFileUrl, captured.get().getSignatureImageFileUrl());
        assertEquals(signatureSubjectId, captured.get().getSignatureSubjectId());
        assertEquals(signatureEvidenceHash, captured.get().getSignatureEvidenceHash());
    }

    @Test
    void reviewTaskReturnsBusinessErrorWhenSignatureImageSnapshotHasNoUrl() {
        when(signatureRecordService.recordReviewSignature(any())).thenReturn(
                ApprovalSignatureRecordResult.builder().build());
        AtomicBoolean dispatched = new AtomicBoolean(false);
        ApprovalTaskProvider provider = reviewProvider(ApprovalModuleCode.BPM, loginUserId -> true,
                context -> dispatched.set(true));
        ApprovalCenterService service = new ApprovalCenterServiceImpl(
                new ApprovalTaskProviderRegistry(List.of(provider)), permissionApi, adminUserApi,
                signatureRecordService);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.reviewTask(100L, new ApprovalTaskReviewCommand()
                        .setModuleCode(ApprovalModuleCode.BPM)
                        .setSourceTaskType("BPM_TASK_TODO")
                        .setSourceTaskId("task-approve-101")
                        .setBusinessKey("pi-approve-101")
                        .setProcessInstanceId("pi-approve-101")
                        .setResult(ApprovalTaskReviewResult.APPROVE)
                        .setReason("reviewed production evidence")
                        .setSignaturePassword("secret")));

        assertEquals("审批电子签名图片缺失或未启用，请先上传并启用电子签名图片", ex.getMessage());
        assertFalse(dispatched.get());
    }

    @ParameterizedTest
    @MethodSource("validReviewRoutes")
    void reviewTaskPreservesSameOriginalOpinionInSignatureAndProviderCommands(
            ApprovalModuleCode moduleCode, ApprovalTaskReviewResult result) {
        String originalReason = "\u2003  production evidence checked\n  user explanation kept  \r\n\u2003";
        String normalizedReason = "production evidence checked\n  user explanation kept";
        ElectronicSignatureService kernel = mock(ElectronicSignatureService.class);
        ApprovalSignatureImageSnapshotProvider images = mock(ApprovalSignatureImageSnapshotProvider.class);
        ApprovalTaskProvider provider = mock(ApprovalTaskProvider.class,
                withSettings().extraInterfaces(ApprovalCenterReviewPreflight.class));
        when(provider.getModuleCode()).thenReturn(moduleCode);
        when(provider.getSupportedViewTypes()).thenReturn(Set.of(ApprovalTaskViewType.TODO));
        when(provider.isVisibleTo(100L)).thenReturn(true);
        when(images.requireActiveSnapshot(100L)).thenReturn(ApprovalSignatureImageSnapshot.builder()
                .imageId(9101L).versionNo(2).fileId(8101L).fileUrl("/signature/test.png")
                .sha256("image-hash").contentType("image/png").fileSize(2048L)
                .imageStatus("ACTIVE").verifiedStatus("VALID").build());
        when(kernel.sign(any())).thenReturn(new ElectronicSignatureResult(810004L, "VALID", LocalDateTime.now(),
                "SERVER_CLOCK:now", "version", "contentHash", "evidenceHash", "SHA-256", "system-local-v1"));
        ApprovalCenterService service = new ApprovalCenterServiceImpl(new ApprovalTaskProviderRegistry(List.of(provider)),
                permissionApi, adminUserApi, new ApprovalSignatureRecordServiceImpl(kernel, images));
        ApprovalTaskReviewCommand command = new ApprovalTaskReviewCommand().setModuleCode(moduleCode)
                .setSourceTaskType(moduleCode == ApprovalModuleCode.EDHR
                        ? "EDHR_WORK_TASK" : "MES_PRO_FEEDBACK")
                .setSourceTaskId("9001").setBusinessKey("9001").setResult(result)
                .setReason(originalReason).setSignaturePassword("test-credential");

        service.reviewTask(100L, command);

        ArgumentCaptor<ElectronicSignatureCommand> signatureCommand = ArgumentCaptor.forClass(ElectronicSignatureCommand.class);
        ArgumentCaptor<ApprovalTaskReviewContext> providerCommand = ArgumentCaptor.forClass(ApprovalTaskReviewContext.class);
        var ordered = inOrder(provider, images, kernel);
        if (moduleCode == ApprovalModuleCode.EDHR) {
            ordered.verify((ApprovalCenterReviewPreflight) provider).preflightReview(any());
        } else {
            verify((ApprovalCenterReviewPreflight) provider, never()).preflightReview(any());
        }
        ordered.verify(images).requireActiveSnapshot(100L);
        ordered.verify(kernel).sign(signatureCommand.capture());
        ordered.verify(images).markReferenced(9101L);
        ordered.verify(provider).review(providerCommand.capture());
        assertEquals(normalizedReason, signatureCommand.getValue().reason());
        assertEquals(normalizedReason, providerCommand.getValue().getReason());
        assertEquals(signatureCommand.getValue().reason(), providerCommand.getValue().getReason());
        assertEquals(result, providerCommand.getValue().getResult());
        assertEquals(originalReason, command.getReason());
    }

    private static Stream<Arguments> blankReviewReasons() {
        return Stream.of(ApprovalTaskReviewResult.values()).flatMap(result ->
                Stream.of(null, "", "  ", " \t\r\n ", "\u2003", "\u2003 \t\r\n\u2003", "\000", "\u2003\000 ",
                                "\u00a0", "\u202f\u2007", "\uFEFF")
                        .map(reason -> Arguments.of(result, reason)));
    }

    private static Stream<Arguments> validReviewRoutes() {
        return Stream.of(Arguments.of(ApprovalModuleCode.EDHR, ApprovalTaskReviewResult.APPROVE),
                Arguments.of(ApprovalModuleCode.MES_FEEDBACK, ApprovalTaskReviewResult.APPROVE),
                Arguments.of(ApprovalModuleCode.MES_FEEDBACK, ApprovalTaskReviewResult.REJECT));
    }

    private static ApprovalTaskProvider provider(ApprovalModuleCode moduleCode,
                                                   Set<ApprovalTaskViewType> supportedViewTypes,
                                                   List<ApprovalTaskSummary> rows,
                                                  Predicate<Long> visibilityPredicate) {
        return new ApprovalTaskProvider() {

            @Override
            public ApprovalModuleCode getModuleCode() {
                return moduleCode;
            }

            @Override
            public String getModuleName() {
                return moduleCode.name();
            }

            @Override
            public String getProviderCode() {
                return moduleCode.name().toLowerCase() + "-provider";
            }

            @Override
            public String getProviderVersion() {
                return "phase1";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return supportedViewTypes;
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return Set.of(ApprovalTaskCapability.TIMELINE);
            }

            @Override
            public boolean isVisibleTo(Long loginUserId) {
                return visibilityPredicate.test(loginUserId);
            }

            @Override
            public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
                assertEquals(100L, context.getLoginUserId());
                assertEquals(ApprovalTaskViewType.TODO, context.getViewType());
                return new PageResult<>(rows, (long) rows.size());
            }

            @Override
            public PageResult<ApprovalTaskSummary> window(ApprovalTaskQueryContext context,ApprovalQueryWindow window) {
                assertEquals(100L,context.getLoginUserId());
                var ordered=new ArrayList<>(rows);
                ordered.forEach(row -> row.setModuleCode(moduleCode));
                return window.slice(ApprovalCenterServiceImpl.sortRows(ordered,context.getViewType()));
            }

            @Override
            public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
                return List.of();
            }
        };
    }

    private static ApprovalTaskProvider pagedProvider(ApprovalModuleCode moduleCode,
                                                      List<ApprovalTaskSummary> rows,
                                                      Consumer<ApprovalTaskQueryContext> contextRecorder) {
        return new ApprovalTaskProvider() {

            @Override
            public ApprovalModuleCode getModuleCode() {
                return moduleCode;
            }

            @Override
            public String getModuleName() {
                return moduleCode.name();
            }

            @Override
            public String getProviderCode() {
                return moduleCode.name().toLowerCase() + "-provider";
            }

            @Override
            public String getProviderVersion() {
                return "phase1";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return Set.of(ApprovalTaskViewType.TODO);
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return Set.of(ApprovalTaskCapability.TIMELINE);
            }

            @Override
            public boolean isVisibleTo(Long loginUserId) {
                return true;
            }

            @Override
            public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
                contextRecorder.accept(context);
                int safePageNo = context.getPageNo() == null || context.getPageNo() < 1 ? 1 : context.getPageNo();
                int safePageSize = context.getPageSize() == null || context.getPageSize() < 1
                        ? 10 : context.getPageSize();
                int fromIndex = Math.min((safePageNo - 1) * safePageSize, rows.size());
                int toIndex = Math.min(fromIndex + safePageSize, rows.size());
                return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
            }

            @Override
            public PageResult<ApprovalTaskSummary> window(ApprovalTaskQueryContext context,ApprovalQueryWindow window) {
                contextRecorder.accept(context);
                var ordered=new ArrayList<>(rows);
                ordered.forEach(row -> row.setModuleCode(moduleCode));
                return window.slice(ApprovalCenterServiceImpl.sortRows(ordered,context.getViewType()));
            }

            @Override
            public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
                return List.of();
            }
        };
    }

    private static ApprovalTaskProvider inconsistentTotalProvider(ApprovalModuleCode moduleCode, long total) {
        return new ApprovalTaskProvider() {

            @Override
            public ApprovalModuleCode getModuleCode() {
                return moduleCode;
            }

            @Override
            public String getModuleName() {
                return moduleCode.name();
            }

            @Override
            public String getProviderCode() {
                return moduleCode.name().toLowerCase() + "-inconsistent-provider";
            }

            @Override
            public String getProviderVersion() {
                return "phase1";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return Set.of(ApprovalTaskViewType.TODO);
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return Set.of(ApprovalTaskCapability.TIMELINE);
            }

            @Override
            public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
                return new PageResult<>(List.of(), total);
            }

            @Override
            public PageResult<ApprovalTaskSummary> window(ApprovalTaskQueryContext context,ApprovalQueryWindow window) {
                return new PageResult<>(List.of(),total);
            }

            @Override
            public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
                return List.of();
            }
        };
    }

    private static ApprovalTaskProvider reviewProvider(ApprovalModuleCode moduleCode,
                                                       Predicate<Long> visibilityPredicate,
                                                       Consumer<ApprovalTaskReviewContext> reviewer) {
        return new ApprovalTaskProvider() {

            @Override
            public ApprovalModuleCode getModuleCode() {
                return moduleCode;
            }

            @Override
            public String getModuleName() {
                return moduleCode.name();
            }

            @Override
            public String getProviderCode() {
                return moduleCode.name().toLowerCase() + "-provider";
            }

            @Override
            public String getProviderVersion() {
                return "phase1";
            }

            @Override
            public Set<ApprovalTaskViewType> getSupportedViewTypes() {
                return Set.of(ApprovalTaskViewType.TODO);
            }

            @Override
            public Set<ApprovalTaskCapability> getCapabilities() {
                return Set.of(ApprovalTaskCapability.TIMELINE);
            }

            @Override
            public boolean isVisibleTo(Long loginUserId) {
                return visibilityPredicate.test(loginUserId);
            }

            @Override
            public PageResult<ApprovalTaskSummary> page(ApprovalTaskQueryContext context) {
                return new PageResult<>(List.of(), 0L);
            }

            @Override
            public PageResult<ApprovalTaskSummary> window(ApprovalTaskQueryContext context,ApprovalQueryWindow window) {
                return PageResult.empty();
            }

            @Override
            public List<ApprovalTaskTimelineEntry> listTimeline(ApprovalTaskTimelineQueryContext context) {
                return List.of();
            }

            @Override
            public void review(ApprovalTaskReviewContext context) {
                reviewer.accept(context);
            }
        };
    }

    private static ApprovalTaskSummary summary(String id, LocalDateTime taskCreatedAt) {
        return ApprovalTaskSummary.builder()
                .id(id)
                .moduleCode(ApprovalModuleCode.SHOWROOM)
                .sourceTaskType("TEST_TASK")
                .sourceTaskId(id)
                .businessTitle(id)
                .detailRoute("/test/detail")
                .taskCreatedAt(taskCreatedAt)
                .capabilities(Set.of(ApprovalTaskCapability.TIMELINE))
                .build();
    }

    private static List<ApprovalTaskSummary> rows(ApprovalModuleCode moduleCode, String prefix, int count,
                                                  LocalDateTime firstTaskCreatedAt) {
        List<ApprovalTaskSummary> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(ApprovalTaskSummary.builder()
                    .id(prefix + "-" + i)
                    .moduleCode(moduleCode)
                    .sourceTaskType("TEST_TASK")
                    .sourceTaskId(prefix + "-" + i)
                    .businessTitle(prefix + "-" + i)
                    .detailRoute("/test/detail")
                    .taskCreatedAt(firstTaskCreatedAt.minusMinutes(i))
                    .capabilities(Set.of(ApprovalTaskCapability.TIMELINE))
                    .build());
        }
        return rows;
    }
}
