package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 生产组长活跃订单申请放行 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseApplyRespVO {

    @Schema(description = "申请记录编号", example = "10001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;

    @Schema(description = "活跃订单记录编号", example = "8101")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long activeOrderId;

    @Schema(description = "生产订单编号", example = "9001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workOrderId;

    @Schema(description = "生产工单号", example = "MO20260808001")
    private String workOrderCode;

    @Schema(description = "生产批号", example = "BATCH-20260808-01")
    private String batchCode;

    @Schema(description = "工艺路线编号", example = "7001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeId;

    @Schema(description = "工艺路线版本编号", example = "7002")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long routeVersionId;

    @Schema(description = "PQC生产放行待办编号", example = "9001")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long pqcReleaseWorkTaskId;

    @Schema(description = "申请状态", example = "PENDING_RELEASE_APPROVAL")
    private String status;

    @Schema(description = "正式来源快照哈希")
    private String sourceSnapshotHash;

    @Schema(description = "申请版本", example = "1")
    private Integer version;

    @Schema(description = "申请时间")
    private LocalDateTime appliedAt;
}
