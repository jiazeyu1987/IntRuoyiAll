package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.approval.service.ownership.ApprovalHistoryBackfillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

/** Deployment must explicitly enable this manual maintenance entry; it is never a startup job. */
@RestController
@Validated
@RequestMapping("/approval-center/history-backfill")
@ConditionalOnProperty(name="yudao.bpm.approval-history-backfill.enabled",havingValue="true")
public class ApprovalHistoryBackfillController {
    private final ApprovalHistoryBackfillService service;
    public ApprovalHistoryBackfillController(ApprovalHistoryBackfillService service) {this.service=service;}
    public record ExecuteRequest(
            @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_-]{0,47}") String batchId,
            @NotBlank @Pattern(regexp="[a-f0-9]{64}") String inspectionDigest,
            @NotBlank @Pattern(regexp="EXECUTE_HISTORY_BACKFILL") String confirmation) { }
    @GetMapping("/inspect")
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<ApprovalHistoryBackfillService.Report> inspect(
            @RequestParam @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9_-]{0,47}") String batchId) {
        return success(service.inspect(getLoginUserId(),batchId));
    }
    @PostMapping("/execute")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<ApprovalHistoryBackfillService.Report> execute(@Valid @RequestBody ExecuteRequest request) {
        return success(service.execute(getLoginUserId(),request.batchId(),request.inspectionDigest()));
    }
}
