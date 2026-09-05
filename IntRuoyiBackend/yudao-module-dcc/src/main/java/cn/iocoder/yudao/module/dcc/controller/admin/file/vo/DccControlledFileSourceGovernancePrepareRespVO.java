package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileSourceGovernancePreparationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "Admin - Frozen DCC source governance manifest summary")
@Data
@Builder
public class DccControlledFileSourceGovernancePrepareRespVO {

    private String taskKey;
    private String batchStatus;
    private String ruleVersion;
    private String schemaVersion;
    private String manifestSha256;
    private String requestSha256;
    private Long snapshotMaxControlledFileId;
    private Long startAfterControlledFileId;
    private Long lastControlledFileId;
    private Integer totalCount;
    private Integer readyCount;
    private Integer blockedCount;

    public static DccControlledFileSourceGovernancePrepareRespVO from(
            DccControlledFileSourceGovernancePreparationResult result) {
        return builder().taskKey(result.taskKey()).batchStatus(result.batchStatus())
                .ruleVersion(result.ruleVersion()).schemaVersion(result.schemaVersion())
                .manifestSha256(result.manifestSha256()).requestSha256(result.requestSha256())
                .snapshotMaxControlledFileId(result.snapshotMaxControlledFileId())
                .startAfterControlledFileId(result.startAfterControlledFileId())
                .lastControlledFileId(result.lastControlledFileId())
                .totalCount(result.totalCount()).readyCount(result.readyCount())
                .blockedCount(result.blockedCount()).build();
    }
}
