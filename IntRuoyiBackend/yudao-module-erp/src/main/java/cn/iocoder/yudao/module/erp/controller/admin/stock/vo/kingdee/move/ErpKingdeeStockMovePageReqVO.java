package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 金蝶调拨单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpKingdeeStockMovePageReqVO extends PageParam {

    @Schema(description = "调拨单号")
    private String sourceBillNo;

    @Schema(description = "单据状态")
    private String documentStatus;

    @Schema(description = "调拨方向")
    private String transferDirect;

    @Schema(description = "单据日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] billDate;

}
