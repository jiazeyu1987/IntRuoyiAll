package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 运行控制台备份点 Response VO")
@Data
public class RuntimeControlBackupPointRespVO {

    @Schema(description = "备份点编号")
    private String backupId;

    @Schema(description = "manifest 路径")
    private String manifestPath;

    @Schema(description = "checksum 路径")
    private String checksumPath;

    @Schema(description = "恢复演练报告路径")
    private String rehearsalReportPath;

    @Schema(description = "现场快照路径")
    private String snapshotPath;

    @Schema(description = "最近验证时间")
    private LocalDateTime lastVerifiedAt;

    @Schema(description = "备份点程序镜像标签")
    private String imageTag;

    @Schema(description = "备份模式")
    private String backupMode;

    @Schema(description = "保留最近备份点数量")
    private Integer retentionKeepLast;

    @Schema(description = "保留天数")
    private Integer retentionKeepDays;

    @Schema(description = "NAS 容量阈值百分比")
    private Integer retentionMaxNasUsedPercent;

    @Schema(description = "本次新增对象数量")
    private Integer objectAddedCount;

    @Schema(description = "本次修改对象数量")
    private Integer objectModifiedCount;

    @Schema(description = "本次删除对象数量")
    private Integer objectDeletedCount;

    @Schema(description = "本次复用对象数量")
    private Integer objectReusedCount;

    @Schema(description = "可恢复状态：RECOVERABLE/UNRECOVERABLE")
    private String recoverabilityStatus;

    @Schema(description = "DCC 备份模式：baseline/incremental")
    private String dccBackupMode;

    @Schema(description = "DCC 增量链状态")
    private String dccChainStatus;

    @Schema(description = "DCC 当前段增删改作废统计")
    private Map<String, String> dccChangeSummary;

    @Schema(description = "恢复演练状态")
    private String rehearsalStatus;

    @Schema(description = "不可恢复原因")
    private List<String> unrecoverableReasons;
}
