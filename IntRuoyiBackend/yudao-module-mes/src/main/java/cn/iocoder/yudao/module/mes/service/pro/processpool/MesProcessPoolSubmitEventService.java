package cn.iocoder.yudao.module.mes.service.pro.processpool;

import java.math.BigDecimal;
import java.util.Optional;

public interface MesProcessPoolSubmitEventService {

    Optional<MesProcessPoolSubmitEventResult> findExistingSubmitEvent(MesProcessPoolSubmitEventCreateReqBO reqBO);

    Long createSubmitEvent(MesProcessPoolSubmitEventCreateReqBO reqBO);

    void createInitialAllocation(Long eventId, Long activeOrderId, BigDecimal outputQuantity);

}
