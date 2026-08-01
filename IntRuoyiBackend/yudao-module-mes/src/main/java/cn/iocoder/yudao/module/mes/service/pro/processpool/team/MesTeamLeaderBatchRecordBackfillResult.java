package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderBatchRecordBackfillResult {

    private Long executionId;

    private Integer appliedFieldCount;
}
