package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - DCC 项目代码关联文件 AI 分类 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectCodeAssociatedFileAiCategoryRespVO {

    private Long fileId;
    private String fileName;
    private String currentStage;
    private String currentFileType;
    private String targetStage;
    private String targetFileType;
    private Boolean matched;
    private String classificationStatus;
    private String classificationMessage;
}
