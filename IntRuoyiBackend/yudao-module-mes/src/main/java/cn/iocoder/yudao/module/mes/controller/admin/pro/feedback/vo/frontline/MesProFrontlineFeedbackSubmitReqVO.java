package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Schema(description = "管理后台 - MES 一线报工与记录本一体提交 Request VO")
@Data
@Accessors(chain = true)
public class MesProFrontlineFeedbackSubmitReqVO {

    @Schema(description = "报工载荷", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "报工载荷不能为空")
    private MesProFrontlineFeedbackPayloadReqVO feedbackPayload;

    @Schema(description = "记录本原始条目载荷；一线生产无记录本上下文时可为空")
    @Valid
    private MesProFrontlineRecordbookPayloadReqVO recordbookPayload;

    @Schema(description = "工序池上下文", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "工序池上下文不能为空")
    private MesProFrontlineProcessPoolContextReqVO processPoolContext;

    @Schema(description = "工序池主提交幂等键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序池主提交幂等键不能为空")
    private String processPoolSubmissionIdempotencyKey;

    @Schema(description = "一线生产最大化会话快照编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "一线生产最大化会话快照编号不能为空")
    private String frontlineSessionSnapshotId;

    @Schema(description = "一线生产最大化会话快照哈希", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "一线生产最大化会话快照哈希不能为空")
    private String frontlineSessionSnapshotHash;

    @Schema(description = "实际操作员工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    @NotNull(message = "实际操作员工不能为空")
    private Long actualEmployeeId;

    @Schema(description = "服务端生成的签名编号；客户端不得预传", example = "4001")
    private Long signatureId;

    @Schema(description = "签名员工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3001")
    @NotNull(message = "签名员工不能为空")
    private Long signatureEmployeeId;

    @Schema(description = "签名员工电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "电子签名密码不能为空")
    private String signaturePassword;

    @Schema(description = "一线原始提交载荷", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "一线原始提交载荷不能为空")
    private Map<String, Object> rawPayload;

}
