package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesWorkflowInteractionLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/mes/pro/workflow-interaction-log")
@RequiredArgsConstructor
public class MesWorkflowInteractionLogController {
    private final MesWorkflowInteractionLog log;

    @Data
    public static class Request {
        @NotBlank @Size(max=32) private String scope;
        @NotBlank @Pattern(regexp="CANCELED|BLOCKED") private String outcome;
        @NotBlank @Size(max=80) private String control;
    }
    public record Entry(Long id, String actorName, Long actorUserId, String actionName,
                        String resultStatus, LocalDateTime occurredAt) { }

    @PostMapping
    @Operation(summary="记录本人页面取消或校验拦截")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Boolean> record(@Valid @RequestBody Request request) {
        log.recordClient(SecurityFrameworkUtils.getLoginUserId(), request.scope, request.outcome, request.control);
        return success(true);
    }

    @GetMapping("/own-recent")
    @Operation(summary="查看本人最近失败和取消日志")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<List<Entry>> ownRecent() {
        return success(log.ownRecent(SecurityFrameworkUtils.getLoginUserId()).stream().map(e ->
                new Entry(e.getId(), e.getActorUsername(), e.getActorUserId(), e.getActionName(),
                        e.getResultStatus(), e.getOccurredAt())).toList());
    }
}
