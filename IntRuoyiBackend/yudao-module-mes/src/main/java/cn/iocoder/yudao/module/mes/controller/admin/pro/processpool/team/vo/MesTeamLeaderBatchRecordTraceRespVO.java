package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderBatchRecordTraceRespVO {

    private Long workOrderId;
    private Long routeProcessId;
    private Long processId;
    private Long executionId;
    private String executionCode;
    private String workOrderCode;
    private String batchRecordReportId;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    private Long fieldAuditRevision;
    private String fieldAuditHeadHash;
    private Long fieldAuditLastBatchId;
    private String cellValuesJson;
    private List<Cell> cells;

    @Data
    @Accessors(chain = true)
    public static class Cell {

        private Long auditItemId;
        private Long auditBatchId;
        private Long executionId;
        private Long fieldAuditRevision;
        private String fieldPath;
        private String fieldKey;
        private Integer rowIndex;
        private Integer columnIndex;
        private String valueType;
        private String valueJson;
        private String valueDisplay;
    }
}
