package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 一线设备账号切换实际填写员工 Request VO")
@Data
public class MesFrontlineSwitchEmployeeReqVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活跃订单编号不能为空")
    private Long activeOrderId;
    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线编号不能为空")
    private Long routeId;
    @Schema(description = "工艺路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线工序编号不能为空")
    private Long routeProcessId;
    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工序编号不能为空")
    private Long processId;
    @Schema(description = "实际填写员工编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实际填写员工编号不能为空")
    private Long actualEmployeeId;

}
