package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES PQC 切换实际填写员工 Request VO")
@Data
public class MesFrontlinePqcSwitchEmployeeReqVO {

    @Schema(description = "活跃订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活跃订单编号不能为空")
    private Long activeOrderId;

    @Schema(description = "QA 规程发布版本编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "QA 规程发布版本编号不能为空")
    private Long regulationVersionId;

    @Schema(description = "QA 工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "QA 工序编号不能为空")
    private Long qaProcessId;

    @Schema(description = "PQC 检验任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "PQC 检验任务不能为空")
    private Long pqcTaskId;

    @Schema(description = "实际填写员工编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "实际填写员工编号不能为空")
    private Long actualEmployeeId;
}
