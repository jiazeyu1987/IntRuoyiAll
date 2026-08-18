package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo.DccRegistrationCertificateReminderConfigRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo.DccRegistrationCertificateReminderConfigUpdateReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 国内注册证提醒配置")
@RestController
@RequestMapping("/dcc/registration-certificates/reminder-config")
@Validated
public class DccRegistrationCertificateReminderConfigController {

    private final DccRegistrationCertificateConfigService configService;

    public DccRegistrationCertificateReminderConfigController(DccRegistrationCertificateConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @Operation(summary = "获取国内注册证提醒配置")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:config:query')")
    public CommonResult<DccRegistrationCertificateReminderConfigRespVO> getConfig() {
        return success(DccRegistrationCertificateReminderConfigRespVO.of(
                configService.getOrCreate(TenantContextHolder.getRequiredTenantId())));
    }

    @PutMapping
    @Operation(summary = "更新国内注册证提醒配置")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:config:update')")
    public CommonResult<DccRegistrationCertificateReminderConfigRespVO> updateConfig(
            @Valid @RequestBody DccRegistrationCertificateReminderConfigUpdateReqVO reqVO) {
        return success(DccRegistrationCertificateReminderConfigRespVO.of(configService.update(
                TenantContextHolder.getRequiredTenantId(), getLoginUserId(), reqVO.toCommand())));
    }
}
