package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.export.DccRegistrationCertificateExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/registration-certificates/excel-export")
public class DccRegistrationCertificateExportController {
    private final DccRegistrationCertificateExportService exports;
    public DccRegistrationCertificateExportController(DccRegistrationCertificateExportService exports) { this.exports = exports; }
    public record Generate(String snapshotId, String requestId) {}

    @PostMapping("/snapshot")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<DccRegistrationCertificateExportService.Preview> preview(@RequestBody DccRegistrationCertificateExportService.Scope scope) {
        return success(exports.preview(TenantContextHolder.getRequiredTenantId(), getLoginUserId(), scope));
    }

    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public void generate(@RequestBody Generate request, HttpServletResponse response) throws IOException {
        Long tenant = TenantContextHolder.getRequiredTenantId(), actor = getLoginUserId();
        exports.generate(tenant, actor, request.snapshotId(), request.requestId());
        write(tenant, actor, request.requestId(), response);
    }

    @GetMapping("/{requestId}/status")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccRegistrationCertificateExportService.Status> status(@PathVariable String requestId) {
        return success(exports.status(TenantContextHolder.getRequiredTenantId(), getLoginUserId(), requestId));
    }

    @GetMapping("/{requestId}/file")
    @PreAuthorize("isAuthenticated()")
    public void download(@PathVariable String requestId, HttpServletResponse response) throws IOException {
        write(TenantContextHolder.getRequiredTenantId(), getLoginUserId(), requestId, response);
    }

    private void write(Long tenant, Long actor, String requestId, HttpServletResponse response) throws IOException {
        try (var download = exports.open(tenant, actor, requestId)) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Content-Disposition", ContentDisposition.attachment().filename(download.status().fileName(), StandardCharsets.UTF_8).build().toString());
            response.setHeader("X-Export-Count", String.valueOf(download.status().exported()));
            response.setHeader("X-Export-Skipped", String.valueOf(download.status().skipped()));
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition,X-Export-Count,X-Export-Skipped");
            response.setContentLength(download.size());
            download.write(response.getOutputStream());
        }
    }
}
