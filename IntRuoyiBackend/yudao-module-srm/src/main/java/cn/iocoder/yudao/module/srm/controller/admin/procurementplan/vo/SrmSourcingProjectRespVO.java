package cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SrmSourcingProjectRespVO {

    private Long id;
    private String projectNo;
    private String projectTitle;
    private String projectType;
    private String projectTypeLabel;
    private String projectStatus;
    private String projectStatusLabel;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private BigDecimal expectedAmount;
    private List<Line> lines;

    @Data
    public static class Line {
        private Long id;
        private Long sourcePlanLineId;
        private String lineNo;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private String unit;
        private LocalDate requiredDate;
    }
}
