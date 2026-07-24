package cn.iocoder.yudao.module.mes.service.pro.feedback.importer;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ThirdPartyFeedbackImportPayload {

    private String sheetName;
    private Integer rowNo;
    private LocalDateTime feedbackTime;
    private String feedbackUserCode;
    private String feedbackUserName;
    private String approverName;
    private String workOrderCode;
    private String resourceGroup;
    private String resourceName;
    private String taskCode;
    private String itemCode;
    private String itemName;
    private String specification;
    private String moldCode;
    private String processCode;
    private String processName;
    private String department;
    private BigDecimal feedbackQuantity;
}
