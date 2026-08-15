package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkSourceFieldVO {

    private String sourceType;
    private String fieldCode;
    private String fieldName;
    private String valueType;
    private Long routeProcessId;
}
