package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.supportingdocument;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@RequestMapping("/dcc/registration-certificates/{certificateId}/supporting-documents")
public class DccRegistrationCertificateSupportingDocumentController {
    private final DccRegistrationCertificateSupportingDocumentService service;
    public DccRegistrationCertificateSupportingDocumentController(DccRegistrationCertificateSupportingDocumentService service) {
        this.service = service;
    }
    @PostMapping
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:renewal:upload')")
    public CommonResult<DccRegistrationCertificateSupportingDocumentResult> upload(
            @PathVariable("certificateId") Long certificateId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestParam("versionId") Long versionId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) throws java.io.IOException {
        return CommonResult.success(service.uploadFile(TenantContextHolder.getRequiredTenantId(), getLoginUserId(),
                key, certificateId, versionId, documentType, file));
    }
}
