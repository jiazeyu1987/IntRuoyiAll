package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseLossReportPlan {

    private MesTeamLeaderActiveOrderReleaseLossReportPlanCommand command;
    private List<PreparedLossReport> preparedReports;
    private List<Long> sourceObjectIds;
    private List<String> sourceValueHashes;
    private List<MesTeamLeaderActiveOrderReleaseSignatureEvidence> signatureEvidence;
    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;
    private List<ProcessLossDecision> processDecisions;
    private Boolean hasActualLoss;
    private BigDecimal lossQuantity;
    private String lossDecision;

    @Data
    @Accessors(chain = true)
    public static class ProcessLossDecision {

        private Long routeProcessId;
        private Long processId;
        private String decision;
        private Boolean hasActualLoss;
        private BigDecimal lossQuantity;
    }

    @Data
    @Accessors(chain = true)
    public static class PreparedLossReport {

        private List<MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource> sources;
        private MesProRouteFlowProcessBatchRecordDO binding;
        private List<MesProBatchRecordCellLinkRuleDO> rules;
        private MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPort.TargetResolution dynamicTarget;
        private Map<String, Object> mappedValues;
        private List<String> targetSnapshotHashes;
        private String evidenceHash;
    }
}
