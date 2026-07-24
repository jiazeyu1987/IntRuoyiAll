package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleDependencyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProLatestScheduleApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoSchedulePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttLinkRespVO;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProAutoScheduleService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProAutoScheduleControllerContractTest {

    @Mock
    private MesProAutoScheduleService autoScheduleService;

    @InjectMocks
    private MesProAutoScheduleController controller;

    @Test
    void previewRequestScope_shouldDocumentOnlyScheduleOrderIdsAsPublicScope() throws Exception {
        Field workOrderIds = MesProAutoSchedulePreviewReqVO.class.getDeclaredField("workOrderIds");
        Field scheduleOrderIds = MesProAutoSchedulePreviewReqVO.class.getDeclaredField("scheduleOrderIds");

        Schema workOrderSchema = workOrderIds.getAnnotation(Schema.class);
        Schema scheduleOrderSchema = scheduleOrderIds.getAnnotation(Schema.class);

        assertTrue(workOrderSchema.hidden(), "workOrderIds 只能作为服务内部派生字段，不能暴露成客户端范围入口");
        assertEquals("排产工单编号列表", scheduleOrderSchema.description());
        assertEquals(Schema.RequiredMode.REQUIRED, scheduleOrderSchema.requiredMode());
    }

    @Test
    void preview_shouldRejectWorkOrderIdsOnlyAtControllerBoundary() {
        MesProAutoSchedulePreviewReqVO reqVO = buildPreviewReq();
        reqVO.setWorkOrderIds(List.of(1L));
        reqVO.setScheduleOrderIds(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.preview(reqVO));

        assertEquals("排产工单编号列表不能为空", exception.getMessage());
        verifyNoInteractions(autoScheduleService);
    }

    @Test
    void apply_shouldRejectWorkOrderIdsOnlyAtControllerBoundary() {
        MesProAutoSchedulePreviewReqVO reqVO = buildPreviewReq();
        reqVO.setWorkOrderIds(List.of(1L));
        reqVO.setScheduleOrderIds(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.apply(reqVO));

        assertEquals("排产工单编号列表不能为空", exception.getMessage());
        verifyNoInteractions(autoScheduleService);
    }

    @Test
    void replanPreview_shouldRejectWorkOrderIdsOnlyAtControllerBoundary() {
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setWorkOrderIds(List.of(1L));
        reqVO.setScheduleOrderIds(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.replanPreview(reqVO));

        assertEquals("排产工单编号列表不能为空", exception.getMessage());
        verifyNoInteractions(autoScheduleService);
    }

    @Test
    void replanApply_shouldRejectWorkOrderIdsOnlyAtControllerBoundary() {
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setWorkOrderIds(List.of(1L));
        reqVO.setScheduleOrderIds(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> controller.replanApply(reqVO));

        assertEquals("排产工单编号列表不能为空", exception.getMessage());
        verifyNoInteractions(autoScheduleService);
    }

    @Test
    void replanApply_shouldApplyImmediatelyWithoutFormCenterApproval() {
        MesProAutoScheduleReplanReqVO reqVO = buildReplanReq();
        reqVO.setIdempotencyKey("M5-REPLAN-501");
        MesProAutoScheduleApplyRespVO applied = new MesProAutoScheduleApplyRespVO();
        applied.setApplied(true);
        when(autoScheduleService.replanApply(reqVO)).thenReturn(applied);

        CommonResult<MesProAutoScheduleApplyRespVO> response = controller.replanApply(reqVO);

        assertEquals(0, response.getCode());
        assertEquals(applied, response.getData());
        verify(autoScheduleService).replanApply(reqVO);
    }

    @Test
    void dependencies_shouldAcceptPostJsonBodyForLargeWorkOrderScopes() throws Exception {
        Method method = MesProAutoScheduleController.class.getDeclaredMethod(
                "getDependencies", MesProAutoScheduleDependencyReqVO.class);

        assertArrayEquals(new String[]{"/dependencies"}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasAnyPermissions('mes:pro-task:query', 'mes:pro-schedule-order:query')",
                method.getAnnotation(PreAuthorize.class).value());
        assertTrue(hasParameterAnnotation(method, 0, RequestBody.class));

        MesProAutoScheduleDependencyReqVO reqVO = new MesProAutoScheduleDependencyReqVO();
        reqVO.setWorkOrderIds(List.of(1L, 2L, 3L));
        reqVO.setTaskIds(List.of(10L, 11L));
        List<GanttLinkRespVO> links = List.of(new GanttLinkRespVO());
        when(autoScheduleService.getDependencies(reqVO.getWorkOrderIds(), reqVO.getTaskIds())).thenReturn(links);

        CommonResult<List<GanttLinkRespVO>> response = controller.getDependencies(reqVO);

        assertEquals(0, response.getCode());
        assertEquals(links, response.getData());
        verify(autoScheduleService).getDependencies(reqVO.getWorkOrderIds(), reqVO.getTaskIds());
    }

    @Test
    void latestReplanExplanation_shouldUseWorkbenchQueryPermission() throws Exception {
        Method method = MesProAutoScheduleController.class.getDeclaredMethod("getLatestReplanExplanation");

        assertArrayEquals(new String[]{"/replan/explanation/latest"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-scheduler-workbench:query')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void latestSuccessfulScheduleApply_shouldUseScheduleOrderQueryPermissionAndReturnServiceResult() throws Exception {
        Method method = MesProAutoScheduleController.class.getDeclaredMethod("getLatestSuccessfulScheduleApply");

        assertArrayEquals(new String[]{"/apply/latest-success"}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:query')",
                method.getAnnotation(PreAuthorize.class).value());

        MesProLatestScheduleApplyRespVO latest = new MesProLatestScheduleApplyRespVO();
        latest.setHasData(true);
        latest.setAppliedAt(LocalDateTime.of(2026, 7, 21, 9, 30));
        latest.setOperationType("AUTO_APPLY");
        when(autoScheduleService.getLatestSuccessfulScheduleApply()).thenReturn(latest);

        CommonResult<MesProLatestScheduleApplyRespVO> response = controller.getLatestSuccessfulScheduleApply();

        assertEquals(0, response.getCode());
        assertEquals(latest, response.getData());
        verify(autoScheduleService).getLatestSuccessfulScheduleApply();
    }

    private MesProAutoSchedulePreviewReqVO buildPreviewReq() {
        MesProAutoSchedulePreviewReqVO reqVO = new MesProAutoSchedulePreviewReqVO();
        reqVO.setScheduleOrderIds(List.of(100L));
        reqVO.setStartTime(LocalDateTime.of(2026, 7, 14, 8, 0));
        reqVO.setRuntimeCapacityBasis("PLANNED");
        return reqVO;
    }

    private MesProAutoScheduleReplanReqVO buildReplanReq() {
        MesProAutoScheduleReplanReqVO reqVO = new MesProAutoScheduleReplanReqVO();
        reqVO.setScheduleOrderIds(List.of(100L));
        reqVO.setStartTime(LocalDateTime.of(2026, 7, 14, 8, 0));
        reqVO.setRuntimeCapacityBasis("PLANNED");
        return reqVO;
    }

    private boolean hasParameterAnnotation(
            Method method, int parameterIndex, Class<? extends Annotation> annotationType) {
        for (Annotation annotation : method.getParameterAnnotations()[parameterIndex]) {
            if (annotation.annotationType().equals(annotationType)) {
                return true;
            }
        }
        return false;
    }

}
