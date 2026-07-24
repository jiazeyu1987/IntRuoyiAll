package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalCenterService;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalProviderDescriptor;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 统一审批中心")
@RestController
@RequestMapping("/approval-center")
@Validated
public class ApprovalCenterController {

    private final ApprovalCenterService approvalCenterService;

    public ApprovalCenterController(ApprovalCenterService approvalCenterService) {
        this.approvalCenterService = approvalCenterService;
    }

    @GetMapping("/modules")
    @Operation(summary = "获取统一审批中心已接入模块")
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<List<ApprovalProviderDescriptor>> getModules() {
        return success(approvalCenterService.listProviders(getLoginUserId()));
    }

    @GetMapping("/tasks/page")
    @Operation(summary = "获取统一审批中心任务分页")
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<PageResult<ApprovalTaskSummary>> getTaskPage(@Valid ApprovalTaskPageReqVO reqVO) {
        return success(approvalCenterService.getTaskPage(getLoginUserId(), reqVO.toQuery()));
    }

    @GetMapping("/tasks/timeline")
    @Operation(summary = "获取统一审批中心任务轨迹")
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<List<ApprovalTaskTimelineEntry>> getTaskTimeline(@Valid ApprovalTaskTimelineReqVO reqVO) {
        return success(approvalCenterService.listTaskTimeline(getLoginUserId(), reqVO.toQuery()));
    }

    @PostMapping("/tasks/review")
    @Operation(summary = "统一审批中心提交审核")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> reviewTask(@Valid @RequestBody ApprovalTaskReviewReqVO reqVO) {
        approvalCenterService.reviewTask(getLoginUserId(), reqVO.toCommand());
        return success(true);
    }
}
