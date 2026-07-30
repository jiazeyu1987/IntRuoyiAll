package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceParameterRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeBindingDisableReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesWorkOrderAbnormalReportReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonCatalogService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderWorkbenchService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolTeamLeaderControllerTest {

    @Mock
    private MesTeamLeaderWorkbenchService workbenchService;
    @Mock
    private MesTeamLeaderSubmissionReviewService submissionReviewService;
    @Mock
    private MesWorkOrderAbnormalReportService abnormalReportService;
    @Mock
    private MesTeamEmployeeBindingService employeeBindingService;
    @Mock
    private MesDefectReasonCatalogService defectReasonCatalogService;
    @Mock
    private MesProcessDeviceParameterRuleService deviceParameterRuleService;

    @InjectMocks
    private MesProcessPoolTeamLeaderController controller;

    @Test
    void getSubmissionPage_delegatesCurrentLoginLeaderAndScopedFilters() {
        MesTeamLeaderSubmissionPageReqVO reqVO = new MesTeamLeaderSubmissionPageReqVO();
        reqVO.setLeaderType("PRODUCTION");
        reqVO.setSubmitDate(LocalDate.of(2026, 7, 30));
        reqVO.setProcessId(6001L);
        PageResult<ProcessPoolTimelineEventRespVO> pageResult =
                new PageResult<>(Collections.emptyList(), 0L);
        when(workbenchService.getSubmissionPage(3001L, "PRODUCTION", reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<ProcessPoolTimelineEventRespVO>> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            response = controller.getSubmissionPage(reqVO);
        }

        assertEquals(pageResult, response.getData());
        verify(workbenchService).getSubmissionPage(3001L, "PRODUCTION", reqVO);
    }

    @Test
    void reviewSubmission_usesLoginUserAsLeaderNotClientProvidedUser() {
        when(submissionReviewService.reviewSubmission(org.mockito.ArgumentMatchers.any())).thenReturn(9101L);

        MesTeamLeaderSubmissionReviewReqVO reqVO = new MesTeamLeaderSubmissionReviewReqVO()
                .setEventId(1001L)
                .setLeaderType("PQC")
                .setReviewStatus("APPROVED")
                .setReviewRemark("已复核");

        CommonResult<Long> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3002L);
            response = controller.reviewSubmission(reqVO);
        }

        assertEquals(9101L, response.getData());
        ArgumentCaptor<MesTeamLeaderSubmissionReviewReqBO> captor =
                ArgumentCaptor.forClass(MesTeamLeaderSubmissionReviewReqBO.class);
        verify(submissionReviewService).reviewSubmission(captor.capture());
        assertEquals(3002L, captor.getValue().getLeaderUserId());
        assertEquals("PQC", captor.getValue().getLeaderType());
        assertEquals(1001L, captor.getValue().getEventId());
        assertEquals("APPROVED", captor.getValue().getReviewStatus());
    }

    @Test
    void maintenanceRequestsInjectCurrentLeaderUserIntoServiceCommands() {
        when(employeeBindingService.addEmployeeBinding(org.mockito.ArgumentMatchers.any())).thenReturn(8201L);
        when(defectReasonCatalogService.createReason(org.mockito.ArgumentMatchers.any())).thenReturn(8301L);
        when(deviceParameterRuleService.saveRule(org.mockito.ArgumentMatchers.any())).thenReturn(8401L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            assertEquals(8201L, controller.addEmployeeBinding(new MesTeamEmployeeBindingSaveReqVO()
                    .setProcessId(6001L)
                    .setEmployeeUserId(2001L)).getData());
            controller.disableEmployeeBinding(new MesTeamEmployeeBindingDisableReqVO().setBindingId(8201L));
            assertEquals(8301L, controller.createDefectReason(new MesTeamDefectReasonSaveReqVO()
                    .setProcessId(6001L)
                    .setReasonType("LOSS")
                    .setReasonCode("LOSS-001")
                    .setReasonName("损耗")).getData());
            assertEquals(8401L, controller.saveDeviceParameterRule(new MesTeamDeviceParameterRuleSaveReqVO()
                    .setProcessId(6001L)
                    .setDeviceId(7001L)
                    .setParameterCode("pressure")
                    .setParameterName("压力")
                    .setLowerLimit(new BigDecimal("20"))
                    .setUpperLimit(new BigDecimal("40"))
                    .setValueType("DECIMAL")).getData());
        }

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingSaveReqBO>
                bindingCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingSaveReqBO.class);
        verify(employeeBindingService).addEmployeeBinding(bindingCaptor.capture());
        assertEquals(3001L, bindingCaptor.getValue().getLeaderUserId());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingDisableReqBO>
                disableCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingDisableReqBO.class);
        verify(employeeBindingService).disableEmployeeBinding(disableCaptor.capture());
        assertEquals(3001L, disableCaptor.getValue().getLeaderUserId());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO>
                reasonCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO.class);
        verify(defectReasonCatalogService).createReason(reasonCaptor.capture());
        assertEquals(3001L, reasonCaptor.getValue().getLeaderUserId());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleSaveReqBO>
                ruleCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleSaveReqBO.class);
        verify(deviceParameterRuleService).saveRule(ruleCaptor.capture());
        assertEquals(3001L, ruleCaptor.getValue().getLeaderUserId());
    }

    @Test
    void mappingsAndPermissions_matchTeamLeaderWorkbenchContract() throws Exception {
        RequestMapping requestMapping = MesProcessPoolTeamLeaderController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/mes/pro/process-pool/team-leader"}, requestMapping.value());

        assertEndpoint("getSubmissionPage", new Class[]{MesTeamLeaderSubmissionPageReqVO.class}, GetMapping.class,
                new String[]{"/submission/page"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("getSubmissionDetail", new Class[]{Long.class, String.class}, GetMapping.class,
                new String[]{"/submission/detail"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("reviewSubmission", new Class[]{MesTeamLeaderSubmissionReviewReqVO.class}, PostMapping.class,
                new String[]{"/submission/review"}, "mes:pro-process-pool-team-leader:review");
        assertEndpoint("markAndReportWorkOrderAbnormal", new Class[]{MesWorkOrderAbnormalReportReqVO.class},
                PostMapping.class, new String[]{"/work-order/abnormal/report"},
                "mes:pro-process-pool-team-leader:abnormal");
        assertEndpoint("addEmployeeBinding", new Class[]{MesTeamEmployeeBindingSaveReqVO.class}, PostMapping.class,
                new String[]{"/employee-binding/add"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("disableEmployeeBinding", new Class[]{MesTeamEmployeeBindingDisableReqVO.class},
                PutMapping.class, new String[]{"/employee-binding/disable"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("createDefectReason", new Class[]{MesTeamDefectReasonSaveReqVO.class}, PostMapping.class,
                new String[]{"/defect-reason/create"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("saveDeviceParameterRule", new Class[]{MesTeamDeviceParameterRuleSaveReqVO.class},
                PostMapping.class, new String[]{"/device-parameter-rule/save"},
                "mes:pro-process-pool-team-leader:maintain");

        assertNoClientLeaderUserField(MesTeamLeaderSubmissionPageReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderSubmissionReviewReqVO.class);
        assertNoClientLeaderUserField(MesWorkOrderAbnormalReportReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeBindingSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeBindingDisableReqVO.class);
        assertNoClientLeaderUserField(MesTeamDefectReasonSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamDeviceParameterRuleSaveReqVO.class);

        requireGetter(ProcessPoolTimelineDetailRespVO.class, "getOriginalPayloadJson");
    }

    private void assertEndpoint(String methodName, Class<?>[] parameterTypes,
                                Class<? extends java.lang.annotation.Annotation> mappingType,
                                String[] expectedPath, String expectedPermission) throws Exception {
        Method method = MesProcessPoolTeamLeaderController.class.getDeclaredMethod(methodName, parameterTypes);
        Object mapping = method.getAnnotation(mappingType);
        assertNotNull(mapping);
        String[] value = (String[]) mappingType.getMethod("value").invoke(mapping);
        assertArrayEquals(expectedPath, value);
        assertEquals("@ss.hasPermission('" + expectedPermission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private void assertNoClientLeaderUserField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            assertFalse("leaderUserId".equals(field.getName()),
                    "Client request VO must not accept leaderUserId: " + type.getName());
        }
    }

    private void requireGetter(Class<?> type, String getterName) throws NoSuchMethodException {
        assertNotNull(type.getMethod(getterName));
    }
}
