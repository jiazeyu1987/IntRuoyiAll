package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.process;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "设备工序标准小时产能维护")
public class MesMachineryProcessCapacitySaveReqVO {
    @NotNull @Positive
    private Long machineryId;
    @NotNull @Positive
    private Long processId;
    @NotNull(message = "标准小时产能不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "标准小时产能必须大于0")
    @Digits(integer = 12, fraction = 6)
    private BigDecimal standardHourlyCapacity;
    @NotBlank(message = "请填写产能维护说明")
    @Size(max = 500)
    private String remark;
}
