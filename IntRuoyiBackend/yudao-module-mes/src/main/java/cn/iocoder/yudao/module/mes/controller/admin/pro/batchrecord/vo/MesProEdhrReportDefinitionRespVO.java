package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR 报表定义 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrReportDefinitionRespVO {

    private Long id;
    private String reportCode;
    private String reportName;
    private String reportType;
    private Long datasetId;
    private String datasetCode;
    private String datasetVersion;
    private String status;
    private String caliberVersion;
    private String fieldCaliberJson;
    private String filterSchemaJson;
    private String drilldownTargetJson;
    private String permissionSummaryJson;
    private String dataSourceStatus;
    private String sampleQueryJson;
    private LocalDateTime publishedAt;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
