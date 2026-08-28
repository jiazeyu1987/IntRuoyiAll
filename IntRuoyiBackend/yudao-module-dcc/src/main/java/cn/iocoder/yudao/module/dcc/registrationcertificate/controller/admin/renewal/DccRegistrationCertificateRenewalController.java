package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.vo.DccRegistrationCertificateRenewalUploadReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.renewal.vo.DccRegistrationCertificateRenewalVoidReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.renewal.DccRegistrationCertificateRenewalSubmitCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证延续")
@RestController
@RequestMapping("/dcc/registration-certificates/{certificateId}/renewals")
@Validated
public class DccRegistrationCertificateRenewalController {

    private final DccRegistrationCertificateRenewalService renewalService;
    private final DccRegistrationCertificateApprovalService approvalService;

    public DccRegistrationCertificateRenewalController(
            DccRegistrationCertificateRenewalService renewalService,
            DccRegistrationCertificateApprovalService approvalService) {
        this.renewalService = renewalService;
        this.approvalService = approvalService;
    }

    @PostMapping
    @Operation(summary = "提交延续注册证审批")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:renewal:upload')")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Long> submitForApproval(
            @PathVariable("certificateId") @Positive Long certificateId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @ModelAttribute DccRegistrationCertificateRenewalUploadReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long actorId = getLoginUserId();
        Long requestId = renewalService.submitRenewalForApproval(new DccRegistrationCertificateRenewalSubmitCommand(
                tenantId, actorId, idempotencyKey, TracerUtils.getTraceId(), certificateId,
                reqVO.getExpectedRowVersion(), reqVO.getCurrentVersionId(),
                reqVO.getApprovalDate(), reqVO.getEffectiveDate(), reqVO.getExpiryDate(), reqVO.getFile()))
                .requestId();
        approvalService.startNativeApproval(
                tenantId, actorId, new DccRegistrationCertificateApprovalStartCommand(requestId));
        return success(requestId);
    }

    @PostMapping("/{pendingVersionId}/void")
    @Operation(summary = "作废待生效延续候选")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:renewal:void')")
    public CommonResult<DccRegistrationCertificateRenewalResult> voidPendingCandidate(
            @PathVariable("certificateId") @Positive Long certificateId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable("pendingVersionId") @Positive Long pendingVersionId,
            @Valid @RequestBody DccRegistrationCertificateRenewalVoidReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return success(renewalService.voidPendingCandidate(
                tenantId, getLoginUserId(), idempotencyKey, TracerUtils.getTraceId(), certificateId,
                reqVO.getExpectedRowVersion(), pendingVersionId, reqVO.getVoidReason()));
    }
}
