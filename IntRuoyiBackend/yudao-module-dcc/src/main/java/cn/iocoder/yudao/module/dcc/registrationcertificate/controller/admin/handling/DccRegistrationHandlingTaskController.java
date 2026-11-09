package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.handling;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.handling.DccRegistrationHandlingTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/registration-certificates/handling-tasks")
@Validated
public class DccRegistrationHandlingTaskController {
    private final DccRegistrationHandlingTaskService service;
    public DccRegistrationHandlingTaskController(DccRegistrationHandlingTaskService service){this.service=service;}
    public record Settings(@NotNull @Positive Long responsibleUserId,@Min(1) @Max(3650) int projectUploadDays,@Min(1) @Max(3650) int renewalDays) {}
    public record Delegation(@NotNull @Positive Long assignedUserId,@NotNull LocalDateTime dueAt,@NotBlank @Size(max=1024) String reason) {}
    @GetMapping("/config") @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:config:query')")
    public CommonResult<DccRegistrationHandlingTaskService.Config> settings(){return success(service.getConfig());}
    @PutMapping("/config") @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:config:update')")
    public CommonResult<DccRegistrationHandlingTaskService.Config> settings(@Valid @RequestBody Settings settings){return success(service.configure(getLoginUserId(),settings.responsibleUserId(),settings.projectUploadDays(),settings.renewalDays()));}
    @GetMapping @PreAuthorize("isAuthenticated()")
    public CommonResult<List<DccRegistrationHandlingTaskService.Task>> mine(){return success(service.list(getLoginUserId()));}
    @GetMapping("/{id:[0-9]+}") @PreAuthorize("isAuthenticated()")
    public CommonResult<Map<String,Object>> detail(@PathVariable @Positive Long id){return success(service.detail(getLoginUserId(),id));}
    @GetMapping("/candidates") @PreAuthorize("isAuthenticated()")
    public CommonResult<List<DccRegistrationHandlingTaskService.Candidate>> candidates(@RequestParam(required=false) Long taskId,@RequestParam String keyword){return success(service.candidates(getLoginUserId(),taskId,keyword));}
    @PostMapping("/{id:[0-9]+}/delegate") @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> delegate(@PathVariable @Positive Long id,@Valid @RequestBody Delegation command){service.delegate(getLoginUserId(),id,command.assignedUserId(),command.dueAt(),command.reason());return success(true);}
}
