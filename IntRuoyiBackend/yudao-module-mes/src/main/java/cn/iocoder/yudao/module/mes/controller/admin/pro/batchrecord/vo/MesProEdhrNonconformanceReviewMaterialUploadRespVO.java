package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 不合格评审材料上传结果")
@Data
public class MesProEdhrNonconformanceReviewMaterialUploadRespVO {

    @Schema(description = "正式文件记录编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileId;

    @Schema(description = "文件访问地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(description = "正式原文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "存储配置编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long configId;

    @Schema(description = "存储路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String path;

    @Schema(description = "文件类型")
    private String type;

    @Schema(description = "文件大小")
    private Long size;
}
