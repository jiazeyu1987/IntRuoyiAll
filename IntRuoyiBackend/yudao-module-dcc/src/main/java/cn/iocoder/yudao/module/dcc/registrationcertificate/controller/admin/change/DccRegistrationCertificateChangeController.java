package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change.vo.DccRegistrationCertificateChangeApplyReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.change.vo.DccRegistrationCertificateVoidReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.change.DccRegistrationCertificateChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证变更")
@RestController
@RequestMapping("/dcc/registration-certificates/{certificateId}/changes")
@Validated
public class DccRegistrationCertificateChangeController {

    private final DccRegistrationCertificateChangeService changeService;

    public DccRegistrationCertificateChangeController(DccRegistrationCertificateChangeService changeService) {
        this.changeService = changeService;
    }

    @PostMapping
    @Operation(summary = "提交注册证变更批件")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:change:submit')")
    public CommonResult<DccRegistrationCertificateChangeResult> applyChange(
            @PathVariable("certificateId") @Positive Long certificateId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateChangeApplyReqVO reqVO) {
        return success(changeService.applyChange(new DccRegistrationCertificateChangeCommand(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                TracerUtils.getTraceId(), certificateId, reqVO.getExpectedRowVersion(),
                reqVO.getApprovalDate(), reqVO.getStructuredValues(), reqVO.getOtherDescription(),
                reqVO.getEntrustedProduction(), reqVO.getSelfProduction(),
                reqVO.getEntrustedEnterprisesJson(), null, reqVO.getBusinessFileId())));
    }

    @PostMapping("/void")
    @Operation(summary = "作废注册证")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:void')")
    public CommonResult<DccRegistrationCertificateChangeResult> voidCertificate(
            @PathVariable("certificateId") @Positive Long certificateId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateVoidReqVO reqVO) {
        return success(changeService.voidCertificate(new DccRegistrationCertificateChangeCommand(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                TracerUtils.getTraceId(), certificateId, reqVO.getExpectedRowVersion(),
                reqVO.getApprovalDate(), null, null, null, null, null, reqVO.getVoidReason())));
    }
}
