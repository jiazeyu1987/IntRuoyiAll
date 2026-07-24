package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrRehearsalReadinessCommand {

    private Long routeId;

    private Long executorUserId;

    private Long approverUserId;

    private Long archiverUserId;
}
