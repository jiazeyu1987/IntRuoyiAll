package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MesDeviationInvestigationWithdrawReqVO {
    @NotNull @Positive private Long deviationId;
    @NotNull @Positive private Integer expectedContentVersion;
    @NotBlank @Size(max = 128) private String expectedContentHash;
    @NotBlank @Size(max = 1000) private String reason;
}
