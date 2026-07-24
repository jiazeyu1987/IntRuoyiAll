package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 生产用料清单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesKingdeeProductionMaterialListPageReqVO extends PageParam {

    @Schema(description = "ERP 生产用料清单单据编号", example = "PPBOM003088")
    private String sourceBillNo;

    @Schema(description = "产品编码", example = "AW.106.03.08.10")
    private String productCode;

    @Schema(description = "生产订单编号", example = "CODXMO20260")
    private String productionOrderNo;

    @Schema(description = "子项物料编码", example = "A001.02.014.300")
    private String childMaterialCode;

    @Schema(description = "子项物料名称", example = "造影导管软端")
    private String childMaterialName;

    @Schema(description = "ERP 来源修改时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] sourceModifyTime;

    @Schema(description = "最后同步时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] lastSyncTime;

}
