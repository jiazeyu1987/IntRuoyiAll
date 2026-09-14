package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单版本升级重启提交 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradeSubmitRespVO {

    private Long activeOrderId;
    private String requestCode;
    private String approvalStatus;
    private String freezeStatus;
}
