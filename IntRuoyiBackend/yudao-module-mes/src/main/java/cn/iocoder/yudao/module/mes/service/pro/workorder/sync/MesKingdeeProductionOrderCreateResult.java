package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesKingdeeProductionOrderCreateResult {

    private Long workOrderId;

    private String erpFid;

    private String erpBillNo;

    private Boolean saved;

    private Boolean submitted;

}
