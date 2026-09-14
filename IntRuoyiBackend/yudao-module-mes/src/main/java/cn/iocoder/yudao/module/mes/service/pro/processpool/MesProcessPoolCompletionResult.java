package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MesProcessPoolCompletionResult {

    private final boolean completed;
    private final BigDecimal totalSubmittedQuantity;
    private final int submissionCount;
    private final List<Long> submittedEventIds;
    private final List<Long> employeeUserIds;

}
