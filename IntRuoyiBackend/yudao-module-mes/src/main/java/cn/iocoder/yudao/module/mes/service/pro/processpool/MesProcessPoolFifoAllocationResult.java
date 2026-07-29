package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolFifoAllocationLineDO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MesProcessPoolFifoAllocationResult {

    private final List<MesProcessPoolFifoAllocationLineDO> lines;
    private final BigDecimal totalAllocatedQuantity;

}
