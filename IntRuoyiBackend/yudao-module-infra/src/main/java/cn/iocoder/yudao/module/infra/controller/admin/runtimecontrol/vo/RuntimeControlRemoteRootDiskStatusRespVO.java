package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运行控制台远程根分区状态 Response VO")
@Data
public class RuntimeControlRemoteRootDiskStatusRespVO {

    @Schema(description = "目标环境")
    private String targetEnvironment;

    @Schema(description = "服务器 IP")
    private String serverHost;

    @Schema(description = "挂载点")
    private String mountPoint;

    @Schema(description = "文件系统")
    private String filesystem;

    @Schema(description = "总容量字节数")
    private Long totalBytes;

    @Schema(description = "已用容量字节数")
    private Long usedBytes;

    @Schema(description = "可用容量字节数")
    private Long availableBytes;

    @Schema(description = "已用百分比")
    private Double usagePercent;

    @Schema(description = "inode 总数")
    private Long inodeTotal;

    @Schema(description = "inode 已用数")
    private Long inodeUsed;

    @Schema(description = "inode 可用数")
    private Long inodeAvailable;

    @Schema(description = "inode 已用百分比")
    private Double inodeUsagePercent;

    @Schema(description = "备份临时目录大小字节数")
    private Long backupTempBytes;

    @Schema(description = "/tmp 大小字节数")
    private Long tmpBytes;

    @Schema(description = "采样时间")
    private LocalDateTime sampledAt;
}
