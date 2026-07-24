package cn.iocoder.yudao.module.dcc.controller.admin.directory.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - DCC 目录 active NAS 收集任务 Response VO")
@Data
public class DccDirectoryActiveNasTransferRespVO {

    @Schema(description = "是否存在 active 后台收集任务", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean active = Boolean.FALSE;

    @Schema(description = "后台收集任务编号")
    private Long taskId;

    @Schema(description = "后台收集任务状态")
    private String status;

    @Schema(description = "任务选择的 NAS 路径")
    private List<String> selectedNasPaths = new ArrayList<>();

    @Schema(description = "剩余待处理项数量")
    private Integer remainingPendingCount = 0;

    @Schema(description = "最后失败信息")
    private String lastFailureMessage;

    public static DccDirectoryActiveNasTransferRespVO inactive() {
        return new DccDirectoryActiveNasTransferRespVO();
    }
}
