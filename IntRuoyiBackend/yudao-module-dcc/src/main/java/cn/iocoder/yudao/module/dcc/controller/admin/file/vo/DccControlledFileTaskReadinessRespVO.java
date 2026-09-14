package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - DCC 受控文件任务动作就绪度 Response VO")
@Data
public class DccControlledFileTaskReadinessRespVO {

    @Schema(description = "当前动作是否就绪", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean ready;

    @Schema(description = "是否为最终批准节点", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean finalApproval;

    @Schema(description = "阻塞项", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DccControlledFileTaskReadinessBlockerRespVO> blockers;

}
