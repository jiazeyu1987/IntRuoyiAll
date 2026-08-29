package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkRouteProcessVO {

    private Long id;
    private Long processId;
    private String processCode;
    private String processName;
    private Integer sort;
    private String batchRecordReportId;
}
