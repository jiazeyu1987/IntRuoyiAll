package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesReleaseTaskNotifyDeliveryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesReleaseTaskNotifyRetryReqVO;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationDispatchService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@Validated
@RequestMapping("/mes/pro/production-release-task-notification")
public class MesReleaseTaskNotificationController {
    @Resource private MesReleaseTaskNotificationService notificationService;
    @Resource private MesReleaseTaskNotificationDispatchService dispatchService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<List<MesReleaseTaskNotifyDeliveryRespVO>> list(@RequestParam @Positive Long workTaskId) {
        return success(notificationService.listForTask(TenantContextHolder.getRequiredTenantId(), getLoginUserId(), workTaskId)
                .stream().map(MesReleaseTaskNotifyDeliveryRespVO::from).toList());
    }

    @PostMapping("/retry")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<Boolean> retry(@Valid @RequestBody MesReleaseTaskNotifyRetryReqVO request) {
        dispatchService.retryDelivery(TenantContextHolder.getRequiredTenantId(), getLoginUserId(),
                request.getDeliveryId(), request.getExpectedVersion(), request.getReason());
        return success(true);
    }
}
