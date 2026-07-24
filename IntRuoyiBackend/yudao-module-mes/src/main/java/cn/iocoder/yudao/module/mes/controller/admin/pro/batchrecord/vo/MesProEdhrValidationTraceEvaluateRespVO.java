package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - eDHR 追溯门禁评估 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrValidationTraceEvaluateRespVO {

    @Schema(description = "验证包ID")
    private Long packageId;

    @Schema(description = "验证包编码")
    private String packageCode;

    @Schema(description = "验证包状态")
    private String validationStatus;

    @Schema(description = "是否具备OQ Ready")
    private Boolean oqReady;

    @Schema(description = "追溯状态")
    private String traceStatus;

    @Schema(description = "URS数量")
    private Integer ursCount;

    @Schema(description = "FRS数量")
    private Integer frsCount;

    @Schema(description = "风险数量")
    private Integer riskCount;

    @Schema(description = "IQ数量")
    private Integer iqCount;

    @Schema(description = "OQ数量")
    private Integer oqCount;

    @Schema(description = "PQ数量")
    private Integer pqCount;

    @Schema(description = "追溯关系数量")
    private Integer traceLinkCount;

    @Schema(description = "断裂追溯数量")
    private Integer brokenTraceCount;

    @Schema(description = "断裂明细")
    private List<MesProEdhrValidationTraceIssueRespVO> brokenItems;

    @Schema(description = "阻断原因")
    private String blockedReason;

    @Schema(description = "追溯摘要")
    private String summary;

    @Schema(description = "下一步动作")
    private String nextAction;
}
