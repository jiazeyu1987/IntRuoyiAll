package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RuntimeControlReleaseWorkflowTestAcceptanceReqVO {

    @NotBlank(message = "测试验收结果不能为空")
    private String result;

    @NotBlank(message = "测试验收结论不能为空")
    private String conclusion;

    @JsonAnySetter
    public void rejectClientOwnedField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("RELEASE_WORKFLOW_FIELD_NOT_ALLOWED: " + fieldName);
    }
}
