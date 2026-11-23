package cn.iocoder.yudao.module.mes.controller.admin.pro.handoff;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.handoff.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/active-order-handoff")
@Validated
public class MesActiveOrderHandoffController {
    @Resource private MesActiveOrderHandoffService handoff;
    @Resource private MesActiveOrderHandoffDeliveryService delivery;
    private static final String OWN="@ss.hasAnyPermissions('mes:pro-feedback:query','mes:pro-process-pool-team-leader:query','mes:pro-edhr-nonconformance-review:query','mes:pro-edhr-work-task:query')";
    @lombok.Data
    @lombok.EqualsAndHashCode(callSuper = true)
    public static class OwnTask extends MesActiveOrderHandoffTaskDO {
        private boolean notificationDeliveryEnabled;
        private boolean notificationReceiptAvailable;
    }
    @GetMapping("/my-list") @PreAuthorize(OWN)
    public CommonResult<List<OwnTask>> own() {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        var rows = handoff.listOwnTasks(actor);
        var receiptTaskIds = delivery.ownReceiptTaskIds(rows.stream().map(MesActiveOrderHandoffTaskDO::getId).toList(), actor);
        return success(rows.stream().map(task -> {
            var row = BeanUtils.toBean(task, OwnTask.class);
            boolean enabled = delivery.notificationDeliveryEnabled(task.getTaskType());
            row.setNotificationDeliveryEnabled(enabled);
            row.setNotificationReceiptAvailable(enabled || receiptTaskIds.contains(task.getId()));
            return row;
        }).toList());
    }
    @GetMapping("/navigation-context") @PreAuthorize(OWN)
    public CommonResult<MesActiveOrderHandoffService.NavigationContext> navigation(@RequestParam Long taskId) {return success(handoff.navigationContext(taskId,SecurityFrameworkUtils.getLoginUserId()));}
    @GetMapping("/receipts") @PreAuthorize(OWN)
    public CommonResult<List<MesActiveOrderHandoffDeliveryDO>> receipts(@RequestParam Long taskId) {return success(delivery.ownReceipts(taskId,SecurityFrameworkUtils.getLoginUserId()));}
    public record Retry(@NotNull Long id,@NotNull @Min(0) Integer rowVersion,@NotBlank @Size(max=1000) String reason) { }
    @PostMapping("/retry") @PreAuthorize(OWN)
    public CommonResult<Boolean> retry(@Valid @RequestBody Retry req) { delivery.retry(req.id(),req.rowVersion(),SecurityFrameworkUtils.getLoginUserId(),req.reason());return success(true); }
}
