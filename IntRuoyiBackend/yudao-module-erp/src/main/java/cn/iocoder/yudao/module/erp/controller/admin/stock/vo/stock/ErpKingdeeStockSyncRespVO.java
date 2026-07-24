package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP Kingdee 即时库存同步 Response VO")
@Data
public class ErpKingdeeStockSyncRespVO {

    private Integer syncedCount;

}
