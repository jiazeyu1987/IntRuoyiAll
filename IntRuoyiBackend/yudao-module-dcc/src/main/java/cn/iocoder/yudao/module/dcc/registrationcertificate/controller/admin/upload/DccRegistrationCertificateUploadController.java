package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadSubmitReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadSubmitResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证上传")
@RestController
@RequestMapping("/dcc/registration-certificates/uploads")
@Validated
public class DccRegistrationCertificateUploadController {

    private final DccRegistrationCertificateUploadService uploadService;
    private final DccRegistrationCertificateApprovalService approvalService;

    public DccRegistrationCertificateUploadController(
            DccRegistrationCertificateUploadService uploadService,
            DccRegistrationCertificateApprovalService approvalService) {
        this.uploadService = uploadService;
        this.approvalService = approvalService;
    }

    @PostMapping
    @Operation(summary = "提交注册证上传审批")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:upload:create')")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Long> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @ModelAttribute DccRegistrationCertificateUploadSubmitReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long actorId = getLoginUserId();
        DccRegistrationCertificateUploadSubmitResult result = uploadService.submitUploadForApproval(
                tenantId, actorId, idempotencyKey, TracerUtils.getTraceId(), reqVO.toCommand());
        approvalService.startNativeApproval(
                tenantId, actorId, new DccRegistrationCertificateApprovalStartCommand(result.requestId()));
        return success(result.requestId());
    }
}
