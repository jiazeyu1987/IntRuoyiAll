package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - MES PQC 生产放行拒绝 Request VO")
@Data
public class MesPqcProductionReleaseRejectReqVO {

    @NotNull
    private Long applicationId;

    @NotNull
    private Long pqcReleaseWorkTaskId;

    @NotNull
    @Min(1)
    private Integer expectedVersion;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[\\x21-\\x7E]+")
    private String idempotencyKey;

    @NotBlank
    @Size(max = 500)
    private String rejectReason;
}
