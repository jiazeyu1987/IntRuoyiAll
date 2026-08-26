package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkWorkbenchContextRespVO {

    private String scopeType;
    private Long scopeId;
    private Long routeId;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    private List<BatchRecordCellLinkFormRespVO> forms;

    private List<BatchRecordCellLinkPqcProcessVO> pqcProcesses;
    private List<BatchRecordCellLinkSourceFieldVO> sourceFields;
    private String defaultSourceReportId;
    private String defaultTargetReportId;
    private List<BatchRecordCellLinkRuleVO> rules;
    private List<BatchRecordRepeatRowGroupVO> repeatRowGroups;
}
