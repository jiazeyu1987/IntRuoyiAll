package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES eDHR 放行资料限制配置 Response VO")
@Data
@Accessors(chain = true)
public class EdhrReleaseDossierRequirementSettingRespVO {

    @Schema(description = "来料检报告是否放行必传")
    private Boolean incomingInspectionReportRequired;

    @Schema(description = "灭菌报告是否放行必传")
    private Boolean sterilizationReportRequired;

    @Schema(description = "成品检报告是否放行必传")
    private Boolean finishedProductInspectionReportRequired;

    @Schema(description = "成品检记录是否放行必传")
    private Boolean finishedProductInspectionRecordRequired;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置内容哈希")
    private String configHash;

    @Schema(description = "最后更新人")
    private String updatedBy;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
