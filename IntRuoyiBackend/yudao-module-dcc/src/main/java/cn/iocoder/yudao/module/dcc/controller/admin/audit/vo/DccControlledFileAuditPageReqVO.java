package cn.iocoder.yudao.module.dcc.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - DCC 受控文件审计分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class DccControlledFileAuditPageReqVO extends PageParam {

    @Schema(description = "访问事件码")
    private String accessEventCode;

    @Schema(description = "水印追踪码")
    private String watermarkTraceCode;

    @Schema(description = "受控文件编号")
    private Long controlledFileId;

    @Schema(description = "访问用户编号")
    private Long userId;

    @Schema(description = "操作类型")
    private String actionType;

    @Schema(description = "访问结果")
    private String result;

    @Schema(description = "失败码")
    private String failureCode;

    @Schema(description = "请求编号")
    private String requestId;

    @Schema(description = "发生时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] occurredAt;

}
