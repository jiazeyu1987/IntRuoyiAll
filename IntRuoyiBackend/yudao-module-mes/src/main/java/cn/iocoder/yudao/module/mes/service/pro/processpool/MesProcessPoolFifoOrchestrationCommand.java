package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MesProcessPoolFifoOrchestrationCommand {

    private String allocationBatchNo;
    private Long sourceProcessId;
    private Long targetRouteProcessId;
    private Long targetProcessId;
    private List<Long> targetWorkOrderIds;

}
