package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 一线报工工序池上下文 Request VO")
@Data
@Accessors(chain = true)
public class MesProFrontlineProcessPoolContextReqVO {

    @Schema(description = "生产工单编号；一线生产不匹配工单时为空", example = "41")
    private Long workOrderId;

    @Schema(description = "生产任务编号；一线生产不匹配任务时为空", example = "51")
    private Long taskId;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "21")
    @NotNull(message = "工艺路线不能为空")
    private Long routeId;

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "71")
    @NotNull(message = "路线工序不能为空")
    private Long routeProcessId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "31")
    @NotNull(message = "工序不能为空")
    private Long processId;

    @Schema(description = "工作站编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11")
    @NotNull(message = "工作站不能为空")
    private Long workstationId;

    @Schema(description = "设备编号；无正式设备配置的工序可为空", example = "501")
    private Long deviceId;

    @Schema(description = "设备账号用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    @NotNull(message = "设备账号不能为空")
    private Long deviceAccountUserId;

    @Schema(description = "模板类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCTION_SIMPLE")
    @NotNull(message = "模板类型不能为空")
    private String templateType;

}
