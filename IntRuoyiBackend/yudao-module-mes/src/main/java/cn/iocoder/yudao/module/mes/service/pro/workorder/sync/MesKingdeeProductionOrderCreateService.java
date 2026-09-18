package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

public interface MesKingdeeProductionOrderCreateService {

    MesKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(Long workOrderId);

    MesKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(
            Long workOrderId, MesKingdeeProductionOrderDeterministicCreateCommand command);

    MesKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(
            MesKingdeeProductionOrderDeterministicCreateCommand command);

}
