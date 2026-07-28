package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordReportCellRulesRespVO {

    private String reportId;

    private String sheetLayoutJson;

    private List<BatchRecordReportCellRuleVO> rules;

    private List<BatchRecordReportCellRuleVO> suggestions;

    private Integer unreviewedFillableCellCount;

    private List<BatchRecordReportAssistRowVO> assistRows;
}
