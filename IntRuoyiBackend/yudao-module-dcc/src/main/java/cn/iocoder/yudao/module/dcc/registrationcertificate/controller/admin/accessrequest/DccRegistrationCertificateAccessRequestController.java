package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo.DccRegistrationCertificateAccessRequestSubmitReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest.DccRegistrationCertificateAccessRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证访问申请")
@RestController
@RequestMapping("/dcc/registration-certificates/access-requests")
@Validated
public class DccRegistrationCertificateAccessRequestController {

    private final DccRegistrationCertificateAccessRequestService accessRequestService;

    public DccRegistrationCertificateAccessRequestController(
            DccRegistrationCertificateAccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    @PostMapping
    @Operation(summary = "提交注册证访问申请")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:access-request:create')")
    public CommonResult<Long> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateAccessRequestSubmitReqVO reqVO) {
        return success(accessRequestService.submit(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                reqVO.toCommand()).requestId());
    }
}
