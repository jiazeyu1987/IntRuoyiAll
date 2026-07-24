package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ThirdPartyFeedbackImportResult {

    private Integer sheetCount;

    private Integer importedCount;

    private Integer pendingCount;

    private Integer submittedCount;

    private Integer skippedRows;

    private List<String> feedbackCodes;

    private List<Long> importRecordIds;

    private List<DirectWorkReportDetail> directWorkReportDetails;

    private List<DirectWorkReportSkipWarning> directWorkReportSkipWarnings;

    @Data
    public static class DirectWorkReportDetail {

        private String sheetName;

        private Integer rowNo;

        private String attributionStatus;

        private String workOrderCode;

        private String scheduleOrderCode;

        private String productCode;

        private String productName;

        private String workstationCode;

        private String workstationName;

        private String processCode;

        private String processName;

        private String feedbackUserCode;

        private String feedbackUserName;

        private String approverName;

        private BigDecimal feedbackQuantity;

        private BigDecimal beforeReportedQuantity;

        private BigDecimal afterReportedQuantity;

        private BigDecimal reportedQuantityDelta;

        private BigDecimal beforeProgressPercent;

        private BigDecimal afterProgressPercent;

        private BigDecimal progressDeltaPercent;

        private String feedbackCode;

        private String resultCode;

        private String resultMessage;

        private Long importRecordId;

        private String remark;
    }

    @Data
    public static class DirectWorkReportSkipWarning {

        private String sheetName;

        private Integer rowNo;

        private String workOrderCode;

        private String scheduleOrderCode;

        private String productCode;

        private String productName;

        private String processCode;

        private String processName;

        private String feedbackUserCode;

        private String feedbackUserName;

        private String approverName;

        private BigDecimal feedbackQuantity;

        private BigDecimal reportedQuantity;

        private BigDecimal remainingQuantity;

        private BigDecimal progressPercent;

        private String reasonCode;

        private String reason;
    }
}
