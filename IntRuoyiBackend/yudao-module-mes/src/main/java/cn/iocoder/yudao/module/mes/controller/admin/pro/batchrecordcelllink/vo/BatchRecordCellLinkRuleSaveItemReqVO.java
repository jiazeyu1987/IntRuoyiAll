package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkRuleSaveItemReqVO {

    private String sourceType;
    @NotBlank(message = "源表单不能为空")
    private String sourceReportId;
    @NotNull(message = "源单元格行号不能为空")
    private Integer sourceRowIndex;
    @NotNull(message = "源单元格列号不能为空")
    private Integer sourceColumnIndex;
    private String sourceCellKey;
    private String sourceFieldCode;
    private String sourceFieldName;
    private String sourceLabel;
    @NotBlank(message = "目标表单不能为空")
    private String targetReportId;
    @NotNull(message = "目标单元格行号不能为空")
    private Integer targetRowIndex;
    @NotNull(message = "目标单元格列号不能为空")
    private Integer targetColumnIndex;
    private String targetLabel;
    private String overwritePolicy;
    private Boolean enabled;
    private String remark;
}
