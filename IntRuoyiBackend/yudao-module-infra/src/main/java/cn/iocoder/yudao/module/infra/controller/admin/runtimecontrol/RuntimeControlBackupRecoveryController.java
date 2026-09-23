package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupRecoveryRequests;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseWorkflowRespVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowBackupRecoveryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/infra/runtime-control/release-workflows/{workflowId}/backup-recovery")
public class RuntimeControlBackupRecoveryController {
    private final ReleaseWorkflowBackupRecoveryService recovery;
    public RuntimeControlBackupRecoveryController(ReleaseWorkflowBackupRecoveryService recovery) { this.recovery = recovery; }

    @PostMapping("/inspect")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:publish-backup')")
    public CommonResult<ReleaseWorkflowBackupRecoveryService.Preview> inspect(@PathVariable String workflowId,
            @Valid @RequestBody RuntimeControlBackupRecoveryRequests.Inspect request) {
        return success(ReleaseWorkflowApiErrors.call(() -> recovery.inspect(workflowId, request.expectedStateVersion(), actor())));
    }

    @PostMapping("/recover")
    @PreAuthorize("@ss.hasPermission('infra:runtime-control:publish-backup')")
    public CommonResult<RuntimeControlReleaseWorkflowRespVO> recover(@PathVariable String workflowId,
            @Valid @RequestBody RuntimeControlBackupRecoveryRequests.Recover request) {
        return success(RuntimeControlReleaseWorkflowRespVO.from(ReleaseWorkflowApiErrors.call(() -> recovery.recover(workflowId,
                request.expectedStateVersion(), request.previewId(), actor(), request.prodConfirmText()))));
    }

    private static String actor() {
        Long userId = getLoginUserId();
        if (userId == null) throw new IllegalStateException("RECOVERY_AUTHENTICATED_ACTOR_REQUIRED");
        return userId.toString();
    }
}
