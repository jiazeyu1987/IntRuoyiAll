package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.businesstime.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun.DccRegistrationCertificateBusinessTimeSimulationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 注册证业务时间模拟 Response VO")
@Data
@Builder
public class DccRegistrationCertificateBusinessTimeSimulationRespVO {

    @Schema(description = "租户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tenantId;

    @Schema(description = "模拟业务日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessDate;

    @Schema(description = "模拟触发时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private String simulatedAt;

    @Schema(description = "定时任务执行结果", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobResult;

    public static DccRegistrationCertificateBusinessTimeSimulationRespVO of(
            DccRegistrationCertificateBusinessTimeSimulationResult result) {
        return DccRegistrationCertificateBusinessTimeSimulationRespVO.builder()
                .tenantId(result.tenantId())
                .businessDate(result.businessDate().toString())
                .simulatedAt(result.simulatedAt().toString())
                .jobResult(result.jobResult())
                .build();
    }
}
