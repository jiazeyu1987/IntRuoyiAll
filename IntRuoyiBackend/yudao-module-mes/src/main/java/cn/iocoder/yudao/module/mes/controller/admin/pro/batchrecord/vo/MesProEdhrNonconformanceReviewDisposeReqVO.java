package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - eDHR 不合格评审处置 Request VO")
@Data
public class MesProEdhrNonconformanceReviewDisposeReqVO {

    @Schema(description = "评审单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "评审单不能为空")
    private Long id;

    @Schema(description = "处置结论：concession_release/rework/void", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处置结论不能为空")
    private String disposition;

    @Schema(description = "评审材料URL摘要")
    private String reviewMaterialUrl;

    @Schema(description = "当前有效评审材料清单", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ReviewMaterialReqVO> reviewMaterials;

    @Schema(description = "评审材料上传删除操作记录")
    private List<ReviewMaterialEventReqVO> reviewMaterialEvents;

    @Schema(description = "评审意见", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "评审意见不能为空")
    private String reviewOpinion;

    @Schema(description = "电子签名密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "电子签名密码不能为空")
    private String signaturePassword;

    @Data
    public static class ReviewMaterialReqVO {

        @Schema(description = "材料 URL", requiredMode = Schema.RequiredMode.REQUIRED)
        private String url;

        @Schema(description = "材料文件名")
        private String fileName;

        @Schema(description = "页面排序")
        private Integer sortNo;
    }

    @Data
    public static class ReviewMaterialEventReqVO {

        @Schema(description = "操作类型：UPLOAD/DELETE", requiredMode = Schema.RequiredMode.REQUIRED)
        private String action;

        @Schema(description = "材料 URL", requiredMode = Schema.RequiredMode.REQUIRED)
        private String url;

        @Schema(description = "材料文件名")
        private String fileName;

        @Schema(description = "页面顺序")
        private Integer sequence;
    }
}
