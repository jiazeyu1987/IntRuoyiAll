package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - MES QA 检验规程保存 Response VO")
@Data
@Builder
public class MesQaInspectionRegulationSaveRespVO {

    @Schema(description = "DCC 项目代码 ID")
    private Long dccProjectCodeId;

    @Schema(description = "QA 检验规程 ID")
    private Long regulationId;

    @Schema(description = "QA 检验规程草稿版本 ID")
    private Long draftVersionId;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "生命周期状态")
    private String lifecycleStatus;

    @Schema(description = "是否不可变")
    private Boolean immutable;
}
