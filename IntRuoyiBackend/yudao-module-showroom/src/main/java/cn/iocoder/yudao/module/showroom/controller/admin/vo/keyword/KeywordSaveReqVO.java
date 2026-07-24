package cn.iocoder.yudao.module.showroom.controller.admin.vo.keyword;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "管理后台 - 展厅关键词新增/修改 Request VO")
public record KeywordSaveReqVO(
        @Schema(description = "关键词编号", example = "1")
        Long id,
        @Schema(description = "中文关键词", requiredMode = Schema.RequiredMode.REQUIRED, example = "上海瑛泰医疗器械有限公司")
        @NotBlank(message = "中文关键词不能为空")
        @Size(max = 255, message = "中文关键词长度不能超过255个字符")
        String nameZh,
        @Schema(description = "English Keyword", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "Shanghai INT Medical Instruments Co., Ltd.")
        @NotBlank(message = "English Keyword不能为空")
        @Size(max = 255, message = "English Keyword长度不能超过255个字符")
        String nameEn) {
}
