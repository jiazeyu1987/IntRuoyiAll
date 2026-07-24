package cn.iocoder.yudao.module.mes.controller.admin.pro.schedule;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.*;
import cn.iocoder.yudao.module.mes.controller.admin.pro.task.vo.GanttLinkRespVO;
import cn.iocoder.yudao.module.mes.service.pro.schedule.MesProAutoScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES 自动排产")
@RestController
@RequestMapping("/mes/pro/auto-schedule")
@Validated
public class MesProAutoScheduleController {

    @Resource
    private MesProAutoScheduleService autoScheduleService;

    private void validateScope(MesProAutoSchedulePreviewReqVO reqVO) {
        boolean hasScheduleOrders = reqVO.getScheduleOrderIds() != null && !reqVO.getScheduleOrderIds().isEmpty();
        if (!hasScheduleOrders) {
            throw new ValidationException("排产工单编号列表不能为空");
        }
    }

    @PostMapping("/preview")
    @Operation(summary = "生成自动排产预览")
    @PreAuthorize("@ss.hasPermission('mes:pro-auto-schedule:preview')")
    public CommonResult<MesProAutoSchedulePreviewRespVO> preview(@Valid @RequestBody MesProAutoSchedulePreviewReqVO reqVO) {
        validateScope(reqVO);
        return success(autoScheduleService.preview(reqVO));
    }

    @PostMapping("/apply")
    @Operation(summary = "发布当前自动排产")
    @PreAuthorize("@ss.hasPermission('mes:pro-auto-schedule:apply')")
    public CommonResult<MesProAutoScheduleApplyRespVO> apply(@Valid @RequestBody MesProAutoSchedulePreviewReqVO reqVO) {
        validateScope(reqVO);
        return success(autoScheduleService.apply(reqVO));
    }

    @PostMapping("/replan/preview")
    @Operation(summary = "生成当前范围重排预览")
    @PreAuthorize("@ss.hasPermission('mes:pro-auto-schedule:replan')")
    public CommonResult<MesProAutoScheduleReplanPreviewRespVO> replanPreview(
            @Valid @RequestBody MesProAutoScheduleReplanReqVO reqVO) {
        validateScope(reqVO);
        return success(autoScheduleService.replanPreview(reqVO));
    }

    @PostMapping("/replan/apply")
    @Operation(summary = "应用当前范围重排")
    @PreAuthorize("@ss.hasPermission('mes:pro-auto-schedule:replan')")
    public CommonResult<MesProAutoScheduleApplyRespVO> replanApply(
            @Valid @RequestBody MesProAutoScheduleReplanReqVO reqVO) {
        validateScope(reqVO);
        return success(autoScheduleService.replanApply(reqVO));
    }

    @GetMapping("/apply/latest-success")
    @Operation(summary = "查询最近一次成功排产时间")
    @PreAuthorize("@ss.hasPermission('mes:pro-schedule-order:query')")
    public CommonResult<MesProLatestScheduleApplyRespVO> getLatestSuccessfulScheduleApply() {
        return success(autoScheduleService.getLatestSuccessfulScheduleApply());
    }

    @GetMapping("/replan/explanation/latest")
    @Operation(summary = "查询最近一次成功重排说明")
    @PreAuthorize("@ss.hasPermission('mes:pro-scheduler-workbench:query')")
    public CommonResult<MesProReplanExplanationRespVO> getLatestReplanExplanation() {
        return success(autoScheduleService.getLatestReplanExplanation());
    }

    @GetMapping("/issues")
    @Operation(summary = "查询当前排产问题")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-task:query', 'mes:pro-schedule-order:query')")
    public CommonResult<List<MesProAutoScheduleIssueRespVO>> getIssues(@Valid MesProAutoScheduleIssueQueryReqVO reqVO) {
        return success(autoScheduleService.getIssues(reqVO));
    }

    @PostMapping("/issues")
    @Operation(summary = "登记生产异常回流问题")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-task:update', 'mes:pro-schedule-order:update')")
    public CommonResult<Long> createIssue(@Valid @RequestBody MesProAutoScheduleIssueCreateReqVO reqVO) {
        return success(autoScheduleService.createIssue(reqVO));
    }

    @PostMapping("/issues/cancel-night-shift")
    @Operation(summary = "????????")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-task:update', 'mes:pro-schedule-order:update')")
    public CommonResult<Long> cancelNightShift(@Valid @RequestBody MesProAutoScheduleCancelNightShiftReqVO reqVO) {
        return success(autoScheduleService.cancelNightShift(reqVO));
    }

    @PutMapping("/issues/resolve")
    @Operation(summary = "关闭生产异常回流问题")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-task:update', 'mes:pro-schedule-order:update')")
    public CommonResult<Boolean> resolveIssue(@Valid @RequestBody MesProAutoScheduleIssueResolveReqVO reqVO) {
        autoScheduleService.resolveIssue(reqVO);
        return success(true);
    }

    @GetMapping("/dependencies")
    @Operation(summary = "查询当前甘特图依赖线")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-task:query', 'mes:pro-schedule-order:query')")
    public CommonResult<List<GanttLinkRespVO>> getDependencies(
            @Parameter(name = "workOrderIds", description = "工单编号列表")
            @RequestParam(value = "workOrderIds", required = false) List<Long> workOrderIds,
            @Parameter(name = "taskIds", description = "任务编号列表")
            @RequestParam(value = "taskIds", required = false) List<Long> taskIds) {
        return success(autoScheduleService.getDependencies(workOrderIds, taskIds));
    }

    @PostMapping("/dependencies")
    @Operation(summary = "查询当前甘特图依赖线")
    @PreAuthorize("@ss.hasAnyPermissions('mes:pro-task:query', 'mes:pro-schedule-order:query')")
    public CommonResult<List<GanttLinkRespVO>> getDependencies(
            @Valid @RequestBody MesProAutoScheduleDependencyReqVO reqVO) {
        return success(autoScheduleService.getDependencies(reqVO.getWorkOrderIds(), reqVO.getTaskIds()));
    }

}
