package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ThirdPartyFeedbackExcelRow(
        String sheetName,
        int rowNo,
        LocalDateTime feedbackTime,
        String feedbackUserCode,
        String feedbackUserName,
        String approverName,
        String workOrderCode,
        String resourceGroup,
        String resourceName,
        String taskCode,
        String itemCode,
        String itemName,
        String specification,
        String moldCode,
        String processCode,
        String processName,
        String department,
        BigDecimal feedbackQuantity
) {
}
