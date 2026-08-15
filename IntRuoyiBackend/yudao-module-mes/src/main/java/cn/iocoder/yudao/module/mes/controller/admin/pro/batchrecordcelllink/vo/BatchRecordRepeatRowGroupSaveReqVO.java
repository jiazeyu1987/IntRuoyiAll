package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordRepeatRowGroupSaveReqVO {

    private String scopeType;
    private Long scopeId;
    private Long routeId;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    @NotNull(message = "路线工序不能为空")
    private Long routeProcessId;
    @NotBlank(message = "目标表单不能为空")
    private String targetReportId;
    @NotNull(message = "模板开始行不能为空")
    private Integer templateStartRowIndex;
    @NotNull(message = "模板结束行不能为空")
    private Integer templateEndRowIndex;
    @NotNull(message = "重复区域开始行不能为空")
    private Integer repeatAreaStartRowIndex;
    @NotNull(message = "重复区域结束行不能为空")
    private Integer repeatAreaEndRowIndex;
    @Valid
    @NotEmpty(message = "重复记录不能为空")
    private List<BatchRecordRepeatRowGroupRecordSaveReqVO> records;
    @Valid
    @NotEmpty(message = "模板字段映射不能为空")
    private List<BatchRecordRepeatRowGroupMappingSaveReqVO> mappings;
    private Boolean enabled;
    private String remark;
}