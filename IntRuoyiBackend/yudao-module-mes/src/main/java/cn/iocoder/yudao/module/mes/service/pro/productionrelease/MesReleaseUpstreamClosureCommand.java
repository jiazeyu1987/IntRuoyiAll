package cn.iocoder.yudao.module.mes.service.pro.productionrelease;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesReleaseUpstreamClosureCommand {

    private Long releaseDecisionId;
    private Long activeOrderId;
    private Integer activeOrderExpectedVersion;
    private Long workOrderId;
    private Long actorUserId;
}
