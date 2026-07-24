package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运行控制台远程根分区临时目录清理 Response VO")
@Data
public class RuntimeControlRemoteRootCleanupRespVO {

    @Schema(description = "目标环境")
    private String targetEnvironment;

    @Schema(description = "服务器 IP")
    private String serverHost;

    @Schema(description = "清理目录")
    private List<String> cleanupPaths;

    @Schema(description = "清理前状态")
    private RuntimeControlRemoteRootDiskStatusRespVO before;

    @Schema(description = "清理后状态")
    private RuntimeControlRemoteRootDiskStatusRespVO after;

    @Schema(description = "删除的顶层条目数量")
    private Integer deletedEntryCount;

    @Schema(description = "操作人")
    private String requestedBy;

    @Schema(description = "清理原因")
    private String reason;

    @Schema(description = "清理时间")
    private LocalDateTime cleanedAt;
}
