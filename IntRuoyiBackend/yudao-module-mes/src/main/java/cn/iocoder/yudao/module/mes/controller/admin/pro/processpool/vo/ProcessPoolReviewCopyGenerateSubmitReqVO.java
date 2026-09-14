package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFragmentOriginalField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - MES 工序池审核副本生成提交 Request VO")
@Data
public class ProcessPoolReviewCopyGenerateSubmitReqVO {

    @Schema(description = "来源工序池提交事件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long eventId;

    @Schema(description = "审核人用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long reviewerUserId;

    @Schema(description = "审核电子签名ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long reviewerSignatureId;

    @Schema(description = "审核电子签名用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long reviewerSignatureUserId;

    @Schema(description = "审核电子签名快照", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String reviewerSignatureSnapshot;

    @Schema(description = "字段映射和上下限元数据", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty
    private List<FieldMapping> fieldMappings;

    @Schema(description = "审核副本字段映射")
    @Data
    public static class FieldMapping {

        @Schema(description = "字段编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String fieldCode;

        @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        private String fieldName;

        @Schema(description = "下限", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal lowerLimit;

        @Schema(description = "上限", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private BigDecimal upperLimit;

        @Schema(description = "值类型")
        private String valueType;

        @Schema(description = "是否影响 FIFO 分配")
        private Boolean affectsAllocation;

        @Schema(description = "影响 FIFO 分配的原始字段")
        private MesProcessPoolFragmentOriginalField allocationField;

        @Schema(description = "来源数量片段ID")
        private Long sourceQuantityFragmentId;

        @Schema(description = "模板字段元数据快照")
        private String templateFieldMetadataJson;
    }
}
