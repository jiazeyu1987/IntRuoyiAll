package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeOpsInspectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运行控制台业务健康 Response VO")
@Data
public class RuntimeControlBusinessHealthRespVO {

    @Schema(description = "汇总状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "NO_GO")
    private RuntimeOpsInspectionStatus status;

    @Schema(description = "采样时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime sampledAt;

    @Schema(description = "健康项", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<RuntimeControlBusinessHealthItemRespVO> items;
}
