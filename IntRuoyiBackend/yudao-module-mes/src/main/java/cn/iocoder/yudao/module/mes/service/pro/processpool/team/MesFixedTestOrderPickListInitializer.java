package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** All fixed-order test entry points rebuild the same three document types. */
@Service
@RequiredArgsConstructor
public class MesFixedTestOrderPickListInitializer {
    private final MesFixedTestOrderDataService dataService;

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void prepare(String workOrderCode, Long actor) {
        if (!MesFixedTestOrderInitializer.WORK_ORDER_CODE.equals(workOrderCode)) return;
        dataService.rebuildForReset(actor);
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void prepareForReset(String workOrderCode, Long actor) {
        prepare(workOrderCode, actor);
    }
}
