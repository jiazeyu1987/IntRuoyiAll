package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
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

    private List<MesProcessPoolSubmissionReviewDO> reviews;

    private String aggregateHash;

    private String idempotencyKey;

    private MesProWorkOrderDO workOrder;

    private Long pickListBindingId;

    private Long dccProjectCodeId;

    /**
     * Frozen route-version production report binding resolved by the release writer.
     * When present, backfill must use this immutable snapshot instead of current route master data.
     */
    private MesProRouteFlowProcessBatchRecordDO routeBinding;

    /** Current eDHR batch context. Both fields are required for release dossier writes. */
    private Long batchExecutionId;

    private Long batchExecutionTaskId;

    /** Exact production source rules validated by the completion-source adapter. */
    private List<MesProBatchRecordCellLinkRuleDO> mappingRules;

    /** Frozen source provenance persisted in the existing field audit. */
    private String sourceAuditReasonText;

    /** Completion materialization must not overwrite an unsigned manual value. */
    private boolean requireEmptyTargets;
}
