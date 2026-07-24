package cn.iocoder.yudao.module.srm.controller.admin.framework.vo;

import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanRespVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SrmFrameworkPlanRespVO {

    private Long id;
    private String frameworkPlanNo;
    private String planTitle;
    private Long supplierId;
    private String supplierName;
    private String procurementMethod;
    private String procurementMethodLabel;
    private BigDecimal budgetAmount;
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    private String planStatus;
    private String planStatusLabel;
    private String remark;
    private String submittedName;
    private LocalDateTime submittedTime;
    private String auditName;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Long agreementId;
    private String agreementNo;
    private LocalDateTime agreementTime;
    private LocalDateTime createTime;
    private List<Line> lines;
    private List<SrmProcurementPlanRespVO.ApprovalRecord> approvalRecords;

    @Data
    public static class Line {
        private Long id;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal budgetAmount;
    }
}
