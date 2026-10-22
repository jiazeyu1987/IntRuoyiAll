package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileLifecycleEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** D-owned bridge for the A lifecycle event. Required relation facts belong to the control transaction. */
@Service
public class DccRelationControlledEventConsumer {
    private final DccRelationRemediationService remediation;
    public DccRelationControlledEventConsumer(DccRelationRemediationService remediation){this.remediation=remediation;}
    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void onLifecycleEvent(DccControlledFileLifecycleEvent event) {
        if(event==null || !"CONTROLLED".equals(event.eventType()))return;
        remediation.recordControlled(new DccRelationContracts.ControlledEvent(event.tenantId(),event.masterId(),
                event.controlledFileId(),event.approvalProcessInstanceId(),event.eventKey(),event.occurredAt()));
    }
}
