package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkPqcProcessVO {

    private Long id;
    private String processCode;
    private String processName;
    private Integer sort;
}
