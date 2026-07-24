package cn.iocoder.yudao.module.srm.controller.admin.framework.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SrmFrameworkAgreementRespVO {

    private Long id;
    private String agreementNo;
    private Long frameworkPlanId;
    private String frameworkPlanNo;
    private Long supplierId;
    private String supplierName;
    private String procurementMethod;
    private String procurementMethodLabel;
    private BigDecimal budgetAmount;
    private LocalDate validStartDate;
    private LocalDate validEndDate;
    private String agreementStatus;
    private String agreementStatusLabel;
    private String remark;
    private List<Line> lines;

    @Data
    public static class Line {
        private Long id;
        private Long frameworkPlanLineId;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal budgetAmount;
    }
}
