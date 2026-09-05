package cn.iocoder.yudao.module.erp.controller.admin.production.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 生产补料单同步 Response VO")
@Data
public class ErpProductionReplenishmentListSyncRespVO {

    @Schema(description = "新增数量")
    private Integer createdCount;

    @Schema(description = "更新数量")
    private Integer updatedCount;

}
