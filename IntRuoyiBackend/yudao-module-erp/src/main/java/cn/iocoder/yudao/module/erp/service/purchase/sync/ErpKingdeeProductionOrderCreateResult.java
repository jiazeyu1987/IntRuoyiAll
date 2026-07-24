package cn.iocoder.yudao.module.erp.service.purchase.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpKingdeeProductionOrderCreateResult {

    private String erpFid;

    private String erpBillNo;

    private Boolean saved;

    private Boolean submitted;

}
