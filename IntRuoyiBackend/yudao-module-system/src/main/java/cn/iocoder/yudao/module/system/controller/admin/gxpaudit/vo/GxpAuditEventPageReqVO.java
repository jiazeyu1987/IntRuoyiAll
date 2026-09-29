package cn.iocoder.yudao.module.system.controller.admin.gxpaudit.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - GxP 审计事件分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class GxpAuditEventPageReqVO extends PageParam {

    @Schema(description = "查询范围类型", example = "ACTIVE_ORDER")
    private String scopeType;
    @Schema(description = "查询范围编号", example = "100")
    private Long scopeId;
    @Schema(description = "业务域", example = "MES")
    private String domain;
    @Schema(description = "对象类型", example = "MES_ACTIVE_ORDER")
    private String subjectType;
    @Schema(description = "对象编号", example = "ACTIVE_ORDER:100")
    private String subjectId;
    @Schema(description = "操作编号", example = "mes.active-order.complete")
    private String operationId;
    @Schema(description = "动作", example = "COMPLETE")
    private String action;
    @Schema(description = "结果状态", example = "SUCCESS")
    private String resultStatus;
    @Schema(description = "操作人编号", example = "1")
    private Long actorId;
    @Schema(description = "是否存在电子签名")
    private Boolean signaturePresent;
    @Schema(description = "服务器审计时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] occurredAt;
}
