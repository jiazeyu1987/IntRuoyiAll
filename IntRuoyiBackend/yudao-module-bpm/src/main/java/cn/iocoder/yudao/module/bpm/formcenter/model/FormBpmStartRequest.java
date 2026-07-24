package cn.iocoder.yudao.module.bpm.formcenter.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FormBpmStartRequest {

    private final String processDefinitionKey;
    private final String businessKey;
    private final Map<String, Object> variables;

    public FormBpmStartRequest(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        this.processDefinitionKey = processDefinitionKey;
        this.businessKey = businessKey;
        this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

}
