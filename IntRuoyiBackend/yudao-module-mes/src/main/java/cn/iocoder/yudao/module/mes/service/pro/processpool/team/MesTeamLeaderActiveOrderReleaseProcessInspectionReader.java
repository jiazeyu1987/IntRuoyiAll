package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemEquipmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

public interface MesTeamLeaderActiveOrderReleaseProcessInspectionReader {

    SourceBundle read(MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand command);

    @Data
    @Accessors(chain = true)
    class SourceBundle {

        private List<InspectionSource> sources;
    }

    @Data
    @Accessors(chain = true)
    class InspectionSource {

        private MesPqcInspectionTaskDO task;

        private List<MesPqcProcessInspectionAggregateDetailDO> aggregateDetails;

        private MesProProcessPoolEventDO event;

        private MesProProcessPoolPqcRecordDO pqcRecord;

        private MesProcessPoolSubmissionReviewDO review;

        private MesQaInspectionRegulationDO regulation;

        private MesQaInspectionRegulationVersionDO regulationVersion;

        private List<MesQaInspectionRegulationItemDO> regulationItems;

        private List<MesQaInspectionRegulationItemEquipmentDO> regulationItemEquipment;

        private DccProjectCodeDO dccProject;

        private MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort.Resolution qaDccProvenance;
    }
}
