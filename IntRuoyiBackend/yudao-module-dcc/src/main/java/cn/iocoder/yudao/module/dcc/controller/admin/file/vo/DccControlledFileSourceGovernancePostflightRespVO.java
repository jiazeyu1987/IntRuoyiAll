package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceGovernancePostflightFinding;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceGovernancePostflightReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - DCC source governance postflight report")
@Data
@Builder
public class DccControlledFileSourceGovernancePostflightRespVO {

    private String taskKey;
    private Integer checkedCount;
    private Integer validCount;
    private List<Finding> findings;

    public static DccControlledFileSourceGovernancePostflightRespVO from(
            DccControlledFileSourceGovernancePostflightReport report) {
        return builder().taskKey(report.taskKey()).checkedCount(report.checkedCount())
                .validCount(report.validCount())
                .findings(report.findings().stream().map(Finding::from).toList()).build();
    }

    @Data
    @Builder
    public static class Finding {
        private Long itemId;
        private Long controlledFileId;
        private String status;
        private String reasonCode;
        private String detail;

        static Finding from(DccControlledFileSourceGovernancePostflightFinding finding) {
            return builder().itemId(finding.itemId()).controlledFileId(finding.controlledFileId())
                    .status(finding.status()).reasonCode(finding.reasonCode()).detail(finding.detail()).build();
        }
    }
}
