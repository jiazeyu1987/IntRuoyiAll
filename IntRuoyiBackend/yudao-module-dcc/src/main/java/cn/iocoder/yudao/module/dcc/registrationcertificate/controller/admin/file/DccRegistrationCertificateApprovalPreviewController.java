package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.file.DccRegistrationCertificateApprovalPreviewService;
import cn.iocoder.yudao.module.dcc.service.file.*;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.dcc.controller.admin.filepreview.DccOnlineFilePreviewController.*;

@RestController
@RequestMapping("/dcc/registration-certificates/approval-materials/{requestId}/{processInstanceId}/{businessFileId}")
public class DccRegistrationCertificateApprovalPreviewController {
    private final DccRegistrationCertificateApprovalPreviewService service;
    public DccRegistrationCertificateApprovalPreviewController(DccRegistrationCertificateApprovalPreviewService service){this.service=service;}
    @GetMapping("/preview-metadata")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<DccControlledFilePreviewMetadataRespVO> metadata(@PathVariable Long requestId,@PathVariable String processInstanceId,@PathVariable Long businessFileId,HttpServletRequest request){
        return success(service.getPreviewMetadata(TenantContextHolder.getRequiredTenantId(),getLoginUserId(),requestId,processInstanceId,businessFileId,DccRequestAuditContext.from(request,null)));
    }
    @GetMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> preview(@PathVariable Long requestId,@PathVariable String processInstanceId,@PathVariable Long businessFileId,@RequestHeader(VIEWER_TOKEN_HEADER) String token,@RequestHeader(ACCESS_EVENT_CODE_HEADER) String event,@RequestHeader(WATERMARK_TRACE_CODE_HEADER) String trace,@RequestHeader(VIEWER_TOKEN_ID_HEADER) String tokenId,@RequestHeader(VIEWER_TOKEN_NONCE_HEADER) String nonce,HttpServletRequest request){
        var binary=service.readPreview(TenantContextHolder.getRequiredTenantId(),getLoginUserId(),requestId,processInstanceId,businessFileId,token,event,trace,tokenId,nonce,DccRequestAuditContext.from(request,null));
        return response(binary).header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,PREVIEW_WATERMARK_HEADER+","+ACCESS_EVENT_CODE_HEADER)
                .header(PREVIEW_WATERMARK_HEADER,Base64.getUrlEncoder().withoutPadding().encodeToString(JsonUtils.toJsonString(binary.watermark()).getBytes(StandardCharsets.UTF_8)))
                .header(ACCESS_EVENT_CODE_HEADER,event).body(binary.bytes());
    }
    @GetMapping("/onlyoffice-file")
    @TenantIgnore
    @PermitAll
    public ResponseEntity<byte[]> office(@PathVariable Long requestId,@PathVariable String processInstanceId,@PathVariable Long businessFileId,@RequestParam Long infraFileId,@RequestParam String token){
        var binary=service.readOnlyOffice(requestId,processInstanceId,businessFileId,infraFileId,token);return response(binary).body(binary.bytes());
    }
    private ResponseEntity.BodyBuilder response(DccControlledFileBinary binary){return ResponseEntity.ok().contentType(MediaType.parseMediaType(binary.contentType()))
            .header(HttpHeaders.CACHE_CONTROL,"private, no-store").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.inline().filename(binary.fileName(),StandardCharsets.UTF_8).build().toString());}
}
