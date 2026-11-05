package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MesFrontlinePqcCancelPatrolReqVO {
    @NotNull @Positive private Long activeOrderId;
    @NotNull @Positive private Integer roundNo;
}
