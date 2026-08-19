package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo.DccRegistrationCertificateAccessRequestSubmitReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accessrequest.DccRegistrationCertificateAccessRequestService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
    private final DccRegistrationCertificateApprovalService approvalService;

    public DccRegistrationCertificateAccessRequestController(
            DccRegistrationCertificateAccessRequestService accessRequestService,
            DccRegistrationCertificateApprovalService approvalService) {
        this.accessRequestService = accessRequestService;
        this.approvalService = approvalService;
    }

    @PostMapping
    @Operation(summary = "提交注册证访问申请")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:access-request:create')")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Long> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateAccessRequestSubmitReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long actorId = getLoginUserId();
        Long requestId = accessRequestService.submit(
                tenantId, actorId, idempotencyKey, reqVO.toCommand()).requestId();
        approvalService.startNativeApproval(
                tenantId, actorId, new DccRegistrationCertificateApprovalStartCommand(requestId));
        return success(requestId);
    }
}
