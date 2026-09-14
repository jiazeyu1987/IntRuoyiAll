package cn.iocoder.yudao.module.mes.service.pro.processpool;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class MesProcessPoolSubmissionQuantity {

    private final Long sourceEventId;
    private final Long employeeUserId;
    private final BigDecimal submittedQuantity;

}
