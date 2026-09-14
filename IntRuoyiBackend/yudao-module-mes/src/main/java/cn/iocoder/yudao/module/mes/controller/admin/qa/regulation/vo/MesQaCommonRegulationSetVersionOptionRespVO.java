package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES QA 通用检验规程套发布版本选项 Response VO")
@Data
@Builder
public class MesQaCommonRegulationSetVersionOptionRespVO {

    private Long commonRegulationSetId;
    private Long commonRegulationSetVersionId;
    private String commonRegulationSetCode;
    private String commonRegulationSetName;
    private String versionNo;
    private String lifecycleStatus;
    private LocalDate effectiveDate;
    private LocalDateTime publishedAt;
    private Integer memberCount;
}
