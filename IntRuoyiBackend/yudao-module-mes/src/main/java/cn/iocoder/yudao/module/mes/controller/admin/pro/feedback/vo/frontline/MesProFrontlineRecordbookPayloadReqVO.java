package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - MES 一线报工记录本原始条目 Request VO")
@Data
@Accessors(chain = true)
public class MesProFrontlineRecordbookPayloadReqVO {

    @Schema(description = "记录本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "901")
    @NotNull(message = "记录本不能为空")
    private Long recordbookId;

    @Schema(description = "条目标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "F2 production original")
    @NotNull(message = "条目标题不能为空")
    private String entryTitle;

    @Schema(description = "条目正文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "条目正文不能为空")
    private Map<String, Object> entryContent;

    @Schema(description = "设备参数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设备参数不能为空")
    private Map<String, Object> equipmentParameters;

    @Schema(description = "受控标签编码")
    private List<String> tagCodes;

    @Schema(description = "幂等键", requiredMode = Schema.RequiredMode.REQUIRED, example = "F2-20260730-001")
    @NotNull(message = "幂等键不能为空")
    private String idempotencyKey;

    @Schema(description = "备注", example = "recordbook original")
    private String remark;

}
