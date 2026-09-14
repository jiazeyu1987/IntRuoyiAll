package cn.iocoder.yudao.module.dcc.service.file;

import java.util.List;

public record DccPublicationImpactRevisionOptions(
        List<DccPublicationImpactRevisionOption> sourceIterations,
        DccPublicationImpactRevisionOption openMajorRevision) {
}
