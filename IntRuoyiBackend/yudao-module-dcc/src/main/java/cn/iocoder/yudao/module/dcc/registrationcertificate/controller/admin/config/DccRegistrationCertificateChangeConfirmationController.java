package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateChangeConfirmationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@Validated
@RequestMapping("/dcc/registration-certificates/change-confirmations")
public class DccRegistrationCertificateChangeConfirmationController {
    private final DccRegistrationCertificateChangeConfirmationService service;
    private final DccRegistrationCertificateQueryService queryService;
    public DccRegistrationCertificateChangeConfirmationController(
            DccRegistrationCertificateChangeConfirmationService service,DccRegistrationCertificateQueryService queryService) {
        this.service=service;this.queryService=queryService;
    }
    @GetMapping("/certificate/{id}")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<List<DccRegistrationCertificateChangeConfirmationService.Confirmation>> list(
            @PathVariable("id") @Positive Long certificateId,HttpServletRequest request) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        queryService.getDetail(tenant,getLoginUserId(),certificateId,null,DccRequestAuditContext.from(request,null));
        return success(service.list(tenant,certificateId));
    }
    @GetMapping("/message/{id}")
    public CommonResult<DccRegistrationCertificateChangeConfirmationService.Confirmation> get(
            @PathVariable("id") @Positive Long messageId) {
        return success(service.getForRecipient(TenantContextHolder.getRequiredTenantId(),getLoginUserId(),messageId));
    }
    @PostMapping("/message/{id}/confirm")
    public CommonResult<DccRegistrationCertificateChangeConfirmationService.Confirmation> confirm(
            @PathVariable("id") @Positive Long messageId) {
        return success(service.confirm(TenantContextHolder.getRequiredTenantId(),getLoginUserId(),messageId));
    }
}
