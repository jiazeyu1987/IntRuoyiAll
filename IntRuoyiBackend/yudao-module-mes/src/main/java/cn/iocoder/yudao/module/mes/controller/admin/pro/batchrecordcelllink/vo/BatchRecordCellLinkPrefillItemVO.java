package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkPrefillItemVO {

    private String targetCellKey;
    private Integer targetRowIndex;
    private Integer targetColumnIndex;
    private Object value;
    private Long sourceExecutionId;
    private String sourceReportId;
    private String sourceReportName;
    private String sourceCellKey;
    private String sourceLabel;
    private Long ruleId;
    private Long ruleVersion;
    private String overwritePolicy;
    private String status;
}
