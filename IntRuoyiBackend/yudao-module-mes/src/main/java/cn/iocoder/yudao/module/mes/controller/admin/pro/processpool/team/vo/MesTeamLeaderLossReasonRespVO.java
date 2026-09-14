package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长损耗原因明细 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderLossReasonRespVO {

    @Schema(description = "损耗原因编号", example = "8301")
    private Long id;

    @Schema(description = "原因编码", example = "LOSS-001")
    private String reasonCode;

    @Schema(description = "原因名称", example = "正常损耗")
    private String reasonName;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

}
