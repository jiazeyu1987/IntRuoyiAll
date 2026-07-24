package cn.iocoder.yudao.module.bpm.businessapproval.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@Jacksonized
public class BusinessApprovalContext {

    private final Long tenantId;
    private final String dataDomain;
    private final String systemCode;
    private final String objectType;
    private final String objectId;
    private final String objectVersion;
    private final String actionCode;
    private final String objectState;
    private final Long applicantUserId;
    private final String reason;
    private final Map<String, List<Long>> startUserSelectAssignees;
    private final Map<String, Object> variables;
    @JsonIgnore
    private final Map<String, Object> transientVariables;

}
