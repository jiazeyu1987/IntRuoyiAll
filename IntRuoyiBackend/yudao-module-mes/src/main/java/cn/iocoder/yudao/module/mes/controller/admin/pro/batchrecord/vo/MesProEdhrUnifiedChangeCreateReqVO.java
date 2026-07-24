package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrUnifiedChangeCreateReqVO {

    @NotBlank(message = "对象类型不能为空")
    private String controlledObjectType;

    @NotBlank(message = "对象不能为空")
    private String controlledObjectId;

    @NotBlank(message = "对象编号不能为空")
    private String controlledObjectCode;

    @NotBlank(message = "原版本不能为空")
    private String currentVersion;

    @NotBlank(message = "目标版本不能为空")
    private String targetVersion;

    @NotBlank(message = "变更类型不能为空")
    private String changeType;

    @NotBlank(message = "风险等级不能为空")
    private String riskLevel;

    private String reasonCategory;

    @NotBlank(message = "变更原因不能为空")
    private String reason;

    @NotBlank(message = "差异快照不能为空")
    private String diffSnapshotJson;

    @NotBlank(message = "影响范围不能为空")
    private String impactSummaryJson;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
