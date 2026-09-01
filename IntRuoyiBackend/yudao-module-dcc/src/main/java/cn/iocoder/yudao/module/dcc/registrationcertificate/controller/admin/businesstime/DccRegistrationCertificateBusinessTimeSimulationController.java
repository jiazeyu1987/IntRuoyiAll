package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.businesstime;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.businesstime.vo.DccRegistrationCertificateBusinessTimeSimulationReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.businesstime.vo.DccRegistrationCertificateBusinessTimeSimulationRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateBusinessTimeSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 国内注册证业务时间模拟")
@RestController
@RequestMapping("/dcc/registration-certificates/business-time")
@Validated
public class DccRegistrationCertificateBusinessTimeSimulationController {

    private final DccRegistrationCertificateBusinessTimeSimulationService simulationService;

    public DccRegistrationCertificateBusinessTimeSimulationController(
            DccRegistrationCertificateBusinessTimeSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/simulate-daily-run")
    @Operation(summary = "按业务日期模拟注册证上午 9 点每日任务")
    @PreAuthorize("@ss.hasPermission('dcc:registration-certificate:config:update')")
    public CommonResult<DccRegistrationCertificateBusinessTimeSimulationRespVO> simulateDailyRun(
            @Valid @RequestBody DccRegistrationCertificateBusinessTimeSimulationReqVO reqVO) {
        return success(DccRegistrationCertificateBusinessTimeSimulationRespVO.of(
                simulationService.simulateMorningRun(
                        TenantContextHolder.getRequiredTenantId(), reqVO.getBusinessDate())));
    }
}
