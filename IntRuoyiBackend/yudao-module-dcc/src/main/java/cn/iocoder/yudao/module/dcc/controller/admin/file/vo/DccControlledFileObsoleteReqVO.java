package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DccControlledFileObsoleteReqVO {
    private java.util.List<Long> selectedSignoffDepartmentIds;
    private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes projectAttributes;

    @NotBlank(message = "reason is required")
    private String reason;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String approvalProcessInstanceId;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String approvedVersionNo;

    private Map<String, List<Long>> startUserSelectAssignees;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean needTraining;

    public void setNeedTraining(Boolean needTraining) {
        throw new IllegalArgumentException("obsolete request does not accept needTraining");
    }
}
