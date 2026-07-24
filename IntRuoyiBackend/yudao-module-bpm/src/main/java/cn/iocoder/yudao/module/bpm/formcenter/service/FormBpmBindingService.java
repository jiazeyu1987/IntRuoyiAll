package cn.iocoder.yudao.module.bpm.formcenter.service;

import cn.iocoder.yudao.module.bpm.formcenter.model.BusinessActionContext;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormActionInstance;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmBinding;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormBpmStartRequest;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterErrorCode;
import cn.iocoder.yudao.module.bpm.formcenter.model.FormCenterException;

import java.util.LinkedHashMap;
import java.util.Map;

public class FormBpmBindingService {

    private static final String BUSINESS_KEY_PREFIX = "FORM_ACTION:";

    private final FormBpmStarter bpmStarter;

    public FormBpmBindingService(FormBpmStarter bpmStarter) {
        this.bpmStarter = bpmStarter;
    }

    public String startNewProcess(FormActionInstance instance, Long userId) {
        String processKey = instance.getResolution().getBpmProcessKey();
        if (processKey == null || processKey.isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "Form policy requires BPM process key before submit");
        }
        FormBpmStartRequest request = new FormBpmStartRequest(processKey,
                BUSINESS_KEY_PREFIX + instance.getInstanceCode(), variables(instance.getBusinessContext()));
        String processInstanceId = bpmStarter.start(userId, request);
        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "BPM process instance id is required after start");
        }
        instance.setBpmBinding(new FormBpmBinding(processInstanceId, null));
        return processInstanceId;
    }

    public String reuseProcessForRework(FormActionInstance instance, Long userId, String taskId) {
        if (instance.getBpmBinding() == null || instance.getBpmBinding().getProcessInstanceId() == null) {
            throw new FormCenterException(FormCenterErrorCode.BPM_BINDING_MISSING,
                    "Existing BPM process binding is required for rework submit");
        }
        instance.setBpmBinding(new FormBpmBinding(instance.getBpmBinding().getProcessInstanceId(), taskId));
        return instance.getBpmBinding().getProcessInstanceId();
    }

    private Map<String, Object> variables(BusinessActionContext context) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("tenantId", context.getTenantId());
        variables.put("dataDomain", context.getDataDomain());
        variables.put("systemCode", context.getSystemCode());
        variables.put("objectType", context.getObjectType());
        variables.put("objectId", context.getObjectId());
        variables.put("objectVersion", context.getObjectVersion());
        variables.put("actionCode", context.getActionCode());
        variables.put("objectState", context.getObjectState());
        variables.put("orgCode", context.getOrgCode());
        variables.put("deptCode", context.getDeptCode());
        variables.put("roleCodes", context.getRoleCodes());
        variables.put("productCode", context.getProductCode());
        variables.put("categoryCode", context.getCategoryCode());
        variables.put("reason", context.getReason());
        return variables;
    }

}
