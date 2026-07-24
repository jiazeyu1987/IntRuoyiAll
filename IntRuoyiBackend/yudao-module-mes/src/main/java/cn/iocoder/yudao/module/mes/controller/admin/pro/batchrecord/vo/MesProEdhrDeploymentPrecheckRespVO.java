package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - eDHR 部署授权接口门禁预检 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrDeploymentPrecheckRespVO {

    @Schema(description = "部署证据ID")
    private Long deploymentId;

    @Schema(description = "部署证据编号")
    private String deploymentCode;

    @Schema(description = "部署状态")
    private String deploymentStatus;

    @Schema(description = "门禁是否通过")
    private Boolean gatePassed;

    @Schema(description = "阻断原因")
    private String blockedReason;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "门禁检查时间")
    private LocalDateTime gateCheckedAt;

    @Schema(description = "证据快照校验值")
    private String evidenceSnapshotChecksum;

    @Schema(description = "门禁项")
    private List<MesProEdhrDeploymentGateItemRespVO> gateItems;
}

