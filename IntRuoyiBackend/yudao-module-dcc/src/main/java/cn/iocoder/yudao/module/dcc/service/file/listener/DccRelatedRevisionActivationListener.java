package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileLifecycleEvent;
import cn.iocoder.yudao.module.dcc.service.file.DccPublicationFollowupService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DccRelatedRevisionActivationListener {
    private final DccPublicationFollowupService followups;

    public DccRelatedRevisionActivationListener(DccPublicationFollowupService followups) {
        this.followups = followups;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void onLifecycleEvent(DccControlledFileLifecycleEvent event) {
        if (event != null && "ACTIVATED".equals(event.eventType())) followups.recordActivatedRevision(event);
    }
}
