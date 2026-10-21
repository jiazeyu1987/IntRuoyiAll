package cn.iocoder.yudao.module.bpm.controller.admin.task;

import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.service.task.BpmSupervisorHandoverService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/bpm/task/supervisor-handover")
@Validated
public class BpmSupervisorHandoverController {
    @Resource private BpmSupervisorHandoverService service;

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('bpm:task:manager-query')")
    public CommonResult<PageResult<BpmSupervisorHandoverRespVO>> page(
            @Valid PageParam page, @RequestParam(required = false) @Positive Long assigneeUserId) {
        return success(service.getPage(page, assigneeUserId));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermission('bpm:task:manager-query') && @ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> handover(@Valid @RequestBody BpmSupervisorHandoverReqVO request) {
        service.handover(getLoginUserId(), request);
        return success(true);
    }
}
