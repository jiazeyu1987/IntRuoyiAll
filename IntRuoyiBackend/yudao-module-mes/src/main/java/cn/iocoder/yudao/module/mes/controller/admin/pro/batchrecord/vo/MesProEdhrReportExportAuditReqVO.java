package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - eDHR 报表导出审计 Request VO")
@Data
public class MesProEdhrReportExportAuditReqVO {

    private Long reportDefinitionId;

    @NotBlank(message = "报表编码不能为空")
    private String reportCode;

    @NotBlank(message = "筛选快照不能为空")
    private String filterSnapshotJson;

    @NotBlank(message = "权限摘要不能为空")
    private String permissionSummaryJson;

    @NotBlank(message = "数据范围摘要不能为空")
    private String dataRangeSummary;
}
