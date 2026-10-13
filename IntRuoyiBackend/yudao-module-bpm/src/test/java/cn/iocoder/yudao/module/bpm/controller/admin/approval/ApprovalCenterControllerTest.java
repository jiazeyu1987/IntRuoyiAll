package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskCapability;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskViewType;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterService;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalProviderDescriptor;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskQuery;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskReviewCommand;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class ApprovalCenterControllerTest {

    @Mock
    private ApprovalCenterService approvalCenterService;
    @Mock
    private cn.iocoder.yudao.module.bpm.approval.content.ApprovalCenterReviewFacade reviewFacade;
    @InjectMocks
    private ApprovalCenterController controller;

    @BeforeEach
    void injectReviewFacadeAfterConstructorInjection() {
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "reviewFacade", reviewFacade);
    }

    @Test
    void modulesEndpointDelegatesToService() {
        List<ApprovalProviderDescriptor> descriptors = List.of(new ApprovalProviderDescriptor()
                .setModuleCode(ApprovalModuleCode.SHOWROOM)
                .setModuleName("Showroom 审批")
                .setProviderCode("showroom-approval")
                .setProviderVersion("phase1")
                .setSupportedViewTypes(Set.of(ApprovalTaskViewType.TODO))
                .setCapabilities(Set.of(ApprovalTaskCapability.TIMELINE)));
        when(approvalCenterService.listProviders(100L)).thenReturn(descriptors);

        try (MockedStatic<WebFrameworkUtils> webFrameworkUtils = mockStatic(WebFrameworkUtils.class)) {
            webFrameworkUtils.when(WebFrameworkUtils::getLoginUserId).thenReturn(100L);

            assertSame(descriptors, controller.getModules().getData());
        }

        verify(approvalCenterService).listProviders(100L);
    }

    @Test
    void tasksEndpointDelegatesToService() {
        ApprovalTaskPageReqVO reqVO = new ApprovalTaskPageReqVO()
                .setViewType(ApprovalTaskViewType.TODO)
                .setModuleCode(ApprovalModuleCode.SHOWROOM);
        PageResult<ApprovalTaskSummary> page = PageResult.empty();
        when(approvalCenterService.getTaskPage(100L, reqVO.toQuery())).thenReturn(page);

        try (MockedStatic<WebFrameworkUtils> webFrameworkUtils = mockStatic(WebFrameworkUtils.class)) {
            webFrameworkUtils.when(WebFrameworkUtils::getLoginUserId).thenReturn(100L);

            assertSame(page, controller.getTaskPage(reqVO).getData());
        }

        verify(approvalCenterService).getTaskPage(100L, reqVO.toQuery());
    }

    @Test
    void controllerRoutesAndPermissionsAreStable() throws Exception {
        assertArrayEquals(new String[]{"/approval-center"},
                ApprovalCenterController.class.getAnnotation(RequestMapping.class).value());

        Method modules = ApprovalCenterController.class.getDeclaredMethod("getModules");
        assertArrayEquals(new String[]{"/modules"}, modules.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:task:query')",
                modules.getAnnotation(PreAuthorize.class).value());

        Method tasks = ApprovalCenterController.class.getDeclaredMethod("getTaskPage", ApprovalTaskPageReqVO.class);
        assertArrayEquals(new String[]{"/tasks/page"}, tasks.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:task:query')",
                tasks.getAnnotation(PreAuthorize.class).value());

        Method review = ApprovalCenterController.class.getDeclaredMethod("reviewTask", ApprovalTaskReviewReqVO.class);
        assertArrayEquals(new String[]{"/tasks/review"}, review.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('bpm:task:update')",
                review.getAnnotation(PreAuthorize.class).value());

        ApprovalTaskPageReqVO.class.getDeclaredMethod("toQuery");
        ApprovalTaskReviewReqVO.class.getDeclaredMethod("getSignaturePassword");
        ApprovalTaskQuery.class.getDeclaredMethod("getViewType");
        ApprovalTaskSummary.class.getDeclaredMethod("getDetailRoute");
        ApprovalTaskSummary.class.getDeclaredMethod("getInitiatorUserName");
    }

    @ParameterizedTest
    @MethodSource("blankReviewReasons")
    void reviewRequestVoRejectsBlankOpinionForBothResults(ApprovalTaskReviewResult result, String reason) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(reviewRequest(result, reason));
            assertTrue(!violations.isEmpty());
            assertTrue(violations.stream().allMatch(violation ->
                    "reason".equals(violation.getPropertyPath().toString())
                            && "审核意见不能为空".equals(violation.getMessage())));
        }
        verifyNoInteractions(approvalCenterService);
    }

    @ParameterizedTest
    @MethodSource("blankReviewReasons")
    void reviewEndpointRejectsBlankOpinionBeforeServiceDispatch(ApprovalTaskReviewResult result, String reason)
            throws Exception {
        try (LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvcResult = standaloneSetup(controller).setValidator(validator).build().perform(
                    post("/approval-center/tasks/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(reviewRequest(result, reason))))
                    .andExpect(status().isBadRequest()).andReturn();
            MethodArgumentNotValidException ex = assertInstanceOf(MethodArgumentNotValidException.class,
                    mvcResult.getResolvedException());
            assertEquals("审核意见不能为空", ex.getBindingResult().getFieldError("reason").getDefaultMessage());
        }
        verifyNoInteractions(approvalCenterService);
    }

    @ParameterizedTest
    @EnumSource(ApprovalTaskReviewResult.class)
    void reviewEndpointPassesOriginalOpinionToCenterCommand(ApprovalTaskReviewResult result) throws Exception {
        String originalReason = "\u2003  production evidence checked\n  user explanation kept  \r\n\u2003";
        ApprovalTaskReviewReqVO request = reviewRequest(result, originalReason);
        var receipt = new cn.iocoder.yudao.module.bpm.approval.content.ApprovalDecisionReceipt("receipt", "request", "prepare", "signature", "a".repeat(64), result.name(), "COMMITTED", "2026-10-10T00:00:00Z");
        when(reviewFacade.review(eq(100L), org.mockito.ArgumentMatchers.any())).thenReturn(receipt);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
        }
        try (MockedStatic<WebFrameworkUtils> webFrameworkUtils = mockStatic(WebFrameworkUtils.class)) {
            webFrameworkUtils.when(WebFrameworkUtils::getLoginUserId).thenReturn(100L);
            assertSame(receipt, controller.reviewTask(request).getData());
        }
        ArgumentCaptor<ApprovalTaskReviewCommand> captor = ArgumentCaptor.forClass(ApprovalTaskReviewCommand.class);
        verify(reviewFacade).review(eq(100L), captor.capture());
        assertEquals(result, captor.getValue().getResult());
        assertEquals(originalReason, captor.getValue().getReason());
    }

    private static ApprovalTaskReviewReqVO reviewRequest(ApprovalTaskReviewResult result, String reason) {
        return new ApprovalTaskReviewReqVO().setModuleCode(ApprovalModuleCode.EDHR)
                .setSourceTaskType("EDHR_WORK_TASK").setSourceTaskId("9001").setBusinessKey("9001")
                .setResult(result).setReason(reason).setSignaturePassword("test-credential");
    }

    private static Stream<Arguments> blankReviewReasons() {
        return Stream.of(ApprovalTaskReviewResult.values()).flatMap(result ->
                Stream.of(null, "", "  ", " \t\r\n ", "\u2003", "\u2003 \t\r\n\u2003", "\000", "\u2003\000 ",
                                "\u00a0", "\u202f\u2007", "\uFEFF")
                        .map(reason -> Arguments.of(result, reason)));
    }
}
