package cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.bom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "绠＄悊鍚庡彴 - MES 浜у搧 ERP BOM 鍚屾 Response VO")
@Data
public class MesKingdeeProductBomSyncRespVO {

    private Long itemId;

    private String erpBomVersion;

    private Integer syncedBomCount;

}
