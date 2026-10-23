package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileFinalizationService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DccControlledFileStatusListener implements ApplicationListener<BpmProcessInstanceStatusEvent> {

    private static final String FORM_CENTER_BUSINESS_KEY_PREFIX = "FORM_ACTION:";

    @Resource
    private DccControlledFileFinalizationService finalizationService;

    @Override
    public void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
        if (!DccControlledFileProcessDefinitionKeys.NATIVE_FINALIZATION_KEYS.contains(event.getProcessDefinitionKey())) {
            return;
        }
        if (event.getBusinessKey() != null && event.getBusinessKey().startsWith(FORM_CENTER_BUSINESS_KEY_PREFIX)) {
            return;
        }
        finalizationService.handleProcessInstanceStatusChanged(event);
    }
}
