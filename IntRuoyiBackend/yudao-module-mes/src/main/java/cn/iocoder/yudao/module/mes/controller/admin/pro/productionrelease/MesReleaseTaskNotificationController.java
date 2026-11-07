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
                .stream().map(row -> { var value=MesReleaseTaskNotifyDeliveryRespVO.from(row); value.setRetryAllowed(notificationService.canRetry(getLoginUserId(),row)); return value; }).toList());
    }

    @PostMapping("/retry")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<Boolean> retry(@Valid @RequestBody MesReleaseTaskNotifyRetryReqVO request) {
        dispatchService.retryDelivery(TenantContextHolder.getRequiredTenantId(), getLoginUserId(),
                request.getDeliveryId(), request.getExpectedVersion(), request.getReason());
        return success(true);
    }

    @GetMapping("/my-pending-outcomes")
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task:query')")
    public CommonResult<List<MesReleaseOutcomeRecoveryRespVO>> myPendingOutcomes() {
        return success(notificationService.listOwnPendingOutcomes(TenantContextHolder.getRequiredTenantId(), getLoginUserId())
                .stream().map(MesReleaseOutcomeRecoveryRespVO::from).toList());
    }

    public record MesReleaseOutcomeRecoveryRespVO(String id, String workTaskId, String workOrderCode,
                                                  String batchCode, String eventType) {
        static MesReleaseOutcomeRecoveryRespVO from(
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.productionrelease.MesReleaseTaskNotifyDeliveryDO row) {
            var params = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(row.getTemplateParamsJson());
            if (params == null || !params.path("workOrderCode").isTextual() || !params.path("batchCode").isTextual())
                throw new IllegalStateException("放行结果通知缺少原订单或批号");
            String order = params.get("workOrderCode").textValue();
            String batch = params.get("batchCode").textValue();
            if (order.isBlank() || batch.isBlank()) throw new IllegalStateException("放行结果通知缺少原订单或批号");
            return new MesReleaseOutcomeRecoveryRespVO(row.getId().toString(), row.getWorkTaskId().toString(),
                    order, batch, row.getEventType());
        }
    }
}
