package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCompletionResult {

    private Long activeOrderId;
    private Long completionReceiptId;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private String receiptHash;
    private String flow6ReceiptStatus;
    private Integer activeOrderVersion;
    private String batchRecordStatus;
    private String processInspectionStatus;
    private String lossReportStatus;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private String provisionHandoff;
}
