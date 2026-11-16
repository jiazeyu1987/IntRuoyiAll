package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MesFrontlinePqcRestorePatrolReqVO {
    @NotNull
    @Positive
    private Long activeOrderId;

    @NotNull
    @Positive
    private Integer roundNo;
}
