package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class MesFrontlinePqcCorrectRoundReqVO {
    @NotNull @Positive private Long sourceTaskId;
    @NotNull @Positive private Long targetTaskId;
    @NotNull @Positive private Long expectedSubmittedEventId;
    @NotBlank private String expectedContentHash;
}
