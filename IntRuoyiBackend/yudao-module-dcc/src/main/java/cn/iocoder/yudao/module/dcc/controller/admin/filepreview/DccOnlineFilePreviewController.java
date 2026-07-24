package cn.iocoder.yudao.module.dcc.controller.admin.filepreview;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 统一在线文件预览")
@RestController
@RequestMapping("/dcc/file-preview")
@Validated
public class DccOnlineFilePreviewController {

    public static final String PREVIEW_WATERMARK_HEADER = "X-DCC-Preview-Watermark";
    public static final String ACCESS_EVENT_CODE_HEADER = "X-DCC-Access-Event-Code";
    public static final String VIEWER_TOKEN_HEADER = "X-DCC-Viewer-Token";
    public static final String VIEWER_TOKEN_ID_HEADER = "X-DCC-Viewer-Token-Id";
    public static final String VIEWER_TOKEN_NONCE_HEADER = "X-DCC-Viewer-Token-Nonce";
    public static final String WATERMARK_TRACE_CODE_HEADER = "X-DCC-Watermark-Trace-Code";

    @Resource
    private DccOnlineFilePreviewService previewService;

    @GetMapping("/files/{fileId}/preview-metadata")
    @Operation(summary = "Get unified online file preview metadata")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query', "
            + "'mes:pro-edhr-batch-execution:query', 'mes:pro-edhr-batch-execution:update')")
    public CommonResult<DccControlledFilePreviewMetadataRespVO> getPreviewMetadata(
            @PathVariable("fileId") Long fileId,
            HttpServletRequest request) {
        return success(previewService.getPreviewMetadata(getLoginUserId(), fileId,
                auditContext(request, null)));
    }

    @GetMapping("/files/{fileId}/preview")
    @Operation(summary = "Preview one unified online file")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query', "
            + "'mes:pro-edhr-batch-execution:query', 'mes:pro-edhr-batch-execution:update')")
    public ResponseEntity<byte[]> previewFile(@PathVariable("fileId") Long fileId,
                                              @RequestHeader(VIEWER_TOKEN_HEADER) String viewerToken,
                                              @RequestHeader(ACCESS_EVENT_CODE_HEADER) String accessEventCode,
                                              @RequestHeader(WATERMARK_TRACE_CODE_HEADER) String watermarkTraceCode,
                                              @RequestHeader(VIEWER_TOKEN_ID_HEADER) String viewerTokenId,
                                              @RequestHeader(VIEWER_TOKEN_NONCE_HEADER) String viewerTokenNonce,
                                              HttpServletRequest request) {
        var binary = previewService.readPreviewFile(getLoginUserId(), fileId, viewerToken, accessEventCode,
                watermarkTraceCode, viewerTokenId, viewerTokenNonce, auditContext(request, accessEventCode));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(binary.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(binary.fileName()))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        PREVIEW_WATERMARK_HEADER + "," + ACCESS_EVENT_CODE_HEADER)
                .header(PREVIEW_WATERMARK_HEADER, encodePreviewWatermark(binary.watermark()))
                .header(ACCESS_EVENT_CODE_HEADER, accessEventCode)
                .body(binary.bytes());
    }

    @GetMapping("/files/{fileId}/onlyoffice-file")
    @TenantIgnore
    @PermitAll
    @Operation(summary = "Read one unified online file for OnlyOffice")
    public ResponseEntity<byte[]> getOnlyOfficePreviewFile(@PathVariable("fileId") Long fileId,
                                                           @RequestParam("token") String token,
                                                           HttpServletRequest request) throws Exception {
        var binary = previewService.readOnlyOfficePreviewFile(fileId, token, auditContext(request, null));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(binary.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionInline(binary.fileName()))
                .body(binary.bytes());
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

    private DccRequestAuditContext auditContext(HttpServletRequest request, String explicitRequestId) {
        return DccRequestAuditContext.from(request, explicitRequestId);
    }
}
