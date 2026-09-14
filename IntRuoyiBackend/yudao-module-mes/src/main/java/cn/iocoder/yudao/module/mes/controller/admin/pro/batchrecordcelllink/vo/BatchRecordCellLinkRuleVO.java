package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkRuleVO {

    private Long id;
    private String scopeType;
    private Long scopeId;
    private Long routeId;
    private Long routeProcessId;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    private String sourceType;
    private String sourceReportId;
    private String sourceReportName;
    private Integer sourceRowIndex;
    private Integer sourceColumnIndex;
    private String sourceCellKey;
    private String sourceFieldCode;
    private String sourceFieldName;
    private String sourceLabel;
    private String sourceValueType;
    private String targetReportId;
    private String targetReportName;
    private Integer targetRowIndex;
    private Integer targetColumnIndex;
    private String targetCellKey;
    private String targetLabel;
    private String targetValueType;
    private String aggregationStrategy;
    private String overwritePolicy;
    private String templateSnapshotHash;
    private Long ruleVersion;
    private Boolean enabled;
    private String remark;
}
