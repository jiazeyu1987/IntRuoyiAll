package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 一线正式报工上下文 Request VO")
@Data
@Accessors(chain = true)
public class MesFrontlineSubmitContextReqVO {

    @Schema(description = "生产任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生产任务不能为空")
    private Long taskId;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "工艺路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线工序不能为空")
    private Long routeProcessId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工序不能为空")
    private Long processId;
}
