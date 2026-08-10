package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordCellLinkRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseProcessInspectionPlan {

    private MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command;

    private List<PreparedInspection> preparedInspections;

    private List<Long> sourceObjectIds;

    private List<String> sourceValueHashes;

    private List<MesTeamLeaderActiveOrderReleaseProcessInspectionWriteResult.SignatureEvidence> signatureEvidence;

    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;

    @Data
    @Accessors(chain = true)
    public static class PreparedInspection {

        private MesTeamLeaderActiveOrderReleaseProcessInspectionReader.InspectionSource source;

        private MesProRouteFlowProcessBatchRecordDO binding;

        private List<MappedValue> mappedValues;

        private String evidenceHash;
    }

    @Data
    @Accessors(chain = true)
    public static class MappedValue {

        private MesProBatchRecordCellLinkRuleDO rule;

        private Object value;

        private String displayValue;
    }
}
