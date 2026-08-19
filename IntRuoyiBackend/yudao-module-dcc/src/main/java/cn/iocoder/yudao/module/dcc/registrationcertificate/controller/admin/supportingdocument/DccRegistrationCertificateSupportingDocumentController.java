package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.supportingdocument;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.supportingdocument.vo.DccRegistrationCertificateSupportingDocumentReviewReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.supportingdocument.vo.DccRegistrationCertificateSupportingDocumentUploadReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.DccRegistrationCertificateSupportingDocumentCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.DccRegistrationCertificateSupportingDocumentResult;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.supportingdocument.DccRegistrationCertificateSupportingDocumentService;
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

@Tag(name = "管理后台 - 国内注册证支持文件")
@RestController
@RequestMapping("/dcc/registration-certificates/{certificateId}/supporting-documents")
@Validated
public class DccRegistrationCertificateSupportingDocumentController {

    private final DccRegistrationCertificateSupportingDocumentService supportingDocumentService;

    public DccRegistrationCertificateSupportingDocumentController(
            DccRegistrationCertificateSupportingDocumentService supportingDocumentService) {
        this.supportingDocumentService = supportingDocumentService;
    }

    @PostMapping
    @Operation(summary = "上传注册证支持文件")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:supporting-document:upload')")
    public CommonResult<DccRegistrationCertificateSupportingDocumentResult> upload(
            @PathVariable("certificateId") @Positive Long certificateId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateSupportingDocumentUploadReqVO reqVO) {
        return success(supportingDocumentService.upload(toCommand(
                certificateId, idempotencyKey, null, reqVO.getVersionId(), reqVO.getBusinessFileId(),
                null, reqVO.getDocumentType(), null)));
    }

    @PostMapping("/{supportingDocumentId}/confirm")
    @Operation(summary = "确认注册证支持文件")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:supporting-document:confirm')")
    public CommonResult<DccRegistrationCertificateSupportingDocumentResult> confirm(
            @PathVariable("certificateId") @Positive Long certificateId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable("supportingDocumentId") @Positive Long supportingDocumentId,
            @Valid @RequestBody DccRegistrationCertificateSupportingDocumentReviewReqVO reqVO) {
        return success(supportingDocumentService.confirm(toCommand(
                certificateId, idempotencyKey, supportingDocumentId, reqVO.getVersionId(),
                reqVO.getBusinessFileId(), reqVO.getExpectedRowVersion(),
                reqVO.getDocumentType(), null)));
    }

    @PostMapping("/{supportingDocumentId}/reject")
    @Operation(summary = "驳回注册证支持文件")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:supporting-document:confirm')")
    public CommonResult<DccRegistrationCertificateSupportingDocumentResult> reject(
            @PathVariable("certificateId") @Positive Long certificateId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable("supportingDocumentId") @Positive Long supportingDocumentId,
            @Valid @RequestBody DccRegistrationCertificateSupportingDocumentReviewReqVO reqVO) {
        return success(supportingDocumentService.reject(toCommand(
                certificateId, idempotencyKey, supportingDocumentId, reqVO.getVersionId(),
                reqVO.getBusinessFileId(), reqVO.getExpectedRowVersion(),
                reqVO.getDocumentType(), reqVO.getRejectReason())));
    }

    private DccRegistrationCertificateSupportingDocumentCommand toCommand(
            Long certificateId, String idempotencyKey, Long supportingDocumentId, Long versionId,
            Long businessFileId, Integer expectedRowVersion,
            String documentType, String rejectReason) {
        return new DccRegistrationCertificateSupportingDocumentCommand(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                TracerUtils.getTraceId(), certificateId, versionId, businessFileId,
                supportingDocumentId, expectedRowVersion, documentType, rejectReason);
    }
}
