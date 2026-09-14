package cn.iocoder.yudao.module.mes.productionrelease.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesReleaseFlowFailureRespVO {

    private String stage;
    private String currentStatus;
    private List<MesReleaseFlowBlocker> blockers;
}
