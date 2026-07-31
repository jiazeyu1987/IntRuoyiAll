package cn.iocoder.yudao.module.infra.controller.admin.backupplan;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanHistoryPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanScheduleSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.service.backupplan.BackupPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Tag(name = "管理后台 - 备份计划")
@RestController
@RequestMapping("/infra/backup-plan")
public class BackupPlanController {

    @Resource
    private BackupPlanService backupPlanService;

    @GetMapping("/status")
    @Operation(summary = "获得备份计划状态")
    @PreAuthorize("@ss.hasPermission('system:backup-plan:query')")
    public CommonResult<BackupPlanStatusRespVO> getStatus() {
        return success(backupPlanService.getStatus());
    }

    @PutMapping("/schedule")
    @Operation(summary = "保存备份计划")
    @PreAuthorize("@ss.hasPermission('system:backup-plan:update')")
    public CommonResult<BackupPlanStatusRespVO> saveSchedule(@Valid @RequestBody BackupPlanScheduleSaveReqVO reqVO) {
        return success(backupPlanService.saveSchedule(reqVO));
    }

    @PostMapping("/enable")
    @Operation(summary = "开启自动备份")
    @PreAuthorize("@ss.hasPermission('system:backup-plan:update')")
    public CommonResult<BackupPlanStatusRespVO> enable() {
        return success(backupPlanService.enable());
    }

    @PostMapping("/disable")
    @Operation(summary = "关闭自动备份")
    @PreAuthorize("@ss.hasPermission('system:backup-plan:update')")
    public CommonResult<BackupPlanStatusRespVO> disable() {
        return success(backupPlanService.disable());
    }

    @PostMapping("/backup-now")
    @Operation(summary = "立即备份一次")
    @PreAuthorize("@ss.hasPermission('system:backup-plan:execute')")
    public CommonResult<RuntimeControlOperationRespVO> backupNow() {
        return success(backupPlanService.backupNow(requireLoginUserId()));
    }

    @GetMapping("/history/page")
    @Operation(summary = "获得备份包历史分页")
    @PreAuthorize("@ss.hasPermission('system:backup-plan:query')")
    public CommonResult<PageResult<RuntimeControlBackupPointRespVO>> getHistoryPage(
            @Valid BackupPlanHistoryPageReqVO pageReqVO) {
        return success(backupPlanService.getHistoryPage(pageReqVO));
    }

    private Long requireLoginUserId() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "loginUserId");
        }
        return loginUserId;
    }
}
