package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - eDHR 报表查询 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrReportQueryRespVO {

    private Long reportDefinitionId;
    private String reportCode;
    private String reportName;
    private String caliberVersion;
    private LocalDateTime dataUpdatedAt;
    private String filterSnapshotJson;
    private String permissionSummaryJson;
    private String dataSourceSummary;
    private List<Map<String, Object>> rows;
}
