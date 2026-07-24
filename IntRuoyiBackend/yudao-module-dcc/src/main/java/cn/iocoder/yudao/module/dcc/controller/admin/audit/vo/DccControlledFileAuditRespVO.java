package cn.iocoder.yudao.module.dcc.controller.admin.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 受控文件审计 Response VO")
@Data
public class DccControlledFileAuditRespVO {

    @Schema(description = "审计日志编号")
    private Long id;

    @Schema(description = "访问事件编号")
    private Long accessEventId;

    @Schema(description = "访问事件码")
    private String accessEventCode;

    @Schema(description = "水印追踪码")
    private String watermarkTraceCode;

    @Schema(description = "受控文件编号")
    private Long controlledFileId;

    @Schema(description = "文件编号")
    private String fileNumber;

    @Schema(description = "文件版本")
    private String fileVersionNo;

    @Schema(description = "访问用户编号")
    private Long userId;

    @Schema(description = "用户标识")
    private String userIdentifier;

    @Schema(description = "用户显示名")
    private String userDisplayName;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "操作类型")
    private String actionType;

    @Schema(description = "访问目的")
    private String purpose;

    @Schema(description = "访问结果")
    private String result;

    @Schema(description = "失败码")
    private String failureCode;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "来源 IP")
    private String sourceIp;

    @Schema(description = "请求编号")
    private String requestId;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "隐私模式")
    private String privacyMode;

    @Schema(description = "水印载荷")
    private String watermarkPayloadJson;

    @Schema(description = "发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "水印签发时间")
    private LocalDateTime issuedAt;

    @Schema(description = "水印过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "日志创建时间")
    private LocalDateTime createTime;

}
