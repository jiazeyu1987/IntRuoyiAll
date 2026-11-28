package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.notification.event.DccRegistrationCertificateChangeConfirmationService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import jakarta.annotation.Resource;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@RestController
@Validated
@RequestMapping("/dcc/registration-certificates/change-confirmations")
public class DccRegistrationCertificateChangeConfirmationController {
    private final DccRegistrationCertificateChangeConfirmationService service;
    private final DccRegistrationCertificateQueryService queryService;
    @Resource private AdminUserApi userApi;
    @Resource private DccRegistrationCertificateVersionMapper versionMapper;

    public record ConfirmationView(Long id, Long certificateId, Long versionId, String eventKey,
            Long recipientUserId, Long notifyMessageId, LocalDateTime sentAt, LocalDateTime confirmedAt,
            Integer versionNo, String recipientName) {}

    private List<ConfirmationView> project(List<DccRegistrationCertificateChangeConfirmationService.Confirmation> rows) {
        if (rows.isEmpty()) return List.of();
        var userIds = rows.stream().map(DccRegistrationCertificateChangeConfirmationService.Confirmation::recipientUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        var users = userIds.isEmpty() ? Map.<Long, cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO>of()
                : userApi.getUserMap(userIds);
        var versionIds = rows.stream().map(DccRegistrationCertificateChangeConfirmationService.Confirmation::versionId)
                .collect(Collectors.toSet());
        var versions = versionMapper.selectBatchIds(versionIds).stream()
                .collect(Collectors.toMap(DccRegistrationCertificateVersionDO::getId, Function.identity()));
        return rows.stream().map(row -> {
            var user = users.get(row.recipientUserId());
            var version = versions.get(row.versionId());
            String name = user == null ? null : cn.hutool.core.util.StrUtil.isNotBlank(user.getNickname())
                    ? user.getNickname() : user.getUsername();
            return new ConfirmationView(row.id(), row.certificateId(), row.versionId(), row.eventKey(),
                    row.recipientUserId(), row.notifyMessageId(), row.sentAt(), row.confirmedAt(),
                    version != null && Objects.equals(version.getCertificateId(), row.certificateId())
                            ? version.getVersionNo() : null, name);
        }).toList();
    }
    public DccRegistrationCertificateChangeConfirmationController(
            DccRegistrationCertificateChangeConfirmationService service,DccRegistrationCertificateQueryService queryService) {
        this.service=service;this.queryService=queryService;
    }
    @GetMapping("/certificate/{id}")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:query-current')")
    public CommonResult<List<ConfirmationView>> list(
            @PathVariable("id") @Positive Long certificateId,HttpServletRequest request) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        queryService.getDetail(tenant,getLoginUserId(),certificateId,null,DccRequestAuditContext.from(request,null));
        return success(project(service.list(tenant,certificateId)));
    }
    @GetMapping("/message/{id}")
    public CommonResult<ConfirmationView> get(
            @PathVariable("id") @Positive Long messageId) {
        return success(project(List.of(service.getForRecipient(TenantContextHolder.getRequiredTenantId(),getLoginUserId(),messageId))).get(0));
    }
    @PostMapping("/message/{id}/confirm")
    public CommonResult<ConfirmationView> confirm(
            @PathVariable("id") @Positive Long messageId) {
        return success(project(List.of(service.confirm(TenantContextHolder.getRequiredTenantId(),getLoginUserId(),messageId))).get(0));
    }
}
