package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

public interface MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePort {

    Resolution verify(DccProjectCodeDO dccProject,
                      MesQaInspectionRegulationDO regulation,
                      MesQaInspectionRegulationVersionDO version);

    @Data
    @Accessors(chain = true)
    class Resolution {

        private Long dccProjectCodeId;

        private Long regulationId;

        private Long regulationVersionId;

        private String provenanceType;

        private String provenanceId;

        private String provenanceSnapshotHash;

        private String blockerType;

        private String blockerMessage;

        public boolean isVerifiedFor(DccProjectCodeDO project,
                                     MesQaInspectionRegulationDO regulation,
                                     MesQaInspectionRegulationVersionDO version) {
            return project != null && regulation != null && version != null
                    && Objects.equals(project.getId(), dccProjectCodeId)
                    && Objects.equals(regulation.getId(), regulationId)
                    && Objects.equals(version.getId(), regulationVersionId)
                    && StrUtil.isNotBlank(provenanceType)
                    && StrUtil.isNotBlank(provenanceId)
                    && StrUtil.isNotBlank(provenanceSnapshotHash)
                    && StrUtil.isBlank(blockerType);
        }
    }
}
