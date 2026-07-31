package cn.iocoder.yudao.module.system.controller.admin.profileworkbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 个人工作台任务隐藏保存 Request VO")
@Data
@Accessors(chain = true)
public class ProfileWorkbenchTaskVisibilitySaveReqVO {

    @Schema(description = "个人工作台任务唯一 Key", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "eDHR工作任务:9001")
    @NotBlank(message = "taskKey 不能为空")
    private String taskKey;

    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "批记录")
    @NotBlank(message = "taskType 不能为空")
    private String taskType;

    @Schema(description = "任务来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "eDHR工作任务")
    @NotBlank(message = "source 不能为空")
    private String source;

    @Schema(description = "业务编号", example = "9001")
    private String businessId;

    @Schema(description = "任务摘要", example = "批记录任务 9001")
    private String detail;
}
