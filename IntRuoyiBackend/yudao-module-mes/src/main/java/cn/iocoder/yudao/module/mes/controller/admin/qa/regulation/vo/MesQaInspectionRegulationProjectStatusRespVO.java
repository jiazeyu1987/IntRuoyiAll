package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - MES QA 检验规程项目配置状态 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesQaInspectionRegulationProjectStatusRespVO {

    @Schema(description = "DCC 项目代码 ID")
    private Long dccProjectCodeId;

    @Schema(description = "是否已存在 QA 规程配置")
    private Boolean configured;

    @Schema(description = "该产品下 QA 规程数量")
    private Integer regulationCount;

    @Schema(description = "代表 QA 规程 ID")
    private Long regulationId;

    @Schema(description = "当前版本 ID")
    private Long currentVersionId;

    @Schema(description = "规程编码")
    private String regulationCode;

    @Schema(description = "规程名称")
    private String regulationName;

    @Schema(description = "规程生命周期状态")
    private String lifecycleStatus;
}
