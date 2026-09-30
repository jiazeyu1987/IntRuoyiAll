package cn.iocoder.yudao.module.infra.controller.admin.internuser;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.infra.controller.admin.internuser.vo.InternUserFileUploadTimeUpdateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.internuser.vo.InternUserTimeMaintenanceAuditRespVO;
import cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService;
import cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_FILE_UPLOAD_TIME;

@Tag(name = "管理后台 - 实习用户时间维护")
@RestController
@RequestMapping("/intern-user/time-maintenance")
@Validated
@ConditionalOnProperty(prefix = "yudao.intern-user", name = "enabled", havingValue = "true")
public class InternUserTimeMaintenanceController {

    @Resource
    private InternUserTimeMaintenanceService timeMaintenanceService;
    @Resource
    private InternUserTimeMaintenanceAuditService auditService;

    @PutMapping("/file/upload-time")
    @Operation(summary = "实习用户修改文件上传时间")
    @PreAuthorize("@ss.hasPermission('intern-user:time-maintenance:file-upload-time:update')")
    public CommonResult<Boolean> updateFileUploadTime(
            @Valid @RequestBody InternUserFileUploadTimeUpdateReqVO reqVO) {
        timeMaintenanceService.updateFileUploadTime(reqVO);
        return success(true);
    }

    @GetMapping("/file/upload-time/audits")
    @Operation(summary = "实习用户查看文件上传时间修改审计")
    @PreAuthorize("@ss.hasPermission('intern-user:time-maintenance:file-upload-time-audit:query')")
    public CommonResult<List<InternUserTimeMaintenanceAuditRespVO>> getFileUploadTimeAuditList(
            @RequestParam("fileId") Long fileId) {
        return success(BeanUtils.toBean(auditService.getAuditList(TARGET_FILE_UPLOAD_TIME, fileId),
                InternUserTimeMaintenanceAuditRespVO.class));
    }

}
