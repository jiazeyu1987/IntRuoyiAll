package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MesProcessPoolFifoAllocationCommand {

    private final String allocationBatchNo;
    private final List<MesProcessPoolAllocatableQuantityFragment> fragments;
    private final List<MesProcessPoolFifoTargetWorkOrder> targetWorkOrders;

}
