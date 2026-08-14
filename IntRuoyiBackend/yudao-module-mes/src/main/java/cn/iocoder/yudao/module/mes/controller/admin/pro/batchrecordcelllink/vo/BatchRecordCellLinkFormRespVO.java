package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkFormRespVO {

    private Long id;
    private String batchRecordName;
    private String formSlotType;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    private Long routeProcessId;
    private Integer sourceTableIndex;
    private String tableTitle;
    private String reportId;
    private String reportCode;
    private String reportName;
}
