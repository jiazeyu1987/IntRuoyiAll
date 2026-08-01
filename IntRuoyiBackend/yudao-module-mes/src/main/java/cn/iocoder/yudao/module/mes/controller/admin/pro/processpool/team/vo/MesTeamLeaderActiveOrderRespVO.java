package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 班组长活跃订单 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRespVO {

    @Schema(description = "活跃订单记录编号", example = "8101")
    private Long id;

    @Schema(description = "生产订单编号", example = "9001")
    private Long workOrderId;

    @Schema(description = "活跃状态", example = "ACTIVE")
    private String activeStatus;

    @Schema(description = "加入活跃池时间")
    private LocalDateTime joinedAt;

    @Schema(description = "移除活跃池时间")
    private LocalDateTime removedAt;
}
