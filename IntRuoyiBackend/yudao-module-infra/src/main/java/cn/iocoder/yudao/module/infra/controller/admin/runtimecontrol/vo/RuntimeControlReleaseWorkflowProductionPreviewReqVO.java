package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RuntimeControlReleaseWorkflowProductionPreviewReqVO {
    @NotBlank(message = "预检原因不能为空")
    private String reason;
    @NotNull(message = "状态版本不能为空")
    private Long expectedStateVersion;

    @JsonAnySetter
    public void rejectClientOwnedField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("RELEASE_WORKFLOW_FIELD_NOT_ALLOWED: " + fieldName);
    }
}
