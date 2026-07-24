package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlSiteMessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 运行控制台告警 Response VO")
@Data
public class RuntimeControlAlertRespVO {

    @Schema(description = "告警编号")
    private Long id;

    @Schema(description = "环境", example = "prod")
    private String environment;

    @Schema(description = "动作或异常类型", example = "backup-failed")
    private String action;

    @Schema(description = "严重级别", example = "WARN")
    private String severity;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "站内信模板编码")
    private String notifyTemplateCode;

    @Schema(description = "模板参数")
    private Map<String, Object> templateParams;

    @Schema(description = "站内信发送状态")
    private RuntimeControlSiteMessageStatus siteMessageStatus;

    @Schema(description = "站内信消息编号")
    private Long notifyMessageId;

    @Schema(description = "站内信失败原因")
    private String siteMessageFailureReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "发送时间")
    private LocalDateTime sentAt;

    @Schema(description = "确认人")
    private String acknowledgedBy;

    @Schema(description = "确认时间")
    private LocalDateTime acknowledgedAt;
}
