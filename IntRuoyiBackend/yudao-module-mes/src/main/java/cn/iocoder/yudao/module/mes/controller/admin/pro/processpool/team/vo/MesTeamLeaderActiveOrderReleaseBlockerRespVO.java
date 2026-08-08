package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长活跃订单放行申请阻塞项 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseBlockerRespVO {

    @Schema(description = "阻塞类型", example = "PROCESS_INSPECTION_SOURCE_REQUIRED")
    private String blockerType;

    @Schema(description = "对象类型", example = "ACTIVE_ORDER")
    private String objectType;

    @Schema(description = "对象编号", example = "8101")
    private String objectId;

    @Schema(description = "对象编码", example = "MO20260808001")
    private String objectCode;

    @Schema(description = "阻塞原因", example = "当前活跃订单缺少已确认的过程检验汇集明细")
    private String reason;

    @Schema(description = "处理建议", example = "请由 PQC 组长确认过程检验后重新申请")
    private String suggestion;
}
