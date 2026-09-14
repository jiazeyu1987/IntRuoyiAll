package cn.iocoder.yudao.module.mes.service.pro.productionrelease;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesReleaseUpstreamClosureResult {

    private Long releaseDecisionId;
    private Long activeOrderId;
    private String activeOrderStatus;
    private Long workOrderId;
    private String workOrderStatus;
    private String pickListStatus;
}
