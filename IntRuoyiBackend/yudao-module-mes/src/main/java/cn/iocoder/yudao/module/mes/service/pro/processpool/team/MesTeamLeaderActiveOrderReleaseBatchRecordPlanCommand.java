package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand {

    private Long tenantId;

    private Long activeOrderId;

    private List<Long> pickListBindingIds;

    private Long workOrderId;

    private Long routeId;

    private Long routeVersionId;

    private Long dccProjectCodeId;

    private Long productId;

    private String batchCode;

    private Long applicantUserId;

    private Long batchExecutionId;

    private MesProWorkOrderDO workOrder;

    private String sourceSnapshotHash;

    private List<ProcessSource> processSources;

    @Data
    @Accessors(chain = true)
    public static class ProcessSource {

        private MesProcessPoolActiveOrderProcessSnapshotDO snapshot;

        private MesProcessPoolOrderProcessCompletionDO completion;

        private List<MesProProcessPoolEventDO> sourceEvents;

        private List<MesProcessPoolReportAllocationDO> allocations;

        private List<MesProcessPoolSubmissionReviewDO> reviews;
    }
}
