package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrProductionReleaseBatchCommand {

    private Long applicationId;
    private Long workOrderId;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
    private String activeContextKey;
    private String remark;
}
