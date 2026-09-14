package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES QA 检验规程版本选项 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesQaInspectionRegulationVersionOptionRespVO {

    private Long dccProjectCodeId;
    private Long regulationId;
    private Long versionId;
    private String versionNo;
    private String lifecycleStatus;
    private LocalDate effectiveDate;
    private LocalDateTime publishedAt;
    private LocalDateTime retiredAt;
    private Boolean currentPublished;
}
