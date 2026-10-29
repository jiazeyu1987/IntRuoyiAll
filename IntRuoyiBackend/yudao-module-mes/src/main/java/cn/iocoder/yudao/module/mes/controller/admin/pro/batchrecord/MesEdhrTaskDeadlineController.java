package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrTaskDeadlineService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/edhr-work-task/route-deadline")
public class MesEdhrTaskDeadlineController {
    @Resource private MesEdhrTaskDeadlineService service;
    @GetMapping
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:query')")
    public CommonResult<List<MesProEdhrWorkTaskAssignmentRuleDO>> list(@RequestParam Long routeId) {
        return success(service.list(routeId));
    }
    @PostMapping
    @PreAuthorize("@ss.hasPermission('mes:pro-edhr-work-task-rule:update')")
    public CommonResult<Boolean> save(@Valid @RequestBody SaveRequest req) {
        service.save(req.routeId(), req.taskType(), req.dueMinutes(), req.enabled(), req.reason());
        return success(true);
    }
    public record SaveRequest(@NotNull Long routeId, @NotBlank String taskType,
                              @NotNull @Min(1) Integer dueMinutes, @NotNull Boolean enabled,
                              @NotBlank @Size(max=500) String reason) {}
}
