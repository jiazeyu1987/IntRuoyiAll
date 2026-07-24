package cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SrmProcurementPlanRespVO {

    private Long id;
    private String planNo;
    private String planTitle;
    private String procurementMethod;
    private String procurementMethodLabel;
    private BigDecimal expectedAmount;
    private String planStatus;
    private String planStatusLabel;
    private String remark;
    private String submittedName;
    private LocalDateTime submittedTime;
    private String auditName;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Long generatedProjectId;
    private String generatedProjectNo;
    private String generatedProjectType;
    private LocalDateTime generatedTime;
    private LocalDateTime createTime;
    private List<Line> lines;
    private List<ApprovalRecord> approvalRecords;

    @Data
    public static class Line {
        private Long id;
        private String lineNo;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private String unit;
        private LocalDate requiredDate;
    }

    @Data
    public static class ApprovalRecord {
        private Long id;
        private String action;
        private String actionLabel;
        private String operatorName;
        private LocalDateTime operationTime;
        private String remark;
    }
}
