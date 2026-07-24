package cn.iocoder.yudao.module.dcc.service.file.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileFinalizationService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DccControlledFileStatusListener extends BpmProcessInstanceStatusEventListener {

    private static final String FORM_CENTER_BUSINESS_KEY_PREFIX = "FORM_ACTION:";

    @Resource
    private DccControlledFileFinalizationService finalizationService;

    @Override
    public String getProcessDefinitionKey() {
        return DccControlledFileWorkflowServiceImpl.BPM_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        if (event.getBusinessKey() != null && event.getBusinessKey().startsWith(FORM_CENTER_BUSINESS_KEY_PREFIX)) {
            return;
        }
        finalizationService.handleProcessInstanceStatusChanged(event);
    }
}
