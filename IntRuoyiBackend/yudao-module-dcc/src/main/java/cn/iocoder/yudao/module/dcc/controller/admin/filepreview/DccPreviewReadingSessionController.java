package cn.iocoder.yudao.module.dcc.controller.admin.filepreview;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo.*;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBodyReadContext;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.filepreview.DccOnlineFilePreviewReadingSessionService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** Authenticated viewer lifecycle. Token-bearing bodies must not be recorded in application logs. */
@RestController
@Validated
public class DccPreviewReadingSessionController {
    @Resource private DccOnlineFilePreviewReadingSessionService service;

    @ApiAccessLog(requestEnable = false)
    @PostMapping("/dcc/file-preview/files/{fileId}/reading-session/start")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query','mes:pro-edhr-batch-execution:query','mes:pro-edhr-batch-execution:update')")
    public CommonResult<DccPreviewReadingSessionRespVO> start(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String viewerToken,
            @Valid @RequestBody DccPreviewReadingSessionStartReqVO req, HttpServletRequest request) {
        return success(service.start(getLoginUserId(), fileId, req.withViewerToken(viewerToken), DccRequestAuditContext.from(request, null)));
    }
    @ApiAccessLog(requestEnable = false)
    @PostMapping("/dcc/file-preview/files/{fileId}/reading-session/heartbeat")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query','mes:pro-edhr-batch-execution:query','mes:pro-edhr-batch-execution:update')")
    public CommonResult<DccPreviewReadingSessionRespVO> heartbeat(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String viewerToken,
            @Valid @RequestBody DccPreviewReadingSessionHeartbeatReqVO req, HttpServletRequest request) {
        return success(service.heartbeat(getLoginUserId(), fileId, req.withViewerToken(viewerToken), DccRequestAuditContext.from(request, null)));
    }
    @ApiAccessLog(requestEnable = false)
    @PostMapping("/dcc/file-preview/files/{fileId}/reading-session/stop")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query','mes:pro-edhr-batch-execution:query','mes:pro-edhr-batch-execution:update')")
    public CommonResult<DccPreviewReadingSessionRespVO> stop(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String viewerToken,
            @Valid @RequestBody DccPreviewReadingSessionStopReqVO req, HttpServletRequest request) {
        return success(service.stop(getLoginUserId(), fileId, req.withViewerToken(viewerToken), DccRequestAuditContext.from(request, null)));
    }

    @ApiAccessLog(requestEnable = false)
    @PostMapping("/dcc/controlled-files/{fileId}/reading-session/start")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccPreviewReadingSessionRespVO> startControlled(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccControlledFileBodyReadContext.HEADER) String bodyReadContext,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String viewerToken,
            @Valid @RequestBody DccPreviewReadingSessionStartReqVO req, HttpServletRequest request) {
        return success(service.startControlled(getLoginUserId(), fileId,
                DccControlledFileBodyReadContext.fromWire(fileId, bodyReadContext), req.withViewerToken(viewerToken), DccRequestAuditContext.from(request, null)));
    }
    @ApiAccessLog(requestEnable = false)
    @PostMapping("/dcc/controlled-files/{fileId}/reading-session/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccPreviewReadingSessionRespVO> heartbeatControlled(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccControlledFileBodyReadContext.HEADER) String bodyReadContext,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String viewerToken,
            @Valid @RequestBody DccPreviewReadingSessionHeartbeatReqVO req, HttpServletRequest request) {
        return success(service.heartbeatControlled(getLoginUserId(), fileId,
                DccControlledFileBodyReadContext.fromWire(fileId, bodyReadContext), req.withViewerToken(viewerToken), DccRequestAuditContext.from(request, null)));
    }
    @ApiAccessLog(requestEnable = false)
    @PostMapping("/dcc/controlled-files/{fileId}/reading-session/stop")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccPreviewReadingSessionRespVO> stopControlled(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccControlledFileBodyReadContext.HEADER) String bodyReadContext,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String viewerToken,
            @Valid @RequestBody DccPreviewReadingSessionStopReqVO req, HttpServletRequest request) {
        return success(service.stopControlled(getLoginUserId(), fileId,
                DccControlledFileBodyReadContext.fromWire(fileId, bodyReadContext), req.withViewerToken(viewerToken), DccRequestAuditContext.from(request, null)));
    }

    @ApiAccessLog(requestEnable = false)
    @GetMapping("/dcc/file-preview/files/{fileId}/reading-summary")
    @PreAuthorize("@ss.hasAnyPermissions('dcc:controlled-file:query','mes:pro-edhr-batch-execution:query','mes:pro-edhr-batch-execution:update')")
    public CommonResult<DccPreviewReadingSummaryRespVO> summary(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String token,
            @RequestHeader(DccOnlineFilePreviewController.ACCESS_EVENT_CODE_HEADER) String event,
            @RequestHeader(DccOnlineFilePreviewController.WATERMARK_TRACE_CODE_HEADER) String trace,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_ID_HEADER) String tokenId,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_NONCE_HEADER) String nonce,
            HttpServletRequest request) {
        return success(service.summary(getLoginUserId(), fileId,
                new DccPreviewReadingEvidenceValue(token, event, trace, tokenId, nonce), DccRequestAuditContext.from(request, null)));
    }
    @ApiAccessLog(requestEnable = false)
    @GetMapping("/dcc/controlled-files/{fileId}/reading-summary")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccPreviewReadingSummaryRespVO> summaryControlled(@PathVariable("fileId") Long fileId,
            @RequestHeader(DccControlledFileBodyReadContext.HEADER) String context,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_HEADER) String token,
            @RequestHeader(DccOnlineFilePreviewController.ACCESS_EVENT_CODE_HEADER) String event,
            @RequestHeader(DccOnlineFilePreviewController.WATERMARK_TRACE_CODE_HEADER) String trace,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_ID_HEADER) String tokenId,
            @RequestHeader(DccOnlineFilePreviewController.VIEWER_TOKEN_NONCE_HEADER) String nonce,
            HttpServletRequest request) {
        return success(service.summaryControlled(getLoginUserId(), fileId,
                DccControlledFileBodyReadContext.fromWire(fileId, context),
                new DccPreviewReadingEvidenceValue(token, event, trace, tokenId, nonce), DccRequestAuditContext.from(request, null)));
    }

}
