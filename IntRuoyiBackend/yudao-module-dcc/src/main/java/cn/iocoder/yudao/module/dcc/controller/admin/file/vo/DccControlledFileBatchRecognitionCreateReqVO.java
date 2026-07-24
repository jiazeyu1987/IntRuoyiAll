package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "管理后台 - DCC 受控浏览批量识别创建 Request VO")
@Data
public class DccControlledFileBatchRecognitionCreateReqVO {

    @Schema(description = "识别类型：BASIC_INFO 或 FILE_CATEGORY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String recognitionType;

    @Schema(description = "范围，CURRENT 或 GLOBAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String scope;

    @Schema(description = "目录编号，CURRENT 模式必填")
    private Long directoryId;

    @Schema(description = "是否包含子目录")
    private Boolean includeDescendantDirectories;

    @Schema(description = "关键字过滤")
    private String keyword;

    @Schema(description = "状态过滤")
    private String status;

    @Schema(description = "类别过滤")
    private Long categoryId;

    @Schema(description = "是否覆盖已有产品名称/编号")
    private Boolean overwriteExisting;

    @Schema(description = "已有识别记录策略：SKIP_ALL_EXISTING、RETRY_FAILED、OVERWRITE_ALL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String existingRecordPolicy;

    @Schema(description = "识别成功后是否同步文件名称和标题")
    private Boolean syncFileNameTitle;

    @Schema(description = "本任务 Codex 并发数")
    @Positive
    private Integer workerCount;
}
