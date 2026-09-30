package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DccControlledFileObsoleteReqVO {

    @NotBlank(message = "reason is required")
    private String reason;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    private Map<String, List<Long>> startUserSelectAssignees;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean needTraining;

    public void setNeedTraining(Boolean needTraining) {
        throw new IllegalArgumentException("obsolete request does not accept needTraining");
    }
}
