package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.grant;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo.DccRegistrationCertificateAccessReasonReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证访问授权")
@RestController
@RequestMapping("/dcc/registration-certificates/grants")
@Validated
public class DccRegistrationCertificateGrantController {

    private final DccRegistrationCertificateApprovalService approvalService;

    public DccRegistrationCertificateGrantController(DccRegistrationCertificateApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/{grantId}/revoke")
    @Operation(summary = "撤销注册证访问授权")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:access-request:approve')")
    public CommonResult<Boolean> revokeGrant(
            @PathVariable("grantId") @Positive Long grantId,
            @Valid @RequestBody DccRegistrationCertificateAccessReasonReqVO reqVO) {
        approvalService.revokeGrant(TenantContextHolder.getRequiredTenantId(), getLoginUserId(),
                grantId, reqVO.getReason());
        return success(true);
    }
}
