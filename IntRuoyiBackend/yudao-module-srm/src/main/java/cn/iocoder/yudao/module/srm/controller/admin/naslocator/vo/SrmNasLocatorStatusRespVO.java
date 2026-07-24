package cn.iocoder.yudao.module.srm.controller.admin.naslocator.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - SRM NAS定位状态摘要 Response VO")
@Data
public class SrmNasLocatorStatusRespVO {

    @Schema(description = "受保护共享 UNC 路径", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "\\\\172.30.30.4\\质量体系文件；\\\\172.30.30.4\\生产部")
    private String scopeShare;

    @Schema(description = "刷新起始相对路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "")
    private String rootPath;

    @Schema(description = "最新任务状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUCCESS")
    private String latestTaskStatus;

    @Schema(description = "最近一次成功刷新完成时间", example = "1710000000000")
    private Long latestSuccessTime;

    @Schema(description = "最近一次成功快照文件数", example = "128")
    private Long fileCount;

    @Schema(description = "最近一次成功快照目录数", example = "64")
    private Long directoryCount;

    @Schema(description = "状态消息或失败原因", example = "最近一次刷新成功")
    private String message;

    @Schema(description = "运行中当前共享", example = "质量体系文件")
    private String runningShare;

    @Schema(description = "运行中当前目录路径", example = "质量体系文件/1. QMS documents")
    private String runningPath;

    @Schema(description = "运行中已扫描目录数", example = "12")
    private Long runningDirectoryCount;

    @Schema(description = "运行中已扫描文件数", example = "128")
    private Long runningFileCount;

    @Schema(description = "运行中当前共享序号，从 1 开始", example = "1")
    private Integer runningShareIndex;

    @Schema(description = "运行中共享总数", example = "2")
    private Integer runningShareTotal;
}
