package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.businesstime.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 注册证业务时间模拟 Request VO")
@Data
public class DccRegistrationCertificateBusinessTimeSimulationReqVO {

    @Schema(description = "模拟业务日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-09-01")
    @NotNull(message = "模拟业务日期不能为空")
    private LocalDate businessDate;
}
