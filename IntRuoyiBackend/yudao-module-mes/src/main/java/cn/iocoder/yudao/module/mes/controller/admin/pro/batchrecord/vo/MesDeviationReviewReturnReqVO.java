package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MesDeviationReviewReturnReqVO {
    @NotNull @Positive private Long deviationId;
    @NotNull @Positive private Integer expectedContentVersion;
    @NotBlank @Size(max = 128) private String expectedContentHash;
    @NotBlank @Size(max = 32) private String node;
    @NotBlank @Size(max = 1000) private String reason;
}
