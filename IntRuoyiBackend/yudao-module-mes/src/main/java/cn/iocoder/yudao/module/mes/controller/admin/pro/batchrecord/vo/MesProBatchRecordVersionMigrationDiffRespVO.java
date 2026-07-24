package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - eDHR批记录版本迁移差异 Response VO")
@Data
@Accessors(chain = true)
public class MesProBatchRecordVersionMigrationDiffRespVO {

    @Schema(description = "版本ID")
    private Long versionId;

    @Schema(description = "迁移差异项")
    private List<Item> items;

    @Schema(description = "阻断项数量")
    private Long blockerCount;

    @Schema(description = "需确认项数量")
    private Long confirmRequiredCount;

    @Schema(description = "已确认项数量")
    private Long confirmedCount;

    @Schema(description = "是否允许提交审批")
    private Boolean approvalReady;

    @Data
    @Accessors(chain = true)
    public static class Item {

        @Schema(description = "迁移项ID")
        private Long itemId;

        @Schema(description = "差异分组")
        private String diffGroup;

        @Schema(description = "差异类型")
        private String diffType;

        @Schema(description = "风险等级")
        private String riskLevel;

        @Schema(description = "来源逻辑键")
        private String sourceLogicalKey;

        @Schema(description = "目标逻辑键")
        private String targetLogicalKey;

        @Schema(description = "匹配置信度")
        private BigDecimal matchConfidence;

        @Schema(description = "匹配证据 JSON")
        private String matchEvidenceJson;

        @Schema(description = "规则类型")
        private String ruleType;

        @Schema(description = "业务负责人类型")
        private String businessOwnerType;

        @Schema(description = "是否已确认")
        private Boolean confirmed;

        @Schema(description = "确认人")
        private Long confirmedBy;

        @Schema(description = "确认时间")
        private LocalDateTime confirmedAt;

        @Schema(description = "确认意见")
        private String confirmComment;

        @Schema(description = "提示信息")
        private String message;
    }
}
