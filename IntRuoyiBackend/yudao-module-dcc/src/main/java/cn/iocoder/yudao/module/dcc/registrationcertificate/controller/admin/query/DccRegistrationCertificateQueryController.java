package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.query.vo.DccRegistrationCertificatePageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.history.DccRegistrationCertificateHistoryService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateDetail;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateOldIndexItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageItem;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证查询")
@RestController
@RequestMapping("/dcc/registration-certificates")
@Validated
public class DccRegistrationCertificateQueryController {

    private final DccRegistrationCertificateQueryService queryService;
    private final DccRegistrationCertificateHistoryService historyService;

    public DccRegistrationCertificateQueryController(
            DccRegistrationCertificateQueryService queryService,
            DccRegistrationCertificateHistoryService historyService) {
        this.queryService = queryService;
        this.historyService = historyService;
    }

    @GetMapping("/page")
    @Operation(summary = "获取当前注册证分页")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<PageResult<DccRegistrationCertificatePageItem>> getPage(
            @Valid DccRegistrationCertificatePageReqVO reqVO,
            HttpServletRequest request) {
        return success(queryService.getPage(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), reqVO.toQuery(),
                DccRequestAuditContext.from(request, null)));
    }

    @GetMapping("/old-index/page")
    @Operation(summary = "获取旧注册证索引分页")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<PageResult<DccRegistrationCertificateOldIndexItem>> getOldIndexPage(
            @Valid DccRegistrationCertificatePageReqVO reqVO,
            HttpServletRequest request) {
        return success(queryService.getOldIndexPage(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), reqVO.toQuery(),
                DccRequestAuditContext.from(request, null)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取注册证详情")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<DccRegistrationCertificateDetail> getDetail(
            @PathVariable("id") @Positive Long id,
            HttpServletRequest request) {
        return success(queryService.getDetail(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), id,
                DccRequestAuditContext.from(request, null)));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "获取注册证履历")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<List<DccRegistrationCertificateHistoryItem>> getHistory(
            @PathVariable("id") @Positive Long id,
            HttpServletRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Long actorId = getLoginUserId();
        queryService.getDetail(tenantId, actorId, id, DccRequestAuditContext.from(request, null));
        return success(historyService.listHistory(tenantId, id));
    }
}
