package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长活跃订单放行资料摘要 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseDossierSummaryRespVO {

    @Schema(description = "正式批记录表单数", example = "3")
    private Integer batchRecordCount;

    @Schema(description = "正式过程检验单数", example = "1")
    private Integer processInspectionFormCount;

    @Schema(description = "正式损耗单数", example = "0")
    private Integer lossReportFormCount;

    @Schema(description = "签字证据数", example = "0")
    private Integer signatureEvidenceCount;

    @Schema(description = "来源快照哈希")
    private String sourceSnapshotHash;
}
