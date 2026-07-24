package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestoreApplyReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestoreApplyRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestorePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestoreStatusRespVO;
import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionRestoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC NAS Permission Restore")
@RestController
@RequestMapping("/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore")
@Validated
public class DccNasPermissionRestoreController {

    @Resource
    private DccNasPermissionRestoreService restoreService;

    @GetMapping("/preview")
    @Operation(summary = "Preview NAS permission restore for one transfer task")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<DccNasPermissionRestorePreviewRespVO> previewPermissionRestore(
            @PathVariable("taskId") Long taskId) {
        return success(DccNasPermissionRestorePreviewRespVO.of(restoreService.preview(taskId)));
    }

    @PostMapping("")
    @Operation(summary = "Create one NAS permission restore task")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<DccNasPermissionRestoreApplyRespVO> applyPermissionRestore(
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody DccNasPermissionRestoreApplyReqVO reqVO) {
        DccNasPermissionRestoreService.ApplyRestoreCommand command =
                new DccNasPermissionRestoreService.ApplyRestoreCommand(
                        taskId,
                        reqVO.getIdempotencyKey(),
                        reqVO.getPlanHash(),
                        reqVO.getRestoreMode(),
                        reqVO.getChangeReason(),
                        getLoginUserId());
        return success(DccNasPermissionRestoreApplyRespVO.of(restoreService.apply(command)));
    }

    @GetMapping("/{restoreId}")
    @Operation(summary = "Get one NAS permission restore task status")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:access-rule:manage')")
    public CommonResult<DccNasPermissionRestoreStatusRespVO> getPermissionRestoreStatus(
            @PathVariable("taskId") Long taskId,
            @PathVariable("restoreId") Long restoreId) {
        return success(DccNasPermissionRestoreStatusRespVO.of(restoreService.getStatus(taskId, restoreId)));
    }
}
