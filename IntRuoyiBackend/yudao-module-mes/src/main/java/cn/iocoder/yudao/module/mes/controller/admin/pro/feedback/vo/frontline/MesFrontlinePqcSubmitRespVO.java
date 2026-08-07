package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES PQC 正式提交回执 Response VO")
@Data
public class MesFrontlinePqcSubmitRespVO {

    @Schema(description = "PQC 检验任务编号")
    private Long pqcTaskId;
    @Schema(description = "PQC 工序池事件编号")
    private Long pqcEventId;
    @Schema(description = "PQC 正式记录编号")
    private Long pqcRecordId;
    @Schema(description = "本次提交生成的电子签名编号")
    private Long signatureId;
    @Schema(description = "服务端判定的检验结果")
    private String inspectionResult;
    @Schema(description = "服务端提交时间")
    private LocalDateTime serverSubmitTime;
}
