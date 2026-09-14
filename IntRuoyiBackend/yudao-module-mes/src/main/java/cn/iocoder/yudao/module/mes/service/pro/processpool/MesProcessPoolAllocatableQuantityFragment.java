package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolAllocatableQuantityFragment {

    private Long processPoolId;
    private Long sourceEventId;
    private Long sourceQuantityFragmentId;
    private Long sourceRouteProcessId;
    private Long sourceProcessId;
    private BigDecimal quantity;

}
