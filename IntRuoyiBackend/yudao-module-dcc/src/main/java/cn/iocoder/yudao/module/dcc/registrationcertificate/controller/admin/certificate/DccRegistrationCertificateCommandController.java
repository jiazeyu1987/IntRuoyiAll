package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command.DccRegistrationCertificateDraftReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command.DccRegistrationCertificateFormalizeReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.certificate.vo.command.DccRegistrationCertificateUpdateDraftReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证命令")
@RestController
@RequestMapping("/dcc/registration-certificates")
@Validated
public class DccRegistrationCertificateCommandController {

    private final DccRegistrationCertificateCommandService commandService;

    public DccRegistrationCertificateCommandController(DccRegistrationCertificateCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/drafts")
    @Operation(summary = "创建首证草稿")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:create')")
    public CommonResult<Long> createDraft(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateDraftReqVO reqVO) {
        return success(commandService.createDraft(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                TracerUtils.getTraceId(), reqVO.toDraftData()));
    }

    @PutMapping("/drafts/{id}")
    @Operation(summary = "更新首证草稿")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:update')")
    public CommonResult<Long> updateDraft(
            @PathVariable("id") @Positive Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateUpdateDraftReqVO reqVO) {
        return success(commandService.updateDraft(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                TracerUtils.getTraceId(), id, reqVO.getExpectedRowVersion(),
                reqVO.getExpectedSnapshotRevision(), reqVO.toDraftData()));
    }

    @DeleteMapping("/drafts/{id}")
    @Operation(summary = "删除首证草稿")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:delete-draft')")
    public CommonResult<Long> deleteDraft(
            @PathVariable("id") @Positive Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam("expectedRowVersion") @Positive Integer expectedRowVersion,
            @RequestParam("expectedSnapshotRevision") @Positive Integer expectedSnapshotRevision) {
        return success(commandService.deleteDraft(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                TracerUtils.getTraceId(), id, expectedRowVersion, expectedSnapshotRevision));
    }

    @PostMapping("/{id}/formalize")
    @Operation(summary = "正式化首证")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:formalize')")
    public CommonResult<Long> formalize(
            @PathVariable("id") @Positive Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DccRegistrationCertificateFormalizeReqVO reqVO) {
        return success(commandService.formalize(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), idempotencyKey,
                TracerUtils.getTraceId(), id, reqVO.getExpectedRowVersion(),
                reqVO.getExpectedSnapshotRevision(), reqVO.getBusinessFileId()));
    }
}
