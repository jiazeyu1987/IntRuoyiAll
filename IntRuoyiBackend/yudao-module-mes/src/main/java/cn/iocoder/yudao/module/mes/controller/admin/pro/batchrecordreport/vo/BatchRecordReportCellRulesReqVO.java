package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordReportCellRulesReqVO {

    @NotBlank(message = "报表 ID 不能为空")
    private String reportId;

    @NotNull(message = "单元格规则不能为空")
    @Valid
    private List<BatchRecordReportCellRuleVO> rules;

    @Valid
    private List<BatchRecordReportAssistRowVO> assistRows;

    private Integer assistGridRowCount;

    private Integer assistGridColumnCount;
}
