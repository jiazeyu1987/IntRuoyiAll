package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditTaskRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccNasControlAuditReportFile;
import cn.iocoder.yudao.module.dcc.service.file.DccNasControlAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - DCC NAS Control Audit")
@RestController
@RequestMapping("/dcc/controlled-files/nas-control-audit")
@Validated
public class DccNasControlAuditController {

    @Resource
    private DccNasControlAuditService auditService;

    @PostMapping("/start")
    @Operation(summary = "Start NAS controlled-file audit")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccNasControlAuditTaskRespVO> startAudit() {
        return success(auditService.startTask(getLoginUserId()));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get NAS controlled-file audit task state")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public CommonResult<DccNasControlAuditTaskRespVO> getAuditTask(@PathVariable("taskId") Long taskId) {
        return success(auditService.getTask(taskId));
    }

    @GetMapping("/{taskId}/download")
    @Operation(summary = "Download NAS controlled-file audit report")
    @PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
    public ResponseEntity<byte[]> downloadAuditReport(@PathVariable("taskId") Long taskId) {
        DccNasControlAuditReportFile report = auditService.downloadReport(taskId);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(report.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(report.content());
    }
}
