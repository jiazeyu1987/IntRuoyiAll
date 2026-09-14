package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES QA 通用检验规程发布版本选项 Response VO")
@Data
@Builder
public class MesQaCommonRegulationVersionOptionRespVO {

    @Schema(description = "通用检验规程 DCC 项目代码 ID")
    private Long commonDccProjectCodeId;

    @Schema(description = "通用检验规程主档 ID")
    private Long commonRegulationId;

    @Schema(description = "通用检验规程发布版本 ID")
    private Long commonRegulationVersionId;

    @Schema(description = "通用检验规程编号")
    private String commonRegulationCode;

    @Schema(description = "通用检验规程名称")
    private String commonRegulationName;

    @Schema(description = "版本")
    private String versionNo;

    @Schema(description = "版本状态")
    private String lifecycleStatus;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;
}
