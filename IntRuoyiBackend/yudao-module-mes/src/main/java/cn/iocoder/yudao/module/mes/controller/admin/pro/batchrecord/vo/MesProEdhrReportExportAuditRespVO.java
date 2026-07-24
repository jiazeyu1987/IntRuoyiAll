package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProEdhrReportExportAuditRespVO {

    private Long id;
    private Long reportDefinitionId;
    private String reportCode;
    private String reportName;
    private String caliberVersion;
    private String operationType;
    private String filterSnapshotJson;
    private String permissionSummaryJson;
    private String dataRangeSummary;
    private String resultStatus;
    private String failureReason;
    private Long operatorUserId;
    private String operatorUsername;
    private LocalDateTime occurredAt;
    private LocalDateTime createTime;
}
