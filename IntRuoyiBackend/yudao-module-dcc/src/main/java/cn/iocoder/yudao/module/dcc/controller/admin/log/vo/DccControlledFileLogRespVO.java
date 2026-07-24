package cn.iocoder.yudao.module.dcc.controller.admin.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 文控日志 Response VO")
@Data
public class DccControlledFileLogRespVO {

    @Schema(description = "统一日志编号")
    private String id;

    @Schema(description = "日志类型")
    private String logType;

    @Schema(description = "来源记录编号")
    private Long sourceRecordId;

    @Schema(description = "发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "动作显示")
    private String actionLabel;

    @Schema(description = "结果显示")
    private String resultLabel;

    @Schema(description = "文件编号")
    private String fileNumber;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "操作人编号")
    private Long operatorUserId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "关联对象")
    private String relatedObject;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "旧值")
    private String oldValueText;

    @Schema(description = "新值")
    private String newValueText;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "详情 JSON")
    private String detailJson;

}
