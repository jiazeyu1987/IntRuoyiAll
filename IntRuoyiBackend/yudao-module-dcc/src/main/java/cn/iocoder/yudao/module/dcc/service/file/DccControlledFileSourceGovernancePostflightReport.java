package cn.iocoder.yudao.module.dcc.service.file;

import java.util.List;

public record DccControlledFileSourceGovernancePostflightReport(
        String taskKey, int checkedCount, int validCount,
        List<DccControlledFileSourceGovernancePostflightFinding> findings) {
}
