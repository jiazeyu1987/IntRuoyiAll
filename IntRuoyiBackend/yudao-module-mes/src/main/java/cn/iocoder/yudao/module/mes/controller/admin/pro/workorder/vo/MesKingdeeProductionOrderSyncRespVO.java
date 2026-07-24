package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES Kingdee 生产工单同步 Response VO")
@Data
public class MesKingdeeProductionOrderSyncRespVO {

    private Integer createdCount;
    private Integer updatedCount;

    private Integer skippedCount;

    private List<Long> createdWorkOrderIds;
    private List<Long> updatedWorkOrderIds;

    private List<String> skippedSourceKeys;

}
