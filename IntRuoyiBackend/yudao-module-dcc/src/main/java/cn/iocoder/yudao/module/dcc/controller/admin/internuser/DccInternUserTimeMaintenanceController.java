package cn.iocoder.yudao.module.dcc.controller.admin.internuser;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.internuser.vo.DccInternUserTimeUpdateReqVO;
import cn.iocoder.yudao.module.dcc.service.internuser.DccInternUserTimeMaintenanceService;
import cn.iocoder.yudao.module.infra.controller.admin.internuser.vo.InternUserTimeMaintenanceAuditRespVO;
import cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_DCC_OBSOLETED_TIME;
import static cn.iocoder.yudao.module.infra.service.internuser.InternUserTimeMaintenanceAuditService.TARGET_DCC_PUBLISHED_TIME;

@Tag(name = "管理后台 - 实习用户 DCC 时间维护")
@RestController
@RequestMapping("/intern-user/time-maintenance/dcc")
@Validated
@ConditionalOnProperty(prefix = "yudao.intern-user", name = "enabled", havingValue = "true")
public class DccInternUserTimeMaintenanceController {

    @Resource
    private DccInternUserTimeMaintenanceService timeMaintenanceService;
    @Resource
    private InternUserTimeMaintenanceAuditService auditService;

    @PutMapping("/published-time")
    @Operation(summary = "实习用户修改 DCC 升版时间")
    @PreAuthorize("@ss.hasPermission('intern-user:time-maintenance:dcc-published-time:update')")
    public CommonResult<Boolean> updatePublishedTime(@Valid @RequestBody DccInternUserTimeUpdateReqVO reqVO) {
        timeMaintenanceService.updatePublishedTime(reqVO);
        return success(true);
    }

    @GetMapping("/published-time/audits")
    @Operation(summary = "实习用户查看 DCC 升版时间修改审计")
    @PreAuthorize("@ss.hasPermission('intern-user:time-maintenance:dcc-published-time-audit:query')")
    public CommonResult<List<InternUserTimeMaintenanceAuditRespVO>> getPublishedTimeAuditList(
            @RequestParam("controlledFileId") Long controlledFileId) {
        return success(BeanUtils.toBean(auditService.getAuditList(TARGET_DCC_PUBLISHED_TIME, controlledFileId),
                InternUserTimeMaintenanceAuditRespVO.class));
    }

    @PutMapping("/obsoleted-time")
    @Operation(summary = "实习用户修改 DCC 作废时间")
    @PreAuthorize("@ss.hasPermission('intern-user:time-maintenance:dcc-obsoleted-time:update')")
    public CommonResult<Boolean> updateObsoletedTime(@Valid @RequestBody DccInternUserTimeUpdateReqVO reqVO) {
        timeMaintenanceService.updateObsoletedTime(reqVO);
        return success(true);
    }

    @GetMapping("/obsoleted-time/audits")
    @Operation(summary = "实习用户查看 DCC 作废时间修改审计")
    @PreAuthorize("@ss.hasPermission('intern-user:time-maintenance:dcc-obsoleted-time-audit:query')")
    public CommonResult<List<InternUserTimeMaintenanceAuditRespVO>> getObsoletedTimeAuditList(
            @RequestParam("controlledFileId") Long controlledFileId) {
        return success(BeanUtils.toBean(auditService.getAuditList(TARGET_DCC_OBSOLETED_TIME, controlledFileId),
                InternUserTimeMaintenanceAuditRespVO.class));
    }

}
