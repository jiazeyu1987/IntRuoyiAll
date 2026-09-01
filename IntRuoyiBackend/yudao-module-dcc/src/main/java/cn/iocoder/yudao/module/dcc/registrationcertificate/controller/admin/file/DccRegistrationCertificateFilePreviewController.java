package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDeliveryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFilePreviewService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDownloadResult;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController.ACCESS_EVENT_CODE_HEADER;
import static cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController.PREVIEW_WATERMARK_HEADER;
import static cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER;
import static cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController.VIEWER_TOKEN_ID_HEADER;
import static cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController.VIEWER_TOKEN_NONCE_HEADER;
import static cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController.WATERMARK_TRACE_CODE_HEADER;

@Tag(name = "管理后台 - 注册证文件预览")
@RestController
@RequestMapping("/dcc/registration-certificates/files")
@Validated
public class DccRegistrationCertificateFilePreviewController {

    public static final String DOWNLOAD_ATTEMPT_KEY_HEADER = "X-DCC-Download-Attempt-Key";

    private final DccRegistrationCertificateFilePreviewService previewService;
    private final DccRegistrationCertificateFileDeliveryService deliveryService;

    public DccRegistrationCertificateFilePreviewController(
            DccRegistrationCertificateFilePreviewService previewService,
            DccRegistrationCertificateFileDeliveryService deliveryService) {
        this.previewService = previewService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{businessFileId}/preview-metadata")
    @Operation(summary = "获取注册证文件预览信息")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<DccControlledFilePreviewMetadataRespVO> getPreviewMetadata(
            @PathVariable("businessFileId") Long businessFileId,
            HttpServletRequest request) {
        return success(previewService.getPreviewMetadata(TenantContextHolder.getRequiredTenantId(), getLoginUserId(),
                businessFileId, auditContext(request)));
    }

    @GetMapping("/{businessFileId}/preview")
    @Operation(summary = "预览注册证文件")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public ResponseEntity<byte[]> previewFile(@PathVariable("businessFileId") Long businessFileId,
                                              @RequestHeader(VIEWER_TOKEN_HEADER) String viewerToken,
                                              @RequestHeader(ACCESS_EVENT_CODE_HEADER) String accessEventCode,
                                              @RequestHeader(WATERMARK_TRACE_CODE_HEADER) String watermarkTraceCode,
                                              @RequestHeader(VIEWER_TOKEN_ID_HEADER) String viewerTokenId,
                                              @RequestHeader(VIEWER_TOKEN_NONCE_HEADER) String viewerTokenNonce,
                                              HttpServletRequest request) {
        var binary = previewService.readPreviewFile(TenantContextHolder.getRequiredTenantId(), getLoginUserId(),
                businessFileId, viewerToken, accessEventCode, watermarkTraceCode, viewerTokenId, viewerTokenNonce,
                auditContext(request));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(binary.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(binary.fileName()))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        PREVIEW_WATERMARK_HEADER + "," + ACCESS_EVENT_CODE_HEADER)
                .header(PREVIEW_WATERMARK_HEADER, encodePreviewWatermark(binary.watermark()))
                .header(ACCESS_EVENT_CODE_HEADER, accessEventCode)
                .body(binary.bytes());
    }

    @GetMapping("/{businessFileId}/download")
    @Operation(summary = "下载注册证文件")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:access-request:create')")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable("businessFileId") Long businessFileId,
            @RequestHeader(DOWNLOAD_ATTEMPT_KEY_HEADER) String attemptKey,
            HttpServletRequest request) {
        DccRegistrationCertificateFileDownloadResult binary = deliveryService.download(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), businessFileId, attemptKey,
                DccRequestAuditContext.from(request, attemptKey));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(binary.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(binary.fileName()))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(binary.bytes());
    }

    private DccRequestAuditContext auditContext(HttpServletRequest request) {
        String requestId = request.getHeader(DccRequestAuditContext.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("X-Request-Id");
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("trace-id");
        }
        return new DccRequestAuditContext(ServletUtils.getClientIP(request), ServletUtils.getUserAgent(request),
                requestId);
    }

    private String encodePreviewWatermark(DccControlledPreviewWatermarkRespVO watermark) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JsonUtils.toJsonString(watermark).getBytes(StandardCharsets.UTF_8));
    }

    private String contentDispositionInline(String fileName) {
        return ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private String contentDispositionAttachment(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
