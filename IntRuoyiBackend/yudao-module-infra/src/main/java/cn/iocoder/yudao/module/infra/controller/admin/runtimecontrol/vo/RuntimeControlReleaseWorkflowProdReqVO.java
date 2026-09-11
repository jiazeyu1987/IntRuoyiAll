package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RuntimeControlReleaseWorkflowProdReqVO {
    @NotBlank(message = "操作原因不能为空")
    private String reason;
    @NotBlank(message = "正式授权编号不能为空")
    private String authorizationGrantId;
    @NotBlank(message = "正式确认文本不能为空")
    private String prodConfirmText;

    @JsonAnySetter
    public void rejectClientOwnedField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("RELEASE_WORKFLOW_FIELD_NOT_ALLOWED: " + fieldName);
    }
}
