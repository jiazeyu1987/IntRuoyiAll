package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 一线报工与记录本一体提交 Response VO")
@Data
@Accessors(chain = true)
public class MesProFrontlineFeedbackSubmitRespVO {

    @Schema(description = "报工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "501")
    private Long feedbackId;

    @Schema(description = "记录本条目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "701")
    private Long recordbookEntryId;

    @Schema(description = "记录本事件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "702")
    private Long recordbookEventId;

    @Schema(description = "工序池提交事件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "801")
    private Long processPoolEventId;

}
