package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MesDeviationReceiverAssignReqVO {
    @NotNull @Positive private Long deviationId;
    @NotNull @Positive private Integer expectedDeviationVersion;
    @NotNull @Positive private Long receiverId;
    @NotBlank @Size(max = 1000) private String reason;
}
