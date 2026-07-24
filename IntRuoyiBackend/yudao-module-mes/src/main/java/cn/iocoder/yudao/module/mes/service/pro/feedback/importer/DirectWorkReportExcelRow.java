package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DirectWorkReportExcelRow(
        String sheetName,
        int rowNo,
        String taskCode,
        String workOrderCode,
        String itemCode,
        String itemName,
        String processCode,
        String processName,
        String department,
        String feedbackUserCode,
        String feedbackUserName,
        String approverName,
        LocalDateTime feedbackTime,
        BigDecimal feedbackQuantity
) {
}
