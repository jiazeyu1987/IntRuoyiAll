package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES eDHR 记录本分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrRecordbookPageReqVO extends PageParam {

    @Schema(description = "记录本编码")
    private String recordbookCode;

    @Schema(description = "记录本名称")
    private String recordbookName;

    @Schema(description = "记录本类型")
    private String recordbookType;

    @Schema(description = "记录本状态")
    private String status;

    @Schema(description = "责任人")
    private Long ownerUserId;

    @Schema(description = "业务对象编码")
    private String businessObjectCode;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
