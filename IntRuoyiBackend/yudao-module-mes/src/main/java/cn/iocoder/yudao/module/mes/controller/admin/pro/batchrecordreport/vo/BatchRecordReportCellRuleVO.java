package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class BatchRecordReportCellRuleVO {

    @NotNull(message = "rowIndex 不能为空")
    private Integer rowIndex;

    @NotNull(message = "columnIndex 不能为空")
    private Integer columnIndex;

    private String valueType;

    private String componentFlag;

    private Boolean required;

    private String label;

    private String placeholder;

    private String helpText;

    private Map<String, Object> constraints;

    private Map<String, Object> attachmentRule;

    private String unit;

    private String source;

    private Double confidence;

    private Boolean reviewed;
}
