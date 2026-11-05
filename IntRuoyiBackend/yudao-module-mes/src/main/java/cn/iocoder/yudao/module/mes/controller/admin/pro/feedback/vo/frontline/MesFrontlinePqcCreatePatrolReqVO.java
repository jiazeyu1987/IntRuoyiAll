package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MesFrontlinePqcCreatePatrolReqVO {
    @NotNull @Positive
    private Long activeOrderId;
    @NotNull @Min(0)
    private Integer expectedPreviousRoundNo;
}
