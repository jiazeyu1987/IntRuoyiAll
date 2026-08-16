package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProductionReleaseBatchExecutionCommand {

    private Long applicationId;
    private Long workOrderId;
    private String batchCode;
    private Long routeId;
    private Long routeVersionId;
}
