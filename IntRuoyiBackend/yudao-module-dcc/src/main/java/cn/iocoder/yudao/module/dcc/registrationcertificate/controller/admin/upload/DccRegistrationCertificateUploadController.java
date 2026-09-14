package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodeRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadCompanyRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadEntrustedEnterpriseRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.upload.vo.DccRegistrationCertificateUploadSubmitReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalStartCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.upload.DccRegistrationCertificateUploadSubmitResult;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证上传")
@RestController
@RequestMapping("/dcc/registration-certificates/uploads")
@Validated
public class DccRegistrationCertificateUploadController {

    private final DccRegistrationCertificateUploadService uploadService;
    private final DccRegistrationCertificateApprovalService approvalService;
    private final DccProjectCodeService projectCodeService;

    public DccRegistrationCertificateUploadController(
            DccRegistrationCertificateUploadService uploadService,
            DccRegistrationCertificateApprovalService approvalService,
            DccProjectCodeService projectCodeService) {
        this.uploadService = uploadService;
        this.approvalService = approvalService;
        this.projectCodeService = projectCodeService;
    }

    @GetMapping("/entrusted-enterprises")
    @Operation(summary = "获得注册证上传受托企业候选")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:upload:create')")
    public CommonResult<List<DccRegistrationCertificateUploadEntrustedEnterpriseRespVO>> listEntrustedEnterprises(
            @RequestParam(value = "keyword", required = false) String keyword) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        return success(uploadService.listEntrustedEnterprises(tenantId, keyword).stream()
                .map(DccRegistrationCertificateUploadEntrustedEnterpriseRespVO::from)
                .toList());
    }

    @GetMapping("/owner-companies")
    @Operation(summary = "获得注册证上传公司候选")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:upload:create')")
    public CommonResult<List<DccRegistrationCertificateUploadCompanyRespVO>> listOwnerCompanies(
            @RequestParam(value = "keyword", required = false) String keyword) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long actorId = getLoginUserId();
        return success(uploadService.listOwnerCompanies(tenantId, actorId, keyword).stream()
                .map(DccRegistrationCertificateUploadCompanyRespVO::from)
                .toList());
    }

    @GetMapping("/project-codes")
    @Operation(summary = "获得注册证上传实际项目代码候选")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:upload:create')")
    public CommonResult<List<DccProjectCodeRespVO>> listProjectCodes(
            @RequestParam(value = "keyword", required = false) String keyword) {
        DccProjectCodePageReqVO query = new DccProjectCodePageReqVO();
        query.setPageNo(1);
        query.setPageSize(20);
        query.setKeyword(keyword == null || keyword.isBlank() ? null : keyword.trim());
        query.setStatus("ENABLE");
        return success(BeanUtils.toBean(projectCodeService.getProjectCodePage(query).getList(),
                DccProjectCodeRespVO.class));
    }

    @PostMapping
    @Operation(summary = "提交注册证上传审批")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:upload:create')")
    @Transactional(rollbackFor = Exception.class)
    public CommonResult<Long> submit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @ModelAttribute DccRegistrationCertificateUploadSubmitReqVO reqVO,
            HttpServletRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long actorId = getLoginUserId();
        String requestTraceId = DccRequestAuditContext.from(request, TracerUtils.getTraceId()).requestId();
        DccRegistrationCertificateUploadSubmitResult result = uploadService.submitUploadForApproval(
                tenantId, actorId, idempotencyKey, requestTraceId, reqVO.toCommand());
        approvalService.startNativeApproval(
                tenantId, actorId, new DccRegistrationCertificateApprovalStartCommand(result.requestId()));
        return success(result.requestId());
    }
}
