package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrDeviationCorrectiveVerifyReqVO {
    @NotNull @Positive private Long deviationId;
    @NotNull @Positive private Integer expectedContentVersion;
    @NotBlank private String expectedContentHash;
    @NotNull private Boolean passed;
    @NotBlank @Size(max = 4000) private String comment;
}
