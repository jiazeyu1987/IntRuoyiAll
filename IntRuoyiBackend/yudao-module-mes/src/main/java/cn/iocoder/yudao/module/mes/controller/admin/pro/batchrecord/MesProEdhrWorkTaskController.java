package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskArchiveRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskAssignmentRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskCloseRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskReleaseApprovalRuleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskStatsRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES eDHR 工作任务")
@RestController
@RequestMapping("/mes/pro/edhr-work-task")
@Validated
public class MesProEdhrWorkTaskController {

    @Resource
    private MesProEdhrWorkTaskService workTaskService;

    @GetMapping("/my-page")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-edhr-work-task:query', 'mes:pro-edhr-batch-execution:query')")
    public CommonResult<PageResult<MesProEdhrWorkTaskRespVO>> getMyPage(
            @Valid MesProEdhrWorkTaskPageReqVO reqVO) {
        return success(workTaskService.getMyPage(reqVO));
    }

    @GetMapping("/done-page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<PageResult<MesProEdhrWorkTaskRespVO>> getDonePage(
            @Valid MesProEdhrWorkTaskPageReqVO reqVO) {
        return success(workTaskService.getDonePage(reqVO));
    }

    @GetMapping("/candidate-todo-page")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<PageResult<MesProEdhrWorkTaskRespVO>> getCandidateTodoPage(
            @Valid MesProEdhrWorkTaskPageReqVO reqVO) {
        return success(workTaskService.getCandidateSignatureTodoPage(reqVO));
    }

    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<MesProEdhrWorkTaskStatsRespVO> getStats() {
        return success(workTaskService.getStats());
    }

    @GetMapping("/route-archive-rule")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<MesProEdhrWorkTaskAssignmentRuleRespVO> getArchiveRuleByRoute(
            @RequestParam("routeId") Long routeId) {
        return success(workTaskService.getArchiveRuleByRoute(routeId));
    }

    @PostMapping("/route-archive-rule")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')")
    public CommonResult<MesProEdhrWorkTaskAssignmentRuleRespVO> saveArchiveRule(
            @Valid @RequestBody MesProEdhrWorkTaskArchiveRuleReqVO reqVO) {
        return success(workTaskService.saveArchiveRule(reqVO));
    }

    @GetMapping("/route-close-rule")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<MesProEdhrWorkTaskAssignmentRuleRespVO> getCloseRuleByRoute(
            @RequestParam("routeId") Long routeId) {
        return success(workTaskService.getCloseRuleByRoute(routeId));
    }

    @PostMapping("/route-close-rule")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')")
    public CommonResult<MesProEdhrWorkTaskAssignmentRuleRespVO> saveCloseRule(
            @Valid @RequestBody MesProEdhrWorkTaskCloseRuleReqVO reqVO) {
        return success(workTaskService.saveCloseRule(reqVO));
    }

    @GetMapping("/route-release-approval-rule")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<MesProEdhrWorkTaskAssignmentRuleRespVO> getReleaseApprovalRuleByRoute(
            @RequestParam("routeId") Long routeId) {
        return success(workTaskService.getReleaseApprovalRuleByRoute(routeId));
    }

    @PostMapping("/route-release-approval-rule")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')")
    public CommonResult<MesProEdhrWorkTaskAssignmentRuleRespVO> saveReleaseApprovalRule(
            @Valid @RequestBody MesProEdhrWorkTaskReleaseApprovalRuleReqVO reqVO) {
        return success(workTaskService.saveReleaseApprovalRule(reqVO));
    }

    @PostMapping("/candidate-signature/complete")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:update')")
    public CommonResult<Boolean> completeCandidateSignatureTask(@RequestParam("workTaskId") Long workTaskId,
                                                                @RequestParam("executionId") Long executionId) {
        workTaskService.completeCandidateSignatureTask(workTaskId, executionId);
        return success(true);
    }

    @PostMapping("/fill-task/reassign")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:update')")
    public CommonResult<Boolean> reassignFillTask(@RequestParam("workTaskId") Long workTaskId,
                                                  @RequestParam("reason") String reason) {
        workTaskService.reassignFillTask(workTaskId, reason);
        return success(true);
    }
}
