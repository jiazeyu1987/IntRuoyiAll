package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MesReleaseTaskNotifyRetryReqVO {
    @NotNull @Positive private Long deliveryId;
    @NotNull @Min(0) private Integer expectedVersion;
    @NotBlank @Size(max = 1000) private String reason;
}
