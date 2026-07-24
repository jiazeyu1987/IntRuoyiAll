package cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - ERP 生产用料清单同步 Response VO")
@Data
public class MesKingdeeProductionMaterialListSyncRespVO {

    private Integer createdCount;
    private Integer updatedCount;

    private List<Long> createdIds;
    private List<Long> updatedIds;
}
