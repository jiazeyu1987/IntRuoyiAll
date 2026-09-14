package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseBatchRecordPlan {

    private MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand command;

    private List<PreparedProcess> preparedProcesses;

    private List<Long> sourceObjectIds;

    private List<String> sourceValueHashes;

    private List<MesTeamLeaderActiveOrderReleaseBatchRecordWriteResult.SignatureEvidence> signatureEvidence;

    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;

    @Data
    @Accessors(chain = true)
    public static class PreparedProcess {

        private MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.ProcessSource source;

        private MesProRouteFlowProcessBatchRecordDO binding;

        private List<MesProBatchRecordCellLinkRuleDO> rules;
    }
}
