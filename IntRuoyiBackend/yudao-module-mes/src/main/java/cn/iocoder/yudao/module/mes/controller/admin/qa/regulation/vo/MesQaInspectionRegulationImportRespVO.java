package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - MES QA Word 模板导入 Response VO")
@Data
@Builder
public class MesQaInspectionRegulationImportRespVO {

    private Long dccProjectCodeId;
    private Long regulationId;
    private Long draftVersionId;
    private String regulationCode;
    private String regulationName;
    private String versionNo;
    private LocalDate effectiveDate;
    private String lifecycleStatus;
    private String route;
    private Integer processCount;
    private Integer itemCount;
    private Integer inheritedItemCount;
    private Integer createdItemCount;
}
