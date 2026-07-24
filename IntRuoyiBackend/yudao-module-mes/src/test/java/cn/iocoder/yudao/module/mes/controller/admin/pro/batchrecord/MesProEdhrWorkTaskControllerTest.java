package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskArchiveRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskAssignmentRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskStatsRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrWorkTaskControllerTest {

    @Mock
    private MesProEdhrWorkTaskService workTaskService;
    @InjectMocks
    private MesProEdhrWorkTaskController controller;

    @Test
    void archiveRuleEndpoints_delegateToService() {
        MesProEdhrWorkTaskAssignmentRuleRespVO rule = new MesProEdhrWorkTaskAssignmentRuleRespVO()
                .setId(10L)
                .setScopeType("ROUTE")
                .setScopeId(20L)
                .setTaskType("ARCHIVE")
                .setAssigneeUserId(30L)
                .setEnabled(true);
        MesProEdhrWorkTaskArchiveRuleReqVO reqVO = new MesProEdhrWorkTaskArchiveRuleReqVO()
                .setRouteId(20L)
                .setAssigneeUserId(30L)
                .setDueMinutes(240)
                .setEnabled(true)
                .setRemark("最终归档责任人");
        when(workTaskService.getArchiveRuleByRoute(20L)).thenReturn(rule);
        when(workTaskService.saveArchiveRule(reqVO)).thenReturn(rule);

        assertSame(rule, controller.getArchiveRuleByRoute(20L).getData());
        assertSame(rule, controller.saveArchiveRule(reqVO).getData());

        verify(workTaskService).getArchiveRuleByRoute(20L);
        verify(workTaskService).saveArchiveRule(reqVO);
    }

    @Test
    void candidateSignatureEndpoints_delegateToService() {
        MesProEdhrWorkTaskPageReqVO reqVO = new MesProEdhrWorkTaskPageReqVO().setTaskType("REVIEW");
        MesProEdhrWorkTaskRespVO task = new MesProEdhrWorkTaskRespVO().setId(66L).setExecutionId(77L);
        PageResult<MesProEdhrWorkTaskRespVO> page = new PageResult<>(List.of(task), 1L);
        when(workTaskService.getCandidateSignatureTodoPage(reqVO)).thenReturn(page);

        assertSame(page, controller.getCandidateTodoPage(reqVO).getData());
        assertEquals(Boolean.TRUE, controller.completeCandidateSignatureTask(66L, 77L).getData());
        assertEquals(Boolean.TRUE, controller.reassignFillTask(88L, "规则变更后重新派发").getData());

        verify(workTaskService).getCandidateSignatureTodoPage(reqVO);
        verify(workTaskService).completeCandidateSignatureTask(66L, 77L);
        verify(workTaskService).reassignFillTask(88L, "规则变更后重新派发");
    }

    @Test
    void archiveRuleEndpoints_matchPermissions() throws Exception {
        Method stats = MesProEdhrWorkTaskController.class.getDeclaredMethod("getStats");
        assertArrayEquals(new String[]{"/stats"}, stats.getAnnotation(GetMapping.class).value());

        Method getRule = MesProEdhrWorkTaskController.class.getDeclaredMethod("getArchiveRuleByRoute", Long.class);
        assertArrayEquals(new String[]{"/route-archive-rule"}, getRule.getAnnotation(GetMapping.class).value());
        assertEquals("routeId", getRule.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')",
                getRule.getAnnotation(PreAuthorize.class).value());

        Method saveRule = MesProEdhrWorkTaskController.class.getDeclaredMethod("saveArchiveRule",
                MesProEdhrWorkTaskArchiveRuleReqVO.class);
        assertArrayEquals(new String[]{"/route-archive-rule"}, saveRule.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')",
                saveRule.getAnnotation(PreAuthorize.class).value());

        Method candidatePage = MesProEdhrWorkTaskController.class.getDeclaredMethod("getCandidateTodoPage",
                MesProEdhrWorkTaskPageReqVO.class);
        assertArrayEquals(new String[]{"/candidate-todo-page"}, candidatePage.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-work-task:query')",
                candidatePage.getAnnotation(PreAuthorize.class).value());

        Method completeCandidateSignature = MesProEdhrWorkTaskController.class.getDeclaredMethod(
                "completeCandidateSignatureTask", Long.class, Long.class);
        assertArrayEquals(new String[]{"/candidate-signature/complete"},
                completeCandidateSignature.getAnnotation(PostMapping.class).value());
        assertEquals("workTaskId",
                completeCandidateSignature.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("executionId",
                completeCandidateSignature.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-work-task:update')",
                completeCandidateSignature.getAnnotation(PreAuthorize.class).value());

        Method reassignFillTask = MesProEdhrWorkTaskController.class.getDeclaredMethod(
                "reassignFillTask", Long.class, String.class);
        assertArrayEquals(new String[]{"/fill-task/reassign"},
                reassignFillTask.getAnnotation(PostMapping.class).value());
        assertEquals("workTaskId",
                reassignFillTask.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("reason",
                reassignFillTask.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-work-task:update')",
                reassignFillTask.getAnnotation(PreAuthorize.class).value());

        MesProEdhrWorkTaskArchiveRuleReqVO.class.getDeclaredMethod("getRouteId");
        MesProEdhrWorkTaskArchiveRuleReqVO.class.getDeclaredMethod("getAssigneeUserId");
        MesProEdhrWorkTaskAssignmentRuleRespVO.class.getDeclaredMethod("getTaskType");
        MesProEdhrWorkTaskAssignmentRuleRespVO.class.getDeclaredMethod("getScopeType");
        MesProEdhrWorkTaskPageReqVO.class.getDeclaredMethod("getTaskType");
        MesProEdhrWorkTaskStatsRespVO.class.getDeclaredMethod("getArchiveCount");
    }
}
