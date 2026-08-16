package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderBatchRecordBackfillCommand {

    private MesProProcessPoolEventDO event;

    private MesProcessPoolReportAllocationDO allocation;

    private List<MesProProcessPoolEventDO> sourceEvents;

    private List<MesProcessPoolReportAllocationDO> allocations;

    private String aggregateHash;

    private String idempotencyKey;

    private MesProWorkOrderDO workOrder;

    private Long dccProjectCodeId;

    /** Current eDHR batch context. Both fields are required for release dossier writes. */
    private Long batchExecutionId;

    private Long batchExecutionTaskId;
}
