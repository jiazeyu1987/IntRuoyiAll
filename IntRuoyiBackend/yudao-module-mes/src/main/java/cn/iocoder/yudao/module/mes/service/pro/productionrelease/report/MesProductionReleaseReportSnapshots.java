package cn.iocoder.yudao.module.mes.service.pro.productionrelease.report;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MesProductionReleaseReportSnapshots {

    private MesProductionReleaseReportSnapshots() {
    }

    public static String hash(
            MesProcessPoolActiveOrderReleaseApplicationDO application,
            List<MesProductionReleaseReportNodeEvidence> evidences) {
        if (application == null || application.getId() == null || application.getBatchExecutionId() == null
                || evidences == null) {
            throw new IllegalArgumentException("release application and report evidences are required");
        }
        List<String> parts = new ArrayList<>();
        parts.add(String.valueOf(application.getId()));
        parts.add(String.valueOf(application.getBatchExecutionId()));
        evidences.stream()
                .sorted(Comparator.comparing(MesProductionReleaseReportNodeEvidence::getNodeType))
                .forEach(evidence -> {
                    parts.add(evidence.getNodeType());
                    parts.add(String.valueOf(evidence.getBatchTaskId()));
                    parts.add(evidence.getSterilizationBatchNo());
                    parts.add(String.valueOf(evidence.getActiveAttachmentVersion()));
                    parts.add(String.valueOf(evidence.getAttachmentIds()));
                    parts.add(String.valueOf(evidence.getAttachmentHashes()));
                });
        return MesReleaseFlowIdempotency.payloadHash(parts.toArray(String[]::new));
    }
}
