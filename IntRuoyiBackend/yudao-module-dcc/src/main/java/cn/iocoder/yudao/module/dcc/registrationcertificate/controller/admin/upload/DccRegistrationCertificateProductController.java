package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateProductMaintenanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@Validated
@RequestMapping("/dcc/registration-certificates")
public class DccRegistrationCertificateProductController {
    private final DccRegistrationCertificateProductMaintenanceService service;
    public DccRegistrationCertificateProductController(DccRegistrationCertificateProductMaintenanceService service) { this.service=service; }
    public record AddProducts(@NotNull @Positive Integer expectedRowVersion,
                              @NotEmpty List<@NotNull @Positive Long> projectCodeIds) { }

    @PostMapping("/{id}/products")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:upload:create') && @ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<Boolean> add(@PathVariable("id") @Positive Long id, @Valid @RequestBody AddProducts request) {
        service.addProducts(TenantContextHolder.getRequiredTenantId(),getLoginUserId(),id,request.expectedRowVersion(),request.projectCodeIds());
        return CommonResult.success(true);
    }
}
