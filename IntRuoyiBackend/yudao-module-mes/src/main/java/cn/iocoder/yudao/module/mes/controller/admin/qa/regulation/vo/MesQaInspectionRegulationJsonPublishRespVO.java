package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - 表单解析 QA 检验规程 JSON 发布 Response VO")
@Data
@Builder
public class MesQaInspectionRegulationJsonPublishRespVO {

    @Schema(description = "DCC 项目代码 ID")
    private Long dccProjectCodeId;

    @Schema(description = "QA 检验规程 ID")
    private Long regulationId;

    @Schema(description = "发布版本 ID")
    private Long publishedVersionId;

    @Schema(description = "发布版本号")
    private String versionNo;

    @Schema(description = "处理路由：CREATE / UPDATE")
    private String route;

    @Schema(description = "工序数量")
    private Integer processCount;

    @Schema(description = "检验项目数量")
    private Integer itemCount;
}
