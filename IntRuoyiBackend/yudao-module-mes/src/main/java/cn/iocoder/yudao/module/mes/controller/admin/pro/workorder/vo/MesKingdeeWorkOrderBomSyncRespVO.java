package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 工单 ERP BOM 同步 Response VO")
@Data
public class MesKingdeeWorkOrderBomSyncRespVO {

    private Long workOrderId;

    private String erpBomVersion;

    private Integer syncedBomCount;

}
