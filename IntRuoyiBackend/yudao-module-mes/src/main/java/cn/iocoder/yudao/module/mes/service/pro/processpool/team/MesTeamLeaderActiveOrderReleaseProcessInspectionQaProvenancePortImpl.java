package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import org.springframework.stereotype.Service;

@Service
public class MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImpl
        implements MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort {

    @Override
    public Resolution verify(DccProjectCodeDO dccProject,
                             MesQaInspectionRegulationDO regulation,
                             MesQaInspectionRegulationVersionDO version) {
        Long versionId = version == null ? null : version.getId();
        return new Resolution()
                .setBlockerType("PQC_DCC_QA_PROVENANCE_REQUIRED")
                .setBlockerMessage("QA 版本缺少显式 DCC 项目来源关系，regulationVersionId=" + versionId);
    }
}
