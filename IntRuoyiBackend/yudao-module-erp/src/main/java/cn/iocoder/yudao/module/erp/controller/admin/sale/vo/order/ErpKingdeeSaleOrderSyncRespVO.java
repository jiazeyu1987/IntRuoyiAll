package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP Kingdee 销售订单同步 Response VO")
@Data
public class ErpKingdeeSaleOrderSyncRespVO {

    private Integer createdCount;

    private Integer skippedCount;

    private List<Long> createdSaleOrderIds;

    private List<String> skippedSourceFids;

}
