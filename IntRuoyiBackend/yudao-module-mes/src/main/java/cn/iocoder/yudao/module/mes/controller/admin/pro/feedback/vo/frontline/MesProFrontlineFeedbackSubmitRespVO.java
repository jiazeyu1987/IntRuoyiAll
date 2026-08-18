package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

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

    @Schema(description = "参数审计摘要：RESOLVED/UNRESOLVED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String parameterAuditStatus;

    @Schema(description = "参数审计总数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parameterAuditTotalCount;

    @Schema(description = "已解析参数数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parameterAuditResolvedCount;

    @Schema(description = "未解析参数数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parameterAuditUnresolvedCount;

    @Schema(description = "稳定排序的参数审计明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ParameterAuditItemRespVO> auditItems;

    @Data
    @Accessors(chain = true)
    public static class ParameterAuditItemRespVO {

        private Integer readingIndex;
        private Long deviceId;
        private String parameterCode;
        private String parameterName;
        private String unit;
        private BigDecimal value;
        private String textValue;
        private BigDecimal lowerLimit;
        private BigDecimal upperLimit;
        private String parameterStatus;
        private String resolutionStatus;
        private String reasonCode;
        private String snapshotSource;
    }

}
