package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.file.vo.DccRegistrationCertificateFileDownloadGrantStatusRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDeliveryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFilePreviewService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateFileDownloadResult;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;

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

    private static final Logger log = LoggerFactory.getLogger(DccRegistrationCertificateFilePreviewController.class);

    public static final String DOWNLOAD_ATTEMPT_KEY_HEADER = "X-DCC-Download-Attempt-Key";

    private final DccRegistrationCertificateFilePreviewService previewService;
    private final DccRegistrationCertificateFileDeliveryService deliveryService;
    private final DccRegistrationCertificateAccessPolicyService accessPolicyService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateFilePreviewController(
            DccRegistrationCertificateFilePreviewService previewService,
            DccRegistrationCertificateFileDeliveryService deliveryService,
            DccRegistrationCertificateAccessPolicyService accessPolicyService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.previewService = previewService;
        this.deliveryService = deliveryService;
        this.accessPolicyService = accessPolicyService;
        this.businessClock = businessClock;
    }

    @GetMapping("/download-grants")
    @Operation(summary = "查询当前账号注册证文件下载授权")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:registration-certificate:query-current', "
            + "'dcc:registration-certificate:access-request:create', "
            + "'dcc:registration-certificate:access-request:approve')")
    public CommonResult<List<DccRegistrationCertificateFileDownloadGrantStatusRespVO>> listDownloadGrants(
            @RequestParam("businessFileIds") List<Long> businessFileIds) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = getLoginUserId();
        List<DccRegistrationCertificateFileDownloadGrantStatusRespVO> statuses = new LinkedHashSet<>(businessFileIds)
                .stream()
                .filter(id -> id != null && id > 0)
                .map(id -> DccRegistrationCertificateFileDownloadGrantStatusRespVO.of(id,
                        accessPolicyService.canDownloadFile(tenantId, userId, id, businessClock.now()),
                        accessPolicyService.findPendingDownloadRequestId(tenantId, userId, id)))
                .toList();
        return success(statuses);
    }

    @GetMapping("/{businessFileId}/preview-metadata")
    @Operation(summary = "获取注册证文件预览信息")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<DccControlledFilePreviewMetadataRespVO> getPreviewMetadata(
            @PathVariable("businessFileId") Long businessFileId,
            HttpServletRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = getLoginUserId();
        DccControlledFilePreviewMetadataRespVO metadata = previewService.getPreviewMetadata(
                tenantId, userId, businessFileId, auditContext(request));
        log.info("registration-certificate-preview-metadata tenantId={} userId={} businessFileId={} fileName={}",
                tenantId, userId, businessFileId, metadata.getFileName());
        return success(metadata);
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
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = getLoginUserId();
        var binary = previewService.readPreviewFile(tenantId, userId, businessFileId, viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, auditContext(request));
        log.info("registration-certificate-preview-file tenantId={} userId={} businessFileId={} fileName={}",
                tenantId, userId, businessFileId, binary.fileName());
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
    @PreAuthorize("@ss.hasAnyPermissions('dcc:registration-certificate:access-request:create', "
            + "'dcc:registration-certificate:access-request:approve')")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable("businessFileId") Long businessFileId,
            @RequestHeader(DOWNLOAD_ATTEMPT_KEY_HEADER) String attemptKey,
            HttpServletRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long userId = getLoginUserId();
        DccRegistrationCertificateFileDownloadResult binary = deliveryService.download(
                tenantId, userId, businessFileId, attemptKey, DccRequestAuditContext.from(request, attemptKey));
        log.info("registration-certificate-download-file tenantId={} userId={} businessFileId={} fileName={}",
                tenantId, userId, businessFileId, binary.fileName());
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
