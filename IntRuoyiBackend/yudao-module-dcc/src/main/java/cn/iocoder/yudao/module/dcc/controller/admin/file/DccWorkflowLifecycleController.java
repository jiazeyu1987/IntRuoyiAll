package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** Module-owned commands; the manager embeds these in the common pages. */
@RestController
@RequestMapping("/dcc/controlled-file/workflow-lifecycle")
public class DccWorkflowLifecycleController {
    private final DccControlledFileWorkflowService workflow;
    private final DccWorkflowDistributionService distribution;
    private final DccControlledFileLifecycleService lifecycle;
    private final DccControlledFileCategoryPermissionSupport categoryPermissions;
    private final PermissionApi permissions;
    @jakarta.annotation.Resource private DccWorkflowSignoffAssignmentService signoffAssignments;
    public DccWorkflowLifecycleController(DccControlledFileWorkflowService workflow,
            DccWorkflowDistributionService distribution,DccControlledFileLifecycleService lifecycle,
            DccControlledFileCategoryPermissionSupport categoryPermissions,PermissionApi permissions) {
        this.workflow=workflow;this.distribution=distribution;this.lifecycle=lifecycle;
        this.categoryPermissions=categoryPermissions;this.permissions=permissions;
    }
    @GetMapping("/{id}/signoff-assignment-context")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:review')")
    public CommonResult<DccSignoffAssignmentContext> assignmentContext(@PathVariable Long id,@RequestParam String taskId) {
        return success(signoffAssignments.assignmentContext(getLoginUserId(),id,taskId));
    }
    @PostMapping("/{id}/assign-signoff")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:review')")
    public CommonResult<Boolean> assign(@PathVariable Long id,@Valid @RequestBody DccSignoffAssignmentReqVO request) {
        workflow.assignSignoff(getLoginUserId(),id,request);return success(true);
    }
    @PostMapping("/{id}/distribute")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:distribute')")
    public CommonResult<Boolean> distribute(@PathVariable Long id,@Valid @RequestBody DccWorkflowDistributionReqVO request) {
        distribution.distribute(getLoginUserId(),id,request);return success(true);
    }
    @GetMapping("/{id}/distribution-recipient-options")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:distribute')")
    public CommonResult<List<DccWorkflowDistributionRecipientRespVO>> recipients(@PathVariable Long id,@RequestParam Long departmentId) {
        return success(distribution.recipientOptions(getLoginUserId(),id,departmentId));
    }
    @GetMapping("/pending-distribution")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:distribute')")
    public CommonResult<List<DccControlledFileRespVO>> pending(@RequestParam(defaultValue="false") boolean remindersOnly) {
        Long user=getLoginUserId();
        if(!permissions.hasAnyRoles(user,"doc_control")) throw new IllegalArgumentException("待下发列表仅文控角色可办理");
        var reminderDates=lifecycle.distributionReminderDates();
        return success(lifecycle.pendingDistribution(remindersOnly,reminderDates).stream()
                .filter(file -> categoryPermissions.hasCategoryPermission(file.getCategoryId(),user,
                        cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .map(file -> {
                    var response=cn.iocoder.yudao.framework.common.util.object.BeanUtils.toBean(file,DccControlledFileRespVO.class);
                    response.setDistributionReminderStage(reminderDates.stage(file.getEffectiveDate()));
                    return response;
                }).toList());
    }
}
