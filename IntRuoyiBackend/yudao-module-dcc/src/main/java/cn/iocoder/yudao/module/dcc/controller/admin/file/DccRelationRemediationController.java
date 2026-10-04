package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.RemediationTask;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationNotificationRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/dcc/relation-remediation")
@PreAuthorize("isAuthenticated()")
public class DccRelationRemediationController extends DccRelationsExceptionHandler {
    private final DccRelationRemediationService service;
    private final DccRelationNotificationRecoveryService recovery;
    public DccRelationRemediationController(DccRelationRemediationService service,DccRelationNotificationRecoveryService recovery){this.service=service;this.recovery=recovery;}
    @GetMapping("/my-tasks") public CommonResult<List<RemediationTask>> list(){
        return success(service.listAssignedTasks(SecurityFrameworkUtils.getLoginUserId()));
    }
    @GetMapping("/arrangements") public CommonResult<List<Arrangement>> arrangements(@RequestParam Long sourceFileId,@RequestParam String applicationRound){
        return success(service.listArrangements(SecurityFrameworkUtils.getLoginUserId(),sourceFileId,applicationRound));
    }
    @PostMapping("/events/{eventId}/retry-notifications") public CommonResult<Boolean> requestRetry(@PathVariable Long eventId,@Valid @RequestBody RetryRequest request){
        return success(recovery.requestRetry(SecurityFrameworkUtils.getLoginUserId(),eventId,request.reason()));
    }
    @GetMapping("/events/{eventId}/notifications") public CommonResult<List<DccRelationNotificationRecoveryService.NotificationStatus>> status(@PathVariable Long eventId){
        return success(recovery.listStatus(SecurityFrameworkUtils.getLoginUserId(),eventId));
    }
    public record RetryRequest(@NotBlank String reason){}
    // Arrangement writes intentionally go through A's signed countersign transaction, never a parallel unsigned endpoint.
}
