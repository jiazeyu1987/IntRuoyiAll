package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - eDHR 部署授权接口门禁项 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrDeploymentGateItemRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "部署证据ID")
    private Long deploymentId;

    @Schema(description = "门禁编码")
    private String gateCode;

    @Schema(description = "门禁名称")
    private String gateName;

    @Schema(description = "门禁状态")
    private String gateStatus;

    @Schema(description = "证据来源")
    private String evidenceSource;

    @Schema(description = "缺失证据")
    private String missingEvidence;

    @Schema(description = "责任人")
    private String ownerName;

    @Schema(description = "下一步动作")
    private String nextAction;

    @Schema(description = "签核影响")
    private String signoffImpact;
}

