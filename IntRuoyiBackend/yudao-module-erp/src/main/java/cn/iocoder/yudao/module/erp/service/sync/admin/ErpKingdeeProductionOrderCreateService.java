package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeProductionOrderCreateRespVO;
import jakarta.validation.Valid;

public interface ErpKingdeeProductionOrderCreateService {

    ErpKingdeeProductionOrderCreateRespVO createProductionOrder(
            @Valid ErpKingdeeProductionOrderCreateReqVO reqVO);

}
