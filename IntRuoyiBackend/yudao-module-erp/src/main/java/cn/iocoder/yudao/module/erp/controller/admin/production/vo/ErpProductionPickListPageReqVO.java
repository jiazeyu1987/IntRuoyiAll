package cn.iocoder.yudao.module.erp.controller.admin.production.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 生产领料单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpProductionPickListPageReqVO extends PageParam {

    @Schema(description = "生产领料单号")
    private String sourceBillNo;

    @Schema(description = "单据状态")
    private String documentStatus;

    @Schema(description = "生产订单编号，按领料明细匹配")
    private String productionOrderNo;

    @Schema(description = "库存组织")
    private String stockOrgName;

    @Schema(description = "生产组织")
    private String productionOrgName;

    @Schema(description = "单据日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] billDate;

}
