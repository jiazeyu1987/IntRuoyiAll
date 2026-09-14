package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MesProcessPoolCompletionCommand {

    private final Long workOrderId;
    private final Long targetRouteProcessId;
    private final BigDecimal requiredQuantity;
    private final List<MesProcessPoolSubmissionQuantity> submissions;

}
