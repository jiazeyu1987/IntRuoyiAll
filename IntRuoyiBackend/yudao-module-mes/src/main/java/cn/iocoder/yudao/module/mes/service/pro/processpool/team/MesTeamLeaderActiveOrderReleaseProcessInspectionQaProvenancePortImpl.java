package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
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
        String projectCode = dccProject == null ? null : StrUtil.trim(dccProject.getProjectCode());
        String regulationCode = regulation == null ? null : StrUtil.trim(regulation.getRegulationCode());
        Long versionId = version == null ? null : version.getId();
        boolean lockedIdentity = dccProject != null && dccProject.getId() != null
                && StrUtil.isNotBlank(projectCode)
                && regulation != null && regulation.getId() != null
                && StrUtil.isNotBlank(regulationCode)
                && java.util.Objects.equals(dccProject.getId(), regulation.getDccProjectCodeId())
                && "PUBLISHED".equals(regulation.getLifecycleStatus())
                && version != null && versionId != null
                && java.util.Set.of("PUBLISHED", "RETIRED").contains(version.getLifecycleStatus())
                && version.getPublishedAt() != null
                && java.util.Objects.equals(regulation.getId(), version.getRegulationId())
                && java.util.Objects.equals(dccProject.getTenantId(), regulation.getTenantId())
                && java.util.Objects.equals(regulation.getTenantId(), version.getTenantId());
        if (lockedIdentity) {
            return new Resolution()
                    .setDccProjectCodeId(dccProject.getId())
                    .setRegulationId(regulation.getId())
                    .setRegulationVersionId(versionId)
                    .setProvenanceType("DCC_QA_PROJECT_RELATION")
                    .setProvenanceId(dccProject.getId() + ":" + regulation.getId())
                    .setProvenanceSnapshotHash(hash("DCC_QA_PROJECT_RELATION_V2",
                            dccProject.getTenantId(), dccProject.getId(), projectCode,
                            regulation.getTenantId(), regulation.getId(), regulation.getDccProjectCodeId(),
                            regulationCode,
                            version.getTenantId(), versionId, version.getRegulationId(), version.getVersionNo(),
                            version.getLifecycleStatus(), version.getPublishedAt(), version.getSnapshotJson()));
        }
        return new Resolution()
                .setBlockerType("PQC_DCC_QA_PROVENANCE_REQUIRED")
                .setBlockerMessage("QA 规程未直接归属当前 DCC 项目，dccProjectCodeId="
                        + (dccProject == null ? null : dccProject.getId())
                        + "，regulationDccProjectCodeId="
                        + (regulation == null ? null : regulation.getDccProjectCodeId())
                        + "，regulationVersionId=" + versionId);
    }

    private String hash(Object... values) {
        StringBuilder canonical = new StringBuilder();
        for (Object value : values) {
            String text = String.valueOf(value);
            canonical.append(text.length()).append(':').append(text);
        }
        return DigestUtil.sha256Hex(canonical.toString());
    }
}
