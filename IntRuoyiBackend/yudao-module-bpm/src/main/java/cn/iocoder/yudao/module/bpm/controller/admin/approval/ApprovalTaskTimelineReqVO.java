package cn.iocoder.yudao.module.bpm.controller.admin.approval;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 统一审批中心任务轨迹 Request VO")
@Data
@Accessors(chain = true)
public class ApprovalTaskTimelineReqVO {

    @Schema(description = "模块编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DCC")
    @NotNull(message = "模块编码不能为空")
    private ApprovalModuleCode moduleCode;

    @Schema(description = "来源任务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "DCC_CONTROLLED_FILE_TASK")
    @NotBlank(message = "来源任务类型不能为空")
    private String sourceTaskType;

    @Schema(description = "来源任务编号")
    private String sourceTaskId;

    @Schema(description = "业务键")
    private String businessKey;

    @Schema(description = "流程实例编号")
    private String processInstanceId;

    public ApprovalTaskTimelineQuery toQuery() {
        return new ApprovalTaskTimelineQuery()
                .setModuleCode(moduleCode)
                .setSourceTaskType(sourceTaskType)
                .setSourceTaskId(sourceTaskId)
                .setBusinessKey(businessKey)
                .setProcessInstanceId(processInstanceId);
    }
}
