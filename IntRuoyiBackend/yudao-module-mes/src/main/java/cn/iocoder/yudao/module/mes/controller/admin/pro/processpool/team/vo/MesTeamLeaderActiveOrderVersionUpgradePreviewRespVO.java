package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 班组长活跃订单版本升级重启预览 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradePreviewRespVO {

    @Schema(description = "活跃订单记录编号")
    private Long activeOrderId;

    @Schema(description = "生产工单编号")
    private Long workOrderId;

    @Schema(description = "生产工单号")
    private String workOrderCode;

    @Schema(description = "是否全部解析为最新正式版本")
    private Boolean allLatestFormalVersions;

    @Schema(description = "是否允许逐项版本选择，固定为 false")
    private Boolean perVersionSelectionAllowed;

    @Schema(description = "是否允许提交升级审批")
    private Boolean submittable;

    @Schema(description = "阻塞原因")
    private List<String> blockers = List.of();

    @Schema(description = "当前锁定版本基线")
    private List<VersionLine> currentVersions = List.of();

    @Schema(description = "本次升级目标版本基线")
    private List<VersionLine> targetVersions = List.of();

    @Data
    @Accessors(chain = true)
    public static class VersionLine {

        private String objectType;
        private String objectName;
        private Long objectId;
        private Long currentVersionId;
        private String currentVersionNo;
        private Long targetVersionId;
        private String targetVersionNo;
        private Boolean changed;
    }
}
