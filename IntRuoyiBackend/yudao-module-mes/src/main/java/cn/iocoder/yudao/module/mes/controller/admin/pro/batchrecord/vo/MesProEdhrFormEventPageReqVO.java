package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES eDHR 独立表单事件分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrFormEventPageReqVO extends PageParam {

    @Schema(description = "实例 ID")
    private Long instanceId;

    @Schema(description = "模板 ID")
    private Long templateId;

    @Schema(description = "实例编码")
    private String instanceCode;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "事件结果")
    private String resultStatus;

    @Schema(description = "发生时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] occurredAt;
}
