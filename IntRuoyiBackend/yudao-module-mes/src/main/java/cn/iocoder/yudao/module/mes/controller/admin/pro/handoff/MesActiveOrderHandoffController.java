package cn.iocoder.yudao.module.mes.controller.admin.pro.handoff;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.handoff.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
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
    @Resource private MesQaHandoffAssignmentService assignment;
    @Resource private MesPqcHandoffAssignmentService pqcAssignment;
    private static final String OWN="@ss.hasAnyPermissions('mes:pro-feedback:query','mes:pro-process-pool-team-leader:query','mes:pro-edhr-nonconformance-review:query','mes:pro-edhr-work-task:query')";
    @GetMapping("/my-list") @PreAuthorize(OWN)
    public CommonResult<List<MesActiveOrderHandoffTaskDO>> own() { return success(handoff.listOwnTasks(SecurityFrameworkUtils.getLoginUserId())); }
    @GetMapping("/navigation-context") @PreAuthorize(OWN)
    public CommonResult<MesActiveOrderHandoffService.NavigationContext> navigation(@RequestParam Long taskId) {return success(handoff.navigationContext(taskId,SecurityFrameworkUtils.getLoginUserId()));}
    @GetMapping("/receipts") @PreAuthorize(OWN)
    public CommonResult<List<MesActiveOrderHandoffDeliveryDO>> receipts(@RequestParam Long taskId) {return success(delivery.ownReceipts(taskId,SecurityFrameworkUtils.getLoginUserId()));}
    public record Retry(@NotNull Long id,@NotNull @Min(0) Integer rowVersion,@NotBlank @Size(max=1000) String reason) { }
    @PostMapping("/retry") @PreAuthorize(OWN)
    public CommonResult<Boolean> retry(@Valid @RequestBody Retry req) { delivery.retry(req.id(),req.rowVersion(),SecurityFrameworkUtils.getLoginUserId(),req.reason());return success(true); }
    @GetMapping("/qa-route-options") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<List<MesQaHandoffAssignmentService.Option>> routes() {return success(assignment.routeOptions());}
    @GetMapping("/qa-user-options") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<List<MesQaHandoffAssignmentService.Option>> users(@RequestParam String keyword) {return success(assignment.userOptions(keyword));}
    @GetMapping("/qa-role-options") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<List<MesQaHandoffAssignmentService.Option>> roles() {return success(assignment.roleOptions());}
    @GetMapping("/qa-assignment") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<MesQaHandoffAssignmentService.RuleView> getRule(@RequestParam Long routeId) {return success(assignment.view(routeId));}
    public record SaveRule(@NotNull Long routeId,@NotBlank String candidateSourceType,@NotNull Long candidateSourceId,
            @NotNull Boolean enabled,@NotBlank @Size(max=1000) String reason,Long expectedRuleId) { }
    @PostMapping("/qa-assignment") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')")
    public CommonResult<MesQaHandoffAssignmentService.RuleView> saveRule(@Valid @RequestBody SaveRule req) {
        assignment.save(req.routeId(),req.candidateSourceType(),req.candidateSourceId(),req.enabled(),req.reason(),req.expectedRuleId());
        return success(assignment.view(req.routeId()));
    }
    @GetMapping("/pqc-route-options") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<List<MesPqcHandoffAssignmentService.Option>> pqcRoutes() {return success(pqcAssignment.routeOptions());}
    @GetMapping("/pqc-user-options") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<List<MesPqcHandoffAssignmentService.Option>> pqcUsers(@RequestParam String keyword) {return success(pqcAssignment.userOptions(keyword));}
    @GetMapping("/pqc-role-options") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<List<MesPqcHandoffAssignmentService.Option>> pqcRoles() {return success(pqcAssignment.roleOptions());}
    @GetMapping("/pqc-assignment") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<MesPqcHandoffAssignmentService.RuleView> getPqcRule(@RequestParam Long routeId) {return success(pqcAssignment.view(routeId));}
    @PostMapping("/pqc-assignment") @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')")
    public CommonResult<MesPqcHandoffAssignmentService.RuleView> savePqcRule(@Valid @RequestBody SaveRule req) {
        pqcAssignment.save(req.routeId(),req.candidateSourceType(),req.candidateSourceId(),req.enabled(),req.reason(),req.expectedRuleId());
        return success(pqcAssignment.view(req.routeId()));
    }
}
